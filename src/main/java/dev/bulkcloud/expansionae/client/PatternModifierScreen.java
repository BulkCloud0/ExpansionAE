package dev.bulkcloud.expansionae.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud.expansionae.PatternModifierContainer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class PatternModifierScreen extends AEBaseScreen<PatternModifierContainer> {
    public PatternModifierScreen(PatternModifierContainer container, PlayerInventory inv,
            ITextComponent title, ScreenStyle style) {
        super(container, inv, title, style);
    }

    @Override
    protected void init() {
        super.init();

        int y = guiTop + 91;
        addButton(new Button(guiLeft + 8, y, 28, 18, new TranslationTextComponent("gui.expansionae.pattern_modifier.x2"),
                b -> container.multiply2()));
        addButton(new Button(guiLeft + 38, y, 32, 18, new TranslationTextComponent("gui.expansionae.pattern_modifier.x10"),
                b -> container.multiply10()));
        addButton(new Button(guiLeft + 72, y, 28, 18, new TranslationTextComponent("gui.expansionae.pattern_modifier.div2"),
                b -> container.divide2()));
        addButton(new Button(guiLeft + 102, y, 32, 18, new TranslationTextComponent("gui.expansionae.pattern_modifier.div10"),
                b -> container.divide10()));
        addButton(new Button(guiLeft + 136, y, 48, 18, new TranslationTextComponent("gui.expansionae.pattern_modifier.clear"),
                b -> container.clear()));

        addButton(new Button(guiLeft + 8, guiTop + 119, 76, 18,
                new TranslationTextComponent("gui.expansionae.pattern_modifier.replace"),
                b -> container.replace()));
        addButton(new Button(guiLeft + 108, guiTop + 119, 76, 18,
                new TranslationTextComponent("gui.expansionae.pattern_modifier.clone"),
                b -> container.cloneTarget()));
    }

    @Override
    public void drawBG(MatrixStack pose, int x, int y, int mouseX, int mouseY, float partial) {
        fill(pose, x, y, x + xSize, y + ySize, 0xff373737);
        fill(pose, x + 2, y + 2, x + xSize - 2, y + ySize - 2, 0xffc6c6c6);
        for (Slot slot : container.inventorySlots) {
            fill(pose, x + slot.xPos - 1, y + slot.yPos - 1,
                    x + slot.xPos + 17, y + slot.yPos + 17, 0xffeeeeee);
            fill(pose, x + slot.xPos, y + slot.yPos,
                    x + slot.xPos + 16, y + slot.yPos + 16, 0xff8b8b8b);
        }
    }

    @Override
    public void drawFG(MatrixStack pose, int x, int y, int mouseX, int mouseY) {
        super.drawFG(pose, x, y, mouseX, mouseY);
        font.drawString(pose, new TranslationTextComponent("gui.expansionae.pattern_modifier.bulk").getString(),
                8, 18, 0x404040);
        font.drawString(pose, new TranslationTextComponent("gui.expansionae.pattern_modifier.replace_area").getString(),
                8, 113, 0x404040);
        font.drawString(pose, new TranslationTextComponent("gui.expansionae.pattern_modifier.clone_area").getString(),
                108, 113, 0x404040);
    }
}
