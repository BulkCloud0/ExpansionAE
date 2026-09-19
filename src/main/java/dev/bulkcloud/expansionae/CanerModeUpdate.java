package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class CanerModeUpdate {
    private final BlockPos pos;
    private final CanerMode mode;

    public CanerModeUpdate(BlockPos pos, CanerMode mode) {
        this.pos = pos;
        this.mode = mode;
    }

    public static void encode(CanerModeUpdate message, PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeVarInt(message.mode.ordinal());
    }

    public static CanerModeUpdate decode(PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        int ordinal = buffer.readVarInt();
        CanerMode[] values = CanerMode.values();
        CanerMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : CanerMode.FILL;
        return new CanerModeUpdate(pos, mode);
    }

    public static void handle(CanerModeUpdate message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            if (player.getDistanceSq(message.pos.getX() + 0.5, message.pos.getY() + 0.5,
                    message.pos.getZ() + 0.5) > 64.0) return;
            TileEntity tile = player.world.getTileEntity(message.pos);
            if (tile instanceof CanerTile) {
                ((CanerTile) tile).setMode(message.mode);
            }
        });
        context.setPacketHandled(true);
    }
}
