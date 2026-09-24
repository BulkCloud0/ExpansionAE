package com.bulkcloud.expansionae.feature.extendedprovider;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

public final class PatternProvider36RuntimeValidator {
    private PatternProvider36RuntimeValidator() {
    }

    public static void validate() {
        if (!(ExpansionAEBlocks.PATTERN_PROVIDER_36.get()
                instanceof PatternProvider36Block)) {
            throw new IllegalStateException("36-slot Pattern Provider block type mismatch");
        }

        Item item = ExpansionAEItems.PATTERN_PROVIDER_36.get();
        if (!(item instanceof BlockItem)
                || ((BlockItem) item).getBlock()
                        != ExpansionAEBlocks.PATTERN_PROVIDER_36.get()) {
            throw new IllegalStateException("36-slot Pattern Provider item/block mismatch");
        }

        PatternProvider36TileEntity tile =
                new PatternProvider36TileEntity();
        if (tile.getPatterns().getSlots()
                != PatternProvider36TileEntity.PATTERN_SLOTS) {
            throw new IllegalStateException("36-slot Pattern Provider inventory size mismatch");
        }

        ItemStack marker = new ItemStack(net.minecraft.item.Items.STONE);
        tile.getPatterns().insertItem(35, marker, false);
        CompoundNBT saved = new CompoundNBT();
        tile.write(saved);

        PatternProvider36TileEntity restored =
                new PatternProvider36TileEntity();
        restored.read(
                ExpansionAEBlocks.PATTERN_PROVIDER_36.get().getDefaultState(),
                saved);

        if (restored.getPatterns().getStackInSlot(35).isEmpty()
                || restored.getPatterns().getStackInSlot(35).getItem()
                        != net.minecraft.item.Items.STONE) {
            throw new IllegalStateException(
                    "36-slot Pattern Provider slot 36 did not persist");
        }

        ExpansionAE.LOGGER.info(
                "36-slot Pattern Provider runtime contract validated "
                        + "(36 real pattern slots + slot 36 NBT persistence)");
    }
}
