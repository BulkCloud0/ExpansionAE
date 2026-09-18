package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import appeng.helpers.IInterfaceHost;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Persists ownership of every accepted input until its selected machine face accepts it. */
public final class RoutingBuffer {
    public static final String PATTERN_TAG = "expansionae_faces";
    private final IInterfaceHost host;
    private final List<Entry> pending = new ArrayList<>();
    private BlockPos target;

    public RoutingBuffer(IInterfaceHost host) { this.host = host; }
    public boolean hasPending() { return !pending.isEmpty(); }

    public static int face(ItemStack pattern, int index) {
        if (index < 0 || index >= 9 || !pattern.hasTag()) return -1;
        int[] faces = pattern.getTag().getIntArray(PATTERN_TAG);
        if (index >= faces.length || faces[index] < 0 || faces[index] > 5) return -1;
        return faces[index];
    }

    public static boolean hasRouting(ItemStack pattern) {
        for (int i = 0; i < 9; i++) if (face(pattern, i) != -1) return true;
        return false;
    }

    public boolean push(ItemStack pattern, CraftingInventory inputs, boolean blocking) {
        if (hasPending()) return false;
        TileEntity origin = host.getTileEntity();
        World world = origin.getWorld();
        if (world == null || world.isRemote) return false;
        for (Direction direction : host.getTargets()) {
            BlockPos candidate = origin.getPos().offset(direction);
            if (!world.isBlockLoaded(candidate)) continue;
            TileEntity machine = world.getTileEntity(candidate);
            if (machine == null || machine instanceof IInterfaceHost) continue;
            List<Entry> proposal = new ArrayList<>();
            boolean accepted = true;
            for (int i = 0; i < inputs.getSizeInventory(); i++) {
                ItemStack input = inputs.getStackInSlot(i);
                if (input.isEmpty()) continue;
                int selected = face(pattern, i);
                Direction side = selected == -1 ? direction.getOpposite() : Direction.byIndex(selected);
                IItemHandler handler = handler(machine, side);
                if (handler == null || (blocking && !isEmpty(handler))
                        || !ItemHandlerHelper.insertItemStacked(handler, input.copy(), true).isEmpty()) {
                    accepted = false;
                    break;
                }
                proposal.add(new Entry(input.copy(), side));
            }
            if (!accepted || proposal.isEmpty()) continue;
            target = candidate.toImmutable();
            pending.addAll(proposal);
            host.saveChanges();
            flush();
            // The caller consumes inputs exactly once after true. A partial insertion
            // remains owned here and must never be returned to the crafting CPU as false.
            return true;
        }
        return false;
    }

    public void flush() {
        if (!hasPending() || target == null) return;
        World world = host.getTileEntity().getWorld();
        if (world == null || world.isRemote || !world.isBlockLoaded(target)) return;
        TileEntity machine = world.getTileEntity(target);
        if (machine == null || machine instanceof IInterfaceHost) return;
        boolean changed = false;
        Iterator<Entry> it = pending.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            IItemHandler handler = handler(machine, entry.side);
            if (handler == null) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, entry.stack.copy(), false);
            if (rest.getCount() != entry.stack.getCount()) changed = true;
            entry.stack = rest;
            if (rest.isEmpty()) it.remove();
        }
        if (!hasPending()) target = null;
        if (changed) host.saveChanges();
    }

    private static IItemHandler handler(TileEntity machine, Direction side) {
        return machine.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    }
    private static boolean isEmpty(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    public void addDrops(List<ItemStack> drops) {
        for (Entry entry : pending) if (!entry.stack.isEmpty()) drops.add(entry.stack.copy());
    }
    public void write(CompoundNBT tag) {
        CompoundNBT data = new CompoundNBT();
        if (target != null) data.putLong("target", target.toLong());
        ListNBT list = new ListNBT();
        for (Entry entry : pending) {
            CompoundNBT value = new CompoundNBT();
            value.put("stack", entry.stack.write(new CompoundNBT()));
            value.putInt("face", entry.side.getIndex());
            list.add(value);
        }
        data.put("pending", list);
        tag.put("ExpansionRoutingBuffer", data);
    }
    public void read(CompoundNBT tag) {
        pending.clear();
        CompoundNBT data = tag.getCompound("ExpansionRoutingBuffer");
        target = data.contains("target", 4) ? BlockPos.fromLong(data.getLong("target")) : null;
        ListNBT list = data.getList("pending", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT value = list.getCompound(i);
            ItemStack stack = ItemStack.read(value.getCompound("stack"));
            if (!stack.isEmpty()) pending.add(new Entry(stack, Direction.byIndex(value.getInt("face"))));
        }
    }
    private static final class Entry {
        private ItemStack stack;
        private final Direction side;
        private Entry(ItemStack stack, Direction side) { this.stack = stack; this.side = side; }
    }
}
