package dev.bulkcloud.expansionae;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.AppEngSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
public final class IngredientBufferContainer extends AEBaseContainer {
    public static final ContainerType<IngredientBufferContainer> TYPE=ContainerTypeBuilder
            .create(IngredientBufferContainer::new,IngredientBufferTile.class).build("expansionae_ingredient_buffer");
    private final IngredientBufferTile buffer;
    public IngredientBufferContainer(int id,PlayerInventory player,IngredientBufferTile buffer){
        super(TYPE,id,player,buffer);this.buffer=buffer;
        for(int i=0;i<IngredientBufferTile.ITEM_SLOTS;i++) addSlot(new AppEngSlot(buffer.getItemInventory(),i),SlotSemantic.STORAGE);
        createPlayerInventorySlots(player);
    }
    public IngredientBufferTile getBuffer(){return buffer;}
}
