package com.myroutine.api.dto;

/**
 * Resultado de {@code POST /google/sync}: quantos eventos foram importados/atualizados.
 */
public record GoogleSyncResponse(int syncedCount) {
}
