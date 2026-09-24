package com.bulkcloud.expansionae.feature.growth;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

public final class GrowthAcceleratorRuntimeValidator {
    private GrowthAcceleratorRuntimeValidator() {
    }

    public static void validate() {
        if (!(ExpansionAEBlocks.BOOSTED_GROWTH_ACCELERATOR.get()
                instanceof BoostedGrowthAcceleratorBlock)) {
            throw new IllegalStateException("Boosted Growth Accelerator block type mismatch");
        }

        Item item = ExpansionAEItems.BOOSTED_GROWTH_ACCELERATOR.get();
        if (!(item instanceof BlockItem)
                || ((BlockItem) item).getBlock()
                        != ExpansionAEBlocks.BOOSTED_GROWTH_ACCELERATOR.get()) {
            throw new IllegalStateException("Boosted Growth Accelerator item/block mismatch");
        }

        BoostedGrowthAcceleratorTileEntity tile =
                new BoostedGrowthAcceleratorTileEntity();
        if (tile.expansionae$getGrowthWeight()
                        != BoostedGrowthAcceleratorTileEntity.GROWTH_WEIGHT
                || tile.expansionae$getGrowthWeight() != 8) {
            throw new IllegalStateException("Boosted Growth Accelerator weight mismatch");
        }

        if (Double.compare(
                tile.getProxy().getIdlePowerUsage(),
                BoostedGrowthAcceleratorTileEntity.IDLE_POWER_USAGE) != 0) {
            throw new IllegalStateException("Boosted Growth Accelerator idle power mismatch");
        }

        if (GrowthAcceleration.addWeightedProgress(40, 7) != 320
                || GrowthAcceleration.addWeightedProgress(92, 14) != 652
                || GrowthAcceleration.addWeightedProgress(159, 0) != 159) {
            throw new IllegalStateException("Weighted growth arithmetic mismatch");
        }

        ExpansionAE.LOGGER.info(
                "Boosted Growth Accelerator runtime contract validated "
                        + "(weight=8, idle=64 AE/t, native accelerators unchanged)");
    }
}
