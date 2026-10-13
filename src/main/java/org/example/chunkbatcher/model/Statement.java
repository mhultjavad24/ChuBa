package org.example.chunkbatcher.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a Statement item, which is initialized with an id and subsequently
 * enriched with mock data fetched via HTTP during batch chunk processing.
 */
public class Statement implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String details;
    private String retrievedAt;

    public Statement() {
    }

    public Statement(Long id) {
        this.id = id;
    }

    public Statement(Long id, String status, BigDecimal amount, String currency, String details, String retrievedAt) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.details = details;
        this.retrievedAt = retrievedAt;
    }

    /**
     * Enriches this statement with data retrieved from the mock HTTP endpoint.
     */
    public void enrichWith(StatementMockData data) {
        if (data != null) {
            this.status = data.getStatus();
            this.amount = data.getAmount();
            this.currency = data.getCurrency();
            this.details = data.getDetails();
            this.retrievedAt = data.getRetrievedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        Statement statement = (Statement) o;
        return Objects.equals(id, statement.id) &&
                Objects.equals(status, statement.status) &&
                Objects.equals(amount, statement.amount) &&
                Objects.equals(currency, statement.currency) &&
                Objects.equals(details, statement.details) &&
                Objects.equals(retrievedAt, statement.retrievedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, amount, currency, details, retrievedAt);
    }

    @Override
    public String toString() {
        return "Statement{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", details='" + details + '\'' +
                ", retrievedAt='" + retrievedAt + '\'' +
                '}';
    }
}
