/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Client-only rendering, audio, UI, Ponder, or JEI behavior.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.client;

import com.airwaves.be.CableNode;
import com.airwaves.cable.Link;
import com.airwaves.compat.SableCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CableRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   public static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("airwaves", "textures/misc/cable.png");
   public static final ResourceLocation TEX_PULSE = ResourceLocation.fromNamespaceAndPath("airwaves", "textures/misc/cable_pulse.png");
   private static final Map<BlockEntity, CableRenderer.Bounds> BOUNDS = new WeakHashMap<>();

   public static Vec3 anchor(Level lv, BlockPos pos, int slot) {
      return lv.isLoaded(pos) && lv.getBlockEntity(pos) instanceof CableNode n ? n.anchor(Math.min(slot, n.connectors() - 1)) : Vec3.atCenterOf(pos);
   }

   public void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
      renderCables(be, ps, buf, pt);
   }

   public static void renderCables(BlockEntity be, PoseStack ps, MultiBufferSource buf, float pt) {
      Level lv = be.getLevel();
      if (lv != null && be instanceof CableNode node) {
         BlockPos o = be.getBlockPos();

         for (Link l : node.links().list()) {
            boolean otherDraws = l.other().asLong() < o.asLong()
               && lv.isLoaded(l.other())
               && lv.getBlockEntity(l.other()) instanceof CableNode n2
               && n2.links().has(l.otherSlot(), o, l.slot())
               && SableCompat.sameSpace(o, l.other());
            if (!otherDraws) {
               Vec3 aw = node.anchor(l.slot());
               Vec3 bw = anchor(lv, l.other(), l.otherSlot());
               if (!SableCompat.sameSpace(o, l.other())) {
                  bw = SableCompat.intoRenderSpaceOf(o, SableCompat.rendered(bw, pt), pt);
               }

               Vec3 a = aw.subtract(o.getX(), o.getY(), o.getZ());
               Vec3 b = bw.subtract(o.getX(), o.getY(), o.getZ());
               int la = LevelRenderer.getLightColor(lv, BlockPos.containing(aw));
               int lb = LevelRenderer.getLightColor(lv, BlockPos.containing(bw));
               line(ps, buf, a, b, la, lb, l.type().color, l.type().width);
               int dir = CablePulses.dir(o.asLong(), l.slot(), l.other().asLong(), l.otherSlot());
               if (dir != 0) {
                  pulse(ps, buf, dir > 0 ? a : b, dir > 0 ? b : a, l.type().width * 1.6F);
               }
            }
         }
      }
   }

   public static void renderCables(BlockEntity be, PoseStack ps, MultiBufferSource buf) {
      renderCables(be, ps, buf, 0.0F);
   }

   static Vec3 at(Vec3 a, Vec3 d, double sag, double u) {
      return new Vec3(a.x + d.x * u, a.y + d.y * u - sag * 4.0 * u * (1.0 - u), a.z + d.z * u);
   }

   static double sag(double len) {
      return Math.min(2.4, 0.045 * len + 0.06);
   }

   public static void line(PoseStack ps, MultiBufferSource buf, Vec3 a, Vec3 b, int lightA, int lightB, int argb, float w) {
      Vec3 d = b.subtract(a);
      double len = d.length();
      if (!(len < 0.001)) {
         int n = Math.max(4, Math.min(24, (int)(len * 1.25)));
         double sag = sag(len);
         double hx = -d.z;
         double hz = d.x;
         double hl = Math.sqrt(hx * hx + hz * hz);
         Vec3 side = hl < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(hx / hl, 0.0, hz / hl);
         float r = (argb >> 16 & 0xFF) / 255.0F;
         float g = (argb >> 8 & 0xFF) / 255.0F;
         float bl = (argb & 0xFF) / 255.0F;
         VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(WHITE));
         Pose pose = ps.last();
         Vec3 prev = a;

         for (int i = 1; i <= n; i++) {
            double u = (double)i / n;
            Vec3 p = at(a, d, sag, u);
            Vec3 seg = p.subtract(prev);
            double sl = seg.length();
            Vec3 up = hl < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 wv = up.subtract(seg.scale(up.dot(seg) / Math.max(1.0E-9, sl * sl))).normalize().scale(w / 2.0F);
            Vec3 wh = side.scale(w / 2.0F);
            int la = mix(lightA, lightB, (double)(i - 1) / n);
            int lb = mix(lightA, lightB, u);
            quad(
               vc,
               pose,
               prev.subtract(wv),
               prev.add(wv),
               p.add(wv),
               p.subtract(wv),
               r * 0.86F,
               g * 0.86F,
               bl * 0.86F,
               la,
               lb,
               (float)side.x,
               0.0F,
               (float)side.z
            );
            quad(vc, pose, prev.subtract(wh), prev.add(wh), p.add(wh), p.subtract(wh), r, g, bl, la, lb, 0.0F, 1.0F, 0.0F);
            prev = p;
         }
      }
   }

   private static void quad(
      VertexConsumer vc, Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, int la, int lb, float nx, float ny, float nz
   ) {
      v(vc, pose, a, r, g, bl, 1.0F, 0.0F, 1.0F, la, nx, ny, nz);
      v(vc, pose, b, r, g, bl, 1.0F, 0.0F, 0.0F, la, nx, ny, nz);
      v(vc, pose, c, r, g, bl, 1.0F, 1.0F, 0.0F, lb, nx, ny, nz);
      v(vc, pose, d, r, g, bl, 1.0F, 1.0F, 1.0F, lb, nx, ny, nz);
      v(vc, pose, d, r, g, bl, 1.0F, 1.0F, 1.0F, lb, -nx, -ny, -nz);
      v(vc, pose, c, r, g, bl, 1.0F, 1.0F, 0.0F, lb, -nx, -ny, -nz);
      v(vc, pose, b, r, g, bl, 1.0F, 0.0F, 0.0F, la, -nx, -ny, -nz);
      v(vc, pose, a, r, g, bl, 1.0F, 0.0F, 1.0F, la, -nx, -ny, -nz);
   }

   static void pulse(PoseStack ps, MultiBufferSource buf, Vec3 from, Vec3 to, float w) {
      Vec3 d = to.subtract(from);
      double len = d.length();
      if (!(len < 0.001)) {
         double sag = sag(len);
         double t = System.currentTimeMillis() % 100000L / 1000.0;
         double speed = 9.0;
         double spacing = 3.0;
         double dash = 0.7;
         VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX_PULSE));
         Pose pose = ps.last();
         double hx = -d.z;
         double hz = d.x;
         double hl = Math.sqrt(hx * hx + hz * hz);
         float nx = hl < 1.0E-4 ? 1.0F : (float)(hx / hl);
         float nz = hl < 1.0E-4 ? 0.0F : (float)(hz / hl);
         double off = t * speed % spacing;

         for (double s0 = off - spacing; s0 < len; s0 += spacing) {
            double a0 = Math.max(0.0, s0);
            double a1 = Math.min(len, s0 + dash);
            if (!(a1 <= a0)) {
               Vec3 p0 = at(from, d, sag, a0 / len);
               Vec3 p1 = at(from, d, sag, a1 / len);
               Vec3 seg = p1.subtract(p0);
               double sl = seg.length();
               if (!(sl < 1.0E-4)) {
                  Vec3 up = hl < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
                  Vec3 wv = up.subtract(seg.scale(up.dot(seg) / (sl * sl))).normalize().scale(w / 2.0F);
                  int light = 15728880;
                  v(vc, pose, p0.subtract(wv), 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, light, nx, 0.0F, nz);
                  v(vc, pose, p0.add(wv), 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, light, nx, 0.0F, nz);
                  v(vc, pose, p1.add(wv), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, light, nx, 0.0F, nz);
                  v(vc, pose, p1.subtract(wv), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, light, nx, 0.0F, nz);
               }
            }
         }
      }
   }

   private static void v(VertexConsumer vc, Pose pose, Vec3 p, float r, float g, float b, float a, float u, float vv, int light, float nx, float ny, float nz) {
      vc.addVertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .setColor(r, g, b, a)
         .setUv(u, vv)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, nx, ny, nz);
   }

   private static int mix(int la, int lb, double t) {
      int bl = (int)(LightTexture.block(la) + (LightTexture.block(lb) - LightTexture.block(la)) * t);
      int sk = (int)(LightTexture.sky(la) + (LightTexture.sky(lb) - LightTexture.sky(la)) * t);
      return LightTexture.pack(bl, sk);
   }

   public boolean shouldRenderOffScreen(T be) {
      return false;
   }

   public int getViewDistance() {
      return 96;
   }

   public AABB getRenderBoundingBox(T be) {
      return bounds(be);
   }

   public static AABB bounds(BlockEntity be) {
      long now = System.currentTimeMillis();
      CableRenderer.Bounds c = BOUNDS.get(be);
      if (c != null && now - c.at() < 1000L) {
         return c.box();
      }

      AABB box = computeBounds(be);
      BOUNDS.put(be, new CableRenderer.Bounds(box, now));
      return box;
   }

   private static AABB computeBounds(BlockEntity be) {
      AABB b = new AABB(be.getBlockPos());
      Level lv = be.getLevel();
      if (be instanceof CableNode c) {
         for (int i = 0; i < c.connectors(); i++) {
            Vec3 a = c.anchor(i);
            b = b.minmax(new AABB(a, a));
         }

         for (Link l : c.links().list()) {
            if (SableCompat.sameSpace(be.getBlockPos(), l.other())) {
               Vec3 a = lv == null ? Vec3.atCenterOf(l.other()) : anchor(lv, l.other(), l.otherSlot());
               b = b.minmax(new AABB(a, a));
            }
         }
      }

      return b.inflate(3.0);
   }

   private record Bounds(AABB box, long at) {
   }
}
