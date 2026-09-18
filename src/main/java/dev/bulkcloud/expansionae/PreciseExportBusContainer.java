package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.OptionalFakeSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class PreciseExportBusContainer extends UpgradeableContainer {
    public static final ContainerType<PreciseExportBusContainer> TYPE = ContainerTypeBuilder
            .create(PreciseExportBusContainer::new, PreciseExportBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_precise_export_bus");

    private final PreciseExportBus host;

    @GuiSync(9)
    public boolean multipleMode;

    public PreciseExportBusContainer(int id, PlayerInventory player, PreciseExportBus host) {
        super(TYPE, id, player, host);
        this.host = host;
        registerClientAction("toggleBatchMode", () -> host.setMultipleMode(!host.isMultipleMode()));
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
        IItemHandler config = getUpgradeable().getInventoryByName("config");
        addSlot(new FakeSlot(config, 0), SlotSemantic.CONFIG);
        for (int i = 1; i <= 4; i++) {
            addSlot(new OptionalFakeSlot(config, this, i, 1), SlotSemantic.CONFIG);
        }
        for (int i = 5; i <= 8; i++) {
            addSlot(new OptionalFakeSlot(config, this, i, 2), SlotSemantic.CONFIG);
        }
    }

    public void toggleBatchMode() {
        if (isRemote()) sendClientAction("toggleBatchMode");
        else host.setMultipleMode(!host.isMultipleMode());
    }

    public boolean isMultipleMode() {
        return multipleMode;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) multipleMode = host.isMultipleMode();
        super.detectAndSendChanges();
    }
}
