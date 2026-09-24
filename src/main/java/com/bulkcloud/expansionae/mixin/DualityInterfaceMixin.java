package com.bulkcloud.expansionae.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bulkcloud.expansionae.feature.extendedprovider.Interface36TileEntity;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.MultiCraftingTracker;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;

@Mixin(value = DualityInterface.class, remap = false)
abstract class DualityInterfaceMixin {
    @Shadow
    @Final
    @Mutable
    private IAEItemStack[] requireWork;

    @Shadow
    @Final
    @Mutable
    private MultiCraftingTracker craftingTracker;

    @Shadow
    @Final
    private IInterfaceHost iHost;

    @Shadow
    @Final
    @Mutable
    private AppEngInternalAEInventory config;

    @Shadow
    @Final
    @Mutable
    private AppEngInternalInventory storage;

    @Shadow
    @Final
    @Mutable
    private AppEngInternalInventory patterns;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void expansionae$expandInterfaceInventories(
            AENetworkProxy networkProxy,
            IInterfaceHost host,
            CallbackInfo ci) {
        if (!(host instanceof Interface36TileEntity)) {
            return;
        }

        IAEAppEngInventory owner = (IAEAppEngInventory) (Object) this;
        this.requireWork = new IAEItemStack[Interface36TileEntity.SLOTS];
        this.config =
                new AppEngInternalAEInventory(owner, Interface36TileEntity.SLOTS);
        this.storage =
                new AppEngInternalInventory(owner, Interface36TileEntity.SLOTS);
        this.patterns =
                new AppEngInternalInventory(owner, Interface36TileEntity.SLOTS);
        this.craftingTracker =
                new MultiCraftingTracker(host, Interface36TileEntity.SLOTS);
    }

    @ModifyConstant(
            method = { "readConfig", "updateCraftingList", "updateStorage" },
            constant = @Constant(intValue = 9))
    private int expansionae$expandInterfaceLoopBounds(int original) {
        return this.iHost instanceof Interface36TileEntity
                ? Interface36TileEntity.SLOTS
                : original;
    }
}
