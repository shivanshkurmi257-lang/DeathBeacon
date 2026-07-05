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
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Xaero-style compass bar: a horizontal strip of cardinal directions across
 * the top of the screen, with waypoint markers sliding along it based on
 * bearing from the player. Smoothly interpolated frame to frame.
 */
public final class LocatorHud {

    private static float smoothedYaw = 0f;

    private LocatorHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(LocatorHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
        if (!cfg.modEnabled || !cfg.enableLocatorHud) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) return;

        float scale = cfg.hudScale / 100f;
        int alpha = (int) (255 * (cfg.hudOpacity / 100f));

        int screenWidth = ctx.getScaledWindowWidth();
        int barWidth = (int) (280 * scale);
        int centerX = screenWidth / 2;
        int barY = (int) (10 * scale);

        float targetYaw = MathHelper.wrapDegrees(player.getYaw());
        smoothedYaw = MathHelper.lerpAngleDegrees(0.3f, smoothedYaw, targetYaw);

        // Bar background
        ctx.fill(centerX - barWidth / 2 - 4, barY - 2, centerX + barWidth / 2 + 4, barY + (int) (14 * scale), (alpha / 2) << 24);

        // Cardinal ticks every 22.5 degrees across a 180-degree visible window
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        float pxPerDegree = barWidth / 180f;

        for (int deg = -180; deg <= 180; deg += 15) {
            float relative = MathHelper.wrapDegrees(deg - smoothedYaw + 180);
            if (Math.abs(relative) > 90) continue;
            int x = centerX + (int) (relative * pxPerDegree);
            boolean major = deg % 45 == 0;
            int h = major ? (int) (10 * scale) : (int) (5 * scale);
            ctx.fill(x, barY, x + 1, barY + h, (alpha << 24) | 0xAAAAAA);
            if (major) {
                int dirIndex = (((deg % 360) + 360) % 360) / 45;
                String label = dirs[dirIndex];
                ctx.drawCenteredTextWithShadow(client.textRenderer, label, x, barY + h + 2, 0xFFFFFF | (alpha << 24));
            }
        }

        // Waypoint markers
        List<Waypoint> all = WaypointManager.get().getAllRenderable();
        String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

        for (Waypoint w : all) {
            if (!w.hudEnabled || !w.dimension.equals(dimensionId)) continue;
            double dx = w.x - player.getX();
            double dz = w.z - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            double bearing = Math.toDegrees(Math.atan2(-dx, dz));
            float relative = MathHelper.wrapDegrees((float) bearing - smoothedYaw + 180);
            if (Math.abs(relative) > 90) continue;

            int x = centerX + (int) (relative * pxPerDegree);
            int color = (alpha << 24) | (w.color & 0xFFFFFF);
            ctx.fill(x - 2, barY - 8, x + 2, barY - 2, color);

            String text = w.displayName() + " (" + Math.round(dist) + "m)";
            int fontScale = Math.max(50, cfg.hudFontSize) ;
            ctx.drawCenteredTextWithShadow(client.textRenderer, text, x, barY - 18, color);
        }
    }
}
