package com.deathbeacon.gui;

import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.data.Waypoint;
import com.deathbeacon.data.WaypointCategory;
import com.deathbeacon.storage.WaypointManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.player.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 7 - main waypoint manager GUI. Visually modeled on the layout in
 * the reference screenshots: search bar up top, category tabs, a scrollable
 * list of rows (name/distance + show/favorite/edit/delete buttons), and
 * Add / Settings / Done across the bottom.
 */
public class WaypointListScreen extends Screen {

    private static final int PANEL_MARGIN = 24;
    private static final int ROW_HEIGHT = 22;

    private WaypointCategory activeCategory;
    private TextFieldWidget searchBox;
    private WaypointEntryList entryList;
    private String sortMode = "distance"; // distance | name

    public WaypointListScreen(WaypointCategory startCategory) {
        super(Text.literal("Waypoints"));
        this.activeCategory = startCategory;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, this.width - 40);
        int left = (this.width - panelWidth) / 2;
        int top = 24;

        searchBox = new TextFieldWidget(this.textRenderer, left + 8, top + 20, panelWidth - 16, 16, Text.literal("Search"));
        searchBox.setPlaceholder(Text.literal("Search waypoints..."));
        searchBox.setChangedListener(s -> refreshList());
        addDrawableChild(searchBox);

        int tabWidth = (panelWidth - 16) / 4;
        int tabY = top + 42;
        addTabButton(left + 8, tabY, tabWidth, WaypointCategory.WAYPOINTS);
        addTabButton(left + 8 + tabWidth, tabY, tabWidth, WaypointCategory.DEATHS);
        addTabButton(left + 8 + tabWidth * 2, tabY, tabWidth, WaypointCategory.FAVORITES);
        addTabButton(left + 8 + tabWidth * 3, tabY, tabWidth, WaypointCategory.GLOBAL);

        int listTop = tabY + 24;
        int listBottom = this.height - 40;
        entryList = new WaypointEntryList(this.client, panelWidth, listBottom - listTop, listTop, ROW_HEIGHT, left);
        addDrawableChild(entryList);

        int bottomY = this.height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            boolean global = activeCategory == WaypointCategory.GLOBAL;
            this.client.setScreen(EditWaypointScreen.createNew(this, global));
        }).dimensions(left + 8, bottomY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Waypoint Mod Settings"), b -> {
            this.client.setScreen(AutoConfig.getConfigScreen(DeathBeaconConfig.class, this).get());
        }).dimensions(left + panelWidth / 2 - 90, bottomY, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                .dimensions(left + panelWidth - 108, bottomY, 100, 20).build());

        refreshList();
    }

    private void addTabButton(int x, int y, int w, WaypointCategory category) {
        addDrawableChild(ButtonWidget.builder(Text.literal(category.label), b -> {
            activeCategory = category;
            refreshList();
        }).dimensions(x, y, w - 2, 20).build());
    }

    private void refreshList() {
        if (entryList == null) return;
        entryList.children().clear();

        String query = searchBox != null ? searchBox.getText().toLowerCase() : "";
        List<Waypoint> source = WaypointManager.get().byCategory(activeCategory);

        ClientPlayerEntity player = this.client.player;

        List<Waypoint> filtered = source.stream()
                .filter(w -> query.isEmpty() || w.displayName().toLowerCase().contains(query))
                .sorted(sortMode.equals("name")
                        ? Comparator.comparing(Waypoint::displayName, String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparingDouble(w -> player == null ? 0 : distSq(player, w)))
                .collect(Collectors.toList());

        for (Waypoint w : filtered) {
            double dist = player == null ? 0 : Math.sqrt(distSq(player, w));
            entryList.children().add(new WaypointEntry(w, (int) Math.round(dist)));
        }
    }

    private static double distSq(ClientPlayerEntity player, Waypoint w) {
        double dx = w.x - player.getX();
        double dy = w.y - player.getY();
        double dz = w.z - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        int panelWidth = Math.min(360, this.width - 40);
        int left = (this.width - panelWidth) / 2;

        // Dark translucent panel background, vanilla-styled border - matches the
        // reference screenshots' look for the main waypoint window.
        ctx.fill(left, 16, left + panelWidth, this.height - 20, 0xC0101010);
        ctx.drawBorder(left, 16, panelWidth, this.height - 36, 0xFF3A3A3A);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title.getString(), this.width / 2, 20, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    /** Scrollable list of waypoint rows (Xaero-style: name/distance + 4 icon buttons). */
    private class WaypointEntryList extends ElementListWidget<WaypointEntry> {

        private final int panelLeft;

        public WaypointEntryList(net.minecraft.client.MinecraftClient client, int panelWidth, int height,
