package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.Api;

public final class DiskCellInventory implements ICellInventory<IAEItemStack> {
    static final String TAG_UUID = "expansionae_disk_uuid";
    static final String TAG_ITEM_COUNT = "expansionae_disk_item_count";
    static final String TAG_TYPE_COUNT = "expansionae_disk_type_count";

    private final DiskStorageCellItem cellType;
    private final ItemStack cellStack;
    private final ISaveProvider saveProvider;
    private final IItemStorageChannel channel;

    private IItemList<IAEItemStack> contents;
    private boolean dirty;

    public DiskCellInventory(DiskStorageCellItem cellType, ItemStack cellStack, ISaveProvider saveProvider) {
        this.cellType = cellType;
        this.cellStack = cellStack;
        this.saveProvider = saveProvider;
        this.channel = Api.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }

    private IItemList<IAEItemStack> contents() {
        if (contents != null) {
            return contents;
        }

        contents = channel.createList();

        UUID uuid = getUuid();
        DiskStorageData storage = DiskStorageService.getCurrent();
        if (uuid == null || storage == null) {
            return contents;
        }

        DiskStorageData.DiskRecord record = storage.get(uuid);
        if (record == null) {
            return contents;
        }

        ListNBT keys = record.getKeys();
        long[] amounts = record.getAmounts();
        int count = Math.min(keys.size(), amounts.length);

        for (int i = 0; i < count; i++) {
            IAEItemStack stack = channel.createFromNBT(keys.getCompound(i));
            if (stack != null && amounts[i] > 0) {
                stack.setStackSize(amounts[i]);
                contents.add(stack);
            }
        }

        return contents;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) {
            return null;
        }

        if (isNonEmptyStorageCell(input)) {
            return input;
        }

        long accepted = Math.min(input.getStackSize(), getRemainingItemCount());
        if (accepted <= 0) {
            return input;
        }

        if (mode == Actionable.MODULATE) {
            IAEItemStack existing = contents().findPrecise(input);
            if (existing == null) {
                IAEItemStack inserted = input.copy();
                inserted.setStackSize(accepted);
                contents().add(inserted);
            } else {
                existing.incStackSize(accepted);
            }
            changed();
        }

        if (accepted == input.getStackSize()) {
            return null;
        }

        IAEItemStack remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - accepted);
        return remainder;
    }

    private boolean isNonEmptyStorageCell(IAEItemStack input) {
        ItemStack nestedStack = input.createItemStack();
        if (!Api.instance().registries().cell().isCellHandled(nestedStack)) {
            return false;
        }

        ICellInventoryHandler<IAEItemStack> nested =
                Api.instance().registries().cell().getCellInventory(nestedStack, null, channel);
        if (nested == null) {
            return false;
        }

        return !nested.getAvailableItems(channel.createList()).isEmpty();
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) {
            return null;
        }

        IAEItemStack existing = contents().findPrecise(request);
        if (existing == null || existing.getStackSize() <= 0) {
            return null;
        }

        long amount = Math.min(request.getStackSize(), existing.getStackSize());
        IAEItemStack result = existing.copy();
        result.setStackSize(amount);

        if (mode == Actionable.MODULATE) {
            existing.decStackSize(amount);
            changed();
        }

        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                out.add(stack);
            }
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return channel;
    }

    @Override
    public ItemStack getItemStack() {
        return cellStack;
    }

    @Override
    public double getIdleDrain() {
        return cellType.getIdleDrain();
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return cellType.getFuzzyMode(cellStack);
    }

    @Override
    public IItemHandler getConfigInventory() {
        return cellType.getConfigInventory(cellStack);
    }

    @Override
    public IItemHandler getUpgradesInventory() {
        return cellType.getUpgradesInventory(cellStack);
    }

    @Override
    public int getBytesPerType() {
        return 0;
    }

    @Override
    public boolean canHoldNewItem() {
        return getRemainingItemCount() > 0;
    }

    @Override
    public long getTotalBytes() {
        return cellType.getCapacity();
    }

    @Override
    public long getFreeBytes() {
        return Math.max(0, getTotalBytes() - getStoredItemCount());
    }

    @Override
    public long getUsedBytes() {
        return getStoredItemCount();
    }

    @Override
    public long getTotalItemTypes() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getStoredItemCount() {
        long total = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                total += stack.getStackSize();
            }
        }
        return total;
    }

    @Override
    public long getStoredItemTypes() {
        long total = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() > 0) {
                total++;
            }
        }
        return total;
    }

    @Override
    public long getRemainingItemTypes() {
        return getRemainingItemCount() > 0 ? Long.MAX_VALUE - getStoredItemTypes() : 0;
    }

    @Override
    public long getRemainingItemCount() {
        return getFreeBytes();
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public CellState getStatusForCell() {
        if (getStoredItemCount() == 0) {
            return CellState.EMPTY;
        }
        return getRemainingItemCount() > 0 ? CellState.NOT_EMPTY : CellState.FULL;
    }

    @Override
    public void persist() {
        if (!dirty) {
            return;
        }

        long itemCount = getStoredItemCount();
        long typeCount = getStoredItemTypes();
        DiskStorageData storage = DiskStorageService.getCurrent();

        if (storage == null) {
            return;
        }

        if (itemCount <= 0) {
            UUID uuid = getUuid();
            if (uuid != null) {
                storage.remove(uuid);
            }

            if (cellStack.hasTag()) {
                cellStack.getTag().remove(TAG_UUID);
                cellStack.getTag().remove(TAG_ITEM_COUNT);
                cellStack.getTag().remove(TAG_TYPE_COUNT);
            }

            dirty = false;
            return;
        }

        UUID uuid = ensureUuid();
        ListNBT keys = new ListNBT();
        long[] amounts = new long[(int) typeCount];

        int index = 0;
        for (IAEItemStack stack : contents()) {
            if (stack.getStackSize() <= 0) {
                continue;
            }

            CompoundNBT key = new CompoundNBT();
            stack.writeToNBT(key);
            keys.add(key);
            amounts[index++] = stack.getStackSize();
        }

        if (index != amounts.length) {
            long[] trimmed = new long[index];
            System.arraycopy(amounts, 0, trimmed, 0, index);
            amounts = trimmed;
        }

        storage.put(uuid, keys, amounts, itemCount);

        CompoundNBT tag = cellStack.getOrCreateTag();
        tag.putLong(TAG_ITEM_COUNT, itemCount);
        tag.putLong(TAG_TYPE_COUNT, typeCount);
        dirty = false;
    }

    private void changed() {
        dirty = true;
        if (saveProvider != null) {
            saveProvider.saveChanges(this);
        } else {
            persist();
        }
    }

    private UUID getUuid() {
        CompoundNBT tag = cellStack.getTag();
        if (tag == null || !tag.hasUUID(TAG_UUID)) {
            return null;
        }
        return tag.getUUID(TAG_UUID);
    }

    private UUID ensureUuid() {
        UUID uuid = getUuid();
        if (uuid != null) {
            return uuid;
        }

        uuid = UUID.randomUUID();
        cellStack.getOrCreateTag().putUUID(TAG_UUID, uuid);
        return uuid;
    }
}
