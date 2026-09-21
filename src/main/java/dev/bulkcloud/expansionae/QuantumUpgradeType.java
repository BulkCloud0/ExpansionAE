package dev.bulkcloud.expansionae;

import net.minecraft.inventory.EquipmentSlotType;

/**
 * 1.16.5 representation of AdvancedAE's Quantum Armor upgrade catalog.
 * "coreFunctional" marks upgrades whose gameplay behavior has a native 1.16.5
 * implementation; network/menu dependent upgrades remain installable so NBT
 * and recipes stay forward-compatible with the rest of the port.
 */
public enum QuantumUpgradeType {
    WALK_SPEED("walk_speed", EquipmentSlotType.LEGS, 10, true),
    SPRINT_SPEED("sprint_speed", EquipmentSlotType.LEGS, 10, true),
    STEP_ASSIST("step_assist", EquipmentSlotType.FEET, 5, true),
    JUMP_HEIGHT("jump_height", EquipmentSlotType.FEET, 10, true),
    LAVA_IMMUNITY("lava_immunity", EquipmentSlotType.CHEST, 10, true),
    FLIGHT("flight", EquipmentSlotType.CHEST, 10, true),
    WATER_BREATHING("water_breathing", EquipmentSlotType.HEAD, 10, true),
    AUTO_FEED("auto_feed", EquipmentSlotType.HEAD, 5, true),
    AUTO_STOCK("auto_stock", EquipmentSlotType.HEAD, 5, false),
    MAGNET("magnet", EquipmentSlotType.HEAD, 5, true),
    HP_BUFFER("hp_buffer", EquipmentSlotType.CHEST, 10, true),
    EVASION("evasion", EquipmentSlotType.FEET, 10, true),
    REGENERATION("regeneration", EquipmentSlotType.CHEST, 10, true),
    STRENGTH("strength", EquipmentSlotType.CHEST, 10, true),
    ATTACK_SPEED("attack_speed", EquipmentSlotType.CHEST, 10, true),
    LUCK("luck", EquipmentSlotType.HEAD, 10, true),
    REACH("reach", EquipmentSlotType.LEGS, 10, false),
    SWIM_SPEED("swim_speed", EquipmentSlotType.LEGS, 5, true),
    NIGHT_VISION("night_vision", EquipmentSlotType.HEAD, 10, true),
    FLIGHT_DRIFT("flight_drift", EquipmentSlotType.FEET, 10, true),
    CHARGING("charging", null, 0, false),
    WORKBENCH("portable_workbench", EquipmentSlotType.HEAD, 0, true),
    PICK_CRAFT("pick_craft", EquipmentSlotType.CHEST, 1000, false);

    private final String id;
    private final EquipmentSlotType slot;
    private final double cost;
    private final boolean coreFunctional;

    QuantumUpgradeType(String id, EquipmentSlotType slot, double cost, boolean coreFunctional) {
        this.id = id;
        this.slot = slot;
        this.cost = cost;
        this.coreFunctional = coreFunctional;
    }

    public String id() { return id; }
    public double cost() { return cost; }
    public boolean isCoreFunctional() { return coreFunctional; }
    public boolean supports(EquipmentSlotType candidate) { return slot == null || slot == candidate; }
}
