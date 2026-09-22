package com.bulkcloud.expansionae.core.registry;

import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.disk.DiskStorageCellItem;

public final class ExpansionAEItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExpansionAE.MOD_ID);

    public static final RegistryObject<Item> DISK_1K = ITEMS.register(
            "1k_disk",
            () -> new DiskStorageCellItem(new Item.Properties(), 1000, 0.5));

    private ExpansionAEItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
