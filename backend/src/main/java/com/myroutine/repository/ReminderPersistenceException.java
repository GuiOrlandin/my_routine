package com.myroutine.repository;

/**
 * Erro ao persistir ou consultar lembretes no Supabase (PostgREST).
 */
public class ReminderPersistenceException extends RuntimeException {

    public ReminderPersistenceException(String message) {
        super(message);
    }

    public ReminderPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
