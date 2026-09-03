package ablivity.dev.not_an_old_pulse.client.gui.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import ablivity.dev.not_an_old_pulse.client.gui.GuiColors;
import ablivity.dev.not_an_old_pulse.client.gui.RoundedRect;
import ablivity.dev.not_an_old_pulse.client.gui.animation.AnimatedValue;
import ablivity.dev.not_an_old_pulse.client.gui.widget.CloseButton;
import ablivity.dev.not_an_old_pulse.client.gui.widget.SettingEntry;
import ablivity.dev.not_an_old_pulse.client.gui.widget.SidebarButton;
import ablivity.dev.not_an_old_pulse.client.gui.widget.TabButton;
import ablivity.dev.not_an_old_pulse.client.gui.widget.ThemeToggleButton;
import ablivity.dev.not_an_old_pulse.client.gui.widget.Tooltip;
import ablivity.dev.not_an_old_pulse.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The main ClickGUI. Fully opaque, DrawContext-only rendering; every
 * animation is driven by real elapsed time (see {@link #render}) rather
 * than frame count, so speed doesn't change with FPS.
 */
public class NotAnOldPulseScreen extends Screen {
    private static final String TITLE_TEXT = "not an old pulse";

    private static final int SIDEBAR_WIDTH = 64;
    private static final int SIDEBAR_ICON_SIZE = 44;
    private static final int SIDEBAR_ICON_GAP = 14;
    private static final int SIDEBAR_PADDING = 16;
    private static final int WINDOW_GAP = 16;

    private static final int MAIN_WIDTH = 560;
    private static final int MAIN_HEIGHT = 440;
    private static final int MAIN_PADDING = 20;
    private static final int MAIN_RADIUS = 12;

    private static final int CONTROL_SIZE = 16;
    private static final int TAB_WIDTH = 140;
    private static final int TAB_HEIGHT = 30;
    private static final int TAB_RADIUS = 8;
    private static final int TAB_TRACK_PADDING = 4;

    private static final int GRID_COLUMNS = 2;
    private static final int GRID_COLUMN_GAP = 16;
    private static final int GRID_ROW_HEIGHT = 40;
    private static final int GRID_ROW_GAP = 10;
    private static final int SCROLLBAR_WIDTH = 4;

    private static final float MAX_DELTA_SECONDS = 0.1f;

    private final List<SidebarButton> sidebarButtons = new ArrayList<>();
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final List<List<SettingEntry>> tabContents = new ArrayList<>();
    private final CloseButton closeButton = new CloseButton();
    private final ThemeToggleButton themeToggleButton = new ThemeToggleButton();
    private final Tooltip tooltip = new Tooltip();
    private final AnimatedValue tabIndicatorX = new AnimatedValue(0f, 0.35f);

    private int sidebarX, sidebarY, sidebarHeight;
    private int mainX, mainY;
    private int gridX, gridY, gridWidth, gridViewportHeight;
    private int currentTab = 0;

    private float scrollOffset = 0f;
    private float maxScroll = 0f;

    public NotAnOldPulseScreen() {
        super(Text.literal(TITLE_TEXT));
    }

    @Override
    protected void init() {
        buildSidebar();
        buildTabs();
        buildModules();
        layout();
    }

    private void buildSidebar() {
        sidebarButtons.clear();
        sidebarButtons.add(new SidebarButton(SidebarButton.Icon.SETTINGS, true));
        sidebarButtons.add(new SidebarButton(SidebarButton.Icon.VISUALS, false));
        sidebarButtons.add(new SidebarButton(SidebarButton.Icon.SOCIAL, false));
        sidebarButtons.add(new SidebarButton(SidebarButton.Icon.CALENDAR, false));
    }

    private void buildTabs() {
        tabButtons.clear();
        tabButtons.add(new TabButton("Visuals"));
        tabButtons.add(new TabButton("HUD"));
        tabButtons.add(new TabButton("Utilities"));
        tabButtons.get(currentTab).setActive(true);
    }

    private void buildModules() {
        tabContents.clear();
        ModConfig config = ModConfig.getInstance();

        // ── Visuals ──
        List<SettingEntry> visuals = new ArrayList<>();
        String[][] visualModules = {
            {"Full Bright", "fullBright"},
            {"Custom FOV", "customFov"},
            {"Motion Blur", "motionBlur"},
            {"China Hat", "chinaHat"},
            {"Player Trail", "playerTrail"},
            {"Jump Circles", "jumpCircles"},
            {"No Hurt Cam", "noHurtCam"},
            {"Aspect Ratio", "aspectRatio"},
            {"Crosshair", "crosshair"},
            {"Custom Hand", "customHand"},
            {"Hit Color", "hitColor"},
            {"Hit Particles", "particles"},
            {"Hit Sounds", "hitSounds"},
            {"Render Tweaks", "renderTweaks"},
        };
        for (String[] entry : visualModules) {
            visuals.add(new SettingEntry(entry[0], entry[1], config.get(entry[1])));
        }
        tabContents.add(visuals);

        // ── HUD ──
        List<SettingEntry> hud = new ArrayList<>();
        String[][] hudModules = {
            {"Dynamic Island", "dynamicIsland"},
            {"Target HUD", "targetHud"},
            {"Player Info Card", "playerInfo"},
            {"Cooldowns Card", "cooldowns"},
            {"Potions Card", "potions"},
            {"FPS Display", "fpsDisplay"},
            {"CPS Display", "cpsDisplay"},
            {"Ping Display", "pingDisplay"},
            {"Coordinates", "coords"},
            {"Speedometer", "speedometer"},
            {"Armor Status", "armorStatus"},
            {"Keystrokes", "keystrokes"},
        };
        for (String[] entry : hudModules) {
            hud.add(new SettingEntry(entry[0], entry[1], config.get(entry[1])));
        }
        tabContents.add(hud);

        // ── Utilities ──
        List<SettingEntry> utilities = new ArrayList<>();
        String[][] utilityModules = {
            {"Auto Sprint", "autoSprint"},
            {"Friend Protection", "friendProtection"},
            {"Time Changer", "timeChanger"},
        };
        for (String[] entry : utilityModules) {
            utilities.add(new SettingEntry(entry[0], entry[1], config.get(entry[1])));
        }
        tabContents.add(utilities);
    }

    private void layout() {
        int centerX = width / 2;
        int centerY = height / 2;

        sidebarHeight = SIDEBAR_PADDING * 2 + sidebarButtons.size() * SIDEBAR_ICON_SIZE
            + (sidebarButtons.size() - 1) * SIDEBAR_ICON_GAP;

        int totalWidth = SIDEBAR_WIDTH + WINDOW_GAP + MAIN_WIDTH;
        sidebarX = centerX - totalWidth / 2;
        sidebarY = centerY - sidebarHeight / 2;
        mainX = sidebarX + SIDEBAR_WIDTH + WINDOW_GAP;
        mainY = centerY - MAIN_HEIGHT / 2;

        int iconX = sidebarX + (SIDEBAR_WIDTH - SIDEBAR_ICON_SIZE) / 2;
        int iconY = sidebarY + SIDEBAR_PADDING;
        for (SidebarButton button : sidebarButtons) {
            button.setBounds(iconX, iconY, SIDEBAR_ICON_SIZE);
            iconY += SIDEBAR_ICON_SIZE + SIDEBAR_ICON_GAP;
        }

        closeButton.setBounds(mainX + MAIN_PADDING - 4, mainY + 16, CONTROL_SIZE);
        themeToggleButton.setBounds(mainX + MAIN_WIDTH - MAIN_PADDING + 4 - CONTROL_SIZE, mainY + 16, CONTROL_SIZE);

        int tabsTotalWidth = tabButtons.size() * TAB_WIDTH;
        int tabStartX = mainX + (MAIN_WIDTH - tabsTotalWidth) / 2;
        int tabY = mainY + 16 + CONTROL_SIZE + 12;
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setBounds(tabStartX + i * TAB_WIDTH, tabY, TAB_WIDTH, TAB_HEIGHT);
        }
        tabIndicatorX.setImmediate(tabButtons.get(currentTab).x);

        gridX = mainX + MAIN_PADDING;
        gridY = tabY + TAB_HEIGHT + 16;
        gridWidth = MAIN_WIDTH - MAIN_PADDING * 2;
        gridViewportHeight = mainY + MAIN_HEIGHT - MAIN_PADDING - gridY;

        for (List<SettingEntry> entries : tabContents) {
            layoutModuleGrid(entries);
        }
        updateScrollBounds();
    }

    private void layoutModuleGrid(List<SettingEntry> entries) {
        int columnWidth = (gridWidth - GRID_COLUMN_GAP * (GRID_COLUMNS - 1)) / GRID_COLUMNS;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            int x = gridX + col * (columnWidth + GRID_COLUMN_GAP);
            int y = gridY + row * (GRID_ROW_HEIGHT + GRID_ROW_GAP);
            entries.get(i).setBounds(x, y, columnWidth, GRID_ROW_HEIGHT);
        }
    }

    private void updateScrollBounds() {
        List<SettingEntry> entries = tabContents.get(currentTab);
        int rows = (entries.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
        int contentHeight = rows == 0 ? 0 : rows * GRID_ROW_HEIGHT + (rows - 1) * GRID_ROW_GAP;
        maxScroll = Math.max(0, contentHeight - gridViewportHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    private void switchTab(int index) {
        if (index == currentTab) return;
        tabButtons.get(currentTab).setActive(false);
        currentTab = index;
        tabButtons.get(currentTab).setActive(true);
        tabIndicatorX.setTarget(tabButtons.get(currentTab).x);
        scrollOffset = 0f;
        updateScrollBounds();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaSeconds = MathHelper.clamp(delta / 20f, 0f, MAX_DELTA_SECONDS);

        renderSidebar(context, mouseX, mouseY, deltaSeconds);
        renderMainWindow(context, mouseX, mouseY, deltaSeconds);

        tooltip.updateAnimation(deltaSeconds);
        tooltip.render(context, textRenderer);
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float deltaSeconds) {
        RoundedRect.fill(context, sidebarX + 3, sidebarY + 4, SIDEBAR_WIDTH, sidebarHeight, 14, GuiColors.SIDEBAR_SHADOW);
        RoundedRect.fill(context, sidebarX, sidebarY, SIDEBAR_WIDTH, sidebarHeight, 14, GuiColors.SIDEBAR_BG);

        for (SidebarButton button : sidebarButtons) {
            boolean hovered = button.isMouseOver(mouseX, mouseY);
            button.updateAnimation(deltaSeconds, hovered);
            button.render(context);
        }
    }

    private void renderMainWindow(DrawContext context, int mouseX, int mouseY, float deltaSeconds) {
        RoundedRect.fill(context, mainX, mainY, MAIN_WIDTH, MAIN_HEIGHT, MAIN_RADIUS, GuiColors.WINDOW_BG);

        boolean closeHovered = closeButton.isMouseOver(mouseX, mouseY);
        closeButton.updateAnimation(deltaSeconds, closeHovered);
        closeButton.render(context);

        boolean themeHovered = themeToggleButton.isMouseOver(mouseX, mouseY);
        themeToggleButton.updateAnimation(deltaSeconds, themeHovered);
        themeToggleButton.render(context, GuiColors.WINDOW_BG);

        ablivity.dev.not_an_old_pulse.client.font.FontUtil.drawCenteredTextWithShadow(context, textRenderer, TITLE_TEXT, mainX + MAIN_WIDTH / 2,
            mainY + 16 + (CONTROL_SIZE - 8) / 2, GuiColors.TEXT_PRIMARY);

        renderTabs(context, mouseX, mouseY, deltaSeconds);
        renderModuleGrid(context, mouseX, mouseY, deltaSeconds);
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY, float deltaSeconds) {
        TabButton firstTab = tabButtons.get(0);
        TabButton lastTab = tabButtons.get(tabButtons.size() - 1);
        int trackX = firstTab.x - TAB_TRACK_PADDING;
        int trackWidth = (lastTab.x + lastTab.width) - firstTab.x + TAB_TRACK_PADDING * 2;
        RoundedRect.fill(context, trackX, firstTab.y - TAB_TRACK_PADDING, trackWidth,
            TAB_HEIGHT + TAB_TRACK_PADDING * 2, TAB_RADIUS + TAB_TRACK_PADDING, GuiColors.TAB_TRACK_BG);

        tabIndicatorX.update(deltaSeconds);
        TabButton activeTab = tabButtons.get(currentTab);
        RoundedRect.fill(context, Math.round(tabIndicatorX.get()), activeTab.y, TAB_WIDTH, TAB_HEIGHT,
            TAB_RADIUS, GuiColors.TAB_ACTIVE_BG);

        for (int i = 0; i < tabButtons.size(); i++) {
            TabButton tab = tabButtons.get(i);
            boolean hovered = tab.isMouseOver(mouseX, mouseY);
            tab.updateAnimation(deltaSeconds, hovered);
            tab.render(context, textRenderer);
        }
    }

    private void renderModuleGrid(DrawContext context, int mouseX, int mouseY, float deltaSeconds) {
        List<SettingEntry> entries = tabContents.get(currentTab);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No modules in this tab yet",
                mainX + MAIN_WIDTH / 2, gridY + gridViewportHeight / 2 - 4, GuiColors.TEXT_SECONDARY);
            return;
        }

        boolean scrollable = maxScroll > 0f;
        int viewportBottom = gridY + gridViewportHeight;
        if (scrollable) {
            context.enableScissor(gridX, gridY, gridX + gridWidth, viewportBottom);
        }

        int scrollInt = Math.round(scrollOffset);
        String hoveredLabel = null;

        for (SettingEntry entry : entries) {
            int renderY = entry.y - scrollInt;
            boolean withinViewport = renderY + entry.height >= gridY && renderY <= viewportBottom;
            boolean hovered = withinViewport && mouseX >= entry.x && mouseX < entry.x + entry.width
                && mouseY >= renderY && mouseY < renderY + entry.height;

            entry.updateAnimation(deltaSeconds, hovered);

            if (!withinViewport) continue;

            int actualY = entry.y;
            entry.y = renderY;
            entry.render(context, textRenderer);
            entry.y = actualY;

            if (hovered) {
                hoveredLabel = entry.label;
            }
        }

        if (scrollable) {
            context.disableScissor();
            renderScrollbar(context);
        }

        if (hoveredLabel != null) {
            tooltip.setAnchor(mainX + MAIN_WIDTH / 2, mainY - 8);
            tooltip.show(hoveredLabel);
        } else {
            tooltip.hide();
        }
    }

    private void renderScrollbar(DrawContext context) {
        int trackX = gridX + gridWidth - SCROLLBAR_WIDTH;
        RoundedRect.fill(context, trackX, gridY, SCROLLBAR_WIDTH, gridViewportHeight,
            SCROLLBAR_WIDTH / 2, GuiColors.SCROLLBAR_TRACK);

        float contentHeight = gridViewportHeight + maxScroll;
        int thumbHeight = Math.max(20, Math.round(gridViewportHeight * (gridViewportHeight / contentHeight)));
        int thumbY = gridY + Math.round((scrollOffset / maxScroll) * (gridViewportHeight - thumbHeight));
        RoundedRect.fill(context, trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight,
            SCROLLBAR_WIDTH / 2, GuiColors.SCROLLBAR_THUMB);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        int mx = (int) click.x();
        int my = (int) click.y();

        if (closeButton.isMouseOver(mx, my)) {
            close();
            return true;
        }

        if (themeToggleButton.isMouseOver(mx, my)) {
            themeToggleButton.toggle();
            return true;
        }

        for (int i = 0; i < sidebarButtons.size(); i++) {
            SidebarButton sidebarButton = sidebarButtons.get(i);
            if (sidebarButton.isMouseOver(mx, my)) {
                for (SidebarButton other : sidebarButtons) other.setActive(false);
                sidebarButton.setActive(true);
                return true;
            }
        }

        for (int i = 0; i < tabButtons.size(); i++) {
            if (tabButtons.get(i).isMouseOver(mx, my)) {
                switchTab(i);
                return true;
            }
        }

        int scrollInt = Math.round(scrollOffset);
        int viewportBottom = gridY + gridViewportHeight;
        for (SettingEntry entry : tabContents.get(currentTab)) {
            int renderY = entry.y - scrollInt;
            if (renderY + entry.height < gridY || renderY > viewportBottom) continue;

            int actualY = entry.y;
            entry.y = renderY;
            boolean overToggle = entry.isMouseOverToggle(mx, my);
            entry.y = actualY;

            if (overToggle) {
                entry.toggle();
                ModConfig.getInstance().set(entry.configKey, entry.getValue());
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0f) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        scrollOffset = MathHelper.clamp(scrollOffset - (float) verticalAmount * GRID_ROW_HEIGHT * 0.5f, 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        ModConfig.getInstance().save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

