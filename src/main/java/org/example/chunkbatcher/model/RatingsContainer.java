package org.example.chunkbatcher.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root envelope object matching the JSON structure:
 * { "ratings": [ { "customerInfo": { ... }, "statements": [ ... ] } ] }
 */
public class RatingsContainer implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<RatingItem> ratings = new ArrayList<>();

    public RatingsContainer() {
    }

    public RatingsContainer(List<RatingItem> ratings) {
        this.ratings = ratings != null ? ratings : new ArrayList<>();
    }

    public List<RatingItem> getRatings() {
        return ratings;
    }

    public void setRatings(List<RatingItem> ratings) {
        this.ratings = ratings != null ? ratings : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingsContainer that = (RatingsContainer) o;
        return Objects.equals(ratings, that.ratings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratings);
    }

    @Override
    public String toString() {
        return "RatingsContainer{" +
                "ratings=" + ratings +
                '}';
    }
}
