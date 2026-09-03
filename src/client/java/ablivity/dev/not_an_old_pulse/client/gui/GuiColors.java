package ablivity.dev.not_an_old_pulse.client.gui;

/**
 * Every color here is fully opaque (alpha = 0xFF). The GUI never relies on
 * blending/translucency for its base look — the only place alpha is animated
 * is the dynamic tooltip's appear/disappear fade, which is a transition
 * effect rather than a persistent glass/blur look.
 */
public final class GuiColors {
    private GuiColors() {}

    // Core palette, exactly as specified.
    public static final int WINDOW_BG = 0xFF1C1C1C;
    public static final int MODULE_BG = 0xFF2A2A2A;
    public static final int ACCENT = 0xFF4A90E2;

    // Derived / supporting colors, all opaque.
    public static final int SIDEBAR_BG = 0xFF202020;
    public static final int SIDEBAR_SHADOW = 0xFF0E0E0E;
    public static final int HEADER_BG = 0xFF202020;
    public static final int TAB_TRACK_BG = 0xFF242424;
    public static final int TAB_ACTIVE_BG = 0xFF3A3A3A;
    public static final int MODULE_BG_HOVER = 0xFF323232;
    public static final int ACCENT_HOVER = 0xFF5C9DE8;
    public static final int SCROLLBAR_TRACK = 0xFF242424;
    public static final int SCROLLBAR_THUMB = 0xFF444444;

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFA5A5A5;
    public static final int TEXT_TAB_ACTIVE = 0xFFFFFFFF;
    public static final int TEXT_TAB_INACTIVE = 0xFF8A8A8A;

    public static final int CLOSE_BUTTON = 0xFFE0473F;
    public static final int CLOSE_BUTTON_HOVER = 0xFFEB645C;

    public static final int TOGGLE_OFF = MODULE_BG;
    public static final int TOGGLE_ON = ACCENT;
    public static final int TOGGLE_THUMB = 0xFFF2F2F2;

    public static final int THEME_ICON = 0xFFD0D0D0;
    public static final int THEME_ICON_HOVER = 0xFFFFFFFF;

    public static final int TOOLTIP_BG = MODULE_BG;

    /** Only used for the tooltip fade transition — see class javadoc. */
    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}

