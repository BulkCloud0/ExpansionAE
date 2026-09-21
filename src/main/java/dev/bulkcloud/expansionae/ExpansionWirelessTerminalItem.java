package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import appeng.api.features.ILocatable;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.Api;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.world.World;

/**
 * AE2 8.4 wireless-terminal handler that opens an ExpansionAE container while
 * retaining the native security-station link, battery and WAP range rules.
 */
public final class ExpansionWirelessTerminalItem extends WirelessTerminalItem {
    private final Supplier<ContainerType<?>> containerType;

    public ExpansionWirelessTerminalItem(Item.Properties properties,
            Supplier<ContainerType<?>> containerType) {
        super(properties);
        this.containerType = containerType;
    }

    @Override
    public boolean canHandle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote()) {
            open(stack, player, hand);
        }
        return new ActionResult<>(ActionResultType.func_233537_a_(world.isRemote()), stack);
    }

    private void open(ItemStack stack, PlayerEntity player, Hand hand) {
        String unparsedKey = getEncryptionKey(stack);
        if (unparsedKey.isEmpty()) {
            player.sendMessage(PlayerMessages.DeviceNotLinked.get(), Util.DUMMY_UUID);
            return;
        }

        final long key;
        try {
            key = Long.parseLong(unparsedKey);
        } catch (NumberFormatException ignored) {
            player.sendMessage(PlayerMessages.DeviceNotLinked.get(), Util.DUMMY_UUID);
            return;
        }

        ILocatable securityStation = Api.instance().registries().locatable().getLocatableBy(key);
        if (securityStation == null) {
            player.sendMessage(PlayerMessages.StationCanNotBeLocated.get(), Util.DUMMY_UUID);
            return;
        }

        if (!hasPower(player, 0.5, stack)) {
            player.sendMessage(PlayerMessages.DeviceNotPowered.get(), Util.DUMMY_UUID);
            return;
        }

        ContainerOpener.openContainer(containerType.get(), player, ContainerLocator.forHand(player, hand));
    }
}
