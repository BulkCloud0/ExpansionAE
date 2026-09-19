package dev.bulkcloud.expansionae.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.CanerContainer;
import dev.bulkcloud.expansionae.CanerMode;
import dev.bulkcloud.expansionae.CanerModeUpdate;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class CanerScreen extends AEBaseScreen<CanerContainer> {
    private final Button modeButton;

    public CanerScreen(CanerContainer container, PlayerInventory player, ITextComponent title, ScreenStyle style) {
        super(container, player, title, style);
        modeButton = addToLeftToolbar(new Button(0, 0, 100, 20,
                new TranslationTextComponent("gui.expansionae.caner.fill"), button -> {
                    CanerMode next = container.getMode().next();
                    container.modeOrdinal = next.ordinal();
                    ExpansionNetwork.sendToServer(new CanerModeUpdate(container.getHostPos(), next));
                }));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        modeButton.setMessage(new TranslationTextComponent(
                container.getMode() == CanerMode.FILL
                        ? "gui.expansionae.caner.fill"
                        : "gui.expansionae.caner.empty"));
    }

    @Override
    public void drawFG(com.mojang.blaze3d.matrix.MatrixStack matrices, int offsetX, int offsetY, int mouseX, int mouseY) {
        font.drawText(matrices,
                new TranslationTextComponent("gui.expansionae.caner.fluid", container.getFluidAmount()),
                8, 68, 0x404040);
    }
}
