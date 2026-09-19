package dev.bulkcloud.expansionae;

import java.util.function.Supplier;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.LazyValue;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

public enum QuantumArmorMaterial implements IArmorMaterial {
    QUANTUM_ALLOY("quantum_alloy", 10, new int[] {4, 6, 9, 4}, 15,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 10.0F, 0.25F,
            () -> Ingredient.fromItems(ExpansionAE.QUANTUM_ALLOY.get()));

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] protection;
    private final int enchantability;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final LazyValue<Ingredient> repairIngredient;

    QuantumArmorMaterial(String name, int durabilityMultiplier, int[] protection, int enchantability,
            SoundEvent sound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protection = protection;
        this.enchantability = enchantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = new LazyValue<Ingredient>(repairIngredient);
    }

    @Override public int getDurability(EquipmentSlotType slot) {
        return BASE_DURABILITY[slot.getIndex()] * durabilityMultiplier;
    }
    @Override public int getDamageReductionAmount(EquipmentSlotType slot) {
        return protection[slot.getIndex()];
    }
    @Override public int getEnchantability() { return enchantability; }
    @Override public SoundEvent getSoundEvent() { return sound; }
    @Override public Ingredient getRepairMaterial() { return repairIngredient.getValue(); }
    @Override public String getName() { return ExpansionAE.ID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
