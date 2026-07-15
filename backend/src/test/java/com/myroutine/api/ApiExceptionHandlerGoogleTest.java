package com.myroutine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.myroutine.integration.GoogleAuthError;
import com.myroutine.integration.GoogleAuthErrorCode;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class ApiExceptionHandlerGoogleTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notConnectedReturns400WithDistinctType() {
        ProblemDetail problem = handler.handleGoogleAuth(
                new GoogleAuthError(GoogleAuthErrorCode.NOT_CONNECTED, "Conecte sua agenda"));

        assertEquals(400, problem.getStatus());
        assertEquals("Conecte sua agenda", problem.getDetail());
        assertEquals("Agenda não conectada", problem.getTitle());
        assertEquals(URI.create("urn:myroutine:errors:google-not-connected"), problem.getType());
    }

    @Test
    void revokedReturns401WithGoogleReauthType_notJwtLogout() {
        ProblemDetail problem = handler.handleGoogleAuth(
                new GoogleAuthError(
                        GoogleAuthErrorCode.REVOKED,
                        "Token Google revogado — reconecte sua agenda"));

        assertEquals(401, problem.getStatus());
        assertEquals("Autenticação Google", problem.getTitle());
        assertEquals(URI.create("urn:myroutine:errors:google-reauth"), problem.getType());
        assertNotNull(problem.getDetail());
    }

    @Test
    void reconnectWordingDoesNotBecome400() {
        // regressão: "reconecte" contém a substring "conecte"
        ProblemDetail problem = handler.handleGoogleAuth(
                new GoogleAuthError(
                        GoogleAuthErrorCode.REVOKED,
                        "Token Google revogado ou sem permissão — reconecte sua agenda"));

        assertEquals(401, problem.getStatus());
        assertEquals(URI.create("urn:myroutine:errors:google-reauth"), problem.getType());
    }

    @Test
    void partialSyncFailureReturns500WithSyncType() {
        ProblemDetail problem = handler.handleIllegalState(
                new IllegalStateException(
                        "Falha ao sincronizar Google Calendar após 2 evento(s) importado(s)"));

        assertEquals(500, problem.getStatus());
        assertEquals(URI.create("urn:myroutine:errors:google-sync-failed"), problem.getType());
        assertEquals("Falha na sincronização", problem.getTitle());
    }
}
