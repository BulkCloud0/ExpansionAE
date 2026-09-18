package dev.bulkcloud.expansionae;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.definitions.IItemDefinition;
import appeng.api.features.InscriberProcessType;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.core.Api;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.parts.automation.UpgradeInventory;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.tile.grid.AENetworkPowerTileEntity;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.misc.InscriberRecipes;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.inv.InvOperation;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * Four-process Extended Inscriber backport.
 *
 * Inventory layout:
 * 0..3 top plates, 4..7 bottom plates, 8..11 inputs, 12..15 outputs.
 */
public final class ExpandedInscriberTile extends AENetworkPowerTileEntity
        implements IGridTickable, IUpgradeableHost, IConfigManagerHost {
    public static final int THREADS = 4;
    private static final int MAX_PROCESSING_TIME = 100;

    private final AppEngInternalInventory inv = new AppEngInternalInventory(this, 16);
    private final UpgradeInventory upgrades;
    private final IConfigManager settings;

    private final int[] processingTime = new int[THREADS];
    private final int[] finalStep = new int[THREADS];
    private final boolean[] smash = new boolean[THREADS];
    private final InscriberRecipe[] cachedTask = new InscriberRecipe[THREADS];

    private final IItemHandler topExternal = new SelectedHandler(0, 1, 2, 3);
    private final IItemHandler bottomExternal = new SelectedHandler(4, 5, 6, 7);
    private final IItemHandler sideExternal = new SelectedHandler(8, 9, 10, 11, 12, 13, 14, 15);

    public ExpandedInscriberTile(TileEntityType<?> type) {
        super(type);
        getProxy().setValidSides(EnumSet.noneOf(Direction.class));
        setInternalMaxPower(6400);
        getProxy().setIdlePowerUsage(0);
        settings = new ConfigManager(this);

        upgrades = new UpgradeInventory(this, 4) {
            @Override
            public int getMaxInstalled(Upgrades upgrade) {
                return upgrade == Upgrades.SPEED ? 4 : 0;
            }
        };

        inv.setFilter(new ItemFilter());
        for (int i = 0; i < THREADS; i++) {
            inv.setMaxStackSize(i, 1);
            inv.setMaxStackSize(4 + i, 1);
            inv.setMaxStackSize(8 + i, 64);
            inv.setMaxStackSize(12 + i, 64);
        }
    }

    private static int topSlot(int thread) { return thread; }
    private static int bottomSlot(int thread) { return 4 + thread; }
    private static int inputSlot(int thread) { return 8 + thread; }
    private static int outputSlot(int thread) { return 12 + thread; }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Override
    public void setOrientation(Direction forward, Direction up) {
        super.setOrientation(forward, up);
        EnumSet<Direction> sides = EnumSet.complementOf(EnumSet.of(getForward()));
        getProxy().setValidSides(sides);
        setPowerSides(sides);
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        upgrades.writeToNBT(data, "upgrades");
        settings.writeToNBT(data);
        for (int i = 0; i < THREADS; i++) {
            data.putInt("processing" + i, processingTime[i]);
            data.putInt("finalStep" + i, finalStep[i]);
            data.putBoolean("smash" + i, smash[i]);
        }
        return data;
    }

    @Override
    public void read(BlockState state, CompoundNBT data) {
        super.read(state, data);
        upgrades.readFromNBT(data, "upgrades");
        settings.readFromNBT(data);
        for (int i = 0; i < THREADS; i++) {
            processingTime[i] = data.getInt("processing" + i);
            finalStep[i] = data.getInt("finalStep" + i);
            smash[i] = data.getBoolean("smash" + i);
            cachedTask[i] = null;
        }
    }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        for (ItemStack stack : upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Override
    public IItemHandler getInternalInventory() {
        return inv;
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removed, ItemStack added) {
        int thread = threadForSlot(slot);
        if (thread >= 0) {
            cachedTask[thread] = null;
            if (slot == inputSlot(thread)) {
                processingTime[thread] = 0;
            }
        }
        try {
            getProxy().getTick().wakeDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
        markForUpdate();
    }

    private int threadForSlot(int slot) {
        if (slot >= 0 && slot < 4) return slot;
        if (slot >= 4 && slot < 8) return slot - 4;
        if (slot >= 8 && slot < 12) return slot - 8;
        if (slot >= 12 && slot < 16) return slot - 12;
        return -1;
    }

    @Nullable
    public InscriberRecipe getTask(int thread) {
        if (thread < 0 || thread >= THREADS || world == null) {
            return null;
        }
        if (cachedTask[thread] == null) {
            ItemStack input = inv.getStackInSlot(inputSlot(thread));
            ItemStack top = inv.getStackInSlot(topSlot(thread));
            ItemStack bottom = inv.getStackInSlot(bottomSlot(thread));
            if (input.isEmpty()) {
                return null;
            }
            ItemStack singleInput = input.copy();
            singleInput.setCount(1);
            cachedTask[thread] = InscriberRecipes.findRecipe(world, singleInput, top, bottom, true);
        }
        return cachedTask[thread];
    }

    public int getProcessingTime(int thread) {
        return thread >= 0 && thread < THREADS ? processingTime[thread] : 0;
    }

    public int getMaxProcessingTime() {
        return MAX_PROCESSING_TIME;
    }

    public boolean isSmash(int thread) {
        return thread >= 0 && thread < THREADS && smash[thread];
    }

    private boolean hasWork() {
        for (int i = 0; i < THREADS; i++) {
            if (smash[i] || getTask(i) != null) {
                return true;
            }
            processingTime[i] = 0;
        }
        return false;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Inscriber.getMin(), TickRates.Inscriber.getMax(), !hasWork(), false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        boolean active = false;
        for (int thread = 0; thread < THREADS; thread++) {
            active |= tickThread(thread, ticksSinceLastCall);
        }
        return active ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    private boolean tickThread(int thread, int ticksSinceLastCall) {
        if (smash[thread]) {
            finalStep[thread]++;
            if (finalStep[thread] == 8) {
                finishThread(thread);
            } else if (finalStep[thread] >= 16) {
                finalStep[thread] = 0;
                smash[thread] = false;
                markForUpdate();
            }
            return true;
        }

        InscriberRecipe task = getTask(thread);
        if (task == null) {
            processingTime[thread] = 0;
            return false;
        }

        int speedFactor = 1 + upgrades.getInstalledUpgrades(Upgrades.SPEED);
        int powerConsumption = 10 * speedFactor;
        double threshold = powerConsumption - 0.01;

        try {
            IEnergyGrid gridEnergy = getProxy().getEnergy();
            IEnergySource source = this;
            double available = extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            if (available <= threshold) {
                source = gridEnergy;
                available = gridEnergy.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            }
            if (available > threshold) {
                source.extractAEPower(powerConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
                processingTime[thread] += Math.max(1, ticksSinceLastCall) * speedFactor;
            }
        } catch (GridAccessException ignored) {
        }

        if (processingTime[thread] >= MAX_PROCESSING_TIME) {
            processingTime[thread] = MAX_PROCESSING_TIME;
            ItemStack output = task.getOutput().copy();
            output.setCount(1);
            if (inv.insertItem(outputSlot(thread), output, true).isEmpty()) {
                smash[thread] = true;
                finalStep[thread] = 0;
                markForUpdate();
            }
        }
        return true;
    }

    private void finishThread(int thread) {
        InscriberRecipe task = getTask(thread);
        if (task == null) {
            return;
        }
        ItemStack output = task.getOutput().copy();
        output.setCount(1);
        if (!inv.insertItem(outputSlot(thread), output, false).isEmpty()) {
            return;
        }

        processingTime[thread] = 0;
        if (task.getProcessType() == InscriberProcessType.PRESS) {
            consumeOne(topSlot(thread));
            consumeOne(bottomSlot(thread));
        }
        consumeOne(inputSlot(thread));
        cachedTask[thread] = null;
        saveChanges();
    }

    private void consumeOne(int slot) {
        ItemStack stack = inv.getStackInSlot(slot);
        if (stack.isEmpty()) return;
        ItemStack copy = stack.copy();
        copy.shrink(1);
        inv.setStackInSlot(slot, copy);
    }

    @Override
    public IConfigManager getConfigManager() {
        return settings;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("inv".equals(name)) return inv;
        if ("upgrades".equals(name)) return upgrades;
        return null;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return upgrades.getInstalledUpgrades(upgrade);
    }

    @Override
    public void updateSetting(IConfigManager manager, Settings settingName, Enum<?> newValue) {
    }

    @Override
    protected IItemHandler getItemHandlerForSide(@Nonnull Direction facing) {
        if (facing == getUp()) return topExternal;
        if (facing == getUp().getOpposite()) return bottomExternal;
        return sideExternal;
    }

    private final class ItemFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(IItemHandler handler, int slot, int amount) {
            int thread = threadForSlot(slot);
            if (thread >= 0 && smash[thread]) {
                return false;
            }
            return slot < 8 || slot >= 12;
        }

        @Override
        public boolean allowInsert(IItemHandler handler, int slot, ItemStack stack) {
            int thread = threadForSlot(slot);
            if (thread >= 0 && smash[thread]) {
                return false;
            }
            if (slot >= 12) return false;
            if (slot < 8) {
                IItemDefinition namePress = Api.instance().definitions().materials().namePress();
                return namePress.isSameAs(stack) || InscriberRecipes.isValidOptionalIngredient(getWorld(), stack);
            }
            return true;
        }
    }

    private final class SelectedHandler implements IItemHandlerModifiable {
        private final int[] slots;

        SelectedHandler(int... slots) {
            this.slots = slots;
        }

        private int map(int slot) {
            if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException();
            return slots[slot];
        }

        @Override public void setStackInSlot(int slot, ItemStack stack) { inv.setStackInSlot(map(slot), stack); }
        @Override public int getSlots() { return slots.length; }
        @Override public ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(map(slot)); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inv.insertItem(map(slot), stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inv.extractItem(map(slot), amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inv.getSlotLimit(map(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return inv.isItemValid(map(slot), stack); }
    }
}
