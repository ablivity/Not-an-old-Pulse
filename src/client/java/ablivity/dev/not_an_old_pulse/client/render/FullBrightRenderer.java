package ablivity.dev.not_an_old_pulse.client.render;

/**
 * DISABLED for the 1.21.11 port — same reason as ChinaHatRenderer: it hooked
 * WorldRenderEvents (START/END), which Fabric API no longer provides as of
 * 1.21.9+. See ChinaHatRenderer.java for the full explanation and the
 * upstream link. This one was already only a placeholder (no real gamma
 * manipulation was implemented yet), so there's nothing being lost here
 * beyond the hook itself.
 */
public class FullBrightRenderer {
    public static void register() {
        // no-op until rewritten as a WorldRenderer / GameRenderer mixin
    }
}

