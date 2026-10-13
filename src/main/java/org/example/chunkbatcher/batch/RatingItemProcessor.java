package org.example.chunkbatcher.batch;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.example.chunkbatcher.client.HttpStatementApiClient;
import org.example.chunkbatcher.client.StatementApiClient;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.Statement;
import org.example.chunkbatcher.model.StatementMockData;

import java.util.logging.Logger;

/**
 * JSR-352 ItemProcessor that processes each RatingItem, enriching its statements
 * with data retrieved from the mock HTTP endpoint.
 */
@Named("ratingItemProcessor")
@Dependent
public class RatingItemProcessor implements ItemProcessor {

    private static final Logger LOGGER = Logger.getLogger(RatingItemProcessor.class.getName());

    @Inject
    private StatementApiClient statementApiClient;

    @Inject
    @BatchProperty(name = "apiBaseUrl")
    private String apiBaseUrl;

    @Inject
    private JobContext jobContext;

    public RatingItemProcessor() {
    }

    public RatingItemProcessor(StatementApiClient statementApiClient) {
        this.statementApiClient = statementApiClient;
    }

    @Override
    public Object processItem(Object item) throws Exception {
        if (item == null) {
            return null;
        }

        if (!(item instanceof RatingItem ratingItem)) {
            LOGGER.warning(() -> "Unexpected item type received by processor: " + item.getClass().getName());
            return null;
        }

        ensureClientInitialized();

        Long customerId = ratingItem.getCustomerInfo() != null ? ratingItem.getCustomerInfo().getId() : null;
        LOGGER.info(() -> "Processing RatingItem for customer: " + customerId + " with "
                + (ratingItem.getStatements() != null ? ratingItem.getStatements().size() : 0) + " statements");

        if (ratingItem.getStatements() != null) {
            for (Statement statement : ratingItem.getStatements()) {
                if (statement != null && statement.getId() != null) {
                    LOGGER.fine(() -> "Fetching mock data for statement ID: " + statement.getId());
                    StatementMockData mockData = statementApiClient.fetchStatementData(statement.getId());
                    statement.enrichWith(mockData);
                }
            }
        }

        return ratingItem;
    }

    private void ensureClientInitialized() {
        String resolvedBaseUrl = resolveApiBaseUrl();
        if (statementApiClient == null) {
            LOGGER.info(() -> "Initializing HttpStatementApiClient with base URL: " + resolvedBaseUrl);
            this.statementApiClient = new HttpStatementApiClient(resolvedBaseUrl);
        } else if (statementApiClient instanceof HttpStatementApiClient httpClientImpl) {
            if (resolvedBaseUrl != null && !resolvedBaseUrl.isBlank()) {
                httpClientImpl.setApiBaseUrl(resolvedBaseUrl);
            }
        }
    }

    private String resolveApiBaseUrl() {
        if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
            return apiBaseUrl.trim();
        }
        if (jobContext != null && jobContext.getProperties() != null) {
            String prop = jobContext.getProperties().getProperty("apiBaseUrl");
            if (prop != null && !prop.isBlank()) {
                return prop.trim();
            }
        }
        return "mock";
    }

    public StatementApiClient getStatementApiClient() {
        return statementApiClient;
    }

    public void setStatementApiClient(StatementApiClient statementApiClient) {
        this.statementApiClient = statementApiClient;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
