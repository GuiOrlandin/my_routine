package com.myroutine.repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistência de refresh tokens Google (tabela {@code google_tokens}).
 */
public interface GoogleTokenRepository {

    void save(String userId, String encryptedRefreshToken, List<String> scopes);

    Optional<String> findEncryptedRefreshToken(String userId);
}
