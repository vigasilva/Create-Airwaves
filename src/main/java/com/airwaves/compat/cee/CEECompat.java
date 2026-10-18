/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Optional compatibility bridge for another mod or API.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.compat.cee;

import com.airwaves.Tier;
import com.airwaves.block.DimensionalRelayBlock;
import com.airwaves.block.JammerBlock;
import com.airwaves.block.TransmitterBlock;
import com.airwaves.structure.KineticStructureBlock;
import com.airwaves.structure.StructureDef;
import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CEECompat {
   private static final DeferredRegister<SimulatedDeviceType<?>> DEVICES = DeferredRegister.create(CEERegistries.SIMULATED_DEVICE_TYPE, "airwaves");
   public static final DeferredHolder<SimulatedDeviceType<?>, SimulatedDeviceType<MachineDevice>> MACHINE = DEVICES.register(
      "machine",
      () -> new SimulatedDeviceType(
         ResourceLocation.fromNamespaceAndPath("airwaves", "machine"), (type, level, pos, sd) -> new MachineDevice(level, pos, sd, type)
      )
   );

   private CEECompat() {
   }

   public static void register(IEventBus modBus) {
      DEVICES.register(modBus);
   }

   public static KineticStructureBlock tower(StructureDef d, Properties p, BiFunction<BlockPos, BlockState, BlockEntity> be) {
      return new CEETowerBlock(d, p, be);
   }

   public static TransmitterBlock transmitter(Tier t, Properties p) {
      return new CEEBlocks.Transmitter(t, p);
   }

   public static JammerBlock jammer(Properties p) {
      return new CEEBlocks.Jammer(p);
   }

   public static DimensionalRelayBlock dimensionalRelay(Properties p) {
      return new CEEBlocks.DimRelay(p);
   }
}
