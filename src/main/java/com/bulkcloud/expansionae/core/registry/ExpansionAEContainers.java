package com.bulkcloud.expansionae.core.registry;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.stockexport.StockExportBusContainer;
import com.bulkcloud.expansionae.feature.extendedbus.ExtendedBusContainer;

public final class ExpansionAEContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, ExpansionAE.MOD_ID);

    public static final RegistryObject<ContainerType<ExtendedBusContainer>> EXTENDED_IMPORT_BUS =
            CONTAINERS.register(
                    "extended_import_bus",
                    () -> IForgeContainerType.create(ExtendedBusContainer::fromImportNetwork));

    public static final RegistryObject<ContainerType<ExtendedBusContainer>> EXTENDED_EXPORT_BUS =
            CONTAINERS.register(
                    "extended_export_bus",
                    () -> IForgeContainerType.create(ExtendedBusContainer::fromExportNetwork));

    public static final RegistryObject<ContainerType<StockExportBusContainer>> STOCK_EXPORT_BUS =
            CONTAINERS.register(
                    "stock_export_bus",
                    () -> IForgeContainerType.create(StockExportBusContainer::fromNetwork));

    private ExpansionAEContainers() {
    }

    public static void register(IEventBus modBus) {
        CONTAINERS.register(modBus);
    }
}
