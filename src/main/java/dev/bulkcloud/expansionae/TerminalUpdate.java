package dev.bulkcloud.expansionae;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

/** A server-owned inventory row, scoped to the currently open menu. */
public final class TerminalUpdate {
    public int windowId;
    public final boolean clear;
    public final long inventoryId;
    public final CompoundNBT data;

    private TerminalUpdate(boolean clear, long inventoryId, CompoundNBT data) {
        this.clear = clear;
        this.inventoryId = inventoryId;
        this.data = data;
    }
    public static TerminalUpdate clearExistingData() { return new TerminalUpdate(true, -1, new CompoundNBT()); }
    public static TerminalUpdate inventory(long id, CompoundNBT data) { return new TerminalUpdate(false, id, data); }
    public static TerminalUpdate decode(PacketBuffer buffer) {
        int window = buffer.readVarInt();
        TerminalUpdate message = new TerminalUpdate(buffer.readBoolean(), buffer.readLong(), buffer.readCompoundTag());
        message.windowId = window;
        return message;
    }
    public static void encode(TerminalUpdate message, PacketBuffer buffer) {
        buffer.writeVarInt(message.windowId);
        buffer.writeBoolean(message.clear);
        buffer.writeLong(message.inventoryId);
        buffer.writeCompoundTag(message.data);
    }
}
