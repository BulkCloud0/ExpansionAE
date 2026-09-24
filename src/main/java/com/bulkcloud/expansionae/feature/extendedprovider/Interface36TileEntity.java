package com.bulkcloud.expansionae.feature.extendedprovider;

import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;

import com.bulkcloud.expansionae.core.registry.ExpansionAEContainers;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;
import com.bulkcloud.expansionae.core.registry.ExpansionAETileEntities;

import appeng.tile.misc.InterfaceTileEntity;

public final class Interface36TileEntity extends InterfaceTileEntity {
    public static final int SLOTS = 36;

    public Interface36TileEntity() {
        super(ExpansionAETileEntities.INTERFACE_36.get());
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return new ItemStack(ExpansionAEItems.INTERFACE_36.get());
    }

    @Override
    public ContainerType<?> getContainerType() {
        return ExpansionAEContainers.INTERFACE_36.get();
    }
}
