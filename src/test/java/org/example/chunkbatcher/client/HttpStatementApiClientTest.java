package org.example.chunkbatcher.client;

import com.sun.net.httpserver.HttpServer;
import org.example.chunkbatcher.model.StatementMockData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpStatementApiClientTest {

    private static HttpServer mockHttpServer;
    private static String serverBaseUrl;

    @BeforeAll
    static void startMockServer() throws IOException {
        // Bind to ephemeral port on localhost
        mockHttpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        mockHttpServer.createContext("/api/statements", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            if ("456".equals(idStr)) {
                String responseBody = "{"
                        + "\"statementId\":456,"
                        + "\"status\":\"VERIFIED\","
                        + "\"amount\":500.25,"
                        + "\"currency\":\"EUR\","
                        + "\"details\":\"Real HTTP mock response for 456\","
                        + "\"retrievedAt\":\"2026-10-13T12:00:00Z\""
                        + "}";
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else if ("999".equals(idStr)) {
                String responseBody = "{\"error\":\"Statement not found\"}";
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String responseBody = "{\"error\":\"Server error\"}";
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        mockHttpServer.start();
        int port = mockHttpServer.getAddress().getPort();
        serverBaseUrl = "http://localhost:" + port + "/api/statements";
    }

    @AfterAll
    static void stopMockServer() {
        if (mockHttpServer != null) {
            mockHttpServer.stop(0);
        }
    }

    @Test
    @DisplayName("Successfully fetches and deserializes statement data over a live HTTP connection")
    void testRealHttpCallSuccess() {
        HttpStatementApiClient client = new HttpStatementApiClient(serverBaseUrl);
        StatementMockData data = client.fetchStatementData(456L);

        assertThat(data).isNotNull();
        assertThat(data.getStatementId()).isEqualTo(456L);
        assertThat(data.getStatus()).isEqualTo("VERIFIED");
        assertThat(data.getAmount()).isEqualTo(BigDecimal.valueOf(500.25));
        assertThat(data.getCurrency()).isEqualTo("EUR");
        assertThat(data.getDetails()).isEqualTo("Real HTTP mock response for 456");
        assertThat(data.getRetrievedAt()).isEqualTo("2026-10-13T12:00:00Z");
    }

    @Test
    @DisplayName("Throws StatementApiException on HTTP 404 response")
    void testHttp404Error() {
        HttpStatementApiClient client = new HttpStatementApiClient(serverBaseUrl);
        assertThatThrownBy(() -> client.fetchStatementData(999L))
                .isInstanceOf(StatementApiException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("Throws StatementApiException on HTTP 500 response")
    void testHttp500Error() {
        HttpStatementApiClient client = new HttpStatementApiClient(serverBaseUrl);
        assertThatThrownBy(() -> client.fetchStatementData(888L))
                .isInstanceOf(StatementApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("Returns deterministic mock data when in mock fallback mode")
    void testMockFallbackMode() {
        HttpStatementApiClient client = new HttpStatementApiClient("mock");
        StatementMockData data = client.fetchStatementData(123L);

        assertThat(data).isNotNull();
        assertThat(data.getStatementId()).isEqualTo(123L);
        assertThat(data.getStatus()).isEqualTo("PROCESSED");
        assertThat(data.getAmount()).isNotNull();
        assertThat(data.getCurrency()).isEqualTo("USD");
        assertThat(data.getDetails()).contains("123");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when statementId is null")
    void testNullStatementId() {
        HttpStatementApiClient client = new HttpStatementApiClient("mock");
        assertThatThrownBy(() -> client.fetchStatementData(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
