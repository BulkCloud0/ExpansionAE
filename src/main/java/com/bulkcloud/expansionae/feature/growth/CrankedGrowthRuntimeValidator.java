package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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
import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.implementations.tiles.ICrankable;
import appeng.api.implementations.tiles.ICrystalGrowthAccelerator;
import appeng.core.Api;
import appeng.tile.grindstone.CrankTileEntity;
import appeng.entity.GrowingCrystalEntity;
import appeng.items.misc.CrystalSeedItem;

public final class CrankedGrowthRuntimeValidator {
    private CrankedGrowthRuntimeValidator() {
    }

    public static void validate() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException("Dedicated server is not available for Cranked Growth validation");
        }

        ServerWorld world = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException("Overworld is not available for Cranked Growth validation");
        }

        BlockPos pos = world.getSpawnPoint().up(24);
        BlockPos acceleratedSeedPos = pos.west();
        BlockPos controlSeedPos = pos.add(8, 0, 0);

        clearTestPosition(world, pos);
        clearTestPosition(world, acceleratedSeedPos);
        clearTestPosition(world, controlSeedPos);
        for (Direction direction : Direction.values()) {
            if (!acceleratedSeedPos.offset(direction).equals(pos)) {
                clearTestPosition(world, acceleratedSeedPos.offset(direction));
            }
            clearTestPosition(world, controlSeedPos.offset(direction));
        }

        try {
            world.setBlockState(
                    pos,
                    ExpansionAEBlocks.CRANKED_GROWTH_ACCELERATOR.get().getDefaultState(),
                    3);

            TileEntity raw = world.getTileEntity(pos);
            if (!(raw instanceof CrankedGrowthAcceleratorTileEntity)) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not create its tile entity");
            }

            if (!(raw instanceof ICrankable) || !(raw instanceof ICrystalGrowthAccelerator)) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator does not expose the AE2 crank/growth contracts");
            }

            CrankedGrowthAcceleratorTileEntity tile =
                    (CrankedGrowthAcceleratorTileEntity) raw;

            if (tile.getStoredPower() != 0 || tile.isPowered() || !tile.canTurn()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator initial power state is invalid");
            }

            for (Direction direction : Direction.values()) {
                if (!tile.canCrankAttach(direction)) {
                    throw new IllegalStateException(
                            "Cranked Growth Accelerator rejected crank attachment on " + direction);
                }
            }

            // Prove interoperability with AE2's real crank tile instead of only
            // exercising ICrankable directly. The crank is placed east of the
            // accelerator and oriented so its working face points west.
            BlockPos crankPos = pos.east();
            world.removeBlock(crankPos, false);
            world.setBlockState(
                    crankPos,
                    Api.instance().definitions().blocks().crank().block().getDefaultState(),
                    3);

            TileEntity rawCrank = world.getTileEntity(crankPos);
            if (!(rawCrank instanceof CrankTileEntity)) {
                throw new IllegalStateException("AE2 crank did not create CrankTileEntity");
            }

            CrankTileEntity crank = (CrankTileEntity) rawCrank;
            crank.setOrientation(Direction.SOUTH, Direction.EAST);

            if (!crank.power()) {
                throw new IllegalStateException(
                        "AE2 crank refused to turn the Cranked Growth Accelerator");
            }

            for (int i = 0; i < 18; i++) {
                crank.tick();
            }

            if (tile.getStoredPower() != CrankedGrowthAcceleratorTileEntity.POWER_PER_CRANK_TURN) {
                throw new IllegalStateException(
                        "AE2 crank turn did not inject exactly 160 AE into the Cranked Growth Accelerator");
            }

            world.removeBlock(crankPos, false);

            for (int i = 1; i < 20; i++) {
                if (!tile.canTurn()) {
                    throw new IllegalStateException(
                            "Cranked Growth Accelerator stopped accepting turns before its buffer was full");
                }
                tile.applyTurn();
            }

            if (tile.getStoredPower() != CrankedGrowthAcceleratorTileEntity.MAX_STORED_POWER) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not reach the 3200 AE buffer");
            }
            if (tile.canTurn()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator accepts crank turns while its buffer is full");
            }
            if (!tile.isPowered()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator is not powered with a full buffer");
            }

            tile.applyTurn();
            if (tile.getStoredPower() != CrankedGrowthAcceleratorTileEntity.MAX_STORED_POWER) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator overflowed its maximum buffer");
            }

            tile.tick();
            int afterOneTick = CrankedGrowthAcceleratorTileEntity.MAX_STORED_POWER
                    - CrankedGrowthAcceleratorTileEntity.POWER_PER_TICK;
            if (tile.getStoredPower() != afterOneTick || !tile.isPowered()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not consume exactly 8 AE on its first powered tick");
            }

            BlockState poweredState = world.getBlockState(pos);
            if (!poweredState.get(CrankedGrowthAcceleratorBlock.POWERED)) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator powered blockstate did not turn on");
            }

            CompoundNBT saved = tile.write(new CompoundNBT());
            CrankedGrowthAcceleratorTileEntity restored =
                    ExpansionAETileEntities.CRANKED_GROWTH_ACCELERATOR.get().create();
            if (restored == null) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator TileEntityType could not create a tile");
            }
            restored.read(poweredState, saved);
            if (restored.getStoredPower() != afterOneTick || !restored.isPowered()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator power buffer did not survive NBT round-trip");
            }

            for (int i = 0; i < 399; i++) {
                tile.tick();
            }

            if (tile.getStoredPower() != 0) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not drain to zero after 400 powered ticks");
            }

            tile.tick();
            if (tile.isPowered()
                    || world.getBlockState(pos).get(CrankedGrowthAcceleratorBlock.POWERED)) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator remained powered after its buffer emptied");
            }

            if (!tile.canTurn()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not accept turns after draining");
            }

            tile.applyTurn();
            if (tile.getStoredPower() != CrankedGrowthAcceleratorTileEntity.POWER_PER_CRANK_TURN
                    || !tile.isPowered()) {
                throw new IllegalStateException(
                        "Cranked Growth Accelerator did not recover after a new crank turn");
            }

            validateActualCrystalGrowth(world, tile, acceleratedSeedPos, controlSeedPos);

            ExpansionAE.LOGGER.info(
                    "Cranked growth runtime validated "
                            + "(160 AE/turn, 3200 AE buffer, 8 AE/t, NBT, powered state + real crystal growth)");
        } finally {
            clearTestPosition(world, pos);
            clearTestPosition(world, acceleratedSeedPos);
            clearTestPosition(world, controlSeedPos);
            for (Direction direction : Direction.values()) {
                if (!acceleratedSeedPos.offset(direction).equals(pos)) {
                    clearTestPosition(world, acceleratedSeedPos.offset(direction));
                }
                clearTestPosition(world, controlSeedPos.offset(direction));
            }
        }
    }

    private static void validateActualCrystalGrowth(
            ServerWorld world,
            CrankedGrowthAcceleratorTileEntity tile,
            BlockPos acceleratedSeedPos,
            BlockPos controlSeedPos) {
        world.setBlockState(acceleratedSeedPos, Blocks.WATER.getDefaultState(), 3);
        world.setBlockState(controlSeedPos, Blocks.WATER.getDefaultState(), 3);

        ItemStack acceleratedStack =
                ExpansionAEApi.get().definitions().items().certusCrystalSeed().stack(1);
        ItemStack controlStack =
                ExpansionAEApi.get().definitions().items().certusCrystalSeed().stack(1);

        if (!(acceleratedStack.getItem() instanceof CrystalSeedItem)
                || !(controlStack.getItem() instanceof CrystalSeedItem)) {
            throw new IllegalStateException("AE2 Certus seed definition is not a CrystalSeedItem");
        }

        GrowingCrystalEntity accelerated = createStationarySeed(world, acceleratedSeedPos, acceleratedStack);
        GrowingCrystalEntity control = createStationarySeed(world, controlSeedPos, controlStack);

        // The previous recovery test leaves 160 AE in the buffer. One more turn
        // gives 320 AE, enough for all 30 validation ticks at 8 AE/t.
        tile.applyTurn();

        for (int i = 0; i < 30; i++) {
            tile.tick();

            keepStationary(accelerated, acceleratedSeedPos);
            accelerated.tick();
            keepStationary(accelerated, acceleratedSeedPos);

            keepStationary(control, controlSeedPos);
            control.tick();
            keepStationary(control, controlSeedPos);
        }

        int acceleratedGrowth = CrystalSeedItem.getGrowthTicks(accelerated.getItem());
        int controlGrowth = CrystalSeedItem.getGrowthTicks(control.getItem());

        if (acceleratedGrowth < 1) {
            throw new IllegalStateException(
                    "Powered Cranked Growth Accelerator did not advance an adjacent AE2 crystal seed");
        }

        if (controlGrowth != 0) {
            throw new IllegalStateException(
                    "Control AE2 crystal seed unexpectedly advanced without an adjacent accelerator");
        }

        if (acceleratedGrowth <= controlGrowth) {
            throw new IllegalStateException(
                    "Cranked Growth Accelerator did not outperform the unaccelerated control seed");
        }
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
        entity.setPosition(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D);
        entity.setMotion(Vector3d.ZERO);
    }

    private static void clearTestPosition(ServerWorld world, BlockPos pos) {
        world.removeBlock(pos, false);
    }
}
