package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.SlotSemantic;
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

    public QuantumCrafterContainer(int id, PlayerInventory player, QuantumCrafterTile host) {
        super(TYPE, id, player, host);
        this.host = host;
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
    protected boolean supportCapacity() { return false; }

    @Override
    public int availableUpgrades() { return 5; }
}
