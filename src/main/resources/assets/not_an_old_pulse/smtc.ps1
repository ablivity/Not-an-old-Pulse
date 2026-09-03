param(
    [switch]$Server,
    [string]$Cmd = ''
)

# sn0w.visual SMTC bridge version 13 - inline cover with automatic Java-side refresh
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public class Win32Media {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
    public static void Next() {
        keybd_event(0xB0, 0, 0, UIntPtr.Zero);
        keybd_event(0xB0, 0, 2, UIntPtr.Zero);
    }
    public static void Prev() {
        keybd_event(0xB1, 0, 0, UIntPtr.Zero);
        keybd_event(0xB1, 0, 2, UIntPtr.Zero);
    }
    public static void Toggle() {
        keybd_event(0xB3, 0, 0, UIntPtr.Zero);
        keybd_event(0xB3, 0, 2, UIntPtr.Zero);
    }
}
"@

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
                   $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]

function Await($WinRtTask, $ResultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
    $netTask = $asTask.Invoke($null, @($WinRtTask))
    $netTask.Wait(5000) | Out-Null
    if (-not $netTask.IsCompleted) { throw 'WinRT operation timed out' }
    $netTask.Result
}

$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) `
                 ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])

$script:lastMediaKey = ''
$script:lastCover = ''

function Get-CurrentSession {
    try { return $manager.GetCurrentSession() } catch { return $null }
}

function Get-MediaKey([string]$appId, [string]$title, [string]$artist) {
    $text = $appId + "`n" + $title + "`n" + $artist
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [Convert]::ToBase64String($sha.ComputeHash($bytes))
    } finally {
        $sha.Dispose()
    }
}

function Invoke-Control([string]$command) {
    $session = Get-CurrentSession

    try {
        if ($command -match '^seek:(\d+)$') {
            if ($session) {
                $timeline = $session.GetTimelineProperties()
                $relativeMs = [long]$Matches[1]
                $startTicks = [long]$timeline.StartTime.Ticks
                $endTicks = [long]$timeline.EndTime.Ticks
                $targetTicks = $startTicks + ($relativeMs * 10000L)
                $targetTicks = [Math]::Max($startTicks, [Math]::Min($endTicks, $targetTicks))
                $null = $session.TryChangePlaybackPositionAsync([long]$targetTicks)
                return '{"ok":true}'
            }
            return '{"ok":false}'
        }

        switch ($command) {
            'toggle' {
                $ok = $false
                if ($session) {
                    try {
                        $task = $asTaskGeneric.MakeGenericMethod([bool]).Invoke($null, @($session.TryTogglePlayPauseAsync()))
                        $task.Wait(1000) | Out-Null
                        $ok = $task.Result
                    } catch {}
                }
                if (-not $ok) { [Win32Media]::Toggle() }
            }
            'next'   {
                $ok = $false
                if ($session) {
                    try {
                        $task = $asTaskGeneric.MakeGenericMethod([bool]).Invoke($null, @($session.TrySkipNextAsync()))
                        $task.Wait(1000) | Out-Null
                        $ok = $task.Result
                    } catch {}
                }
                if (-not $ok) { [Win32Media]::Next() }
            }
            'prev'   {
                $ok = $false
                if ($session) {
                    try {
                        $task = $asTaskGeneric.MakeGenericMethod([bool]).Invoke($null, @($session.TrySkipPreviousAsync()))
                        $task.Wait(1000) | Out-Null
                        $ok = $task.Result
                    } catch {}
                }
                if (-not $ok) { [Win32Media]::Prev() }
            }
            'shuffle' {
                if ($session) {
                    $playbackInfo = $session.GetPlaybackInfo()
                    $currentShuffle = $false
                    if ($null -ne $playbackInfo.IsShuffleActive) {
                        $currentShuffle = [bool]$playbackInfo.IsShuffleActive
                    }
                    $null = $session.TryChangeShuffleActiveAsync(-not $currentShuffle)
                }
            }
            'repeat' {
                if ($session) {
                    $null = [Windows.Media.MediaPlaybackAutoRepeatMode, Windows.Media, ContentType=WindowsRuntime]
                    $playbackInfo = $session.GetPlaybackInfo()
                    $currentRepeat = 'None'
                    if ($null -ne $playbackInfo.AutoRepeatMode) {
                        $currentRepeat = $playbackInfo.AutoRepeatMode.ToString()
                    }
                    $nextRepeat = switch ($currentRepeat) {
                        'None'  { [Windows.Media.MediaPlaybackAutoRepeatMode]::Track }
                        'Track' { [Windows.Media.MediaPlaybackAutoRepeatMode]::List }
                        default { [Windows.Media.MediaPlaybackAutoRepeatMode]::None }
                    }
                    $null = $session.TryChangeAutoRepeatModeAsync($nextRepeat)
                }
            }
            default { return '{"ok":false}' }
        }
        return '{"ok":true}'
    } catch {
        return '{"ok":false}'
    }
}

function Read-Cover($props, [string]$mediaKey) {
    if ($mediaKey -eq $script:lastMediaKey -and -not [string]::IsNullOrEmpty($script:lastCover)) {
        return $script:lastCover
    }

    try {
        $ref = $props.Thumbnail
        if (-not $ref) {
            $script:lastCover = ''
            $script:lastMediaKey = $mediaKey
            return ''
        }

        $null = [Windows.Storage.Streams.RandomAccessStream, Windows.Storage.Streams, ContentType=WindowsRuntime]
        $srcStream = Await ($ref.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
        $asStreamForRead = [System.IO.WindowsRuntimeStreamExtensions].GetMethod(
            'AsStreamForRead',
            [Type[]]@([Windows.Storage.Streams.IInputStream])
        )
        $netStream = $asStreamForRead.Invoke($null, @($srcStream))

        $inputMemory = New-Object System.IO.MemoryStream
        $netStream.CopyTo($inputMemory)
        $inputMemory.Position = 0
        if ($inputMemory.Length -le 0 -or $inputMemory.Length -gt 16777216) {
            $inputMemory.Dispose()
            return ''
        }

        $image = [System.Drawing.Image]::FromStream($inputMemory)
        $side = 128
        $bitmap = New-Object System.Drawing.Bitmap $side, $side
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.Clear([System.Drawing.Color]::Transparent)

        $graphics.DrawImage($image, 0, 0, $side, $side)
        $graphics.Dispose()

        $outputMemory = New-Object System.IO.MemoryStream
        $bitmap.Save($outputMemory, [System.Drawing.Imaging.ImageFormat]::Png)
        $script:lastCover = [Convert]::ToBase64String($outputMemory.ToArray())
        $script:lastMediaKey = $mediaKey

        $outputMemory.Dispose()
        $bitmap.Dispose()
        $image.Dispose()
        $inputMemory.Dispose()
        return $script:lastCover
    } catch {
        return ''
    }
}

function Get-StateJson {
    $session = Get-CurrentSession
    if (-not $session) {
        $script:lastMediaKey = ''
        $script:lastCover = ''
        return ([ordered]@{
            playing = $false
            title = ''
            artist = ''
            albumTitle = ''
            source = ''
            mediaKey = ''
            cover = ''
            positionMs = 0
            durationMs = 0
            shuffleActive = $false
            shuffleSupported = $false
            repeatMode = 'none'
            repeatSupported = $false
            seekSupported = $false
        } | ConvertTo-Json -Compress)
    }

    try {
        $props = Await ($session.TryGetMediaPropertiesAsync()) `
                       ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
        $info = $session.GetPlaybackInfo()
        $playing = $info.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing

        $shuffleActive = $false
        $shuffleSupported = $false
        $repeatMode = 'none'
        $repeatSupported = $false
        $seekSupported = $false
        try {
            if ($null -ne $info.IsShuffleActive) {
                $shuffleActive = [bool]$info.IsShuffleActive
            }
            if ($null -ne $info.AutoRepeatMode) {
                switch ($info.AutoRepeatMode.ToString()) {
                    'Track' { $repeatMode = 'track' }
                    'List'  { $repeatMode = 'list' }
                    default { $repeatMode = 'none' }
                }
            }
            if ($info.Controls) {
                $shuffleSupported = [bool]$info.Controls.IsShuffleEnabled
                $repeatSupported = [bool]$info.Controls.IsRepeatEnabled
                $seekSupported = [bool]$info.Controls.IsPlaybackPositionEnabled
            }
        } catch { }

        $positionMs = 0
        $durationMs = 0
        try {
            $timeline = $session.GetTimelineProperties()
            if ($timeline) {
                $endTicks = [long]$timeline.EndTime.Ticks
                $posTicks = [long]$timeline.Position.Ticks
                $startTicks = [long]$timeline.StartTime.Ticks
                $durationMs = [long][Math]::Max(0, ($endTicks - $startTicks) / 10000L)
                $positionMs = [long][Math]::Max(0, ($posTicks - $startTicks) / 10000L)

                if ($playing -and $timeline.LastUpdatedTime.Ticks -gt 0) {
                    $elapsedMs = [long]([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalMilliseconds
                    if ($elapsedMs -gt 0 -and $elapsedMs -lt 86400000) {
                        $positionMs = [long]($positionMs + $elapsedMs)
                    }
                }
                if ($durationMs -gt 0) {
                    $positionMs = [Math]::Min($durationMs, $positionMs)
                }
            }
        } catch { }

        $appId = [string]$session.SourceAppUserModelId
        $source = 'Media'
        if     ($appId -match 'Spotify')                           { $source = 'Spotify' }
        elseif ($appId -match 'Apple|iTunes|AppleInc')             { $source = 'Apple Music' }
        elseif ($appId -match 'Chrome|msedge|firefox|opera|brave') { $source = 'SoundCloud / Web' }

        $title = [string]$props.Title
        $artist = [string]$props.Artist
        $albumTitle = ''
        try { $albumTitle = [string]$props.AlbumTitle } catch { }
        $mediaKey = Get-MediaKey $appId $title $artist
        $cover = Read-Cover $props $mediaKey

        return ([ordered]@{
            playing = [bool]$playing
            title = $title
            artist = $artist
            albumTitle = $albumTitle
            source = $source
            mediaKey = $mediaKey
            cover = $cover
            positionMs = [long]$positionMs
            durationMs = [long]$durationMs
            shuffleActive = [bool]$shuffleActive
            shuffleSupported = [bool]$shuffleSupported
            repeatMode = $repeatMode
            repeatSupported = [bool]$repeatSupported
            seekSupported = [bool]$seekSupported
        } | ConvertTo-Json -Compress)
    } catch {
        return ([ordered]@{
            playing = $false
            title = ''
            artist = ''
            albumTitle = ''
            source = ''
            mediaKey = ''
            cover = ''
            positionMs = 0
            durationMs = 0
            shuffleActive = $false
            shuffleSupported = $false
            repeatMode = 'none'
            repeatSupported = $false
            seekSupported = $false
        } | ConvertTo-Json -Compress)
    }
}

function Handle-Request([string]$request) {
    if ($request -eq 'poll' -or $request -eq 'debug' -or [string]::IsNullOrWhiteSpace($request)) {
        return Get-StateJson
    }
    return Invoke-Control $request
}

if ($Server) {
    while (($line = [Console]::In.ReadLine()) -ne $null) {
        $response = Handle-Request $line.Trim()
        [Console]::Out.WriteLine($response)
        [Console]::Out.Flush()
    }
    return
}

Write-Output (Handle-Request $Cmd)
