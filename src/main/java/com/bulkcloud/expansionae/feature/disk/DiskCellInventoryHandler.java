package com.bulkcloud.expansionae.feature.disk;

import java.util.UUID;

import javax.annotation.Nullable;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.AE2Bridge;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ICellRegistry;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public final class DiskCellInventoryHandler implements ICellInventoryHandler<IAEItemStack> {

    private final ItemStack cell;
    @Nullable
    private final ISaveProvider saveProvider;
    private final IItemStorageChannel channel;

    private IItemList<IAEItemStack> contents;
    private boolean loaded;
    private long storedCount;

    DiskCellInventoryHandler(ItemStack cell, @Nullable ISaveProvider saveProvider, IItemStorageChannel channel) {
        this.cell = cell;
        this.saveProvider = saveProvider;
        this.channel = channel;
        this.contents = channel.createList();
        this.storedCount = DiskItem.getStoredCount(cell);
        this.ensureLoaded();
    }

    private DiskItem diskItem() {
        return (DiskItem) this.cell.getItem();
    }

    private void ensureLoaded() {
        if (this.loaded) {
            return;
        }

        UUID id = DiskItem.getDiskId(this.cell);
        if (id == null) {
            this.contents = this.channel.createList();
            this.storedCount = 0L;
            this.loaded = true;
            return;
        }

        DiskStorageData data = DiskStorageData.get();
        if (data == null) {
            return;
        }

        IItemList<IAEItemStack> loadedContents = this.channel.createList();
        long count = 0L;

        ListNBT stacks = data.getContents(id);
        for (int i = 0; i < stacks.size(); i++) {
            CompoundNBT stackTag = stacks.getCompound(i);
            IAEItemStack stack = this.channel.createFromNBT(stackTag);
            if (stack == null || stack.getStackSize() <= 0L) {
                continue;
            }

            loadedContents.addStorage(stack);
            count += stack.getStackSize();
        }

        this.contents = loadedContents;
        this.storedCount = count;
        DiskItem.setStoredCount(this.cell, count);
        this.loaded = true;
    }

    private boolean isStorageCell(IAEItemStack input) {
        ICellRegistry registry = AE2Bridge.api().registries().cell();
        return registry.isCellHandled(input.createItemStack());
    }

    private long freeCapacity() {
        long free = this.diskItem().getCapacity() - this.storedCount;
        return Math.max(0L, free);
    }

    private void persistContents() {
        DiskStorageData data = DiskStorageData.get();
        if (data == null) {
            ExpansionAE.LOGGER.error("Cannot persist DISK contents because no server is available");
            return;
        }

        if (this.storedCount <= 0L) {
            UUID oldId = DiskItem.getDiskId(this.cell);
            if (oldId != null) {
                data.remove(oldId);
            }

            DiskItem.clearDiskId(this.cell);
            DiskItem.setStoredCount(this.cell, 0L);
            this.notifySaveProvider();
            return;
        }

        UUID id = DiskItem.getOrCreateDiskId(this.cell);
        ListNBT stacks = new ListNBT();

        for (IAEItemStack stack : this.contents) {
            if (stack.getStackSize() <= 0L) {
                continue;
            }

            CompoundNBT stackTag = new CompoundNBT();
            stack.writeToNBT(stackTag);
            stacks.add(stackTag);
        }

        data.putContents(id, stacks);
        DiskItem.setStoredCount(this.cell, this.storedCount);
        this.notifySaveProvider();
    }

    private void notifySaveProvider() {
        if (this.saveProvider != null) {
            this.saveProvider.saveChanges(null);
        }
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0L) {
            return null;
        }

        this.ensureLoaded();

        if (!this.loaded || this.isStorageCell(input)) {
            return input;
        }

        long free = this.freeCapacity();
        if (free <= 0L) {
            return input;
        }

        long inserted = Math.min(free, input.getStackSize());
        if (mode == Actionable.MODULATE) {
            IAEItemStack existing = this.contents.findPrecise(input);

            if (existing == null) {
                IAEItemStack toStore = input.copy();
                toStore.setStackSize(inserted);
                this.contents.addStorage(toStore);
            } else {
                existing.incStackSize(inserted);
            }

            this.storedCount += inserted;
            this.persistContents();
        }

        if (inserted >= input.getStackSize()) {
            return null;
        }

        IAEItemStack remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - inserted);
        return remainder;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0L) {
            return null;
        }

        this.ensureLoaded();
        if (!this.loaded) {
            return null;
        }

        IAEItemStack existing = this.contents.findPrecise(request);
        if (existing == null || existing.getStackSize() <= 0L) {
            return null;
        }

        long extracted = Math.min(request.getStackSize(), existing.getStackSize());
        IAEItemStack result = existing.copy();
        result.setStackSize(extracted);

        if (mode == Actionable.MODULATE) {
            existing.decStackSize(extracted);
            this.storedCount = Math.max(0L, this.storedCount - extracted);
            this.persistContents();
        }

        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        this.ensureLoaded();

        if (!this.loaded) {
            return out;
        }

        for (IAEItemStack stack : this.contents) {
            if (stack.getStackSize() > 0L) {
                out.addStorage(stack.copy());
            }
        }

        return out;
    }

    @Override
    public IItemStorageChannel getChannel() {
        return this.channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null || input.getStackSize() <= 0L) {
            return false;
        }

        this.ensureLoaded();
        return this.loaded && this.freeCapacity() > 0L && !this.isStorageCell(input);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int pass) {
        return true;
    }

    @Nullable
    @Override
    public ICellInventory<IAEItemStack> getCellInv() {
        return null;
    }

    @Override
    public boolean isPreformatted() {
        return false;
    }

    @Override
    public boolean isFuzzy() {
        return false;
    }

    @Override
    public IncludeExclude getIncludeExcludeMode() {
        return IncludeExclude.WHITELIST;
    }

    long getStoredCount() {
        this.ensureLoaded();
        return this.loaded ? this.storedCount : DiskItem.getStoredCount(this.cell);
    }
}
