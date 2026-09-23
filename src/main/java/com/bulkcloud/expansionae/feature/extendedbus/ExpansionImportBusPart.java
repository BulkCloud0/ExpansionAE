package com.bulkcloud.expansionae.feature.extendedbus;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.bulkcloud.expansionae.ExpansionAE;

import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.ImportBusPart;

public final class ExpansionImportBusPart extends ImportBusPart {
    public static final int UPGRADE_SLOTS = 4;
    public static final ResourceLocation MODEL_BASE =
            new ResourceLocation(ExpansionAE.MOD_ID, "part/extended_import_bus_base");

    private static final ResourceLocation MODEL_OFF =
            new ResourceLocation("appliedenergistics2", "part/import_bus_off");
    private static final ResourceLocation MODEL_ON =
            new ResourceLocation("appliedenergistics2", "part/import_bus_on");
    private static final ResourceLocation MODEL_HAS_CHANNEL =
            new ResourceLocation("appliedenergistics2", "part/import_bus_has_channel");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_HAS_CHANNEL);

    public ExpansionImportBusPart(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return UPGRADE_SLOTS;
    }

    @Override
    protected int calculateItemsToSend() {
        return ExtendedBusThroughput.scaleBudget(super.calculateItemsToSend());
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }
}
