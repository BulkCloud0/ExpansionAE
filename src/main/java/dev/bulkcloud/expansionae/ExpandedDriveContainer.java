package dev.bulkcloud.expansionae;

import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.RestrictedInputSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;

public final class ExpandedDriveContainer extends AEBaseContainer {
    public static final ContainerType<ExpandedDriveContainer> TYPE = ContainerTypeBuilder
            .create(ExpandedDriveContainer::new, ExpandedDriveTile.class)
            .build("expansionae_ex_drive");

    public ExpandedDriveContainer(int id, PlayerInventory player, ExpandedDriveTile drive) {
        super(TYPE, id, player, drive);

        for (int i = 0; i < drive.getCellCount(); i++) {
            addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.STORAGE_CELLS,
                    drive.getInternalInventory(), i), SlotSemantic.STORAGE_CELL);
        }

        createPlayerInventorySlots(player);
    }
}
