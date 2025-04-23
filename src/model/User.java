package model;

public class User {
    private int id;
    private String username;
    private String password; // Hashed password
    private Role role;
    private boolean locked;
    private int failedAttempts; // Added for login failure tracking

    // Enum for Role
    public enum Role {
        USER, ADMIN
    }

    // Constructors
    public User() {}

    public User(int id, String username, String password, Role role, boolean locked, int failedAttempts) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.locked = locked;
        this.failedAttempts = failedAttempts;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", locked=" + locked +
                '}';
    }
}