package dev.bulkcloud.expansionae;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.CraftingTerminalPart;

/**
 * ExtendedAE crafting terminal adapted to the AE2 8.4 terminal stack.
 *
 * The base 3x3 crafting workflow is deliberately delegated to AE2's native
 * CraftingTerminalPart/CraftingTermContainer so recipe transfer, security,
 * storage extraction and recipe remainder handling retain 1.16.5 semantics.
 * Extra modern workbench modes are layered separately.
 */
public final class ExpandedCraftingTerminalPart extends CraftingTerminalPart {
    @PartModels
    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(ExpansionAE.ID, "part/ex_crafting_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(ExpansionAE.ID, "part/ex_crafting_terminal_on");

    public static final List<ResourceLocation> MODELS = Arrays.asList(MODEL_OFF, MODEL_ON);
    public static final IPartModel MODELS_OFF =
            new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public ExpandedCraftingTerminalPart(ItemStack stack) {
        super(stack);
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
