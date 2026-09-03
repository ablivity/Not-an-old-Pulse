package ablivity.dev.not_an_old_pulse.client.render;

import net.minecraft.client.util.math.MatrixStack;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

public final class ViewmodelHandler {

    public static void applyTransformations(MatrixStack matrices, boolean isOffhand) {
        if (!ModConfig.getInstance().get("customHand", false)) return;

        double x = ModConfig.getInstance().getDouble("customHand_X", 0.0);
        double y = ModConfig.getInstance().getDouble("customHand_Y", 0.0);
        double z = ModConfig.getInstance().getDouble("customHand_Z", 0.0);
        float scale = ModConfig.getInstance().getFloat("customHand_Scale", 1.0f);

        if (isOffhand) x = -x;

        matrices.translate(x, y, z);
        matrices.scale(Math.max(0.2f, scale), Math.max(0.2f, scale), Math.max(0.2f, scale));
    }
}
