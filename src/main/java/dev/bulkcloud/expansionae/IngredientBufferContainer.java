package dev.bulkcloud.expansionae;

import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.AppEngSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class IngredientBufferContainer extends AEBaseContainer {
    public static final ContainerType<IngredientBufferContainer> TYPE = ContainerTypeBuilder
            .create(IngredientBufferContainer::new, IngredientBufferTile.class)
            .build("expansionae_ingredient_buffer");

    public IngredientBufferContainer(int id, PlayerInventory player, IngredientBufferTile host) {
        super(TYPE, id, player, host);
        IItemHandler inv = host.getBufferInventory();
        for (int i = 0; i < IngredientBufferTile.SLOTS; i++) {
            addSlot(new AppEngSlot(inv, i), SlotSemantic.STORAGE);
        }
        createPlayerInventorySlots(player);
    }
}
