package com.bulkcloud.expansionae.feature.growth;

import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.tile.misc.QuartzGrowthAcceleratorTileEntity;

public final class BoostedGrowthAcceleratorTileEntity
        extends QuartzGrowthAcceleratorTileEntity
        implements WeightedCrystalGrowthAccelerator {
    public static final int GROWTH_WEIGHT = 8;
    public static final double IDLE_POWER_USAGE = 64.0;

    public BoostedGrowthAcceleratorTileEntity() {
        super(ExpansionAETileEntities.BOOSTED_GROWTH_ACCELERATOR.get());
        this.getProxy().setIdlePowerUsage(IDLE_POWER_USAGE);
    }

    @Override
    public int expansionae$getGrowthWeight() {
        return GROWTH_WEIGHT;
    }
}
