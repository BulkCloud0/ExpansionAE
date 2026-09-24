package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.api.implementations.tiles.ICrankable;
import appeng.tile.AEBaseTileEntity;

public final class CrankedGrowthAcceleratorTileEntity
        extends AEBaseTileEntity
        implements ITickableTileEntity, ICrankable, WeightedCrystalGrowthAccelerator {
    public static final int GROWTH_WEIGHT = 8;
    public static final int CHARGE_PER_TURN = 40;
    public static final int MAX_CHARGE_TICKS = 80;

    private static final String CHARGE_NBT = "expansionae_cranked_growth_charge";

    private int chargeTicks;

    public CrankedGrowthAcceleratorTileEntity() {
        super(ExpansionAETileEntities.CRANKED_GROWTH_ACCELERATOR.get());
    }

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote || this.chargeTicks <= 0) {
            return;
        }

        this.chargeTicks--;
        if (this.chargeTicks == 0) {
            this.markForUpdate();
            this.saveChanges();
        }
    }

    @Override
    public boolean canTurn() {
        return this.chargeTicks < MAX_CHARGE_TICKS;
    }

    @Override
    public void applyTurn() {
        if (!this.canTurn()) {
            return;
        }

        boolean wasPowered = this.isPowered();
        this.chargeTicks = Math.min(
                MAX_CHARGE_TICKS,
                this.chargeTicks + CHARGE_PER_TURN);

        if (!wasPowered) {
            this.markForUpdate();
        }
        this.saveChanges();
    }

    @Override
    public boolean canCrankAttach(Direction directionToCrank) {
        return true;
    }

    @Override
    public boolean isPowered() {
        return this.chargeTicks > 0;
    }

    @Override
    public int expansionae$getGrowthWeight() {
        return GROWTH_WEIGHT;
    }

    public int expansionae$getChargeTicks() {
        return this.chargeTicks;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        this.chargeTicks = Math.max(
                0,
                Math.min(MAX_CHARGE_TICKS, data.getInt(CHARGE_NBT)));
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putInt(CHARGE_NBT, this.chargeTicks);
        return data;
    }
}
