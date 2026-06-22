package org.example.models;

public class User {
    private long user_id;
    private String username;
    private String phone;
    private String passwordHash;
    private String role;
    private String status;
    private boolean blocked;

    public User() {}

    public User(
            int user_id,
            String username,
            String phone,
            String passwordHash,
            String role,
            String status,
            boolean blocked
    ) {
        this.user_id = user_id;
        this.username = username;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.blocked = blocked;
    }

    public long getUser_id() {
        return user_id;
    }

    public String getUsername() {

        return username;
    }

    public String getPhone() {

        return phone;
    }

    public String getPasswordHash() {

        return passwordHash;
    }

    public String getRole() {

        return role;
    }

    public String getStatus() {

        return status;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setUser_id(int user_id) {

        this.user_id = user_id;
    }

    public void setPhone(String phone) {

        this.phone = phone;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {

        this.passwordHash = passwordHash;
    }

    public void setRole(String role) {

        this.role = role;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public void setBlocked(boolean blocked) {

        this.blocked = blocked;
    }
}

