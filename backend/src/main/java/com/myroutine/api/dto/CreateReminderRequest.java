package com.myroutine.api.dto;

import com.myroutine.domain.Recurrence;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Corpo JSON de {@code POST /reminders}.
 *
 * <p>Bean Validation na borda HTTP; regras de negócio adicionais ficam em {@link com.myroutine.domain.Reminder}.
 */
public record CreateReminderRequest(
        @NotBlank(message = "Título é obrigatório")
        String title,
        @NotNull(message = "Data é obrigatória")
        @Future(message = "Data não pode ser no passado")
        Instant dueAt,
        Recurrence recurrence
) {}
