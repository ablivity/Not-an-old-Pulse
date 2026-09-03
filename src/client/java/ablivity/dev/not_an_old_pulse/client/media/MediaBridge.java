package ablivity.dev.not_an_old_pulse.client.media;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class MediaBridge {

    public static class Track {
        public boolean playing;
        public String title = "";
        public String artist = "";
        public String albumTitle = "";
        public String source = "";
        public String mediaKey = "";
        public String cover = "";
        public long positionMs;
        public long durationMs;
        public boolean shuffleActive;
        public boolean shuffleSupported;
        public String repeatMode = "none";
        public boolean repeatSupported;
        public boolean seekSupported;
        private transient long receivedAtMs;

        public long estimatedPositionMs() {
            if (durationMs <= 0L) return 0L;
            long value = Math.max(0L, positionMs);
            if (playing && receivedAtMs > 0L) {
                value += Math.max(0L, System.currentTimeMillis() - receivedAtMs);
            }
            return Math.min(durationMs, value);
        }

        public float progress() {
            if (durationMs <= 0L) return 0.0f;
            return Math.max(0.0f, Math.min(1.0f, estimatedPositionMs() / (float) durationMs));
        }

        public byte[] coverBytes() {
            if (cover == null || cover.isEmpty()) return null;
            try {
                return Base64.getDecoder().decode(cover);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final Gson GSON = new Gson();
    private static final long RESPONSE_TIMEOUT_SECONDS = 5L;

    public static final MediaBridge INSTANCE = new MediaBridge();

    private final AtomicReference<Track> current = new AtomicReference<>(new Track());
    private final AtomicBoolean started = new AtomicBoolean();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pulse-smtc");
        thread.setDaemon(true);
        return thread;
    });

    private final Object processLock = new Object();
    private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();

    private Path script;
    private Process process;
    private BufferedWriter processInput;
    private Thread processReader;
    private boolean windows;

    public void start() {
        if (!started.compareAndSet(false, true)) return;

        windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (!windows) return;

        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("not_an_old_pulse");
            Files.createDirectories(dir);
            script = dir.resolve("smtc.ps1");
            installScript(script);
        } catch (IOException exception) {
            return;
        }

        exec.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
    }

    public Track track() {
        return current.get();
    }

    public void togglePlay() {
        command("toggle");
    }

    public void next() {
        command("next");
    }

    public void prev() {
        command("prev");
    }

    public void command(String command) {
        if (command == null || command.isBlank()) return;
        exec.execute(() -> exchange(command.trim()));
    }

    private void poll() {
        if (!started.get() || !windows || script == null) return;

        String json = exchange("poll");
        if (json == null || json.isBlank()) return;

        try {
            Track next = GSON.fromJson(json.trim(), Track.class);
            if (next == null) return;
            normalize(next);
            publish(next);
        } catch (Exception ignored) {
        }
    }

    private void publish(Track track) {
        track.receivedAtMs = System.currentTimeMillis();
        current.set(track);
    }

    private void normalize(Track track) {
        track.title = safe(track.title);
        track.artist = safe(track.artist);
        track.albumTitle = safe(track.albumTitle);
        track.source = safe(track.source);
        track.mediaKey = safe(track.mediaKey);
        track.cover = safe(track.cover);

        Track prev = current.get();
        if (track.cover.isEmpty() && prev != null && !prev.cover.isEmpty() && prev.mediaKey.equals(track.mediaKey)) {
            track.cover = prev.cover;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String exchange(String command) {
        synchronized (processLock) {
            if (!ensureProcessLocked()) return null;

            try {
                responses.clear();
                processInput.write(command);
                processInput.newLine();
                processInput.flush();

                return responses.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            } catch (IOException | RuntimeException exception) {
                stopProcessLocked();
                return null;
            }
        }
    }

    private boolean ensureProcessLocked() {
        if (process != null && process.isAlive() && processInput != null) {
            return true;
        }

        stopProcessLocked();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoLogo",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-WindowStyle",
                    "Hidden",
                    "-File",
                    script.toString(),
                    "-Server"
            );
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            process = builder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            responses.clear();

            Process activeProcess = process;
            processReader = new Thread(() -> readResponses(activeProcess), "pulse-smtc-reader");
            processReader.setDaemon(true);
            processReader.start();

            return true;
        } catch (IOException exception) {
            stopProcessLocked();
            return false;
        }
    }

    private void readResponses(Process activeProcess) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (activeProcess == process && (line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    responses.offer(line);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void stopProcessLocked() {
        if (processInput != null) {
            try { processInput.close(); } catch (IOException ignored) {}
            processInput = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (processReader != null) {
            processReader.interrupt();
            processReader = null;
        }
        responses.clear();
    }

    private void installScript(Path destination) throws IOException {
        byte[] bundled;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("assets/not_an_old_pulse/smtc.ps1")) {
            if (input == null) throw new IOException("bundled smtc.ps1 resource is missing");
            bundled = input.readAllBytes();
        }

        if (Files.isRegularFile(destination)) {
            byte[] installed = Files.readAllBytes(destination);
            if (Arrays.equals(installed, bundled)) return;
        }

        Files.write(destination, bundled);
    }
}
