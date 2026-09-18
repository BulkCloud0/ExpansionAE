package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class TextFilterUpdate {
    public final int windowId;
    public final int key;
    public final String value;

    public TextFilterUpdate(int windowId, int key, String value) {
        this.windowId = windowId;
        this.key = key;
        this.value = value == null ? "" : value;
    }

    public static TextFilterUpdate decode(PacketBuffer buffer) {
        return new TextFilterUpdate(buffer.readVarInt(), buffer.readVarInt(), buffer.readString(128));
    }

    public static void encode(TextFilterUpdate message, PacketBuffer buffer) {
        buffer.writeVarInt(message.windowId);
        buffer.writeVarInt(message.key);
        buffer.writeString(message.value, 128);
    }

    public static void handle(TextFilterUpdate message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null || player.openContainer == null
                    || player.openContainer.windowId != message.windowId
                    || !(player.openContainer instanceof TextFilterReceiver)) {
                return;
            }
            ((TextFilterReceiver) player.openContainer).applyTextFilter(message.key, message.value);
        });
        context.setPacketHandled(true);
    }
}
