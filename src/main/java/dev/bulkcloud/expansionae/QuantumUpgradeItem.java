package dev.bulkcloud.expansionae;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public final class QuantumUpgradeItem extends Item {
    private static final EquipmentSlotType[] ARMOR_ORDER = {
            EquipmentSlotType.HEAD, EquipmentSlotType.CHEST,
            EquipmentSlotType.LEGS, EquipmentSlotType.FEET
    };
    private final QuantumUpgradeType type;

    public QuantumUpgradeItem(QuantumUpgradeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public QuantumUpgradeType getUpgradeType() { return type; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack card = player.getHeldItem(hand);
        for (EquipmentSlotType slot : ARMOR_ORDER) {
            ItemStack armorStack = player.getItemStackFromSlot(slot);
            if (!(armorStack.getItem() instanceof QuantumArmorItem)) continue;
            QuantumArmorItem armor = (QuantumArmorItem) armorStack.getItem();
            if (!type.supports(slot) || armor.hasUpgrade(armorStack, type)) continue;

            if (!world.isRemote && armor.installUpgrade(armorStack, type)) {
                if (!player.abilities.isCreativeMode) card.shrink(1);
                player.sendStatusMessage(new StringTextComponent(
                        "Installed " + type.id().replace('_', ' ') + " upgrade"), true);
            }
            return ActionResult.resultSuccess(card);
        }
        return ActionResult.resultPass(card);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag) {
        lines.add(new StringTextComponent("Right-click while wearing compatible Quantum Armor to install."));
        if (!type.isCoreFunctional()) {
            lines.add(new StringTextComponent("Network/config integration is still being backported."));
        }
    }
}
