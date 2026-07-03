package com.myroutine.api.security;

/**
 * Identidade autenticada extraída do JWT Supabase.
 * Usado em controllers via {@code @AuthenticationPrincipal UserPrincipal user}.
 */
public class UserPrincipal {

    private final String id;
    private final String email;

    public UserPrincipal(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
