package org.example.chunkbatcher.client;

import org.example.chunkbatcher.model.StatementMockData;

/**
 * Client interface for retrieving statement data.
 */
public interface StatementApiClient {

    /**
     * Fetches mock/enriched data for the given statement ID.
     *
     * @param statementId the ID of the statement to fetch data for
     * @return the retrieved statement mock data
     * @throws StatementApiException if the data cannot be retrieved
     */
    StatementMockData fetchStatementData(Long statementId);
}
