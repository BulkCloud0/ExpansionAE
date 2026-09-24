package com.bulkcloud.expansionae.mixin;

import net.minecraft.tileentity.TileEntityType;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bulkcloud.expansionae.feature.extendedprovider.Interface36TileEntity;

import appeng.helpers.DualityInterface;
import appeng.tile.misc.InterfaceTileEntity;

@Mixin(value = InterfaceTileEntity.class, remap = false)
abstract class InterfaceTileEntityMixin {
    @Shadow
    @Final
    @Mutable
    private DualityInterface duality;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void expansionae$replaceInterfaceDuality(
            TileEntityType<?> type,
            CallbackInfo ci) {
        if (!((Object) this instanceof Interface36TileEntity)) {
            return;
        }

        InterfaceTileEntity tile = (InterfaceTileEntity) (Object) this;
        this.duality = new DualityInterface(tile.getProxy(), tile);
    }
}
