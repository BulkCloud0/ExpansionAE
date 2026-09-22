package com.bulkcloud.expansionae.feature.disk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.tile.storage.ChestTileEntity;
import appeng.tile.storage.DriveTileEntity;

public final class DiskRuntimeValidator {
    private static final String PERSISTENCE_PHASE_ENV = "EXPANSIONAE_PERSISTENCE_PHASE";
    private static final UUID PERSISTENCE_TEST_UUID =
            UUID.fromString("0d15c000-0000-4000-8000-000000000165");
    private static final long PERSISTENCE_TEST_AMOUNT = 321L;

    private DiskRuntimeValidator() {
    }

    public static void validate() {
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            throw new IllegalStateException("DISK runtime validation requires loaded overworld storage");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        validateTransientStorage(storage, channel);
        validateAe2StorageHosts(channel);
        validatePersistencePhase(storage, channel);
        DiskGridRuntimeValidator.begin();
    }

    private static void validateTransientStorage(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        ItemStack primaryStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> primary = open(primaryStack, channel, "primary");

        IAEItemStack stone = stone(channel, 600);
        IAEItemStack firstRemainder = primary.injectItems(stone, Actionable.MODULATE, null);
        if (firstRemainder != null) {
            throw new IllegalStateException("1k DISK rejected part of the initial 600 item insertion");
        }
        requireStoredCount(primary, 600);

        if (!primaryStack.hasTag() || !primaryStack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("1k DISK did not assign a backing UUID after first insertion");
        }
        UUID uuid = primaryStack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        ItemStack aliasStack = primaryStack.copy();
        ICellInventoryHandler<IAEItemStack> alias = open(aliasStack, channel, "alias");
        requireStoredCount(alias, 600);

        IAEItemStack additional = stone(channel, 500);
        IAEItemStack capacityRemainder = alias.injectItems(additional, Actionable.MODULATE, null);
        if (capacityRemainder == null || capacityRemainder.getStackSize() != 100) {
            throw new IllegalStateException("1k DISK capacity contract expected a remainder of 100 from 500");
        }

        requireStoredCount(alias, 1000);
        requireStoredCount(primary, 1000);

        IAEItemStack request250 = stone(channel, 250);
        IAEItemStack extracted250 = alias.extractItems(request250, Actionable.MODULATE, null);
        if (extracted250 == null || extracted250.getStackSize() != 250) {
            throw new IllegalStateException("DISK alias extraction did not return 250 items");
        }
        requireStoredCount(primary, 750);

        IAEItemStack request750 = stone(channel, 750);
        IAEItemStack extracted750 = primary.extractItems(request750, Actionable.MODULATE, null);
        if (extracted750 == null || extracted750.getStackSize() != 750) {
            throw new IllegalStateException("DISK primary extraction did not return the remaining 750 items");
        }

        requireStoredCount(alias, 0);

        DiskStorageData.DiskRecord emptyRecord = storage.get(uuid);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException("Empty DISK backing record was not preserved for UUID aliases");
        }

        storage.remove(uuid);

