package com.saif.JobNet.model;

import java.util.Locale;

public enum EmploymentType {
    FULL_TIME,
    PART_TIME,
    INTERNSHIP;

    public static EmploymentType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return FULL_TIME;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        for (EmploymentType value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return FULL_TIME;
    }
}
