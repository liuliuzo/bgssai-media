package com.bgssai.media.common.auth;

public class AuthUser {
    private Long id;
    private String username;
    private String role_code;

    public AuthUser() {
    }

    public AuthUser(Long id, String username, String roleCode) {
        this.id = id;
        this.username = username;
        this.role_code = roleCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole_code() {
        return role_code;
    }

    public void setRole_code(String role_code) {
        this.role_code = role_code;
    }
}
