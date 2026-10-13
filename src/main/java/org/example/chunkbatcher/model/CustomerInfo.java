package org.example.chunkbatcher.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents customer information within a RatingItem.
 */
public class CustomerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    public CustomerInfo() {
    }

    public CustomerInfo(Long id) {
        this.id = id;
    }

    public CustomerInfo(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerInfo that = (CustomerInfo) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "CustomerInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
