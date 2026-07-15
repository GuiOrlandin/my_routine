package com.myroutine.integration;

/**
 * Token Google ausente, inválido ou revogado — o cliente deve reconectar a agenda.
 */
public class GoogleAuthError extends RuntimeException {

    public GoogleAuthError(String message) {
        super(message);
    }

    public GoogleAuthError(String message, Throwable cause) {
        super(message, cause);
    }
}
