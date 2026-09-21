package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.container.ContainerNull;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * AdvancedAE Quantum Crafter backport for AE2 8.4.
 *
 * AE2 8.4 only exposes item crafting patterns, so generic fluid-key inputs from
 * newer AdvancedAE cannot be represented here. Item-pattern semantics are kept:
 * nine independently enabled jobs, per-input reserve amounts, per-job output
 * caps, 18 oversized output slots, ME/adjacent export and speed-card factors.
 */
public final class QuantumCrafterTile extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost {
    public static final int PATTERN_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 18;
    public static final int INPUT_CONFIG_SLOTS = 9;
    public static final int OUTPUT_SLOT_LIMIT = 1024;
    private static final double ENERGY_PER_CRAFT = 10.0;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1);
    private final OversizeItemInventory outputs =
            new OversizeItemInventory(this, OUTPUT_SLOTS, OUTPUT_SLOT_LIMIT);
    private final IItemHandler internal = new WrapperChainedItemHandler(patterns, outputs);
    private final UpgradeInventory upgrades;
    private final IActionSource source = new MachineSource(this);

    private final boolean[] enabledPatterns = new boolean[PATTERN_SLOTS];
    private final long[][] minimumInputStock = new long[PATTERN_SLOTS][INPUT_CONFIG_SLOTS];
    private final long[] maximumOutputStock = new long[PATTERN_SLOTS];
    private boolean exportToME = true;
    private int outputSideMask;

    public QuantumCrafterTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.allOf(Direction.class));
        getProxy().setIdlePowerUsage(0.0);
        setInternalMaxPower(8000);

        upgrades = new UpgradeInventory(this, 5) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 4 : 0;
            }
        };
    }

    public IItemHandler getPatternInventory() { return patterns; }
    public IItemHandler getOutputInventory() { return outputs; }

    public boolean isPatternEnabled(int slot) {
        return validPatternSlot(slot) && enabledPatterns[slot];
    }

    public void togglePatternEnabled(int slot) {
        if (!validPatternSlot(slot)) return;
        enabledPatterns[slot] = !enabledPatterns[slot];
        configChanged();
    }

    public long getMinimumInputStock(int patternSlot, int inputSlot) {
        if (!validPatternSlot(patternSlot) || inputSlot < 0 || inputSlot >= INPUT_CONFIG_SLOTS) return 0;
        return minimumInputStock[patternSlot][inputSlot];
    }

    public void adjustMinimumInputStock(int patternSlot, int inputSlot, long delta) {
        if (!validPatternSlot(patternSlot) || inputSlot < 0 || inputSlot >= INPUT_CONFIG_SLOTS) return;
        minimumInputStock[patternSlot][inputSlot] =
                addClamped(minimumInputStock[patternSlot][inputSlot], delta);
        configChanged();
    }

    public long getMaximumOutputStock(int patternSlot) {
        return validPatternSlot(patternSlot) ? maximumOutputStock[patternSlot] : 0;
    }

    public void adjustMaximumOutputStock(int patternSlot, long delta) {
        if (!validPatternSlot(patternSlot)) return;
        maximumOutputStock[patternSlot] = addClamped(maximumOutputStock[patternSlot], delta);
        configChanged();
    }

    public boolean isExportToME() { return exportToME; }

    public void toggleExportToME() {
        exportToME = !exportToME;
        configChanged();
    }

    public int getOutputSideMask() { return outputSideMask; }

    public void toggleOutputSide(int ordinal) {
        if (ordinal < 0 || ordinal >= Direction.values().length) return;
        outputSideMask ^= 1 << ordinal;
        configChanged();
    }

    private static long addClamped(long value, long delta) {
        if (delta > 0 && value > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        if (delta < 0 && value < -delta) return 0;
        return Math.max(0L, value + delta);
    }

    private boolean validPatternSlot(int slot) {
        return slot >= 0 && slot < PATTERN_SLOTS;
    }

    private void configChanged() {
        saveChanges();
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public IItemHandler getInternalInventory() { return internal; }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("patterns".equals(name)) return patterns;
        if ("output".equals(name)) return outputs;
        if ("upgrades".equals(name)) return upgrades;
        if ("inv".equals(name)) return internal;
        return null;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) { return upgrades.getInstalledUpgrades(upgrade); }

    @Override
    public IConfigManager getConfigManager() { return null; }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) { return AECableType.SMART; }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        if (inv == patterns && validPatternSlot(slot)
                && !sameStackIdentity(removed, added)) {
            enabledPatterns[slot] = false;
            maximumOutputStock[slot] = 0;
            for (int i = 0; i < INPUT_CONFIG_SLOTS; i++) {
                minimumInputStock[slot][i] = 0;
            }
        }
        configChanged();
    }

    private static boolean sameStackIdentity(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        return !a.isEmpty() && !b.isEmpty()
                && ItemStack.areItemsEqual(a, b)
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private boolean hasWork() {
        if (!exportToME && hasBufferedOutput()) return true;
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            if (enabledPatterns[i] && !patterns.getStackInSlot(i).isEmpty()) return true;
        }
        return exportToME && hasBufferedOutput();
    }

    private boolean hasBufferedOutput() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (!outputs.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    private int speedFactor() {
        switch (upgrades.getInstalledUpgrades(Upgrades.SPEED)) {
            case 1: return 8;
            case 2: return 16;
            case 3: return 32;
            case 4: return 64;
            default: return 1;
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, !hasWork(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!getProxy().isActive() || world == null) {
            return TickRateModulation.IDLE;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = getProxy().getEnergy();
            boolean worked = exportToME
                    ? flushBufferToME(network, energy, channel)
                    : pushBufferToAdjacent();
            int factor = speedFactor();

            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                if (!enabledPatterns[slot]) continue;
                ItemStack encoded = patterns.getStackInSlot(slot);
                if (encoded.isEmpty()) continue;

                ICraftingPatternDetails details = Api.instance().crafting().decodePattern(encoded, world);
                if (details == null || !details.isCraftable()) continue;

                for (int attempt = 0; attempt < factor; attempt++) {
                    if (!tryCraft(slot, details, network, energy, channel)) break;
                    worked = true;
                }
            }

            return worked ? TickRateModulation.URGENT
                    : hasWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        }
    }

    private boolean tryCraft(int patternSlot, ICraftingPatternDetails details,
            IMEMonitor<IAEItemStack> network, IEnergyGrid energy, IItemStorageChannel channel) {
        CraftPlan plan = planCraft(patternSlot, details, network);
        if (plan == null || !withinOutputLimit(patternSlot, plan, network, channel)
                || !canBufferAll(plan.produced)) {
            return false;
        }

        double availablePower = energy.extractAEPower(
                ENERGY_PER_CRAFT, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (availablePower + 0.001 < ENERGY_PER_CRAFT) return false;

        energy.extractAEPower(ENERGY_PER_CRAFT, Actionable.MODULATE, PowerMultiplier.CONFIG);

        List<IAEItemStack> extracted = new ArrayList<IAEItemStack>();
        for (IAEItemStack request : plan.requests) {
            IAEItemStack got = network.extractItems(request, Actionable.MODULATE, source);
            if (got == null || got.getStackSize() < request.getStackSize()) {
                if (got != null && got.getStackSize() > 0) extracted.add(got);
                rollback(network, extracted);
                return false;
            }
            extracted.add(got);
        }

        for (ItemStack stack : plan.produced) {
            if (stack.isEmpty()) continue;
            if (exportToME) {
                IAEItemStack ae = channel.createStack(stack);
                IAEItemStack failed = Platform.poweredInsert(energy, network, ae, source);
                ItemStack remainder = failed == null ? ItemStack.EMPTY : failed.createItemStack();
                if (!remainder.isEmpty() && !buffer(remainder)) {
                    network.injectItems(channel.createStack(remainder), Actionable.MODULATE, source);
                }
            } else {
                buffer(stack);
            }
        }

        saveChanges();
        return true;
    }

    @Nullable
    private CraftPlan planCraft(int patternSlot, ICraftingPatternDetails details,
            IMEMonitor<IAEItemStack> network) {
        IAEItemStack[] sparse = details.getSparseInputs();
        if (sparse == null || sparse.length != INPUT_CONFIG_SLOTS) return null;

        CraftingInventory table = new CraftingInventory(new ContainerNull(), 3, 3);
        List<IAEItemStack> requests = new ArrayList<IAEItemStack>();

        for (int slot = 0; slot < sparse.length; slot++) {
            IAEItemStack encodedInput = sparse[slot];
            if (encodedInput == null) continue;

            List<IAEItemStack> options = new ArrayList<IAEItemStack>();
            options.add(encodedInput);
            if (details.canSubstitute()) {
                for (IAEItemStack substitute : details.getSubstituteInputs(slot)) {
                    if (substitute != null) options.add(substitute);
                }
            }

            IAEItemStack chosen = null;
            long amount = Math.max(1L, encodedInput.getStackSize());
            long reserve = minimumInputStock[patternSlot][slot];
            for (IAEItemStack option : options) {
                IAEItemStack inNetwork = network.getStorageList().findPrecise(option);
                if (inNetwork == null) continue;
                long alreadyReserved = reservedAmount(requests, option);
                if (inNetwork.getStackSize() - alreadyReserved - reserve >= amount) {
                    chosen = option.copy();
                    chosen.setStackSize(amount);
                    break;
                }
            }

            if (chosen == null) return null;
            requests.add(chosen);
            table.setInventorySlotContents(slot, chosen.createItemStack());
        }

        ItemStack output = details.getOutput(table, world);
        if (output.isEmpty()) return null;

        List<ItemStack> produced = new ArrayList<ItemStack>();
        produced.add(output.copy());

        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack input = table.getStackInSlot(slot);
            if (input.isEmpty()) continue;
            ItemStack remainder = Platform.getContainerItem(input);
            if (!remainder.isEmpty()) produced.add(remainder);
        }

        return new CraftPlan(requests, produced);
    }

    private boolean withinOutputLimit(int patternSlot, CraftPlan plan,
            IMEMonitor<IAEItemStack> network, IItemStorageChannel channel) {
        long limit = maximumOutputStock[patternSlot];
        if (limit <= 0 || plan.produced.isEmpty()) return true;

        ItemStack primary = plan.produced.get(0);
        if (primary.isEmpty()) return true;
        IAEItemStack key = channel.createStack(primary);
        long stored = 0;
        IAEItemStack networkStack = network.getStorageList().findPrecise(key);
        if (networkStack != null) stored += networkStack.getStackSize();
        stored += bufferedAmount(primary);
        return stored <= limit - primary.getCount();
    }

    private long bufferedAmount(ItemStack type) {
        long total = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stack = outputs.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, type)
                    && ItemStack.areItemStackTagsEqual(stack, type)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private long reservedAmount(List<IAEItemStack> requests, IAEItemStack type) {
        long reserved = 0;
        for (IAEItemStack request : requests) {
            if (request.isSameType(type)) reserved += request.getStackSize();
        }
        return reserved;
    }

    private void rollback(IMEMonitor<IAEItemStack> network, List<IAEItemStack> extracted) {
        for (IAEItemStack stack : extracted) {
            IAEItemStack remainder = network.injectItems(stack, Actionable.MODULATE, source);
            if (remainder != null && remainder.getStackSize() > 0) {
                buffer(remainder.createItemStack());
            }
        }
    }

    private boolean canBufferAll(List<ItemStack> stacks) {
        ItemStack[] shadow = new ItemStack[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) shadow[i] = outputs.getStackInSlot(i).copy();

        for (ItemStack produced : stacks) {
            ItemStack remaining = produced.copy();
            for (int slot = 0; slot < shadow.length && !remaining.isEmpty(); slot++) {
                ItemStack current = shadow[slot];
                if (current.isEmpty()) {
                    int moved = Math.min(remaining.getCount(), OUTPUT_SLOT_LIMIT);
                    ItemStack placed = remaining.copy();
                    placed.setCount(moved);
                    shadow[slot] = placed;
                    remaining.shrink(moved);
                } else if (ItemStack.areItemsEqual(current, remaining)
                        && ItemStack.areItemStackTagsEqual(current, remaining)) {
                    int moved = Math.min(remaining.getCount(),
                            Math.max(0, OUTPUT_SLOT_LIMIT - current.getCount()));
                    if (moved > 0) {
                        current.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private boolean buffer(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < OUTPUT_SLOTS && !remaining.isEmpty(); slot++) {
            remaining = outputs.insertItem(slot, remaining, false);
        }
        return remaining.isEmpty();
    }

    private boolean flushBufferToME(IMEMonitor<IAEItemStack> network, IEnergyGrid energy,
            IItemStorageChannel channel) {
        boolean moved = false;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stored = outputs.getStackInSlot(slot);
            if (stored.isEmpty()) continue;

            ItemStack extracted = outputs.extractItem(slot, stored.getCount(), false);
            IAEItemStack failed = Platform.poweredInsert(
                    energy, network, channel.createStack(extracted), source);
            if (failed == null || failed.getStackSize() < extracted.getCount()) moved = true;
            if (failed != null && failed.getStackSize() > 0) {
                buffer(failed.createItemStack());
            }
        }
        return moved;
    }

    private boolean pushBufferToAdjacent() {
        if (outputSideMask == 0 || world == null) return false;
        boolean moved = false;

        for (Direction direction : Direction.values()) {
            if ((outputSideMask & (1 << direction.ordinal())) == 0) continue;
            TileEntity adjacent = world.getTileEntity(pos.offset(direction));
            if (adjacent == null) continue;
            IItemHandler target = adjacent
                    .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())
                    .orElse(null);
            if (target == null) continue;

            for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
                ItemStack stored = outputs.getStackInSlot(slot);
                if (stored.isEmpty()) continue;
                ItemStack simulatedRemainder = ItemHandlerHelper.insertItem(target, stored.copy(), true);
                int transferable = stored.getCount() - simulatedRemainder.getCount();
                if (transferable <= 0) continue;

                ItemStack extracted = outputs.extractItem(slot, transferable, false);
                ItemStack failed = ItemHandlerHelper.insertItem(target, extracted, false);
                if (!failed.isEmpty()) buffer(failed);
                if (failed.getCount() < extracted.getCount()) moved = true;
            }
        }
        return moved;
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        patterns.writeToNBT(data, "patterns");
        outputs.writeToNBT(data, "outputs");
        upgrades.writeToNBT(data, "upgrades");
        data.putBoolean("quantumExportToME", exportToME);
        data.putInt("quantumOutputSideMask", outputSideMask);

        for (int pattern = 0; pattern < PATTERN_SLOTS; pattern++) {
            CompoundNBT config = new CompoundNBT();
            config.putBoolean("enabled", enabledPatterns[pattern]);
            config.putLong("maxOutput", maximumOutputStock[pattern]);
            config.putLongArray("minInputs", minimumInputStock[pattern]);
            data.put("quantumPatternConfig" + pattern, config);
        }
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        patterns.readFromNBT(data, "patterns");
        outputs.readFromNBT(data, "outputs");
        upgrades.readFromNBT(data, "upgrades");
        exportToME = !data.contains("quantumExportToME") || data.getBoolean("quantumExportToME");
        outputSideMask = data.getInt("quantumOutputSideMask") & 0x3F;

        for (int pattern = 0; pattern < PATTERN_SLOTS; pattern++) {
            CompoundNBT config = data.getCompound("quantumPatternConfig" + pattern);
            if (config.isEmpty()) continue;
            enabledPatterns[pattern] = config.getBoolean("enabled");
            maximumOutputStock[pattern] = Math.max(0L, config.getLong("maxOutput"));
            long[] mins = config.getLongArray("minInputs");
            for (int input = 0; input < Math.min(INPUT_CONFIG_SLOTS, mins.length); input++) {
                minimumInputStock[pattern][input] = Math.max(0L, mins[input]);
            }
        }
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        addInventoryDrops(patterns, drops);
        addInventoryDrops(outputs, drops);
        addInventoryDrops(upgrades, drops);
    }

    private static void addInventoryDrops(IItemHandler inventory, List<ItemStack> drops) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
    }

    private static final class CraftPlan {
        private final List<IAEItemStack> requests;
        private final List<ItemStack> produced;

        private CraftPlan(List<IAEItemStack> requests, List<ItemStack> produced) {
            this.requests = requests;
            this.produced = produced;
        }
    }
}
