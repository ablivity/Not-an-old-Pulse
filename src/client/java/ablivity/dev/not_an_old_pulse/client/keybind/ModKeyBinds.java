package ablivity.dev.not_an_old_pulse.client.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ablivity.dev.not_an_old_pulse.client.gui.browser.BrowserScreen;
import ablivity.dev.not_an_old_pulse.client.gui.screen.NotAnOldPulseScreen;

public class ModKeyBinds {
    // KeyBinding.Category is a record as of 1.21.9+ — build one from an
    // Identifier instead of the old raw translation-key string.
    public static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("not_an_old_pulse", "main"));
    public static KeyBinding openGui;
    public static KeyBinding openWebGui;

    public static void register() {
        openWebGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.not_an_old_pulse.open_web_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            CATEGORY
        ));

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.not_an_old_pulse.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openWebGui.wasPressed() || openGui.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new BrowserScreen());
                }
            }
        });
    }
}

