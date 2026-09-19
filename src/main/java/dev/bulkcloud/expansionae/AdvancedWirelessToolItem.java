package dev.bulkcloud.expansionae;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 1.16.5 adaptation of ExtendedAE's Advanced Wireless Tool.
 *
 * Sneak-right-click in the air toggles queue-add/connect mode. In add mode,
 * interacting with a connector/hub port creates a source endpoint and appends
 * its binding to the queue. In connect mode, each interaction consumes the
 * oldest queued binding and connects the target endpoint.
 */
public final class AdvancedWirelessToolItem extends Item {
    static final String TAG_ADD_MODE = "wirelessAddMode";
    static final String TAG_CONNECTIONS = "wirelessConnections";
    static final int MAX_QUEUE = 32;

    public AdvancedWirelessToolItem(Properties properties) {
        super(properties.maxStackSize(1));
    }

    static boolean isAddMode(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_ADD_MODE);
    }

    static ListNBT getQueue(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        return tag.getList(TAG_CONNECTIONS, 10);
    }

    static void saveQueue(ItemStack stack, ListNBT queue) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.put(TAG_CONNECTIONS, queue);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) {
            return ActionResult.resultPass(stack);
        }

        if (!world.isRemote) {
            CompoundNBT tag = stack.getOrCreateTag();
            boolean next = !tag.getBoolean(TAG_ADD_MODE);
            tag.putBoolean(TAG_ADD_MODE, next);
            player.sendStatusMessage(new TranslationTextComponent(
                    next ? "chat.expansionae.wireless_advanced_add"
                         : "chat.expansionae.wireless_advanced_connect"), true);
        }
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        ListNBT queue = getQueue(stack);
        tooltip.add(new TranslationTextComponent(
                isAddMode(stack)
                        ? "tooltip.expansionae.wireless_advanced.add"
                        : "tooltip.expansionae.wireless_advanced.connect"));
        tooltip.add(new TranslationTextComponent(
                "tooltip.expansionae.wireless_advanced.queue", queue.size(), MAX_QUEUE));
    }
}
