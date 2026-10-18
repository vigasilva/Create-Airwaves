/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * Block-entity state and server-side behavior for a placed device.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves.be;

import com.airwaves.ModRegistry;
import com.airwaves.ModSounds;
import com.airwaves.block.ReceiverBlock;
import com.airwaves.logic.AntennaUtil;
import com.airwaves.logic.Cables;
import com.airwaves.logic.ClientSinks;
import com.airwaves.logic.RadioNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ReceiverBE extends TunableBE implements AudioSink, CableNode {
   private static final double[][] JACK = new double[][]{{10.0, 5.0, 4.0}, {4.0, 3.0, 3.0}, {3.0, 3.0, 1.0}, {3.0, 3.0, 0.0}};
   public boolean ok;
   public boolean grounded;
   public int crystal;
   public int antennaHeight;
   public List<SpeakerBE> speakers = new ArrayList<>();
   public List<Cables.Seg> paths = new ArrayList<>();
   public long lastPulse;
   public final Map<SpeakerBE, Float> lineQ = new HashMap<>();
   private AntennaUtil.Info info = AntennaUtil.Info.NONE;
   private int lastSig;
   private final LinkSet links = new LinkSet();

   public ReceiverBE(BlockPos p, BlockState s) {
      super(ModRegistry.RECEIVER_BE.get(), p, s);
   }

   protected ReceiverBE(BlockEntityType<?> type, BlockPos p, BlockState s) {
      super(type, p, s);
   }

   public int tier() {
      return this.getBlockState().getBlock() instanceof ReceiverBlock b ? b.tier.level : 1;
   }

   @Override
   public LinkSet links() {
      return this.links;
   }

   @Override
   public BlockPos nodePos() {
      return this.worldPosition;
   }

   @Override
   public int maxLinks(int slot) {
      return 8;
   }

   @Override
   public Vec3 anchor(int slot) {
      return CableNode.facing(this, JACK[this.tier() - 1][0], JACK[this.tier() - 1][1], JACK[this.tier() - 1][2]);
   }

   @Override
   public void cablesChanged() {
      this.sync();
   }

   @Override
   public void onLoad() {
      super.onLoad();
      if (this.level != null && this.level.isClientSide) {
         ClientSinks.SINKS.add(this);
      }
   }

   @Override
   public void setRemoved() {
      ClientSinks.removed(this, this);
      super.setRemoved();
   }

   @Override
   public void refresh() {
      if (this.level != null) {
         BlockState st = this.getBlockState();
         if (st.getBlock() instanceof ReceiverBlock) {
            Direction f = (Direction)st.getValue(ReceiverBlock.FACING);
            BlockPos back = this.worldPosition.relative(f.getOpposite());
            boolean copper = this.level.isLoaded(back) && this.level.getBlockState(back).is(ModRegistry.GROUND_PLANE);
            AntennaUtil.Info onCopper = copper ? AntennaUtil.scan(this.level, back.above(), this.tier()) : AntennaUtil.Info.NONE;
            AntennaUtil.Info onTop = AntennaUtil.scan(this.level, this.worldPosition.above(), this.tier());
            int oldCr = this.crystal;
            int oldAnt = this.antennaHeight;
            boolean oldOk = this.ok;
            boolean oldGnd = this.grounded;
            int t = this.tier();
            if (onCopper.height() > 0 && onCopper.height() >= onTop.height()) {
               this.info = onCopper;
               this.grounded = true;
            } else if (onTop.height() > 0) {
               this.info = onTop;
               this.grounded = false;
            } else {
               this.info = AntennaUtil.Info.NONE;
               this.grounded = false;
            }

            this.antennaHeight = this.info.height();
            this.crystal = (Integer)st.getValue(ReceiverBlock.CRYSTAL);
            this.ok = this.antennaHeight >= 1 && this.crystal > 0;
            if (oldCr != this.crystal || oldAnt != this.antennaHeight || oldOk != this.ok || oldGnd != this.grounded) {
               this.sync();
            }

            if ((Boolean)st.getValue(ReceiverBlock.ACTIVE) != this.ok) {
               this.level
                  .playSound(
                     null,
                     this.worldPosition,
                     this.ok ? (SoundEvent)ModSounds.POWER_ON.get() : (SoundEvent)ModSounds.POWER_OFF.get(),
                     SoundSource.BLOCKS,
                     0.4F,
                     1.15F
                  );
               this.level.setBlock(this.worldPosition, (BlockState)st.setValue(ReceiverBlock.ACTIVE, this.ok), 3);
            }
         }
      }
   }

   @Override
   public double rxGain() {
      double roof = 1.0;
      return AntennaUtil.rxGain(this.info) * (this.grounded ? 1.25 : 1.0) * roof * (this.crystal == 2 ? 1.15 : 1.0);
   }

   @Override
   public Float rxBeam() {
      return this.info.beam();
   }

   @Override
   public double antennaTop() {
      return super.antennaTop() + 0.5 + this.antennaHeight;
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tip, boolean sneaking) {
      super.addToGoggleTooltip(tip, sneaking);
      int cr = this.getBlockState().getBlock() instanceof ReceiverBlock ? (Integer)this.getBlockState().getValue(ReceiverBlock.CRYSTAL) : this.crystal;
      tip.add(Tuned.chamberLine(cr));
      if (cr == 0) {
         tip.add(Component.literal("    no amethyst in the chamber").withStyle(ChatFormatting.GOLD));
      } else if (!this.ok) {
         tip.add(Component.literal("    needs an antenna").withStyle(ChatFormatting.GOLD));
      } else {
         tip.add(
            Component.literal(
                  "    antenna " + this.antennaHeight + " tall" + (this.grounded ? ", grounded" : "") + (this.links.isEmpty() ? ", built-in speaker" : "")
               )
               .withStyle(ChatFormatting.GRAY)
         );
      }

      return true;
   }

   @Override
   public boolean sinkOn() {
      return this.ok;
   }

   @Override
   public boolean ambientOn() {
      return this.ok && this.links.isEmpty();
   }

   @Override
   public BlockPos sinkPos() {
      return this.worldPosition;
   }

   @Override
   public boolean carrier() {
      return this.carrier;
   }

   @Override
   public float quality() {
      return this.quality;
   }

   @Override
   public boolean fm() {
      return this.tuning.fm();
   }

   public int signal() {
      return this.ok && this.carrier ? 1 + Math.round(this.quality * 14.0F) : 0;
   }

   @Override
   protected void afterSignal() {
      if (this.level instanceof ServerLevel lv) {
         for (SpeakerBE s : this.speakers) {
            s.setState(this.ok, this.carrier, this.quality, this.tuning.fm());
         }

         int sig = this.signal();
         if (sig != this.lastSig) {
            this.lastSig = sig;
            lv.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
         }
      }
   }

   public void deliver(ServerLevel lv, UUID talker, byte[] data, float gain) {
      if (this.speakers.isEmpty()) {
         RadioNetwork.send(lv, this.worldPosition, talker, data, this.quality * 0.92F, this.tuning.fm(), gain * 0.6F);
      } else {
         for (SpeakerBE s : this.speakers) {
            RadioNetwork.send(lv, s.getBlockPos(), talker, data, this.quality * this.lineQ.getOrDefault(s, 1.0F), this.tuning.fm(), gain);
         }
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag, Provider r) {
      super.saveAdditional(tag, r);
      this.links.save(tag);
      tag.putBoolean("ok", this.ok);
      tag.putInt("ant", this.antennaHeight);
      tag.putBoolean("gnd", this.grounded);
      tag.putInt("cr", this.crystal);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, Provider r) {
      super.loadAdditional(tag, r);
      this.links.load(tag);
      this.ok = tag.getBoolean("ok");
      this.antennaHeight = tag.getInt("ant");
      this.grounded = tag.getBoolean("gnd");
      this.crystal = tag.getInt("cr");
   }
}
