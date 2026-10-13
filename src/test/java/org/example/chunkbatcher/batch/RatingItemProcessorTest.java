package org.example.chunkbatcher.batch;

import org.example.chunkbatcher.client.StatementApiClient;
import org.example.chunkbatcher.client.StatementApiException;
import org.example.chunkbatcher.model.CustomerInfo;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.Statement;
import org.example.chunkbatcher.model.StatementMockData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingItemProcessorTest {

    @Mock
    private StatementApiClient statementApiClient;

    private RatingItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RatingItemProcessor(statementApiClient);
    }

    @Test
    @DisplayName("Enriches all statements within a RatingItem using the mocked HTTP client")
    void testProcessItemSuccessfullyEnrichesStatements() throws Exception {
        CustomerInfo customer = new CustomerInfo(123L, "Acme Inc");
        Statement s1 = new Statement(456L);
        Statement s2 = new Statement(789L);
        RatingItem item = new RatingItem(customer, new ArrayList<>(List.of(s1, s2)));

        StatementMockData mockData456 = new StatementMockData(
                456L, "APPROVED", BigDecimal.valueOf(100.50), "USD", "Mock Statement 456", "2026-10-13T20:00:00Z"
        );
        StatementMockData mockData789 = new StatementMockData(
                789L, "VERIFIED", BigDecimal.valueOf(250.75), "USD", "Mock Statement 789", "2026-10-13T20:05:00Z"
        );

        when(statementApiClient.fetchStatementData(456L)).thenReturn(mockData456);
        when(statementApiClient.fetchStatementData(789L)).thenReturn(mockData789);

        Object result = processor.processItem(item);

        assertThat(result).isInstanceOf(RatingItem.class);
        RatingItem enriched = (RatingItem) result;
        assertThat(enriched.getCustomerInfo().getId()).isEqualTo(123L);
        assertThat(enriched.getStatements()).hasSize(2);

        Statement enrichedS1 = enriched.getStatements().get(0);
        assertThat(enrichedS1.getId()).isEqualTo(456L);
        assertThat(enrichedS1.getStatus()).isEqualTo("APPROVED");
        assertThat(enrichedS1.getAmount()).isEqualTo(BigDecimal.valueOf(100.50));
        assertThat(enrichedS1.getCurrency()).isEqualTo("USD");
        assertThat(enrichedS1.getDetails()).isEqualTo("Mock Statement 456");

        Statement enrichedS2 = enriched.getStatements().get(1);
        assertThat(enrichedS2.getId()).isEqualTo(789L);
        assertThat(enrichedS2.getStatus()).isEqualTo("VERIFIED");
        assertThat(enrichedS2.getAmount()).isEqualTo(BigDecimal.valueOf(250.75));

        verify(statementApiClient, times(1)).fetchStatementData(456L);
        verify(statementApiClient, times(1)).fetchStatementData(789L);
        verifyNoMoreInteractions(statementApiClient);
    }

    @Test
    @DisplayName("Handles RatingItem with empty or null statements list safely")
    void testProcessItemWithEmptyStatements() throws Exception {
        CustomerInfo customer = new CustomerInfo(999L);
        RatingItem itemEmpty = new RatingItem(customer, new ArrayList<>());

        Object result = processor.processItem(itemEmpty);
        assertThat(result).isNotNull();
        assertThat(((RatingItem) result).getStatements()).isEmpty();

        RatingItem itemNull = new RatingItem(customer, null);
        Object resultNull = processor.processItem(itemNull);
        assertThat(resultNull).isNotNull();

        verifyNoInteractions(statementApiClient);
    }

    @Test
    @DisplayName("Safely ignores statements with null ID")
    void testProcessItemWithNullStatementId() throws Exception {
        CustomerInfo customer = new CustomerInfo(123L);
        Statement nullIdStatement = new Statement(null);
        RatingItem item = new RatingItem(customer, List.of(nullIdStatement));

        Object result = processor.processItem(item);
        assertThat(result).isNotNull();
        verifyNoInteractions(statementApiClient);
    }

    @Test
    @DisplayName("Propagates StatementApiException when the HTTP client fails")
    void testProcessItemWhenClientFails() {
        CustomerInfo customer = new CustomerInfo(123L);
        Statement s1 = new Statement(456L);
        RatingItem item = new RatingItem(customer, List.of(s1));

        when(statementApiClient.fetchStatementData(456L))
                .thenThrow(new StatementApiException(500, "Internal Server Error"));

        assertThatThrownBy(() -> processor.processItem(item))
                .isInstanceOf(StatementApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("Returns null when input item is null")
    void testProcessNullItem() throws Exception {
        Object result = processor.processItem(null);
        assertThat(result).isNull();
        verifyNoInteractions(statementApiClient);
    }
}
