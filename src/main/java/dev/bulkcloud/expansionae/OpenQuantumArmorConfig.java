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

/** Opens the AdvancedAE-style Quantum Armor configuration for worn armor. */
public final class OpenQuantumArmorConfig {
    private static final EquipmentSlotType[] ARMOR_SLOTS = {
            EquipmentSlotType.HEAD, EquipmentSlotType.CHEST,
            EquipmentSlotType.LEGS, EquipmentSlotType.FEET
    };

    public static void encode(OpenQuantumArmorConfig message, PacketBuffer buffer) {
    }

    public static OpenQuantumArmorConfig decode(PacketBuffer buffer) {
        return new OpenQuantumArmorConfig();
    }

    public static void handle(OpenQuantumArmorConfig message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;

            int selected = firstQuantumArmor(player);
            if (selected < 0) return;

            final int initial = selected;
            INamedContainerProvider provider = new SimpleNamedContainerProvider(
                    (windowId, inventory, ignored) ->
                            new QuantumArmorConfigContainer(windowId, inventory, initial),
                    new TranslationTextComponent("gui.expansionae.quantum_armor_config"));
            NetworkHooks.openGui(player, provider, buffer -> buffer.writeVarInt(initial));
        });
        context.setPacketHandled(true);
    }

    private static int firstQuantumArmor(ServerPlayerEntity player) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemStackFromSlot(ARMOR_SLOTS[i]);
            if (stack.getItem() instanceof QuantumArmorItem) return i;
        }
        return -1;
    }
}
