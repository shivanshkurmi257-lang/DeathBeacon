package com.deathbeacon.keybind;

import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.data.WaypointCategory;
import com.deathbeacon.gui.EditWaypointScreen;
import com.deathbeacon.gui.WaypointListScreen;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {

    public static KeyBinding openGui;
    public static KeyBinding createWaypoint;
    public static KeyBinding toggleBeams;
    public static KeyBinding toggleHud;
    public static KeyBinding openDeaths;
    public static KeyBinding openFavorites;

    private static final String CATEGORY = "key.categories.deathbeacon";

    private KeyBindings() {
    }

    public static void register() {
        openGui = register("key.deathbeacon.open_gui", GLFW.GLFW_KEY_M);
        createWaypoint = register("key.deathbeacon.create_waypoint", GLFW.GLFW_KEY_N);
        toggleBeams = register("key.deathbeacon.toggle_beams", GLFW.GLFW_KEY_B);
        toggleHud = register("key.deathbeacon.toggle_hud", GLFW.GLFW_KEY_H);
        openDeaths = register("key.deathbeacon.open_deaths", InputUtil.UNKNOWN_KEY.getCode());
        openFavorites = register("key.deathbeacon.open_favorites", InputUtil.UNKNOWN_KEY.getCode());

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::onTick);
    }

    private static KeyBinding register(String translationKey, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                defaultKey,
                CATEGORY
        ));
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        while (openGui.wasPressed()) {
            client.setScreen(new WaypointListScreen(WaypointCategory.WAYPOINTS));
        }
        while (createWaypoint.wasPressed()) {
            client.setScreen(EditWaypointScreen.createNew(null));
        }
        while (toggleBeams.wasPressed()) {
            DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
            cfg.enableBeams = !cfg.enableBeams;
            AutoConfig.getConfigHolder(DeathBeaconConfig.class).save();
        }
        while (toggleHud.wasPressed()) {
            DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
            cfg.enableLocatorHud = !cfg.enableLocatorHud;
            cfg.enableWaypointListHud = !cfg.enableWaypointListHud;
            AutoConfig.getConfigHolder(DeathBeaconConfig.class).save();
        }
        while (openDeaths.wasPressed()) {
            client.setScreen(new WaypointListScreen(WaypointCategory.DEATHS));
        }
        while (openFavorites.wasPressed()) {
            client.setScreen(new WaypointListScreen(WaypointCategory.FAVORITES));
        }
    }
}
