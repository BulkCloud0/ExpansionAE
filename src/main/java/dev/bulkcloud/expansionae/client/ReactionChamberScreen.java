package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import dev.bulkcloud.expansionae.ReactionChamberContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.Direction;
import net.minecraft.client.gui.widget.button.Button;

public final class ReactionChamberScreen extends UpgradeableScreen<ReactionChamberContainer> {
    private final ProgressBar progress;
    private final Button[] outputButtons = new Button[Direction.values().length];

    public ReactionChamberScreen(ReactionChamberContainer container, PlayerInventory player,
            ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        progress = new ProgressBar(container, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progress);
    }

    @Override
    protected void init() {
        super.init();
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            final Direction direction = directions[i];
            int x = guiLeft + 8 + (i % 3) * 28;
            int y = guiTop + 18 + (i / 3) * 18;
            outputButtons[i] = addButton(new Button(x, y, 26, 16,
                    outputLabel(direction), b -> container.toggleOutput(direction)));
        }
    }

    private ITextComponent outputLabel(Direction direction) {
        String id = direction.getString().substring(0, 1).toUpperCase();
        return new StringTextComponent((container.isOutputEnabled(direction) ? "*" : "-") + id);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int pct = container.getCurrentProgress() * 100 / Math.max(1, container.getMaxProgress());
        progress.setFullMsg(new StringTextComponent(pct + "%"));
        for (Direction direction : Direction.values()) {
            Button button = outputButtons[direction.getIndex()];
            if (button != null) button.setMessage(outputLabel(direction));
        }
    }
}
