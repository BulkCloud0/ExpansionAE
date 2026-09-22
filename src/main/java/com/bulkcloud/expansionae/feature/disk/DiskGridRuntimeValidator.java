package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.networking.CreativeEnergyCellTileEntity;
import appeng.tile.storage.DriveTileEntity;

public final class DiskGridRuntimeValidator {
    private static final int MIN_TICKS_BEFORE_VALIDATION = 70;
    private static final int MAX_TICKS_BEFORE_FAILURE = 140;
    private static final long INITIAL_AMOUNT = 50;
    private static final long INJECT_AMOUNT = 25;
    private static final long EXTRACT_AMOUNT = 30;

    private static Session session;

    private DiskGridRuntimeValidator() {
    }

    public static void begin() {
        if (session != null) {
            throw new IllegalStateException("DISK grid validation is already active");
        }

        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException("Dedicated server is not available for DISK grid validation");
        }

        ServerWorld world = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException("Overworld is not available for DISK grid validation");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        BlockPos base = world.getSpawnPoint().up(14);
        BlockPos powerAPos = base;
        BlockPos driveAPos = base.east();
        BlockPos driveBPos = base.west();

        BlockPos powerCPos = base.south(8);
        BlockPos driveCPos = powerCPos.east();

        clear(world, driveAPos);
        clear(world, driveBPos);
        clear(world, powerAPos);
        clear(world, driveCPos);
        clear(world, powerCPos);

        ItemStack primaryStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> primary = open(primaryStack, null, channel, "grid primary");
        IAEItemStack initial = stone(channel, INITIAL_AMOUNT);
        IAEItemStack remainder = primary.injectItems(initial, Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("DISK grid validation could not prepare its initial payload");
        }

        if (!primaryStack.hasTag() || !primaryStack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("DISK grid validation primary stack did not receive a UUID");
        }
        UUID uuid = primaryStack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        ItemStack sameGridAlias = primaryStack.copy();
        ItemStack otherGridAlias = primaryStack.copy();

        CreativeEnergyCellTileEntity powerA = placeCreativeEnergyCell(world, powerAPos);
        DriveTileEntity driveA = placeDrive(world, driveAPos);
        DriveTileEntity driveB = placeDrive(world, driveBPos);

        CreativeEnergyCellTileEntity powerC = placeCreativeEnergyCell(world, powerCPos);
        DriveTileEntity driveC = placeDrive(world, driveCPos);

        powerA.onReady();
        driveA.onReady();
        driveB.onReady();
        powerC.onReady();
        driveC.onReady();

        insertDisk(driveA, primaryStack, "primary grid Drive A");
        insertDisk(driveB, sameGridAlias, "same-grid alias Drive B");
        insertDisk(driveC, otherGridAlias, "other-grid alias Drive C");

        session = new Session(
                world,
                channel,
                uuid,
                powerAPos,
                driveAPos,
                driveBPos,
                powerCPos,
                driveCPos,
                driveA,
                driveB,
                driveC);

