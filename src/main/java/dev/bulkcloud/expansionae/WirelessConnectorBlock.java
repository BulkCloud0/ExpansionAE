package dev.bulkcloud.expansionae;

import javax.annotation.Nullable;

import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public final class WirelessConnectorBlock extends AEBaseTileBlock<WirelessConnectorTile> {
    public WirelessConnectorBlock() {
        super(defaultProps(Material.IRON));
    }

    @Override
    public ActionResultType onActivated(World world, BlockPos pos, PlayerEntity player, Hand hand,
            @Nullable ItemStack heldItem, BlockRayTraceResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ActionResultType.PASS;
        }

        ItemStack stack = heldItem == null ? ItemStack.EMPTY : heldItem;
        if (stack.getItem() != ExpansionAE.WIRELESS_TOOL.get()
                && stack.getItem() != ExpansionAE.ADVANCED_WIRELESS_TOOL.get()) {
            return ActionResultType.PASS;
        }

        WirelessConnectorTile tile = getTileEntity(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }

        if (stack.getItem() == ExpansionAE.ADVANCED_WIRELESS_TOOL.get()) {
            return WirelessToolActions.handleAdvanced(
                    stack, world, pos, player, tile::setFrequency);
        }
        return WirelessToolActions.handleBasic(
                stack, world, pos, player, tile::setFrequency);
    }
}
