package com.myroutine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SupabaseProperties.class)
public class SupabaseConfig {

    @Bean
    WebClient supabaseWebClient(SupabaseProperties properties) {
        String baseUrl = properties.getUrl().endsWith("/")
                ? properties.getUrl() + "rest/v1/"
                : properties.getUrl() + "/rest/v1/";

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("apikey", properties.getServiceRoleKey())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceRoleKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
