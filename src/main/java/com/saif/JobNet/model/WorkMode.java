package com.saif.JobNet.model;

import java.util.Locale;

public enum WorkMode {
    ONSITE,
    HYBRID,
    REMOTE;

    public static WorkMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ONSITE;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        for (WorkMode value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return ONSITE;
    }
}
