package com.deathbeacon;

import com.deathbeacon.command.DeathBeaconCommand;
import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.keybind.KeyBindings;
import com.deathbeacon.render.BeamRenderer;
import com.deathbeacon.render.LocatorHud;
import com.deathbeacon.render.WaypointLabelRenderer;
import com.deathbeacon.render.WaypointListHud;
import com.deathbeacon.storage.WaypointManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DeathBeacon - client-only Xaero-style waypoint & death tracker.
 *
 * Entry point registered in fabric.mod.json under "entrypoints" -> "client".
 */
public class DeathBeaconMod implements ClientModInitializer {

    public static final String MOD_ID = "deathbeacon";

    /**
     * Client-side death detection has no dedicated "player died" event in
     * Fabric API, so - same trick most client-side death-tracking mods use -
     * we hook the moment the vanilla DeathScreen opens, which only happens
     * right after the local player dies. We guard against re-firing on the
     * same death with a per-session UUID set, since a screen can technically
     * be re-shown (e.g. resizing the window) without a new death occurring.
     */
    private final Set<UUID> handledDeathScreens = new HashSet<>();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(DeathBeaconConfig.class, GsonConfigSerializer::new);

        WaypointManager.get().onWorldChange("unknown");

        KeyBindings.register();

        BeamRenderer.register();
        WaypointLabelRenderer.register();
        LocatorHud.register();
        WaypointListHud.register();

        ClientCommandRegistrationCallback.EVENT.register(DeathBeaconCommand::register);

        // Re-scope storage to the correct world/server whenever the player joins.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WaypointManager.get().onWorldChange(resolveWorldKey(client));
        });

        // Death detection via the DeathScreen hook described above.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DeathScreen) {
                handlePlayerDeath(client);
            }
        });
    }

    private void handlePlayerDeath(MinecraftClient client) {
        DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
        if (!cfg.modEnabled || !cfg.autoSaveDeaths) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        // De-dupe: tie the guard key to position+tick rather than a random UUID,
        // since a fresh DeathScreen instance is created each time it opens.
        UUID key = UUID.nameUUIDFromBytes((player.getUuidAsString() + "@" + client.world.getTime()).getBytes());
        if (!handledDeathScreens.add(key)) return;

        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        WaypointManager.get().recordDeath(
                player.getGameProfile().getName(),
                player.getX(), player.getY(), player.getZ(),
                dimension,
                cfg.deathColor,
                cfg.maxDeaths
        );
    }

    private String resolveWorldKey(MinecraftClient client) {
        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            return "sp:" + client.getServer().getSaveProperties().getLevelName();
        }
        if (client.getCurrentServerEntry() != null) {
            return "mp:" + client.getCurrentServerEntry().address;
        }
        return "unknown";
    }
}
