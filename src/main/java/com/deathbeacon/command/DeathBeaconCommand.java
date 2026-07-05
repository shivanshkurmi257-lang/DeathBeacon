package com.deathbeacon.command;

import com.deathbeacon.data.Waypoint;
import com.deathbeacon.data.WaypointCategory;
import com.deathbeacon.gui.EditWaypointScreen;
import com.deathbeacon.gui.WaypointListScreen;
import com.deathbeacon.storage.WaypointManager;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers every /db subcommand from Feature 13:
 * /db gui, add, edit, delete, favorite, death, hide, show, export, import, reload
 */
public final class DeathBeaconCommand {

    private DeathBeaconCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                 net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.RegistrationEnvironment env) {
        dispatcher.register(literal("db")
                .then(literal("gui").executes(ctx -> {
                    MinecraftClient.getInstance().setScreen(new WaypointListScreen(WaypointCategory.WAYPOINTS));
                    return 1;
                }))
                .then(literal("add").executes(ctx -> {
                    MinecraftClient.getInstance().setScreen(EditWaypointScreen.createNew(null));
                    return 1;
                }))
                .then(literal("edit")
                        .then(argument("name", StringArgumentType.greedyString()).executes(DeathBeaconCommand::edit)))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.greedyString()).executes(DeathBeaconCommand::delete)))
                .then(literal("favorite")
                        .then(argument("name", StringArgumentType.greedyString()).executes(DeathBeaconCommand::favorite)))
                .then(literal("death").executes(ctx -> {
                    MinecraftClient.getInstance().setScreen(new WaypointListScreen(WaypointCategory.DEATHS));
                    return 1;
                }))
                .then(literal("hide")
                        .then(argument("name", StringArgumentType.greedyString()).executes(c -> setVisible(c, false))))
                .then(literal("show")
                        .then(argument("name", StringArgumentType.greedyString()).executes(c -> setVisible(c, true))))
                .then(literal("export").executes(DeathBeaconCommand::export))
                .then(literal("import").executes(DeathBeaconCommand::doImport))
                .then(literal("reload").executes(ctx -> {
                    WaypointManager.get().loadAll();
                    ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] Data reloaded from disk."));
                    return 1;
                }))
        );
    }

    private static Optional<Waypoint> findByName(String name) {
        List<Waypoint> all = WaypointManager.get().getAllRenderable();
        return all.stream().filter(w -> w.displayName().equalsIgnoreCase(name)).findFirst();
    }

    private static int edit(CommandContext<FabricClientCommandSource> ctx) {
        String name = getString(ctx, "name");
        Optional<Waypoint> w = findByName(name);
        if (w.isEmpty()) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] No waypoint named '" + name + "'."));
            return 0;
        }
        MinecraftClient.getInstance().setScreen(EditWaypointScreen.editExisting(null, w.get()));
        return 1;
    }

    private static int delete(CommandContext<FabricClientCommandSource> ctx) {
        String name = getString(ctx, "name");
        Optional<Waypoint> w = findByName(name);
        if (w.isEmpty()) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] No waypoint named '" + name + "'."));
            return 0;
        }
        WaypointManager.get().delete(w.get().id);
        ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] Deleted '" + name + "'."));
        return 1;
    }

    private static int favorite(CommandContext<FabricClientCommandSource> ctx) {
        String name = getString(ctx, "name");
        Optional<Waypoint> w = findByName(name);
        if (w.isEmpty()) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] No waypoint named '" + name + "'."));
            return 0;
        }
        WaypointManager.get().toggleFavorite(w.get().id);
        ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] Toggled favorite on '" + name + "'."));
        return 1;
    }

    private static int setVisible(CommandContext<FabricClientCommandSource> ctx, boolean visible) {
        String name = getString(ctx, "name");
        Optional<Waypoint> w = findByName(name);
        if (w.isEmpty()) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] No waypoint named '" + name + "'."));
            return 0;
        }
        if (w.get().visible != visible) {
            WaypointManager.get().toggleVisible(w.get().id);
        }
        ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] '" + name + "' is now " + (visible ? "visible" : "hidden") + "."));
        return 1;
    }

    private static int export(CommandContext<FabricClientCommandSource> ctx) {
        try {
            Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("deathbeacon");
            Path out = dir.resolve("export.json");
            List<Waypoint> all = WaypointManager.get().getAllRenderable();
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(all), StandardCharsets.UTF_8);
            ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] Exported " + all.size() + " waypoints to " + out));
        } catch (IOException e) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] Export failed: " + e.getMessage()));
        }
        return 1;
    }

    private static int doImport(CommandContext<FabricClientCommandSource> ctx) {
        try {
            Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("deathbeacon");
            Path in = dir.resolve("export.json");
            if (!Files.exists(in)) {
                ctx.getSource().sendError(Text.literal("[DeathBeacon] No export.json found in config/deathbeacon/."));
                return 0;
            }
            String json = Files.readString(in, StandardCharsets.UTF_8);
            Waypoint[] imported = new GsonBuilder().create().fromJson(json, Waypoint[].class);
            int count = 0;
            for (Waypoint w : imported) {
                w.id = UUID.randomUUID();
                if (w.isGlobal) {
                    WaypointManager.get().addGlobal(w);
                } else if (!w.isDeath) {
                    WaypointManager.get().addWaypoint(w);
                }
                count++;
            }
            ctx.getSource().sendFeedback(Text.literal("[DeathBeacon] Imported " + count + " waypoints."));
        } catch (IOException e) {
            ctx.getSource().sendError(Text.literal("[DeathBeacon] Import failed: " + e.getMessage()));
        }
        return 1;
    }
}
