package dev.bulkcloud.expansionae;

import appeng.block.crafting.AbstractCraftingUnitBlock;
import appeng.block.crafting.AbstractCraftingUnitBlock.CraftingUnitType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.material.Material;

/**
 * AdvancedAE Quantum Computer block adapted to the AE2 8.4 crafting CPU block contract.
 */
public final class QuantumCraftingBlock extends AbstractCraftingUnitBlock<QuantumCraftingTile> {
    public enum Kind {
        UNIT, CORE, STORAGE_128M, STORAGE_256M, DATA_ENTANGLER, ACCELERATOR, MULTI_THREADER, STRUCTURE
    }

    private final Kind kind;

    public QuantumCraftingBlock(Kind kind) {
        super(properties(kind), nativeType(kind));
        this.kind = kind;
    }

    public Kind getQuantumKind() {
        return kind;
    }

    private static CraftingUnitType nativeType(Kind kind) {
        switch (kind) {
            case STORAGE_128M:
            case STORAGE_256M:
            case CORE:
            case DATA_ENTANGLER:
                return CraftingUnitType.STORAGE_64K;
            case ACCELERATOR:
            case MULTI_THREADER:
                return CraftingUnitType.ACCELERATOR;
            default:
                return CraftingUnitType.UNIT;
        }
    }

    private static AbstractBlock.Properties properties(Kind kind) {
        if (kind == Kind.STRUCTURE) {
            return AbstractBlock.Properties.create(Material.GLASS)
                    .hardnessAndResistance(5.0F, 30.0F)
                    .notSolid();
        }
        return AbstractBlock.Properties.create(Material.IRON)
                .hardnessAndResistance(5.0F, 30.0F);
    }
}
