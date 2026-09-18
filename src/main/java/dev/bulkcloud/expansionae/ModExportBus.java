package dev.bulkcloud.expansionae;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.Api;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.ExportBusPart;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/** ExtendedAE-style export bus filtered by one or more comma-separated mod ids/names. */
public final class ModExportBus extends ExportBusPart {
    private final IActionSource source = new MachineSource(this);
    private String modFilter = "";

    public ModExportBus(ItemStack stack) {
        super(stack);
    }

    public String getModFilter() {
        return modFilter;
    }

    public void setModFilter(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 128) normalized = normalized.substring(0, 128);
        if (!normalized.equals(modFilter)) {
            modFilter = normalized;
            getHost().markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT extra) {
        super.readFromNBT(extra);
        modFilter = extra.getString("modFilter");
    }

    @Override
    public void writeToNBT(CompoundNBT extra) {
        super.writeToNBT(extra);
        extra.putString("modFilter", modFilter);
    }

    @Override
    protected int calculateItemsToSend() {
        return 8 * super.calculateItemsToSend();
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!getProxy().isActive() || !canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        InventoryAdaptor destination = getHandler();
        if (destination == null) {
            return TickRateModulation.SLEEP;
        }

        Set<String> acceptedMods = parseFilter();
        if (acceptedMods.isEmpty()) {
            return TickRateModulation.SLOWER;
        }

        try {
            IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> network = getProxy().getStorage().getInventory(channel);
            IEnergyGrid energy = getProxy().getEnergy();
            int budget = calculateItemsToSend();
            boolean worked = false;

            for (IAEItemStack candidate : network.getStorageList()) {
                if (budget <= 0) break;
                if (candidate == null || candidate.getStackSize() <= 0 || !matches(candidate, acceptedMods)) continue;

                ItemStack probe = candidate.createItemStack();
                probe.setCount(Math.min(Math.min(budget, probe.getMaxStackSize()),
                        (int) Math.min(Integer.MAX_VALUE, candidate.getStackSize())));
                ItemStack leftover = destination.simulateAdd(probe);
                int canFit = probe.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
                if (canFit <= 0) continue;

                IAEItemStack request = candidate.copy();
                request.setStackSize(canFit);
                IAEItemStack extracted = Platform.poweredExtraction(energy, network, request, source);
                if (extracted == null || extracted.getStackSize() <= 0) continue;

                ItemStack failed = destination.addItems(extracted.createItemStack());
                int failedCount = failed.isEmpty() ? 0 : failed.getCount();
                if (failedCount > 0) {
                    IAEItemStack restore = extracted.copy();
                    restore.setStackSize(failedCount);
                    network.injectItems(restore, Actionable.MODULATE, source);
                }

                int moved = (int) extracted.getStackSize() - failedCount;
                if (moved > 0) {
                    budget -= moved;
                    worked = true;
                }
            }

            return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    private Set<String> parseFilter() {
        Set<String> result = new HashSet<>();
        for (String token : modFilter.split(",")) {
            String trimmed = token.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private boolean matches(IAEItemStack stack, Set<String> acceptedMods) {
        ResourceLocation id = stack.createItemStack().getItem().getRegistryName();
        if (id == null) return false;
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        if (acceptedMods.contains(namespace)) return true;
        String display = Platform.getModName(namespace);
        return display != null && acceptedMods.contains(display.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(ModExportBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
