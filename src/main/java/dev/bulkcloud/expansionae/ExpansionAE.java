package dev.bulkcloud.expansionae;

import appeng.api.config.Upgrades;
import appeng.core.Api;
import appeng.items.parts.PartItem;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
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
    public static final RegistryObject<ExpandedDriveBlock> EX_DRIVE = BLOCKS.register("ex_drive", () -> {
        ExpandedDriveBlock block = new ExpandedDriveBlock();
        block.setTileEntity(ExpandedDriveTile.class, ExpansionAE::newExpandedDrive);
        return block;
    });
    public static final RegistryObject<TileEntityType<ExpandedDriveTile>> EX_DRIVE_TILE = TILES.register("ex_drive",
            () -> TileEntityType.Builder.create(ExpansionAE::newExpandedDrive, EX_DRIVE.get()).build(null));
    public static final RegistryObject<Item> EX_DRIVE_ITEM = ITEMS.register("ex_drive",
            () -> new BlockItem(EX_DRIVE.get(), props()));
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
    public static Item.Properties props() { return new Item.Properties().group(TAB); }

    public ExpansionAE() {
        ExpansionNetwork.init();
        Api.instance().registries().partModels().registerModels(
                appeng.items.parts.PartModelsHelper.createModels(ExpandedInterfacePart.class));
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILES.register(bus);
        bus.addGenericListener(ContainerType.class, this::registerContainers);
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
        event.getRegistry().register(ExpandedDriveContainer.TYPE);
        event.getRegistry().register(ExpandedTerminalContainer.TYPE);
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
        });
    }
}
