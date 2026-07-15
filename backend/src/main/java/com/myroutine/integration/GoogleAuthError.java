package com.myroutine.integration;

/**
 * Token Google ausente, inválido ou revogado — o cliente deve conectar/reconectar a agenda.
 */
public class GoogleAuthError extends RuntimeException {

    private final GoogleAuthErrorCode code;

    public GoogleAuthError(GoogleAuthErrorCode code, String message) {
        super(message);
        this.code = code != null ? code : GoogleAuthErrorCode.AUTH_FAILED;
    }

    public GoogleAuthError(GoogleAuthErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code != null ? code : GoogleAuthErrorCode.AUTH_FAILED;
    }

    public GoogleAuthErrorCode getCode() {
        return code;
    }
}