        ExpansionAE.LOGGER.info(
                "DISK active grid validation scheduled (same-grid alias dedup + cross-grid alias sync)");
    }

    public static void tick() {
        Session current = session;
        if (current == null) {
            return;
        }

        current.ticks++;

        if (current.ticks < MIN_TICKS_BEFORE_VALIDATION) {
            return;
        }

        if (!current.driveA.getProxy().isActive()
                || !current.driveB.getProxy().isActive()
                || !current.driveC.getProxy().isActive()) {
            if (current.ticks < MAX_TICKS_BEFORE_FAILURE) {
                return;
            }

            throw new IllegalStateException(
                    "DISK active grid validation timed out waiting for powered/channel-active ME Drives");
        }

        validate(current);
        cleanup(current);
        session = null;

        ExpansionAE.LOGGER.info(
                "DISK active grid runtime validated (terminal backend, same-grid alias dedup, cross-grid sync)");
    }

    private static void validate(Session s) {
        try {
            IGrid gridA = s.driveA.getProxy().getGrid();
            IGrid gridB = s.driveB.getProxy().getGrid();
            IGrid gridC = s.driveC.getProxy().getGrid();

            if (gridA != gridB) {
                throw new IllegalStateException("Same-grid alias Drives did not join the same AE2 grid");
            }
            if (gridA == gridC) {
                throw new IllegalStateException("Independent alias Drive unexpectedly joined the primary AE2 grid");
            }

            IStorageGrid storageA = s.driveA.getProxy().getStorage();
            IStorageGrid storageC = s.driveC.getProxy().getStorage();

            IMEMonitor<IAEItemStack> monitorA = storageA.getInventory(s.channel);
            IMEMonitor<IAEItemStack> monitorC = storageC.getInventory(s.channel);

            requireNetworkCount(monitorA, s.channel, INITIAL_AMOUNT, "same-grid aliases");
            requireNetworkCount(monitorC, s.channel, INITIAL_AMOUNT, "independent alias grid");
            requireCachedNetworkCount(monitorA, s.channel, INITIAL_AMOUNT, "primary terminal cache");
            requireCachedNetworkCount(monitorC, s.channel, INITIAL_AMOUNT, "alias terminal cache");

            IAEItemStack injectRemainder = monitorA.injectItems(
                    stone(s.channel, INJECT_AMOUNT),
                    Actionable.MODULATE,
                    new MachineSource(s.driveA));
            if (injectRemainder != null) {
                throw new IllegalStateException("Active ME grid rejected part of the DISK injection payload");
            }

            long afterInject = INITIAL_AMOUNT + INJECT_AMOUNT;
            requireNetworkCount(monitorA, s.channel, afterInject, "primary grid after injection");
            requireNetworkCount(monitorC, s.channel, afterInject, "alias grid after remote injection");
            requireCachedNetworkCount(monitorA, s.channel, afterInject, "primary terminal cache after injection");
            requireCachedNetworkCount(monitorC, s.channel, afterInject, "alias terminal cache after remote injection");

            IAEItemStack extracted = monitorC.extractItems(
                    stone(s.channel, EXTRACT_AMOUNT),
                    Actionable.MODULATE,
                    new MachineSource(s.driveC));
            if (extracted == null || extracted.getStackSize() != EXTRACT_AMOUNT) {
                throw new IllegalStateException("Independent alias grid did not extract the requested DISK amount");
            }

            long finalAmount = afterInject - EXTRACT_AMOUNT;
            requireNetworkCount(monitorA, s.channel, finalAmount, "primary grid after alias extraction");
            requireNetworkCount(monitorC, s.channel, finalAmount, "alias grid after extraction");
            requireCachedNetworkCount(monitorA, s.channel, finalAmount, "primary terminal cache after alias extraction");
            requireCachedNetworkCount(monitorC, s.channel, finalAmount, "alias terminal cache after extraction");

            ICellInventoryHandler<IAEItemStack> driveAHandler = open(
                    s.driveA.getInternalInventory().getStackInSlot(0),
                    s.driveA,
                    s.channel,
                    "Drive A after grid operations");
            ICellInventoryHandler<IAEItemStack> driveCHandler = open(
                    s.driveC.getInternalInventory().getStackInSlot(0),
                    s.driveC,
                    s.channel,
                    "Drive C after grid operations");

            requireCellCount(driveAHandler, finalAmount, "Drive A backing view");
            requireCellCount(driveCHandler, finalAmount, "Drive C backing view");
        } catch (GridAccessException e) {
            throw new IllegalStateException("AE2 grid became unavailable during DISK active grid validation", e);
        }
    }

    private static void requireCachedNetworkCount(
            IMEMonitor<IAEItemStack> monitor,
            IItemStorageChannel channel,
            long expected,
            String stage) {
        IAEItemStack precise = monitor.getStorageList().findPrecise(stone(channel, 1));
        long actual = precise == null ? 0 : precise.getStackSize();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK cached terminal count mismatch during "
                            + stage
                            + ": expected "
                            + expected
                            + " but got "
                            + actual);
        }
    }

    private static void requireNetworkCount(
            IMEMonitor<IAEItemStack> monitor,
            IItemStorageChannel channel,
            long expected,
            String stage) {
        IAEItemStack precise = monitor.getAvailableItems(channel.createList())
                .findPrecise(stone(channel, 1));
        long actual = precise == null ? 0 : precise.getStackSize();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK network count mismatch during "
                            + stage
                            + ": expected "
                            + expected
                            + " but got "
                            + actual);
        }
    }

    private static void requireCellCount(
            ICellInventoryHandler<IAEItemStack> handler,
            long expected,
            String stage) {
        long actual = handler.getCellInv().getStoredItemCount();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK cell count mismatch during "
                            + stage
                            + ": expected "
                            + expected
                            + " but got "
                            + actual);
        }
    }

    private static CreativeEnergyCellTileEntity placeCreativeEnergyCell(
            ServerWorld world,
            BlockPos pos) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().energyCellCreative().block().getDefaultState(),
                3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof CreativeEnergyCellTileEntity)) {
            throw new IllegalStateException("Placed AE2 Creative Energy Cell did not create its tile entity");
        }
        return (CreativeEnergyCellTileEntity) tile;
    }

    private static DriveTileEntity placeDrive(ServerWorld world, BlockPos pos) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Placed AE2 ME Drive did not create DriveTileEntity");
        }
        return (DriveTileEntity) tile;
    }

    private static void insertDisk(DriveTileEntity drive, ItemStack disk, String label) {
        IItemHandler inventory = drive.getInternalInventory();
        ItemStack rejected = inventory.insertItem(0, disk, false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("AE2 rejected the DISK for " + label);
        }
    }

    private static ICellInventoryHandler<IAEItemStack> open(
            ItemStack stack,
            Object host,
            IItemStorageChannel channel,
            String label) {
        ICellInventoryHandler<IAEItemStack> handler =
                ExpansionAEApi.get().registries().cell().getCellInventory(
                        stack,
                        host instanceof appeng.api.storage.cells.ISaveProvider
                                ? (appeng.api.storage.cells.ISaveProvider) host
                                : null,
                        channel);
        if (handler == null || handler.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a DISK handler for " + label);
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

    private static void cleanup(Session s) {
        s.driveA.disableDrops();
        s.driveB.disableDrops();
        s.driveC.disableDrops();

        clear(s.world, s.driveAPos);
        clear(s.world, s.driveBPos);
        clear(s.world, s.powerAPos);
        clear(s.world, s.driveCPos);
        clear(s.world, s.powerCPos);

        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage != null) {
            storage.remove(s.uuid);
        }
    }

    private static void clear(ServerWorld world, BlockPos pos) {
        world.removeBlock(pos, false);
    }

    private static final class Session {
        private final ServerWorld world;
        private final IItemStorageChannel channel;
        private final UUID uuid;
        private final BlockPos powerAPos;
        private final BlockPos driveAPos;
        private final BlockPos driveBPos;
        private final BlockPos powerCPos;
        private final BlockPos driveCPos;
        private final DriveTileEntity driveA;
        private final DriveTileEntity driveB;
        private final DriveTileEntity driveC;
        private int ticks;

        private Session(
                ServerWorld world,
                IItemStorageChannel channel,
                UUID uuid,
                BlockPos powerAPos,
                BlockPos driveAPos,
                BlockPos driveBPos,
                BlockPos powerCPos,
                BlockPos driveCPos,
                DriveTileEntity driveA,
                DriveTileEntity driveB,
                DriveTileEntity driveC) {
            this.world = world;
            this.channel = channel;
            this.uuid = uuid;
            this.powerAPos = powerAPos;
            this.driveAPos = driveAPos;
            this.driveBPos = driveBPos;
            this.powerCPos = powerCPos;
            this.driveCPos = driveCPos;
            this.driveA = driveA;
            this.driveB = driveB;
            this.driveC = driveC;
        }
    }
}
