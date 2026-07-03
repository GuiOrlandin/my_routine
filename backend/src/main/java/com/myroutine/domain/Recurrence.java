package com.myroutine.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Recurrence {
    NONE("none"),
    DAILY("daily"),
    WEEKLY("weekly");

    private final String value;

    Recurrence(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Recurrence fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        for (Recurrence recurrence : values()) {
            if (recurrence.value.equals(value)) {
                return recurrence;
            }
        }
        throw new IllegalArgumentException("Recorrência inválida: " + value);
    }
}
