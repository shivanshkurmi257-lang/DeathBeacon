package com.deathbeacon.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * A minimal position+color, translucent, no-cull render layer for the beam
 * quads. NOTE: RenderLayer's builder internals shift fairly often between MC
 * versions - if this doesn't compile 1:1 against 1.21.11 mappings, compare
 * against RenderLayer#getLightning or #getEndPortal in the deobfuscated
 * source for the current field/method names and adjust accordingly.
 */
public final class DeathBeaconRenderLayers {

    private DeathBeaconRenderLayers() {
    }

    private static final RenderLayer BEAM_LAYER = RenderLayer.of(
            "deathbeacon_beam",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    public static RenderLayer beam() {
        return BEAM_LAYER;
    }
}
