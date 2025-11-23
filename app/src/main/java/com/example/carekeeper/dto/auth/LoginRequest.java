package com.example.carekeeper.dto.auth;

public class LoginRequest {
    private String email;
    private String password; // ⬅️ aqui

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
