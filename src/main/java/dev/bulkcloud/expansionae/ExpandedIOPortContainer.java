package dev.bulkcloud.expansionae;

import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.OutputSlot;
import appeng.container.slot.RestrictedInputSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class ExpandedIOPortContainer extends UpgradeableContainer {
    public static final ContainerType<ExpandedIOPortContainer> TYPE =
            ContainerTypeBuilder
                    .create(ExpandedIOPortContainer::new, ExpandedIOPortTile.class)
                    .requirePermission(SecurityPermissions.BUILD)
                    .build("expansionae_ex_io_port");

    @GuiSync(2)
    public FullnessMode fullnessMode = FullnessMode.EMPTY;
    @GuiSync(3)
    public OperationMode operationMode = OperationMode.EMPTY;

    public ExpandedIOPortContainer(int id, PlayerInventory player, ExpandedIOPortTile host) {
        super(TYPE, id, player, host);
    }

    @Override
    protected void setupConfig() {
        IItemHandler cells = getUpgradeable().getInventoryByName("cells");
        for (int i = 0; i < 6; i++) {
            addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.STORAGE_CELLS, cells, i),
                    SlotSemantic.MACHINE_INPUT);
        }
        for (int i = 0; i < 6; i++) {
            addSlot(new OutputSlot(cells, 6 + i,
                    RestrictedInputSlot.PlacableItemType.STORAGE_CELLS.icon),
                    SlotSemantic.MACHINE_OUTPUT);
        }
        setupUpgrades();
    }

    @Override protected boolean supportCapacity() { return false; }
    @Override public int availableUpgrades() { return 5; }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (isServer()) {
            operationMode = (OperationMode) getUpgradeable().getConfigManager()
                    .getSetting(Settings.OPERATION_MODE);
            fullnessMode = (FullnessMode) getUpgradeable().getConfigManager()
                    .getSetting(Settings.FULLNESS_MODE);
            setRedStoneMode((RedstoneMode) getUpgradeable().getConfigManager()
                    .getSetting(Settings.REDSTONE_CONTROLLED));
        }
        standardDetectAndSendChanges();
    }

    public FullnessMode getFullMode() { return fullnessMode; }
    public OperationMode getOperationMode() { return operationMode; }
}
