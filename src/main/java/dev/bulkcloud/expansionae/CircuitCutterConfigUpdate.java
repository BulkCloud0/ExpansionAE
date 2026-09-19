package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class CircuitCutterConfigUpdate {
    private final BlockPos pos;
    private final boolean autoExport;

    public CircuitCutterConfigUpdate(BlockPos pos, boolean autoExport) {
        this.pos = pos;
        this.autoExport = autoExport;
    }

    public static void encode(CircuitCutterConfigUpdate message, PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeBoolean(message.autoExport);
    }

    public static CircuitCutterConfigUpdate decode(PacketBuffer buffer) {
        return new CircuitCutterConfigUpdate(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(CircuitCutterConfigUpdate message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayerEntity player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            double distance = player.getDistanceSq(
                    message.pos.getX() + 0.5,
                    message.pos.getY() + 0.5,
                    message.pos.getZ() + 0.5);
            if (distance > 64.0) return;
            TileEntity tile = player.world.getTileEntity(message.pos);
            if (tile instanceof CircuitCutterTile) {
                ((CircuitCutterTile) tile).setAutoExport(message.autoExport);
            }
        });
        context.setPacketHandled(true);
    }
}
