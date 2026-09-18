package dev.bulkcloud.expansionae;

import appeng.api.config.AccessRestriction;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class ModStorageBusContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<ModStorageBusContainer> TYPE = ContainerTypeBuilder
            .create(ModStorageBusContainer::new, ModStorageBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_mod_storage_bus");

    private final ModStorageBus host;

    @GuiSync(13)
    public String filter = "";

    @GuiSync(16)
    public AccessRestriction access = AccessRestriction.READ_WRITE;

    @GuiSync(17)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;

    public ModStorageBusContainer(int id, PlayerInventory player, ModStorageBus host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
    }

    public AccessRestriction getReadWriteMode() {
        return access;
    }

    public StorageFilter getStorageFilter() {
        return storageFilter;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            filter = host.getModFilter();
            access = (AccessRestriction) host.getConfigManager().getSetting(Settings.ACCESS);
            storageFilter = (StorageFilter) host.getConfigManager().getSetting(Settings.STORAGE_FILTER);
        }
        super.detectAndSendChanges();
    }

    @Override
    public void applyTextFilter(int key, String value) {
        if (key == 0) host.setModFilter(value);
    }
}
