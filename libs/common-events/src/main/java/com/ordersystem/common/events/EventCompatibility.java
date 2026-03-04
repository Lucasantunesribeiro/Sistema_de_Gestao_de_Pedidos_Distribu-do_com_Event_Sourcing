package com.ordersystem.common.events;

import java.util.Collection;
import java.util.Objects;

public final class EventCompatibility {

    private EventCompatibility() {
    }

    public static boolean isBackwardCompatible(VersionedEvent event, Collection<String> supportedVersions) {
        Objects.requireNonNull(event, "event");
        return supportedVersions.contains(event.getSchemaVersion());
    }
}
