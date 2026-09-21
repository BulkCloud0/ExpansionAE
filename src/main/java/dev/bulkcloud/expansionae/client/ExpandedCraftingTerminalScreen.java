package dev.bulkcloud.expansionae.client;

import appeng.api.config.ActionItems;
import appeng.client.gui.me.items.ItemTerminalScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import dev.bulkcloud.expansionae.ExpandedCraftingMode;
import dev.bulkcloud.expansionae.ExpandedCraftingTerminalContainer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class ExpandedCraftingTerminalScreen
        extends ItemTerminalScreen<ExpandedCraftingTerminalContainer> {
    private Button modeButton;
    private Button previousRecipe;
    private Button nextRecipe;
    private TextFieldWidget anvilName;
    private boolean updatingName;

    public ExpandedCraftingTerminalScreen(ExpandedCraftingTerminalContainer container,
            PlayerInventory playerInventory, ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);
        ActionButton clear = new ActionButton(ActionItems.STASH, btn -> container.clearCraftingGrid());
        clear.setHalfSize(true);
        widgets.add("clearCraftingGrid", clear);
    }

    @Override
    protected void init() {
        super.init();

        modeButton = addButton(new Button(guiLeft + 112, guiTop + 3, 76, 16,
                modeText(), b -> container.cycleMode()));

        previousRecipe = addButton(new Button(guiLeft + 112, guiTop + 21, 20, 16,
                new StringTextComponent("<"), b -> container.previousStoneRecipe()));
        nextRecipe = addButton(new Button(guiLeft + 168, guiTop + 21, 20, 16,
                new StringTextComponent(">"), b -> container.nextStoneRecipe()));

        anvilName = addButton(new TextFieldWidget(font, guiLeft + 112, guiTop + 21, 76, 16,
                new StringTextComponent("Anvil name")));
        anvilName.setMaxStringLength(35);
        anvilName.setText(container.anvilName);
        anvilName.setResponder(value -> {
            if (!updatingName) container.changeAnvilName(value);
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (anvilName != null) anvilName.tick();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (modeButton != null) modeButton.setMessage(modeText());

        boolean stone = container.mode == ExpandedCraftingMode.STONECUTTING;
        if (previousRecipe != null) {
            previousRecipe.visible = stone;
            previousRecipe.active = stone && container.stonecutRecipeCount > 1;
        }
        if (nextRecipe != null) {
            nextRecipe.visible = stone;
            nextRecipe.active = stone && container.stonecutRecipeCount > 1;
        }

        boolean anvil = container.mode == ExpandedCraftingMode.ANVIL;
        if (anvilName != null) {
            anvilName.setVisible(anvil);
            if (anvil && !anvilName.isFocused() && !anvilName.getText().equals(container.anvilName)) {
                updatingName = true;
                anvilName.setText(container.anvilName);
                updatingName = false;
            }
        }
    }

    private ITextComponent modeText() {
        String label;
        switch (container.mode) {
            case SMITHING: label = "Smithing"; break;
            case STONECUTTING:
                label = "Stone " + (container.stonecutRecipeCount <= 0 ? "0/0"
                        : (container.stonecutRecipe + 1) + "/" + container.stonecutRecipeCount);
                break;
            case ANVIL:
                label = "Anvil " + (container.anvilCost > 0 ? container.anvilCost + "L" : "");
                break;
            case CRAFTING:
            default: label = "Crafting";
        }
        return new StringTextComponent(label);
    }
}
