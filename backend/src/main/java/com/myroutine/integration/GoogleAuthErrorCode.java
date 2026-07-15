package com.myroutine.integration;

/**
 * Motivo de falha Google — define status HTTP e {@code ProblemDetail.type} (não usar matching de mensagem).
 */
public enum GoogleAuthErrorCode {
    /** Usuário ainda não conectou a agenda → HTTP 400 */
    NOT_CONNECTED,
    /** Token revogado / sem permissão → HTTP 401 (cliente deve reconectar Google, não logout JWT) */
    REVOKED,
    /** Outra falha de auth Google → HTTP 401 */
    AUTH_FAILED
}
