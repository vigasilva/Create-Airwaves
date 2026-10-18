/**
 * Optional bridge to Sable Companion.
 *
 * <p>The companion API is deliberately accessed reflectively: it is not
 * necessary to compile the reconstruction, and worlds without the companion
 * retain ordinary Minecraft-space behaviour. When the companion is installed,
 * the original sub-level coordinate conversions are used.</p>
 */
package com.airwaves.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class SableCompat {
   private static final String COMPANION = "dev.ryanhcode.sable.companion.SableCompanion";
   private static final Object INSTANCE = loadInstance();

   private SableCompat() {
   }

   private static Object loadInstance() {
      try {
         Class<?> type = Class.forName(COMPANION);
         Field field = type.getField("INSTANCE");
         return field.get(null);
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return null;
      }
   }

   private static Object call(Object target, String name, Object... args) {
      if (target == null) {
         return null;
      }

      for (Method method : target.getClass().getMethods()) {
         if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
            continue;
         }

         Class<?>[] types = method.getParameterTypes();
         boolean matches = true;

         for (int i = 0; i < types.length; i++) {
            if (args[i] != null && !accepts(types[i], args[i].getClass())) {
               matches = false;
               break;
            }
         }

         if (matches) {
            try {
               return method.invoke(target, args);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
               return null;
            }
         }
      }

      return null;
   }

   private static boolean accepts(Class<?> parameter, Class<?> value) {
      if (!parameter.isPrimitive()) {
         return parameter.isAssignableFrom(value);
      }

      return parameter == boolean.class && value == Boolean.class
         || parameter == byte.class && value == Byte.class
         || parameter == short.class && value == Short.class
         || parameter == int.class && value == Integer.class
         || parameter == long.class && value == Long.class
         || parameter == float.class && value == Float.class
         || parameter == double.class && value == Double.class
         || parameter == char.class && value == Character.class;
   }

   public static boolean onShip(Level level, BlockPos pos) {
      return call(INSTANCE, "getContaining", level, pos) != null;
   }

   public static Vec3 global(Level level, Vec3 pos) {
      if (level == null) {
         return pos;
      }

      Object result = call(INSTANCE, "projectOutOfSubLevel", level, pos);
      if (result instanceof Vec3 vec) {
         return vec;
      }

      result = call(INSTANCE, "projectOutOfSubLevel", level, new Vector3d(pos.x, pos.y, pos.z));
      if (result instanceof Vector3d vec) {
         return new Vec3(vec.x, vec.y, vec.z);
      }

      return pos;
   }

   public static Vec3 global(Level level, BlockPos pos) {
      return global(level, Vec3.atCenterOf(pos));
   }

   public static BlockPos globalPos(Level level, BlockPos pos) {
      return onShip(level, pos) ? BlockPos.containing(global(level, pos)) : pos;
   }

   public static double distance(Level level, Vec3 a, Vec3 b) {
      if (level == null) {
         return a.distanceTo(b);
      }

      Object result = call(INSTANCE, "distanceSquaredWithSubLevels", level, a, b);
      return result instanceof Number n ? Math.sqrt(n.doubleValue()) : a.distanceTo(b);
   }

   public static double altitude(Level level, BlockPos pos) {
      if (!onShip(level, pos)) {
         return 0.0;
      }

      Vec3 global = global(level, pos);
      int x = (int)Math.floor(global.x);
      int z = (int)Math.floor(global.z);
      return !level.isLoaded(new BlockPos(x, 64, z)) ? 0.0 : Math.max(0.0, global.y - level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z));
   }

   public static Vec3 intoRenderSpaceOf(BlockPos local, Vec3 globalPoint, float partialTick) {
      Object subLevel = call(INSTANCE, "getContainingClient", local);
      Object pose = call(subLevel, "renderPose", partialTick);
      Object result = call(pose, "transformPositionInverse", globalPoint);
      return result instanceof Vec3 vec ? vec : globalPoint;
   }

   public static Vec3 rendered(Vec3 plotPoint, float partialTick) {
      Object subLevel = call(INSTANCE, "getContainingClient", plotPoint);
      Object pose = call(subLevel, "renderPose", partialTick);
      Object result = call(pose, "transformPosition", plotPoint);
      return result instanceof Vec3 vec ? vec : plotPoint;
   }

   public static boolean sameSpace(BlockPos a, BlockPos b) {
      Object x = call(INSTANCE, "getContainingClient", a);
      Object y = call(INSTANCE, "getContainingClient", b);
      if (x == y) {
         return true;
      }

      Object xId = call(x, "getUniqueId");
      Object yId = call(y, "getUniqueId");
      return xId != null && xId.equals(yId);
   }
}
