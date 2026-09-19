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
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

/**
 * AdvancedAE Quantum Crafter backport for AE2 8.4.
 *
 * <p>AE2 8.4 only exposes item crafting patterns, so this implementation
 * executes vanilla/AE2 crafting patterns directly against ME item storage. It
 * preserves the upstream 9 pattern slots, 18 output-buffer slots and speed-card
 * factors. Modern per-pattern GenericStack/fluid configuration is intentionally
 * kept separate from this core backport.</p>
 */
public final class QuantumCrafterTile extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost {
    public static final int PATTERN_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 18;
    private static final double ENERGY_PER_CRAFT = 10.0;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1);
    private final AppEngInternalInventory outputs = new AppEngInternalInventory(this, OUTPUT_SLOTS, 64);
    private final IItemHandler internal = new WrapperChainedItemHandler(patterns, outputs);
    private final UpgradeInventory upgrades;
    private final IActionSource source = new MachineSource(this);

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
        saveChanges();
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    private boolean hasPatterns() {
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            if (!patterns.getStackInSlot(i).isEmpty()) return true;
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
        return new TickingRequest(1, 20, !hasPatterns(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!getProxy().isActive() || !hasPatterns() || world == null) {
            return TickRateModulation.IDLE;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = getProxy().getEnergy();
            int factor = speedFactor();
            boolean worked = false;

            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                ItemStack encoded = patterns.getStackInSlot(slot);
                if (encoded.isEmpty()) continue;

                ICraftingPatternDetails details = Api.instance().crafting().decodePattern(encoded, world);
                if (details == null || !details.isCraftable()) continue;

                for (int attempt = 0; attempt < factor; attempt++) {
                    if (!tryCraft(details, network, energy, channel)) break;
                    worked = true;
                }
            }

            return worked ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        }
    }

    private boolean tryCraft(ICraftingPatternDetails details, IMEMonitor<IAEItemStack> network,
            IEnergyGrid energy, IItemStorageChannel channel) {
        CraftPlan plan = planCraft(details, network);
        if (plan == null || !canBufferAll(plan.produced)) return false;

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
            IAEItemStack ae = channel.createStack(stack);
            IAEItemStack failed = Platform.poweredInsert(energy, network, ae, source);
            ItemStack remainder = failed == null ? ItemStack.EMPTY : failed.createItemStack();
            if (!remainder.isEmpty() && !buffer(remainder)) {
                network.injectItems(channel.createStack(remainder), Actionable.MODULATE, source);
            }
        }

        saveChanges();
        return true;
    }

    @Nullable
    private CraftPlan planCraft(ICraftingPatternDetails details, IMEMonitor<IAEItemStack> network) {
        IAEItemStack[] sparse = details.getSparseInputs();
        if (sparse == null || sparse.length != 9) return null;

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
            for (IAEItemStack option : options) {
                IAEItemStack inNetwork = network.getStorageList().findPrecise(option);
                if (inNetwork == null) continue;
                long reserved = reservedAmount(requests, option);
                if (inNetwork.getStackSize() - reserved >= amount) {
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
                    int moved = Math.min(remaining.getCount(), Math.min(64, remaining.getMaxStackSize()));
                    ItemStack placed = remaining.copy();
                    placed.setCount(moved);
                    shadow[slot] = placed;
                    remaining.shrink(moved);
                } else if (ItemStack.areItemsEqual(current, remaining)
                        && ItemStack.areItemStackTagsEqual(current, remaining)) {
                    int limit = Math.min(64, current.getMaxStackSize());
                    int moved = Math.min(remaining.getCount(), Math.max(0, limit - current.getCount()));
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

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        patterns.writeToNBT(data, "patterns");
        outputs.writeToNBT(data, "outputs");
        upgrades.writeToNBT(data, "upgrades");
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        patterns.readFromNBT(data, "patterns");
        outputs.readFromNBT(data, "outputs");
        upgrades.readFromNBT(data, "upgrades");
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
