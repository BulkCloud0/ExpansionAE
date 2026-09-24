package com.bulkcloud.expansionae.feature.extendedprovider;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import appeng.api.util.IOrientable;
import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;

public final class Interface36Block
        extends AEBaseTileBlock<Interface36TileEntity> {
    public static final BooleanProperty OMNIDIRECTIONAL =
            BooleanProperty.create("omnidirectional");

    public Interface36Block() {
        super(defaultProps(Material.IRON));
        this.setDefaultState(this.getDefaultState().with(OMNIDIRECTIONAL, true));
        this.setTileEntity(Interface36TileEntity.class, Interface36TileEntity::new);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(OMNIDIRECTIONAL);
    }

    @Override
    protected BlockState updateBlockStateFromTileEntity(
            BlockState currentState,
            Interface36TileEntity tile) {
        return currentState.with(OMNIDIRECTIONAL, tile.isOmniDirectional());
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

        Interface36TileEntity tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        if (!world.isRemote()) {
            Interface36Container.open(player, tile);
        }
        return ActionResultType.func_233537_a_(world.isRemote());
    }

    @Override
    protected boolean hasCustomRotation() {
        return true;
    }

    @Override
    protected void customRotateBlock(IOrientable rotatable, Direction axis) {
        if (rotatable instanceof Interface36TileEntity) {
            ((Interface36TileEntity) rotatable).setSide(axis);
        }
    }
}
