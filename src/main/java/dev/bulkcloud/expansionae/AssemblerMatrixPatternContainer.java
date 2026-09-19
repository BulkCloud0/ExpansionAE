package dev.bulkcloud.expansionae;

import appeng.api.config.SecurityPermissions;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.RestrictedInputSlot;
import appeng.container.slot.RestrictedInputSlot.PlacableItemType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class AssemblerMatrixPatternContainer extends AEBaseContainer {
    public static final ContainerType<AssemblerMatrixPatternContainer> TYPE = ContainerTypeBuilder
            .create(AssemblerMatrixPatternContainer::new, AssemblerMatrixTile.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_assembler_matrix_pattern");

    public AssemblerMatrixPatternContainer(int id, PlayerInventory player, AssemblerMatrixTile host) {
        super(TYPE, id, player, host);
        for (int i = 0; i < AssemblerMatrixTile.PATTERN_SLOTS; i++) {
            addSlot(new RestrictedInputSlot(PlacableItemType.ENCODED_CRAFTING_PATTERN,
                    host.getPatternInventory(), i), SlotSemantic.ENCODED_PATTERN);
        }
        createPlayerInventorySlots(player);
    }
}
