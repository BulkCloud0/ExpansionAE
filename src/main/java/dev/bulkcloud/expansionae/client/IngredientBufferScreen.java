package dev.bulkcloud.expansionae.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.IngredientBufferContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class IngredientBufferScreen extends AEBaseScreen<IngredientBufferContainer> {
    public IngredientBufferScreen(IngredientBufferContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
    }
}
