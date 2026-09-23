package com.bulkcloud.expansionae.feature.disk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.bulkcloud.expansionae.ExpansionAE;
import com.bulkcloud.expansionae.ae2.ExpansionAEApi;
import com.bulkcloud.expansionae.core.registry.ExpansionAEItems;

import appeng.api.config.Actionable;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.tile.storage.ChestTileEntity;
import appeng.tile.storage.DriveTileEntity;

public final class DiskRuntimeValidator {
    private static final String PERSISTENCE_PHASE_ENV = "EXPANSIONAE_PERSISTENCE_PHASE";
    private static final UUID PERSISTENCE_TEST_UUID =
            UUID.fromString("0d15c000-0000-4000-8000-000000000165");
    private static final long PERSISTENCE_TEST_AMOUNT = 321L;

    private DiskRuntimeValidator() {
    }

    public static void validate() {
        if (FMLEnvironment.production) {
            return;
        }

        DiskStorageData storage = DiskStorageService.getCurrent();
        if (storage == null) {
            throw new IllegalStateException("DISK runtime validation requires loaded overworld storage");
        }

        IItemStorageChannel channel =
                ExpansionAEApi.get().storage().getStorageChannel(IItemStorageChannel.class);

        validateTierCapacities(storage, channel);
        validateMoreThanSixtyThreeTypes(storage, channel);
        validateCrossTierUuidRejection(storage, channel);
        validateOverCapacityBackingRejection(storage, channel);
        validateUndecodableBackingRejection(storage, channel);
        validateWorkbenchSemantics(channel);
        validateRecipes();
        validateTransientStorage(storage, channel);
        validateAe2StorageHosts(channel);
        validatePersistencePhase(storage, channel);
        DiskGridRuntimeValidator.begin();
    }

    private static void validateTierCapacities(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        validateTierCapacity(storage, channel, ExpansionAEItems.DISK_1K.get(), 1_000, "1k");
        validateTierCapacity(storage, channel, ExpansionAEItems.DISK_4K.get(), 4_000, "4k");
        validateTierCapacity(storage, channel, ExpansionAEItems.DISK_16K.get(), 16_000, "16k");
        validateTierCapacity(storage, channel, ExpansionAEItems.DISK_64K.get(), 64_000, "64k");

        ExpansionAE.LOGGER.info(
                "DISK tier capacities validated in runtime (1k/4k/16k/64k)");
    }

    private static void validateTierCapacity(
            DiskStorageData storage,
            IItemStorageChannel channel,
            Item item,
            long capacity,
            String tier) {
        ItemStack stack = new ItemStack(item);
        ICellInventoryHandler<IAEItemStack> handler =
                open(stack, channel, tier + " capacity validation");

        IAEItemStack remainder = handler.injectItems(
                stone(channel, capacity + 137),
                Actionable.MODULATE,
                null);
        if (remainder == null || remainder.getStackSize() != 137) {
            throw new IllegalStateException(
                    tier + " DISK expected a 137-item remainder at capacity " + capacity);
        }

        requireStoredCount(handler, capacity);
        requireCachedMetadata(stack, capacity, 1, tier + " at capacity");
        if (handler.getCellInv().getStatusForCell() != appeng.api.storage.cells.CellState.FULL) {
            throw new IllegalStateException(tier + " DISK did not report FULL at capacity");
        }

        if (!stack.hasTag() || !stack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException(tier + " DISK did not receive a storage UUID");
        }
        UUID uuid = stack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        IAEItemStack extracted = handler.extractItems(
                stone(channel, capacity),
                Actionable.MODULATE,
                null);
        if (extracted == null || extracted.getStackSize() != capacity) {
            throw new IllegalStateException(
                    tier + " DISK could not extract its full capacity");
        }

        requireStoredCount(handler, 0);
        requireCachedMetadata(stack, 0, 0, tier + " after full extraction");
        storage.remove(uuid);
    }

    private static void validateMoreThanSixtyThreeTypes(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> handler =
                open(stack, channel, "70-type validation");

        int insertedTypes = 0;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = item.getRegistryName();
            if (item == Items.AIR || id == null || !"minecraft".equals(id.getNamespace())) {
                continue;
            }

            IAEItemStack candidate = channel.createStack(new ItemStack(item));
            if (candidate == null) {
                continue;
            }
            candidate.setStackSize(1);

            IAEItemStack remainder =
                    handler.injectItems(candidate, Actionable.MODULATE, null);
            if (remainder != null) {
                throw new IllegalStateException(
                        "1k DISK rejected distinct type " + id
                                + " before reaching the 70-type validation target");
            }

            insertedTypes++;
            if (insertedTypes == 70) {
                break;
            }
        }

        if (insertedTypes != 70) {
            throw new IllegalStateException(
                    "Could not gather 70 vanilla item types for DISK validation; got "
                            + insertedTypes);
        }

        if (handler.getCellInv().getStoredItemTypes() != 70L
                || handler.getCellInv().getStoredItemCount() != 70L) {
            throw new IllegalStateException(
                    "1k DISK did not retain 70 distinct item types above the classic 63-type limit");
        }

        requireCachedMetadata(stack, 70L, 70L, "70-type validation");

        if (handler.getCellInv().getRemainingItemTypes() <= 63L) {
            throw new IllegalStateException(
                    "1k DISK reported an artificial remaining type limit after storing 70 types");
        }

        if (!stack.hasTag() || !stack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException(
                    "70-type DISK validation did not allocate a storage UUID");
        }
        storage.remove(stack.getTag().getUniqueId(DiskCellInventory.TAG_UUID));

