package dev.bulkcloud.expansionae;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongConsumer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

final class WirelessToolActions {
    private WirelessToolActions() {
    }

    static ActionResultType handleBasic(ItemStack stack, World world, BlockPos pos,
            PlayerEntity player, LongConsumer setFrequency) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.contains(WirelessLinkToolItem.TAG_FREQUENCY)) {
            long frequency = newFrequency();
            setFrequency.accept(frequency);
            writeBinding(tag, frequency, world, pos);
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_bound", pos.getX(), pos.getY(), pos.getZ()), true);
            return ActionResultType.SUCCESS;
        }

        if (!validateBinding(tag, world, pos, player)) {
            return ActionResultType.FAIL;
        }

        setFrequency.accept(tag.getLong(WirelessLinkToolItem.TAG_FREQUENCY));
        WirelessLinkToolItem.clearBinding(stack);
        BlockPos first = readPos(tag);
        player.sendStatusMessage(new TranslationTextComponent(
                "chat.expansionae.wireless_linked", first.getX(), first.getY(), first.getZ()), true);
        return ActionResultType.SUCCESS;
    }

    static ActionResultType handleAdvanced(ItemStack stack, World world, BlockPos pos,
            PlayerEntity player, LongConsumer setFrequency) {
        ListNBT queue = AdvancedWirelessToolItem.getQueue(stack);

        if (AdvancedWirelessToolItem.isAddMode(stack)) {
            if (queue.size() >= AdvancedWirelessToolItem.MAX_QUEUE) {
                player.sendStatusMessage(new TranslationTextComponent(
                        "chat.expansionae.wireless_advanced_full",
                        AdvancedWirelessToolItem.MAX_QUEUE), true);
                return ActionResultType.FAIL;
            }

            long frequency = newFrequency();
            setFrequency.accept(frequency);
            CompoundNBT binding = new CompoundNBT();
            writeBinding(binding, frequency, world, pos);
            queue.add(binding);
            AdvancedWirelessToolItem.saveQueue(stack, queue);
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_advanced_queued",
                    queue.size(), pos.getX(), pos.getY(), pos.getZ()), true);
            return ActionResultType.SUCCESS;
        }

        if (queue.isEmpty()) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_advanced_empty"), true);
            return ActionResultType.FAIL;
        }

        CompoundNBT binding = queue.getCompound(0);
        if (!validateBinding(binding, world, pos, player)) {
            return ActionResultType.FAIL;
        }

        setFrequency.accept(binding.getLong(WirelessLinkToolItem.TAG_FREQUENCY));
        BlockPos first = readPos(binding);
        queue.remove(0);
        AdvancedWirelessToolItem.saveQueue(stack, queue);
        player.sendStatusMessage(new TranslationTextComponent(
                "chat.expansionae.wireless_advanced_connected",
                first.getX(), first.getY(), first.getZ(), queue.size()), true);
        return ActionResultType.SUCCESS;
    }

    private static long newFrequency() {
        long frequency;
        do {
            frequency = ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
        } while (frequency == 0L);
        return frequency;
    }

    private static void writeBinding(CompoundNBT tag, long frequency, World world, BlockPos pos) {
        tag.putLong(WirelessLinkToolItem.TAG_FREQUENCY, frequency);
        tag.putString(WirelessLinkToolItem.TAG_DIMENSION,
                world.getDimensionKey().getLocation().toString());
        tag.putInt(WirelessLinkToolItem.TAG_X, pos.getX());
        tag.putInt(WirelessLinkToolItem.TAG_Y, pos.getY());
        tag.putInt(WirelessLinkToolItem.TAG_Z, pos.getZ());
    }

    private static BlockPos readPos(CompoundNBT tag) {
        return new BlockPos(
                tag.getInt(WirelessLinkToolItem.TAG_X),
                tag.getInt(WirelessLinkToolItem.TAG_Y),
                tag.getInt(WirelessLinkToolItem.TAG_Z));
    }

    private static boolean validateBinding(CompoundNBT tag, World world, BlockPos target,
            PlayerEntity player) {
        String dimension = world.getDimensionKey().getLocation().toString();
        if (!dimension.equals(tag.getString(WirelessLinkToolItem.TAG_DIMENSION))) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_cross_dimension"), true);
            return false;
        }

        BlockPos source = readPos(tag);
        if (source.equals(target)) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_same_connector"), true);
            return false;
        }

        if (source.distanceSq(target) > WirelessLink.MAX_RANGE * WirelessLink.MAX_RANGE) {
            player.sendStatusMessage(new TranslationTextComponent(
                    "chat.expansionae.wireless_out_of_range",
                    (int) WirelessLink.MAX_RANGE), true);
            return false;
        }
        return true;
    }
}
