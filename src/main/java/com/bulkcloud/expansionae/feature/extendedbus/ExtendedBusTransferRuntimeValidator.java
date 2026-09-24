package com.bulkcloud.expansionae.feature.extendedbus;

import java.util.UUID;

import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.feature.disk.DiskStorageData;
import com.bulkcloud.expansionae.feature.disk.DiskStorageService;
import com.bulkcloud.expansionae.feature.stockexport.StockExportBusPart;

import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.parts.IPart;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.core.Api;
import appeng.tile.networking.CableBusTileEntity;
import appeng.tile.networking.CreativeEnergyCellTileEntity;
import appeng.tile.storage.DriveTileEntity;

public final class ExtendedBusTransferRuntimeValidator {
    private static final String DISK_UUID_TAG = "expansionae_disk_uuid";

    private static final int IMPORT_INITIAL_CHEST = 1_024;
    private static final int IMPORT_EXPECTED_DISK = 1_000;
    private static final int IMPORT_EXPECTED_CHEST = 24;

    private static final int EXPORT_INITIAL_DISK = 100;
    private static final int EXPORT_INITIAL_CHEST = 60;
    private static final int EXPORT_EXPECTED_DISK = 96;
    private static final int EXPORT_EXPECTED_CHEST = 64;

    private static final int STOCK_INITIAL_DISK = 100;
    private static final int STOCK_INITIAL_CHEST = 40;
    private static final int STOCK_TARGET = 64;
    private static final int STOCK_EXPECTED_DISK = 76;
    private static final int STOCK_EXPECTED_CHEST = 64;

    private static final int MIN_TICKS = 20;
    private static final int MAX_TICKS = 200;

    private static Session session;

    private ExtendedBusTransferRuntimeValidator() {
    }

    public static void begin() {
        if (FMLEnvironment.production) {
            return;
        }

        if (session != null) {
            throw new IllegalStateException("8x item bus transfer validation is already active");
        }

        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException(
                    "Dedicated server is not available for 8x item bus transfer validation");
        }

        ServerWorld world = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException(
                    "Overworld is not available for 8x item bus transfer validation");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        BlockPos importBase = world.getSpawnPoint().up(30).east(12);
        BlockPos exportBase = world.getSpawnPoint().up(30).west(12);
        BlockPos stockBase = world.getSpawnPoint().up(30).south(12);

        Network importNetwork = createNetwork(
                world,
                channel,
                importBase,
                ExpansionAEItems.EXTENDED_IMPORT_BUS.get(),
                false,
                0,
                "import");

        fillImportChest(importNetwork.chestInventory);

        Network exportNetwork = createNetwork(
                world,
                channel,
                exportBase,
                ExpansionAEItems.EXTENDED_EXPORT_BUS.get(),
                true,
                EXPORT_INITIAL_DISK,
                "export");

        fillExportChest(exportNetwork.chestInventory);
        configureExportFilter(exportNetwork.bus);

        Network stockNetwork = createNetwork(
                world,
                channel,
                stockBase,
                ExpansionAEItems.STOCK_EXPORT_BUS.get(),
                true,
                STOCK_INITIAL_DISK,
                "stock");

        fillStockChest(stockNetwork.chestInventory);
        configureStockFilter(stockNetwork.bus);

        session = new Session(
                world,
                channel,
                importNetwork,
                exportNetwork,
                stockNetwork);

