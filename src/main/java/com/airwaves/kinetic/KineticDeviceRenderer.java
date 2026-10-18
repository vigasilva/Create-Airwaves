/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Create kinetic/stress behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.kinetic;

import com.airwaves.client.CableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.phys.AABB;

public class KineticDeviceRenderer<T extends KineticBlockEntity> extends ShaftRenderer<T> {
   public KineticDeviceRenderer(Context ctx) {
      super(ctx);
   }

   protected void renderSafe(T be, float pt, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
      super.renderSafe(be, pt, ms, buf, light, overlay);
      CableRenderer.renderCables(be, ms, buf, pt);
   }

   public boolean shouldRenderOffScreen(T be) {
      return false;
   }

   public int getViewDistance() {
      return 96;
   }

   public AABB getRenderBoundingBox(T be) {
      return CableRenderer.bounds(be);
   }
}
