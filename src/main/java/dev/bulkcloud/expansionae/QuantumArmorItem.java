package dev.bulkcloud.expansionae;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public final class QuantumArmorItem extends ArmorItem implements IAEItemPowerStorage {
    private static final String POWER = "ExpansionAEQuantumPower";
    private static final String UPGRADES = "ExpansionAEQuantumUpgrades";
    private final double capacity;

    public QuantumArmorItem(EquipmentSlotType slot, double capacity, Item.Properties properties) {
        super(QuantumArmorMaterial.QUANTUM_ALLOY, slot, properties.maxStackSize(1).rarity(Rarity.EPIC));
        this.capacity = capacity;
    }

    public boolean canInstall(QuantumUpgradeType type) {
        return type.supports(this.slot);
    }

    public boolean hasUpgrade(ItemStack stack, QuantumUpgradeType type) {
        CompoundNBT tag = stack.getChildTag(UPGRADES);
        return tag != null && tag.getBoolean(type.id());
    }

    public boolean installUpgrade(ItemStack stack, QuantumUpgradeType type) {
        if (!canInstall(type) || hasUpgrade(stack, type)) return false;
        stack.getOrCreateChildTag(UPGRADES).putBoolean(type.id(), true);
        return true;
    }

    public boolean isUpgradeUsable(ItemStack stack, QuantumUpgradeType type) {
        return hasUpgrade(stack, type) && getAECurrentPower(stack) + 0.0001 >= type.cost();
    }

    public boolean consumeUpgradeEnergy(ItemStack stack, QuantumUpgradeType type) {
        if (!isUpgradeUsable(stack, type)) return false;
        if (type.cost() > 0) extractAEPower(stack, type.cost(), Actionable.MODULATE);
        return true;
    }

    @Override
    public double injectAEPower(ItemStack stack, double amount, Actionable mode) {
        double current = getAECurrentPower(stack);
        double accepted = Math.min(Math.max(0, amount), capacity - current);
        if (mode == Actionable.MODULATE && accepted > 0) {
            stack.getOrCreateTag().putDouble(POWER, current + accepted);
        }
        return Math.max(0, amount - accepted);
    }

    @Override
    public double extractAEPower(ItemStack stack, double amount, Actionable mode) {
        double current = getAECurrentPower(stack);
        double extracted = Math.min(Math.max(0, amount), current);
        if (mode == Actionable.MODULATE) {
            double remaining = current - extracted;
            if (remaining <= 0.0001) stack.getOrCreateTag().remove(POWER);
            else stack.getOrCreateTag().putDouble(POWER, remaining);
        }
        return extracted;
    }

    @Override public double getAEMaxPower(ItemStack stack) { return capacity; }
    @Override public double getAECurrentPower(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getDouble(POWER) : 0;
    }
    @Override public AccessRestriction getPowerFlow(ItemStack stack) { return AccessRestriction.WRITE; }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount,
            @Nullable T entity, Consumer<T> onBroken) {
        extractAEPower(stack, amount * 100.0, Actionable.MODULATE);
        return 0;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag) {
        super.addInformation(stack, world, lines, flag);
        lines.add(new StringTextComponent(String.format("AE: %,.0f / %,.0f", getAECurrentPower(stack), capacity)));
        int installed = 0;
        for (QuantumUpgradeType type : QuantumUpgradeType.values()) {
            if (hasUpgrade(stack, type)) installed++;
        }
        lines.add(new StringTextComponent("Quantum upgrades: " + installed));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return slot == EquipmentSlotType.LEGS
                ? "minecraft:textures/models/armor/netherite_layer_2.png"
                : "minecraft:textures/models/armor/netherite_layer_1.png";
    }
}
