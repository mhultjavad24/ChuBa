package org.example.chunkbatcher.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data retrieved from the external HTTP endpoint for a given statement ID.
 */
public class StatementMockData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long statementId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String details;
    private String retrievedAt;

    public StatementMockData() {
    }

    public StatementMockData(Long statementId, String status, BigDecimal amount, String currency, String details, String retrievedAt) {
        this.statementId = statementId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.details = details;
        this.retrievedAt = retrievedAt;
    }

    public Long getStatementId() {
        return statementId;
    }

    public void setStatementId(Long statementId) {
        this.statementId = statementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(String retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementMockData that = (StatementMockData) o;
        return Objects.equals(statementId, that.statementId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(details, that.details) &&
                Objects.equals(retrievedAt, that.retrievedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statementId, status, amount, currency, details, retrievedAt);
    }

    @Override
    public String toString() {
        return "StatementMockData{" +
                "statementId=" + statementId +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", details='" + details + '\'' +
                ", retrievedAt='" + retrievedAt + '\'' +
                '}';
    }
}
