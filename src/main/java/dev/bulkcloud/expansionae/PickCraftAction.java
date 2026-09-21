package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.me.helpers.PlayerSource;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * Server-side Pick-Craft action for the Quantum Chestplate.
 *
 * <p>AdvancedAE's modern implementation opens AE2's craft-amount submenu.
 * AE2 8.4 predates that item-menu-host path, so the 1.16.5 adaptation requests
 * one of the targeted block and submits the calculated job directly to the
 * linked ME network.</p>
 */
public final class PickCraftAction {
    public static void encode(PickCraftAction message, PacketBuffer buffer) {
    }

    public static PickCraftAction decode(PacketBuffer buffer) {
        return new PickCraftAction();
    }

    public static void handle(PickCraftAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) attempt(player);
        });
        context.setPacketHandled(true);
    }

    private static void attempt(ServerPlayerEntity player) {
        ItemStack chest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (!(chest.getItem() instanceof QuantumArmorItem)) return;

        QuantumArmorItem armor = (QuantumArmorItem) chest.getItem();
        if (!armor.isUpgradeUsable(chest, QuantumUpgradeType.PICK_CRAFT)) {
            player.sendStatusMessage(new StringTextComponent("Pick-Craft upgrade is not installed, enabled, or powered."), true);
            return;
        }

        IGrid grid = armor.getLinkedGrid(chest);
        if (grid == null) {
            player.sendStatusMessage(new StringTextComponent("Quantum Chestplate is not linked to an active ME network."), true);
            return;
        }

        RayTraceResult hit = player.pick(5.0D, 1.0F, false);
        if (!(hit instanceof BlockRayTraceResult) || hit.getType() != RayTraceResult.Type.BLOCK) {
            player.sendStatusMessage(new StringTextComponent("No block targeted for Pick-Craft."), true);
            return;
        }

        BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
        BlockState state = player.world.getBlockState(blockHit.getPos());
        ItemStack targetStack = new ItemStack(state.getBlock().asItem());
        if (targetStack.isEmpty()) {
            player.sendStatusMessage(new StringTextComponent("The targeted block has no craftable item form."), true);
            return;
        }

        ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
        IAEItemStack target = channel.createStack(targetStack);
        if (crafting == null || target == null) return;
        target.setStackSize(1);

        if (crafting.getCraftingFor(target, null, 0, player.world).isEmpty()) {
            player.sendStatusMessage(new StringTextComponent("The targeted block is not craftable on this ME network."), true);
            return;
        }

        IActionSource source = new PlayerSource(player, null);
        try {
            crafting.beginCraftingJob(player.world, grid, source, target, job -> {
                if (player.getServer() == null) return;
                player.getServer().execute(() -> {
                    ItemStack currentChest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
                    if (!(currentChest.getItem() instanceof QuantumArmorItem)) return;
                    QuantumArmorItem currentArmor = (QuantumArmorItem) currentChest.getItem();
                    if (!currentArmor.isUpgradeUsable(currentChest, QuantumUpgradeType.PICK_CRAFT)
                            || currentArmor.getLinkedGrid(currentChest) != grid) return;

                    if (crafting.submitJob(job, null, null, true, source) != null) {
                        currentArmor.consumeUpgradeEnergy(currentChest, QuantumUpgradeType.PICK_CRAFT);
                        player.sendStatusMessage(new StringTextComponent(
                                "Pick-Craft requested 1x " + targetStack.getDisplayName().getString()), true);
                    } else {
                        player.sendStatusMessage(new StringTextComponent("No crafting CPU accepted the Pick-Craft job."), true);
                    }
                });
            });
        } catch (RuntimeException e) {
            player.sendStatusMessage(new StringTextComponent("Unable to calculate the Pick-Craft request."), true);
        }
    }
}
