package dev.bulkcloud.expansionae;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.implementations.UpgradeableContainer;
import appeng.container.slot.FakeTypeOnlySlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.items.IItemHandler;

public final class ThresholdLevelEmitterContainer extends UpgradeableContainer implements TextFilterReceiver {
    public static final ContainerType<ThresholdLevelEmitterContainer> TYPE = ContainerTypeBuilder
            .create(ThresholdLevelEmitterContainer::new, ThresholdLevelEmitter.class)
            .requirePermission(SecurityPermissions.BUILD)
            .build("expansionae_threshold_level_emitter");

    private final ThresholdLevelEmitter host;

    @GuiSync(21)
    public long upperValue;

    @GuiSync(22)
    public long lowerValue;

    public ThresholdLevelEmitterContainer(int id, PlayerInventory player, ThresholdLevelEmitter host) {
        super(TYPE, id, player, host);
        this.host = host;
    }

    @Override
    protected void setupConfig() {
        setupUpgrades();
        IItemHandler config = getUpgradeable().getInventoryByName("config");
        addSlot(new FakeTypeOnlySlot(config, 0), SlotSemantic.CONFIG);
    }

    @Override
    protected boolean supportCapacity() {
        return false;
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        if (isServer()) {
            setFuzzyMode((FuzzyMode) host.getConfigManager().getSetting(Settings.FUZZY_MODE));
            setRedStoneMode((RedstoneMode) host.getConfigManager().getSetting(Settings.REDSTONE_EMITTER));
            upperValue = host.getUpperValue();
            lowerValue = host.getLowerValue();
        }
        standardDetectAndSendChanges();
    }

    @Override
    public void applyTextFilter(int key, String value) {
        try {
            long parsed = Math.max(0L, Long.parseLong(value.trim()));
            if (key == 0) host.setUpperValue(parsed);
            if (key == 1) host.setLowerValue(parsed);
        } catch (NumberFormatException ignored) {
        }
    }
}
