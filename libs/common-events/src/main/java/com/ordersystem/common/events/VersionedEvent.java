package com.ordersystem.common.events;

public interface VersionedEvent {
    String getSchemaVersion();

    String getEventType();

    default boolean isBackwardCompatibleWith(String priorVersion) {
        return this.getSchemaVersion().equals(priorVersion);
    }
}
