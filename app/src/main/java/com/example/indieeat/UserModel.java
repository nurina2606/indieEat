package com.example.indieeat;

public class UserModel {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String role;

    public UserModel() {
        // Required for Firebase
    }

    public UserModel(String username, String email, String phone, String password, String role) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}
