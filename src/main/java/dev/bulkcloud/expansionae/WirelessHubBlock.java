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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public final class WirelessHubBlock extends AEBaseTileBlock<WirelessHubTile> {
    public WirelessHubBlock() {
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

        WirelessHubTile tile = getTileEntity(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }

        int port = tile.allocatePort();
        if (port < 0) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_hub_full"), true);
            return ActionResultType.FAIL;
        }

        if (stack.getItem() == ExpansionAE.ADVANCED_WIRELESS_TOOL.get()) {
            return WirelessToolActions.handleAdvanced(
                    stack, world, pos, player, frequency -> tile.setFrequency(port, frequency));
        }
        return WirelessToolActions.handleBasic(
                stack, world, pos, player, frequency -> tile.setFrequency(port, frequency));
    }
}
