package com.myroutine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url = "";
    private String serviceRoleKey = "";
    private String jwtSecret = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
