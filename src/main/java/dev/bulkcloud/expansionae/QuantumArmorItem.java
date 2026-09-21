package dev.bulkcloud.expansionae;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nullable;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.features.ILocatable;
import appeng.api.features.INetworkEncodable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.ContainerLocator;
import appeng.core.Api;
import appeng.container.ContainerOpener;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;

public final class QuantumArmorItem extends ArmorItem implements IAEItemPowerStorage, IGuiItem, INetworkEncodable {
    private static final String POWER = "ExpansionAEQuantumPower";
    private static final String UPGRADES = "ExpansionAEQuantumUpgrades";
    private static final String DISABLED_UPGRADES = "ExpansionAEQuantumDisabledUpgrades";
    private static final String ENCRYPTION_KEY = "encryptionKey";
    private static final UUID REACH_MODIFIER = UUID.fromString("2083e57d-4744-4d2b-bad5-5517c13a1734");
    private final double capacity;

    public QuantumArmorItem(EquipmentSlotType slot, double capacity, Item.Properties properties) {
        super(QuantumArmorMaterial.QUANTUM_ALLOY, slot, properties.maxStackSize(1).rarity(Rarity.EPIC));
        this.capacity = capacity;
    }

    @Override
    public IGuiItemObject getGuiObject(ItemStack stack, int playerInventorySlot, World world, @Nullable BlockPos pos) {
        if (slot == EquipmentSlotType.HEAD && isUpgradeEnabled(stack, QuantumUpgradeType.WORKBENCH)) {
            return new PortableWorkbenchGuiObject(stack, world.isRemote);
        }
        return null;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, net.minecraft.entity.player.PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (slot == EquipmentSlotType.HEAD && hasUpgrade(stack, QuantumUpgradeType.WORKBENCH)) {
            if (!world.isRemote) {
                ContainerOpener.openContainer(PortableWorkbenchContainer.TYPE, player, ContainerLocator.forHand(player, hand));
            }
            return new ActionResult<ItemStack>(ActionResultType.func_233537_a_(world.isRemote), stack);
        }
        return new ActionResult<ItemStack>(ActionResultType.PASS, stack);
    }

    public boolean canInstall(QuantumUpgradeType type) {
        return type.supports(this.slot);
    }

    @Override
    public String getEncryptionKey(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag == null ? "" : tag.getString(ENCRYPTION_KEY);
    }

    @Override
    public void setEncryptionKey(ItemStack stack, String encKey, String name) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (encKey == null || encKey.isEmpty()) tag.remove(ENCRYPTION_KEY);
        else tag.putString(ENCRYPTION_KEY, encKey);
    }

    @Nullable
    public IGrid getLinkedGrid(ItemStack stack) {
        String key = getEncryptionKey(stack);
        if (key.isEmpty()) return null;

        try {
            ILocatable locatable = Api.instance().registries().locatable().getLocatableBy(Long.parseLong(key));
            if (!(locatable instanceof IActionHost)) return null;
            IGridNode node = ((IActionHost) locatable).getActionableNode();
            return node == null ? null : node.getGrid();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public boolean isLinked(ItemStack stack) {
        return !getEncryptionKey(stack).isEmpty();
    }

    public boolean hasUpgrade(ItemStack stack, QuantumUpgradeType type) {
        CompoundNBT tag = stack.getChildTag(UPGRADES);
        return tag != null && tag.getBoolean(type.id());
    }

    public boolean installUpgrade(ItemStack stack, QuantumUpgradeType type) {
        if (!canInstall(type) || hasUpgrade(stack, type)) return false;
        stack.getOrCreateChildTag(UPGRADES).putBoolean(type.id(), true);
        CompoundNBT disabled = stack.getChildTag(DISABLED_UPGRADES);
        if (disabled != null) disabled.remove(type.id());
        return true;
    }

    public boolean removeUpgrade(ItemStack stack, QuantumUpgradeType type) {
        if (!hasUpgrade(stack, type)) return false;
        CompoundNBT upgrades = stack.getChildTag(UPGRADES);
        if (upgrades != null) upgrades.remove(type.id());
        CompoundNBT disabled = stack.getChildTag(DISABLED_UPGRADES);
        if (disabled != null) disabled.remove(type.id());
        return true;
    }

    public boolean isUpgradeEnabled(ItemStack stack, QuantumUpgradeType type) {
        if (!hasUpgrade(stack, type)) return false;
        CompoundNBT disabled = stack.getChildTag(DISABLED_UPGRADES);
        return disabled == null || !disabled.getBoolean(type.id());
    }

    public void toggleUpgrade(ItemStack stack, QuantumUpgradeType type) {
        if (!hasUpgrade(stack, type)) return;
        CompoundNBT disabled = stack.getOrCreateChildTag(DISABLED_UPGRADES);
        boolean enabled = isUpgradeEnabled(stack, type);
        if (enabled) disabled.putBoolean(type.id(), true);
        else disabled.remove(type.id());
    }

    public boolean isUpgradeUsable(ItemStack stack, QuantumUpgradeType type) {
        return isUpgradeEnabled(stack, type) && getAECurrentPower(stack) + 0.0001 >= type.cost();
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(equipmentSlot, stack);
        if (equipmentSlot != EquipmentSlotType.LEGS || !isUpgradeUsable(stack, QuantumUpgradeType.REACH)) {
            return base;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(base);
        builder.put(ForgeMod.REACH_DISTANCE.get(), new AttributeModifier(
                REACH_MODIFIER, "expansionae_quantum_reach", 1.0D, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

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
        lines.add(new StringTextComponent(isLinked(stack) ? "ME network: Linked" : "ME network: Unlinked"));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return slot == EquipmentSlotType.LEGS
                ? "minecraft:textures/models/armor/netherite_layer_2.png"
                : "minecraft:textures/models/armor/netherite_layer_1.png";
    }
}
