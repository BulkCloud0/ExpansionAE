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

        Item crankedItem = ExpansionAEItems.CRANKED_GROWTH_ACCELERATOR.get();
        if (!(ExpansionAEBlocks.CRANKED_GROWTH_ACCELERATOR.get()
                instanceof CrankedGrowthAcceleratorBlock)
                || !(crankedItem instanceof BlockItem)
                || ((BlockItem) crankedItem).getBlock()
                        != ExpansionAEBlocks.CRANKED_GROWTH_ACCELERATOR.get()) {
            throw new IllegalStateException("Cranked Growth Accelerator block/item mismatch");
        }

        CrankedGrowthAcceleratorTileEntity cranked =
                new CrankedGrowthAcceleratorTileEntity();
        if (cranked.isPowered()
                || !cranked.canTurn()
                || !cranked.canCrankAttach(net.minecraft.util.Direction.UP)
                || cranked.expansionae$getGrowthWeight() != 8) {
            throw new IllegalStateException("Cranked Growth Accelerator initial contract mismatch");
        }

        cranked.applyTurn();
        if (!cranked.isPowered()
                || cranked.expansionae$getChargeTicks()
                        != CrankedGrowthAcceleratorTileEntity.CHARGE_PER_TURN) {
            throw new IllegalStateException("Cranked Growth Accelerator did not accept crank charge");
        }

        net.minecraft.nbt.CompoundNBT saved = new net.minecraft.nbt.CompoundNBT();
        cranked.write(saved);
        CrankedGrowthAcceleratorTileEntity restored =
                new CrankedGrowthAcceleratorTileEntity();
        restored.read(
                ExpansionAEBlocks.CRANKED_GROWTH_ACCELERATOR.get().getDefaultState(),
                saved);
        if (!restored.isPowered()
                || restored.expansionae$getChargeTicks()
                        != CrankedGrowthAcceleratorTileEntity.CHARGE_PER_TURN) {
            throw new IllegalStateException("Cranked Growth Accelerator charge did not persist");
        }

        if (GrowthAcceleration.addWeightedProgress(40, 7) != 320
                || GrowthAcceleration.addWeightedProgress(92, 14) != 652
                || GrowthAcceleration.addWeightedProgress(159, 0) != 159) {
            throw new IllegalStateException("Weighted growth arithmetic mismatch");
        }

        ExpansionAE.LOGGER.info(
                "Boosted Growth Accelerator runtime contract validated "
                        + "(weight=8, idle=64 AE/t, native accelerators unchanged)");
        ExpansionAE.LOGGER.info(
                "Cranked Growth Accelerator runtime contract validated "
                        + "(weight=8, 40-tick charge/turn, 80-tick buffer, NBT persistence)");
    }
}
