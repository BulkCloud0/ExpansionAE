package com.bulkcloud.expansionae.feature.extendedbus;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.ResourceLocation;

import com.bulkcloud.expansionae.ExpansionAE;

import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.ExportBusPart;

public final class ExpansionExportBusPart extends ExportBusPart {
    public static final int UPGRADE_SLOTS = 4;
    public static final ResourceLocation MODEL_BASE =
            new ResourceLocation(ExpansionAE.MOD_ID, "part/extended_export_bus_base");

    private static final ResourceLocation MODEL_OFF =
            new ResourceLocation("appliedenergistics2", "part/export_bus_off");
    private static final ResourceLocation MODEL_ON =
            new ResourceLocation("appliedenergistics2", "part/export_bus_on");
    private static final ResourceLocation MODEL_HAS_CHANNEL =
            new ResourceLocation("appliedenergistics2", "part/export_bus_has_channel");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_HAS_CHANNEL);

    public ExpansionExportBusPart(ItemStack stack) {
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
    public boolean onPartActivate(PlayerEntity player, Hand hand, Vector3d pos) {
        if (!this.isRemote()) {
            ExtendedBusContainer.openExport(player, this);
        }
        return true;
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
