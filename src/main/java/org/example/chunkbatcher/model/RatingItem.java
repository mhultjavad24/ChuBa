package org.example.chunkbatcher.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single RatingItem containing customerInfo and multiple statements.
 * This is the individual chunk item processed by the JSR-352 batch job.
 */
public class RatingItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private CustomerInfo customerInfo;
    private List<Statement> statements = new ArrayList<>();

    public RatingItem() {
    }

    public RatingItem(CustomerInfo customerInfo, List<Statement> statements) {
        this.customerInfo = customerInfo;
        this.statements = statements != null ? statements : new ArrayList<>();
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements != null ? statements : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingItem that = (RatingItem) o;
        return Objects.equals(customerInfo, that.customerInfo) &&
                Objects.equals(statements, that.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerInfo, statements);
    }

    @Override
    public String toString() {
        return "RatingItem{" +
                "customerInfo=" + customerInfo +
                ", statements=" + statements +
                '}';
    }
}
