package com.myroutine.integration;

/**
 * Port (OCP/ISP): contrato mínimo da integração Google Calendar.
 * Novas integrações (Gmail, Alexa) seguem o mesmo padrão adapter.
 */
public interface GoogleCalendarPort {

    /**
     * Troca o authorization code por refresh_token e persiste criptografado.
     */
    void connect(String userId, String authCode);

    /**
     * Importa eventos dos próximos 7 dias como lembretes {@code source=google}.
     *
     * @return quantidade de eventos upserted
     */
    int syncEvents(String userId);
}
