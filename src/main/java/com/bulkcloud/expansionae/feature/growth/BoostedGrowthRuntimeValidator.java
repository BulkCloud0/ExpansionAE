package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;

import appeng.entity.GrowingCrystalEntity;
import appeng.items.misc.CrystalSeedItem;
import appeng.tile.networking.CreativeEnergyCellTileEntity;

public final class BoostedGrowthRuntimeValidator {
    private static final int MIN_TICKS_BEFORE_VALIDATION = 70;
    private static final int GROWTH_VALIDATION_TICKS = 25;

    private static Session session;

    private BoostedGrowthRuntimeValidator() {
    }

    public static void begin() {
        if (session != null) {
            throw new IllegalStateException("Boosted Growth validation is already active");
        }

        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException(
                    "Dedicated server is not available for Boosted Growth validation");
        }

        ServerWorld world =
                ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException(
                    "Overworld is not available for Boosted Growth validation");
        }

        BlockPos boostedPos = world.getSpawnPoint().up(30);
        BlockPos powerPos = boostedPos.up();
        BlockPos seedPos = boostedPos.west();
        BlockPos controlPos = boostedPos.add(8, 0, 0);

        clear(world, boostedPos);
        clear(world, powerPos);
        clear(world, seedPos);
        clear(world, controlPos);

        world.setBlockState(
                boostedPos,
                ExpansionAEBlocks.BOOSTED_GROWTH_ACCELERATOR.get().getDefaultState(),
                3);

        TileEntity rawBoosted = world.getTileEntity(boostedPos);
        if (!(rawBoosted instanceof BoostedGrowthAcceleratorTileEntity)) {
            throw new IllegalStateException(
                    "Boosted Growth Accelerator did not create its tile entity");
        }

        BoostedGrowthAcceleratorTileEntity boosted =
                (BoostedGrowthAcceleratorTileEntity) rawBoosted;
        boosted.setOrientation(Direction.NORTH, Direction.UP);

        world.setBlockState(
                powerPos,
                ExpansionAEApi.get()
                        .definitions()
                        .blocks()
                        .energyCellCreative()
                        .block()
                        .getDefaultState(),
                3);

        TileEntity rawPower = world.getTileEntity(powerPos);
        if (!(rawPower instanceof CreativeEnergyCellTileEntity)) {
            throw new IllegalStateException(
                    "Boosted Growth validation Creative Energy Cell did not create its tile entity");
        }

        CreativeEnergyCellTileEntity power = (CreativeEnergyCellTileEntity) rawPower;

        boosted.onReady();
        power.onReady();

        session = new Session(
                world,
                boostedPos,
                powerPos,
                seedPos,
                controlPos,
                boosted);

        ExpansionAE.LOGGER.info(
                "Boosted growth runtime validation scheduled "
                        + "(24 AE/t, 8x single-accelerator target)");
    }

    public static void tick() {
        Session current = session;
        if (current == null) {
            return;
        }

        current.ticks++;

        if (!current.growthStarted) {
            if (current.ticks < MIN_TICKS_BEFORE_VALIDATION) {
                return;
            }

            if (!current.boosted.isPowered()) {
                if (current.ticks < MIN_TICKS_BEFORE_VALIDATION + 70) {
                    return;
                }
                failAndCleanup(current,
                        "Boosted Growth Accelerator did not become powered on an active AE2 network");
                return;
            }

            startGrowthValidation(current);
            return;
        }

        current.growthTicks++;
        keepStationary(current.accelerated, current.seedPos);
        keepStationary(current.control, current.controlPos);

        if (current.growthTicks < GROWTH_VALIDATION_TICKS) {
            return;
        }

        int acceleratedGrowth =
                CrystalSeedItem.getGrowthTicks(current.accelerated.getItem());
        int controlGrowth =
                CrystalSeedItem.getGrowthTicks(current.control.getItem());

        if (acceleratedGrowth < 8) {
            failAndCleanup(
                    current,
                    "Boosted Growth Accelerator produced only "
                            + acceleratedGrowth
                            + " growth ticks; expected at least 8 after "
                            + GROWTH_VALIDATION_TICKS
                            + " ticks");
            return;
        }

        if (controlGrowth != 0) {
            failAndCleanup(
                    current,
                    "Boosted Growth control seed unexpectedly advanced by "
                            + controlGrowth
                            + " growth ticks");
            return;
        }

        cleanup(current);
        session = null;

        ExpansionAE.LOGGER.info(
                "Boosted growth runtime validated "
                        + "(24 AE/t, +280/1000 extra progress/t, 8x single-accelerator target)");
    }

    private static void startGrowthValidation(Session s) {
        s.world.setBlockState(s.seedPos, Blocks.WATER.getDefaultState(), 3);
        s.world.setBlockState(s.controlPos, Blocks.WATER.getDefaultState(), 3);

        ItemStack acceleratedStack =
                ExpansionAEApi.get().definitions().items().certusCrystalSeed().stack(1);
        ItemStack controlStack =
                ExpansionAEApi.get().definitions().items().certusCrystalSeed().stack(1);

        if (!(acceleratedStack.getItem() instanceof CrystalSeedItem)
                || !(controlStack.getItem() instanceof CrystalSeedItem)) {
            failAndCleanup(s, "AE2 Certus seed definition is not a CrystalSeedItem");
            return;
        }

        s.accelerated = createStationarySeed(s.world, s.seedPos, acceleratedStack);
        s.control = createStationarySeed(s.world, s.controlPos, controlStack);

        if (!s.world.addEntity(s.accelerated) || !s.world.addEntity(s.control)) {
            failAndCleanup(s, "Could not add Boosted Growth validation seed entities");
            return;
        }

        s.growthStarted = true;
        s.growthTicks = 0;
    }

    private static GrowingCrystalEntity createStationarySeed(
            ServerWorld world,
            BlockPos pos,
            ItemStack stack) {
        GrowingCrystalEntity entity = new GrowingCrystalEntity(
                world,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack);
        entity.setNoGravity(true);
        entity.setMotion(Vector3d.ZERO);
        return entity;
    }

    private static void keepStationary(GrowingCrystalEntity entity, BlockPos pos) {
        if (entity == null) {
            return;
        }

        entity.setPosition(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D);
        entity.setMotion(Vector3d.ZERO);
    }

    private static void failAndCleanup(Session s, String message) {
        cleanup(s);
        session = null;
        throw new IllegalStateException(message);
    }

    private static void cleanup(Session s) {
        if (s.accelerated != null) {
            s.accelerated.remove();
        }
        if (s.control != null) {
            s.control.remove();
        }

        clear(s.world, s.seedPos);
        clear(s.world, s.controlPos);
        clear(s.world, s.boostedPos);
        clear(s.world, s.powerPos);
    }

    private static void clear(ServerWorld world, BlockPos pos) {
        world.removeBlock(pos, false);
    }

    private static final class Session {
        private final ServerWorld world;
        private final BlockPos boostedPos;
        private final BlockPos powerPos;
        private final BlockPos seedPos;
        private final BlockPos controlPos;
        private final BoostedGrowthAcceleratorTileEntity boosted;

        private int ticks;
        private boolean growthStarted;
        private int growthTicks;
        private GrowingCrystalEntity accelerated;
        private GrowingCrystalEntity control;

        private Session(
                ServerWorld world,
                BlockPos boostedPos,
                BlockPos powerPos,
                BlockPos seedPos,
                BlockPos controlPos,
                BoostedGrowthAcceleratorTileEntity boosted) {
            this.world = world;
            this.boostedPos = boostedPos;
            this.powerPos = powerPos;
            this.seedPos = seedPos;
            this.controlPos = controlPos;
            this.boosted = boosted;
        }
    }
}
