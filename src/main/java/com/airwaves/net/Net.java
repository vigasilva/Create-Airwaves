/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Custom network payloads and packet registration.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.net;

import com.airwaves.ModRegistry;
import com.airwaves.Tuning;
import com.airwaves.client.ClientHandlers;
import com.airwaves.compat.svc.SvcState;
import com.airwaves.logic.RadioNetwork;
import com.airwaves.menu.TuningTableMenu;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Net {
   private Net() {
   }

   public static void register(RegisterPayloadHandlersEvent e) {
      PayloadRegistrar r = e.registrar("3");
      r.playToServer(TunePayload.TYPE, TunePayload.CODEC, Net::onTune);
      r.playToServer(VoiceFramePayload.TYPE, VoiceFramePayload.CODEC, Net::onVoice);
      r.playToClient(VoiceRelayPayload.TYPE, VoiceRelayPayload.CODEC, ClientHandlers::onVoiceRelay);
      r.playToClient(RadioAudioPayload.TYPE, RadioAudioPayload.CODEC, ClientHandlers::onRadioAudio);
      r.playToServer(SpectrumRequestPayload.TYPE, SpectrumRequestPayload.CODEC, Net::onSpectrumRequest);
      r.playToClient(SpectrumPayload.TYPE, SpectrumPayload.CODEC, ClientHandlers::onSpectrum);
      r.playToClient(HandheldAudioPayload.TYPE, HandheldAudioPayload.CODEC, ClientHandlers::onHandheldAudio);
      r.playToClient(HandheldStatePayload.TYPE, HandheldStatePayload.CODEC, ClientHandlers::onHandheldState);
      r.playToClient(CablePulsePayload.TYPE, CablePulsePayload.CODEC, ClientHandlers::onPulse);
      r.playToClient(StaticBurstPayload.TYPE, StaticBurstPayload.CODEC, ClientHandlers::onBurst);
      r.playToClient(RadioSoundPayload.TYPE, RadioSoundPayload.CODEC, ClientHandlers::onRadioSound);
      r.playToServer(VoiceModePayload.TYPE, VoiceModePayload.CODEC, Net::onVoiceMode);
   }

   private static void onTune(TunePayload p, IPayloadContext ctx) {
      if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof TuningTableMenu m) {
         ItemStack s = m.getSlot(0).getItem();
         if (!s.isEmpty()) {
            s.set((DataComponentType)ModRegistry.TUNING.get(), new Tuning(p.fm(), p.freq()).clamped());
            m.broadcastChanges();
         }
      }
   }

   private static void onSpectrumRequest(SpectrumRequestPayload p, IPayloadContext ctx) {
      if (ctx.player() instanceof ServerPlayer sp) {
         PacketDistributor.sendToPlayer(sp, new SpectrumPayload(RadioNetwork.spectrum(sp)), new CustomPacketPayload[0]);
      }
   }

   private static void onVoiceMode(VoiceModePayload p, IPayloadContext ctx) {
      if (ctx.player() instanceof ServerPlayer sp) {
         SvcState.set(sp.getUUID(), p.simpleVoiceChat());
      }
   }

   private static void onVoice(VoiceFramePayload p, IPayloadContext ctx) {
      if (ctx.player() instanceof ServerPlayer sp && !sp.isSpectator()) {
         if (!SvcState.usesSvc(sp.getUUID())) {
            byte[] d = p.data();
            if (d.length >= 16 && d.length <= 960) {
               PacketDistributor.sendToPlayersNear(
                  sp.serverLevel(), sp, sp.getX(), sp.getY(), sp.getZ(), 48.0, new VoiceRelayPayload(sp.getUUID(), d), new CustomPacketPayload[0]
               );
               RadioNetwork.onVoice(sp, d);
            }
         }
      }
   }
}
