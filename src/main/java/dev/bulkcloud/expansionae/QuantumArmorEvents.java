package dev.bulkcloud.expansionae;

import java.util.List;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

@Mod.EventBusSubscriber(modid = ExpansionAE.ID)
public final class QuantumArmorEvents {
    private static final String FLIGHT_GRANTED = "ExpansionAEQuantumFlight";
    private static final String STEP_GRANTED = "ExpansionAEQuantumStep";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        PlayerEntity player = event.player;

        ItemStack helmet = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
        ItemStack chest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        ItemStack legs = player.getItemStackFromSlot(EquipmentSlotType.LEGS);
        ItemStack boots = player.getItemStackFromSlot(EquipmentSlotType.FEET);

        if (player.world.getGameTime() % 20 == 0) {
            buff(helmet, QuantumUpgradeType.WATER_BREATHING, player, Effects.WATER_BREATHING, 0);
            buff(helmet, QuantumUpgradeType.NIGHT_VISION, player, Effects.NIGHT_VISION, 0);
            buff(helmet, QuantumUpgradeType.LUCK, player, Effects.LUCK, 0);
            autoFeed(helmet, player);

            buff(chest, QuantumUpgradeType.LAVA_IMMUNITY, player, Effects.FIRE_RESISTANCE, 0);
            buff(chest, QuantumUpgradeType.REGENERATION, player, Effects.REGENERATION, 0);
            buff(chest, QuantumUpgradeType.STRENGTH, player, Effects.STRENGTH, 0);
            buff(chest, QuantumUpgradeType.ATTACK_SPEED, player, Effects.HASTE, 1);
            buff(chest, QuantumUpgradeType.HP_BUFFER, player, Effects.ABSORPTION, 1);

            if (player.isSprinting()) buff(legs, QuantumUpgradeType.SPRINT_SPEED, player, Effects.SPEED, 1);
            else buff(legs, QuantumUpgradeType.WALK_SPEED, player, Effects.SPEED, 0);
            if (player.isInWater()) buff(legs, QuantumUpgradeType.SWIM_SPEED, player, Effects.DOLPHINS_GRACE, 0);
            consumePassive(legs, QuantumUpgradeType.REACH);

            buff(boots, QuantumUpgradeType.JUMP_HEIGHT, player, Effects.JUMP_BOOST, 1);
        }

        recharge(helmet, player, false);
        recharge(chest, player, true);
        recharge(legs, player, false);
        recharge(boots, player, false);

