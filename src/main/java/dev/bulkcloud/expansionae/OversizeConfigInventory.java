package dev.bulkcloud.expansionae;

import javax.annotation.Nonnull;

import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.inv.IAEAppEngInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/** Config/fake inventory that exposes the Oversize Interface's 16x capacity. */
public final class OversizeConfigInventory extends AppEngInternalAEInventory {
    private final int maxStack;

    public OversizeConfigInventory(IAEAppEngInventory owner, int size, int maxStack) {
        super(owner, size);
        this.maxStack = maxStack;
        setMaxStackSize(maxStack);
    }

    @Override
    protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
        return maxStack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemHandlerHelper.canItemStacksStack(stack, existing)) return stack;

        int moved = Math.min(Math.max(0, maxStack - existing.getCount()), stack.getCount());
        if (moved <= 0) return stack;

        if (!simulate) {
            ItemStack updated = existing.isEmpty() ? stack.copy() : existing.copy();
            if (existing.isEmpty()) updated.setCount(moved); else updated.grow(moved);
            setStackInSlot(slot, updated);
        }

        return moved == stack.getCount()
                ? ItemStack.EMPTY
                : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - moved);
    }
}
