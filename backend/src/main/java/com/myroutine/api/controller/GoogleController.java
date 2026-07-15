package com.myroutine.api.controller;

import com.myroutine.api.dto.GoogleConnectRequest;
import com.myroutine.api.dto.GoogleConnectResponse;
import com.myroutine.api.dto.GoogleSyncResponse;
import com.myroutine.api.security.UserPrincipal;
import com.myroutine.integration.GoogleCalendarPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de integração Google Calendar (OAuth + sync).
 *
 * <p>Auth: JWT Supabase via {@link AuthenticationPrincipal}; connect/sync ficam em {@link GoogleCalendarPort}.
 */
@RestController
@RequestMapping("/google")
public class GoogleController {

    private final GoogleCalendarPort googleCalendarPort;

    public GoogleController(GoogleCalendarPort googleCalendarPort) {
        this.googleCalendarPort = googleCalendarPort;
    }

    @PostMapping("/connect")
    public ResponseEntity<GoogleConnectResponse> connect(
            @Valid @RequestBody GoogleConnectRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        googleCalendarPort.connect(user.getId(), request.code());
        return ResponseEntity.status(HttpStatus.OK).body(GoogleConnectResponse.ofConnected());
    }

    /**
     * Dispara sync manual Google Calendar → lembretes ({@code source=google}).
     */
    @PostMapping("/sync")
    public ResponseEntity<GoogleSyncResponse> sync(@AuthenticationPrincipal UserPrincipal user) {
        int syncedCount = googleCalendarPort.syncEvents(user.getId());
        return ResponseEntity.ok(new GoogleSyncResponse(syncedCount));
    }
}
