package com.myroutine.api.dto;

import com.myroutine.domain.Recurrence;
import com.myroutine.domain.ReminderStatus;
import jakarta.validation.constraints.Future;

import java.time.Instant;

/**
 * Corpo JSON de {@code PATCH /reminders/{id}}.
 *
 * <p>Campos opcionais: {@code null} significa que o campo não será alterado.
 */
public record UpdateReminderRequest(
        String title,
        @Future(message = "Data não pode ser no passado")
        Instant dueAt,
        ReminderStatus status,
        Recurrence recurrence
) {}
