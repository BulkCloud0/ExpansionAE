package dev.bulkcloud.expansionae;

import appeng.items.misc.EncodedPatternItem;

/**
 * 1.16.5 representation of AdvancedAE's Advanced Processing Pattern.
 *
 * <p>AE2 8.4 decodes every {@link EncodedPatternItem} subclass through the same
 * crafting API. Directional metadata is stored alongside the normal AE2
 * processing-pattern NBT and consumed by {@link RoutingBuffer}. This preserves
 * the distinct AdvancedAE pattern item without replacing AE2's crafting
 * engine.</p>
 */
public final class AdvancedProcessingPatternItem extends EncodedPatternItem {
    public AdvancedProcessingPatternItem(Properties properties) {
        super(properties);
    }
}
