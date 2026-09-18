package dev.bulkcloud.expansionae;

import appeng.helpers.IInterfaceHost;

public interface ExpandedHost extends IInterfaceHost {
    @Override ExpandedDuality getInterfaceDuality();
    default boolean supportsAdvancedRouting() { return false; }
}