        ExpansionAE.LOGGER.info(
                "DISK unlimited-type runtime validated (70 distinct item types stored in one 1k DISK)");
    }

    private static void validateCrossTierUuidRejection(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        ItemStack oneK = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> oneKHandler =
                open(oneK, channel, "cross-tier source");

        IAEItemStack initial = stone(channel, 200);
        if (oneKHandler.injectItems(initial, Actionable.MODULATE, null) != null) {
            throw new IllegalStateException("Cross-tier validation could not prepare the 1k DISK");
        }

        if (!oneK.hasTag() || !oneK.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("Cross-tier source DISK did not receive a UUID");
        }
        UUID uuid = oneK.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        DiskStorageData.DiskRecord record = storage.get(uuid);
        if (record == null || record.getCapacity() != 1_000L) {
            throw new IllegalStateException("1k DISK backing record was not bound to capacity 1000");
        }

        ItemStack forgedFourK = new ItemStack(ExpansionAEItems.DISK_4K.get());
        forgedFourK.setTag(oneK.getTag().copy());

        ICellInventoryHandler<IAEItemStack> forgedHandler =
                open(forgedFourK, channel, "cross-tier forged alias");

        IAEItemStack rejected =
                forgedHandler.injectItems(stone(channel, 1), Actionable.MODULATE, null);
        if (rejected == null || rejected.getStackSize() != 1) {
            throw new IllegalStateException("Cross-tier 4k alias was allowed to inject into a 1k backing record");
        }

        IAEItemStack extracted =
                forgedHandler.extractItems(stone(channel, 1), Actionable.MODULATE, null);
        if (extracted != null) {
            throw new IllegalStateException("Cross-tier 4k alias was allowed to extract from a 1k backing record");
        }

        if (!forgedHandler.getAvailableItems(channel.createList()).isEmpty()) {
            throw new IllegalStateException("Cross-tier 4k alias exposed contents from a 1k backing record");
        }

        DiskStorageData.DiskRecord unchanged = storage.get(uuid);
        if (unchanged == null
                || unchanged.getCapacity() != 1_000L
                || unchanged.getItemCount() != 200L) {
            throw new IllegalStateException("Cross-tier alias attempt mutated the authoritative 1k backing record");
        }

        storage.remove(uuid);

        ExpansionAE.LOGGER.info(
                "DISK cross-tier UUID rejection validated (1k backing cannot be opened as 4k)");
    }

    private static void validateOverCapacityBackingRejection(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        UUID uuid = UUID.randomUUID();

        IAEItemStack stoneKey = stone(channel, 1);
        CompoundNBT key = new CompoundNBT();
        stoneKey.writeToNBT(key);

        ListNBT keys = new ListNBT();
        keys.add(key);

        storage.put(
                uuid,
                keys,
                new long[] { 1_001L },
                1_001L,
                1_000L);

        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        stack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, uuid);
        stack.getOrCreateTag().putLong(DiskCellInventory.TAG_ITEM_COUNT, 1_001L);
        stack.getOrCreateTag().putLong(DiskCellInventory.TAG_TYPE_COUNT, 1L);

        ICellInventoryHandler<IAEItemStack> handler =
                open(stack, channel, "over-capacity backing validation");

        IAEItemStack rejected =
                handler.injectItems(stone(channel, 1), Actionable.MODULATE, null);
        if (rejected == null || rejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "Over-capacity 1k DISK backing record accepted an insertion");
        }

        if (handler.extractItems(stone(channel, 1), Actionable.MODULATE, null) != null) {
            throw new IllegalStateException(
                    "Over-capacity 1k DISK backing record allowed extraction");
        }

        if (!handler.getAvailableItems(channel.createList()).isEmpty()) {
            throw new IllegalStateException(
                    "Over-capacity 1k DISK backing record exposed corrupted contents");
        }

        DiskStorageData.DiskRecord unchanged = storage.get(uuid);
        if (unchanged == null
                || unchanged.getCapacity() != 1_000L
                || unchanged.getItemCount() != 1_001L
                || unchanged.getAmounts().length != 1
                || unchanged.getAmounts()[0] != 1_001L) {
            throw new IllegalStateException(
                    "Fail-closed over-capacity validation mutated authoritative backing data");
        }

        storage.remove(uuid);

        UUID legacyUuid = UUID.randomUUID();
        storage.put(
                legacyUuid,
                keys,
                new long[] { 1_001L },
                1_001L);

        ItemStack legacyStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        legacyStack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, legacyUuid);
        legacyStack.getOrCreateTag().putLong(DiskCellInventory.TAG_ITEM_COUNT, 1_001L);
        legacyStack.getOrCreateTag().putLong(DiskCellInventory.TAG_TYPE_COUNT, 1L);

        ICellInventoryHandler<IAEItemStack> legacyHandler =
                open(legacyStack, channel, "legacy over-capacity backing validation");

        IAEItemStack legacyRejected =
                legacyHandler.injectItems(stone(channel, 1), Actionable.MODULATE, null);
        if (legacyRejected == null || legacyRejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "Legacy over-capacity 1k DISK backing record accepted an insertion");
        }

        if (legacyHandler.extractItems(stone(channel, 1), Actionable.MODULATE, null) != null) {
            throw new IllegalStateException(
                    "Legacy over-capacity 1k DISK backing record allowed extraction");
        }

        if (!legacyHandler.getAvailableItems(channel.createList()).isEmpty()) {
            throw new IllegalStateException(
                    "Legacy over-capacity 1k DISK backing record exposed corrupted contents");
        }

        DiskStorageData.DiskRecord unchangedLegacy = storage.get(legacyUuid);
        if (unchangedLegacy == null
                || unchangedLegacy.getCapacity() != 0L
                || unchangedLegacy.getItemCount() != 1_001L
                || unchangedLegacy.getAmounts().length != 1
                || unchangedLegacy.getAmounts()[0] != 1_001L) {
            throw new IllegalStateException(
                    "Legacy fail-closed over-capacity validation mutated or bound authoritative data");
        }

        storage.remove(legacyUuid);

        ExpansionAE.LOGGER.info(
                "DISK over-capacity backing rejection validated (bound + legacy records preserved, access blocked)");
    }

    private static void validateUndecodableBackingRejection(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        UUID uuid = UUID.randomUUID();

        ListNBT keys = new ListNBT();
        keys.add(new CompoundNBT());

        storage.put(
                uuid,
                keys,
                new long[] { 1L },
                1L,
                1_000L);

        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        stack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, uuid);
        stack.getOrCreateTag().putLong(DiskCellInventory.TAG_ITEM_COUNT, 1L);
        stack.getOrCreateTag().putLong(DiskCellInventory.TAG_TYPE_COUNT, 1L);

        ICellInventoryHandler<IAEItemStack> handler =
                open(stack, channel, "undecodable backing validation");

        IAEItemStack rejected =
                handler.injectItems(stone(channel, 1), Actionable.MODULATE, null);
        if (rejected == null || rejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "DISK with undecodable backing data accepted an insertion");
        }

        if (handler.extractItems(stone(channel, 1), Actionable.MODULATE, null) != null) {
            throw new IllegalStateException(
                    "DISK with undecodable backing data allowed extraction");
        }

        if (!handler.getAvailableItems(channel.createList()).isEmpty()) {
            throw new IllegalStateException(
                    "DISK with undecodable backing data exposed corrupted contents");
        }

        DiskStorageData.DiskRecord unchanged = storage.get(uuid);
        if (unchanged == null
                || unchanged.getCapacity() != 1_000L
                || unchanged.getItemCount() != 1L
                || unchanged.getKeys().size() != 1
                || unchanged.getAmounts().length != 1
                || unchanged.getAmounts()[0] != 1L) {
            throw new IllegalStateException(
                    "Fail-closed undecodable backing validation mutated authoritative data");
        }

        storage.remove(uuid);

        UUID legacyUuid = UUID.randomUUID();
        storage.put(
                legacyUuid,
                keys,
                new long[] { 1L },
                1L);

        ItemStack legacyStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        legacyStack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, legacyUuid);
        legacyStack.getOrCreateTag().putLong(DiskCellInventory.TAG_ITEM_COUNT, 1L);
        legacyStack.getOrCreateTag().putLong(DiskCellInventory.TAG_TYPE_COUNT, 1L);

        ICellInventoryHandler<IAEItemStack> legacyHandler =
                open(legacyStack, channel, "legacy undecodable backing validation");

        IAEItemStack legacyRejected =
                legacyHandler.injectItems(stone(channel, 1), Actionable.MODULATE, null);
        if (legacyRejected == null || legacyRejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "Legacy DISK with undecodable backing data accepted an insertion");
        }

        if (legacyHandler.extractItems(stone(channel, 1), Actionable.MODULATE, null) != null) {
            throw new IllegalStateException(
                    "Legacy DISK with undecodable backing data allowed extraction");
        }

        DiskStorageData.DiskRecord unchangedLegacy = storage.get(legacyUuid);
        if (unchangedLegacy == null
                || unchangedLegacy.getCapacity() != 0L
                || unchangedLegacy.getItemCount() != 1L
                || unchangedLegacy.getKeys().size() != 1
                || unchangedLegacy.getAmounts().length != 1
                || unchangedLegacy.getAmounts()[0] != 1L) {
            throw new IllegalStateException(
                    "Legacy undecodable backing was mutated or bound before semantic validation");
        }

        storage.remove(legacyUuid);

        ExpansionAE.LOGGER.info(
                "DISK undecodable backing rejection validated (bound + legacy data preserved, access blocked)");
    }

    private static void validateWorkbenchSemantics(
            IItemStorageChannel channel) {
        validateWorkbenchNbtPersistence(ExpansionAEItems.DISK_1K.get(), "1k");
        validateWorkbenchNbtPersistence(ExpansionAEItems.DISK_4K.get(), "4k");
        validateWorkbenchNbtPersistence(ExpansionAEItems.DISK_16K.get(), "16k");
        validateWorkbenchNbtPersistence(ExpansionAEItems.DISK_64K.get(), "64k");
        validatePartitionFiltering(channel);
        validateFuzzyPartitionFiltering(channel);

        ExpansionAE.LOGGER.info(
                "DISK Cell Workbench runtime semantics validated "
                        + "(config/upgrades persist in NBT, FUZZY+INVERTER accepted, "
                        + "CAPACITY rejected, whitelist/inverter/fuzzy filtering active)");
    }

    private static void validateWorkbenchNbtPersistence(
            Item item,
            String tier) {
        if (!(item instanceof DiskStorageCellItem)) {
            throw new IllegalStateException(tier + " DISK is not a DiskStorageCellItem");
        }

        DiskStorageCellItem disk = (DiskStorageCellItem) item;
        ItemStack stack = new ItemStack(disk);

        IItemHandler config = disk.getConfigInventory(stack);
        ItemStack configRemainder =
                config.insertItem(0, new ItemStack(Items.STONE), false);
        if (!configRemainder.isEmpty()) {
            throw new IllegalStateException(
                    tier + " DISK config inventory rejected a partition item");
        }

        IItemHandler reopenedConfig = disk.getConfigInventory(stack);
        if (reopenedConfig.getStackInSlot(0).getItem() != Items.STONE) {
            throw new IllegalStateException(
                    tier + " DISK config inventory did not persist through ItemStack NBT");
        }

        IItemHandler upgrades = disk.getUpgradesInventory(stack);
        ItemStack fuzzyCard =
                ExpansionAEApi.get().definitions().materials().cardFuzzy().stack(1);
        ItemStack fuzzyRemainder = upgrades.insertItem(0, fuzzyCard, false);
        if (!fuzzyRemainder.isEmpty()) {
            throw new IllegalStateException(
                    tier + " DISK rejected its registered FUZZY card");
        }

        ItemStack capacityCard =
                ExpansionAEApi.get().definitions().materials().cardCapacity().stack(1);
        ItemStack capacityRemainder = upgrades.insertItem(1, capacityCard, false);
        if (capacityRemainder.isEmpty() || capacityRemainder.getCount() != 1) {
            throw new IllegalStateException(
                    tier + " DISK accepted an unsupported CAPACITY card");
        }

        ItemStack inverterCard =
                ExpansionAEApi.get().definitions().materials().cardInverter().stack(1);
        ItemStack inverterRemainder = upgrades.insertItem(1, inverterCard, false);
        if (!inverterRemainder.isEmpty()) {
            throw new IllegalStateException(
                    tier + " DISK rejected its registered INVERTER card");
        }

        IItemHandler reopenedUpgrades = disk.getUpgradesInventory(stack);
        if (reopenedUpgrades.getStackInSlot(0).getItem() != fuzzyCard.getItem()
                || reopenedUpgrades.getStackInSlot(1).getItem() != inverterCard.getItem()) {
            throw new IllegalStateException(
                    tier + " DISK upgrades did not persist through ItemStack NBT");
        }

        ItemStack extractedFuzzy = reopenedUpgrades.extractItem(0, 1, false);
        if (extractedFuzzy.isEmpty() || extractedFuzzy.getItem() != fuzzyCard.getItem()) {
            throw new IllegalStateException(
                    tier + " DISK could not remove the persisted FUZZY card");
        }

        IItemHandler afterExtraction = disk.getUpgradesInventory(stack);
        if (!afterExtraction.getStackInSlot(0).isEmpty()
                || afterExtraction.getStackInSlot(1).getItem() != inverterCard.getItem()) {
            throw new IllegalStateException(
                    tier + " DISK upgrade removal did not persist through ItemStack NBT");
        }
    }

    private static void validatePartitionFiltering(
            IItemStorageChannel channel) {
        DiskStorageCellItem disk =
                (DiskStorageCellItem) ExpansionAEItems.DISK_1K.get();

        ItemStack whitelistStack = new ItemStack(disk);
        IItemHandler whitelistConfig = disk.getConfigInventory(whitelistStack);
        if (!whitelistConfig.insertItem(0, new ItemStack(Items.STONE), false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK whitelist validation could not configure stone");
        }

        ICellInventoryHandler<IAEItemStack> whitelist =
                open(whitelistStack, channel, "Cell Workbench whitelist validation");

        if (whitelist.injectItems(
                item(channel, Items.STONE, 1),
                Actionable.SIMULATE,
                null) != null) {
            throw new IllegalStateException(
                    "Partitioned DISK rejected its configured whitelist item");
        }

        IAEItemStack dirtRejected = whitelist.injectItems(
                item(channel, Items.DIRT, 1),
                Actionable.SIMULATE,
                null);
        if (dirtRejected == null || dirtRejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "Partitioned DISK accepted an item outside its whitelist");
        }

        ItemStack blacklistStack = new ItemStack(disk);
        IItemHandler blacklistConfig = disk.getConfigInventory(blacklistStack);
        if (!blacklistConfig.insertItem(0, new ItemStack(Items.STONE), false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK inverter validation could not configure stone");
        }

        IItemHandler blacklistUpgrades = disk.getUpgradesInventory(blacklistStack);
        ItemStack inverterCard =
                ExpansionAEApi.get().definitions().materials().cardInverter().stack(1);
        if (!blacklistUpgrades.insertItem(0, inverterCard, false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK inverter validation could not install an INVERTER card");
        }

        ICellInventoryHandler<IAEItemStack> blacklist =
                open(blacklistStack, channel, "Cell Workbench inverter validation");

        IAEItemStack stoneRejected = blacklist.injectItems(
                item(channel, Items.STONE, 1),
                Actionable.SIMULATE,
                null);
        if (stoneRejected == null || stoneRejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "Inverted DISK accepted its configured blacklist item");
        }

        if (blacklist.injectItems(
                item(channel, Items.DIRT, 1),
                Actionable.SIMULATE,
                null) != null) {
            throw new IllegalStateException(
                    "Inverted DISK rejected an item outside its blacklist");
        }
    }

    private static void validateFuzzyPartitionFiltering(
            IItemStorageChannel channel) {
        DiskStorageCellItem disk =
                (DiskStorageCellItem) ExpansionAEItems.DISK_1K.get();

        ItemStack configuredPickaxe = new ItemStack(Items.IRON_PICKAXE);
        configuredPickaxe.setDamage(10);

        ItemStack candidatePickaxe = new ItemStack(Items.IRON_PICKAXE);
        candidatePickaxe.setDamage(100);

        ItemStack preciseStack = new ItemStack(disk);
        IItemHandler preciseConfig = disk.getConfigInventory(preciseStack);
        if (!preciseConfig.insertItem(0, configuredPickaxe.copy(), false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK fuzzy validation could not configure the precise damageable item");
        }

        ICellInventoryHandler<IAEItemStack> precise =
                open(preciseStack, channel, "Cell Workbench precise partition validation");

        IAEItemStack preciseRejected = precise.injectItems(
                stack(channel, candidatePickaxe, 1),
                Actionable.SIMULATE,
                null);
        if (preciseRejected == null || preciseRejected.getStackSize() != 1) {
            throw new IllegalStateException(
                    "DISK precise partition unexpectedly accepted a different durability variant");
        }

        ItemStack fuzzyStack = new ItemStack(disk);
        IItemHandler fuzzyConfig = disk.getConfigInventory(fuzzyStack);
        if (!fuzzyConfig.insertItem(0, configuredPickaxe.copy(), false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK fuzzy validation could not configure the damageable item");
        }

        IItemHandler fuzzyUpgrades = disk.getUpgradesInventory(fuzzyStack);
        ItemStack fuzzyCard =
                ExpansionAEApi.get().definitions().materials().cardFuzzy().stack(1);
        if (!fuzzyUpgrades.insertItem(0, fuzzyCard, false).isEmpty()) {
            throw new IllegalStateException(
                    "DISK fuzzy validation could not install a FUZZY card");
        }

        disk.setFuzzyMode(fuzzyStack, appeng.api.config.FuzzyMode.PERCENT_50);

        ICellInventoryHandler<IAEItemStack> fuzzy =
                open(fuzzyStack, channel, "Cell Workbench fuzzy partition validation");

        if (!fuzzy.isFuzzy()) {
            throw new IllegalStateException(
                    "DISK with FUZZY card did not expose a fuzzy partition handler");
        }

        if (fuzzy.injectItems(
                stack(channel, candidatePickaxe, 1),
                Actionable.SIMULATE,
                null) != null) {
            throw new IllegalStateException(
                    "DISK fuzzy partition rejected a durability variant in the same 50% band");
        }
    }

    private static void validateRecipes() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException("Dedicated server is not available for DISK recipe validation");
        }

        validateRecipe("1k_disk", ExpansionAEItems.DISK_1K.get());
        validateRecipe("4k_disk", ExpansionAEItems.DISK_4K.get());
        validateRecipe("16k_disk", ExpansionAEItems.DISK_16K.get());
        validateRecipe("64k_disk", ExpansionAEItems.DISK_64K.get());

        ExpansionAE.LOGGER.info(
                "DISK recipes validated in runtime (1k/4k/16k/64k loaded as crafting recipes with correct outputs)");
    }

    private static void validateRecipe(
            String recipePath,
            Item expectedOutput) {
        ResourceLocation id = new ResourceLocation(ExpansionAE.MOD_ID, recipePath);
        IRecipe<?> recipe = ServerLifecycleHooks.getCurrentServer()
                .getRecipeManager()
                .getRecipe(id)
                .orElseThrow(() -> new IllegalStateException("Missing DISK recipe " + id));

        if (recipe.getType() != IRecipeType.CRAFTING) {
            throw new IllegalStateException("DISK recipe " + id + " is not a crafting recipe");
        }

        ItemStack output = recipe.getRecipeOutput();
        if (output.isEmpty() || output.getItem() != expectedOutput || output.getCount() != 1) {
            throw new IllegalStateException(
                    "DISK recipe " + id + " has unexpected output " + output);
        }
    }

    private static void validateTransientStorage(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        ItemStack primaryStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> primary = open(primaryStack, channel, "primary");

        IAEItemStack stone = stone(channel, 600);
        IAEItemStack firstRemainder = primary.injectItems(stone, Actionable.MODULATE, null);
        if (firstRemainder != null) {
            throw new IllegalStateException("1k DISK rejected part of the initial 600 item insertion");
        }
        requireStoredCount(primary, 600);
        requireCachedMetadata(primaryStack, 600, 1, "primary after initial insertion");

        if (!primaryStack.hasTag() || !primaryStack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("1k DISK did not assign a backing UUID after first insertion");
        }
        UUID uuid = primaryStack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        ItemStack aliasStack = primaryStack.copy();
        ICellInventoryHandler<IAEItemStack> alias = open(aliasStack, channel, "alias");
        requireStoredCount(alias, 600);
        requireCachedMetadata(aliasStack, 600, 1, "alias after copy");

        IAEItemStack additional = stone(channel, 500);
        IAEItemStack capacityRemainder = alias.injectItems(additional, Actionable.MODULATE, null);
        if (capacityRemainder == null || capacityRemainder.getStackSize() != 100) {
            throw new IllegalStateException("1k DISK capacity contract expected a remainder of 100 from 500");
        }

        requireStoredCount(alias, 1000);
        requireStoredCount(primary, 1000);
        requireCachedMetadata(aliasStack, 1000, 1, "alias after capacity insertion");
        requireCachedMetadata(primaryStack, 1000, 1, "primary after alias capacity insertion");

        IAEItemStack request250 = stone(channel, 250);
        IAEItemStack extracted250 = alias.extractItems(request250, Actionable.MODULATE, null);
        if (extracted250 == null || extracted250.getStackSize() != 250) {
            throw new IllegalStateException("DISK alias extraction did not return 250 items");
        }
        requireStoredCount(primary, 750);
        requireCachedMetadata(primaryStack, 750, 1, "primary after alias extraction");
        requireCachedMetadata(aliasStack, 750, 1, "alias after alias extraction");

        IAEItemStack request750 = stone(channel, 750);
        IAEItemStack extracted750 = primary.extractItems(request750, Actionable.MODULATE, null);
        if (extracted750 == null || extracted750.getStackSize() != 750) {
            throw new IllegalStateException("DISK primary extraction did not return the remaining 750 items");
        }

        requireStoredCount(alias, 0);
        requireCachedMetadata(primaryStack, 0, 0, "primary after emptying");
        requireCachedMetadata(aliasStack, 0, 0, "alias after remote emptying");

        DiskStorageData.DiskRecord emptyRecord = storage.get(uuid);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException("Empty DISK backing record was not preserved for UUID aliases");
        }

        IAEItemStack selfAliasItem = channel.createStack(aliasStack);
        if (selfAliasItem == null) {
            throw new IllegalStateException("AE2 item channel could not create the DISK self-alias test stack");
        }
        selfAliasItem.setStackSize(1);

        IAEItemStack selfAliasRemainder =
                primary.injectItems(selfAliasItem, Actionable.MODULATE, null);
        if (selfAliasRemainder == null || selfAliasRemainder.getStackSize() != 1) {
            throw new IllegalStateException("DISK accepted an item alias pointing at its own backing UUID");
        }
        requireStoredCount(primary, 0);

        DiskStorageData.DiskRecord afterSelfAliasAttempt = storage.get(uuid);
        if (afterSelfAliasAttempt == null || afterSelfAliasAttempt.getItemCount() != 0) {
            throw new IllegalStateException("Rejected DISK self-alias insertion modified the backing record");
        }

        ItemStack nestedDiskStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> nestedDisk =
                open(nestedDiskStack, channel, "non-empty nested DISK");
        if (nestedDisk.injectItems(stone(channel, 1), Actionable.MODULATE, null) != null) {
            throw new IllegalStateException(
                    "Nested DISK preparation unexpectedly rejected one stone");
        }
        if (!nestedDiskStack.hasTag()
                || !nestedDiskStack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException(
                    "Nested DISK preparation did not allocate a backing UUID");
        }

        UUID nestedUuid = nestedDiskStack.getTag().getUniqueId(DiskCellInventory.TAG_UUID);
        if (uuid.equals(nestedUuid)) {
            throw new IllegalStateException(
                    "Nested DISK validation unexpectedly reused the outer backing UUID");
        }

        IAEItemStack nestedDiskItem = channel.createStack(nestedDiskStack);
        if (nestedDiskItem == null) {
            throw new IllegalStateException(
                    "AE2 item channel could not create the non-empty nested DISK test stack");
        }
        nestedDiskItem.setStackSize(1);

        IAEItemStack nestedRemainder =
                primary.injectItems(nestedDiskItem, Actionable.MODULATE, null);
        if (nestedRemainder == null || nestedRemainder.getStackSize() != 1) {
            throw new IllegalStateException(
                    "DISK accepted a different storage cell that already contained items");
        }
        requireStoredCount(primary, 0);

        DiskStorageData.DiskRecord afterNestedAttempt = storage.get(uuid);
        if (afterNestedAttempt == null || afterNestedAttempt.getItemCount() != 0) {
            throw new IllegalStateException(
                    "Rejected non-empty nested DISK insertion modified the outer backing record");
        }

        DiskStorageData.DiskRecord nestedRecord = storage.get(nestedUuid);
        if (nestedRecord == null || nestedRecord.getItemCount() != 1) {
            throw new IllegalStateException(
                    "Rejected nested DISK insertion modified the nested backing record");
        }

        storage.remove(nestedUuid);
        storage.remove(uuid);

        ExpansionAE.LOGGER.info(
                "DISK storage runtime validated (capacity, insert/extract, UUID alias sync, empty backing record, self-alias + non-empty-cell rejection)");
    }

    private static void validateAe2StorageHosts(IItemStorageChannel channel) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            throw new IllegalStateException("Dedicated server is not available for AE2 host validation");
        }

        ServerWorld world = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        if (world == null) {
            throw new IllegalStateException("Overworld is not available for AE2 host validation");
        }

        BlockPos base = world.getSpawnPoint().up(8);
        BlockPos drivePos = base;
        BlockPos chestPos = base.east(2);

        world.removeBlock(drivePos, false);
        world.removeBlock(chestPos, false);

        try {
            validateDriveHost(world, drivePos, channel);
            validateChestHost(world, chestPos, channel);
        } finally {
            world.removeBlock(drivePos, false);
            world.removeBlock(chestPos, false);
        }

        ExpansionAE.LOGGER.info(
                "DISK AE2 host validation passed (ME Drive acceptance/state + ME Chest terminal monitor)");
    }

    private static void validateDriveHost(
            ServerWorld world,
            BlockPos pos,
            IItemStorageChannel channel) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Placed AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity drive = (DriveTileEntity) tile;
        drive.onReady();

        ItemStack diskStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> handler = open(diskStack, channel, "ME Drive test DISK");
        IAEItemStack remainder =
                handler.injectItems(stone(channel, 37), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("ME Drive test DISK rejected its preparation payload");
        }

        IItemHandler driveInventory = drive.getInternalInventory();
        ItemStack rejected = driveInventory.insertItem(0, diskStack, false);
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("AE2 ME Drive rejected the ExpansionAE DISK");
        }

        if (drive.getCellItem(0) != ExpansionAEItems.DISK_1K.get()) {
            throw new IllegalStateException("AE2 ME Drive did not retain the ExpansionAE DISK in slot 0");
        }

        CellState state = drive.getCellStatus(0);
        if (state != CellState.NOT_EMPTY) {
            throw new IllegalStateException(
                    "AE2 ME Drive reported unexpected DISK state: " + state);
        }

        ICellInventoryHandler<IAEItemStack> driveHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(driveInventory.getStackInSlot(0), drive, channel);
        if (driveHandler == null || driveHandler.getCellInv() == null) {
            throw new IllegalStateException("AE2 ME Drive could not reopen the inserted DISK");
        }
        requireStoredCount(driveHandler, 37);

        ItemStack hostedDisk = driveInventory.getStackInSlot(0);
        if (!hostedDisk.hasTag() || !hostedDisk.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)) {
            throw new IllegalStateException("ME Drive hosted DISK lost its storage UUID");
        }
        UUID expectedUuid = hostedDisk.getTag().getUniqueId(DiskCellInventory.TAG_UUID);

        // Simulate the persistent part of a chunk unload/reload: serialize the real
        // Drive tile, tear down its network node, recreate the block entity and load
        // the saved NBT before readying it again.
        CompoundNBT savedDrive = drive.write(new CompoundNBT());
        drive.onChunkUnloaded();
        drive.disableDrops();
        world.removeBlock(pos, false);

        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);
        TileEntity reloadedTile = world.getTileEntity(pos);
        if (!(reloadedTile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Reloaded AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity reloadedDrive = (DriveTileEntity) reloadedTile;
        reloadedDrive.read(world.getBlockState(pos), savedDrive);
        reloadedDrive.onReady();

        IItemHandler reloadedInventory = reloadedDrive.getInternalInventory();
        ItemStack reloadedDisk = reloadedInventory.getStackInSlot(0);
        requireDiskUuid(reloadedDisk, expectedUuid, "chunk reload");

        if (reloadedDrive.getCellStatus(0) != CellState.NOT_EMPTY) {
            throw new IllegalStateException("Reloaded AE2 ME Drive did not restore DISK state");
        }

        ICellInventoryHandler<IAEItemStack> reloadedHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(reloadedDisk, reloadedDrive, channel);
        if (reloadedHandler == null || reloadedHandler.getCellInv() == null) {
            throw new IllegalStateException("Reloaded AE2 ME Drive could not reopen the DISK");
        }
        requireStoredCount(reloadedHandler, 37);

        // Exercise the same inventory-drop hook AEBaseTileBlock uses when a Drive is
        // broken, then reinsert that dropped cell into a fresh Drive.
        List<ItemStack> drops = new ArrayList<>();
        reloadedDrive.getDrops(world, pos, drops);

        ItemStack droppedDisk = ItemStack.EMPTY;
        for (ItemStack drop : drops) {
            if (drop.getItem() == ExpansionAEItems.DISK_1K.get()) {
                droppedDisk = drop.copy();
                break;
            }
        }
        if (droppedDisk.isEmpty()) {
            throw new IllegalStateException("Breaking the AE2 ME Drive did not expose the hosted DISK as a drop");
        }
        requireDiskUuid(droppedDisk, expectedUuid, "Drive drop");

        reloadedDrive.disableDrops();
        world.removeBlock(pos, false);
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().drive().block().getDefaultState(),
                3);

        TileEntity replacedTile = world.getTileEntity(pos);
        if (!(replacedTile instanceof DriveTileEntity)) {
            throw new IllegalStateException("Replaced AE2 ME Drive did not create DriveTileEntity");
        }

        DriveTileEntity replacedDrive = (DriveTileEntity) replacedTile;
        replacedDrive.onReady();
        ItemStack replaceRejected =
                replacedDrive.getInternalInventory().insertItem(0, droppedDisk, false);
        if (!replaceRejected.isEmpty()) {
            throw new IllegalStateException("Fresh AE2 ME Drive rejected the dropped ExpansionAE DISK");
        }

        if (replacedDrive.getCellStatus(0) != CellState.NOT_EMPTY) {
            throw new IllegalStateException("Fresh AE2 ME Drive did not restore the dropped DISK state");
        }

        ItemStack replacedDisk = replacedDrive.getInternalInventory().getStackInSlot(0);
        requireDiskUuid(replacedDisk, expectedUuid, "Drive replacement");

        ICellInventoryHandler<IAEItemStack> replacedHandler =
                ExpansionAEApi.get().registries().cell()
                        .getCellInventory(replacedDisk, replacedDrive, channel);
        if (replacedHandler == null || replacedHandler.getCellInv() == null) {
            throw new IllegalStateException("Fresh AE2 ME Drive could not reopen the dropped DISK");
        }
        requireStoredCount(replacedHandler, 37);
    }

    private static void requireDiskUuid(ItemStack stack, UUID expected, String stage) {
        if (stack.isEmpty()
                || !stack.hasTag()
                || !stack.getTag().hasUniqueId(DiskCellInventory.TAG_UUID)
                || !expected.equals(stack.getTag().getUniqueId(DiskCellInventory.TAG_UUID))) {
            throw new IllegalStateException(
                    "DISK UUID was not preserved during " + stage);
        }
    }

    private static void validateChestHost(
            ServerWorld world,
            BlockPos pos,
            IItemStorageChannel channel) {
        world.setBlockState(
                pos,
                Api.instance().definitions().blocks().chest().block().getDefaultState(),
                3);

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ChestTileEntity)) {
            throw new IllegalStateException("Placed AE2 ME Chest did not create ChestTileEntity");
        }

        ChestTileEntity chest = (ChestTileEntity) tile;
        chest.onReady();

        ItemStack diskStack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        ICellInventoryHandler<IAEItemStack> handler = open(diskStack, channel, "ME Chest test DISK");
        IAEItemStack remainder =
                handler.injectItems(stone(channel, 41), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("ME Chest test DISK rejected its preparation payload");
        }

        IItemHandler chestInventory = chest.getInternalInventory();
        ItemStack rejected = chestInventory.insertItem(1, diskStack, false);
        if (!rejected.isEmpty()) {
            // Chest internal inventory layout can vary; retry the storage-cell slot explicitly.
            rejected = chestInventory.insertItem(0, diskStack, false);
        }
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("AE2 ME Chest rejected the ExpansionAE DISK");
        }

        IMEMonitor<IAEItemStack> monitor = chest.getInventory(channel);
        if (monitor == null) {
            throw new IllegalStateException("AE2 ME Chest did not expose an item monitor for the DISK");
        }

        IAEItemStack precise = monitor.getAvailableItems(channel.createList())
                .findPrecise(stone(channel, 1));
        if (precise == null || precise.getStackSize() != 41) {
            throw new IllegalStateException(
                    "AE2 ME Chest terminal monitor did not expose the expected 41 stored items");
        }
    }

    private static void validatePersistencePhase(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        String phase = System.getenv(PERSISTENCE_PHASE_ENV);
        if (phase == null || phase.isEmpty()) {
            return;
        }

        if ("write".equals(phase)) {
            validatePersistenceWrite(storage, channel);
            return;
        }

        if ("read".equals(phase)) {
            validatePersistenceRead(storage, channel);
            return;
        }

        throw new IllegalStateException(
                "Unknown " + PERSISTENCE_PHASE_ENV + " value: " + phase);
    }

    private static void validatePersistenceWrite(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        storage.remove(PERSISTENCE_TEST_UUID);
        storage.put(PERSISTENCE_TEST_UUID, new ListNBT(), new long[0], 0);

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence writer");

        IAEItemStack remainder =
                handler.injectItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (remainder != null) {
            throw new IllegalStateException("Persistence write phase rejected part of the test payload");
        }

        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence write phase did not update the backing record");
        }

        ExpansionAE.LOGGER.info(
                "DISK persistence write phase validated ({} items staged for restart)",
                PERSISTENCE_TEST_AMOUNT);
    }

    private static void validatePersistenceRead(
            DiskStorageData storage,
            IItemStorageChannel channel) {
        DiskStorageData.DiskRecord record = storage.get(PERSISTENCE_TEST_UUID);
        if (record == null || record.getItemCount() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException(
                    "Persistence read phase did not recover the expected backing record after restart");
        }

        ItemStack stack = stackForUuid(PERSISTENCE_TEST_UUID);
        ICellInventoryHandler<IAEItemStack> handler = open(stack, channel, "persistence reader");
        requireStoredCount(handler, PERSISTENCE_TEST_AMOUNT);

        IAEItemStack extracted =
                handler.extractItems(stone(channel, PERSISTENCE_TEST_AMOUNT), Actionable.MODULATE, null);
        if (extracted == null || extracted.getStackSize() != PERSISTENCE_TEST_AMOUNT) {
            throw new IllegalStateException("Persistence read phase could not extract the recovered payload");
        }

        requireStoredCount(handler, 0);

        DiskStorageData.DiskRecord emptyRecord = storage.get(PERSISTENCE_TEST_UUID);
        if (emptyRecord == null || emptyRecord.getItemCount() != 0) {
            throw new IllegalStateException(
                    "Persistence read phase did not preserve the empty backing record after extraction");
        }

        storage.remove(PERSISTENCE_TEST_UUID);

        ExpansionAE.LOGGER.info(
                "DISK persistence read phase validated (backing data survived server restart)");
    }

    private static ItemStack stackForUuid(UUID uuid) {
        ItemStack stack = new ItemStack(ExpansionAEItems.DISK_1K.get());
        stack.getOrCreateTag().putUniqueId(DiskCellInventory.TAG_UUID, uuid);
        return stack;
    }

    private static ICellInventoryHandler<IAEItemStack> open(
            ItemStack stack,
            IItemStorageChannel channel,
            String label) {
        ICellInventoryHandler<IAEItemStack> handler =
                ExpansionAEApi.get().registries().cell().getCellInventory(stack, null, channel);
        if (handler == null || handler.getCellInv() == null) {
            throw new IllegalStateException("AE2 did not provide a DISK cell inventory handler for " + label);
        }
        return handler;
    }

    private static void requireCachedMetadata(
            ItemStack stack,
            long expectedItems,
            long expectedTypes,
            String stage) {
        if (!stack.hasTag()) {
            throw new IllegalStateException(
                    "DISK cached metadata missing during " + stage);
        }

        long items = Math.max(0, stack.getTag().getLong(DiskCellInventory.TAG_ITEM_COUNT));
        long types = Math.max(0, stack.getTag().getLong(DiskCellInventory.TAG_TYPE_COUNT));

        if (items != expectedItems || types != expectedTypes) {
            throw new IllegalStateException(
                    "DISK cached metadata mismatch during "
                            + stage
                            + ": expected "
                            + expectedItems
                            + " items / "
                            + expectedTypes
                            + " types but got "
                            + items
                            + " / "
                            + types);
        }
    }

    private static IAEItemStack stone(IItemStorageChannel channel, long amount) {
        return item(channel, Items.STONE, amount);
    }

    private static IAEItemStack item(
            IItemStorageChannel channel,
            Item item,
            long amount) {
        return stack(channel, new ItemStack(item), amount);
    }

    private static IAEItemStack stack(
            IItemStorageChannel channel,
            ItemStack itemStack,
            long amount) {
        IAEItemStack stack = channel.createStack(itemStack);
        if (stack == null) {
            throw new IllegalStateException(
                    "AE2 item channel could not create a stack for "
                            + itemStack.getItem().getRegistryName());
        }
        stack.setStackSize(amount);
        return stack;
    }

    private static void requireStoredCount(
            ICellInventoryHandler<IAEItemStack> handler,
            long expected) {
        long actual = handler.getCellInv().getStoredItemCount();
        if (actual != expected) {
            throw new IllegalStateException(
                    "DISK stored item count mismatch: expected " + expected + " but got " + actual);
        }
    }
}
