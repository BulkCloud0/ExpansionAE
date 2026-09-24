package com.bulkcloud.expansionae.mixin;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bulkcloud.expansionae.feature.growth.GrowthAcceleration;
import com.bulkcloud.expansionae.feature.growth.WeightedCrystalGrowthAccelerator;

import appeng.entity.GrowingCrystalEntity;

@Mixin(value = GrowingCrystalEntity.class, remap = false)
abstract class GrowingCrystalEntityMixin {
    @Inject(
            method = "getSpeed",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void expansionae$applyWeightedGrowth(
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        GrowingCrystalEntity self = (GrowingCrystalEntity) (Object) this;
        int extraWeight = 0;

        BlockPos.Mutable testPos = new BlockPos.Mutable();
        for (Direction direction : Direction.values()) {
            TileEntity tile = self.getEntityWorld()
                    .getTileEntity(testPos.setAndMove(pos, direction));
            if (!(tile instanceof WeightedCrystalGrowthAccelerator)) {
                continue;
            }

            WeightedCrystalGrowthAccelerator accelerator =
                    (WeightedCrystalGrowthAccelerator) tile;
            if (!accelerator.isPowered()) {
                continue;
            }

            extraWeight += Math.max(0, accelerator.expansionae$getGrowthWeight() - 1);
        }

        cir.setReturnValue(
                GrowthAcceleration.addWeightedProgress(
                        cir.getReturnValue(),
                        extraWeight));
    }
}
