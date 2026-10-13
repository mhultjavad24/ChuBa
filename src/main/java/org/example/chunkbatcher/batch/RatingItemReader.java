package org.example.chunkbatcher.batch;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.RatingsContainer;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSR-352 ItemReader that reads RatingItem elements from a JSON file located at a provided URL.
 */
@Named("ratingItemReader")
@Dependent
public class RatingItemReader extends AbstractItemReader {

    private static final Logger LOGGER = Logger.getLogger(RatingItemReader.class.getName());

    @Inject
    @BatchProperty(name = "inputUrl")
    private String inputUrl;

    @Inject
    private JobContext jobContext;

    private List<RatingItem> items = new ArrayList<>();
    private int currentIndex = 0;

    public RatingItemReader() {
    }

    public RatingItemReader(String inputUrl) {
        this.inputUrl = inputUrl;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        String resolvedUrl = resolveInputUrl();
        LOGGER.info(() -> "Opening RatingItemReader with URL: " + resolvedUrl);

        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            throw new BatchRuntimeException("Job parameter 'inputUrl' is required but was not provided.");
        }

        try (InputStream inputStream = openStreamFromUrl(resolvedUrl)) {
            if (inputStream == null) {
                throw new BatchRuntimeException("Could not open stream for URL: " + resolvedUrl);
            }
            try (Jsonb jsonb = JsonbBuilder.create()) {
                RatingsContainer container = jsonb.fromJson(inputStream, RatingsContainer.class);
                if (container != null && container.getRatings() != null) {
                    this.items = new ArrayList<>(container.getRatings());
                } else {
                    this.items = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse ratings JSON from " + resolvedUrl, e);
            throw new BatchRuntimeException("Error reading ratings JSON from " + resolvedUrl + ": " + e.getMessage(), e);
        }

        if (checkpoint instanceof Integer savedIndex) {
            this.currentIndex = savedIndex;
            LOGGER.info(() -> "Resuming RatingItemReader from checkpoint offset: " + savedIndex);
        } else {
            this.currentIndex = 0;
        }

        LOGGER.info(() -> "RatingItemReader initialized. Total items to process: " + items.size()
                + ", starting at index: " + currentIndex);
    }

    @Override
    public Object readItem() throws Exception {
        if (currentIndex < items.size()) {
            RatingItem item = items.get(currentIndex);
            currentIndex++;
            return item;
        }
        return null; // Signals end of data to JSR-352 chunk step
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return currentIndex;
    }

    @Override
    public void close() throws Exception {
        LOGGER.fine(() -> "Closing RatingItemReader. Items processed: " + currentIndex);
        items.clear();
        currentIndex = 0;
    }

    private String resolveInputUrl() {
        if (inputUrl != null && !inputUrl.isBlank()) {
            return inputUrl.trim();
        }
        if (jobContext != null && jobContext.getProperties() != null) {
            String prop = jobContext.getProperties().getProperty("inputUrl");
            if (prop != null && !prop.isBlank()) {
                return prop.trim();
            }
        }
        return null;
    }

    private InputStream openStreamFromUrl(String urlString) throws Exception {
        if (urlString.startsWith("classpath:")) {
            String resourcePath = urlString.substring("classpath:".length());
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1));
            }
            return is;
        }

        if (urlString.contains("://") || urlString.startsWith("file:")) {
            return URI.create(urlString).toURL().openStream();
        }

        // Treat as local file path
        Path path = Paths.get(urlString);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }

        // Fallback to URI to URL
        return URI.create(urlString).toURL().openStream();
    }

    public String getInputUrl() {
        return inputUrl;
    }

    public void setInputUrl(String inputUrl) {
        this.inputUrl = inputUrl;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalItems() {
        return items.size();
    }
}
