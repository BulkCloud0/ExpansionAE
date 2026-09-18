package dev.bulkcloud.expansionae;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class PatternModifierHost implements IGuiItemObject, IAEAppEngInventory {
    final AppEngInternalInventory patterns = new AppEngInternalInventory(this, 27);
    final AppEngInternalInventory target = new AppEngInternalInventory(this, 1);
    final AppEngInternalInventory blanks = new AppEngInternalInventory(this, 4);
    final AppEngInternalInventory cloneOutput = new AppEngInternalInventory(this, 1);
    final AppEngInternalInventory replace = new AppEngInternalInventory(this, 2);

    final int slot;
    private final ItemStack stack;
    private final boolean remote;

    PatternModifierHost(ItemStack stack, int slot, boolean remote) {
        this.stack = stack;
        this.slot = slot;
        this.remote = remote;
        read(patterns, "patterns");
        read(target, "target");
        read(blanks, "blanks");
        read(cloneOutput, "clone");
        read(replace, "replace");

        for (int i = 0; i < patterns.getSlots(); i++) patterns.setMaxStackSize(i, 1);
        target.setMaxStackSize(0, 1);
        cloneOutput.setMaxStackSize(0, 1);
        for (int i = 0; i < blanks.getSlots(); i++) blanks.setMaxStackSize(i, 64);
        for (int i = 0; i < replace.getSlots(); i++) replace.setMaxStackSize(i, 1);
    }

    private void read(AppEngInternalInventory inv, String key) {
        inv.readFromNBT(stack.getOrCreateTag(), key);
    }

    @Override public ItemStack getItemStack() { return stack; }
    @Override public boolean isRemote() { return remote; }

    @Override
    public void saveChanges() {
        if (remote) return;
        write(patterns, "patterns");
        write(target, "target");
        write(blanks, "blanks");
        write(cloneOutput, "clone");
        write(replace, "replace");
    }

    private void write(AppEngInternalInventory inv, String key) {
        inv.writeToNBT(stack.getOrCreateTag(), key);
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation op, ItemStack removed, ItemStack added) {
        saveChanges();
    }
}
