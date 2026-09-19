package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import appeng.api.config.FuzzyMode;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class PortableWorkbenchAction {
    public static final int COPY_MODE = 0;
    public static final int PARTITION = 1;
    public static final int CLEAR = 2;
    public static final int FUZZY = 3;

    public final int windowId;
    public final int action;
    public final int value;

    public PortableWorkbenchAction(int windowId, int action, int value) {
        this.windowId = windowId;
        this.action = action;
        this.value = value;
    }

    public static void encode(PortableWorkbenchAction message, PacketBuffer buffer) {
        buffer.writeVarInt(message.windowId);
        buffer.writeVarInt(message.action);
        buffer.writeVarInt(message.value);
    }

    public static PortableWorkbenchAction decode(PacketBuffer buffer) {
        return new PortableWorkbenchAction(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(PortableWorkbenchAction message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null || !(player.openContainer instanceof PortableWorkbenchContainer)
                    || player.openContainer.windowId != message.windowId) return;

            PortableWorkbenchContainer container = (PortableWorkbenchContainer) player.openContainer;
            switch (message.action) {
                case COPY_MODE:
                    container.nextCopyMode();
                    break;
                case PARTITION:
                    container.partitionFromCell();
                    break;
                case CLEAR:
                    container.clearPartition();
                    break;
                case FUZZY:
                    FuzzyMode[] values = FuzzyMode.values();
                    if (message.value >= 0 && message.value < values.length) {
                        container.setCellFuzzy(values[message.value]);
                    }
                    break;
                default:
                    break;
            }
        });
        context.setPacketHandled(true);
    }
}
