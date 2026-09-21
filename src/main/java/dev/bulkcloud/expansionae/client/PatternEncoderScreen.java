package dev.bulkcloud.expansionae.client;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud.expansionae.PatternEncoderContainer;
import dev.bulkcloud.expansionae.RoutingBuffer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class PatternEncoderScreen extends AEBaseScreen<PatternEncoderContainer> {
    private final Button[] faces = new Button[9];
    private Button convert;
    public PatternEncoderScreen(PatternEncoderContainer container, PlayerInventory inv, ITextComponent title, ScreenStyle style) {
        super(container, inv, title, style);
    }
    @Override protected void init() {
        super.init();
        convert = addButton(new Button(guiLeft + 40, guiTop + 28, 140, 18,
                StringTextComponent.EMPTY, button -> container.convertPattern()));
        for (int i = 0; i < 9; i++) {
            final int input = i;
            faces[i] = addButton(new Button(guiLeft + 40, guiTop + 48 + 18 * i, 140, 18,
                    StringTextComponent.EMPTY, button -> container.cycleFace(input)));
        }
    }
    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        ICraftingPatternDetails details = container.details();
        if (convert != null) {
            convert.active = details != null && !details.isCraftable();
            convert.setMessage(new TranslationTextComponent(container.isAdvancedPattern()
                    ? "gui.expansionae.pattern_encoder.normal"
                    : "gui.expansionae.pattern_encoder.advanced"));
        }
        for (int i = 0; i < 9; i++) if (faces[i] != null) {
            int face = RoutingBuffer.face(container.pattern(), i);
            faces[i].setMessage(new TranslationTextComponent(face == -1 ? "gui.expansionae.face.auto"
                    : "gui.expansionae.face." + Direction.byIndex(face).getString()));
            faces[i].active = details != null && !details.isCraftable() && i < details.getInputs().size()
                    && details.getInputs().get(i) != null;
        }
    }
    @Override public void drawBG(MatrixStack pose, int x, int y, int mouseX, int mouseY, float partial) {
        fill(pose, x, y, x + xSize, y + ySize, 0xff373737);
        fill(pose, x+2, y+2, x+xSize-2, y+ySize-2, 0xffc6c6c6);
        for (Slot slot : container.inventorySlots) {
            fill(pose, x+slot.xPos-1, y+slot.yPos-1, x+slot.xPos+17, y+slot.yPos+17, 0xffeeeeee);
            fill(pose, x+slot.xPos, y+slot.yPos, x+slot.xPos+16, y+slot.yPos+16, 0xff8b8b8b);
        }
    }
    @Override public void drawFG(MatrixStack pose, int x, int y, int mouseX, int mouseY) {
        super.drawFG(pose,x,y,mouseX,mouseY);
        ICraftingPatternDetails details = container.details();
        if (details != null) for (int i = 0; i < Math.min(9, details.getInputs().size()); i++) {
            if (details.getInputs().get(i) != null) drawItem(18, 49 + 18*i, details.getInputs().get(i).createItemStack());
        }
    }
}
