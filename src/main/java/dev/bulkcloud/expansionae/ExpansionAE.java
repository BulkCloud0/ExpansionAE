package dev.bulkcloud.expansionae;

import appeng.api.config.Upgrades;
import appeng.core.Api;
import appeng.items.parts.PartItem;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

@Mod(ExpansionAE.ID)
public final class ExpansionAE {
    public static final String ID = "expansionae";
    public static final ItemGroup TAB = new ItemGroup(ID) {
        @Override public ItemStack createIcon() { return new ItemStack(PROVIDER.get()); }
    };
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, ID);
    public static final DeferredRegister<net.minecraft.fluid.Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, ID);

    public static final RegistryObject<ExpandedInterfaceBlock> PROVIDER = BLOCKS.register("ex_pattern_provider", () -> {
        ExpandedInterfaceBlock block = new ExpandedInterfaceBlock();
        block.setTileEntity(ExpandedInterfaceTile.class, ExpansionAE::newProvider);
        return block;
    });
    public static final RegistryObject<ExpandedInterfaceBlock> INTERFACE = BLOCKS.register("ex_interface", () -> {
        ExpandedInterfaceBlock block = new ExpandedInterfaceBlock();
        block.setTileEntity(ExpandedInterfaceTile.class, ExpansionAE::newInterface);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedInterfaceTile>> PROVIDER_TILE = TILES.register("ex_pattern_provider",
            () -> TileEntityType.Builder.create(ExpansionAE::newProvider, PROVIDER.get()).build(null));
    public static final RegistryObject<TileEntityType<ExpandedInterfaceTile>> INTERFACE_TILE = TILES.register("ex_interface",
            () -> TileEntityType.Builder.create(ExpansionAE::newInterface, INTERFACE.get()).build(null));
    public static final RegistryObject<Item> PROVIDER_ITEM = ITEMS.register("ex_pattern_provider", () -> new BlockItem(PROVIDER.get(), props()));
    public static final RegistryObject<Item> INTERFACE_ITEM = ITEMS.register("ex_interface", () -> new BlockItem(INTERFACE.get(), props()));
    public static final RegistryObject<Item> WATER_CELL = ITEMS.register("infinity_water_cell", () -> new Item(props().maxStackSize(1)));
    public static final RegistryObject<Item> COBBLE_CELL = ITEMS.register("infinity_cobblestone_cell", () -> new Item(props().maxStackSize(1)));
    public static final RegistryObject<Item> IMPORT_BUS = ITEMS.register("ex_import_bus", () -> new PartItem<>(props(), FastImportBus::new));
    public static final RegistryObject<Item> EXPORT_BUS = ITEMS.register("ex_export_bus", () -> new PartItem<>(props(), FastExportBus::new));
    public static final RegistryObject<Item> IMPORT_EXPORT_BUS = ITEMS.register("import_export_bus", () ->
            new PartItem<>(props(), ImportExportBus::new));
    public static final RegistryObject<Item> STOCK_EXPORT_BUS = ITEMS.register("stock_export_bus", () ->
            new PartItem<>(props(), StockExportBus::new));
    public static final RegistryObject<Item> ADVANCED_IO_BUS = ITEMS.register("advanced_io_bus", () ->
            new PartItem<>(props(), AdvancedIOBus::new));
    public static final RegistryObject<Item> THRESHOLD_EXPORT_BUS = ITEMS.register("threshold_export_bus", () ->
            new PartItem<>(props(), ThresholdExportBus::new));
    public static final RegistryObject<Item> PRECISE_EXPORT_BUS = ITEMS.register("precise_export_bus", () ->
            new PartItem<>(props(), PreciseExportBus::new));
    public static final RegistryObject<Item> MOD_EXPORT_BUS = ITEMS.register("mod_export_bus", () ->
            new PartItem<>(props(), ModExportBus::new));
    public static final RegistryObject<Item> TAG_EXPORT_BUS = ITEMS.register("tag_export_bus", () ->
            new PartItem<>(props(), TagExportBus::new));
    public static final RegistryObject<Item> MOD_STORAGE_BUS = ITEMS.register("mod_storage_bus", () ->
            new PartItem<>(props(), ModStorageBus::new));
    public static final RegistryObject<Item> TAG_STORAGE_BUS = ITEMS.register("tag_storage_bus", () ->
            new PartItem<>(props(), TagStorageBus::new));
    public static final RegistryObject<Item> PRECISE_STORAGE_BUS = ITEMS.register("precise_storage_bus", () ->
            new PartItem<>(props(), PreciseStorageBus::new));
    public static final RegistryObject<Item> THRESHOLD_LEVEL_EMITTER = ITEMS.register("threshold_level_emitter", () ->
            new PartItem<>(props(), ThresholdLevelEmitter::new));
    public static final RegistryObject<Item> THROUGHPUT_MONITOR = ITEMS.register("throughput_monitor", () ->
            new PartItem<>(props(), ThroughputMonitorPart::new));
    public static final RegistryObject<Item> THROUGHPUT_MONITOR_CONFIGURATOR = ITEMS.register(
            "throughput_monitor_configurator", () -> new Item(props().maxStackSize(1)));

    // ExtendedAE Wireless Connector pair bridge and binding tool.
    public static final RegistryObject<WirelessConnectorBlock> WIRELESS_CONNECTOR = BLOCKS.register("wireless_connector", () -> {
        WirelessConnectorBlock block = new WirelessConnectorBlock();
        block.setTileEntity(WirelessConnectorTile.class, ExpansionAE::newWirelessConnector);
        return block;
    });
    public static final RegistryObject<TileEntityType<WirelessConnectorTile>> WIRELESS_CONNECTOR_TILE =
            TILES.register("wireless_connector", () -> TileEntityType.Builder
                    .create(ExpansionAE::newWirelessConnector, WIRELESS_CONNECTOR.get()).build(null));
    public static final RegistryObject<Item> WIRELESS_CONNECTOR_ITEM = ITEMS.register("wireless_connector",
            () -> new BlockItem(WIRELESS_CONNECTOR.get(), props()));
    public static final RegistryObject<Item> WIRELESS_TOOL = ITEMS.register("wireless_tool",
            () -> new WirelessLinkToolItem(props()));

    // AdvancedAE quantum material chain.
    public static final RegistryObject<Item> QUANTUM_INFUSED_DUST = ITEMS.register("quantum_infused_dust",
            () -> new Item(props()));
    public static final RegistryObject<Item> QUANTUM_ALLOY = ITEMS.register("quantum_alloy",
            () -> new Item(props()));
    public static final RegistryObject<Item> QUANTUM_ALLOY_PLATE = ITEMS.register("quantum_alloy_plate",
            () -> new Item(props()));
    public static final RegistryObject<Item> SHATTERED_SINGULARITY = ITEMS.register("shattered_singularity",
            () -> new Item(props()));
    public static final RegistryObject<Item> QUANTUM_PROCESSOR_PRESS = ITEMS.register("quantum_processor_press",
            () -> new Item(props().maxStackSize(1)));
    public static final RegistryObject<Item> PRINTED_QUANTUM_PROCESSOR = ITEMS.register("printed_quantum_processor",
            () -> new Item(props()));
    public static final RegistryObject<Item> QUANTUM_PROCESSOR = ITEMS.register("quantum_processor",
            () -> new Item(props()));
    public static final RegistryObject<Item> QUANTUM_STORAGE_COMPONENT = ITEMS.register("quantum_storage_component",
            () -> new Item(props()));

    // ExtendedAE silicon block used by the Circuit Slicer's high-throughput silicon recipe.
    public static final RegistryObject<Block> SILICON_BLOCK = BLOCKS.register("silicon_block",
            () -> new Block(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(5.0F, 6.0F)));
    public static final RegistryObject<Item> SILICON_BLOCK_ITEM = ITEMS.register("silicon_block",
            () -> new BlockItem(SILICON_BLOCK.get(), props()));

    public static final RegistryObject<FlowingFluid> QUANTUM_INFUSION_SOURCE =
            FLUIDS.register("quantum_infusion_source",
                    () -> new ForgeFlowingFluid.Source(quantumInfusionProperties()));
    public static final RegistryObject<FlowingFluid> QUANTUM_INFUSION_FLOWING =
            FLUIDS.register("quantum_infusion_flowing",
                    () -> new ForgeFlowingFluid.Flowing(quantumInfusionProperties()));
    public static final RegistryObject<FlowingFluidBlock> QUANTUM_INFUSION_BLOCK =
            BLOCKS.register("quantum_infusion_block",
                    () -> new FlowingFluidBlock(QUANTUM_INFUSION_SOURCE,
                            AbstractBlock.Properties.create(Material.WATER)
                                    .doesNotBlockMovement()
                                    .hardnessAndResistance(100.0F)
                                    .noDrops()));
    public static final RegistryObject<Item> QUANTUM_INFUSION_BUCKET =
            ITEMS.register("quantum_infusion_bucket",
                    () -> new BucketItem(QUANTUM_INFUSION_SOURCE,
                            props().maxStackSize(1).containerItem(Items.BUCKET)));

    public static final RegistryObject<ReactionChamberBlock> REACTION_CHAMBER = BLOCKS.register("reaction_chamber", () -> {
        ReactionChamberBlock block = new ReactionChamberBlock();
        block.setTileEntity(ReactionChamberTile.class, ExpansionAE::newReactionChamber);
        return block;
    });
    public static final RegistryObject<TileEntityType<ReactionChamberTile>> REACTION_CHAMBER_TILE =
            TILES.register("reaction_chamber", () -> TileEntityType.Builder
                    .create(ExpansionAE::newReactionChamber, REACTION_CHAMBER.get()).build(null));
    public static final RegistryObject<Item> REACTION_CHAMBER_ITEM = ITEMS.register("reaction_chamber",
            () -> new BlockItem(REACTION_CHAMBER.get(), props()));

    // ExtendedAE Circuit Slicer / Circuit Cutter.
    public static final RegistryObject<CircuitCutterBlock> CIRCUIT_CUTTER = BLOCKS.register("circuit_cutter", () -> {
        CircuitCutterBlock block = new CircuitCutterBlock();
        block.setTileEntity(CircuitCutterTile.class, ExpansionAE::newCircuitCutter);
        return block;
    });
    public static final RegistryObject<TileEntityType<CircuitCutterTile>> CIRCUIT_CUTTER_TILE =
            TILES.register("circuit_cutter", () -> TileEntityType.Builder
                    .create(ExpansionAE::newCircuitCutter, CIRCUIT_CUTTER.get()).build(null));
    public static final RegistryObject<Item> CIRCUIT_CUTTER_ITEM = ITEMS.register("circuit_cutter",
            () -> new BlockItem(CIRCUIT_CUTTER.get(), props()));

    // ExtendedAE ME Ingredient Buffer (36 shared item/fluid resource slots).
    public static final RegistryObject<IngredientBufferBlock> INGREDIENT_BUFFER = BLOCKS.register("ingredient_buffer", () -> {
        IngredientBufferBlock block = new IngredientBufferBlock();
        block.setTileEntity(IngredientBufferTile.class, ExpansionAE::newIngredientBuffer);
        return block;
    });
    public static final RegistryObject<TileEntityType<IngredientBufferTile>> INGREDIENT_BUFFER_TILE =
            TILES.register("ingredient_buffer", () -> TileEntityType.Builder
                    .create(ExpansionAE::newIngredientBuffer, INGREDIENT_BUFFER.get()).build(null));
    public static final RegistryObject<Item> INGREDIENT_BUFFER_ITEM = ITEMS.register("ingredient_buffer",
            () -> new BlockItem(INGREDIENT_BUFFER.get(), props()));

    // ExtendedAE ME Canner, adapted to Forge 1.16.5 fluid-container capabilities.
    public static final RegistryObject<CanerBlock> CANER = BLOCKS.register("caner", () -> {
        CanerBlock block = new CanerBlock();
        block.setTileEntity(CanerTile.class, ExpansionAE::newCaner);
        return block;
    });
    public static final RegistryObject<TileEntityType<CanerTile>> CANER_TILE =
            TILES.register("caner", () -> TileEntityType.Builder
                    .create(ExpansionAE::newCaner, CANER.get()).build(null));
    public static final RegistryObject<Item> CANER_ITEM = ITEMS.register("caner",
            () -> new BlockItem(CANER.get(), props()));

    // AdvancedAE Quantum Computer, adapted onto the AE2 8.4 crafting CPU cluster.
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_UNIT = BLOCKS.register("quantum_unit",
            () -> quantumBlock(QuantumCraftingBlock.Kind.UNIT));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_CORE = BLOCKS.register("quantum_core",
            () -> quantumBlock(QuantumCraftingBlock.Kind.CORE));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_STORAGE_128 = BLOCKS.register("quantum_storage_128",
            () -> quantumBlock(QuantumCraftingBlock.Kind.STORAGE_128M));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_STORAGE_256 = BLOCKS.register("quantum_storage_256",
            () -> quantumBlock(QuantumCraftingBlock.Kind.STORAGE_256M));
    public static final RegistryObject<QuantumCraftingBlock> DATA_ENTANGLER = BLOCKS.register("data_entangler",
            () -> quantumBlock(QuantumCraftingBlock.Kind.DATA_ENTANGLER));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_ACCELERATOR = BLOCKS.register("quantum_accelerator",
            () -> quantumBlock(QuantumCraftingBlock.Kind.ACCELERATOR));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_MULTI_THREADER = BLOCKS.register("quantum_multi_threader",
            () -> quantumBlock(QuantumCraftingBlock.Kind.MULTI_THREADER));
    public static final RegistryObject<QuantumCraftingBlock> QUANTUM_STRUCTURE = BLOCKS.register("quantum_structure",
            () -> quantumBlock(QuantumCraftingBlock.Kind.STRUCTURE));
    public static final RegistryObject<TileEntityType<QuantumCraftingTile>> QUANTUM_CRAFTING_TILE =
            TILES.register("quantum_crafting", () -> TileEntityType.Builder.create(
                    ExpansionAE::newQuantumCrafting,
                    QUANTUM_UNIT.get(), QUANTUM_CORE.get(), QUANTUM_STORAGE_128.get(), QUANTUM_STORAGE_256.get(),
                    DATA_ENTANGLER.get(), QUANTUM_ACCELERATOR.get(), QUANTUM_MULTI_THREADER.get(),
                    QUANTUM_STRUCTURE.get()).build(null));
    public static final RegistryObject<Item> QUANTUM_UNIT_ITEM = ITEMS.register("quantum_unit",
            () -> new BlockItem(QUANTUM_UNIT.get(), props()));
    public static final RegistryObject<Item> QUANTUM_CORE_ITEM = ITEMS.register("quantum_core",
            () -> new BlockItem(QUANTUM_CORE.get(), props()));
    public static final RegistryObject<Item> QUANTUM_STORAGE_128_ITEM = ITEMS.register("quantum_storage_128",
            () -> new BlockItem(QUANTUM_STORAGE_128.get(), props()));
    public static final RegistryObject<Item> QUANTUM_STORAGE_256_ITEM = ITEMS.register("quantum_storage_256",
            () -> new BlockItem(QUANTUM_STORAGE_256.get(), props()));
    public static final RegistryObject<Item> DATA_ENTANGLER_ITEM = ITEMS.register("data_entangler",
            () -> new BlockItem(DATA_ENTANGLER.get(), props()));
    public static final RegistryObject<Item> QUANTUM_ACCELERATOR_ITEM = ITEMS.register("quantum_accelerator",
            () -> new BlockItem(QUANTUM_ACCELERATOR.get(), props()));
    public static final RegistryObject<Item> QUANTUM_MULTI_THREADER_ITEM = ITEMS.register("quantum_multi_threader",
            () -> new BlockItem(QUANTUM_MULTI_THREADER.get(), props()));
    public static final RegistryObject<Item> QUANTUM_STRUCTURE_ITEM = ITEMS.register("quantum_structure",
            () -> new BlockItem(QUANTUM_STRUCTURE.get(), props()));
    public static final RegistryObject<ExpandedDriveBlock> EX_DRIVE = BLOCKS.register("ex_drive", () -> {
        ExpandedDriveBlock block = new ExpandedDriveBlock();
        block.setTileEntity(ExpandedDriveTile.class, ExpansionAE::newExpandedDrive);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedDriveTile>> EX_DRIVE_TILE = TILES.register("ex_drive",
            () -> TileEntityType.Builder.create(ExpansionAE::newExpandedDrive, EX_DRIVE.get()).build(null));
    public static final RegistryObject<Item> EX_DRIVE_ITEM = ITEMS.register("ex_drive",
            () -> new BlockItem(EX_DRIVE.get(), props()));
    public static final RegistryObject<ExpandedChargerBlock> EX_CHARGER = BLOCKS.register("ex_charger", () -> {
        ExpandedChargerBlock block = new ExpandedChargerBlock();
        block.setTileEntity(ExpandedChargerTile.class, ExpansionAE::newExpandedCharger);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedChargerTile>> EX_CHARGER_TILE = TILES.register("ex_charger",
            () -> TileEntityType.Builder.create(ExpansionAE::newExpandedCharger, EX_CHARGER.get()).build(null));
    public static final RegistryObject<Item> EX_CHARGER_ITEM = ITEMS.register("ex_charger",
            () -> new BlockItem(EX_CHARGER.get(), props()));
    public static final RegistryObject<ExpandedInscriberBlock> EX_INSCRIBER = BLOCKS.register("ex_inscriber", () -> {
        ExpandedInscriberBlock block = new ExpandedInscriberBlock();
        block.setTileEntity(ExpandedInscriberTile.class, ExpansionAE::newExpandedInscriber);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedInscriberTile>> EX_INSCRIBER_TILE = TILES.register("ex_inscriber",
            () -> TileEntityType.Builder.create(ExpansionAE::newExpandedInscriber, EX_INSCRIBER.get()).build(null));
    public static final RegistryObject<Item> EX_INSCRIBER_ITEM = ITEMS.register("ex_inscriber",
            () -> new BlockItem(EX_INSCRIBER.get(), props()));
    public static final RegistryObject<ExpandedMolecularAssemblerBlock> EX_ASSEMBLER = BLOCKS.register("ex_molecular_assembler", () -> {
        ExpandedMolecularAssemblerBlock block = new ExpandedMolecularAssemblerBlock();
        block.setTileEntity(ExpandedMolecularAssemblerTile.class, ExpansionAE::newExpandedAssembler);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedMolecularAssemblerTile>> EX_ASSEMBLER_TILE =
            TILES.register("ex_molecular_assembler", () -> TileEntityType.Builder
                    .create(ExpansionAE::newExpandedAssembler, EX_ASSEMBLER.get()).build(null));
    public static final RegistryObject<Item> EX_ASSEMBLER_ITEM = ITEMS.register("ex_molecular_assembler",
            () -> new BlockItem(EX_ASSEMBLER.get(), props()));
    public static final RegistryObject<ExpandedIOPortBlock> EX_IO_PORT = BLOCKS.register("ex_io_port", () -> {
        ExpandedIOPortBlock block = new ExpandedIOPortBlock();
        block.setTileEntity(ExpandedIOPortTile.class, ExpansionAE::newExpandedIOPort);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedIOPortTile>> EX_IO_PORT_TILE =
            TILES.register("ex_io_port", () -> TileEntityType.Builder
                    .create(ExpansionAE::newExpandedIOPort, EX_IO_PORT.get()).build(null));
    public static final RegistryObject<Item> EX_IO_PORT_ITEM = ITEMS.register("ex_io_port",
            () -> new BlockItem(EX_IO_PORT.get(), props()));
    public static final RegistryObject<Item> ACTIVE_FORMATION_PLANE = ITEMS.register("active_formation_plane", () ->
            new PartItem<>(props(), ActiveFormationPlane::new));
    public static final RegistryObject<ExpandedInterfaceBlock> ADV_PROVIDER = BLOCKS.register("advanced_pattern_provider", () -> {
        ExpandedInterfaceBlock block = new ExpandedInterfaceBlock();
        block.setTileEntity(ExpandedInterfaceTile.class, ExpansionAE::newAdvancedProvider);
        return block;
    });
    public static final RegistryObject<ExpandedInterfaceBlock> SMALL_ADV_PROVIDER = BLOCKS.register("small_advanced_pattern_provider", () -> {
        ExpandedInterfaceBlock block = new ExpandedInterfaceBlock();
        block.setTileEntity(ExpandedInterfaceTile.class, ExpansionAE::newSmallAdvancedProvider);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedInterfaceTile>> ADV_PROVIDER_TILE = TILES.register("advanced_pattern_provider",
            () -> TileEntityType.Builder.create(ExpansionAE::newAdvancedProvider, ADV_PROVIDER.get()).build(null));
    public static final RegistryObject<TileEntityType<ExpandedInterfaceTile>> SMALL_ADV_PROVIDER_TILE = TILES.register("small_advanced_pattern_provider",
            () -> TileEntityType.Builder.create(ExpansionAE::newSmallAdvancedProvider, SMALL_ADV_PROVIDER.get()).build(null));
    public static final RegistryObject<Item> ADV_PROVIDER_ITEM = ITEMS.register("advanced_pattern_provider", () -> new BlockItem(ADV_PROVIDER.get(), props()));
    public static final RegistryObject<Item> SMALL_ADV_PROVIDER_ITEM = ITEMS.register("small_advanced_pattern_provider", () -> new BlockItem(SMALL_ADV_PROVIDER.get(), props()));
    public static final RegistryObject<Item> PATTERN_ENCODER = ITEMS.register("advanced_pattern_encoder", () -> new PatternEncoderItem(props()));
    public static final RegistryObject<Item> PATTERN_MODIFIER = ITEMS.register("pattern_modifier", () ->
            new PatternModifierItem(props()));
    public static final RegistryObject<Item> PACKING_TAPE = ITEMS.register("me_packing_tape", () ->
            new PackingTapeItem(props().maxDamage(64)));
    public static final RegistryObject<Item> PACKED_DEVICE = ITEMS.register("package", () ->
            new PackedDeviceItem(new Item.Properties().maxStackSize(1)));
    public static final RegistryObject<Item> PROVIDER_PART = ITEMS.register("ex_pattern_provider_part", () ->
            new PartItem<>(props(), stack -> new ExpandedInterfacePart(stack, 9, 36, false, "ex_pattern_provider")));
    public static final RegistryObject<Item> INTERFACE_PART = ITEMS.register("ex_interface_part", () ->
            new PartItem<>(props(), stack -> new ExpandedInterfacePart(stack, 36, 0, false, "ex_interface")));
    public static final RegistryObject<Item> ADV_PROVIDER_PART = ITEMS.register("advanced_pattern_provider_part", () ->
            new PartItem<>(props(), stack -> new ExpandedInterfacePart(stack, 9, 36, true, "advanced_pattern_provider")));
    public static final RegistryObject<Item> SMALL_ADV_PROVIDER_PART = ITEMS.register("small_advanced_pattern_provider_part", () ->
            new PartItem<>(props(), stack -> new ExpandedInterfacePart(stack, 9, 9, true, "small_advanced_pattern_provider")));
    public static final RegistryObject<Item> PATTERN_TERMINAL = ITEMS.register("ex_pattern_access_terminal", () ->
            new PartItem<>(props(), ExpandedTerminalPart::new));

    public static final RegistryObject<Item> INTERFACE_UPGRADE = ITEMS.register("interface_upgrade", () ->
            new UpgradeItem(props().maxStackSize(16), UpgradeItem.Target.INTERFACE));
    public static final RegistryObject<Item> PATTERN_PROVIDER_UPGRADE = ITEMS.register("pattern_provider_upgrade", () ->
            new UpgradeItem(props().maxStackSize(16), UpgradeItem.Target.PATTERN_PROVIDER));
    public static final RegistryObject<Item> IO_BUS_UPGRADE = ITEMS.register("io_bus_upgrade", () ->
            new UpgradeItem(props().maxStackSize(16), UpgradeItem.Target.IO_BUS));
    public static final RegistryObject<Item> PATTERN_TERMINAL_UPGRADE = ITEMS.register("pattern_terminal_upgrade", () ->
            new UpgradeItem(props().maxStackSize(16), UpgradeItem.Target.PATTERN_TERMINAL));
    public static final RegistryObject<Item> DRIVE_UPGRADE = ITEMS.register("drive_upgrade", () ->
            new UpgradeItem(props().maxStackSize(16), UpgradeItem.Target.DRIVE));

    private static ExpandedInterfaceTile newProvider() { return new ExpandedInterfaceTile(PROVIDER_TILE.get(), 9, 36); }
    private static ExpandedInterfaceTile newInterface() { return new ExpandedInterfaceTile(INTERFACE_TILE.get(), 36, 0); }
    private static ExpandedInterfaceTile newAdvancedProvider() { return new ExpandedInterfaceTile(ADV_PROVIDER_TILE.get(), 9, 36, true); }
    private static ExpandedInterfaceTile newSmallAdvancedProvider() { return new ExpandedInterfaceTile(SMALL_ADV_PROVIDER_TILE.get(), 9, 9, true); }
    private static ExpandedDriveTile newExpandedDrive() { return new ExpandedDriveTile(EX_DRIVE_TILE.get()); }
    private static ExpandedChargerTile newExpandedCharger() { return new ExpandedChargerTile(EX_CHARGER_TILE.get()); }
    private static ExpandedInscriberTile newExpandedInscriber() { return new ExpandedInscriberTile(EX_INSCRIBER_TILE.get()); }
    private static ExpandedMolecularAssemblerTile newExpandedAssembler() {
        return new ExpandedMolecularAssemblerTile(EX_ASSEMBLER_TILE.get());
    }
    private static ExpandedIOPortTile newExpandedIOPort() {
        return new ExpandedIOPortTile(EX_IO_PORT_TILE.get());
    }
    private static ReactionChamberTile newReactionChamber() {
        return new ReactionChamberTile(REACTION_CHAMBER_TILE.get());
    }

    private static CircuitCutterTile newCircuitCutter() {
        return new CircuitCutterTile(CIRCUIT_CUTTER_TILE.get());
    }

    private static IngredientBufferTile newIngredientBuffer() {
        return new IngredientBufferTile(INGREDIENT_BUFFER_TILE.get());
    }

    private static CanerTile newCaner() {
        return new CanerTile(CANER_TILE.get());
    }

    private static WirelessConnectorTile newWirelessConnector() {
        return new WirelessConnectorTile(WIRELESS_CONNECTOR_TILE.get());
    }

    private static QuantumCraftingBlock quantumBlock(QuantumCraftingBlock.Kind kind) {
        QuantumCraftingBlock block = new QuantumCraftingBlock(kind);
        block.setTileEntity(QuantumCraftingTile.class, ExpansionAE::newQuantumCrafting);
        return block;
    }

    private static QuantumCraftingTile newQuantumCrafting() {
        return new QuantumCraftingTile(QUANTUM_CRAFTING_TILE.get());
    }

    private static ForgeFlowingFluid.Properties quantumInfusionProperties() {
        return new ForgeFlowingFluid.Properties(
                QUANTUM_INFUSION_SOURCE,
                QUANTUM_INFUSION_FLOWING,
                FluidAttributes.builder(
                        new ResourceLocation("minecraft", "block/water_still"),
                        new ResourceLocation("minecraft", "block/water_flow"))
                        .translationKey("fluid.expansionae.quantum_infusion")
                        .color(0xFF7B61FF)
                        .luminosity(4)
                        .density(1200)
                        .viscosity(1400))
                .block(QUANTUM_INFUSION_BLOCK)
                .bucket(QUANTUM_INFUSION_BUCKET)
                .tickRate(8)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    public static Item.Properties props() { return new Item.Properties().group(TAB); }

    public ExpansionAE() {
        ExpansionNetwork.init();
        Api.instance().registries().partModels().registerModels(
                appeng.items.parts.PartModelsHelper.createModels(ExpandedInterfacePart.class));
        Api.instance().registries().partModels().registerModels(
                appeng.items.parts.PartModelsHelper.createModels(ThroughputMonitorPart.class));
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILES.register(bus);
        FLUIDS.register(bus);
        bus.addGenericListener(ContainerType.class, this::registerContainers);
        bus.addGenericListener(IRecipeSerializer.class, this::registerRecipeSerializers);
        bus.addListener(this::setup);
    }
    private void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().register(ExpandedContainer.TYPE);
        event.getRegistry().register(PatternEncoderContainer.TYPE);
        event.getRegistry().register(PatternModifierContainer.TYPE);
        event.getRegistry().register(StockExportBusContainer.TYPE);
        event.getRegistry().register(AdvancedIOBusContainer.TYPE);
        event.getRegistry().register(ThresholdExportBusContainer.TYPE);
        event.getRegistry().register(PreciseExportBusContainer.TYPE);
        event.getRegistry().register(ModExportBusContainer.TYPE);
        event.getRegistry().register(TagExportBusContainer.TYPE);
        event.getRegistry().register(ModStorageBusContainer.TYPE);
        event.getRegistry().register(TagStorageBusContainer.TYPE);
        event.getRegistry().register(PreciseStorageBusContainer.TYPE);
        event.getRegistry().register(ThresholdLevelEmitterContainer.TYPE);
        event.getRegistry().register(ExpandedInscriberContainer.TYPE);
        event.getRegistry().register(ExpandedMolecularAssemblerContainer.TYPE);
        event.getRegistry().register(ExpandedIOPortContainer.TYPE);
        event.getRegistry().register(ExpandedDriveContainer.TYPE);
        event.getRegistry().register(ExpandedTerminalContainer.TYPE);
        event.getRegistry().register(ReactionChamberContainer.TYPE);
        event.getRegistry().register(CircuitCutterContainer.TYPE);
        event.getRegistry().register(IngredientBufferContainer.TYPE);
        event.getRegistry().register(CanerContainer.TYPE);
    }

    private void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(ReactionChamberRecipeSerializer.INSTANCE);
        event.getRegistry().register(CircuitCutterRecipeSerializer.INSTANCE);
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Api.instance().registries().cell().addCellHandler(new InfinityCellHandler());
            Upgrades.CRAFTING.registerItem(INTERFACE_ITEM.get(), 1);
            Upgrades.CRAFTING.registerItem(PROVIDER_ITEM.get(), 1);
            for (Item item : new Item[]{PROVIDER_PART.get(), INTERFACE_PART.get(), ADV_PROVIDER_ITEM.get(),
                    SMALL_ADV_PROVIDER_ITEM.get(), ADV_PROVIDER_PART.get(), SMALL_ADV_PROVIDER_PART.get()}) {
                Upgrades.CRAFTING.registerItem(item, 1);
            }
            for (Item item : new Item[]{IMPORT_BUS.get(), EXPORT_BUS.get(), IMPORT_EXPORT_BUS.get()}) {
                Upgrades.SPEED.registerItem(item, 4);
                Upgrades.CAPACITY.registerItem(item, 2);
                Upgrades.REDSTONE.registerItem(item, 1);
                Upgrades.FUZZY.registerItem(item, 1);
            }
            Upgrades.CRAFTING.registerItem(EXPORT_BUS.get(), 1);
            Upgrades.CRAFTING.registerItem(IMPORT_EXPORT_BUS.get(), 1);
            Upgrades.INVERTER.registerItem(IMPORT_EXPORT_BUS.get(), 1);
            Upgrades.SPEED.registerItem(STOCK_EXPORT_BUS.get(), 4);
            Upgrades.CAPACITY.registerItem(STOCK_EXPORT_BUS.get(), 2);
            Upgrades.REDSTONE.registerItem(STOCK_EXPORT_BUS.get(), 1);
            Upgrades.FUZZY.registerItem(STOCK_EXPORT_BUS.get(), 1);
            Upgrades.CRAFTING.registerItem(STOCK_EXPORT_BUS.get(), 1);
            for (Item item : new Item[]{ADVANCED_IO_BUS.get(), THRESHOLD_EXPORT_BUS.get(), PRECISE_EXPORT_BUS.get()}) {
                Upgrades.SPEED.registerItem(item, 4);
                Upgrades.CAPACITY.registerItem(item, 2);
                Upgrades.REDSTONE.registerItem(item, 1);
                Upgrades.FUZZY.registerItem(item, 1);
                Upgrades.INVERTER.registerItem(item, 1);
                Upgrades.CRAFTING.registerItem(item, 1);
            }
            Upgrades.SPEED.registerItem(ACTIVE_FORMATION_PLANE.get(), 4);
            Upgrades.CAPACITY.registerItem(ACTIVE_FORMATION_PLANE.get(), 5);
            Upgrades.REDSTONE.registerItem(ACTIVE_FORMATION_PLANE.get(), 1);
            Upgrades.FUZZY.registerItem(ACTIVE_FORMATION_PLANE.get(), 1);
            Upgrades.INVERTER.registerItem(ACTIVE_FORMATION_PLANE.get(), 1);
            Upgrades.CRAFTING.registerItem(ACTIVE_FORMATION_PLANE.get(), 1);
            Upgrades.SPEED.registerItem(MOD_EXPORT_BUS.get(), 4);
            Upgrades.REDSTONE.registerItem(MOD_EXPORT_BUS.get(), 1);
            Upgrades.SPEED.registerItem(TAG_EXPORT_BUS.get(), 4);
            Upgrades.REDSTONE.registerItem(TAG_EXPORT_BUS.get(), 1);
            Upgrades.INVERTER.registerItem(MOD_STORAGE_BUS.get(), 1);
            Upgrades.INVERTER.registerItem(TAG_STORAGE_BUS.get(), 1);
            Upgrades.CAPACITY.registerItem(PRECISE_STORAGE_BUS.get(), 5);
            Upgrades.FUZZY.registerItem(PRECISE_STORAGE_BUS.get(), 1);
            Upgrades.INVERTER.registerItem(PRECISE_STORAGE_BUS.get(), 1);
            Upgrades.FUZZY.registerItem(THRESHOLD_LEVEL_EMITTER.get(), 1);
            Upgrades.CRAFTING.registerItem(THRESHOLD_LEVEL_EMITTER.get(), 1);
            Upgrades.SPEED.registerItem(EX_INSCRIBER_ITEM.get(), 4);
            Upgrades.SPEED.registerItem(EX_ASSEMBLER_ITEM.get(), 5);
            Upgrades.SPEED.registerItem(EX_IO_PORT_ITEM.get(), 5);
            Upgrades.REDSTONE.registerItem(EX_IO_PORT_ITEM.get(), 1);
            Upgrades.SPEED.registerItem(REACTION_CHAMBER_ITEM.get(), 4);
            Upgrades.SPEED.registerItem(CIRCUIT_CUTTER_ITEM.get(), 4);
        });
    }
}
