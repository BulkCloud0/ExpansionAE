package dev.bulkcloud.expansionae.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.ExpandedDriveContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class ExpandedDriveScreen extends AEBaseScreen<ExpandedDriveContainer> {
    public ExpandedDriveScreen(ExpandedDriveContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        widgets.addOpenPriorityButton();
    }
}
