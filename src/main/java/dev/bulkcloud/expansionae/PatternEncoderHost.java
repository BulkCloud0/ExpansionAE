package dev.bulkcloud.expansionae;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class PatternEncoderHost implements IGuiItemObject, IAEAppEngInventory {
    final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 1);
    final int slot;
    private final ItemStack stack;
    private final boolean remote;
    PatternEncoderHost(ItemStack stack, int slot, boolean remote) {
        this.stack = stack;
        this.slot = slot;
        this.remote = remote;
        inventory.readFromNBT(stack.getOrCreateTag(), "pattern");
        inventory.setMaxStackSize(0, 1);
    }
    @Override public ItemStack getItemStack() { return stack; }
    @Override public boolean isRemote() { return remote; }
    @Override public void saveChanges() { if (!remote) inventory.writeToNBT(stack.getOrCreateTag(), "pattern"); }
    @Override public void onChangeInventory(IItemHandler inv, int slot, InvOperation op, ItemStack removed, ItemStack added) { }
}
