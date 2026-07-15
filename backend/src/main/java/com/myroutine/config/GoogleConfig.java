package com.myroutine.config;

import com.myroutine.integration.TokenCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleProperties.class)
public class GoogleConfig {

    @Bean
    TokenCipher tokenCipher(GoogleProperties properties) {
        return new TokenCipher(properties.getTokenEncryptionKey());
    }
}
