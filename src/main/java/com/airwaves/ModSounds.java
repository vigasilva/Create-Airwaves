/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "airwaves");
   public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_IN = s("crystal_in");
   public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_OUT = s("crystal_out");
   public static final DeferredHolder<SoundEvent, SoundEvent> KNOB = s("knob");
   public static final DeferredHolder<SoundEvent, SoundEvent> TUNE = s("tune");
   public static final DeferredHolder<SoundEvent, SoundEvent> SEEK = s("seek");
   public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ON = s("power_on");
   public static final DeferredHolder<SoundEvent, SoundEvent> POWER_OFF = s("power_off");
   public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_GRAB = s("cable_grab");
   public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_ATTACH = s("cable_attach");
   public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_HEAVY = s("cable_heavy");
   public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_DETACH = s("cable_detach");
   public static final DeferredHolder<SoundEvent, SoundEvent> CABLE_REJECT = s("cable_reject");
   public static final DeferredHolder<SoundEvent, SoundEvent> STRUCTURE = s("structure");
   public static final DeferredHolder<SoundEvent, SoundEvent> SWITCH = s("switch");
   public static final DeferredHolder<SoundEvent, SoundEvent> PROBE = s("probe");
   public static final DeferredHolder<SoundEvent, SoundEvent> BOOK = s("book");

   private static DeferredHolder<SoundEvent, SoundEvent> s(String n) {
      return SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("airwaves", n)));
   }

   public static void register(IEventBus bus) {
      SOUNDS.register(bus);
   }

   private ModSounds() {
   }
}
