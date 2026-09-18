package dev.bulkcloud.expansionae;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.FakeSlot;
import appeng.container.slot.OutputSlot;
import appeng.container.slot.RestrictedInputSlot;
import appeng.core.Api;
import appeng.items.misc.EncodedPatternItem;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public final class PatternModifierContainer extends AEBaseContainer {
    public static final ContainerType<PatternModifierContainer> TYPE = ContainerTypeBuilder
            .create(PatternModifierContainer::new, PatternModifierHost.class)
            .build("expansionae_pattern_modifier");

    private final PatternModifierHost host;

    public PatternModifierContainer(int id, PlayerInventory player, PatternModifierHost host) {
        super(TYPE, id, player, host);
        this.host = host;
        lockPlayerInventorySlot(host.slot);

        for (int i = 0; i < host.patterns.getSlots(); i++) {
            addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, host.patterns, i),
                    SlotSemantic.ENCODED_PATTERN);
        }

        addSlot(new RestrictedInputSlot(
                RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, host.target, 0),
                SlotSemantic.MACHINE_INPUT);
        addSlot(new OutputSlot(host.cloneOutput, 0, null), SlotSemantic.MACHINE_OUTPUT);

        for (int i = 0; i < host.blanks.getSlots(); i++) {
            addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.BLANK_PATTERN, host.blanks, i),
                    SlotSemantic.BLANK_PATTERN);
        }

        addSlot(new FakeSlot(host.replace, 0), SlotSemantic.CONFIG);
        addSlot(new FakeSlot(host.replace, 1), SlotSemantic.CONFIG);

        createPlayerInventorySlots(player);

        registerClientAction("multiply2", () -> modify(2, false));
        registerClientAction("multiply10", () -> modify(10, false));
        registerClientAction("divide2", () -> modify(2, true));
        registerClientAction("divide10", () -> modify(10, true));
        registerClientAction("clearPatterns", this::clearPatterns);
        registerClientAction("replaceIngredient", this::replaceIngredient);
        registerClientAction("clonePattern", this::clonePattern);
    }

    public void multiply2() { runAction("multiply2", () -> modify(2, false)); }
    public void multiply10() { runAction("multiply10", () -> modify(10, false)); }
    public void divide2() { runAction("divide2", () -> modify(2, true)); }
    public void divide10() { runAction("divide10", () -> modify(10, true)); }
    public void clear() { runAction("clearPatterns", this::clearPatterns); }
    public void replace() { runAction("replaceIngredient", this::replaceIngredient); }
    public void cloneTarget() { runAction("clonePattern", this::clonePattern); }

    private void runAction(String name, Runnable serverAction) {
        if (isRemote()) {
            sendClientAction(name);
        } else {
            serverAction.run();
        }
    }

    private boolean holdingModifier() {
        return getPlayerInventory().getStackInSlot(host.slot) == host.getItemStack();
    }

    private void modify(int scale, boolean divide) {
        if (!canMutate() || scale <= 0) return;

        for (int slot = 0; slot < host.patterns.getSlots(); slot++) {
            ItemStack pattern = host.patterns.getStackInSlot(slot);
            if (!isProcessingPattern(pattern)) continue;

            CompoundNBT original = pattern.getTag();
            if (original == null) continue;

            CompoundNBT changed = original.copy();
            if (!modifyList(changed.getList(EncodedPatternItem.NBT_INGREDIENTS, Constants.NBT.TAG_COMPOUND),
                    scale, divide)
                    || !modifyList(changed.getList(EncodedPatternItem.NBT_PRODUCTS, Constants.NBT.TAG_COMPOUND),
                    scale, divide)) {
                continue;
            }

            ItemStack candidate = pattern.copy();
            candidate.setTag(changed);

            if (Api.instance().crafting().decodePattern(candidate, getPlayerInventory().player.world) != null) {
                ItemHandlerUtil.setStackInSlot(host.patterns, slot, candidate);
            }
        }

        host.saveChanges();
        detectAndSendChanges();
    }

    /**
     * 1.16.5 ItemStack NBT stores Count in a signed byte. We deliberately cap at
     * 127 instead of allowing silent overflow/corruption.
     */
    private boolean modifyList(ListNBT list, int scale, boolean divide) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.read(list.getCompound(i));
            if (stack.isEmpty()) continue;

            int old = stack.getCount();
            if (divide && old % scale != 0) {
                return false;
            }

            long next = divide ? old / scale : (long) old * scale;
            if (next <= 0 || next > 127) {
                return false;
            }
        }

        for (int i = 0; i < list.size(); i++) {
            CompoundNBT entry = list.getCompound(i);
            ItemStack stack = ItemStack.read(entry);
            if (stack.isEmpty()) continue;

            int next = divide ? stack.getCount() / scale : stack.getCount() * scale;
            stack.setCount(next);
            CompoundNBT replacement = new CompoundNBT();
            stack.write(replacement);
            list.set(i, replacement);
        }

        return true;
    }

    private void replaceIngredient() {
        if (!canMutate()) return;

        ItemStack from = host.replace.getStackInSlot(0);
        ItemStack to = host.replace.getStackInSlot(1);
        if (from.isEmpty()) return;

        for (int slot = 0; slot < host.patterns.getSlots(); slot++) {
            ItemStack pattern = host.patterns.getStackInSlot(slot);
            if (!Api.instance().crafting().isEncodedPattern(pattern) || pattern.getTag() == null) continue;

            CompoundNBT changed = pattern.getTag().copy();
            boolean didChange = replaceInList(
                    changed.getList(EncodedPatternItem.NBT_INGREDIENTS, Constants.NBT.TAG_COMPOUND), from, to);
            didChange |= replaceInList(
                    changed.getList(EncodedPatternItem.NBT_PRODUCTS, Constants.NBT.TAG_COMPOUND), from, to);
            if (!didChange) continue;

            ItemStack candidate = pattern.copy();
            candidate.setTag(changed);

            // Crafting replacements must remain a valid recipe in this AE2 version.
            ICraftingPatternDetails decoded =
                    Api.instance().crafting().decodePattern(candidate, getPlayerInventory().player.world);
            if (decoded != null) {
                ItemHandlerUtil.setStackInSlot(host.patterns, slot, candidate);
            }
        }

        host.saveChanges();
        detectAndSendChanges();
    }

    private boolean replaceInList(ListNBT list, ItemStack from, ItemStack to) {
        boolean changed = false;

        for (int i = 0; i < list.size(); i++) {
            ItemStack current = ItemStack.read(list.getCompound(i));
            if (current.isEmpty() || !Platform.itemComparisons().isSameItem(current, from)) continue;

            CompoundNBT replacement = new CompoundNBT();
            if (!to.isEmpty()) {
                ItemStack newStack = to.copy();
                newStack.setCount(current.getCount());
                newStack.write(replacement);
            }
            list.set(i, replacement);
            changed = true;
        }

        return changed;
    }

    private void clearPatterns() {
        if (!canMutate()) return;

        for (int slot = 0; slot < host.patterns.getSlots(); slot++) {
            ItemStack pattern = host.patterns.getStackInSlot(slot);
            if (!Api.instance().crafting().isEncodedPattern(pattern)) continue;

            ItemStack blank = Api.instance().definitions().materials().blankPattern()
                    .maybeStack(1).orElse(ItemStack.EMPTY);
            if (!blank.isEmpty()) {
                ItemHandlerUtil.setStackInSlot(host.patterns, slot, blank);
            }
        }

        host.saveChanges();
        detectAndSendChanges();
    }

    private void clonePattern() {
        if (!canMutate()) return;

        ItemStack target = host.target.getStackInSlot(0);
        if (!Api.instance().crafting().isEncodedPattern(target)
                || Api.instance().crafting().decodePattern(target, getPlayerInventory().player.world) == null) {
            return;
        }

        ItemStack existing = host.cloneOutput.getStackInSlot(0);
        if (existing.isEmpty()) {
            if (!consumeBlankPattern()) return;
        } else if (!Api.instance().crafting().isEncodedPattern(existing)) {
            return;
        }

        ItemHandlerUtil.setStackInSlot(host.cloneOutput, 0, target.copy());
        host.saveChanges();
        detectAndSendChanges();
    }

    private boolean consumeBlankPattern() {
        for (int i = 0; i < host.blanks.getSlots(); i++) {
            ItemStack stack = host.blanks.getStackInSlot(i);
            if (stack.isEmpty()
                    || !Api.instance().definitions().materials().blankPattern().isSameAs(stack)) {
                continue;
            }

            ItemStack updated = stack.copy();
            updated.shrink(1);
            ItemHandlerUtil.setStackInSlot(host.blanks, i, updated);
            return true;
        }
        return false;
    }

    private boolean isProcessingPattern(ItemStack pattern) {
        ICraftingPatternDetails details =
                Api.instance().crafting().decodePattern(pattern, getPlayerInventory().player.world);
        return details != null && !details.isCraftable();
    }

    private boolean canMutate() {
        if (!holdingModifier()) {
            setValidContainer(false);
            return false;
        }
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        if (!isRemote() && !holdingModifier()) {
            setValidContainer(false);
        }
        super.detectAndSendChanges();
    }
}
