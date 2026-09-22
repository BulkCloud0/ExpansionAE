package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.implementations.tiles.ICrankable;
import appeng.api.implementations.tiles.ICrystalGrowthAccelerator;

public final class CrankedGrowthAcceleratorTileEntity extends TileEntity
        implements ITickableTileEntity, ICrankable, ICrystalGrowthAccelerator {

    public static final int POWER_PER_CRANK_TURN = 160;
    public static final int POWER_PER_TICK = 8;
    public static final int MAX_STORED_POWER = 20 * POWER_PER_CRANK_TURN;

    private static final String TAG_STORED_POWER = "stored_power";

    private int storedPower;
    private boolean powered;

    public CrankedGrowthAcceleratorTileEntity() {
        super(ExpansionAETileEntities.CRANKED_GROWTH_ACCELERATOR.get());
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) {
            return;
        }

        boolean activeThisTick = storedPower >= POWER_PER_TICK;
        if (activeThisTick) {
            storedPower -= POWER_PER_TICK;
            markDirty();
        }

        setPowered(activeThisTick);
    }

    @Override
    public boolean canTurn() {
        return world == null || !world.isRemote
                ? storedPower <= MAX_STORED_POWER - POWER_PER_CRANK_TURN
                : false;
    }

    @Override
    public void applyTurn() {
        if (world != null && world.isRemote) {
            return;
        }

        if (!canTurn()) {
            return;
        }

        storedPower += POWER_PER_CRANK_TURN;
        markDirty();

        if (storedPower >= POWER_PER_TICK) {
            setPowered(true);
        }
    }

    @Override
    public boolean canCrankAttach(Direction directionToCrank) {
        return true;
    }

    @Override
    public boolean isPowered() {
        return powered;
    }

    public int getStoredPower() {
        return storedPower;
    }

    @Override
    public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);
        storedPower = Math.max(0, Math.min(MAX_STORED_POWER, tag.getInt(TAG_STORED_POWER)));
        powered = storedPower >= POWER_PER_TICK;
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);
        tag.putInt(TAG_STORED_POWER, storedPower);
        return tag;
    }

    private void setPowered(boolean value) {
        if (powered == value) {
            return;
        }

        powered = value;

        if (world == null || world.isRemote || isRemoved()) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CrankedGrowthAcceleratorBlock
                && state.hasProperty(CrankedGrowthAcceleratorBlock.POWERED)
                && state.get(CrankedGrowthAcceleratorBlock.POWERED) != value) {
            world.setBlockState(
                    pos,
                    state.with(CrankedGrowthAcceleratorBlock.POWERED, value),
                    3);
        }
    }
}
