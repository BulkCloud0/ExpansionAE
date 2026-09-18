package dev.bulkcloud.expansionae;

import appeng.block.AEBaseTileBlock;
import net.minecraft.block.material.Material;

public final class WirelessConnectorBlock extends AEBaseTileBlock<WirelessConnectorTile> {
    public WirelessConnectorBlock() {
        super(defaultProps(Material.IRON).hardnessAndResistance(3.0F));
    }
}
