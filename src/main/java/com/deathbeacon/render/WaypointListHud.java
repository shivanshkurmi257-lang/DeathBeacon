package com.deathbeacon.render;

import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.data.Waypoint;
import com.deathbeacon.storage.WaypointManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class WaypointListHud {

    private WaypointListHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(WaypointListHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
        if (!cfg.modEnabled || !cfg.enableWaypointListHud) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) return;

        String dimensionId = player.getWorld().getRegistryKey().getValue().toString();
        List<Waypoint> visible = WaypointManager.get().getAllRenderable().stream()
                .filter(w -> w.hudEnabled && w.dimension.equals(dimensionId))
                .sorted(Comparator.comparingDouble(w -> distSq(player, w)))
                .limit(cfg.hudMaxEntries)
                .collect(Collectors.toList());

        int total = WaypointManager.get().getAllRenderable().size();
        float scale = cfg.hudScale / 100f;
        int alpha = (int) (255 * (cfg.hudOpacity / 100f));

        int lineHeight = (int) (10 * scale);
        int panelWidth = (int) (150 * scale);
        int panelHeight = lineHeight * (visible.size() + 1) + 6;

        int[] origin = originFor(cfg.listHudPosition.name(), ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), panelWidth, panelHeight);
        int x = origin[0];
        int y = origin[1];

        ctx.fill(x, y, x + panelWidth, y + panelHeight, (alpha / 2) << 24);

        String header = "Waypoints " + visible.size() + "/" + total;
        ctx.drawTextWithShadow(client.textRenderer, header, x + 4, y + 3, 0xFFFFFF | (alpha << 24));

        int lineY = y + 3 + lineHeight;
        for (Waypoint w : visible) {
            double dist = Math.sqrt(distSq(player, w));
            String line = "\u25CF " + w.displayName() + " (" + Math.round(dist) + "m)";
            int color = (alpha << 24) | (w.color & 0xFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer, line, x + 4, lineY, color);
            lineY += lineHeight;
        }
    }

    private static double distSq(PlayerEntity player, Waypoint w) {
        double dx = w.x - player.getX();
        double dy = w.y - player.getY();
        double dz = w.z - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static int[] originFor(String position, int screenW, int screenH, int panelW, int panelH) {
        int margin = 6;
        return switch (position) {
            case "TOP_CENTER" -> new int[]{(screenW - panelW) / 2, margin};
            case "TOP_RIGHT" -> new int[]{screenW - panelW - margin, margin};
            case "BOTTOM_LEFT" -> new int[]{margin, screenH - panelH - margin};
            case "BOTTOM_RIGHT" -> new int[]{screenW - panelW - margin, screenH - panelH - margin};
            default -> new int[]{margin, margin}; // TOP_LEFT
        };
    }
}
