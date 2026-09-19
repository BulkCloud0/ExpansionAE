package dev.bulkcloud.expansionae;

import java.util.EnumSet;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.Api;
import appeng.items.misc.EncodedPatternItem;
import appeng.me.GridAccessException;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkInvTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

public final class AssemblerMatrixTile extends AENetworkInvTileEntity
        implements IAEMultiBlock<AssemblerMatrixCluster>, ICraftingProvider, IGridTickable, IPowerChannelState {
    public static final int PATTERN_SLOTS = 36;
    public static final int CRAFTER_LANES = 8;
    private static final int LANE_SLOTS = 10;

    private final AssemblerMatrixCalculator calculator = new AssemblerMatrixCalculator(this);
    private final AppEngInternalInventory patterns =
            new AppEngInternalInventory(this, PATTERN_SLOTS, 1, new PatternFilter());
    private final Lane[] lanes = new Lane[CRAFTER_LANES];
    private final IItemHandler crafterInventory;
    private AssemblerMatrixCluster cluster;

    public AssemblerMatrixTile(TileEntityType<?> type) {
        super(type);
        getProxy().setIdlePowerUsage(0.5);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        IItemHandler[] handlers = new IItemHandler[CRAFTER_LANES];
        for (int i = 0; i < CRAFTER_LANES; i++) {
            lanes[i] = new Lane();
            handlers[i] = lanes[i].inv;
        }
        crafterInventory = new WrapperChainedItemHandler(handlers);
    }

    public AssemblerMatrixBlock.Kind getKind() {
        if (world != null) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof AssemblerMatrixBlock) return ((AssemblerMatrixBlock) block).getKind();
        }
        return AssemblerMatrixBlock.Kind.FRAME;
    }

    public IItemHandler getPatternInventory() { return patterns; }
    public boolean isFormed() { return cluster != null; }

    public void updateStatus(AssemblerMatrixCluster next) {
        if (cluster != null && cluster != next) cluster.destroy();
        cluster = next;
        refreshMatrixState();
        notifyCraftingChanged();
    }

    public void refreshMatrixState() { markForUpdate(); }

    public void updateMultiBlock(BlockPos changedPos) {
        if (world != null && !world.isRemote) calculator.updateMultiblockAfterNeighborUpdate(world, pos, changedPos);
    }

    @Override
    public void onReady() {
        super.onReady();
        if (world != null && !world.isRemote) calculator.calculateMultiblock(world, pos);
        notifyCraftingChanged();
    }

    @Override
    public void disconnect(boolean update) {
        AssemblerMatrixCluster old = cluster;
        cluster = null;
        if (old != null && !old.isDestroyed()) old.destroy();
        if (update) refreshMatrixState();
    }

    @Override public AssemblerMatrixCluster getCluster() { return cluster; }
    @Override public boolean isValid() { return !isRemoved(); }

    @Override public AECableType getCableConnectionType(AEPartLocation side) { return AECableType.COVERED; }
    @Override public DimensionalCoord getLocation() { return new DimensionalCoord(this); }

    @Override
    protected ItemStack getItemFromTile(Object ignored) {
        return world == null ? ItemStack.EMPTY : new ItemStack(world.getBlockState(pos).getBlock());
    }

    @Override
    public IItemHandler getInternalInventory() {
        switch (getKind()) {
            case PATTERN: return patterns;
            case CRAFTER: return crafterInventory;
            default: return EmptyHandler.INSTANCE;
        }
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        if (inventory == patterns) notifyCraftingChanged();
        wake();
    }

    private void notifyCraftingChanged() {
        if (world == null || world.isRemote) return;
        try {
            getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
        } catch (GridAccessException ignored) { }
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper helper) {
        if (getKind() != AssemblerMatrixBlock.Kind.PATTERN || cluster == null || !getProxy().isActive()) return;
        for (int i = 0; i < patterns.getSlots(); i++) {
            ICraftingPatternDetails details = decode(patterns.getStackInSlot(i));
            if (details != null && details.isCraftable()) helper.addCraftingOption(this, details);
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, CraftingInventory table) {
        if (getKind() != AssemblerMatrixBlock.Kind.PATTERN || cluster == null || !getProxy().isActive()) return false;
        boolean owns = false;
        for (int i = 0; i < patterns.getSlots(); i++) {
            ICraftingPatternDetails local = decode(patterns.getStackInSlot(i));
            if (local != null && ItemStack.areItemStacksEqual(local.getPattern(), details.getPattern())) {
                owns = true;
                break;
            }
        }
        return owns && cluster.pushPattern(details, table);
    }

    @Override public boolean isBusy() { return cluster == null || cluster.isBusy(); }

    public boolean hasFreeMatrixLane() {
        if (getKind() != AssemblerMatrixBlock.Kind.CRAFTER) return false;
        for (Lane lane : lanes) if (lane.isFree()) return true;
        return false;
    }

    public boolean acceptMatrixJob(ICraftingPatternDetails details, CraftingInventory table) {
        if (getKind() != AssemblerMatrixBlock.Kind.CRAFTER || cluster == null || !getProxy().isActive()) return false;
        for (Lane lane : lanes) {
            if (lane.accept(details, table)) {
                wake();
                saveChanges();
                return true;
            }
        }
        return false;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 4,
                getKind() != AssemblerMatrixBlock.Kind.CRAFTER || !hasLaneWork(), false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getKind() != AssemblerMatrixBlock.Kind.CRAFTER || cluster == null) return TickRateModulation.SLEEP;
        boolean work = false;
        for (Lane lane : lanes) work |= lane.tick(Math.max(1, ticksSinceLastCall));
        return work ? TickRateModulation.FASTER : TickRateModulation.SLEEP;
    }

    private boolean hasLaneWork() {
        for (Lane lane : lanes) if (!lane.isFree()) return true;
        return false;
    }

    private void wake() {
        try { getProxy().getTick().wakeDevice(getProxy().getNode()); } catch (GridAccessException ignored) { }
    }

    private int speedPerTick() {
        int cores = cluster == null ? 0 : cluster.getSpeedCores();
        switch (cores) {
            case 1: return 13;
            case 2: return 17;
            case 3: return 20;
            case 4: return 25;
            case 5: return 50;
            default: return 10;
        }
    }

    private int poweredProgress(int ticks) {
        int requested = speedPerTick() * ticks;
        try {
            return (int) Math.floor(getProxy().getEnergy().extractAEPower(
                    requested, Actionable.MODULATE, PowerMultiplier.CONFIG));
        } catch (GridAccessException ignored) {
            return 0;
        }
    }

    private void insertNetwork(ItemStack stack) {
        if (stack.isEmpty()) return;
        try {
            IAEItemStack ae = Api.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(stack);
            IAEItemStack rest = getProxy().getStorage()
                    .getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class))
                    .injectItems(ae, Actionable.MODULATE, new MachineSource(this));
            stack.setCount(rest == null ? 0 : (int) Math.min(Integer.MAX_VALUE, rest.getStackSize()));
        } catch (GridAccessException ignored) { }
    }

    private ICraftingPatternDetails decode(ItemStack stack) {
        if (stack.isEmpty() || world == null || !(stack.getItem() instanceof EncodedPatternItem)) return null;
        return Api.instance().crafting().decodePattern(stack, world);
    }

    @Override public boolean isPowered() { return getProxy().isPowered(); }
    @Override public boolean isActive() { return cluster != null && getProxy().isActive(); }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        for (int i = 0; i < CRAFTER_LANES; i++) data.put("matrixLane" + i, lanes[i].write());
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        for (int i = 0; i < CRAFTER_LANES; i++) {
            if (data.contains("matrixLane" + i)) lanes[i].read(data.getCompound("matrixLane" + i));
        }
    }

    private final class Lane {
        private final AppEngInternalInventory inv = new AppEngInternalInventory(AssemblerMatrixTile.this, LANE_SLOTS);
        private final CraftingInventory crafting = new CraftingInventory(new Container(null, -1) {
            @Override public boolean canInteractWith(net.minecraft.entity.player.PlayerEntity player) { return false; }
        }, 3, 3);
        private ICraftingPatternDetails plan;
        private double progress;

        boolean isFree() { return plan == null && ItemHandlerUtil.isEmpty(inv); }

        boolean accept(ICraftingPatternDetails details, CraftingInventory table) {
            if (!isFree() || details == null || !details.isCraftable()) return false;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = table.getStackInSlot(i);
                inv.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            plan = details;
            progress = 0;
            return true;
        }

        boolean tick(int ticks) {
            flush();
            if (plan == null) return !isFree();
            for (int i = 0; i < 9; i++) crafting.setInventorySlotContents(i, inv.getStackInSlot(i));
            ItemStack result = plan.getOutput(crafting, world);
            if (result.isEmpty()) return true;
            progress += poweredProgress(ticks);
            if (progress < 100) return true;
            progress = 0;
            if (!inv.getStackInSlot(9).isEmpty()) return true;
            inv.setStackInSlot(9, result.copy());
            for (int i = 0; i < 9; i++) {
                inv.setStackInSlot(i, Platform.getContainerItem(crafting.getStackInSlot(i)));
            }
            flush();
            saveChanges();
            return true;
        }

        private void flush() {
            if (plan == null && ItemHandlerUtil.isEmpty(inv)) return;
            for (int i = 0; i < LANE_SLOTS; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                ItemStack copy = stack.copy();
                insertNetwork(copy);
                inv.setStackInSlot(i, copy);
            }
            if (ItemHandlerUtil.isEmpty(inv)) {
                plan = null;
                progress = 0;
            }
        }

        CompoundNBT write() {
            CompoundNBT tag = new CompoundNBT();
            inv.writeToNBT(tag, "inv");
            tag.putDouble("progress", progress);
            if (plan != null) {
                ItemStack p = plan.getPattern();
                if (!p.isEmpty()) tag.put("pattern", p.write(new CompoundNBT()));
            }
            return tag;
        }

        void read(CompoundNBT tag) {
            inv.readFromNBT(tag, "inv");
            progress = tag.getDouble("progress");
            plan = null;
            if (tag.contains("pattern") && world != null) {
                plan = decode(ItemStack.read(tag.getCompound("pattern")));
                if (plan != null && !plan.isCraftable()) plan = null;
            }
        }
    }

    private final class PatternFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(IItemHandler handler, int slot, ItemStack stack) {
            ICraftingPatternDetails details = decode(stack);
            return details != null && details.isCraftable();
        }
        @Override public boolean allowExtract(IItemHandler handler, int slot, int amount) { return true; }
    }
}
