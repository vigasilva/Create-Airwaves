/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Client-only rendering, audio, UI, Ponder, or JEI behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.client;

import com.airwaves.item.BoardItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BoardItemRenderer extends BlockEntityWithoutLevelRenderer {
   public BoardItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public void renderByItem(ItemStack st, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
      ResourceLocation tex = BoardIcons.texture(BoardItem.layout(st));
      VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(tex));
      Pose p = ps.last();
      float zf = 0.53125F;
      float zb = 0.46875F;
      v(vc, p, 0.0F, 0.0F, zf, 0.0F, 1.0F, light, overlay, 0.0F, 0.0F, 1.0F);
      v(vc, p, 1.0F, 0.0F, zf, 1.0F, 1.0F, light, overlay, 0.0F, 0.0F, 1.0F);
      v(vc, p, 1.0F, 1.0F, zf, 1.0F, 0.0F, light, overlay, 0.0F, 0.0F, 1.0F);
      v(vc, p, 0.0F, 1.0F, zf, 0.0F, 0.0F, light, overlay, 0.0F, 0.0F, 1.0F);
      v(vc, p, 0.0F, 1.0F, zb, 0.0F, 0.0F, light, overlay, 0.0F, 0.0F, -1.0F);
      v(vc, p, 1.0F, 1.0F, zb, 1.0F, 0.0F, light, overlay, 0.0F, 0.0F, -1.0F);
      v(vc, p, 1.0F, 0.0F, zb, 1.0F, 1.0F, light, overlay, 0.0F, 0.0F, -1.0F);
      v(vc, p, 0.0F, 0.0F, zb, 0.0F, 1.0F, light, overlay, 0.0F, 0.0F, -1.0F);
   }

   private static void v(VertexConsumer vc, Pose p, float x, float y, float z, float u, float v, int light, int overlay, float nx, float ny, float nz) {
      vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
   }
}
