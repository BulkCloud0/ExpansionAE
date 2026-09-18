package dev.bulkcloud.expansionae;

import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PatternEncoderItem extends Item implements IGuiItem {
    public PatternEncoderItem(Properties properties) { super(properties.maxStackSize(1)); }
    @Override public PatternEncoderHost getGuiObject(ItemStack stack, int slot, World world, BlockPos pos) {
        return new PatternEncoderHost(stack, slot, world.isRemote);
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return new ActionResult<>(ActionResultType.PASS, player.getHeldItem(hand));
        if (!world.isRemote) ContainerOpener.openContainer(PatternEncoderContainer.TYPE, player, ContainerLocator.forHand(player, hand));
        return new ActionResult<>(ActionResultType.func_233537_a_(world.isRemote), player.getHeldItem(hand));
    }
}
