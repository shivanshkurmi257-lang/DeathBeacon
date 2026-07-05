package com.deathbeacon.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * All user-facing settings (Feature 10). Backed by Cloth Config's AutoConfig,
 * which handles loading/saving config/deathbeacon/settings.json for us.
 */
@Config(name = "deathbeacon")
public class DeathBeaconConfig implements ConfigData {

    // ---------------- MASTER ----------------
    @ConfigEntry.Category("master")
    @ConfigEntry.Gui.Tooltip
    public boolean modEnabled = true;

    // ---------------- WAYPOINTS ----------------
    @ConfigEntry.Category("waypoints")
    public boolean enableWaypoints = true;

    @ConfigEntry.Category("waypoints")
    public boolean enableDeaths = true;

    @ConfigEntry.Category("waypoints")
    public boolean enableFavorites = true;

    @ConfigEntry.Category("waypoints")
    public boolean enableGlobal = true;

    // ---------------- RENDERING ----------------
    @ConfigEntry.Category("rendering")
    public boolean enableBeams = true;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.BoundedDiscrete(min = 16, max = 320)
    public int beamHeight = 256;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int beamWidth = 3;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int beamOpacity = 65;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.BoundedDiscrete(min = 16, max = 4096)
    public int beamRenderDistance = 512;

    @ConfigEntry.Category("rendering")
    public boolean beamAnimation = true;

    // ---------------- HUD ----------------
    @ConfigEntry.Category("hud")
    public boolean enableLocatorHud = true;

    @ConfigEntry.Category("hud")
    public boolean enableWaypointListHud = true;

    public enum HudPosition { TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    @ConfigEntry.Category("hud")
    public HudPosition listHudPosition = HudPosition.TOP_LEFT;

    @ConfigEntry.Category("hud")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
    public int hudScale = 100;

    @ConfigEntry.Category("hud")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hudOpacity = 80;

    @ConfigEntry.Category("hud")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
    public int hudFontSize = 100;

    @ConfigEntry.Category("hud")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    public int hudMaxEntries = 20;

    // ---------------- DEATH ----------------
    @ConfigEntry.Category("death")
    public boolean autoSaveDeaths = true;

    @ConfigEntry.Category("death")
    @ConfigEntry.ColorPicker
    public int deathColor = 0xFFFF0000;

    @ConfigEntry.Category("death")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 500)
    public int maxDeaths = 0; // 0 = unlimited (spec default: no automatic deletion)

    @ConfigEntry.Category("death")
    public boolean deathBeamEnabled = true;
}
