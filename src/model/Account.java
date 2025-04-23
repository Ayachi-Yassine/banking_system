package model;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Or java.sql.Timestamp

public class Account {
    private int id;
    private int userId;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    // Constructors
    public Account() {}

    public Account(int id, int userId, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public Account(int userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }


    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", userId=" + userId +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                '}';
    }
}