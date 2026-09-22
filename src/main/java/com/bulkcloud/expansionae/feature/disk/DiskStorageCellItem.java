package com.bulkcloud.expansionae.feature.disk;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;

public final class DiskStorageCellItem extends Item implements ICellWorkbenchItem {
    private final int capacity;
    private final double idleDrain;

    public DiskStorageCellItem(Properties properties, int capacity, double idleDrain) {
        super(properties.maxStackSize(1));
        this.capacity = capacity;
        this.idleDrain = idleDrain;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getIdleDrain() {
        return idleDrain;
    }

    @Override
    public boolean isEditable(ItemStack stack) {
        return true;
    }

    @Nonnull
    @Override
    public IItemHandler getUpgradesInventory(ItemStack stack) {
        return new CellUpgrades(stack, 2);
    }

    @Nonnull
    @Override
    public IItemHandler getConfigInventory(ItemStack stack) {
        return new CellConfig(stack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        String value = stack.getOrCreateTag().getString("FuzzyMode");
        if (value.isEmpty()) {
            return FuzzyMode.IGNORE_ALL;
        }

        try {
            return FuzzyMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return FuzzyMode.IGNORE_ALL;
        }
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
        stack.getOrCreateTag().putString("FuzzyMode", mode.name());
    }
}
