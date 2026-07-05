package com.deathbeacon.render;

import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.data.Waypoint;
import com.deathbeacon.storage.WaypointManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OrderedRenderCommandQueue;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * "HOME (235m)" / "Death #4 (845m)" floating labels above each beam.
 * Billboarded text drawn via TextRenderer#draw with a look-at rotation
 * matching the player's camera - the standard approach used by nameplate-style
 * in-world text (see EntityRenderer#renderLabelIfPresent for the vanilla
 * equivalent this is modeled on).
 */
public final class WaypointLabelRenderer {

    private WaypointLabelRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WaypointLabelRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        DeathBeaconConfig cfg = AutoConfig.getConfigHolder(DeathBeaconConfig.class).getConfig();
        if (!cfg.modEnabled || !cfg.enableBeams) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || context.camera() == null) return;

        Vec3d camPos = context.camera().getPos();
        String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        TextRenderer textRenderer = client.textRenderer;
        VertexConsumerProvider.Immediate vcp = client.getBufferBuilders().getEntityVertexConsumers();

        for (Waypoint w : WaypointManager.get().getAllRenderable()) {
            if (!w.hudEnabled) continue;
            if (!w.dimension.equals(dimensionId)) continue;

            double dx = w.x - camPos.x;
            double dy = (w.y + cfg.beamHeight + 1) - camPos.y;
            double dz = w.z - camPos.z;
            double dist = Math.sqrt((w.x - camPos.x) * (w.x - camPos.x)
                    + (w.y - camPos.y) * (w.y - camPos.y)
                    + (w.z - camPos.z) * (w.z - camPos.z));
            if (dist > cfg.beamRenderDistance) continue;

            String label = w.displayName() + "  (" + Math.round(dist) + "m)";

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);

            float textX = -textRenderer.getWidth(label) / 2f;
            int bg = (int) (0.4f * 255) << 24;
            textRenderer.draw(label, textX, 0, w.color, false,
                    matrices.peek().getPositionMatrix(), vcp,
                    TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);

            matrices.pop();
        }

        vcp.draw();
    }
}
