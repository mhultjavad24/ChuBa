package org.example.chunkbatcher.batch;

import jakarta.batch.operations.BatchRuntimeException;
import org.example.chunkbatcher.model.RatingItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingItemReaderTest {

    private RatingItemReader reader;

    @BeforeEach
    void setUp() {
        reader = new RatingItemReader();
    }

    @AfterEach
    void tearDown() throws Exception {
        reader.close();
    }

    @Test
    @DisplayName("Successfully reads all RatingItems from a valid JSON file URL")
    void testReadItemsSuccess() throws Exception {
        URL url = getClass().getResource("/sample-ratings.json");
        assertThat(url).isNotNull();

        reader.setInputUrl(url.toExternalForm());
        reader.open(null);

        assertThat(reader.getTotalItems()).isEqualTo(2);

        RatingItem item1 = (RatingItem) reader.readItem();
        assertThat(item1).isNotNull();
        assertThat(item1.getCustomerInfo().getId()).isEqualTo(123L);
        assertThat(item1.getStatements()).hasSize(2);
        assertThat(item1.getStatements().get(0).getId()).isEqualTo(456L);
        assertThat(item1.getStatements().get(1).getId()).isEqualTo(789L);

        RatingItem item2 = (RatingItem) reader.readItem();
        assertThat(item2).isNotNull();
        assertThat(item2.getCustomerInfo().getId()).isEqualTo(124L);
        assertThat(item2.getStatements()).hasSize(2);

        // Third read should return null signaling EOF
        Object item3 = reader.readItem();
        assertThat(item3).isNull();
    }

    @Test
    @DisplayName("Supports checkpoint and restart by resuming from saved index")
    void testCheckpointAndResume() throws Exception {
        URL url = getClass().getResource("/sample-ratings.json");
        reader.setInputUrl(url.toExternalForm());

        // Open with checkpoint at index 1 (skip first item)
        reader.open(1);

        assertThat(reader.checkpointInfo()).isEqualTo(1);

        RatingItem item = (RatingItem) reader.readItem();
        assertThat(item).isNotNull();
        assertThat(item.getCustomerInfo().getId()).isEqualTo(124L);

        assertThat(reader.readItem()).isNull();
    }

    @Test
    @DisplayName("Handles empty ratings array gracefully")
    void testEmptyRatings() throws Exception {
        URL url = getClass().getResource("/empty-ratings.json");
        reader.setInputUrl(url.toExternalForm());
        reader.open(null);

        assertThat(reader.getTotalItems()).isZero();
        assertThat(reader.readItem()).isNull();
    }

    @Test
    @DisplayName("Throws BatchRuntimeException when inputUrl is null or blank")
    void testMissingInputUrl() {
        reader.setInputUrl(null);
        assertThatThrownBy(() -> reader.open(null))
                .isInstanceOf(BatchRuntimeException.class)
                .hasMessageContaining("inputUrl");

        reader.setInputUrl("   ");
        assertThatThrownBy(() -> reader.open(null))
                .isInstanceOf(BatchRuntimeException.class)
                .hasMessageContaining("inputUrl");
    }

    @Test
    @DisplayName("Throws BatchRuntimeException when input JSON is malformed")
    void testMalformedJson() {
        URL url = getClass().getResource("/malformed-ratings.json");
        reader.setInputUrl(url.toExternalForm());

        assertThatThrownBy(() -> reader.open(null))
                .isInstanceOf(BatchRuntimeException.class)
                .hasMessageContaining("Error reading ratings JSON");
    }

    @Test
    @DisplayName("Throws BatchRuntimeException when file does not exist")
    void testNonExistentFile() {
        reader.setInputUrl("file:/path/does/not/exist/ratings.json");
        assertThatThrownBy(() -> reader.open(null))
                .isInstanceOf(BatchRuntimeException.class);
    }
}
