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
import com.airwaves.logic.AntennaUtil;
import com.airwaves.structure.StructureBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RelayBE extends KineticTunableBE implements CableNode {
   public static final double STRESS_SMALL = 12.0;
   public static final double STRESS_BIG = 24.0;
   public boolean active;
   public boolean relaying;
   public double range;
   public float antQ;
   private final LinkSet links = new LinkSet();

   public RelayBE(BlockPos p, BlockState s) {
      super(ModRegistry.RELAY_BE.get(), p, s);
   }

   public boolean big() {
      return this.getBlockState().getBlock() == ModRegistry.RELAY_TOWER_BIG.get();
   }

   public int height() {
      return this.big() ? 28 : 16;
   }

   public int tier() {
      return this.big() ? 3 : 2;
   }

   @Override
   public double stressImpact() {
      return this.big() ? 24.0 : 12.0;
   }

   @Override
   protected boolean usesPower() {
      return this.active;
   }

   @Override
   public int ratedVolts() {
      return this.big() ? 240 : 120;
   }

   @Override
   public void refresh() {
      if (this.level != null) {
         AntennaUtil.Info info = new AntennaUtil.Info(this.height(), this.tier());
         this.active = this.turning() && !this.level.hasNeighborSignal(this.worldPosition);
         this.range = this.active ? AntennaUtil.range(info, this.tuning.fm()) * (this.big() ? 1.8 : 1.5) * this.powerFactor() * this.altitudeFactor() : 0.0;
         this.antQ = Math.min(1.0F, AntennaUtil.quality(info) + 0.1F);
         this.sync();
      }
   }

   @Override
   public double rxGain() {
      return AntennaUtil.rxGain(new AntennaUtil.Info(this.height(), this.tier())) * 1.2;
   }

   @Override
   public double antennaTop() {
      return this.radioPos().getY() + this.height();
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
      return 4;
   }

   @Override
   public Vec3 anchor(int slot) {
      return this.getBlockState().getBlock() instanceof StructureBlock b
         ? b.def.connector(this.worldPosition, 0, (Axis)this.getBlockState().getValue(StructureBlock.AXIS))
         : Vec3.atCenterOf(this.worldPosition);
   }

   @Override
   public void cablesChanged() {
      this.sync();
   }

   public boolean addToGoggleTooltip(List<Component> tip, boolean sneaking) {
      super.addToGoggleTooltip(tip, sneaking);
      tip.add(Component.literal("    " + (this.big() ? "Big Relay Tower" : "Relay Tower")).withStyle(ChatFormatting.WHITE));
      this.radioTooltip(tip, this.active, this.range);
      if (this.active) {
         tip.add(
            Component.literal(this.relaying ? "    relaying a station" : "    listening, nothing to relay")
               .withStyle(this.relaying ? ChatFormatting.GREEN : ChatFormatting.GRAY)
         );
      }

      return true;
   }

   @Override
   protected void write(CompoundTag tag, Provider r, boolean clientPacket) {
      super.write(tag, r, clientPacket);
      this.links.save(tag);
      tag.putBoolean("active", this.active);
      tag.putBoolean("relaying", this.relaying);
      tag.putDouble("range", this.range);
   }

   @Override
   protected void read(CompoundTag tag, Provider r, boolean clientPacket) {
      super.read(tag, r, clientPacket);
      this.links.load(tag);
      if (clientPacket) {
         this.active = tag.getBoolean("active");
         this.relaying = tag.getBoolean("relaying");
         this.range = tag.getDouble("range");
      }
   }
}