        ExpansionAE.LOGGER.info(
                "8x item bus + Stock Export transfer validation scheduled "
                        + "(DISK saturation + partial remainder + exact stock target)");
    }

    public static void tick() {
        if (FMLEnvironment.production) {
            return;
        }

        Session current = session;
        if (current == null) {
            return;
        }

        current.ticks++;

        if (current.ticks < MIN_TICKS) {
            return;
        }

        if (!isActive(current.importNetwork)
                || !isActive(current.exportNetwork)
                || !isActive(current.stockNetwork)) {
            if (current.ticks < MAX_TICKS) {
                return;
            }

            throw new IllegalStateException(
                    "8x item bus transfer validation timed out waiting for active AE2 grids");
        }

        long importDisk = storedCount(
                current.importNetwork.drive,
                current.channel,
                "import DISK");
        int importChest = countItem(current.importNetwork.chestInventory, Items.STONE);

        long exportDisk = storedCount(
                current.exportNetwork.drive,
                current.channel,
                "export DISK");
        int exportChest = countItem(current.exportNetwork.chestInventory, Items.STONE);

        long stockDisk = storedCount(
                current.stockNetwork.drive,
                current.channel,
                "stock DISK");
        int stockChest = countItem(current.stockNetwork.chestInventory, Items.STONE);

        boolean importDone =
                importDisk == IMPORT_EXPECTED_DISK
                        && importChest == IMPORT_EXPECTED_CHEST;
        boolean exportDone =
                exportDisk == EXPORT_EXPECTED_DISK
                        && exportChest == EXPORT_EXPECTED_CHEST;
        boolean stockDone =
                stockDisk == STOCK_EXPECTED_DISK
                        && stockChest == STOCK_EXPECTED_CHEST;

        if (!importDone || !exportDone || !stockDone) {
            if (current.ticks < MAX_TICKS) {
                return;
            }

            throw new IllegalStateException(
                    "8x item bus transfer validation timed out: "
                            + "import=" + importDisk + " disk/" + importChest + " chest, "
                            + "export=" + exportDisk + " disk/" + exportChest + " chest, "
                            + "stock=" + stockDisk + " disk/" + stockChest + " chest");
        }

        requireConservation(
                importDisk,
                importChest,
                IMPORT_INITIAL_CHEST,
                "import saturation");
        requireConservation(
                exportDisk,
                exportChest,
                EXPORT_INITIAL_DISK + EXPORT_INITIAL_CHEST,
                "export partial remainder");
        requireConservation(
                stockDisk,
                stockChest,
                STOCK_INITIAL_DISK + STOCK_INITIAL_CHEST,
                "stock exact target");

        requireSpeedCards(current.importNetwork.bus, "import");
        requireSpeedCards(current.exportNetwork.bus, "export");
        requireSpeedCards(current.stockNetwork.bus, "stock");

        cleanup(current.importNetwork);
        cleanup(current.exportNetwork);
        cleanup(current.stockNetwork);
        session = null;

        ExpansionAE.LOGGER.info(
                "8x item bus transfer runtime validated "
                        + "(import 1024 -> DISK 1000 + chest 24; "
                        + "export partial target 100+60 -> DISK 96 + chest 64; "
                        + "item counts conserved)");
        ExpansionAE.LOGGER.info(
                "Stock Export Bus runtime validated "
                        + "(target 64; initial DISK/chest 100+40 -> 76+64; "
                        + "no overshoot; item counts conserved)");
    }

    private static Network createNetwork(
            ServerWorld world,
            IItemStorageChannel channel,
            BlockPos cablePos,
            Item busItem,
            boolean prepareExportDisk,
            int initialDiskStone,
            String label) {
        BlockPos powerPos = cablePos.west();
        BlockPos drivePos = cablePos.north();
        BlockPos chestPos = cablePos.east();

        clear(world, powerPos);
        clear(world, drivePos);
        clear(world, cablePos);
        clear(world, chestPos);

        CreativeEnergyCellTileEntity power = placeCreativeEnergyCell(world, powerPos);
        DriveTileEntity drive = placeDrive(world, drivePos);
        CableBusTileEntity cable = placeCableBus(world, cablePos);
        ChestTileEntity chest = placeChest(world, chestPos);

        ItemStack centerCable =
                Api.instance().definitions().parts().cableGlass().stack(AEColor.TRANSPARENT, 1);
        AEPartLocation center =
                cable.addPart(centerCable, AEPartLocation.INTERNAL, null, null);
        if (center != AEPartLocation.INTERNAL) {
            throw new IllegalStateException(
                    "Could not install center glass cable for 8x " + label + " bus validation");
        }

        AEPartLocation side =
                cable.addPart(new ItemStack(busItem), AEPartLocation.EAST, null, null);
        if (side != AEPartLocation.EAST) {
            throw new IllegalStateException(
                    "Could not install 8x " + label + " bus on cable host");
        }

        IPart rawPart = cable.getPart(AEPartLocation.EAST);
        if (!(rawPart instanceof ExpansionImportBusPart)
                && !(rawPart instanceof ExpansionExportBusPart)
                && !(rawPart instanceof StockExportBusPart)) {
            throw new IllegalStateException(
                    "Unexpected part created for 8x " + label + " bus: "
                            + (rawPart == null ? "null" : rawPart.getClass().getName()));
        }

        power.onReady();
        drive.onReady();
        cable.onReady();

        ItemStack disk = new ItemStack(ExpansionAEItems.DISK_1K.get());
        if (prepareExportDisk) {
            ICellInventoryHandler<IAEItemStack> handler =
                    open(disk, channel, label + " preparation");
            IAEItemStack remainder =
                    handler.injectItems(stone(channel, initialDiskStone), Actionable.MODULATE, null);
            if (remainder != null) {
                throw new IllegalStateException(
                        "Could not prepare " + label + " DISK payload");
            }
        }

        IItemHandler driveInventory = drive.getInternalInventory();
        ItemStack rejected = driveInventory.insertItem(0, disk, false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException(
                    "AE2 ME Drive rejected the DISK for 8x " + label + " bus validation");
        }

        installFourSpeedCards(rawPart, label);

        return new Network(
                world,
                powerPos,
                drivePos,
                cablePos,
                chestPos,
                drive,
                rawPart,
                new InvWrapper(chest));
    }

    private static void installFourSpeedCards(IPart part, String label) {
        Item speedCard =
                ForgeRegistries.ITEMS.getValue(
                        new ResourceLocation("appliedenergistics2", "speed_card"));
        if (speedCard == null || speedCard == Items.AIR) {
            throw new IllegalStateException("AE2 speed card item is unavailable");
        }

        IItemHandler upgrades;
        if (part instanceof ExpansionImportBusPart) {
            upgrades = ((ExpansionImportBusPart) part).getInventoryByName("upgrades");
        } else if (part instanceof ExpansionExportBusPart) {
            upgrades = ((ExpansionExportBusPart) part).getInventoryByName("upgrades");
        } else if (part instanceof StockExportBusPart) {
            upgrades = ((StockExportBusPart) part).getInventoryByName("upgrades");
        } else {
            throw new IllegalStateException(
                    "Unexpected part while installing SPEED cards for " + label);
        }

        if (upgrades == null || upgrades.getSlots() != 4) {
            throw new IllegalStateException(
                    "8x " + label + " bus does not expose exactly four upgrade slots");
        }

        for (int slot = 0; slot < 4; slot++) {
            ItemStack rejected =
                    upgrades.insertItem(slot, new ItemStack(speedCard), false);
            if (!rejected.isEmpty()) {
                throw new IllegalStateException(
                        "8x " + label + " bus rejected SPEED card in slot " + slot);
            }
        }
    }

    private static void configureExportFilter(IPart part) {
        if (!(part instanceof ExpansionExportBusPart)) {
            throw new IllegalStateException("Export validation part is not ExpansionExportBusPart");
        }

        IItemHandler config =
                ((ExpansionExportBusPart) part).getInventoryByName("config");
        if (config == null || config.getSlots() < 1) {
            throw new IllegalStateException("8x export bus config inventory is unavailable");
        }

        ItemStack rejected =
                config.insertItem(0, new ItemStack(Items.STONE), false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("8x export bus rejected its Stone filter");
        }
    }

    private static void configureStockFilter(IPart part) {
        if (!(part instanceof StockExportBusPart)) {
            throw new IllegalStateException(
                    "Stock validation part is not StockExportBusPart");
        }

        StockExportBusPart stock = (StockExportBusPart) part;
        IItemHandler config = stock.getInventoryByName("config");
        if (config == null || config.getSlots() < 1) {
            throw new IllegalStateException(
                    "Stock Export Bus config inventory is unavailable");
        }

        ItemStack rejected =
                config.insertItem(0, new ItemStack(Items.STONE), false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException(
                    "Stock Export Bus rejected its Stone filter");
        }

        stock.getTargets().set(0, STOCK_TARGET);
        if (stock.getTargets().get(0) != STOCK_TARGET) {
            throw new IllegalStateException(
                    "Stock Export Bus target did not retain " + STOCK_TARGET);
        }
    }

    private static void fillImportChest(IItemHandler chest) {
        for (int slot = 0; slot < 16; slot++) {
            requireChestInsert(
                    chest,
                    slot,
                    new ItemStack(Items.STONE, 64),
                    "import source");
        }

        if (countItem(chest, Items.STONE) != IMPORT_INITIAL_CHEST) {
            throw new IllegalStateException(
                    "Import source chest was not prepared with exactly 1024 Stone");
        }
    }

    private static void fillExportChest(IItemHandler chest) {
        requireChestInsert(
                chest,
                0,
                new ItemStack(Items.STONE, EXPORT_INITIAL_CHEST),
                "export destination Stone slot");

        for (int slot = 1; slot < chest.getSlots(); slot++) {
            requireChestInsert(
                    chest,
                    slot,
                    new ItemStack(Items.COBBLESTONE, 64),
                    "export destination blocker");
        }

        if (countItem(chest, Items.STONE) != EXPORT_INITIAL_CHEST) {
            throw new IllegalStateException(
                    "Export destination chest was not prepared with exactly 60 Stone");
        }
    }

    private static void fillStockChest(IItemHandler chest) {
        requireChestInsert(
                chest,
                0,
                new ItemStack(Items.STONE, STOCK_INITIAL_CHEST),
                "stock destination Stone slot");

        for (int slot = 1; slot < chest.getSlots(); slot++) {
            requireChestInsert(
                    chest,
                    slot,
                    new ItemStack(Items.COBBLESTONE, 64),
                    "stock destination blocker");
        }

        if (countItem(chest, Items.STONE) != STOCK_INITIAL_CHEST) {
            throw new IllegalStateException(
                    "Stock destination chest was not prepared with exactly "
                            + STOCK_INITIAL_CHEST + " Stone");
        }
    }

    private static void requireChestInsert(
            IItemHandler chest,
            int slot,
            ItemStack stack,
            String label) {
        ItemStack rejected = chest.insertItem(slot, stack, false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException(
                    "Could not prepare " + label + " slot " + slot);
        }
    }

    private static boolean isActive(Network network) {
        boolean driveActive = network.drive.getProxy().isActive();

        if (network.bus instanceof ExpansionImportBusPart) {
            return driveActive
                    && ((ExpansionImportBusPart) network.bus).getProxy().isActive();
        }
        if (network.bus instanceof ExpansionExportBusPart) {
            return driveActive
                    && ((ExpansionExportBusPart) network.bus).getProxy().isActive();
        }
        if (network.bus instanceof StockExportBusPart) {
            return driveActive
                    && ((StockExportBusPart) network.bus).getProxy().isActive();
        }

        return false;
    }

    private static void requireSpeedCards(IPart part, String label) {
        int installed;
        if (part instanceof ExpansionImportBusPart) {
            installed =
                    ((ExpansionImportBusPart) part).getInstalledUpgrades(Upgrades.SPEED);
        } else if (part instanceof ExpansionExportBusPart) {
            installed =
                    ((ExpansionExportBusPart) part).getInstalledUpgrades(Upgrades.SPEED);
        } else if (part instanceof StockExportBusPart) {
            installed =
                    ((StockExportBusPart) part).getInstalledUpgrades(Upgrades.SPEED);
        } else {
            throw new IllegalStateException(
                    "Unexpected part while checking SPEED cards for " + label);
        }

        if (installed != 4) {
            throw new IllegalStateException(
                    "8x " + label + " bus SPEED card count changed during runtime: "
                            + installed);
        }
    }

    private static long storedCount(
            DriveTileEntity drive,
            IItemStorageChannel channel,
            String label) {
        ItemStack stack = drive.getInternalInventory().getStackInSlot(0);
        if (stack.isEmpty()) {
            throw new IllegalStateException(
                    "DISK disappeared from ME Drive during " + label);
        }

        return open(stack, channel, label).getCellInv().getStoredItemCount();
    }

    private static ICellInventoryHandler<IAEItemStack> open(
            ItemStack stack,
            IItemStorageChannel channel,
            String label) {
        ICellInventoryHandler<IAEItemStack> handler =
                ExpansionAEApi.get().registries().cell().getCellInventory(
                        stack,
                        null,
                        channel);
        if (handler == null || handler.getCellInv() == null) {
            throw new IllegalStateException(
                    "AE2 did not provide a DISK handler for " + label);
        }
        return handler;
    }

    private static IAEItemStack stone(
            IItemStorageChannel channel,
            long amount) {
        IAEItemStack stack = channel.createStack(new ItemStack(Items.STONE));
        if (stack == null) {
            throw new IllegalStateException(
                    "AE2 item channel could not create a Stone stack");
        }
        stack.setStackSize(amount);
        return stack;
    }

    private static int countItem(IItemHandler inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void requireConservation(
            long disk,
            int chest,
            long expectedTotal,
            String stage) {
        long actual = disk + chest;
        if (actual != expectedTotal) {
            throw new IllegalStateException(
                    "Item conservation failed during " + stage
                            + ": expected " + expectedTotal
                            + " Stone but found " + actual);
        }
    }

    private static CreativeEnergyCellTileEntity placeCreativeEnergyCell(
            ServerWorld world,
            BlockPos pos) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().energyCellCreative()
                        .block().getDefaultState(),
                3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof CreativeEnergyCellTileEntity)) {
            throw new IllegalStateException(
                    "Placed AE2 Creative Energy Cell did not create its tile entity");
        }
        return (CreativeEnergyCellTileEntity) tile;
    }

    private static DriveTileEntity placeDrive(
            ServerWorld world,
            BlockPos pos) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive()
                        .block().getDefaultState(),
                3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof DriveTileEntity)) {
            throw new IllegalStateException(
                    "Placed AE2 ME Drive did not create DriveTileEntity");
        }
        return (DriveTileEntity) tile;
    }

    private static CableBusTileEntity placeCableBus(
            ServerWorld world,
            BlockPos pos) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().multiPart()
                        .block().getDefaultState(),
                3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof CableBusTileEntity)) {
            throw new IllegalStateException(
                    "Placed AE2 cable bus block did not create CableBusTileEntity");
        }
        return (CableBusTileEntity) tile;
    }

    private static ChestTileEntity placeChest(
            ServerWorld world,
            BlockPos pos) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ChestTileEntity)) {
            throw new IllegalStateException(
                    "Placed vanilla chest did not create ChestTileEntity");
        }
        return (ChestTileEntity) tile;
    }

    private static void cleanup(Network network) {
        ItemStack disk = network.drive.getInternalInventory().getStackInSlot(0).copy();

        network.drive.disableDrops();

        clear(network.world, network.chestPos);
        clear(network.world, network.cablePos);
        clear(network.world, network.drivePos);
        clear(network.world, network.powerPos);

        if (disk.hasTag() && disk.getTag().hasUniqueId(DISK_UUID_TAG)) {
            UUID uuid = disk.getTag().getUniqueId(DISK_UUID_TAG);
            DiskStorageData storage = DiskStorageService.getCurrent();
            if (storage != null) {
                storage.remove(uuid);
            }
        }
    }

    private static void clear(ServerWorld world, BlockPos pos) {
        world.removeBlock(pos, false);
    }

    private static final class Session {
        private final ServerWorld world;
        private final IItemStorageChannel channel;
        private final Network importNetwork;
        private final Network exportNetwork;
        private final Network stockNetwork;
        private int ticks;

        private Session(
                ServerWorld world,
                IItemStorageChannel channel,
                Network importNetwork,
                Network exportNetwork,
                Network stockNetwork) {
            this.world = world;
            this.channel = channel;
            this.importNetwork = importNetwork;
            this.exportNetwork = exportNetwork;
            this.stockNetwork = stockNetwork;
        }
    }

    private static final class Network {
        private final ServerWorld world;
        private final BlockPos powerPos;
        private final BlockPos drivePos;
        private final BlockPos cablePos;
        private final BlockPos chestPos;
        private final DriveTileEntity drive;
        private final IPart bus;
        private final IItemHandler chestInventory;

        private Network(
                ServerWorld world,
                BlockPos powerPos,
                BlockPos drivePos,
                BlockPos cablePos,
                BlockPos chestPos,
                DriveTileEntity drive,
                IPart bus,
                IItemHandler chestInventory) {
            this.world = world;
            this.powerPos = powerPos;
            this.drivePos = drivePos;
            this.cablePos = cablePos;
            this.chestPos = chestPos;
            this.drive = drive;
            this.bus = bus;
            this.chestInventory = chestInventory;
        }
    }
}
