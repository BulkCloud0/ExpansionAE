package com.bulkcloud.expansionae.feature.disk;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(
            ItemStack stack,
            @Nullable World world,
            List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        long storedItems = cachedCount(stack, DiskCellInventory.TAG_ITEM_COUNT);
        long storedTypes = cachedCount(stack, DiskCellInventory.TAG_TYPE_COUNT);

        tooltip.add(new TranslationTextComponent(
                "tooltip.expansionae.disk.items",
                storedItems,
                capacity));
        tooltip.add(new TranslationTextComponent(
                "tooltip.expansionae.disk.types",
                storedTypes));
        tooltip.add(new TranslationTextComponent(
                "tooltip.expansionae.disk.no_type_limit")
                .mergeStyle(TextFormatting.DARK_GRAY));
    }

    private static long cachedCount(ItemStack stack, String key) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getLong(key)) : 0;
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
