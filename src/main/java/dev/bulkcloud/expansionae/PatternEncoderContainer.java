package dev.bulkcloud.expansionae;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantic;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.slot.RestrictedInputSlot;
import appeng.core.Api;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;

public final class PatternEncoderContainer extends AEBaseContainer {
    public static final ContainerType<PatternEncoderContainer> TYPE = ContainerTypeBuilder
            .create(PatternEncoderContainer::new, PatternEncoderHost.class).build("expansionae_pattern_encoder");
    private final PatternEncoderHost host;
    public PatternEncoderContainer(int id, PlayerInventory player, PatternEncoderHost host) {
        super(TYPE, id, player, host);
        this.host = host;
        lockPlayerInventorySlot(host.slot);
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, host.inventory, 0), SlotSemantic.ENCODED_PATTERN);
        createPlayerInventorySlots(player);
        registerClientAction("cycleFace", Integer.class, index -> { if (index != null) cycleFace(index); });
        registerClientAction("convertPattern", this::convertPatternServer);
    }
    public ItemStack pattern() { return host.inventory.getStackInSlot(0); }
    public boolean isAdvancedPattern() {
        return !pattern().isEmpty() && pattern().getItem() == ExpansionAE.ADV_PROCESSING_PATTERN.get();
    }

    public void convertPattern() {
        if (isRemote()) sendClientAction("convertPattern");
        else convertPatternServer();
    }

    private void convertPatternServer() {
        if (!holdingEncoder()) { setValidContainer(false); return; }
        ItemStack current = pattern();
        ICraftingPatternDetails details = details();
        if (current.isEmpty() || details == null || details.isCraftable()) return;

        ItemStack replacement;
        if (current.getItem() == ExpansionAE.ADV_PROCESSING_PATTERN.get()) {
            replacement = Api.instance().definitions().items().encodedPattern().maybeStack(1).orElse(ItemStack.EMPTY);
        } else {
            replacement = new ItemStack(ExpansionAE.ADV_PROCESSING_PATTERN.get());
        }
        if (replacement.isEmpty()) return;
        if (current.hasTag()) replacement.setTag(current.getTag().copy());
        if (current.hasDisplayName()) replacement.setDisplayName(current.getDisplayName());
        host.inventory.setStackInSlot(0, replacement);
        host.saveChanges();
        detectAndSendChanges();
    }

    public ICraftingPatternDetails details() { return Api.instance().crafting().decodePattern(pattern(), getPlayerInventory().player.world); }
    public void cycleFace(int inputIndex) {
        if (inputIndex < 0 || inputIndex >= 9) return;
        if (isRemote()) { sendClientAction("cycleFace", inputIndex); return; }
        if (!holdingEncoder()) { setValidContainer(false); return; }
        ICraftingPatternDetails details = details();
        if (details == null || details.isCraftable() || inputIndex >= details.getInputs().size()
                || details.getInputs().get(inputIndex) == null) return;
        int[] faces = new int[9];
        for (int i = 0; i < 9; i++) faces[i] = RoutingBuffer.face(pattern(), i);
        faces[inputIndex] = faces[inputIndex] == 5 ? -1 : faces[inputIndex] + 1;
        pattern().getOrCreateTag().putIntArray(RoutingBuffer.PATTERN_TAG, faces);
        host.saveChanges();
        detectAndSendChanges();
    }
    private boolean holdingEncoder() { return getPlayerInventory().getStackInSlot(host.slot) == host.getItemStack(); }
    @Override public void detectAndSendChanges() {
        if (!isRemote() && !holdingEncoder()) setValidContainer(false);
        super.detectAndSendChanges();
    }
}
