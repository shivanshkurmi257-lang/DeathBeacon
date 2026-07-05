package com.deathbeacon.render;

import com.deathbeacon.config.DeathBeaconConfig;
import com.deathbeacon.data.Waypoint;
import com.deathbeacon.storage.WaypointManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws a semi-transparent animated beam above every visible waypoint whose
 * beamEnabled flag is set, similar to a beacon beam. Distance/name labels are
 * handled separately in {@link WaypointLabelRenderer} (billboarded text is
 * drawn through the in-world text renderer there); this class owns the quad.
 */
public final class BeamRenderer {

    private BeamRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BeamRenderer::render);
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

        float time = (client.world.getTime() % 1000) + client.getTickDelta();

        VertexConsumerProvider.Immediate vcp = client.getBufferBuilders().getEntityVertexConsumers();

        for (Waypoint w : WaypointManager.get().getAllRenderable()) {
            if (!w.beamEnabled) continue;
            if (!w.dimension.equals(dimensionId)) continue;

            double dx = w.x - camPos.x;
            double dy = w.y - camPos.y;
            double dz = w.z - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > (double) cfg.beamRenderDistance * cfg.beamRenderDistance) continue;

            matrices.push();
            matrices.translate(dx, dy, dz);
            drawBeam(matrices, vcp, w.color, cfg, time);
            matrices.pop();
        }

        vcp.draw();
    }

    private static void drawBeam(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, int argb, DeathBeaconConfig cfg, float time) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        float baseAlpha = cfg.beamOpacity / 100f;
        float alpha = baseAlpha;
        if (cfg.beamAnimation) {
            // gentle pulse between 70% and 100% of base alpha
            alpha = baseAlpha * (0.85f + 0.15f * MathHelper.sin(time * 0.05f));
        }

        float half = Math.max(0.02f, cfg.beamWidth / 20f);
        float height = cfg.beamHeight;

        VertexConsumer buffer = vcp.getBuffer(DeathBeaconRenderLayers.beam());
        Matrix4f model = matrices.peek().getPositionMatrix();

        // Two crossed quads so the beam reads from any horizontal angle.
        quad(buffer, model, -half, 0, 0, half, 0, 0, half, height, 0, -half, height, 0, r, g, b, alpha);
        quad(buffer, model, 0, 0, -half, 0, 0, half, 0, height, half, 0, height, -half, r, g, b, alpha);
    }

    private static void quad(VertexConsumer buffer, Matrix4f model,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              float r, float g, float b, float a) {
        buffer.vertex(model, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(model, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(model, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(model, x4, y4, z4).color(r, g, b, a);
    }
}
