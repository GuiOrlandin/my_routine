package com.myroutine.api.dto;

/**
 * Status da conexão Google Agenda após {@code POST /google/connect}.
 */
public record GoogleConnectResponse(boolean connected) {

    public static GoogleConnectResponse ofConnected() {
        return new GoogleConnectResponse(true);
    }
}
