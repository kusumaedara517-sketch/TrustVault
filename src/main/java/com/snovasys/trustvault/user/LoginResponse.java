package com.snovasys.trustvault.user;

public class LoginResponse {

    private String message;
    private String email;
    private Role role;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String message, String email, Role role, String token) {
        this.message = message;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}