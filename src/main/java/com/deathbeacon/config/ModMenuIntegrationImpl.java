package com.deathbeacon.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

/**
 * Feature 16 - Mod Menu support. Opens the Cloth-Config-generated settings
 * screen (the same one the in-game "Waypoint Mod Settings" button opens).
 */
public class ModMenuIntegrationImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(DeathBeaconConfig.class, parent).get();
    }
}
