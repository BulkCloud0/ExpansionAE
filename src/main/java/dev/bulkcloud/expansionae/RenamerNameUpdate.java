package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RenamerNameUpdate {
    private final String name;

    public RenamerNameUpdate(String name) {
        this.name = name == null ? "" : name;
    }

    public static void encode(RenamerNameUpdate message, PacketBuffer buffer) {
        buffer.writeString(message.name, 64);
    }

    public static RenamerNameUpdate decode(PacketBuffer buffer) {
        return new RenamerNameUpdate(buffer.readString(64));
    }

    public static void handle(RenamerNameUpdate message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null && player.openContainer instanceof RenamerContainer) {
                ((RenamerContainer) player.openContainer).setNameServer(message.name);
            }
        });
        context.setPacketHandled(true);
    }
}
