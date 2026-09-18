package dev.bulkcloud.expansionae;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.storage.IOPortTileEntity;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.InvOperation;

/**
 * Extended IO Port with the 1.20 ExtendedAE transfer rates backported to AE2 8.4.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExpandedIOPortTile extends IOPortTileEntity {
    private final UpgradeInventory expandedUpgrades = new UpgradeInventory(this, 5) {
        @Override
        public int getMaxInstalled(Upgrades upgrade) {
            if (upgrade == Upgrades.SPEED) return 5;
            if (upgrade == Upgrades.REDSTONE) return 1;
            return 0;
        }
    };
    private final IActionSource source = new MachineSource(this);
    private ItemStack currentCell = ItemStack.EMPTY;
    private Map<IStorageChannel<?>, IMEInventory<?>> cachedInventories = new IdentityHashMap<>();

    public ExpandedIOPortTile(TileEntityType<?> type) {
        super(type);
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        expandedUpgrades.writeToNBT(data, "expandedUpgrades");
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        expandedUpgrades.readFromNBT(data, "expandedUpgrades");
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("upgrades".equals(name)) {
            return expandedUpgrades;
        }
        return super.getInventoryByName(name);
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return expandedUpgrades.getInstalledUpgrades(upgrade);
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        super.onChangeInventory(inventory, slot, operation, removed, added);
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!getProxy().isActive()) {
            return TickRateModulation.IDLE;
        }

        long itemsToMove = transferBudget();
        TickRateModulation modulation = TickRateModulation.SLEEP;
        IItemHandler cells = super.getInventoryByName("cells");

        try {
            IEnergySource energy = getProxy().getEnergy();

            for (int slot = 0; slot < 6 && itemsToMove > 0; slot++) {
                ItemStack cell = cells.getStackInSlot(slot);
                if (cell.isEmpty()) {
                    continue;
                }

                boolean shouldMove = true;
                for (IStorageChannel<? extends IAEStack<?>> channel
                        : Api.instance().storage().storageChannels()) {
                    if (itemsToMove <= 0) break;

                    IMEMonitor<? extends IAEStack<?>> network =
                            getProxy().getStorage().getInventory(channel);
                    IMEInventory<?> cellInv = getCellInventory(cell, channel);
                    if (cellInv == null) {
                        continue;
                    }

                    if (getConfigManager().getSetting(Settings.OPERATION_MODE) == OperationMode.EMPTY) {
                        itemsToMove = transferContents(energy, cellInv, network, itemsToMove, channel);
                    } else {
                        itemsToMove = transferContents(energy, network, cellInv, itemsToMove, channel);
                    }

                    shouldMove &= matchesFullness(cellInv);
                    modulation = itemsToMove > 0
                            ? TickRateModulation.IDLE
                            : TickRateModulation.URGENT;
                }

                if (itemsToMove > 0 && shouldMove && moveCellToOutput(cells, slot)) {
                    modulation = TickRateModulation.URGENT;
                }
            }
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        }

        return modulation;
    }

    private long transferBudget() {
        switch (getInstalledUpgrades(Upgrades.SPEED)) {
            case 1: return 2048L * 2L;
            case 2: return 2048L * 8L;
            case 3: return 2048L * 32L;
            case 4: return 2048L * 128L;
            case 5: return 2048L * 512L;
            default: return 2048L;
        }
    }

    private IMEInventory<?> getCellInventory(ItemStack cell, IStorageChannel<?> channel) {
        if (currentCell != cell) {
            currentCell = cell;
            cachedInventories = new IdentityHashMap<>();
            for (IStorageChannel<? extends IAEStack<?>> c : Api.instance().storage().storageChannels()) {
                cachedInventories.put(c, Api.instance().registries().cell().getCellInventory(cell, null, c));
            }
        }
        return cachedInventories.get(channel);
    }

    private long transferContents(IEnergySource energy, IMEInventory sourceInventory,
            IMEInventory destination, long operations, IStorageChannel channel) {
        IItemList<? extends IAEStack> list;
        if (sourceInventory instanceof IMEMonitor) {
            list = ((IMEMonitor) sourceInventory).getStorageList();
        } else {
            list = sourceInventory.getAvailableItems(sourceInventory.getChannel().createList());
        }

        long budget = operations * channel.transferFactor();
        boolean moved;
        do {
            moved = false;
            for (IAEStack available : list) {
                long total = available.getStackSize();
                if (total <= 0) continue;

                IAEStack rejected = destination.injectItems(available, Actionable.SIMULATE, source);
                long possible = rejected == null ? total : total - rejected.getStackSize();
                if (possible <= 0) continue;

                possible = Math.min(possible, budget);
                IAEStack request = available.copy();
                request.setStackSize(possible);

                IAEStack extracted = (IAEStack) sourceInventory.extractItems(
                        request, Actionable.MODULATE, this.source);
                if (extracted == null) continue;

                long accepted = extracted.getStackSize();
                IAEStack failed = (IAEStack) Platform.poweredInsert(
                        energy, destination, extracted, this.source);
                if (failed != null) {
                    accepted -= failed.getStackSize();
                    sourceInventory.injectItems(failed, Actionable.MODULATE, this.source);
                }

                if (accepted > 0) {
                    budget -= accepted;
                    moved = true;
                }
                break;
            }
        } while (budget > 0 && moved);

        return budget / channel.transferFactor();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean matchesFullness(IMEInventory inventory) {
        FullnessMode mode = (FullnessMode) getConfigManager().getSetting(Settings.FULLNESS_MODE);
        if (mode == FullnessMode.HALF || inventory == null) {
            return true;
        }

        // AE2 8.4 ties IMEInventory<T>, its channel and IItemList<T> through a
        // self-referential generic. This method intentionally works across both
        // item and fluid channels, so use the raw boundary here and keep all
        // channel-specific values together instead of breaking the capture type.
        IItemList list;
        if (inventory instanceof IMEMonitor) {
            list = ((IMEMonitor) inventory).getStorageList();
        } else {
            list = inventory.getAvailableItems(inventory.getChannel().createList());
        }

        if (mode == FullnessMode.EMPTY) {
            return list.isEmpty();
        }

        IAEStack first = (IAEStack) list.getFirstItem();
        if (first == null) {
            return false;
        }
        IAEStack probe = first.copy();
        probe.setStackSize(1);
        return inventory.injectItems(probe, Actionable.SIMULATE, source) != null;
    }

    private boolean moveCellToOutput(IItemHandler cells, int inputSlot) {
        ItemStack moving = cells.getStackInSlot(inputSlot);
        if (moving.isEmpty()) return false;

        ItemStack remainder = moving.copy();
        for (int output = 6; output < 12 && !remainder.isEmpty(); output++) {
            remainder = cells.insertItem(output, remainder, false);
        }
        if (remainder.getCount() == moving.getCount()) {
            return false;
        }
        ItemHandlerUtil.setStackInSlot(cells, inputSlot, remainder);
        return true;
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (ItemStack stack : expandedUpgrades) {
            if (!stack.isEmpty()) drops.add(stack);
        }
    }
}
