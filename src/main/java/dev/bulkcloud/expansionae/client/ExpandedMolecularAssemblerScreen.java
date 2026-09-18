package dev.bulkcloud.expansionae.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import dev.bulkcloud.expansionae.ExpandedMolecularAssemblerContainer;
import dev.bulkcloud.expansionae.ExpandedMolecularAssemblerTile;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class ExpandedMolecularAssemblerScreen
        extends UpgradeableScreen<ExpandedMolecularAssemblerContainer> {
    private final ProgressBar progress;
    private Button pageButton;

    public ExpandedMolecularAssemblerScreen(ExpandedMolecularAssemblerContainer container,
            PlayerInventory player, ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        progress = new ProgressBar(container, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progress);
    }

    @Override
    protected void init() {
        super.init();
        addButton(new Button(guiLeft + 8, guiTop + 18, 18, 18,
                new StringTextComponent("<"), b -> container.previousPage()));
        pageButton = addButton(new Button(guiLeft + 28, guiTop + 18, 70, 18,
                pageText(), b -> container.nextPage()));
    }

    private ITextComponent pageText() {
        return new TranslationTextComponent("gui.expansionae.ex_assembler.page",
                container.page + 1, ExpandedMolecularAssemblerTile.LANES);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        progress.setFullMsg(new StringTextComponent(container.getCurrentProgress() + "%"));
        if (pageButton != null) pageButton.setMessage(pageText());
    }
}
