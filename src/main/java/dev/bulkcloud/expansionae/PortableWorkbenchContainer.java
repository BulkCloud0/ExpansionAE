package dev.bulkcloud.expansionae;

import java.util.Iterator;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeTypeOnlySlot;
import appeng.container.slot.OptionalRestrictedInputSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.core.Api;
import appeng.util.EnumCycler;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.WrapperSupplierItemHandler;
import appeng.util.iterators.NullIterator;

public final class PortableWorkbenchContainer extends UpgradeableContainer {
    public static final ContainerType<PortableWorkbenchContainer> TYPE = ContainerTypeBuilder
            .create(PortableWorkbenchContainer::new, PortableWorkbenchGuiObject.class)
            .build("expansionae_portable_workbench");

    private final PortableWorkbenchGuiObject workbench;

    @GuiSync(2)
    public CopyMode copyMode = CopyMode.CLEAR_ON_REMOVE;

    private ItemStack previousCell = ItemStack.EMPTY;
    private int lastUpgradeSlots;

    public PortableWorkbenchContainer(int id, PlayerInventory playerInventory, PortableWorkbenchGuiObject workbench) {
        super(TYPE, id, playerInventory, workbench);
        this.workbench = workbench;
    }

    @Override
    protected void setupConfig() {
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.WORKBENCH_CELL,
                workbench.getInventoryByName("cell"), 0), SlotSemantic.STORAGE_CELL);

        IItemHandler config = workbench.getInventoryByName("config");
        for (int i = 0; i < 63; i++) {
            addSlot(new FakeTypeOnlySlot(config, i), SlotSemantic.CONFIG);
        }

        WrapperSupplierItemHandler upgrades = new WrapperSupplierItemHandler(this::getCellUpgradeInventory);
        for (int i = 0; i < 24; i++) {
            addSlot(new OptionalRestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES,
                    upgrades, this, i, i, getPlayerInventory()), SlotSemantic.UPGRADE);
        }
    }

    public ItemStack getWorkbenchItem() {
        return workbench.getInventoryByName("cell").getStackInSlot(0);
    }

    public IItemHandler getCellUpgradeInventory() {
        IItemHandler inventory = workbench.getCellUpgradeInventory();
        return inventory == null ? EmptyHandler.INSTANCE : inventory;
    }

    @Override
    public int availableUpgrades() {
        ItemStack current = getWorkbenchItem();
        if (!ItemStack.areItemStacksEqual(previousCell, current)) {
            previousCell = current.copy();
            lastUpgradeSlots = getCellUpgradeInventory().getSlots();
        }
        return lastUpgradeSlots;
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return idx < availableUpgrades();
    }

    public CopyMode getCopyMode() {
        return copyMode;
    }

    public void nextCopyMode() {
        workbench.getConfigManager().putSetting(Settings.COPY_MODE, EnumCycler.next(copyMode));
        copyMode = (CopyMode) workbench.getConfigManager().getSetting(Settings.COPY_MODE);
        workbench.saveChanges();
    }

    public void setCellFuzzy(FuzzyMode fuzzyMode) {
        workbench.setCellFuzzyMode(fuzzyMode);
        setFuzzyMode(fuzzyMode);
    }

    public void clearPartition() {
        ItemHandlerUtil.clear(workbench.getInventoryByName("config"));
        workbench.saveChanges();
        detectAndSendChanges();
    }

    public void partitionFromCell() {
        IItemHandler config = workbench.getInventoryByName("config");
        ItemStack cell = getWorkbenchItem();
        IStorageChannel<?> channel = cell.getItem() instanceof IStorageCell
                ? ((IStorageCell<?>) cell.getItem()).getChannel()
                : Api.instance().storage().getStorageChannel(IItemStorageChannel.class);

        Iterator<? extends IAEStack<?>> iterator = iterateCellStacks(cell, channel);
        for (int slot = 0; slot < config.getSlots(); slot++) {
            ItemHandlerUtil.setStackInSlot(config, slot,
                    iterator.hasNext() ? iterator.next().asItemStackRepresentation() : ItemStack.EMPTY);
        }
        workbench.saveChanges();
        detectAndSendChanges();
    }

    private <T extends IAEStack<T>> Iterator<? extends IAEStack<T>> iterateCellStacks(
            ItemStack cell, IStorageChannel<T> channel) {
        IMEInventory<T> inventory = Api.instance().registries().cell().getCellInventory(cell, null, channel);
        if (inventory == null) return new NullIterator<T>();
        IItemList<T> list = inventory.getAvailableItems(channel.createList());
        return list.iterator();
    }

    @Override
    public void detectAndSendChanges() {
        ItemStack current = getWorkbenchItem();

        if (isServer()) {
            for (IContainerListener listener : listeners) {
                if (!ItemStack.areItemStacksEqual(previousCell, current)) {
                    for (Slot slot : inventorySlots) {
                        if (slot instanceof OptionalRestrictedInputSlot) {
                            listener.sendSlotContents(this, slot.slotNumber, slot.getStack());
                        }
                    }
                    if (listener instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) listener).isChangingQuantityOnly = false;
                    }
                }
            }
            copyMode = (CopyMode) workbench.getConfigManager().getSetting(Settings.COPY_MODE);
            setFuzzyMode(workbench.getCellFuzzyMode());
        }

        previousCell = current.copy();
        standardDetectAndSendChanges();
    }
}
