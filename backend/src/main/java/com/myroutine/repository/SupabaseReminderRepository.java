package com.myroutine.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.myroutine.domain.Recurrence;
import com.myroutine.domain.Reminder;
import com.myroutine.domain.ReminderStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * POO: IMPLEMENTAÇÃO — adapter concreto do contrato {@link ReminderRepository}.
 *
 * <p>SOLID — LSP: pode substituir qualquer outra implementação do contrato sem quebrar
 * {@code ReminderService}. Composição: delega HTTP ao {@link WebClient} configurado para
 * PostgREST com service role (bypass RLS no servidor).
 */
@Repository
public class SupabaseReminderRepository implements ReminderRepository {

    private static final String TABLE = "reminders";
    private static final ParameterizedTypeReference<List<ReminderRow>> ROW_LIST =
            new ParameterizedTypeReference<>() {};

    private final WebClient supabaseClient;

    public SupabaseReminderRepository(@Qualifier("supabaseWebClient") WebClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    @Override
    public Reminder save(Reminder reminder) {
        ReminderWrite payload = ReminderWrite.fromDomain(reminder);

        try {
            List<ReminderRow> rows = supabaseClient.post()
                    .uri(TABLE)
                    .header("Prefer", "return=representation")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ROW_LIST)
                    .block();

            return toDomain(requireSingleRow(rows, "salvar lembrete"));
        } catch (WebClientResponseException e) {
            throw persistenceError("Falha ao salvar lembrete", e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw persistenceError("Falha ao salvar lembrete", e);
        }
    }

    @Override
    public Optional<Reminder> findById(UUID id) {
        try {
            List<ReminderRow> rows = supabaseClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TABLE)
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .bodyToMono(ROW_LIST)
                    .block();

            if (rows == null || rows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toDomain(rows.getFirst()));
        } catch (WebClientResponseException e) {
            throw persistenceError("Falha ao buscar lembrete por id", e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw persistenceError("Falha ao buscar lembrete por id", e);
        }
    }

    @Override
    public List<Reminder> findByUserId(String userId) {
        try {
            List<ReminderRow> rows = supabaseClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TABLE)
                            .queryParam("user_id", "eq." + userId)
                            .queryParam("order", "due_at.asc")
                            .build())
                    .retrieve()
                    .bodyToMono(ROW_LIST)
                    .block();

            if (rows == null) {
                return List.of();
            }
            return rows.stream().map(this::toDomain).toList();
        } catch (WebClientResponseException e) {
            throw persistenceError("Falha ao listar lembretes do usuário", e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw persistenceError("Falha ao listar lembretes do usuário", e);
        }
    }

    @Override
    public Reminder update(UUID id, Reminder reminder) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", reminder.getTitle());
        payload.put("due_at", reminder.getDueAt().toString());
        payload.put("status", reminder.getStatus().getValue());
        payload.put("recurrence", toDbRecurrence(reminder.getRecurrence()));
        payload.put("updated_at", Instant.now().toString());

        try {
            List<ReminderRow> rows = supabaseClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path(TABLE)
                            .queryParam("id", "eq." + id)
                            .build())
                    .header("Prefer", "return=representation")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ROW_LIST)
                    .block();

            return toDomain(requireSingleRow(rows, "atualizar lembrete"));
        } catch (WebClientResponseException e) {
            throw persistenceError("Falha ao atualizar lembrete", e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw persistenceError("Falha ao atualizar lembrete", e);
        }
    }

    @Override
    public void delete(UUID id) {
        try {
            supabaseClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path(TABLE)
                            .queryParam("id", "eq." + id)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw persistenceError("Falha ao excluir lembrete", e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw persistenceError("Falha ao excluir lembrete", e);
        }
    }

    private Reminder toDomain(ReminderRow row) {
        return Reminder.fromPersistence(
                row.title(),
                row.dueAt(),
                row.userId(),
                Recurrence.fromValue(row.recurrence()),
                ReminderStatus.fromValue(row.status()));
    }

    private ReminderRow requireSingleRow(List<ReminderRow> rows, String operation) {
        if (rows == null || rows.isEmpty()) {
            throw new ReminderPersistenceException("Supabase não retornou linha após " + operation);
        }
        return rows.getFirst();
    }

    private static String toDbRecurrence(Recurrence recurrence) {
        if (recurrence == null || recurrence == Recurrence.NONE) {
            return null;
        }
        return recurrence.getValue();
    }

    private static ReminderPersistenceException persistenceError(String message, Exception cause) {
        if (cause instanceof WebClientResponseException webError) {
            return new ReminderPersistenceException(
                    message + " (HTTP " + webError.getStatusCode().value() + ": "
                            + webError.getResponseBodyAsString() + ")",
                    cause);
        }
        return new ReminderPersistenceException(message, cause);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ReminderWrite(
            @JsonProperty("user_id") String userId,
            String title,
            @JsonProperty("due_at") String dueAt,
            String status,
            String source,
            String recurrence) {

        static ReminderWrite fromDomain(Reminder reminder) {
            return new ReminderWrite(
                    reminder.getUserId(),
                    reminder.getTitle(),
                    reminder.getDueAt().toString(),
                    reminder.getStatus().getValue(),
                    "manual",
                    toDbRecurrence(reminder.getRecurrence()));
        }
    }

    private record ReminderRow(
            UUID id,
            @JsonProperty("user_id") String userId,
            String title,
            @JsonProperty("due_at") Instant dueAt,
            String status,
            String source,
            @JsonProperty("external_id") String externalId,
            String recurrence,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt) {
    }
}
