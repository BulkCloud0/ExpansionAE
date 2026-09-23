package com.bulkcloud.expansionae.mixin;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.BasicCellInventory;

/**
 * AE2 8.4.x only recognizes items implementing IStorageCell when deciding
 * whether a storage cell may be nested. ExpansionAE intentionally uses a
 * custom ICellHandler instead, because the default IStorageCell path would
 * force BasicCellInventory's 63-type/NBT-backed model.
 *
 * Reject ExpansionAE DISKs at the native BasicCellInventory insertion boundary
 * so UUID-backed aliases cannot bypass nesting rules by being treated as plain
 * items inside a normal AE2 storage cell.
 */
@Mixin(value = BasicCellInventory.class, remap = false)
abstract class BasicCellInventoryMixin<T extends IAEStack<T>> {

    @Inject(
            method = "injectItems",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void expansionae$rejectNestedDisk(
            T input,
            Actionable mode,
            IActionSource source,
            CallbackInfoReturnable<T> cir) {
        if (!(input instanceof IAEItemStack)) {
            return;
        }

        ItemStack stack = ((IAEItemStack) input).createItemStack();
        if (stack.getItem() instanceof DiskStorageCellItem) {
            cir.setReturnValue(input);
        }
    }
}
