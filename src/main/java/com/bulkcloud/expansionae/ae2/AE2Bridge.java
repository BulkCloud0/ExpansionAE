package com.bulkcloud.expansionae.ae2;

import java.util.Objects;

import com.bulkcloud.expansionae.ExpansionAE;

import appeng.api.AEAddon;
import appeng.api.IAEAddon;
import appeng.api.IAppEngApi;

@AEAddon
public final class AE2Bridge implements IAEAddon {

    private static IAppEngApi api;

    public AE2Bridge() {
    }

    @Override
    public void onAPIAvailable(IAppEngApi api) {
        AE2Bridge.api = Objects.requireNonNull(api, "api");

        ExpansionAE.LOGGER.info(
                "Applied Energistics 2 API is available with {} registered storage channel(s)",
                api.storage().storageChannels().size());
    }

    public static IAppEngApi api() {
        if (api == null) {
            throw new IllegalStateException("AE2 API is not available yet");
        }
        return api;
    }

    public static boolean isAvailable() {
        return api != null;
    }
}
