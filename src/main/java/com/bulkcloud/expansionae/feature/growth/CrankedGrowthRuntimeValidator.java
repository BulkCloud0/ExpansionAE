package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.implementations.tiles.ICrankable;
import appeng.api.implementations.tiles.ICrystalGrowthAccelerator;

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
        world.removeBlock(pos, false);

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

            for (int i = 0; i < 20; i++) {
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

            ExpansionAE.LOGGER.info(
                    "Cranked growth runtime validated (160 AE/turn, 3200 AE buffer, 8 AE/t, NBT + powered state)");
        } finally {
            world.removeBlock(pos, false);
        }
    }
}
