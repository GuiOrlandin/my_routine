package com.myroutine.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Lembrete persistido com metadados do banco (id, source, timestamps).
 * Retornado por {@link com.myroutine.repository.ReminderRepository#save} para montar respostas da API.
 */
public record SavedReminder(
        Reminder reminder,
        UUID id,
        String source,
        Instant createdAt,
        Instant updatedAt) {}
