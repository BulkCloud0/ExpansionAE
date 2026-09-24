package com.bulkcloud.expansionae.feature.stockexport;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import com.bulkcloud.expansionae.ExpansionAE;

import appeng.api.config.FuzzyMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.items.parts.PartModels;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.PartModel;
import appeng.parts.automation.ExportBusPart;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.ItemSlot;
import appeng.util.item.AEItemStack;

public final class StockExportBusPart extends ExportBusPart {
    public static final ResourceLocation MODEL_BASE =
            new ResourceLocation(ExpansionAE.MOD_ID, "part/stock_export_bus_base");

    private static final ResourceLocation MODEL_OFF =
            new ResourceLocation("appliedenergistics2", "part/export_bus_off");
    private static final ResourceLocation MODEL_ON =
            new ResourceLocation("appliedenergistics2", "part/export_bus_on");
    private static final ResourceLocation MODEL_HAS_CHANNEL =
            new ResourceLocation("appliedenergistics2", "part/export_bus_has_channel");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_HAS_CHANNEL);

    private static final String NEXT_SLOT_NBT = "stock_next_slot";

    private final StockExportTargets targets = new StockExportTargets();
    private final IActionSource actionSource = new MachineSource(this);

    private int nextSlot;

    public StockExportBusPart(ItemStack stack) {
        super(stack);
    }

    public StockExportTargets getTargets() {
        return this.targets;
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        this.targets.readFromNBT(extra);
        this.nextSlot = Math.floorMod(extra.getInt(NEXT_SLOT_NBT), StockExportTargets.SLOT_COUNT);
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        this.targets.writeToNBT(extra);
        extra.putInt(NEXT_SLOT_NBT, this.nextSlot);
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy().isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        try {
            InventoryAdaptor destination = this.getHandler();
            if (destination == null) {
                return TickRateModulation.SLEEP;
            }

            IMEMonitor<IAEItemStack> network = this.getProxy().getStorage()
                    .getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
            IEnergyGrid energy = this.getProxy().getEnergy();
            IItemHandler config = this.getInventoryByName("config");
            FuzzyMode fuzzyMode =
                    (FuzzyMode) this.getConfigManager().getSetting(Settings.FUZZY_MODE);
            SchedulingMode schedulingMode =
                    (SchedulingMode) this.getConfigManager().getSetting(Settings.SCHEDULING_MODE);

            long remainingBudget = this.calculateItemsToSend();
            boolean didWork = false;
            int visited = 0;

            for (; visited < this.availableSlots() && remainingBudget > 0; visited++) {
                int slot = this.getStartingSlot(schedulingMode, visited);
                ItemStack filterStack = config.getStackInSlot(slot);

                if (filterStack.isEmpty()) {
                    continue;
                }

                IAEItemStack filter = AEItemStack.fromItemStack(filterStack);
                if (filter == null) {
                    continue;
                }

                long current = this.countCurrentStock(destination, filter, fuzzyMode);
                long missing = this.targets.missingAmount(slot, current, remainingBudget);
                if (missing <= 0) {
                    continue;
                }

                long consumed;
                if (this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                    consumed = this.pushFuzzy(
                            destination, energy, network, filter, fuzzyMode, missing);
                } else {
                    consumed = this.pushExact(destination, energy, network, filter, missing);
                }

                if (consumed > 0) {
                    remainingBudget -= consumed;
                    didWork = true;
                }
            }

            if (didWork && schedulingMode == SchedulingMode.ROUNDROBIN) {
                this.nextSlot = (this.nextSlot + Math.max(1, visited))
                        % Math.max(1, this.availableSlots());
            }

            return didWork ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.SLOWER;
        }
    }

    long countCurrentStock(
            InventoryAdaptor destination,
            IAEItemStack filter,
            FuzzyMode fuzzyMode) {
        long total = 0;

        for (ItemSlot slot : destination) {
            ItemStack stack = slot.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }

            IAEItemStack candidate = AEItemStack.fromItemStack(stack);
            if (candidate == null) {
                continue;
            }

            boolean matches = filter.isSameType(candidate);
            if (!matches && this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                matches = filter.fuzzyComparison(candidate, fuzzyMode);
            }

            if (matches) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private long pushFuzzy(
            InventoryAdaptor destination,
            IEnergyGrid energy,
            IMEMonitor<IAEItemStack> network,
            IAEItemStack filter,
            FuzzyMode fuzzyMode,
            long wanted) {
        long consumed = 0;

        for (IAEItemStack candidate : network.getStorageList().findFuzzy(filter, fuzzyMode)) {
            long remaining = wanted - consumed;
            if (remaining <= 0) {
                break;
            }

            consumed += this.pushExact(destination, energy, network, candidate, remaining);
        }

        return consumed;
    }

    private long pushExact(
            InventoryAdaptor destination,
            IEnergyGrid energy,
            IMEMonitor<IAEItemStack> network,
            IAEItemStack candidate,
            long wanted) {
        if (wanted <= 0) {
            return 0;
        }

        int requestAmount = (int) Math.min(Integer.MAX_VALUE, wanted);
        ItemStack simulatedInput = candidate.createItemStack();
        simulatedInput.setCount(requestAmount);

        ItemStack simulatedRemainder = destination.simulateAdd(simulatedInput);
        long canFit = simulatedRemainder.isEmpty()
                ? requestAmount
                : requestAmount - simulatedRemainder.getCount();

        if (canFit <= 0) {
            return 0;
        }

        IAEItemStack request = candidate.copy();
        request.setStackSize(canFit);
        IAEItemStack extracted =
                Platform.poweredExtraction(energy, network, request, this.actionSource);

        if (extracted == null || extracted.getStackSize() <= 0) {
            return 0;
        }

        ItemStack failed = destination.addItems(extracted.createItemStack());
        if (!failed.isEmpty()) {
            IAEItemStack remainder = AEItemStack.fromItemStack(failed);
            if (remainder != null) {
                network.injectItems(
                        remainder,
                        appeng.api.config.Actionable.MODULATE,
                        this.actionSource);
            }
        }

        long failedAmount = failed.isEmpty() ? 0 : failed.getCount();
        return Math.max(0, extracted.getStackSize() - failedAmount);
    }

    private int getStartingSlot(SchedulingMode schedulingMode, int offset) {
        int slots = Math.max(1, this.availableSlots());

        if (schedulingMode == SchedulingMode.RANDOM) {
            return Platform.getRandom().nextInt(slots);
        }

        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            return (this.nextSlot + offset) % slots;
        }

        return offset;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }
}
