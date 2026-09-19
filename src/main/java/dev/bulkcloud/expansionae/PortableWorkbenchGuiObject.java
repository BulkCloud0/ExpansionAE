package dev.bulkcloud.expansionae;

import javax.annotation.Nullable;

import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.util.IConfigManager;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

/**
 * Item-backed version of AE2 8.4's CellWorkbenchTileEntity.
 * The inserted cell and temporary partition copy are stored in the helmet NBT.
 */
public final class PortableWorkbenchGuiObject implements IGuiItemObject, IUpgradeableHost,
        IAEAppEngInventory, IConfigManagerHost {
    private static final String TAG = "ExpansionAEPortableWorkbench";

    private final ItemStack helmet;
    private final boolean remote;
    private final AppEngInternalInventory cell = new AppEngInternalInventory(this, 1);
    private final AppEngInternalAEInventory config = new AppEngInternalAEInventory(this, 63);
    private final ConfigManager manager = new ConfigManager(this);
    private IItemHandler cachedUpgrades;
    private IItemHandler cachedCellConfig;
    private boolean locked;

    public PortableWorkbenchGuiObject(ItemStack helmet, boolean remote) {
        this.helmet = helmet;
        this.remote = remote;
        manager.registerSetting(Settings.COPY_MODE, CopyMode.CLEAR_ON_REMOVE);

        CompoundNBT root = helmet.getChildTag(TAG);
        if (root != null) {
            cell.readFromNBT(root, "cell");
            config.readFromNBT(root, "config");
            manager.readFromNBT(root);
        }
    }

    @Override
    public ItemStack getItemStack() {
        return helmet;
    }

    public ICellWorkbenchItem getCell() {
        ItemStack stack = cell.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ICellWorkbenchItem) {
            return (ICellWorkbenchItem) stack.getItem();
        }
        return null;
    }

    @Nullable
    public IItemHandler getCellUpgradeInventory() {
        if (cachedUpgrades == null) {
            ICellWorkbenchItem workbenchCell = getCell();
            ItemStack stack = cell.getStackInSlot(0);
            if (workbenchCell == null || stack.isEmpty()) return null;
            cachedUpgrades = workbenchCell.getUpgradesInventory(stack);
        }
        return cachedUpgrades;
    }

    @Nullable
    private IItemHandler getCellConfigInventory() {
        if (cachedCellConfig == null) {
            ICellWorkbenchItem workbenchCell = getCell();
            ItemStack stack = cell.getStackInSlot(0);
            if (workbenchCell == null || stack.isEmpty()) return null;
            cachedCellConfig = workbenchCell.getConfigInventory(stack);
        }
        return cachedCellConfig;
    }

    public void setCellFuzzyMode(FuzzyMode mode) {
        ICellWorkbenchItem workbenchCell = getCell();
        ItemStack stack = cell.getStackInSlot(0);
        if (workbenchCell != null && !stack.isEmpty()) {
            workbenchCell.setFuzzyMode(stack, mode);
            saveChanges();
        }
    }

    public FuzzyMode getCellFuzzyMode() {
        ICellWorkbenchItem workbenchCell = getCell();
        ItemStack stack = cell.getStackInSlot(0);
        return workbenchCell != null && !stack.isEmpty()
                ? workbenchCell.getFuzzyMode(stack)
                : FuzzyMode.IGNORE_ALL;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("cell".equals(name)) return cell;
        if ("config".equals(name)) return config;
        if ("upgrades".equals(name)) {
            IItemHandler upgrades = getCellUpgradeInventory();
            return upgrades == null ? EmptyHandler.INSTANCE : upgrades;
        }
        return null;
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return 0;
    }

    @Override
    public TileEntity getTile() {
        return null;
    }

    @Override
    public IConfigManager getConfigManager() {
        return manager;
    }

    @Override
    public void updateSetting(IConfigManager configManager, Settings settingName, Enum<?> newValue) {
        saveChanges();
    }

    @Override
    public void saveChanges() {
        CompoundNBT root = new CompoundNBT();
        cell.writeToNBT(root, "cell");
        config.writeToNBT(root, "config");
        manager.writeToNBT(root);
        helmet.getOrCreateTag().put(TAG, root);
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot, InvOperation operation,
            ItemStack removedStack, ItemStack newStack) {
        if (locked) return;

        if (inventory == cell) {
            locked = true;
            try {
                cachedUpgrades = null;
                cachedCellConfig = null;
                IItemHandler cellConfig = getCellConfigInventory();
                if (cellConfig != null) {
                    boolean hasConfig = false;
                    for (int i = 0; i < cellConfig.getSlots(); i++) {
                        if (!cellConfig.getStackInSlot(i).isEmpty()) {
                            hasConfig = true;
                            break;
                        }
                    }

                    if (hasConfig) {
                        for (int i = 0; i < Math.min(config.getSlots(), cellConfig.getSlots()); i++) {
                            config.setStackInSlot(i, cellConfig.getStackInSlot(i));
                        }
                    } else {
                        ItemHandlerUtil.copy(config, cellConfig, false);
                    }
                } else if (manager.getSetting(Settings.COPY_MODE) == CopyMode.CLEAR_ON_REMOVE) {
                    ItemHandlerUtil.clear(config);
                }
            } finally {
                locked = false;
            }
        } else if (inventory == config) {
            locked = true;
            try {
                IItemHandler cellConfig = getCellConfigInventory();
                if (cellConfig != null) {
                    ItemHandlerUtil.copy(config, cellConfig, false);
                    ItemHandlerUtil.copy(cellConfig, config, false);
                }
            } finally {
                locked = false;
            }
        }

        saveChanges();
    }

    @Override
    public boolean isRemote() {
        return remote;
    }
}
