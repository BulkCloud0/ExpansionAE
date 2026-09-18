package dev.bulkcloud.expansionae;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;

import appeng.util.InteractionUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public final class WirelessConnectToolItem extends Item {
    public WirelessConnectToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        TileEntity te = world.getTileEntity(context.getPos());
        if (!(te instanceof WirelessConnectorTile)) return ActionResultType.PASS;
        if (world.isRemote) return ActionResultType.SUCCESS;

        WirelessConnectorTile connector = (WirelessConnectorTile) te;
        ItemStack tool = context.getItem();
        CompoundNBT tag = tool.getOrCreateTag();
        PlayerEntity player = context.getPlayer();

        if (!tag.contains("freq")) {
            long freq = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
            connector.setFrequency(freq);
            tag.putLong("freq", freq);
            tag.putInt("x", context.getPos().getX());
            tag.putInt("y", context.getPos().getY());
            tag.putInt("z", context.getPos().getZ());
            if (player != null) {
                player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless.first"), true);
            }
        } else {
            connector.setFrequency(-Math.abs(tag.getLong("freq")));
            tool.setTag(null);
            if (player != null) {
                player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless.linked"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack tool = player.getHeldItem(hand);
        if (InteractionUtil.isInAlternateUseMode(player) && tool.hasTag()) {
            if (!world.isRemote) {
                tool.setTag(null);
                player.sendStatusMessage(new TranslationTextComponent("chat.expansionae.wireless.clear"), true);
            }
            return ActionResult.resultSuccess(tool);
        }
        return ActionResult.resultPass(tool);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains("freq")) {
            tooltip.add(new TranslationTextComponent("tooltip.expansionae.wireless.bound",
                    tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
        }
    }
}
