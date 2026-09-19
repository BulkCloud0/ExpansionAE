package dev.bulkcloud.expansionae;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import appeng.block.AEBaseTileBlock;
import appeng.util.InteractionUtil;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
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
        if (stack.getItem() != ExpansionAE.WIRELESS_TOOL.get()) {
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
            player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless_hub_full"), true);
            return ActionResultType.FAIL;
        }

        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.contains(WirelessLinkToolItem.TAG_FREQUENCY)) {
            long frequency;
            do {
                frequency = ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
            } while (frequency == 0L);

            tile.setFrequency(port, frequency);
            tag.putLong(WirelessLinkToolItem.TAG_FREQUENCY, frequency);
            tag.putString(WirelessLinkToolItem.TAG_DIMENSION, world.getDimensionKey().getLocation().toString());
            tag.putInt(WirelessLinkToolItem.TAG_X, pos.getX());
            tag.putInt(WirelessLinkToolItem.TAG_Y, pos.getY());
            tag.putInt(WirelessLinkToolItem.TAG_Z, pos.getZ());
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_hub_bound", port + 1, pos.getX(), pos.getY(), pos.getZ()), true);
            return ActionResultType.SUCCESS;
        }

        String dimension = world.getDimensionKey().getLocation().toString();
        if (!dimension.equals(tag.getString(WirelessLinkToolItem.TAG_DIMENSION))) {
            player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless_cross_dimension"), true);
            return ActionResultType.FAIL;
        }

        BlockPos first = new BlockPos(
                tag.getInt(WirelessLinkToolItem.TAG_X),
                tag.getInt(WirelessLinkToolItem.TAG_Y),
                tag.getInt(WirelessLinkToolItem.TAG_Z));
        if (first.equals(pos)) {
            player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless_same_connector"), true);
            return ActionResultType.FAIL;
        }
        if (first.distanceSq(pos) > WirelessLink.MAX_RANGE * WirelessLink.MAX_RANGE) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_out_of_range", (int) WirelessLink.MAX_RANGE), true);
            return ActionResultType.FAIL;
        }

        tile.setFrequency(port, tag.getLong(WirelessLinkToolItem.TAG_FREQUENCY));
        WirelessLinkToolItem.clearBinding(stack);
        player.sendStatusMessage(new TranslationTextComponent(
                "chat.expansionae.wireless_hub_linked", port + 1, first.getX(), first.getY(), first.getZ()), true);
        return ActionResultType.SUCCESS;
    }
}
