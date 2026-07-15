package com.myroutine.integration;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.myroutine.config.GoogleProperties;
import com.myroutine.domain.Reminder;
import com.myroutine.repository.GoogleTokenRepository;
import com.myroutine.repository.ReminderRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Adapter da Google Calendar API (OCP): isolado em {@code integration/}.
 * Controllers/services só dependem de {@link GoogleCalendarPort}.
 */
@Service
public class GoogleCalendarService implements GoogleCalendarPort {

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "My Routine";
    private static final List<String> CALENDAR_SCOPES = List.of(CalendarScopes.CALENDAR_READONLY);

    private final GoogleProperties googleProperties;
    private final TokenCipher tokenCipher;
    private final GoogleTokenRepository googleTokenRepository;
    private final ReminderRepository reminderRepository;

    public GoogleCalendarService(
            GoogleProperties googleProperties,
            TokenCipher tokenCipher,
            GoogleTokenRepository googleTokenRepository,
            ReminderRepository reminderRepository) {
        this.googleProperties = googleProperties;
        this.tokenCipher = tokenCipher;
        this.googleTokenRepository = googleTokenRepository;
        this.reminderRepository = reminderRepository;
    }

    @Override
    public void connect(String userId, String authCode) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId não pode ser vazio");
        }
        if (authCode == null || authCode.isBlank()) {
            throw new IllegalArgumentException("authCode não pode ser vazio");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    googleProperties.getClientId(),
                    googleProperties.getClientSecret(),
                    authCode,
                    googleProperties.getRedirectUri())
                    .execute();

            String refreshToken = tokenResponse.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new GoogleAuthError(
                        GoogleAuthErrorCode.REVOKED,
                        "Google não retornou refresh_token. Reconecte com access_type=offline e prompt=consent.");
            }

            String encrypted = tokenCipher.encrypt(refreshToken);
            googleTokenRepository.save(userId, encrypted, CALENDAR_SCOPES);
        } catch (TokenResponseException e) {
            throw toAuthError(e);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao trocar authorization code com Google", e);
        }
    }

    @Override
    public int syncEvents(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId não pode ser vazio");
        }

        String encrypted = googleTokenRepository.findEncryptedRefreshToken(userId)
                .orElseThrow(() -> new GoogleAuthError(
                        GoogleAuthErrorCode.NOT_CONNECTED,
                        "Conecte sua agenda"));

        String refreshToken = tokenCipher.decrypt(encrypted);
        Calendar calendar = buildCalendarClient(refreshToken);

        Instant now = Instant.now();
        Instant weekAhead = now.plus(7, ChronoUnit.DAYS);

        try {
            Events events = calendar.events()
                    .list("primary")
                    .setTimeMin(new DateTime(now.toEpochMilli()))
                    .setTimeMax(new DateTime(weekAhead.toEpochMilli()))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            List<Event> items = events.getItems();
            if (items == null || items.isEmpty()) {
                return 0;
            }

            int upserted = 0;
            try {
                for (Event event : items) {
                    if (event.getId() == null || event.getId().isBlank()) {
                        continue;
                    }
                    Instant dueAt = resolveStart(event);
                    if (dueAt == null) {
                        continue;
                    }
                    String title = event.getSummary();
                    if (title == null || title.isBlank()) {
                        title = "(Sem título)";
                    }

                    Reminder reminder = new Reminder(title, dueAt, userId);
                    reminderRepository.upsertGoogleEvent(reminder, event.getId());
                    upserted++;
                }
            } catch (RuntimeException e) {
                // Sync parcial: eventos já upsertados permanecem; o detail cita a contagem.
                throw new IllegalStateException(
                        "Falha ao sincronizar Google Calendar após " + upserted
                                + " evento(s) importado(s)",
                        e);
            }
            return upserted;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                throw new GoogleAuthError(
                        GoogleAuthErrorCode.REVOKED,
                        "Token Google revogado ou sem permissão — reconecte sua agenda", e);
            }
            throw new IllegalStateException("Falha ao listar eventos do Google Calendar", e);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao sincronizar Google Calendar", e);
        }
    }

    private Calendar buildCalendarClient(String refreshToken) {
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(googleProperties.getClientId())
                .setClientSecret(googleProperties.getClientSecret())
                .setRefreshToken(refreshToken)
                .build();

        try {
            credentials.refreshIfExpired();
        } catch (IOException e) {
            throw new GoogleAuthError(
                    GoogleAuthErrorCode.REVOKED,
                    "Token Google revogado — reconecte sua agenda", e);
        }

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Instant resolveStart(Event event) {
        if (event.getStart() == null) {
            return null;
        }
        if (event.getStart().getDateTime() != null) {
            return Instant.ofEpochMilli(event.getStart().getDateTime().getValue());
        }
        if (event.getStart().getDate() != null) {
            return Instant.ofEpochMilli(event.getStart().getDate().getValue());
        }
        return null;
    }

    private static GoogleAuthError toAuthError(TokenResponseException e) {
        String detail = e.getDetails() != null ? e.getDetails().getError() : null;
        if ("invalid_grant".equals(detail)) {
            return new GoogleAuthError(
                    GoogleAuthErrorCode.REVOKED,
                    "Token Google revogado — reconecte sua agenda", e);
        }
        return new GoogleAuthError(
                GoogleAuthErrorCode.AUTH_FAILED,
                "Falha na autenticação Google: " + (detail != null ? detail : e.getMessage()), e);
    }
}

