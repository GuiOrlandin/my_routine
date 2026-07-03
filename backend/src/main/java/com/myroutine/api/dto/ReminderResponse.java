package com.myroutine.api.dto;

import com.myroutine.domain.Reminder;
import com.myroutine.domain.SavedReminder;

import java.time.Instant;
import java.util.UUID;

/**
 * Representação JSON de um lembrete retornado pela API.
 *
 * <p>Metadados de persistência ({@code id}, {@code source}, timestamps) vêm do banco;
 * o domínio fornece título, data, status e recorrência.
 */
public record ReminderResponse(
        UUID id,
        String title,
        Instant dueAt,
        String status,
        String recurrence,
        String source,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReminderResponse from(SavedReminder saved) {
        return from(saved.reminder(), saved.id(), saved.source(), saved.createdAt(), saved.updatedAt());
    }

    public static ReminderResponse from(
            Reminder reminder,
            UUID id,
            String source,
            Instant createdAt,
            Instant updatedAt) {
        return new ReminderResponse(
                id,
                reminder.getTitle(),
                reminder.getDueAt(),
                reminder.getStatus().getValue(),
                reminder.getRecurrence().getValue(),
                source,
                createdAt,
                updatedAt);
    }
}
