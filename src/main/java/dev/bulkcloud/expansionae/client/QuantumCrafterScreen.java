package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.QuantumCrafterContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class QuantumCrafterScreen extends UpgradeableScreen<QuantumCrafterContainer> {
    public QuantumCrafterScreen(QuantumCrafterContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
    }
}
