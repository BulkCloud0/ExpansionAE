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

public final class AdvancedIOBusContainer extends UpgradeableContainer {
    public static final ContainerType<AdvancedIOBusContainer> TYPE = ContainerTypeBuilder
            .create(AdvancedIOBusContainer::new, AdvancedIOBus.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_advanced_io_bus");

    private final AdvancedIOBus host;

    @GuiSync(7)
    public boolean regulateStock;

    public AdvancedIOBusContainer(int id, PlayerInventory player, AdvancedIOBus host) {
        super(TYPE, id, player, host);
        this.host = host;
        registerClientAction("toggleRegulate", () -> host.setRegulateStock(!host.isRegulateStock()));
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

    public void toggleRegulate() {
        if (isRemote()) sendClientAction("toggleRegulate");
        else host.setRegulateStock(!host.isRegulateStock());
    }

    public boolean isRegulateStock() {
        return regulateStock;
    }

    @Override
    public int availableUpgrades() {
        return 8;
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) regulateStock = host.isRegulateStock();
        super.detectAndSendChanges();
    }
}
