package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ablivity.dev.not_an_old_pulse.client.media.MediaBridge;
import ablivity.dev.not_an_old_pulse.client.util.RenderUtils;
import ablivity.dev.not_an_old_pulse.config.ModConfig;
import ablivity.dev.not_an_old_pulse.mixin.BossBarHudAccessor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicIslandRenderer {

    private static final Identifier COVER_CIRCLE_ID = Identifier.of("not_an_old_pulse", "music_cover_circle");
    private static final Identifier COVER_ROUNDED_ID = Identifier.of("not_an_old_pulse", "music_cover_rounded");
    private static String lastLoadedCover = null;
    private static NativeImageBackedTexture coverCircleTexture = null;
    private static NativeImageBackedTexture coverRoundedTexture = null;

    // Background thread for cover processing
    private static final ExecutorService COVER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pulse-cover-processor");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean coverProcessing = new AtomicBoolean(false);
    private static volatile NativeImage pendingCircleImg = null;
    private static volatile NativeImage pendingRoundedImg = null;

    private static float currentY = 8.0f;
    private static float animWave = 0.0f;
    private static float expandAnim = 0.0f;

    // Track active bounds
    private static int lastX = 0;
    private static int lastY = 0;
    private static int lastW = 0;
    private static int lastH = 0;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        ModConfig config = ModConfig.getInstance();
        if (!config.get("dynamicIsland", true)) return;

        // Hide Dynamic Island when Tab (Player List) is open
        if (client.options.playerListKey.isPressed()) return;

        // Apply any pending cover textures from background thread (main thread only)
        applyPendingCovers(client);

        // 1. Calculate bossbar avoidance offset
        float targetY = 8.0f;
        String bossTitle = null;
        if (client.inGameHud != null && client.inGameHud.getBossBarHud() != null) {
            Map<UUID, ClientBossBar> bossBars = ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).getBossBars();
            if (bossBars != null && !bossBars.isEmpty()) {
                targetY = 22.0f + (bossBars.size() - 1) * 19.0f;
                for (ClientBossBar bar : bossBars.values()) {
                    bossTitle = bar.getName().getString();
                    break;
                }
            }
        }
        currentY += (targetY - currentY) * 0.16f;

        MediaBridge.Track track = MediaBridge.INSTANCE.track();
        boolean hasMusic = track != null && (!track.title.isBlank() || track.playing);

        TextRenderer tr = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int y = Math.round(currentY);

        boolean inChat = client.currentScreen instanceof ChatScreen;
        double mouseX = 0, mouseY = 0;
        if (inChat) {
            mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
            mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();
        }

        // Expand ONLY if in chat AND hovering over island
        boolean isHovered = inChat && hasMusic && (mouseX >= lastX && mouseX <= lastX + lastW && mouseY >= lastY && mouseY <= lastY + lastH + 12);
        float targetExpand = isHovered ? 1.0f : 0.0f;
        expandAnim += (targetExpand - expandAnim) * 0.22f;

        // Snap to 0/1 when very close to avoid unnecessary blending
        if (expandAnim < 0.01f) expandAnim = 0.0f;
        else if (expandAnim > 0.99f) expandAnim = 1.0f;

        if (hasMusic) {
            updateCoverTexture(client, track);
            renderMusicIsland(context, client, tr, track, screenW, y, expandAnim > 0.05f);
        } else {
            renderIdleIsland(context, client, tr, null, screenW, y);
        }
    }

    private static void applyPendingCovers(MinecraftClient client) {
        NativeImage circleImg = pendingCircleImg;
        NativeImage roundedImg = pendingRoundedImg;
        if (circleImg == null || roundedImg == null) return;

        // Clear pending so we don't apply twice
        pendingCircleImg = null;
        pendingRoundedImg = null;

        // Destroy old textures
        if (coverCircleTexture != null) {
            client.getTextureManager().destroyTexture(COVER_CIRCLE_ID);
            coverCircleTexture = null;
        }
        if (coverRoundedTexture != null) {
            client.getTextureManager().destroyTexture(COVER_ROUNDED_ID);
            coverRoundedTexture = null;
        }

        coverCircleTexture = new NativeImageBackedTexture(() -> "music_cover_circle", circleImg);
        coverRoundedTexture = new NativeImageBackedTexture(() -> "music_cover_rounded", roundedImg);

        client.getTextureManager().registerTexture(COVER_CIRCLE_ID, coverCircleTexture);
        client.getTextureManager().registerTexture(COVER_ROUNDED_ID, coverRoundedTexture);

        coverProcessing.set(false);
    }

    private static void updateCoverTexture(MinecraftClient client, MediaBridge.Track track) {
        if (track == null || track.cover == null || track.cover.isEmpty()) {
            return;
        }
        if (track.cover.equals(lastLoadedCover) && coverCircleTexture != null && coverRoundedTexture != null) return;
        if (coverProcessing.get()) return; // Already processing

        lastLoadedCover = track.cover;
        coverProcessing.set(true);

        byte[] bytes = track.coverBytes();
        if (bytes == null || bytes.length == 0) {
            coverProcessing.set(false);
            return;
        }

        COVER_EXECUTOR.execute(() -> {
            try {
                try (NativeImage raw = NativeImage.read(bytes)) {
                    int w = raw.getWidth();
                    int h = raw.getHeight();
                    int dim = Math.min(w, h);
                    int cropX = (w - dim) / 2;
                    int cropY = (h - dim) / 2;

                    NativeImage circleImg = new NativeImage(dim, dim, false);
                    NativeImage roundedImg = new NativeImage(dim, dim, false);

                    float cx = (dim - 1) / 2.0f;
                    float cy = (dim - 1) / 2.0f;
                    float maxR = dim / 2.0f;
                    float cornerR = dim * 0.22f;

                    for (int py = 0; py < dim; py++) {
                        for (int px = 0; px < dim; px++) {
                            int srcColor = raw.getColorArgb(cropX + px, cropY + py);
                            int srcAlpha = (srcColor >>> 24) & 0xFF;
                            int rgb = srcColor & 0x00FFFFFF;

                            // 1. Circle Mask
                            float dxCircle = px - cx;
                            float dyCircle = py - cy;
                            float distCircle = (float) Math.sqrt(dxCircle * dxCircle + dyCircle * dyCircle);
                            if (distCircle <= maxR) {
                                float alphaMult = Math.min(1.0f, Math.max(0.0f, maxR - distCircle + 0.5f));
                                int a = (int) (srcAlpha * alphaMult);
                                circleImg.setColorArgb(px, py, (a << 24) | rgb);
                            } else {
                                circleImg.setColorArgb(px, py, 0);
                            }

                            // 2. Rounded Square Mask
                            float dxCorner = Math.max(0.0f, Math.max(cornerR - px, (px + 1) - (dim - cornerR)));
                            float dyCorner = Math.max(0.0f, Math.max(cornerR - py, (py + 1) - (dim - cornerR)));
                            if (dxCorner > 0.0f && dyCorner > 0.0f) {
                                float distCorner = (float) Math.sqrt(dxCorner * dxCorner + dyCorner * dyCorner);
                                if (distCorner <= cornerR) {
                                    float alphaMult = Math.min(1.0f, Math.max(0.0f, cornerR - distCorner + 0.5f));
                                    int a = (int) (srcAlpha * alphaMult);
                                    roundedImg.setColorArgb(px, py, (a << 24) | rgb);
                                } else {
                                    roundedImg.setColorArgb(px, py, 0);
                                }
                            } else {
                                roundedImg.setColorArgb(px, py, srcColor);
                            }
                        }
                    }

                    // Post to main thread via volatile fields
                    pendingCircleImg = circleImg;
                    pendingRoundedImg = roundedImg;
                }
            } catch (Throwable ignored) {
                coverProcessing.set(false);
            }
        });
    }

    /**
     * Idle Capsule (sn0wisy  /  24 ms  /  144 FPS)
     */
    private static void renderIdleIsland(DrawContext context, MinecraftClient client, TextRenderer tr, String bossTitle, int screenW, int y) {
        String part1 = "not an old pulse";
        String sep1 = " / ";
        String part2 = (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()) != null ? 
                        client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()).getLatency() : 24) + " ms";
        String sep2 = " / ";
        String part3 = client.getCurrentFps() + " FPS";

        int w1 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, part1);
        int wS1 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, sep1);
        int w2 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, part2);
        int wS2 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, sep2);
        int w3 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, part3);

        int textW = w1 + wS1 + w2 + wS2 + w3;
        
        if (false) {
            part1 = "Bossbar";
            part2 = bossTitle;
            w1 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, part1);
            w2 = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, part2);
            textW = w1 + wS1 + w2;
            part3 = ""; wS2 = 0; w3 = 0; sep2 = "";
        }

        int pillW = textW + 42;
        int pillH = 24;
        int x = (screenW - pillW) / 2;

        lastX = x; lastY = y; lastW = pillW; lastH = pillH;

        // Draw smooth rounded capsule (slightly darker background like screenshot)
        RenderUtils.drawRoundedCapsule(context, x, y, pillW, pillH, 0xEE0A0A0C);
        RenderUtils.drawRoundedCapsule(context, x + 1, y + 1, pillW - 2, pillH - 2, 0xFA111114);

        // Draw soft blue glow (Centered perfectly behind the logo)
        RenderUtils.drawFilledCircle(context, x + 8, y + 3, 16, 0x2A0099FF);

        // Draw new blue logo
        int cx = x + 12;
        int cy = y + 7;
        int color = 0xFF00A3FF; // Bright blue

        // Top arc
        context.fill(cx + 2, cy, cx + 7, cy + 1, color);
        context.fill(cx + 1, cy + 1, cx + 2, cy + 2, color);
        context.fill(cx + 7, cy + 1, cx + 8, cy + 2, color);
        context.fill(cx, cy + 2, cx + 1, cy + 5, color);
        context.fill(cx + 8, cy + 2, cx + 9, cy + 5, color);
        context.fill(cx + 1, cy + 5, cx + 2, cy + 6, color);
        context.fill(cx + 7, cy + 5, cx + 8, cy + 6, color);

        // Horizontal bar
        context.fill(cx + 1, cy + 6, cx + 8, cy + 7, color);

        // Bottom V
        context.fill(cx + 2, cy + 7, cx + 3, cy + 8, color);
        context.fill(cx + 6, cy + 7, cx + 7, cy + 8, color);
        context.fill(cx + 3, cy + 8, cx + 4, cy + 9, color);
        context.fill(cx + 5, cy + 8, cx + 6, cy + 9, color);
        context.fill(cx + 4, cy + 9, cx + 5, cy + 10, color);

        // Draw Text
        int tx = x + 28;
        int ty = y + 7;
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, part1, tx, ty, 0xFFFFFFFF);
        tx += w1;
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, sep1, tx, ty, 0xFF555555);
        tx += wS1;
        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, part2, tx, ty, 0xFFFFFFFF);
        
        if (!part3.isEmpty()) {
            tx += w2;
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, sep2, tx, ty, 0xFF555555);
            tx += wS2;
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, part3, tx, ty, 0xFFFFFFFF);
        }
    }

    /**
     * Music Island (Compact Pill -> Expanded Screenshot Player in Chat)
     */
    private static void renderMusicIsland(DrawContext context, MinecraftClient client, TextRenderer tr, MediaBridge.Track track, int screenW, int y, boolean showExpanded) {
        String title = track.title.isBlank() ? "Unknown Track" : track.title;
        String artist = track.artist.isBlank() ? "Media Player" : track.artist;
        int titleTextW = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, title);

        int compactW = Math.min(145, Math.max(90, titleTextW + 36));
        int expandedW = 190;
        int pillW = (int) (compactW + (expandedW - compactW) * expandAnim);

        int compactH = 22;
        int expandedH = 80;
        int pillH = (int) (compactH + (expandedH - compactH) * expandAnim);

        int x = (screenW - pillW) / 2;
        lastX = x; lastY = y; lastW = pillW; lastH = pillH;

        if (pillH <= 26) {
            RenderUtils.drawRoundedCapsule(context, x, y, pillW, pillH, 0xEE111114);
            RenderUtils.drawRoundedCapsule(context, x + 1, y + 1, pillW - 2, pillH - 2, 0xFA141418);
        } else {
            RenderUtils.drawSmoothRoundedRect(context, x, y, pillW, pillH, 6, 0xEE111116);
            RenderUtils.drawSmoothRoundedRect(context, x + 1, y + 1, pillW - 2, pillH - 2, 5, 0xFA15151B);
        }

        boolean hasCover = coverCircleTexture != null && coverRoundedTexture != null;

        if (expandAnim < 0.3f) {
            // --- Compact Mode (Circular Cover on Left + Title) ---
            int coverSize = 16;
            int coverX = x + 3;
            int coverY = y + 3;

            if (hasCover) {
                // Draw circular cover art using row-by-row circular clipping
                RenderUtils.drawCircularTexture(context, COVER_CIRCLE_ID, coverX, coverY, coverSize);
            } else {
                RenderUtils.drawFilledCircle(context, coverX, coverY, coverSize, 0xFF2A2A35);
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u266A", coverX + 5, coverY + 4, 0xFF8A5AE2);
            }

            int textX = coverX + coverSize + 5;
            int availW = pillW - (textX - x) - 8;

            if (availW > 10) {
                context.enableScissor(textX, y, textX + availW, y + compactH);
                if (titleTextW > availW) {
                    long now = System.currentTimeMillis();
                    int shift = (int) ((now / 28) % (titleTextW + 24));
                    ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, title, textX - shift, y + 6, 0xFFFFFFFF);
                    ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, title, textX - shift + titleTextW + 24, y + 6, 0xFFFFFFFF);
                } else {
                    ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, title, textX, y + 6, 0xFFFFFFFF);
                }
                context.disableScissor();
            }
        } else {
            // --- Expanded Mode (Rounded Square Cover + Info) ---
            int coverSize = 36;
            int coverX = x + 12;
            int coverY = y + 12;

            if (hasCover) {
                // Draw rounded square cover art
                RenderUtils.drawRoundedTexture(context, COVER_ROUNDED_ID, coverX, coverY, coverSize, coverSize, 8);
            } else {
                RenderUtils.drawSmoothRoundedRect(context, coverX, coverY, coverSize, coverSize, 3, 0xFF22222E);
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u266A", coverX + 14, coverY + 13, 0xFF8A5AE2);
            }

            // Title & Artist
            int textX = coverX + coverSize + 10;
            int maxTitleW = pillW - (textX - x) - 34;
            String trimmedTitle = ablivity.dev.not_an_old_pulse.client.font.FontUtil.trimToWidth(tr, title, maxTitleW);
            String trimmedArtist = ablivity.dev.not_an_old_pulse.client.font.FontUtil.trimToWidth(tr, artist, maxTitleW);

            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, trimmedTitle, textX, y + 14, 0xFFFFFFFF);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, trimmedArtist, textX, y + 27, 0xFFA0A0B0);

            // Mini Status Equalizer dots (Top right)
            animWave += 0.08f;
            int waveX = x + pillW - 28;
            int waveCenterY = y + 18;
            for (int i = 0; i < 5; i++) {
                int bx = waveX + i * 4;
                int bh = track.playing ? (int) (2 + Math.abs(MathHelper.sin(animWave + i * 1.1f)) * 4) : 2;
                int by = waveCenterY - bh / 2;
                context.fill(bx, by, bx + 2, by + bh, 0xFF5C5C6E);
            }

            // Progress Bar & Times
            long pos = track.estimatedPositionMs();
            long dur = Math.max(1000L, track.durationMs);
            float progress = track.progress();

            int barY = y + 52;
            int barX = x + 34;
            int barW = pillW - 68;

            long posSec = pos / 1000;
            long remainSec = Math.max(0L, dur - pos) / 1000;
            String timeElapsed = String.format("%d:%02d", posSec / 60, posSec % 60);
            String timeRemain = String.format("-%d:%02d", remainSec / 60, remainSec % 60);

            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, timeElapsed, x + 10, barY - 2, 0xFF9E9EAE);
            int twRemain = ablivity.dev.not_an_old_pulse.client.font.FontUtil.getWidth(tr, timeRemain);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, timeRemain, x + pillW - 10 - twRemain, barY - 2, 0xFF9E9EAE);

            // Bar background & fill
            context.fill(barX, barY, barX + barW, barY + 3, 0xFF2D2D3A);
            if (progress > 0.0f) {
                context.fill(barX, barY, barX + (int) (barW * progress), barY + 3, 0xFFFFFFFF);
            }

            // Media Controls (<<  ||  >>)
            int btnY = y + 62;
            int cx = x + pillW / 2;

            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u25C0\u25C0", cx - 38, btnY + 2, 0xFFD8D8E5);
            if (track.playing) {
                // Two rounded vertical pause bars
                context.fill(cx - 5, btnY - 1, cx - 2, btnY + 12, 0xFFFFFFFF);
                context.fill(cx + 2, btnY - 1, cx + 5, btnY + 12, 0xFFFFFFFF);
            } else {
                ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u25B6", cx - 3, btnY + 2, 0xFFFFFFFF);
            }
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawTextWithShadow(context, tr, "\u25B6\u25B6", cx + 26, btnY + 2, 0xFFD8D8E5);
        }
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= lastX && mouseX <= lastX + lastW && mouseY >= lastY && mouseY <= lastY + lastH) {
            MediaBridge.Track track = MediaBridge.INSTANCE.track();
            if (track != null && (!track.title.isBlank() || track.playing)) {
                int cx = lastX + lastW / 2;
                if (mouseY >= lastY + 52) {
                    if (mouseX < cx - 16) {
                        MediaBridge.INSTANCE.prev();
                    } else if (mouseX > cx + 16) {
                        MediaBridge.INSTANCE.next();
                    } else {
                        MediaBridge.INSTANCE.togglePlay();
                    }
                    return true;
                }
            }
            MediaBridge.INSTANCE.togglePlay();
            return true;
        }
        return false;
    }
}

