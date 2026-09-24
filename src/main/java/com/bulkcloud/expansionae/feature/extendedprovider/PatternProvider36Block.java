package com.bulkcloud.expansionae.feature.extendedprovider;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;

public final class PatternProvider36Block
        extends AEBaseTileBlock<PatternProvider36TileEntity> {
    public PatternProvider36Block() {
        super(defaultProps(Material.IRON));
        this.setTileEntity(
                PatternProvider36TileEntity.class,
                PatternProvider36TileEntity::new);
    }

    @Override
    public ActionResultType onActivated(
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            @Nullable ItemStack heldItem,
            BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ActionResultType.PASS;
        }

        PatternProvider36TileEntity tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        if (!world.isRemote()) {
            PatternProvider36Container.open(player, tile);
        }
        return ActionResultType.func_233537_a_(world.isRemote());
    }
}
