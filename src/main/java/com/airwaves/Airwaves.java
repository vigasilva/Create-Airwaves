/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

import com.airwaves.be.TransmitterBE;
import com.airwaves.block.TransmitterBlock;
import com.airwaves.cable.ForeignWires;
import com.airwaves.client.ClientInit;
import com.airwaves.compat.create.RadioMovement;
import com.airwaves.compat.svc.SvcState;
import com.airwaves.logic.RadioNetwork;
import com.airwaves.logic.SoundCapture;
import com.airwaves.logic.TapeAudio;
import com.airwaves.net.Net;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.api.stress.BlockStressValues;
import java.util.function.DoubleSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;

/**
 * NeoForge entry point for Airwaves.
 *
 * <p>Registers the mod's content and global event listeners. The server-side
 * radio simulation deliberately runs once each second (20 game ticks), rather
 * than once per tick.</p>
 */
@Mod("airwaves")
public class Airwaves {
   public static final String MODID = "airwaves";
   public static final String BUILD = "9.9";

   public Airwaves(IEventBus modEventBus, ModContainer modContainer) {
      ModRegistry.register(modEventBus);
      ModSounds.register(modEventBus);
      modEventBus.addListener(Net::register);
      modEventBus.addListener(Airwaves::commonSetup);
      modContainer.registerConfig(Type.COMMON, Config.SPEC);
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.register(ForeignWires.class);
      NeoForge.EVENT_BUS.register(SoundCapture.class);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ClientInit.init(modEventBus);
      }
   }

   /** Advances the radio world simulation on each server level once per second. */
   @SubscribeEvent
   public void levelTick(Post tickEvent) {
      if (tickEvent.getLevel() instanceof ServerLevel serverLevel) {
         if (serverLevel.getGameTime() % 20L == 0L) {
            RadioNetwork.tick(serverLevel);
         }
      }
   }

   /** Registers Create stress costs after Create's registries are available. */
   private static void commonSetup(FMLCommonSetupEvent setupEvent) {
      setupEvent.enqueueWork(() -> {
         RadioMovement.register();
         SimpleRegistry<Block, DoubleSupplier> impacts = BlockStressValues.IMPACTS;

         for (int transmitterIndex = 0; transmitterIndex < ModRegistry.TRANSMITTERS.size(); transmitterIndex++) {
            double stressImpact = TransmitterBE.STRESS[((TransmitterBlock)ModRegistry.TRANSMITTERS.get(transmitterIndex).get()).tier.level - 1];
            impacts.register((Block)ModRegistry.TRANSMITTERS.get(transmitterIndex).get(), (DoubleSupplier)() -> stressImpact);
         }

         impacts.register((Block)ModRegistry.JAMMER.get(), (DoubleSupplier)() -> 8.0);
         impacts.register((Block)ModRegistry.DIMENSIONAL_RELAY.get(), (DoubleSupplier)() -> 16.0);
         impacts.register((Block)ModRegistry.RELAY_TOWER_SMALL.get(), (DoubleSupplier)() -> 12.0);
         impacts.register((Block)ModRegistry.RELAY_TOWER_BIG.get(), (DoubleSupplier)() -> 24.0);
         impacts.register((Block)ModRegistry.RADIO_TOWER.get(), (DoubleSupplier)() -> 64.0);
      });
   }

   @SubscribeEvent
   public void login(PlayerLoggedInEvent e) {
      CompoundTag tag = e.getEntity().getPersistentData();
      if (!tag.getBoolean("airwaves_manual")) {
         tag.putBoolean("airwaves_manual", true);
         e.getEntity().getInventory().add(new ItemStack((ItemLike)ModRegistry.FIELD_MANUAL.get()));
      }
   }

   @SubscribeEvent
   public void lightning(EntityJoinLevelEvent e) {
      if (e.getEntity() instanceof LightningBolt b && e.getLevel() instanceof ServerLevel sl) {
         RadioNetwork.lightning(sl, b.position());
      }
   }

   @SubscribeEvent
   public void serverStopped(ServerStoppedEvent e) {
      RadioNetwork.clear();
      SoundCapture.clear();
      TapeAudio.clear();
      SvcState.USING_SVC.clear();
   }
}
