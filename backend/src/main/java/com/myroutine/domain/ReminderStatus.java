package com.myroutine.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReminderStatus {
    PENDING("pending"),
    DONE("done"),
    SNOOZED("snoozed");

    private final String value;

    ReminderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReminderStatus fromValue(String value) {
        for (ReminderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status inválido: " + value);
    }
}