        ExpansionAE.LOGGER.info(
                "DISK storage runtime validated (capacity, insert/extract, UUID alias sync, empty backing record)");
    }

    private static void validateAe2StorageHosts(IItemStorageChannel channel) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException("Dedicated server is not available for AE2 host validation");
        }

        ServerWorld world = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException("Overworld is not available for AE2 host validation");
        }

        BlockPos base = world.getSpawnPoint().up(8);
        BlockPos drivePos = base;
        BlockPos chestPos = base.east(2);

        world.removeBlock(drivePos, false);
        world.removeBlock(chestPos, false);

        try {
            validateDriveHost(world, drivePos, channel);
            validateChestHost(world, chestPos, channel);
        } finally {
            world.removeBlock(drivePos, false);
            world.removeBlock(chestPos, false);
        }

        ExpansionAE.LOGGER.info(
                "DISK AE2 host validation passed (ME Drive acceptance/state + ME Chest terminal monitor)");
    }

    private static void validateDriveHost(
            ServerWorld world,
            BlockPos pos,
            IItemStorageChannel channel) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Placed AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity drive = (DriveTileEntity) tile;
        drive.onReady();

        ItemStack diskStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> handler = open(diskStack, channel, "ME Drive test DISK");
        IAEItemStack remainder =
                handler.injectItems(stone(channel, 37), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("ME Drive test DISK rejected its preparation payload");
        }

        IItemHandler driveInventory = drive.getInternalInventory();
        ItemStack rejected = driveInventory.insertItem(0, diskStack, false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("AE2 ME Drive rejected the ExpansionAE DISK");
        }

        if (drive.getCellItem(0) != ExpansionAEItems.DISK_1K.get()) {
            throw new IllegalStateException("AE2 ME Drive did not retain the ExpansionAE DISK in slot 0");
        }

        CellState state = drive.getCellStatus(0);
        if (state != CellState.NOT_EMPTY) {
            throw new IllegalStateException(
                    "AE2 ME Drive reported unexpected DISK state: " + state);
        }

        ICellInventoryHandler<IAEItemStack> driveHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(driveInventory.getStackInSlot(0), drive, channel);
        if (driveHandler == null || driveHandler.getCellInv() == null) {
            throw new IllegalStateException("AE2 ME Drive could not reopen the inserted DISK");
        }
        requireStoredCount(driveHandler, 37);

        ItemStack hostedDisk = driveInventory.getStackInSlot(0);
        if (!hostedDisk.hasTag() || !hostedDisk.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("ME Drive hosted DISK lost its storage UUID");
        }
        UUID expectedUuid = hostedDisk.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        // Simulate the persistent part of a chunk unload/reload: serialize the real
        // Drive tile, tear down its network node, recreate the block entity and load
        // the saved NBT before readying it again.
        CompoundNBT savedDrive = drive.write(new CompoundNBT());
        drive.onChunkUnloaded();
        drive.disableDrops();
        world.removeBlock(pos, false);

        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);
        TileEntity reloadedTile = world.getTileEntity(pos);
        if (!(reloadedTile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Reloaded AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity reloadedDrive = (DriveTileEntity) reloadedTile;
        reloadedDrive.read(world.getBlockState(pos), savedDrive);
        reloadedDrive.onReady();

        IItemHandler reloadedInventory = reloadedDrive.getInternalInventory();
        ItemStack reloadedDisk = reloadedInventory.getStackInSlot(0);
        requireDiskUuid(reloadedDisk, expectedUuid, "chunk reload");

        if (reloadedDrive.getCellStatus(0) != CellState.NOT_EMPTY) {
            throw new IllegalStateException("Reloaded AE2 ME Drive did not restore DISK state");
        }

        ICellInventoryHandler<IAEItemStack> reloadedHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(reloadedDisk, reloadedDrive, channel);
        if (reloadedHandler == null || reloadedHandler.getCellInv() == null) {
            throw new IllegalStateException("Reloaded AE2 ME Drive could not reopen the DISK");
        }
        requireStoredCount(reloadedHandler, 37);

        // Exercise the same inventory-drop hook AEBaseTileBlock uses when a Drive is
        // broken, then reinsert that dropped cell into a fresh Drive.
        List<ItemStack> drops = new ArrayList<>();
        reloadedDrive.getDrops(world, pos, drops);

        ItemStack droppedDisk = ItemStack.EMPTY;
        for (ItemStack drop : drops) {
            if (drop.getItem() == ExpansionAEItems.DISK_1K.get()) {
                droppedDisk = drop.copy();
                break;
            }
        }
        if (droppedDisk.isEmpty()) {
            throw new IllegalStateException("Breaking the AE2 ME Drive did not expose the hosted DISK as a drop");
        }
        requireDiskUuid(droppedDisk, expectedUuid, "Drive drop");

        reloadedDrive.disableDrops();
        world.removeBlock(pos, false);
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);

        TileEntity replacedTile = world.getTileEntity(pos);
        if (!(replacedTile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Replaced AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity replacedDrive = (DriveTileEntity) replacedTile;
        replacedDrive.onReady();
        ItemStack replaceRejected =
                replacedDrive.getInternalInventory().insertItem(0, droppedDisk, false);
        if (!replaceRejected.isEmpty()) {
            throw new IllegalStateException("Fresh AE2 ME Drive rejected the dropped ExpansionAE DISK");
        }

        if (replacedDrive.getCellStatus(0) != CellState.NOT_EMPTY) {
            throw new IllegalStateException("Fresh AE2 ME Drive did not restore the dropped DISK state");
        }

        ItemStack replacedDisk = replacedDrive.getInternalInventory().getStackInSlot(0);
        requireDiskUuid(replacedDisk, expectedUuid, "Drive replacement");

        ICellInventoryHandler<IAEItemStack> replacedHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(replacedDisk, replacedDrive, channel);
        if (replacedHandler == null || replacedHandler.getCellInv() == null) {
            throw new IllegalStateException("Fresh AE2 ME Drive could not reopen the dropped DISK");
        }
        requireStoredCount(replacedHandler, 37);
    }

    private static void requireDiskUuid(ItemStack stack, UUID expected, String stage) {
        if (stack.isEmpty()
                || !stack.hasTag()
                || !stack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)
                || !expected.equals(stack.getTag().getUniqueId(DiskCellInventory.TAG_UUID))) {
            throw new IllegalStateException(
                    "DISK UUID was not preserved during " + stage);
        }
    }

    private static void validateChestHost(
            ServerWorld world,
            BlockPos pos,
            IItemStorageChannel channel) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().chest().block().getDefaultState(),
                3);

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ChestTileEntity)) {
            throw new IllegalStateException("Placed AE2 ME Chest did not create ChestTileEntity");
        }

        ChestTileEntity chest = (ChestTileEntity) tile;
        chest.onReady();

        ItemStack diskStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> handler = open(diskStack, channel, "ME Chest test DISK");
        IAEItemStack remainder =
                handler.injectItems(stone(channel, 41), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("ME Chest test DISK rejected its preparation payload");
        }

        IItemHandler chestInventory = chest.getInternalInventory();
        ItemStack rejected = chestInventory.insertItem(1, diskStack, false);
        if (!rejected.isEmpty()) {
            // Chest internal inventory layout can vary; retry the storage-cell slot explicitly.
            rejected = chestInventory.insertItem(0, diskStack, false);
        }
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("AE2 ME Chest rejected the ExpansionAE DISK");
        }

        IMEMonitor<IAEItemStack> monitor = chest.getInventory(channel);
        if (monitor == null) {
            throw new IllegalStateException("AE2 ME Chest did not expose an item monitor for the DISK");
        }

        IAEItemStack precise = monitor.getAvailableItems(channel.createList())
                .findPrecise(stone(channel, 1));
        if (precise == null || precise.getStackSize() != 41) {
            throw new IllegalStateException(
                    "AE2 ME Chest terminal monitor did not expose the expected 41 stored items");
        }
    }

    private static void validatePersistencePhase(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        String phase = System.getenv(PERSISTENCE_PHASE_ENV);
        if (phase == null || phase.isEmpty()) {
            return;
        }

        if ("write".equals(phase)) {
            validatePersistenceWrite(storage, channel);
            return;
        }

        if ("read".equals(phase)) {
            validatePersistenceRead(storage, channel);
            return;
        }

        throw new IllegalStateException(
                "Unknown " + PERSISTENCE_PHASE_ENV + " value: " + phase);
    }

    private static void validatePersistenceWrite(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        storage.remove(PERSISTENCE_TEST_UUID);
        storage.put(PERSISTENCE_TEST_UUID, new ListNBT(), new long[0], 0);

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence writer");

        IAEItemStack remainder =
                handler.injectItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("Persistence write phase rejected part of the test payload");
        }

        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence write phase did not update the backing record");
        }

        ExpansionAE.LOGGER.info(
                "DISK persistence write phase validated ({} items staged for restart)",
                PERSISTENCE_TEST_AMOUNT);
    }

    private static void validatePersistenceRead(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException(
                    "Persistence read phase did not recover the expected backing record after restart");
        }

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence reader");
        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        IAEItemStack extracted =
                handler.extractItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (extracted == null || extracted.getStackSize() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence read phase could not extract the recovered payload");
        }

        requireStoredCount(handler, 0);

        DiskStorageData.DiskRecord emptyRecord = storage.get(PERSISTENCE_TEST_UUID);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException(
                    "Persistence read phase did not preserve the empty backing record after extraction");
        }

        storage.remove(PERSISTENCE_TEST_UUID);

        ExpansionAE.LOGGER.info(
                "DISK persistence read phase validated (backing data survived server restart)");
    }

    private static ItemStack stackForUuid(UUID uuid) {
        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        stack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, uuid);
        return stack;
    }

    private static ICellInventoryHandler<IAEItemStack> open(
            ItemStack stack,
            IItemStorageChannel channel,
            String label) {
        ICellInventoryHandler<IAEItemStack> handler =
                ExpansionAEApi.get().registries().cell().getCellInventory(stack, null, channel);
        if (handler == null || handler.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a DISK cell inventory handler for " + label);
        }
        return handler;
    }

    private static IAEItemStack stone(IItemStorageChannel channel, long amount) {
        IAEItemStack stack = channel.createStack(new ItemStack(Items.STONE));
        if (stack == null) {
            throw new IllegalStateException("AE2 item channel could not create a stone stack");
        }
        stack.setStackSize(amount);
        return stack;
    }

    private static void requireStoredCount(
            ICellInventoryHandler<IAEItemStack> handler,
            long expected) {
        long actual = handler.getCellInv().getStoredItemCount();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK stored item count mismatch: expected " + expected + " but got " + actual);
        }
    }
}
