package com.myroutine.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myroutine.api.ApiExceptionHandler;
import com.myroutine.api.dto.GoogleSyncResponse;
import com.myroutine.api.security.UserPrincipal;
import com.myroutine.integration.GoogleAuthError;
import com.myroutine.integration.GoogleAuthErrorCode;
import com.myroutine.integration.GoogleCalendarPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Seams: {@link GoogleController#sync} (unit) + HTTP mapping via {@link ApiExceptionHandler} (MockMvc).
 */
class GoogleControllerSyncTest {

    private GoogleCalendarPort googleCalendarPort;
    private GoogleController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        googleCalendarPort = mock(GoogleCalendarPort.class);
        controller = new GoogleController(googleCalendarPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new FixedUserPrincipalResolver("user-1"))
                .build();
    }

    @Test
    void syncReturnsSyncedCount() {
        when(googleCalendarPort.syncEvents(eq("user-1"))).thenReturn(3);

        ResponseEntity<GoogleSyncResponse> response =
                controller.sync(new UserPrincipal("user-1", "user@example.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().syncedCount());
    }

    @Test
    void syncWithoutGoogleTokenReturns400() throws Exception {
        when(googleCalendarPort.syncEvents(eq("user-1")))
                .thenThrow(new GoogleAuthError(GoogleAuthErrorCode.NOT_CONNECTED, "Conecte sua agenda"));

        mockMvc.perform(post("/google/sync"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Conecte sua agenda"))
                .andExpect(jsonPath("$.type").value("urn:myroutine:errors:google-not-connected"));
    }

    @Test
    void syncWithRevokedTokenReturns401GoogleReauth() throws Exception {
        when(googleCalendarPort.syncEvents(eq("user-1")))
                .thenThrow(new GoogleAuthError(
                        GoogleAuthErrorCode.REVOKED,
                        "Token Google revogado — reconecte sua agenda"));

        mockMvc.perform(post("/google/sync"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:myroutine:errors:google-reauth"))
                .andExpect(jsonPath("$.title").value("Autenticação Google"));
    }

    @Test
    void syncPropagatesNotConnectedToCaller() {
        when(googleCalendarPort.syncEvents(eq("user-1")))
                .thenThrow(new GoogleAuthError(GoogleAuthErrorCode.NOT_CONNECTED, "Conecte sua agenda"));

        GoogleAuthError error = assertThrows(
                GoogleAuthError.class,
                () -> controller.sync(new UserPrincipal("user-1", "user@example.com")));

        assertEquals(GoogleAuthErrorCode.NOT_CONNECTED, error.getCode());
    }

    private static final class FixedUserPrincipalResolver implements HandlerMethodArgumentResolver {

        private final String userId;

        private FixedUserPrincipalResolver(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(UserPrincipal.class)
                    && parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return new UserPrincipal(userId, "user@example.com");
        }
    }
}
