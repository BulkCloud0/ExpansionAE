package dev.bulkcloud.expansionae;

import javax.annotation.Nullable;

import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/** Block shell for the 1.16.5 Crystal Fixer adaptation. */
public final class CrystalFixerBlock extends AEBaseTileBlock<CrystalFixerTile> {
    public CrystalFixerBlock() {
        super(defaultProps(Material.IRON).hardnessAndResistance(3.0F, 8.0F).notSolid());
    }

    @Override
    public int getOpacity(BlockState state, IBlockReader world, BlockPos pos) {
        return 2;
    }

    @Override
    public ActionResultType onActivated(World world, BlockPos pos, PlayerEntity player, Hand hand,
            @Nullable ItemStack heldItem, BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ActionResultType.PASS;
        }
        if (!world.isRemote()) {
            CrystalFixerTile tile = getTileEntity(world, pos);
            if (tile != null) {
                tile.activate(player);
            }
        }
        return ActionResultType.func_233537_a_(world.isRemote());
    }
}
