package com.myroutine.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo JSON de {@code POST /google/connect}.
 *
 * <p>{@code code} é o authorization code do OAuth Google (fluxo Calendar).
 */
public record GoogleConnectRequest(
        @NotBlank(message = "Código OAuth é obrigatório")
        String code
) {}
