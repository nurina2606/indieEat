package com.example.indieeat;

public class ReportModel {
    private String username;
    private String email;
    private String role;

    public ReportModel() {
        // Required empty constructor for Firebase
    }

    public ReportModel(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
