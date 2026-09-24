package com.bulkcloud.expansionae.feature.growth;

import appeng.block.misc.QuartzGrowthAcceleratorBlock;

public final class BoostedGrowthAcceleratorBlock extends QuartzGrowthAcceleratorBlock {
    public BoostedGrowthAcceleratorBlock() {
        super();
        this.setTileEntity(
                BoostedGrowthAcceleratorTileEntity.class,
                BoostedGrowthAcceleratorTileEntity::new);
    }
}
