package dev.bulkcloud.expansionae;

import javax.annotation.Nonnull;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

/**
 * Oversized item inventory for the 1.16.5 port. Vanilla 1.16.5 serializes
 * ItemStack Count as a byte, so the real count is persisted separately.
 */
public final class OversizeItemInventory extends AppEngInternalInventory {
    private static final String COUNT_TAG = "ExpansionAECount";

    public OversizeItemInventory(IAEAppEngInventory owner, int size, int maxStack) {
        super(owner, size, maxStack);
    }

    @Override
    protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
        return getSlotLimit(slot);
    }

    @Override
    public void writeToNBT(CompoundNBT data, String name) {
        CompoundNBT inventory = new CompoundNBT();
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundNBT tag = new CompoundNBT();
            ItemStack identity = stack.copy();
            identity.setCount(1);
            identity.write(tag);
            tag.putInt(COUNT_TAG, stack.getCount());
            inventory.put("#" + slot, tag);
        }
        data.put(name, inventory);
    }

    @Override
    public void readFromNBT(CompoundNBT data, String name) {
        CompoundNBT inventory = data.getCompound(name);
        for (int slot = 0; slot < getSlots(); slot++) {
            CompoundNBT tag = inventory.getCompound("#" + slot);
            if (tag.isEmpty()) {
                this.stacks.set(slot, ItemStack.EMPTY);
                continue;
            }
            ItemStack stack = ItemStack.read(tag);
            int count = tag.contains(COUNT_TAG, 3) ? tag.getInt(COUNT_TAG) : stack.getCount();
            if (!stack.isEmpty()) {
                stack.setCount(Math.max(1, Math.min(count, getSlotLimit(slot))));
            }
            this.stacks.set(slot, stack);
        }
    }
}
