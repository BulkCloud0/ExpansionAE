package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;

/** Opens the helmet-backed portable cell workbench from an equipped helmet. */
public final class OpenQuantumArmorMenu {
    public static void encode(OpenQuantumArmorMenu message, PacketBuffer buffer) {
    }

    public static OpenQuantumArmorMenu decode(PacketBuffer buffer) {
        return new OpenQuantumArmorMenu();
    }

    public static void handle(OpenQuantumArmorMenu message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;

            ItemStack helmet = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
            if (!(helmet.getItem() instanceof QuantumArmorItem)) return;

            QuantumArmorItem armor = (QuantumArmorItem) helmet.getItem();
            if (!armor.hasUpgrade(helmet, QuantumUpgradeType.WORKBENCH)) return;

            int inventorySlot = findInventorySlot(player, helmet);
            if (inventorySlot < 0) return;

            PortableWorkbenchGuiObject host = new PortableWorkbenchGuiObject(helmet, false);
            INamedContainerProvider provider = new SimpleNamedContainerProvider(
                    (windowId, inventory, ignored) ->
                            new PortableWorkbenchContainer(windowId, inventory, host),
                    new TranslationTextComponent("gui.appliedenergistics2.CellWorkbench"));

            final int serializedSlot = inventorySlot;
            NetworkHooks.openGui(player, provider, buffer -> {
                // ContainerLocator.Type.PLAYER_INVENTORY = 0.
                // ContainerTypeBuilder will deserialize this on the client.
                buffer.writeByte(0);
                buffer.writeInt(serializedSlot);
            });
        });
        context.setPacketHandled(true);
    }

    private static int findInventorySlot(ServerPlayerEntity player, ItemStack target) {
        int fallback = -1;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack candidate = player.inventory.getStackInSlot(i);
            if (candidate == target) return i;
            if (fallback < 0 && ItemStack.areItemStacksEqual(candidate, target)) {
                fallback = i;
            }
        }
        return fallback;
    }
}
