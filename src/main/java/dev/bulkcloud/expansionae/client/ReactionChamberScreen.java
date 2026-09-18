package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import dev.bulkcloud.expansionae.ReactionChamberContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class ReactionChamberScreen extends UpgradeableScreen<ReactionChamberContainer> {
    private final ProgressBar progress;

    public ReactionChamberScreen(ReactionChamberContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        progress = new ProgressBar(container, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progress);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int pct = container.getCurrentProgress() * 100 / Math.max(1, container.getMaxProgress());
        progress.setFullMsg(new StringTextComponent(pct + "%"));
    }
}
