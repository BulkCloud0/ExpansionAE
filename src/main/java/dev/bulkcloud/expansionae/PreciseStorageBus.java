package dev.bulkcloud.expansionae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.Api;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;

/**
 * ExtendedAE precise storage bus backport.
 *
 * Configured stack counts are target amounts for insertion. Storage modes
 * control whether a configured item is exposed for extraction based on its
 * current amount versus the configured threshold.
 */
public final class PreciseStorageBus extends FilteredStorageBus {
    private PreciseStorageMode storageMode = PreciseStorageMode.DEFAULT;

    public PreciseStorageBus(ItemStack stack) {
        super(stack);
    }

    public PreciseStorageMode getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(PreciseStorageMode mode) {
        if (mode != null && mode != storageMode) {
            storageMode = mode;
            getHost().markForSave();
            forceFilterUpdate();
        }
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        int ordinal = data.getByte("preciseStorageMode");
        PreciseStorageMode[] values = PreciseStorageMode.values();
        storageMode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : PreciseStorageMode.DEFAULT;
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        data.putByte("preciseStorageMode", (byte) storageMode.ordinal());
    }

    @Override
    protected IPartitionList<IAEItemStack> createPartitionList() {
        final List<IAEItemStack> configured = configuredStacks();
        return new IPartitionList<IAEItemStack>() {
            @Override
            public boolean isListed(IAEItemStack input) {
                return configuredAmount(input) > 0;
            }

            @Override
            public boolean isEmpty() {
                return configured.isEmpty();
            }

            @Override
            public Iterable<IAEItemStack> getItems() {
                return configured;
            }
        };
    }

    @Override
    protected MEInventoryHandler<IAEItemStack> createInventoryHandler(IMEInventory<IAEItemStack> inv) {
        return new PreciseHandler(inv);
    }

    private List<IAEItemStack> configuredStacks() {
        List<IAEItemStack> result = new ArrayList<>();
        IItemHandler config = getInventoryByName("config");
        IItemStorageChannel channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
        if (config == null) return result;
        for (int i = 0; i < config.getSlots(); i++) {
            ItemStack stack = config.getStackInSlot(i);
            if (!stack.isEmpty()) {
                IAEItemStack ae = channel.createStack(stack);
                ae.setStackSize(Math.max(1, stack.getCount()));
                result.add(ae);
            }
        }
        return result;
    }

    private long configuredAmount(IAEItemStack what) {
        if (what == null) return 0;
        IItemHandler config = getInventoryByName("config");
        if (config == null) return 0;
        for (int i = 0; i < config.getSlots(); i++) {
            ItemStack filter = config.getStackInSlot(i);
            if (!filter.isEmpty() && what.isSameType(filter)) {
                return Math.max(1, filter.getCount());
            }
        }
        return 0;
    }

    private final class PreciseHandler extends MEInventoryHandler<IAEItemStack> {
        PreciseHandler(IMEInventory<IAEItemStack> inv) {
            super(inv, Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
        }

        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
            long threshold = configuredAmount(input);
            if (threshold <= 0) return input;

            IItemList<IAEItemStack> list = Api.instance().storage()
                    .getStorageChannel(IItemStorageChannel.class).createList();
            super.getAvailableItems(list);
            IAEItemStack current = list.findPrecise(input);
            long stored = current == null ? 0 : current.getStackSize();
            long room = threshold - stored;
            if (room <= 0) return input;

            IAEItemStack limited = input.copy();
            limited.setStackSize(Math.min(room, input.getStackSize()));
            IAEItemStack limitedRemainder = super.injectItems(limited, type, src);

            long accepted = limited.getStackSize()
                    - (limitedRemainder == null ? 0 : limitedRemainder.getStackSize());
            if (accepted >= input.getStackSize()) return null;
            IAEItemStack remainder = input.copy();
            remainder.setStackSize(input.getStackSize() - accepted);
            return remainder;
        }

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable type, IActionSource src) {
            if (storageMode == PreciseStorageMode.DEFAULT) {
                return super.extractItems(request, type, src);
            }

            long threshold = configuredAmount(request);
            if (threshold <= 0) return null;

            IAEItemStack probe = request.copy();
            probe.setStackSize(Long.MAX_VALUE);
            IAEItemStack stored = super.extractItems(probe, Actionable.SIMULATE, src);
            long amount = stored == null ? 0 : stored.getStackSize();
            if (!storageMode.test(amount, threshold)) return null;
            return super.extractItems(request, type, src);
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
            if (storageMode == PreciseStorageMode.DEFAULT) {
                return super.getAvailableItems(out);
            }

            IItemList<IAEItemStack> current = Api.instance().storage()
                    .getStorageChannel(IItemStorageChannel.class).createList();
            super.getAvailableItems(current);
            for (IAEItemStack stack : current) {
                long threshold = configuredAmount(stack);
                if (threshold > 0 && storageMode.test(stack.getStackSize(), threshold)) {
                    out.addStorage(stack);
                }
            }
            return out;
        }
    }

    @Override
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!isRemote()) {
            ContainerOpener.openContainer(PreciseStorageBusContainer.TYPE, player, ContainerLocator.forPart(this));
        }
        return true;
    }
}
