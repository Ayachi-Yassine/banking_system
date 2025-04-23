package model;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Or java.sql.Timestamp

public class Transaction {
    private int id;
    private int accountId;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private Integer relatedAccountId; // Use Integer to allow null

    // Enum for Transaction Type
    public enum TransactionType {
        DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN
    }

    // Constructors
    public Transaction() {}

    public Transaction(int id, int accountId, TransactionType type, BigDecimal amount, LocalDateTime transactionDate, Integer relatedAccountId) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.relatedAccountId = relatedAccountId;
    }

    public Transaction(int accountId, TransactionType type, BigDecimal amount, Integer relatedAccountId) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now(); // Default to now
        this.relatedAccountId = relatedAccountId;
    }


    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getRelatedAccountId() {
        return relatedAccountId;
    }

    public void setRelatedAccountId(Integer relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", type=" + type +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", relatedAccountId=" + relatedAccountId +
                '}';
    }
}