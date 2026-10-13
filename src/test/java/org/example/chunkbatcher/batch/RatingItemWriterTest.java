package org.example.chunkbatcher.batch;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.example.chunkbatcher.model.CustomerInfo;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.RatingsContainer;
import org.example.chunkbatcher.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RatingItemWriterTest {

    @TempDir
    Path tempDir;

    private RatingItemWriter writer;
    private Path outputPath;

    @BeforeEach
    void setUp() {
        outputPath = tempDir.resolve("nested/dir/output-ratings.json");
        writer = new RatingItemWriter(outputPath.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        writer.close();
    }

    @Test
    @DisplayName("Writes a single chunk of items and creates valid finalized JSON file")
    void testWriteSingleChunk() throws Exception {
        writer.open(null);

        RatingItem item = new RatingItem(
                new CustomerInfo(123L, "Customer A"),
                new ArrayList<>(List.of(
                        new Statement(456L, "APPROVED", BigDecimal.valueOf(100.0), "USD", "Details 456", "2026-10-13T10:00:00Z")
                ))
        );

        writer.writeItems(List.of(item));
        writer.close();

        assertThat(Files.exists(outputPath)).isTrue();

        try (InputStream is = Files.newInputStream(outputPath);
             Jsonb jsonb = JsonbBuilder.create()) {
            RatingsContainer container = jsonb.fromJson(is, RatingsContainer.class);
            assertThat(container).isNotNull();
            assertThat(container.getRatings()).hasSize(1);
            RatingItem savedItem = container.getRatings().get(0);
            assertThat(savedItem.getCustomerInfo().getId()).isEqualTo(123L);
            assertThat(savedItem.getStatements()).hasSize(1);
            assertThat(savedItem.getStatements().get(0).getId()).isEqualTo(456L);
            assertThat(savedItem.getStatements().get(0).getStatus()).isEqualTo("APPROVED");
        }
    }

    @Test
    @DisplayName("Accumulates items across multiple chunks into a single finalized JSON array")
    void testWriteMultipleChunks() throws Exception {
        writer.open(null);

        RatingItem chunk1Item = new RatingItem(
                new CustomerInfo(1L, "First"),
                List.of(new Statement(10L))
        );
        RatingItem chunk2Item = new RatingItem(
                new CustomerInfo(2L, "Second"),
                List.of(new Statement(20L))
        );

        writer.writeItems(List.of(chunk1Item));
        assertThat(writer.checkpointInfo()).isEqualTo(1);

        writer.writeItems(List.of(chunk2Item));
        assertThat(writer.checkpointInfo()).isEqualTo(2);

        writer.close();

        try (InputStream is = Files.newInputStream(outputPath);
             Jsonb jsonb = JsonbBuilder.create()) {
            RatingsContainer container = jsonb.fromJson(is, RatingsContainer.class);
            assertThat(container.getRatings()).hasSize(2);
            assertThat(container.getRatings().get(0).getCustomerInfo().getId()).isEqualTo(1L);
            assertThat(container.getRatings().get(1).getCustomerInfo().getId()).isEqualTo(2L);
        }
    }

    @Test
    @DisplayName("Handles empty chunk list gracefully")
    void testWriteEmptyChunk() throws Exception {
        writer.open(null);
        writer.writeItems(List.of());
        writer.close();

        assertThat(writer.getWrittenItems()).isEmpty();
    }
}
