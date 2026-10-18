/**
 * Readable decompilation of Create: Airwaves from the supplied binary.
 * General mod bootstrap, registry, configuration, or shared type.
 *
 * <p>Names present in bytecode are preserved. Local names and comments that
 * did not survive compilation cannot be recovered with certainty; verify
 * behavior against the original bytecode before changing game logic.</p>
 */
package com.airwaves;

import com.airwaves.be.CircuitBoxBE;
import com.airwaves.be.CircuitWorkbenchBE;
import com.airwaves.be.CreativeAntennaBE;
import com.airwaves.be.CreativeReceiverBE;
import com.airwaves.be.CreativeTransmitterBE;
import com.airwaves.be.DimensionalRelayBE;
import com.airwaves.be.InsulatorBE;
import com.airwaves.be.JammerBE;
import com.airwaves.be.MicrophoneBE;
import com.airwaves.be.RadioBE;
import com.airwaves.be.RadioConsoleBE;
import com.airwaves.be.ReceiverBE;
import com.airwaves.be.RelayBE;
import com.airwaves.be.SpeakerBE;
import com.airwaves.be.TapePlayerBE;
import com.airwaves.be.TransmitterBE;
import com.airwaves.be.YagiAntennaBE;
import com.airwaves.block.AntennaBlock;
import com.airwaves.block.CircuitBoxBlock;
import com.airwaves.block.CircuitWorkbenchBlock;
import com.airwaves.block.ConnectedBlock;
import com.airwaves.block.CreativeAntennaBlock;
import com.airwaves.block.CreativeReceiverBlock;
import com.airwaves.block.CreativeTransmitterBlock;
import com.airwaves.block.DimensionalRelayBlock;
import com.airwaves.block.InsulatorBlock;
import com.airwaves.block.JammerBlock;
import com.airwaves.block.MastBlock;
import com.airwaves.block.MicrophoneBlock;
import com.airwaves.block.RadioBlock;
import com.airwaves.block.RadioConsoleBlock;
import com.airwaves.block.ReceiverBlock;
import com.airwaves.block.SpeakerBlock;
import com.airwaves.block.TapePlayerBlock;
import com.airwaves.block.TransmitterBlock;
import com.airwaves.block.TuningTableBlock;
import com.airwaves.block.YagiAntennaBlock;
import com.airwaves.cable.CableType;
import com.airwaves.compat.ElectricCompat;
import com.airwaves.compat.cee.CEECompat;
import com.airwaves.item.BoardItem;
import com.airwaves.item.CableItem;
import com.airwaves.item.FieldManualItem;
import com.airwaves.item.FrequencyCardItem;
import com.airwaves.item.MastItem;
import com.airwaves.item.PortableRadioItem;
import com.airwaves.item.TapeItem;
import com.airwaves.item.TunableBlockItem;
import com.airwaves.logic.BoardDesign;
import com.airwaves.logic.BoardLayout;
import com.airwaves.logic.TapeData;
import com.airwaves.menu.CircuitWorkbenchMenu;
import com.airwaves.menu.TapePlayerMenu;
import com.airwaves.menu.TuningTableMenu;
import com.airwaves.structure.KineticStructureBlock;
import com.airwaves.structure.StructureBlock;
import com.airwaves.structure.StructureItem;
import com.airwaves.structure.TowerCoreBE;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class ModRegistry {
   private static final String ID = "airwaves";
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("airwaves");
   public static final Items ITEMS = DeferredRegister.createItems("airwaves");
   public static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "airwaves");
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "airwaves");
   public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "airwaves");
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "airwaves");
   public static final TagKey<Block> GROUND_PLANE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("airwaves", "ground_plane"));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Tuning>> TUNING = COMPONENTS.register(
      "tuning", () -> DataComponentType.<Tuning>builder().persistent(Tuning.CODEC).networkSynchronized(Tuning.STREAM).build()
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoardLayout>> BOARD_LAYOUT = COMPONENTS.register(
      "board_layout", () -> DataComponentType.<BoardLayout>builder().persistent(BoardLayout.CODEC).networkSynchronized(BoardLayout.STREAM).build()
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WIRE_START = COMPONENTS.register(
      "wire_start", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build()
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WIRE_SLOT = COMPONENTS.register(
      "wire_slot", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<TapeData>> TAPE = COMPONENTS.register(
      "tape", () -> DataComponentType.<TapeData>builder().persistent(TapeData.CODEC).networkSynchronized(TapeData.STREAM).build()
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POWERED = COMPONENTS.register(
      "powered", () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()
   );
   public static final DeferredBlock<RadioBlock> RADIO = BLOCKS.register("radio", () -> new RadioBlock(metal()));
   public static final DeferredBlock<TuningTableBlock> TUNING_TABLE = BLOCKS.register("tuning_table", () -> new TuningTableBlock(wood().strength(2.5F)));
   public static final List<DeferredBlock<TransmitterBlock>> TRANSMITTERS = new ArrayList<>();
   public static final List<DeferredBlock<ReceiverBlock>> RECEIVERS = new ArrayList<>();
   public static final DeferredBlock<AntennaBlock.Small> ANTENNA = BLOCKS.register("antenna", () -> new AntennaBlock.Small(mast(1.5F)));
   public static final DeferredBlock<MicrophoneBlock> MICROPHONE = BLOCKS.register("microphone", () -> new MicrophoneBlock(metal().strength(1.0F)));
   public static final DeferredBlock<SpeakerBlock> SPEAKER = BLOCKS.register("speaker", () -> new SpeakerBlock(wood().strength(1.5F)));
   public static final DeferredBlock<TapePlayerBlock> TAPE_PLAYER = BLOCKS.register("tape_player", () -> new TapePlayerBlock(metal().strength(1.5F)));
   public static final DeferredBlock<InsulatorBlock> INSULATOR = BLOCKS.register("insulator", () -> new InsulatorBlock(wood().strength(1.0F)));
   public static final DeferredBlock<JammerBlock> JAMMER = BLOCKS.register(
      "jammer", () -> ElectricCompat.jammer(metal().strength(3.0F).lightLevel(st -> st.getValue(JammerBlock.POWERED) ? 6 : 0))
   );
   public static final DeferredBlock<CircuitBoxBlock> CIRCUIT_BOX = BLOCKS.register(
      "circuit_box", () -> new CircuitBoxBlock(metal().strength(1.5F, 6.0F).lightLevel(st -> st.getValue(CircuitBoxBlock.LIT) ? 5 : 0))
   );
   public static final DeferredBlock<DimensionalRelayBlock> DIMENSIONAL_RELAY = BLOCKS.register(
      "dimensional_relay",
      () -> ElectricCompat.dimensionalRelay(metal().strength(4.0F, 1200.0F).lightLevel(st -> st.getValue(DimensionalRelayBlock.ACTIVE) ? 9 : 2))
   );
   public static final DeferredBlock<CreativeTransmitterBlock> CREATIVE_TRANSMITTER = BLOCKS.register(
      "creative_transmitter", () -> new CreativeTransmitterBlock(metal().strength(-1.0F, 3600000.0F).noLootTable())
   );
   public static final DeferredBlock<CreativeReceiverBlock> CREATIVE_RECEIVER = BLOCKS.register(
      "creative_receiver", () -> new CreativeReceiverBlock(metal().strength(-1.0F, 3600000.0F).noLootTable())
   );
   public static final DeferredBlock<CreativeAntennaBlock> CREATIVE_ANTENNA = BLOCKS.register(
      "creative_antenna", () -> new CreativeAntennaBlock(mast(1.0F).strength(-1.0F, 3600000.0F).noLootTable())
   );
   public static final DeferredBlock<RadioConsoleBlock> RADIO_CONSOLE = BLOCKS.register("radio_console", () -> new RadioConsoleBlock(metal().strength(2.0F)));
   public static final DeferredBlock<CircuitWorkbenchBlock> CIRCUIT_WORKBENCH = BLOCKS.register(
      "circuit_workbench", () -> new CircuitWorkbenchBlock(wood().strength(2.5F))
   );
   public static final DeferredBlock<YagiAntennaBlock> YAGI_ANTENNA = BLOCKS.register("yagi_antenna", () -> new YagiAntennaBlock(mast(1.5F)));
   public static final DeferredBlock<DropExperienceBlock> SILVER_ORE = BLOCKS.register(
      "silver_ore", () -> new DropExperienceBlock(ConstantInt.of(0), ore(false))
   );
   public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_SILVER_ORE = BLOCKS.register(
      "deepslate_silver_ore", () -> new DropExperienceBlock(ConstantInt.of(0), ore(true))
   );
   public static final DeferredBlock<Block> SILVER_BLOCK = BLOCKS.register(
      "silver_block",
      () -> new ConnectedBlock(Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<Block> RADIO_CASING = BLOCKS.register(
      "radio_casing", () -> new Block(Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F).sound(SoundType.COPPER))
   );
   public static final DeferredBlock<StructureBlock> RELAY_TOWER_SMALL = BLOCKS.register(
      "relay_tower_small", () -> StructureBlock.createKinetic("relay_tower_small", structure(mast(4.0F)), RelayBE::new)
   );
   public static final DeferredBlock<StructureBlock> RELAY_TOWER_BIG = BLOCKS.register(
      "relay_tower_big", () -> StructureBlock.createKinetic("relay_tower_big", structure(mast(5.0F)), RelayBE::new)
   );
   public static final DeferredBlock<StructureBlock> RADIO_TOWER = BLOCKS.register(
      "radio_tower",
      () -> StructureBlock.createKinetic(
         "radio_tower",
         structure(mast(6.0F)).lightLevel(st -> st.hasProperty(TowerCoreBE.ACTIVE) && st.getValue(TowerCoreBE.ACTIVE) ? 7 : 0),
         TowerCoreBE::new,
         TowerCoreBE.CRYSTAL,
         TowerCoreBE.ACTIVE
      )
   );
   public static final DeferredItem<CableItem> COPPER_WIRE = ITEMS.register(
      "copper_wire", () -> new CableItem(CableType.COPPER, new net.minecraft.world.item.Item.Properties())
   );
   public static final DeferredItem<FrequencyCardItem> FREQUENCY_CARD = ITEMS.register(
      "frequency_card", () -> new FrequencyCardItem(new net.minecraft.world.item.Item.Properties().stacksTo(16))
   );
   public static final DeferredItem<Item> SILVER_INGOT = part("silver_ingot");
   public static final DeferredItem<Item> SILVER_NUGGET = part("silver_nugget");
   public static final DeferredItem<Item> RAW_SILVER = part("raw_silver");
   public static final DeferredItem<Item> SILVER_SHEET = part("silver_sheet");
   public static final DeferredItem<Item> SIGNAL_MECHANISM = part("signal_mechanism");
   public static final DeferredItem<Item> INCOMPLETE_CIRCUIT_BOARD = part("incomplete_circuit_board");
   public static final DeferredItem<Item> INCOMPLETE_SIGNAL_MECHANISM = part("incomplete_signal_mechanism");
   public static final DeferredItem<FieldManualItem> FIELD_MANUAL = ITEMS.register(
      "field_manual", () -> new FieldManualItem(new net.minecraft.world.item.Item.Properties().stacksTo(1))
   );
   public static final DeferredItem<Item> COPPER_COIL = part("copper_coil");
   public static final DeferredItem<Item> CRYSTAL_OSCILLATOR = part("crystal_oscillator");
   public static final DeferredItem<Item> CIRCUIT_BOARD = part("circuit_board");
   public static final DeferredItem<Item> SPEAKER_CONE = part("speaker_cone");
   public static final DeferredItem<Item> MIC_CAPSULE = part("mic_capsule");
   public static final DeferredItem<Item> MEMORY_CHIP = part("memory_chip");
   public static final DeferredItem<Item> COUNTER_CHIP = part("counter_chip");
   public static final DeferredItem<Item> PULSE_CHIP = part("pulse_chip");
   public static final DeferredItem<Item> LIGHT_SENSOR = part("light_sensor");
   public static final DeferredItem<Item> RAIN_SENSOR = part("rain_sensor");
   public static final DeferredItem<Item> MOTION_SENSOR = part("motion_sensor");
   public static final DeferredItem<Item> RESISTOR = part("resistor");
   public static final DeferredItem<Item> CAPACITOR = part("capacitor");
   public static final DeferredItem<Item> LED = part("led");
   public static final DeferredItem<Item> TRANSISTOR = part("transistor");
   public static final Map<BoardDesign, DeferredItem<Item>> BOARDS = new EnumMap<>(BoardDesign.class);
   public static final DeferredItem<TunableBlockItem> RADIO_ITEM;
   public static final DeferredItem<PortableRadioItem> PORTABLE_RADIO;
   public static final DeferredItem<TunableBlockItem> CREATIVE_TRANSMITTER_ITEM;
   public static final DeferredItem<TunableBlockItem> CREATIVE_RECEIVER_ITEM;
   public static final DeferredItem<BlockItem> CREATIVE_ANTENNA_ITEM;
   public static final DeferredItem<TunableBlockItem> RADIO_CONSOLE_ITEM;
   public static final DeferredItem<BlockItem> TUNING_TABLE_ITEM;
   public static final List<DeferredItem<TunableBlockItem>> TRANSMITTER_ITEMS;
   public static final List<DeferredItem<TunableBlockItem>> RECEIVER_ITEMS;
   public static final DeferredItem<MastItem> ANTENNA_ITEM;
   public static final DeferredItem<BlockItem> YAGI_ANTENNA_ITEM;
   public static final DeferredItem<BlockItem> SILVER_ORE_ITEM;
   public static final DeferredItem<BlockItem> DEEPSLATE_SILVER_ORE_ITEM;
   public static final DeferredItem<BlockItem> SILVER_BLOCK_ITEM;
   public static final DeferredItem<BlockItem> RADIO_CASING_ITEM;
   public static final DeferredItem<BlockItem> CIRCUIT_WORKBENCH_ITEM;
   public static final DeferredItem<StructureItem> RELAY_TOWER_SMALL_ITEM;
   public static final DeferredItem<StructureItem> RELAY_TOWER_BIG_ITEM;
   public static final DeferredItem<StructureItem> RADIO_TOWER_ITEM;
   public static final DeferredItem<BlockItem> MICROPHONE_ITEM;
   public static final DeferredItem<BlockItem> SPEAKER_ITEM;
   public static final DeferredItem<BlockItem> TAPE_PLAYER_ITEM;
   public static final DeferredItem<TapeItem> TAPE_ITEM;
   public static final DeferredItem<BlockItem> INSULATOR_ITEM;
   public static final DeferredItem<BlockItem> JAMMER_ITEM;
   public static final DeferredItem<TunableBlockItem> CIRCUIT_BOX_ITEM;
   public static final DeferredItem<BlockItem> DIMENSIONAL_RELAY_ITEM;
   public static final Supplier<BlockEntityType<RadioBE>> RADIO_BE;
   public static final Supplier<BlockEntityType<CreativeTransmitterBE>> CREATIVE_TX_BE;
   public static final Supplier<BlockEntityType<CreativeReceiverBE>> CREATIVE_RX_BE;
   public static final Supplier<BlockEntityType<KineticStructureBlock.TowerShaftBE>> TOWER_SHAFT_BE;
   public static final Supplier<BlockEntityType<CreativeAntennaBE>> CREATIVE_ANTENNA_BE;
   public static final Supplier<BlockEntityType<RadioConsoleBE>> RADIO_CONSOLE_BE;
   public static final Supplier<BlockEntityType<TransmitterBE>> TRANSMITTER_BE;
   public static final Supplier<BlockEntityType<ReceiverBE>> RECEIVER_BE;
   public static final Supplier<BlockEntityType<RelayBE>> RELAY_BE;
   public static final Supplier<BlockEntityType<TowerCoreBE>> TOWER_BE;
   public static final Supplier<BlockEntityType<MicrophoneBE>> MIC_BE;
   public static final Supplier<BlockEntityType<TapePlayerBE>> TAPE_PLAYER_BE;
   public static final Supplier<BlockEntityType<SpeakerBE>> SPEAKER_BE;
   public static final Supplier<BlockEntityType<YagiAntennaBE>> YAGI_BE;
   public static final Supplier<BlockEntityType<InsulatorBE>> INSULATOR_BE;
   public static final Supplier<BlockEntityType<JammerBE>> JAMMER_BE;
   public static final Supplier<BlockEntityType<CircuitWorkbenchBE>> WORKBENCH_BE;
   public static final Supplier<BlockEntityType<CircuitBoxBE>> CIRCUIT_BOX_BE;
   public static final Supplier<BlockEntityType<DimensionalRelayBE>> DIM_RELAY_BE;
   public static final Supplier<MenuType<TuningTableMenu>> TUNING_MENU;
   public static final Supplier<MenuType<TapePlayerMenu>> TAPE_MENU;
   public static final Supplier<MenuType<CircuitWorkbenchMenu>> WORKBENCH_MENU;
   public static final Supplier<CreativeModeTab> TAB;

   private static Properties metal() {
      return Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops();
   }

   private static Properties mast(float s) {
      return metal().strength(s).isViewBlocking((a, b, c) -> false).isSuffocating((a, b, c) -> false).isRedstoneConductor((a, b, c) -> false);
   }

   private static Properties wood() {
      return Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion();
   }

   private static Properties ore(boolean deep) {
      return Properties.of()
         .mapColor(deep ? MapColor.DEEPSLATE : MapColor.STONE)
         .strength(deep ? 4.5F : 3.0F, 3.0F)
         .sound(deep ? SoundType.DEEPSLATE : SoundType.STONE)
         .requiresCorrectToolForDrops();
   }

   private static Properties structure(Properties p) {
      return p.noOcclusion().isViewBlocking((a, b, c) -> false).isSuffocating((a, b, c) -> false).isRedstoneConductor((a, b, c) -> false);
   }

   private static DeferredItem<Item> part(String n) {
      return ITEMS.register(n, () -> new Item(new net.minecraft.world.item.Item.Properties()));
   }

   public static Item electronTube() {
      return (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "electron_tube"));
   }

   public static Item board(BoardDesign d) {
      return (Item)BOARDS.get(d).get();
   }

   private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> b) {
      return ITEMS.register(b.getId().getPath(), () -> new BlockItem((Block)b.get(), new net.minecraft.world.item.Item.Properties()));
   }

   private static Block[] all(List<? extends DeferredBlock<? extends Block>> l) {
      return l.stream().map(DeferredHolder::get).toArray(Block[]::new);
   }

   public static void register(IEventBus bus) {
      ElectricCompat.register();
      if (Config.ceeLoaded()) {
         CEECompat.register(bus);
      }

      COMPONENTS.register(bus);
      BLOCKS.register(bus);
      ITEMS.register(bus);
      BES.register(bus);
      MENUS.register(bus);
      TABS.register(bus);
   }

   private ModRegistry() {
   }

   static {
      TRANSMITTERS.add(BLOCKS.register("transmitter", () -> ElectricCompat.transmitter(Tier.GIANT, metal().strength(6.0F))));
      RECEIVERS.add(BLOCKS.register("receiver", () -> new ReceiverBlock(Tier.GIANT, metal().strength(5.5F))));

      for (BoardDesign d : BoardDesign.values()) {
         BOARDS.put(d, ITEMS.register(d.id, () -> new BoardItem(d, new net.minecraft.world.item.Item.Properties())));
      }

      RADIO_ITEM = ITEMS.register("radio", () -> new TunableBlockItem((Block)RADIO.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1)));
      PORTABLE_RADIO = ITEMS.register("portable_radio", () -> new PortableRadioItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
      CREATIVE_TRANSMITTER_ITEM = ITEMS.register(
         "creative_transmitter",
         () -> new TunableBlockItem((Block)CREATIVE_TRANSMITTER.get(), new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC))
      );
      CREATIVE_RECEIVER_ITEM = ITEMS.register(
         "creative_receiver", () -> new TunableBlockItem((Block)CREATIVE_RECEIVER.get(), new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC))
      );
      CREATIVE_ANTENNA_ITEM = ITEMS.register(
         "creative_antenna", () -> new BlockItem((Block)CREATIVE_ANTENNA.get(), new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC))
      );
      RADIO_CONSOLE_ITEM = ITEMS.register(
         "radio_console", () -> new TunableBlockItem((Block)RADIO_CONSOLE.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1))
      );
      TUNING_TABLE_ITEM = ITEMS.register("tuning_table", () -> new BlockItem((Block)TUNING_TABLE.get(), new net.minecraft.world.item.Item.Properties()));
      TRANSMITTER_ITEMS = new ArrayList<>();
      RECEIVER_ITEMS = new ArrayList<>();
      TRANSMITTER_ITEMS.add(
         ITEMS.register("transmitter", () -> new TunableBlockItem((Block)TRANSMITTERS.get(0).get(), new net.minecraft.world.item.Item.Properties()))
      );
      RECEIVER_ITEMS.add(ITEMS.register("receiver", () -> new TunableBlockItem((Block)RECEIVERS.get(0).get(), new net.minecraft.world.item.Item.Properties())));
      ANTENNA_ITEM = ITEMS.register("antenna", () -> new MastItem((MastBlock)ANTENNA.get(), new net.minecraft.world.item.Item.Properties()));
      YAGI_ANTENNA_ITEM = blockItem(YAGI_ANTENNA);
      SILVER_ORE_ITEM = blockItem(SILVER_ORE);
      DEEPSLATE_SILVER_ORE_ITEM = blockItem(DEEPSLATE_SILVER_ORE);
      SILVER_BLOCK_ITEM = blockItem(SILVER_BLOCK);
      RADIO_CASING_ITEM = blockItem(RADIO_CASING);
      CIRCUIT_WORKBENCH_ITEM = blockItem(CIRCUIT_WORKBENCH);
      RELAY_TOWER_SMALL_ITEM = ITEMS.register(
         "relay_tower_small",
         () -> new StructureItem.Tuned((StructureBlock)RELAY_TOWER_SMALL.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1))
      );
      RELAY_TOWER_BIG_ITEM = ITEMS.register(
         "relay_tower_big", () -> new StructureItem.Tuned((StructureBlock)RELAY_TOWER_BIG.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1))
      );
      RADIO_TOWER_ITEM = ITEMS.register(
         "radio_tower", () -> new StructureItem.Tuned((StructureBlock)RADIO_TOWER.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1))
      );
      MICROPHONE_ITEM = ITEMS.register("microphone", () -> new BlockItem((Block)MICROPHONE.get(), new net.minecraft.world.item.Item.Properties()));
      SPEAKER_ITEM = ITEMS.register("speaker", () -> new BlockItem((Block)SPEAKER.get(), new net.minecraft.world.item.Item.Properties()));
      TAPE_PLAYER_ITEM = ITEMS.register("tape_player", () -> new BlockItem((Block)TAPE_PLAYER.get(), new net.minecraft.world.item.Item.Properties()));
      TAPE_ITEM = ITEMS.register("tape", () -> new TapeItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
      INSULATOR_ITEM = ITEMS.register("insulator", () -> new BlockItem((Block)INSULATOR.get(), new net.minecraft.world.item.Item.Properties()));
      JAMMER_ITEM = ITEMS.register("jammer", () -> new BlockItem((Block)JAMMER.get(), new net.minecraft.world.item.Item.Properties()));
      CIRCUIT_BOX_ITEM = ITEMS.register("circuit_box", () -> new TunableBlockItem((Block)CIRCUIT_BOX.get(), new net.minecraft.world.item.Item.Properties()));
      DIMENSIONAL_RELAY_ITEM = ITEMS.register(
         "dimensional_relay", () -> new BlockItem((Block)DIMENSIONAL_RELAY.get(), new net.minecraft.world.item.Item.Properties())
      );
      RADIO_BE = BES.register("radio", () -> Builder.of(RadioBE::new, new Block[]{(Block)RADIO.get()}).build(null));
      CREATIVE_TX_BE = BES.register(
         "creative_transmitter", () -> Builder.of(CreativeTransmitterBE::new, new Block[]{(Block)CREATIVE_TRANSMITTER.get()}).build(null)
      );
      CREATIVE_RX_BE = BES.register("creative_receiver", () -> Builder.of(CreativeReceiverBE::new, new Block[]{(Block)CREATIVE_RECEIVER.get()}).build(null));
      TOWER_SHAFT_BE = BES.register(
         "tower_shaft",
         () -> Builder.of(
               KineticStructureBlock.TowerShaftBE::new, new Block[]{(Block)RADIO_TOWER.get(), (Block)RELAY_TOWER_SMALL.get(), (Block)RELAY_TOWER_BIG.get()}
            )
            .build(null)
      );
      CREATIVE_ANTENNA_BE = BES.register("creative_antenna", () -> Builder.of(CreativeAntennaBE::new, new Block[]{(Block)CREATIVE_ANTENNA.get()}).build(null));
      RADIO_CONSOLE_BE = BES.register("radio_console", () -> Builder.of(RadioConsoleBE::new, new Block[]{(Block)RADIO_CONSOLE.get()}).build(null));
      TRANSMITTER_BE = BES.register("transmitter", () -> Builder.of(ElectricCompat.transmitterBE(), all(TRANSMITTERS)).build(null));
      RECEIVER_BE = BES.register("receiver", () -> Builder.of(ReceiverBE::new, all(RECEIVERS)).build(null));
      RELAY_BE = BES.register("relay", () -> Builder.of(RelayBE::new, new Block[]{(Block)RELAY_TOWER_SMALL.get(), (Block)RELAY_TOWER_BIG.get()}).build(null));
      TOWER_BE = BES.register("radio_tower", () -> Builder.of(TowerCoreBE::new, new Block[]{(Block)RADIO_TOWER.get()}).build(null));
      MIC_BE = BES.register("microphone", () -> Builder.of(MicrophoneBE::new, new Block[]{(Block)MICROPHONE.get()}).build(null));
      TAPE_PLAYER_BE = BES.register("tape_player", () -> Builder.of(TapePlayerBE::new, new Block[]{(Block)TAPE_PLAYER.get()}).build(null));
      SPEAKER_BE = BES.register("speaker", () -> Builder.of(SpeakerBE::new, new Block[]{(Block)SPEAKER.get()}).build(null));
      YAGI_BE = BES.register(
         "yagi_antenna", () -> Builder.of((p, st) -> new YagiAntennaBE(ModRegistry.YAGI_BE.get(), p, st), new Block[]{(Block)YAGI_ANTENNA.get()}).build(null)
      );
      INSULATOR_BE = BES.register("insulator", () -> Builder.of(InsulatorBE::new, new Block[]{(Block)INSULATOR.get()}).build(null));
      JAMMER_BE = BES.register("jammer", () -> Builder.of(ElectricCompat.jammerBE(), new Block[]{(Block)JAMMER.get()}).build(null));
      WORKBENCH_BE = BES.register("circuit_workbench", () -> Builder.of(CircuitWorkbenchBE::new, new Block[]{(Block)CIRCUIT_WORKBENCH.get()}).build(null));
      CIRCUIT_BOX_BE = BES.register("circuit_box", () -> Builder.of(CircuitBoxBE::new, new Block[]{(Block)CIRCUIT_BOX.get()}).build(null));
      DIM_RELAY_BE = BES.register(
         "dimensional_relay", () -> Builder.of(ElectricCompat.dimensionalRelayBE(), new Block[]{(Block)DIMENSIONAL_RELAY.get()}).build(null)
      );
      TUNING_MENU = MENUS.register("tuning_table", () -> IMenuTypeExtension.create((id, inv, buf) -> new TuningTableMenu(id, inv)));
      TAPE_MENU = MENUS.register("tape_player", () -> IMenuTypeExtension.create((id, inv, buf) -> new TapePlayerMenu(id, inv)));
      WORKBENCH_MENU = MENUS.register("circuit_workbench", () -> IMenuTypeExtension.create((id, inv, buf) -> new CircuitWorkbenchMenu(id, inv)));
      TAB = TABS.register(
         "main",
         () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.airwaves"))
            .icon(() -> new ItemStack((ItemLike)RADIO_ITEM.get()))
            .displayItems((p, out) -> {
               out.accept((ItemLike)FIELD_MANUAL.get());
               out.accept((ItemLike)RADIO_ITEM.get());
               out.accept((ItemLike)PORTABLE_RADIO.get());
               out.accept((ItemLike)TUNING_TABLE_ITEM.get());
               out.accept((ItemLike)MICROPHONE_ITEM.get());
               out.accept((ItemLike)TAPE_PLAYER_ITEM.get());
               out.accept((ItemLike)TAPE_ITEM.get());
               out.accept((ItemLike)SPEAKER_ITEM.get());
               TRANSMITTER_ITEMS.forEach(i -> out.accept((ItemLike)i.get()));
               RECEIVER_ITEMS.forEach(i -> out.accept((ItemLike)i.get()));
               out.accept((ItemLike)ANTENNA_ITEM.get());
               out.accept((ItemLike)YAGI_ANTENNA_ITEM.get());
               out.accept((ItemLike)RELAY_TOWER_SMALL_ITEM.get());
               out.accept((ItemLike)RELAY_TOWER_BIG_ITEM.get());
               out.accept((ItemLike)RADIO_TOWER_ITEM.get());
               out.accept((ItemLike)JAMMER_ITEM.get());
               out.accept((ItemLike)DIMENSIONAL_RELAY_ITEM.get());
               out.accept((ItemLike)CIRCUIT_BOX_ITEM.get());
               out.accept((ItemLike)RADIO_CONSOLE_ITEM.get());
               out.accept((ItemLike)COPPER_WIRE.get());
               out.accept((ItemLike)INSULATOR_ITEM.get());
               out.accept((ItemLike)FREQUENCY_CARD.get());
               out.accept((ItemLike)RADIO_CASING_ITEM.get());
               out.accept((ItemLike)CIRCUIT_WORKBENCH_ITEM.get());
               out.accept((ItemLike)CIRCUIT_BOARD.get());
               out.accept((ItemLike)RESISTOR.get());
               out.accept((ItemLike)CAPACITOR.get());
               out.accept((ItemLike)LED.get());
               out.accept((ItemLike)TRANSISTOR.get());
               out.accept((ItemLike)MEMORY_CHIP.get());
               out.accept((ItemLike)COUNTER_CHIP.get());
               out.accept((ItemLike)PULSE_CHIP.get());
               out.accept((ItemLike)LIGHT_SENSOR.get());
               out.accept((ItemLike)RAIN_SENSOR.get());
               out.accept((ItemLike)MOTION_SENSOR.get());
               out.accept((ItemLike)COPPER_COIL.get());
               out.accept((ItemLike)CRYSTAL_OSCILLATOR.get());
               BOARDS.values().forEach(b -> out.accept((ItemLike)b.get()));
               out.accept((ItemLike)SIGNAL_MECHANISM.get());
               out.accept((ItemLike)SPEAKER_CONE.get());
               out.accept((ItemLike)MIC_CAPSULE.get());
               out.accept((ItemLike)SILVER_ORE_ITEM.get());
               out.accept((ItemLike)DEEPSLATE_SILVER_ORE_ITEM.get());
               out.accept((ItemLike)RAW_SILVER.get());
               out.accept((ItemLike)SILVER_INGOT.get());
               out.accept((ItemLike)SILVER_NUGGET.get());
               out.accept((ItemLike)SILVER_SHEET.get());
               out.accept((ItemLike)SILVER_BLOCK_ITEM.get());
               out.accept((ItemLike)CREATIVE_TRANSMITTER_ITEM.get());
               out.accept((ItemLike)CREATIVE_RECEIVER_ITEM.get());
               out.accept((ItemLike)CREATIVE_ANTENNA_ITEM.get());
            })
            .build()
      );
   }
}
