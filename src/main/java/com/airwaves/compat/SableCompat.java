/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Optional compatibility bridge for another mod or API.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.compat;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class SableCompat {
   private static final SableCompanion S = SableCompanion.INSTANCE;

   private SableCompat() {
   }

   public static boolean onShip(Level lv, BlockPos p) {
      return lv != null && S.getContaining(lv, p) != null;
   }

   public static Vec3 global(Level lv, Vec3 p) {
      if (lv == null) {
         return p;
      }

      Vector3d v = S.projectOutOfSubLevel(lv, new Vector3d(p.x, p.y, p.z));
      return new Vec3(v.x, v.y, v.z);
   }

   public static Vec3 global(Level lv, BlockPos p) {
      return global(lv, Vec3.atCenterOf(p));
   }

   public static BlockPos globalPos(Level lv, BlockPos p) {
      return onShip(lv, p) ? BlockPos.containing(global(lv, p)) : p;
   }

   public static double distance(Level lv, Vec3 a, Vec3 b) {
      return lv == null ? a.distanceTo(b) : Math.sqrt(S.distanceSquaredWithSubLevels(lv, a, b));
   }

   public static double altitude(Level lv, BlockPos p) {
      if (!onShip(lv, p)) {
         return 0.0;
      }

      Vec3 g = global(lv, p);
      int x = (int)Math.floor(g.x);
      int z = (int)Math.floor(g.z);
      return !lv.isLoaded(new BlockPos(x, 64, z)) ? 0.0 : Math.max(0.0, g.y - lv.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z));
   }

   public static Vec3 intoRenderSpaceOf(BlockPos local, Vec3 globalPoint, float partialTick) {
      ClientSubLevelAccess s = S.getContainingClient(local);
      return s == null ? globalPoint : s.renderPose(partialTick).transformPositionInverse(globalPoint);
   }

   public static Vec3 rendered(Vec3 plotPoint, float partialTick) {
      ClientSubLevelAccess s = S.getContainingClient(plotPoint);
      return s == null ? plotPoint : s.renderPose(partialTick).transformPosition(plotPoint);
   }

   public static boolean sameSpace(BlockPos a, BlockPos b) {
      SubLevelAccess x = S.getContainingClient(a);
      SubLevelAccess y = S.getContainingClient(b);
      return x == y || x != null && y != null && x.getUniqueId().equals(y.getUniqueId());
   }
}
