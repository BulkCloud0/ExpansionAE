package dev.bulkcloud.expansionae.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud.expansionae.ExpansionNetwork;
import dev.bulkcloud.expansionae.RenamerContainer;
import dev.bulkcloud.expansionae.RenamerNameUpdate;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class RenamerScreen extends ContainerScreen<RenamerContainer> {
    private TextFieldWidget nameField;

    public RenamerScreen(RenamerContainer container, PlayerInventory inventory, ITextComponent title) {
        super(container, inventory, title);
        this.xSize = 220;
        this.ySize = 92;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new TextFieldWidget(font, guiLeft + 12, guiTop + 32, 196, 20,
                new StringTextComponent("Name"));
        nameField.setMaxStringLength(64);
        nameField.setText(container.getCurrentName());
        addButton(nameField);
        setFocusedDefault(nameField);

        addButton(new Button(guiLeft + 65, guiTop + 60, 90, 20,
                new StringTextComponent("Rename"), button -> submit()));
    }

    private void submit() {
        ExpansionNetwork.sendToServer(new RenamerNameUpdate(nameField == null ? "" : nameField.getText()));
        if (minecraft != null && minecraft.player != null) minecraft.player.closeScreen();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks,
            int mouseX, int mouseY) {
        fill(matrixStack, guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF202020);
        fill(matrixStack, guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + 23, 0xFF303030);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.drawString(matrixStack, "AE2 Renamer", 8, 8, 0xFFFFFF);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }
}
