package com.bulkcloud.expansionae.registry;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.feature.disk.DiskItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ExpansionAEItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExpansionAE.MOD_ID);

    public static final RegistryObject<DiskItem> DISK_1K = ITEMS.register(
            "disk_1k",
            () -> new DiskItem(1_000L, 0.5D, new Item.Properties()
                    .stacksTo(1)
                    .tab(ItemGroup.TAB_MISC)
                    .fireResistant()));

    public static final RegistryObject<DiskItem> DISK_4K = ITEMS.register(
            "disk_4k",
            () -> new DiskItem(4_000L, 1.0D, new Item.Properties()
                    .stacksTo(1)
                    .tab(ItemGroup.TAB_MISC)
                    .fireResistant()));

    public static final RegistryObject<DiskItem> DISK_16K = ITEMS.register(
            "disk_16k",
            () -> new DiskItem(16_000L, 1.5D, new Item.Properties()
                    .stacksTo(1)
                    .tab(ItemGroup.TAB_MISC)
                    .fireResistant()));

    public static final RegistryObject<DiskItem> DISK_64K = ITEMS.register(
            "disk_64k",
            () -> new DiskItem(64_000L, 2.0D, new Item.Properties()
                    .stacksTo(1)
                    .tab(ItemGroup.TAB_MISC)
                    .fireResistant()));

    private ExpansionAEItems() {
    }
}
