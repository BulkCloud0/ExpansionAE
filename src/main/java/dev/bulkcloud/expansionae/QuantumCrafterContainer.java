package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.OutputSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.container.slot.RestrictedInputSlot.PlacableItemType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class QuantumCrafterContainer extends UpgradeableContainer {
    public static final ContainerType<QuantumCrafterContainer> TYPE = ContainerTypeBuilder
            .create(QuantumCrafterContainer::new, QuantumCrafterTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_quantum_crafter");

    private final QuantumCrafterTile host;

    @GuiSync(20) public int selectedPattern;
    @GuiSync(21) public int selectedInput;
    @GuiSync(22) public boolean selectedEnabled;
    @GuiSync(23) public long selectedMinimum;
    @GuiSync(24) public long selectedMaximum;
    @GuiSync(25) public boolean exportToME;
    @GuiSync(26) public int outputSideMask;

    public QuantumCrafterContainer(int id, PlayerInventory player, QuantumCrafterTile host) {
        super(TYPE, id, player, host);
        this.host = host;

        registerClientAction("selectPattern", Integer.class, value -> {
            selectedPattern = clamp(value, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
            selectedInput = 0;
        });
        registerClientAction("selectInput", Integer.class, value ->
                selectedInput = clamp(value, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1));
        registerClientAction("togglePattern", () -> host.togglePatternEnabled(selectedPattern));
        registerClientAction("adjustMinimum", Long.class,
                value -> host.adjustMinimumInputStock(selectedPattern, selectedInput, value));
        registerClientAction("adjustMaximum", Long.class,
                value -> host.adjustMaximumOutputStock(selectedPattern, value));
        registerClientAction("toggleExport", host::toggleExportToME);
        registerClientAction("toggleOutputSide", Integer.class, host::toggleOutputSide);
    }

    @Override
    protected void setupConfig() {
        IItemHandler patterns = host.getPatternInventory();
        for (int i = 0; i < QuantumCrafterTile.PATTERN_SLOTS; i++) {
            addSlot(new RestrictedInputSlot(PlacableItemType.ENCODED_CRAFTING_PATTERN, patterns, i),
                    SlotSemantic.ENCODED_PATTERN);
        }

        IItemHandler outputs = host.getOutputInventory();
        for (int i = 0; i < QuantumCrafterTile.OUTPUT_SLOTS; i++) {
            addSlot(new OutputSlot(outputs, i, null), SlotSemantic.MACHINE_OUTPUT);
        }

        setupUpgrades();
    }

    @Override
    public void detectAndSendChanges() {
        if (isServer()) {
            selectedPattern = clamp(selectedPattern, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
            selectedInput = clamp(selectedInput, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1);
            selectedEnabled = host.isPatternEnabled(selectedPattern);
            selectedMinimum = host.getMinimumInputStock(selectedPattern, selectedInput);
            selectedMaximum = host.getMaximumOutputStock(selectedPattern);
            exportToME = host.isExportToME();
            outputSideMask = host.getOutputSideMask();
        }
        super.detectAndSendChanges();
    }

    public void selectPattern(int value) {
        value = clamp(value, 0, QuantumCrafterTile.PATTERN_SLOTS - 1);
        if (isRemote()) sendClientAction("selectPattern", Integer.valueOf(value));
        selectedPattern = value;
        selectedInput = 0;
    }

    public void selectInput(int value) {
        value = clamp(value, 0, QuantumCrafterTile.INPUT_CONFIG_SLOTS - 1);
        if (isRemote()) sendClientAction("selectInput", Integer.valueOf(value));
        selectedInput = value;
    }

    public void togglePattern() {
        if (isRemote()) sendClientAction("togglePattern");
        else host.togglePatternEnabled(selectedPattern);
    }

    public void adjustMinimum(long delta) {
        if (isRemote()) sendClientAction("adjustMinimum", Long.valueOf(delta));
        else host.adjustMinimumInputStock(selectedPattern, selectedInput, delta);
    }

    public void adjustMaximum(long delta) {
        if (isRemote()) sendClientAction("adjustMaximum", Long.valueOf(delta));
        else host.adjustMaximumOutputStock(selectedPattern, delta);
    }

    public void toggleExport() {
        if (isRemote()) sendClientAction("toggleExport");
        else host.toggleExportToME();
    }

    public void toggleOutputSide(int ordinal) {
        if (isRemote()) sendClientAction("toggleOutputSide", Integer.valueOf(ordinal));
        else host.toggleOutputSide(ordinal);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected boolean supportCapacity() { return false; }

    @Override
    public int availableUpgrades() { return 5; }
}
