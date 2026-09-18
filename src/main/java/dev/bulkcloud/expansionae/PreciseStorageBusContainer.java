package dev.bulkcloud.expansionae;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.OptionalFakeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class PreciseStorageBusContainer extends UpgradeableContainer {
    public static final ContainerType<PreciseStorageBusContainer> TYPE = ContainerTypeBuilder
            .create(PreciseStorageBusContainer::new, PreciseStorageBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_precise_storage_bus");

    private final PreciseStorageBus host;

    @GuiSync(18)
    public AccessRestriction access = AccessRestriction.READ_WRITE;

    @GuiSync(19)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;

    @GuiSync(20)
    public PreciseStorageMode storageMode = PreciseStorageMode.DEFAULT;

    public PreciseStorageBusContainer(int id, PlayerInventory player, PreciseStorageBus host) {
        super(TYPE, id, player, host);
        this.host = host;
        registerClientAction("cycleStorageMode", () -> host.setStorageMode(host.getStorageMode().next()));
    }

    @Override
    protected void setupConfig() {
        IItemHandler config = getUpgradeable().getInventoryByName("config");
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 9; x++) {
                int slot = y * 9 + x;
                if (y < 2) {
                    addSlot(new FakeSlot(config, slot), SlotSemantic.CONFIG);
                } else {
                    addSlot(new OptionalFakeSlot(config, this, slot, y - 2), SlotSemantic.CONFIG);
                }
            }
        }
        setupUpgrades();
    }

    @Override
    protected boolean supportCapacity() {
        return true;
    }

    @Override
    public int availableUpgrades() {
        return 5;
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return getUpgradeable().getInstalledUpgrades(Upgrades.CAPACITY) > idx;
    }

    public AccessRestriction getReadWriteMode() {
        return access;
    }

    public StorageFilter getStorageFilter() {
        return storageFilter;
    }

    public PreciseStorageMode getStorageMode() {
        return storageMode;
    }

    public void cycleStorageMode() {
        if (isRemote()) sendClientAction("cycleStorageMode");
        else host.setStorageMode(host.getStorageMode().next());
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            setFuzzyMode((FuzzyMode) host.getConfigManager().getSetting(Settings.FUZZY_MODE));
            access = (AccessRestriction) host.getConfigManager().getSetting(Settings.ACCESS);
            storageFilter = (StorageFilter) host.getConfigManager().getSetting(Settings.STORAGE_FILTER);
            storageMode = host.getStorageMode();
        }
        standardDetectAndSendChanges();
    }
}