        magnet(helmet, player);
        updateFlight(chest, player);
        updateStepAssist(boots, player);
        applyFlightDrift(boots, player);
    }

    private static QuantumArmorItem armor(ItemStack stack) {
        return stack.getItem() instanceof QuantumArmorItem ? (QuantumArmorItem) stack.getItem() : null;
    }

    private static void recharge(ItemStack stack, PlayerEntity player, boolean rechargeInventory) {
        QuantumArmorItem armor = armor(stack);
        if (armor == null || !armor.isUpgradeEnabled(stack, QuantumUpgradeType.CHARGING)) return;

        IGrid grid = armor.getLinkedGrid(stack);
        if (grid == null) return;

        IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        if (energy == null || !energy.isNetworkPowered()) return;

        double missing = armor.getAEMaxPower(stack) - armor.getAECurrentPower(stack);
        if (missing > 0.0001) {
            double request = Math.min(10000.0, missing);
            double extracted = energy.extractAEPower(request, Actionable.MODULATE, PowerMultiplier.CONFIG);
            if (extracted > 0) {
                double remainder = armor.injectAEPower(stack, extracted, Actionable.MODULATE);
                if (remainder > 0) energy.injectPower(remainder, Actionable.MODULATE);
            }
        }

        // AdvancedAE's chestplate charging upgrade also services carried Forge Energy
        // items. Keep the same behavior here without introducing an RF/FE dependency:
        // Forge's native IEnergyStorage capability is available in 1.16.5.
        if (rechargeInventory) {
            for (int slot = 0; slot < 36; slot++) {
                rechargeForgeEnergy(player.inventory.getStackInSlot(slot), energy);
            }
            rechargeForgeEnergy(player.getHeldItemOffhand(), energy);
        }
    }

    private static void rechargeForgeEnergy(ItemStack stack, IEnergyGrid energy) {
        if (stack.isEmpty()) return;

        stack.getCapability(CapabilityEnergy.ENERGY).ifPresent(storage -> {
            if (!storage.canReceive()) return;

            int missing = Math.max(0, storage.getMaxEnergyStored() - storage.getEnergyStored());
            if (missing <= 0) return;

            double request = Math.min(10000.0, missing);
            double extracted = energy.extractAEPower(request, Actionable.MODULATE, PowerMultiplier.CONFIG);
            if (extracted <= 0) return;

            int offered = (int) Math.min(Integer.MAX_VALUE, Math.floor(extracted));
            int accepted = offered <= 0 ? 0 : storage.receiveEnergy(offered, false);
            double remainder = extracted - accepted;
            if (remainder > 0) energy.injectPower(remainder, Actionable.MODULATE);
        });
    }

    private static void buff(ItemStack stack, QuantumUpgradeType type, PlayerEntity player, Effect effect, int amplifier) {
        QuantumArmorItem armor = armor(stack);
        if (armor == null || !armor.consumeUpgradeEnergy(stack, type)) return;
        player.addPotionEffect(new EffectInstance(effect, 40, amplifier, true, false));
    }

    private static void consumePassive(ItemStack stack, QuantumUpgradeType type) {
        QuantumArmorItem armor = armor(stack);
        if (armor != null && armor.isUpgradeUsable(stack, type)) {
            armor.consumeUpgradeEnergy(stack, type);
        }
    }

    private static void autoFeed(ItemStack stack, PlayerEntity player) {
        QuantumArmorItem armor = armor(stack);
        if (armor == null || !player.getFoodStats().needFood() || !armor.isUpgradeUsable(stack, QuantumUpgradeType.AUTO_FEED)) return;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack foodStack = player.inventory.getStackInSlot(i);
            Food food = foodStack.getItem().getFood();
            if (food == null) continue;
            player.getFoodStats().addStats(food.getHealing(), food.getSaturation());
            foodStack.shrink(1);
            armor.consumeUpgradeEnergy(stack, QuantumUpgradeType.AUTO_FEED);
            player.inventory.markDirty();
            return;
        }
    }

    private static void magnet(ItemStack stack, PlayerEntity player) {
        QuantumArmorItem armor = armor(stack);
        if (armor == null || !armor.isUpgradeUsable(stack, QuantumUpgradeType.MAGNET)) return;
        AxisAlignedBB box = player.getBoundingBox().grow(5.0);
        boolean moved = false;
        List<ItemEntity> items = player.world.getEntitiesWithinAABB(ItemEntity.class, box);
        for (ItemEntity item : items) {
            if (!item.isAlive()) continue;
            item.setPosition(player.getPosX(), player.getPosY(), player.getPosZ());
            moved = true;
        }
        List<ExperienceOrbEntity> xp = player.world.getEntitiesWithinAABB(ExperienceOrbEntity.class, box);
        for (ExperienceOrbEntity orb : xp) {
            if (!orb.isAlive()) continue;
            orb.setPosition(player.getPosX(), player.getPosY(), player.getPosZ());
            moved = true;
        }
        if (moved) armor.consumeUpgradeEnergy(stack, QuantumUpgradeType.MAGNET);
    }

    private static void updateFlight(ItemStack stack, PlayerEntity player) {
        QuantumArmorItem armor = armor(stack);
        boolean active = armor != null && armor.isUpgradeUsable(stack, QuantumUpgradeType.FLIGHT);
        boolean granted = player.getPersistentData().getBoolean(FLIGHT_GRANTED);

        if (active) {
            if (!player.abilities.allowFlying) {
                player.abilities.allowFlying = true;
                player.getPersistentData().putBoolean(FLIGHT_GRANTED, true);
                syncAbilities(player);
            }
            if (player.abilities.isFlying) {
                armor.consumeUpgradeEnergy(stack, QuantumUpgradeType.FLIGHT);
            }
        } else if (granted && !player.isCreative() && !player.isSpectator()) {
            player.abilities.allowFlying = false;
            player.abilities.isFlying = false;
            player.getPersistentData().remove(FLIGHT_GRANTED);
            syncAbilities(player);
        }
    }

    private static void updateStepAssist(ItemStack stack, PlayerEntity player) {
        QuantumArmorItem armor = armor(stack);
        boolean active = armor != null && armor.isUpgradeUsable(stack, QuantumUpgradeType.STEP_ASSIST);
        if (active) {
            player.stepHeight = 1.25F;
            player.getPersistentData().putBoolean(STEP_GRANTED, true);
            if (player.world.getGameTime() % 20 == 0) armor.consumeUpgradeEnergy(stack, QuantumUpgradeType.STEP_ASSIST);
        } else if (player.getPersistentData().getBoolean(STEP_GRANTED)) {
            player.stepHeight = 0.6F;
            player.getPersistentData().remove(STEP_GRANTED);
        }
    }

    private static void applyFlightDrift(ItemStack stack, PlayerEntity player) {
        QuantumArmorItem armor = armor(stack);
        if (armor == null || !player.abilities.isFlying
                || !armor.isUpgradeUsable(stack, QuantumUpgradeType.FLIGHT_DRIFT)) return;
        if (Math.abs(player.moveForward) < 0.01F && Math.abs(player.moveStrafing) < 0.01F) {
            player.setMotion(player.getMotion().mul(0.4, 1.0, 0.4));
            if (player.world.getGameTime() % 10 == 0) armor.consumeUpgradeEnergy(stack, QuantumUpgradeType.FLIGHT_DRIFT);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        ItemStack boots = player.getItemStackFromSlot(EquipmentSlotType.FEET);
        QuantumArmorItem armor = armor(boots);
        if (armor == null || !armor.isUpgradeUsable(boots, QuantumUpgradeType.EVASION)) return;
        if (player.getRNG().nextFloat() < 0.10F) {
            event.setAmount(0);
            armor.consumeUpgradeEnergy(boots, QuantumUpgradeType.EVASION);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        ItemStack boots = player.getItemStackFromSlot(EquipmentSlotType.FEET);
        QuantumArmorItem armor = armor(boots);
        if (armor == null || armor.getAECurrentPower(boots) < 10) return;
        armor.extractAEPower(boots, Math.max(10, event.getDistance() * 10), appeng.api.config.Actionable.MODULATE);
        event.setDistance(0);
        event.setDamageMultiplier(0);
    }

    private static void syncAbilities(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) ((ServerPlayerEntity) player).sendPlayerAbilities();
    }

    private QuantumArmorEvents() {}
}
