package com.myroutine.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SupabaseGoogleTokenRepository implements GoogleTokenRepository {

    private static final String TABLE = "google_tokens";
    private static final ParameterizedTypeReference<List<TokenRow>> ROW_LIST =
            new ParameterizedTypeReference<>() {};

    private final WebClient supabaseClient;

    public SupabaseGoogleTokenRepository(@Qualifier("supabaseWebClient") WebClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    @Override
    public void save(String userId, String encryptedRefreshToken, List<String> scopes) {
        Map<String, Object> payload = Map.of(
                "user_id", userId,
                "refresh_token", encryptedRefreshToken,
                "scopes", scopes,
                "updated_at", Instant.now().toString());

        try {
            supabaseClient.post()
                    .uri(TABLE)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw new ReminderPersistenceException(
                    "Falha ao salvar google_tokens (HTTP " + e.getStatusCode().value() + ": "
                            + e.getResponseBodyAsString() + ")",
                    e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw new ReminderPersistenceException("Falha ao salvar google_tokens", e);
        }
    }

    @Override
    public Optional<String> findEncryptedRefreshToken(String userId) {
        try {
            List<TokenRow> rows = supabaseClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TABLE)
                            .queryParam("user_id", "eq." + userId)
                            .queryParam("select", "refresh_token")
                            .build())
                    .retrieve()
                    .bodyToMono(ROW_LIST)
                    .block();

            if (rows == null || rows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(rows.getFirst().refreshToken());
        } catch (WebClientResponseException e) {
            throw new ReminderPersistenceException(
                    "Falha ao buscar google_tokens (HTTP " + e.getStatusCode().value() + ": "
                            + e.getResponseBodyAsString() + ")",
                    e);
        } catch (RuntimeException e) {
            if (e instanceof ReminderPersistenceException) {
                throw e;
            }
            throw new ReminderPersistenceException("Falha ao buscar google_tokens", e);
        }
    }

    private record TokenRow(@JsonProperty("refresh_token") String refreshToken) {}
}
