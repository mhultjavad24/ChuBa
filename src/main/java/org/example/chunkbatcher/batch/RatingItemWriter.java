package org.example.chunkbatcher.batch;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.RatingsContainer;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSR-352 ItemWriter that saves chunk-processed RatingItem records into a finalized JSON file.
 */
@Named("ratingItemWriter")
@Dependent
public class RatingItemWriter extends AbstractItemWriter {

    private static final Logger LOGGER = Logger.getLogger(RatingItemWriter.class.getName());
    public static final String DEFAULT_OUTPUT_PATH = "target/finalized-ratings.json";

    @Inject
    @BatchProperty(name = "outputPath")
    private String outputPath;

    @Inject
    private JobContext jobContext;

    private final List<RatingItem> writtenItems = new ArrayList<>();
    private Path resolvedDestination;

    public RatingItemWriter() {
    }

    public RatingItemWriter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        String pathStr = resolveOutputPath();
        this.resolvedDestination = Paths.get(pathStr);
        LOGGER.info(() -> "Opening RatingItemWriter for output file: " + resolvedDestination.toAbsolutePath());

        Path parent = resolvedDestination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        if (checkpoint instanceof Integer savedCount) {
            LOGGER.info(() -> "Resuming RatingItemWriter from checkpoint, previously written items: " + savedCount);
        } else {
            writtenItems.clear();
        }
    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
        if (items == null || items.isEmpty()) {
            return;
        }

        LOGGER.info(() -> "Writing chunk of " + items.size() + " items to output buffer");
        for (Object item : items) {
            if (item instanceof RatingItem ratingItem) {
                writtenItems.add(ratingItem);
            } else {
                LOGGER.warning(() -> "Skipping non-RatingItem object in writer: " + item);
            }
        }

        // Persist chunk to finalized JSON file
        flushToFile();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return writtenItems.size();
    }

    @Override
    public void close() throws Exception {
        LOGGER.info(() -> "Finalizing RatingItemWriter. Total items saved: " + writtenItems.size()
                + " at: " + (resolvedDestination != null ? resolvedDestination.toAbsolutePath() : "unknown"));
        flushToFile();
    }

    private void flushToFile() {
        if (resolvedDestination == null) {
            return;
        }

        try {
            Path parent = resolvedDestination.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            JsonbConfig config = new JsonbConfig().withFormatting(true);
            try (Jsonb jsonb = JsonbBuilder.create(config);
                 BufferedWriter writer = Files.newBufferedWriter(resolvedDestination, StandardCharsets.UTF_8)) {
                RatingsContainer container = new RatingsContainer(writtenItems);
                jsonb.toJson(container, writer);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to write finalized ratings JSON to " + resolvedDestination, e);
            throw new BatchRuntimeException("Failed to write finalized ratings JSON: " + e.getMessage(), e);
        }
    }

    private String resolveOutputPath() {
        if (outputPath != null && !outputPath.isBlank()) {
            return outputPath.trim();
        }
        if (jobContext != null && jobContext.getProperties() != null) {
            String prop = jobContext.getProperties().getProperty("outputPath");
            if (prop != null && !prop.isBlank()) {
                return prop.trim();
            }
        }
        return DEFAULT_OUTPUT_PATH;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public List<RatingItem> getWrittenItems() {
        return new ArrayList<>(writtenItems);
    }

    public Path getResolvedDestination() {
        return resolvedDestination;
    }
}
