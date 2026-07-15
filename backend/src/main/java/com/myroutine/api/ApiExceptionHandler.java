package com.myroutine.api;

import com.myroutine.integration.GoogleAuthError;
import com.myroutine.integration.GoogleAuthErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final URI TYPE_GOOGLE_NOT_CONNECTED =
            URI.create("urn:myroutine:errors:google-not-connected");
    private static final URI TYPE_GOOGLE_REAUTH =
            URI.create("urn:myroutine:errors:google-reauth");
    private static final URI TYPE_SYNC_FAILED =
            URI.create("urn:myroutine:errors:google-sync-failed");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getField())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, message);
        problem.setTitle("Dados inválidos");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Dados inválidos");
        return problem;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Não encontrado");

        return problem;
    }

    /**
     * Google ausente/revogado: 400 se ainda não conectou; 401 se precisa reconectar (T17/T18).
     * {@code type} distingue de 401 JWT — o mobile não deve fazer logout Supabase.
     */
    @ExceptionHandler(GoogleAuthError.class)
    public ProblemDetail handleGoogleAuth(GoogleAuthError ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Falha na autenticação Google";
        boolean notConnectedYet = ex.getCode() == GoogleAuthErrorCode.NOT_CONNECTED;
        HttpStatus status = notConnectedYet ? HttpStatus.BAD_REQUEST : HttpStatus.UNAUTHORIZED;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(notConnectedYet ? "Agenda não conectada" : "Autenticação Google");
        problem.setType(notConnectedYet ? TYPE_GOOGLE_NOT_CONNECTED : TYPE_GOOGLE_REAUTH);
        return problem;
    }

    /**
     * Falhas de sync/I/O (inclui sync parcial — mensagem cita quantos eventos já entraram).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Falha interna";
        String lower = message.toLowerCase();
        boolean googleSync = lower.contains("sincronizar") || lower.contains("google calendar");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message);
        problem.setTitle(googleSync ? "Falha na sincronização" : "Erro interno");
        if (googleSync) {
            problem.setType(TYPE_SYNC_FAILED);
        }
        return problem;
    }
}
