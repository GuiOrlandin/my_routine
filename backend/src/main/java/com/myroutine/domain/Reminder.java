package com.myroutine.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * POO: CLASSE — molde que agrupa dados (atributos) e comportamentos (métodos).
 *
 * <p>Encapsulamento: atributos privados e validação no construtor e nos métodos
 * garantem que a instância nunca fique em estado inválido.
 */
public class Reminder {

    private final String title;
    private Instant dueAt;
    private final String userId;
    private final Recurrence recurrence;
    private ReminderStatus status;

    public Reminder(String title, Instant dueAt, String userId) {
        this(title, dueAt, userId, Recurrence.NONE);
    }

    public Reminder(String title, Instant dueAt, String userId, Recurrence recurrence) {
        this(title, dueAt, userId, recurrence, ReminderStatus.PENDING, false);
    }

    /**
     * Reconstrói um lembrete já persistido (sem validar data futura).
     * Uso exclusivo da camada {@code repository} ao mapear linhas do banco.
     */
    public static Reminder fromPersistence(
            String title,
            Instant dueAt,
            String userId,
            Recurrence recurrence,
            ReminderStatus status) {
        return new Reminder(title, dueAt, userId, recurrence, status, true);
    }

    private Reminder(
            String title,
            Instant dueAt,
            String userId,
            Recurrence recurrence,
            ReminderStatus status,
            boolean fromPersistence) {
        if (!fromPersistence && dueAt != null && dueAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Data não pode ser no passado");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Título não pode ser vazio");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("Data não pode ser nula");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId não pode ser vazio");
        }

        this.title = title.trim();
        this.dueAt = dueAt;
        this.userId = userId;
        this.recurrence = recurrence != null ? recurrence : Recurrence.NONE;
        this.status = status != null ? status : ReminderStatus.PENDING;
    }

    public String getTitle() {
        return title;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public String getUserId() {
        return userId;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    /** Método de instância — altera o estado interno. */
    public void markDone() {
        this.status = ReminderStatus.DONE;
    }

    /**
     * Adia o lembrete para uma nova data/hora.
     *
     * <p>Encapsulamento: valida a nova data antes de atualizar dueAt e status.
     */
    public void snooze(Instant newDueAt) {
        if (newDueAt == null) {
            throw new IllegalArgumentException("Data não pode ser nula");
        }
        if (newDueAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Data não pode ser no passado");
        }
        this.dueAt = newDueAt;
        this.status = ReminderStatus.SNOOZED;
    }

    @Override
    public String toString() {
        return "Reminder{title='%s', dueAt=%s, userId='%s', status=%s, recurrence=%s}"
                .formatted(title, dueAt, userId, status.getValue(), recurrence.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reminder reminder)) {
            return false;
        }
        return Objects.equals(title, reminder.title)
                && Objects.equals(dueAt, reminder.dueAt)
                && Objects.equals(userId, reminder.userId)
                && recurrence == reminder.recurrence
                && status == reminder.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, dueAt, userId, recurrence, status);
    }
}
