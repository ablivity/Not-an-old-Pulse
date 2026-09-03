package ablivity.dev.not_an_old_pulse.client.gui.browser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.Text;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.CefSettings;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.lwjgl.glfw.GLFW;
import ablivity.dev.not_an_old_pulse.client.render.BrowserTextureRenderer;
import ablivity.dev.not_an_old_pulse.config.ModConfig;
import ablivity.dev.not_an_old_pulse.mixin.DrawContextAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class BrowserScreen extends Screen {
    private static final String RESOURCE_PATH = "/assets/not_an_old_pulse/gui/click_gui.html";
    private static CefMessageRouter messageRouter;
    private static boolean routerInitialized = false;
    private static MCEFBrowser cachedBrowser;

    private boolean initializingBrowser = false;

    public BrowserScreen() {
        super(Text.literal("not an old pulse (web)"));
    }

    private GpuSampler cachedSampler;

    private int getBrowserWidth() {
        if (client != null && client.getWindow() != null) {
            return Math.max(1, client.getWindow().getFramebufferWidth());
        }
        return Math.max(1, this.width);
    }

    private int getBrowserHeight() {
        if (client != null && client.getWindow() != null) {
            return Math.max(1, client.getWindow().getFramebufferHeight());
        }
        return Math.max(1, this.height);
    }

    private double getScaleX() {
        if (client != null && client.getWindow() != null && this.width > 0) {
            return (double) client.getWindow().getFramebufferWidth() / (double) this.width;
        }
        return 1.0;
    }

    private double getScaleY() {
        if (client != null && client.getWindow() != null && this.height > 0) {
            return (double) client.getWindow().getFramebufferHeight() / (double) this.height;
        }
        return 1.0;
    }

    private void updateBrowserSize() {
        if (cachedBrowser != null && client != null) {
            cachedBrowser.resize(getBrowserWidth(), getBrowserHeight());
        }
    }

    @Override
    protected void init() {
        super.init();

        if (cachedBrowser != null) {
            updateBrowserSize();
            cachedBrowser.setFocus(true);
            return;
        }

        setupBrowserIfReady();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        updateBrowserSize();
    }

    private void setupBrowserIfReady() {
        if (initializingBrowser || cachedBrowser != null) return;

        MCEFApi.Initialization initialization = MCEFApi.initialize();
        if (!initialization.isDone()) {
            initializingBrowser = true;
            initialization.getFuture().thenAccept(api -> {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    initializingBrowser = false;
                    if (client.currentScreen == this && cachedBrowser == null) {
                        createBrowserInstance(api);
                    }
                });
            }).exceptionally(ex -> {
                initializingBrowser = false;
                ex.printStackTrace();
                return null;
            });
            return;
        }

        try {
            createBrowserInstance(MCEFApi.getInstance());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void preload(MCEFApi api) {
        if (cachedBrowser != null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                if (cachedBrowser == null) {
                    Path htmlFile = extractBundledGui();
                    if (htmlFile != null) {
                        String url = "file:///" + htmlFile.toAbsolutePath().toString().replace("\\", "/");
                        try {
                            cachedBrowser = api.createBrowser(url, true);
                            if (cachedBrowser != null) {
                                cachedBrowser.resize(800, 600);
                                CefBrowser cefBrowser = cachedBrowser.getCefBrowser();
                                if (cefBrowser != null) {
                                    CefClient cefClient = cefBrowser.getClient();
                                    initRouter(cefClient);
                                    cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
                                        @Override
                                        public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                                            System.out.println("[WebGUI JS " + level + "] " + message + " (" + source + ":" + line + ")");
                                            return false;
                                        }
                                    });
                                }
                                System.out.println("[not_an_old_pulse] Web GUI preloaded in background: " + url);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void createBrowserInstance(MCEFApi api) {
        Path htmlFile = extractBundledGui();
        if (htmlFile == null) {
            return;
        }
        String url = "file:///" + htmlFile.toAbsolutePath().toString().replace("\\", "/");

        try {
            cachedBrowser = api.createBrowser(url, true);
            if (cachedBrowser != null) {
                cachedBrowser.resize(this.width, this.height);
                cachedBrowser.setFocus(true);

                CefBrowser cefBrowser = cachedBrowser.getCefBrowser();
                if (cefBrowser != null) {
                    CefClient client = cefBrowser.getClient();
                    initRouter(client);
                    client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                        @Override
                        public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                            System.out.println("[WebGUI JS " + level + "] " + message + " (" + source + ":" + line + ")");
                            return false;
                        }
                    });
                }
                System.out.println("[not_an_old_pulse] Web GUI loaded successfully: " + url);
            }
        } catch (Throwable t) {
            System.err.println("[not_an_old_pulse] Failed to create browser instance: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static synchronized void initRouter(CefClient client) {
        if (routerInitialized || client == null) return;

        messageRouter = CefMessageRouter.create();
        messageRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                try {
                    JsonObject json = JsonParser.parseString(request).getAsJsonObject();
                    String action = json.has("action") ? json.get("action").getAsString() : "";
                    JsonObject payload = json.has("payload") && json.get("payload").isJsonObject() ? json.getAsJsonObject("payload") : new JsonObject();

                    switch (action) {
                        case "close" -> {
                            MinecraftClient.getInstance().execute(() -> {
                                if (MinecraftClient.getInstance().currentScreen instanceof BrowserScreen) {
                                    MinecraftClient.getInstance().currentScreen.close();
                                }
                            });
                            callback.success("ok");
                            return true;
                        }
                        case "getConfig" -> {
                            Gson gson = new Gson();
                            String configJson = gson.toJson(ModConfig.getInstance().getValues());
                            callback.success(configJson);
                            return true;
                        }
                        case "setToggle" -> {
                            if (payload.has("key") && payload.has("value")) {
                                String key = payload.get("key").getAsString();
                                boolean value = payload.get("value").getAsBoolean();
                                ModConfig.getInstance().set(key, value);
                                System.out.println("[not_an_old_pulse] Module (via cefQuery) " + key + " set to " + value);
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "setModuleValue" -> {
                            if (payload.has("key") && payload.has("setting") && payload.has("value")) {
                                String key = payload.get("key").getAsString();
                                String setting = payload.get("setting").getAsString();
                                var elem = payload.get("value");
                                if (elem.isJsonPrimitive()) {
                                    var prim = elem.getAsJsonPrimitive();
                                    if (prim.isNumber()) {
                                        Number num = prim.getAsNumber();
                                        ModConfig.getInstance().set(key + "_" + setting, num);
                                        if ("timeChanger".equalsIgnoreCase(key) || "Time".equalsIgnoreCase(setting)) {
                                            ModConfig.getInstance().set("timeValue", num.longValue());
                                        } else if ("guiScaleMod".equalsIgnoreCase(key) || "Scale".equalsIgnoreCase(setting)) {
                                            ModConfig.getInstance().set("guiScale", num.intValue());
                                        } else if ("customFov".equalsIgnoreCase(key) || "FOV".equalsIgnoreCase(setting)) {
                                            ModConfig.getInstance().set("fovValue", num.floatValue());
                                        } else if ("fullBright".equalsIgnoreCase(key) || "Brightness".equalsIgnoreCase(setting)) {
                                            ModConfig.getInstance().set("brightnessValue", num.intValue());
                                        } else if ("chinaHat".equalsIgnoreCase(key)) {
                                            ModConfig.getInstance().set("chinaHatScale", num.floatValue());
                                        } else if ("motionBlur".equalsIgnoreCase(key) && "Strength".equalsIgnoreCase(setting)) {
                                            // Web GUI sends 10-99 %, MotionBlurRenderer reads 0.0-1.0
                                            ModConfig.getInstance().set("motionBlur_Strength", num.floatValue() / 100.0f);
                                        }
                                    } else if (prim.isBoolean()) {
                                        ModConfig.getInstance().set(key + "_" + setting, prim.getAsBoolean());
                                    } else if (prim.isString()) {
                                        ModConfig.getInstance().set(key + "_" + setting, prim.getAsString());
                                    }
                                }
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "setModuleColor" -> {
                            if (payload.has("key") && payload.has("color")) {
                                String key = payload.get("key").getAsString();
                                String color = payload.get("color").getAsString();
                                ModConfig.getInstance().set(key + "_color", color);
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "getFriends" -> {
                            Gson gson = new Gson();
                            callback.success(gson.toJson(ablivity.dev.not_an_old_pulse.client.social.FriendManager.getFriends()));
                            return true;
                        }
                        case "addFriend" -> {
                            if (payload.has("name")) {
                                ablivity.dev.not_an_old_pulse.client.social.FriendManager.addFriend(payload.get("name").getAsString());
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "removeFriend" -> {
                            if (payload.has("name")) {
                                ablivity.dev.not_an_old_pulse.client.social.FriendManager.removeFriend(payload.get("name").getAsString());
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "getWaypoints" -> {
                            Gson gson = new Gson();
                            callback.success(gson.toJson(ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.getWaypoints()));
                            return true;
                        }
                        case "addWaypoint" -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc.player != null) {
                                String name = payload.has("name") ? payload.get("name").getAsString() : "Waypoint " + (ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.getWaypoints().size() + 1);
                                String color = payload.has("color") ? payload.get("color").getAsString() : "#4a90e2";
                                String dim = mc.world != null ? mc.world.getRegistryKey().getValue().getPath() : "overworld";
                                ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.addWaypoint(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim, color);
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "deleteWaypoint" -> {
                            if (payload.has("name")) {
                                ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.deleteWaypoint(payload.get("name").getAsString());
                            }
                            callback.success("ok");
                            return true;
                        }
                        case "setWaypointVisible" -> {
                            if (payload.has("name") && payload.has("visible")) {
                                ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.setVisible(payload.get("name").getAsString(), payload.get("visible").getAsBoolean());
                            }
                            callback.success("ok");
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.failure(500, e.getMessage());
                    return true;
                }
                return false;
            }
        }, true);

        client.addMessageRouter(messageRouter);

        client.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture, boolean isRedirect) {
                String url = request.getURL();
                if (url != null && url.startsWith("mc://")) {
                    handleMcUrl(url);
                    return true;
                }
                return false;
            }
        });

        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                System.out.println("[WebGUI JS " + level + "] " + message + " (" + source + ":" + line + ")");
                return false;
            }
        });

        routerInitialized = true;
    }

    private static void handleMcUrl(String url) {
        try {
            URI uri = URI.create(url);
            String action = uri.getHost();
            if (action == null || action.isEmpty()) {
                String ssp = uri.getSchemeSpecificPart();
                action = ssp.contains("?") ? ssp.substring(0, ssp.indexOf("?")).replace("/", "") : ssp.replace("/", "");
            }
            String query = uri.getQuery();
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                }
            }

            switch (action.toLowerCase()) {
                case "close" -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().currentScreen instanceof BrowserScreen) {
                            MinecraftClient.getInstance().currentScreen.close();
                        }
                    });
                }
                case "settoggle" -> {
                    String key = params.get("key");
                    String valueStr = params.get("value");
                    if (key != null && valueStr != null) {
                        boolean value = Boolean.parseBoolean(valueStr);
                        ModConfig.getInstance().set(key, value);
                    }
                }
                case "setmodulevalue" -> {
                    String key = params.get("key");
                    String setting = params.get("setting");
                    String valueStr = params.get("value");
                    if (key != null && setting != null && valueStr != null) {
                        try {
                            double num = Double.parseDouble(valueStr);
                            ModConfig.getInstance().set(key + "_" + setting, num);
                            if ("timeChanger".equalsIgnoreCase(key) || "Time".equalsIgnoreCase(setting)) {
                                ModConfig.getInstance().set("timeValue", (long) num);
                            } else if ("guiScaleMod".equalsIgnoreCase(key) || "Scale".equalsIgnoreCase(setting)) {
                                ModConfig.getInstance().set("guiScale", (int) num);
                            } else if ("customFov".equalsIgnoreCase(key) || "FOV".equalsIgnoreCase(setting)) {
                                ModConfig.getInstance().set("fovValue", num);
                            }
                        } catch (NumberFormatException e) {
                            ModConfig.getInstance().set(key + "_" + setting, valueStr);
                        }
                    }
                }
                case "setmodulecolor" -> {
                    String key = params.get("key");
                    String color = params.get("color");
                    if (key != null && color != null) {
                        ModConfig.getInstance().set(key + "_color", color);
                    }
                }
                case "addfriend" -> {
                    String name = params.get("name");
                    if (name != null) {
                        ablivity.dev.not_an_old_pulse.client.social.FriendManager.addFriend(name);
                    }
                }
                case "removefriend" -> {
                    String name = params.get("name");
                    if (name != null) {
                        ablivity.dev.not_an_old_pulse.client.social.FriendManager.removeFriend(name);
                    }
                }
                case "addwaypoint" -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        String name = params.getOrDefault("name", "Waypoint");
                        String color = params.getOrDefault("color", "#4a90e2");
                        String dim = mc.world != null ? mc.world.getRegistryKey().getValue().getPath() : "overworld";
                        ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.addWaypoint(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim, color);
                    }
                }
                case "deletewaypoint" -> {
                    String name = params.get("name");
                    if (name != null) {
                        ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.deleteWaypoint(name);
                    }
                }
                case "setwaypointvisible" -> {
                    String name = params.get("name");
                    String visStr = params.get("visible");
                    if (name != null && visStr != null) {
                        ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.setVisible(name, Boolean.parseBoolean(visStr));
                    }
                }
                case "requeststate" -> {
                    // Handled below by pushStateToBrowser
                }
            }
            // Always push state to browser after a fallback mutation or request
            pushStateToBrowser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pushStateToBrowser() {
        if (cachedBrowser != null) {
            Gson gson = new Gson();
            String configJson = gson.toJson(ModConfig.getInstance().getValues());
            String waypointsJson = gson.toJson(ablivity.dev.not_an_old_pulse.client.waypoint.WaypointManager.getWaypoints());
            String friendsJson = gson.toJson(ablivity.dev.not_an_old_pulse.client.social.FriendManager.getFriends());
            
            String js = String.format("if (typeof window.applyState === 'function') { window.applyState(%s, %s, %s); }", configJson, waypointsJson, friendsJson);
            if (cachedBrowser.getCefBrowser() != null) {
                cachedBrowser.getCefBrowser().executeJavaScript(js, "", 0);
            }
        }
    }

    private static Path extractBundledGui() {
        try {
            Path target = FabricLoader.getInstance().getConfigDir()
                    .resolve("not_an_old_pulse")
                    .resolve("click_gui.html");
            Files.createDirectories(target.getParent());

            try (InputStream in = BrowserScreen.class.getResourceAsStream(RESOURCE_PATH)) {
                if (in == null) {
                    System.err.println("[not_an_old_pulse] Bundled GUI resource not found: " + RESOURCE_PATH);
                    return null;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (cachedBrowser == null) {
            // Background backdrop while loading MCEF
            context.fill(0, 0, width, height, 0xC0141414);

            MCEFApi.Initialization init = MCEFApi.initialize();
            String stageText = "Loading Chromium engine: " + init.getStage().name();
            if (init.getPercentage() >= 0) {
                stageText += " (" + (int) init.getPercentage() + "%)";
            }
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawCenteredTextWithShadow(context, textRenderer, "not an old pulse (web)", width / 2, height / 2 - 12, 0xFF4A90E2);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawCenteredTextWithShadow(context, textRenderer, stageText, width / 2, height / 2 + 4, 0xFFAAAAAA);

            if (init.isDone() && !initializingBrowser) {
                setupBrowserIfReady();
            }
            return;
        }

        GpuTextureView textureView = cachedBrowser.getTextureView();
        if (textureView == null) {
            context.fill(0, 0, width, height, 0x60000000);
            ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawCenteredTextWithShadow(context, textRenderer, "Rendering Web GUI...", width / 2, height / 2, 0xFFAAAAAA);
            return;
        }

        if (cachedSampler == null) {
            cachedSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
        ((DrawContextAccessor) context).callDrawTexturedQuad(
            RenderPipelines.GUI_TEXTURED,
            textureView,
            cachedSampler,
            0, 0, width, height,
            0.0f, 1.0f, 0.0f, 1.0f,
            -1
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (cachedBrowser != null) {
            double sx = getScaleX();
            double sy = getScaleY();
            Click scaledClick = new Click(click.x() * sx, click.y() * sy, click.buttonInfo());
            cachedBrowser.onMouseClicked(scaledClick, doubled);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (cachedBrowser != null) {
            double sx = getScaleX();
            double sy = getScaleY();
            Click scaledClick = new Click(click.x() * sx, click.y() * sy, click.buttonInfo());
            cachedBrowser.onMouseReleased(scaledClick);
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (cachedBrowser != null) {
            double sx = getScaleX();
            double sy = getScaleY();
            cachedBrowser.onMouseMoved((int) Math.round(mouseX * sx), (int) Math.round(mouseY * sy));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (cachedBrowser != null) {
            double sx = getScaleX();
            double sy = getScaleY();
            cachedBrowser.onMouseScrolled((int) Math.round(mouseX * sx), (int) Math.round(mouseY * sy), verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (cachedBrowser != null) {
            cachedBrowser.onKeyPressed(input);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (cachedBrowser != null) {
            cachedBrowser.onKeyReleased(input);
            return true;
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (cachedBrowser != null) {
            cachedBrowser.onCharTyped(input);
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (cachedBrowser != null) {
            cachedBrowser.setFocus(false);
        }
        super.close();
    }

    @Override
    public void onDisplayed() {
        super.onDisplayed();
        if (cachedBrowser != null) {
            cachedBrowser.setFocus(true);
        }
    }
}
