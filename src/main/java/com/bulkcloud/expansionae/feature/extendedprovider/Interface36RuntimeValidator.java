package com.bulkcloud.expansionae.feature.extendedprovider;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.core.registry.ExpansionAEBlocks;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.helpers.DualityInterface;

public final class Interface36RuntimeValidator {
    private Interface36RuntimeValidator() {
    }

    public static void validate() {
        if (!(ExpansionAEBlocks.INTERFACE_36.get() instanceof Interface36Block)) {
            throw new IllegalStateException("36-slot Interface block type mismatch");
        }

        Item item = ExpansionAEItems.INTERFACE_36.get();
        if (!(item instanceof BlockItem)
                || ((BlockItem) item).getBlock()
                        != ExpansionAEBlocks.INTERFACE_36.get()) {
            throw new IllegalStateException("36-slot Interface item/block mismatch");
        }

        Interface36TileEntity tile = new Interface36TileEntity();
        DualityInterface duality = tile.getInterfaceDuality();
        Interface36Container.assertExpanded(duality);

        duality.getConfig().insertItem(
                35,
                new ItemStack(net.minecraft.item.Items.STONE),
                false);
        duality.getStorage().insertItem(
                35,
                new ItemStack(net.minecraft.item.Items.COBBLESTONE, 7),
                false);
        duality.getPatterns().insertItem(
                35,
                new ItemStack(net.minecraft.item.Items.PAPER),
                false);

        CompoundNBT saved = new CompoundNBT();
        tile.write(saved);

        Interface36TileEntity restored = new Interface36TileEntity();
        restored.read(
                ExpansionAEBlocks.INTERFACE_36.get().getDefaultState(),
                saved);
        Interface36Container.assertExpanded(restored.getInterfaceDuality());

        if (restored.getInterfaceDuality().getConfig().getStackInSlot(35).isEmpty()
                || restored.getInterfaceDuality().getStorage()
                        .getStackInSlot(35).getCount() != 7
                || restored.getInterfaceDuality().getPatterns()
                        .getStackInSlot(35).isEmpty()) {
            throw new IllegalStateException(
                    "36-slot Interface slot 36 did not persist across config/storage/pattern banks");
        }

        ExpansionAE.LOGGER.info(
                "36-slot Interface runtime contract validated "
                        + "(36 config + 36 storage + 36 pattern slots, slot 36 persistence)");
    }
}
