package org.example.chunkbatcher.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.example.chunkbatcher.model.StatementMockData;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of StatementApiClient.
 * Performs HTTP calls using java.net.http.HttpClient when a valid HTTP/HTTPS URL
 * is configured, or provides mock data fallback when running in mock mode.
 */
@ApplicationScoped
public class HttpStatementApiClient implements StatementApiClient {

    private static final Logger LOGGER = Logger.getLogger(HttpStatementApiClient.class.getName());
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final Jsonb jsonb;
    private String apiBaseUrl;

    public HttpStatementApiClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build(),
                JsonbBuilder.create(),
                "mock");
    }

    public HttpStatementApiClient(String apiBaseUrl) {
        this(HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build(),
                JsonbBuilder.create(),
                apiBaseUrl);
    }

    public HttpStatementApiClient(HttpClient httpClient, Jsonb jsonb, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.jsonb = jsonb;
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public StatementMockData fetchStatementData(Long statementId) {
        if (statementId == null) {
            throw new IllegalArgumentException("statementId cannot be null");
        }

        // Mock mode when URL is not an HTTP endpoint
        if (apiBaseUrl == null || apiBaseUrl.isBlank() || "mock".equalsIgnoreCase(apiBaseUrl.trim())) {
            LOGGER.fine(() -> "Generating mock data for statement ID " + statementId);
            return createDeterministicMockData(statementId);
        }

        // Real HTTP call to external endpoint
        String endpoint = apiBaseUrl.endsWith("/") ? apiBaseUrl + statementId : apiBaseUrl + "/" + statementId;
        LOGGER.fine(() -> "Fetching statement data from HTTP endpoint: " + endpoint);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return jsonb.fromJson(response.body(), StatementMockData.class);
            } else {
                throw new StatementApiException(response.statusCode(),
                        "Failed to fetch statement " + statementId + ", response: " + response.body());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "I/O error fetching statement " + statementId, e);
            throw new StatementApiException("Network error fetching statement data for ID: " + statementId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StatementApiException("Interrupted while fetching statement data for ID: " + statementId, e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error fetching statement " + statementId, e);
            if (e instanceof StatementApiException) {
                throw (StatementApiException) e;
            }
            throw new StatementApiException("Error fetching statement data for ID: " + statementId, e);
        }
    }

    /**
     * Creates deterministic mock data for a statement ID.
     */
    public static StatementMockData createDeterministicMockData(Long statementId) {
        BigDecimal amount = BigDecimal.valueOf(statementId * 12.75).setScale(2, RoundingMode.HALF_UP);
        return new StatementMockData(
                statementId,
                "PROCESSED",
                amount,
                "USD",
                "Mocked HTTP statement data for ID " + statementId,
                Instant.now().toString()
        );
    }
}
