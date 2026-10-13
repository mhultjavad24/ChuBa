package org.example.chunkbatcher;

import com.sun.net.httpserver.HttpServer;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.example.chunkbatcher.batch.BatchJobLauncher;
import org.example.chunkbatcher.model.RatingItem;
import org.example.chunkbatcher.model.RatingsContainer;
import org.example.chunkbatcher.model.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RatingsBatchJobIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Runs full JSR-352 batch job end-to-end with mock mode")
    void testRunBatchJobEndToEndMockMode() throws Exception {
        URL resourceUrl = getClass().getResource("/sample-ratings.json");
        assertThat(resourceUrl).isNotNull();

        Path outputPath = tempDir.resolve("finalized-ratings-mock.json");

        BatchJobLauncher launcher = new BatchJobLauncher();
        long executionId = launcher.startJob(
                resourceUrl.toExternalForm(),
                outputPath.toString(),
                "mock"
        );

        JobExecution execution = launcher.waitForCompletion(executionId, 10000);
        assertThat(execution).isNotNull();
        assertThat(execution.getBatchStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(Files.exists(outputPath)).isTrue();

        // Verify Step metrics
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        assertThat(stepExecutions).isNotEmpty();
        StepExecution stepExecution = stepExecutions.get(0);
        assertThat(stepExecution.getStepName()).isEqualTo("enrichRatingsStep");
        assertThat(stepExecution.getBatchStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify finalized output JSON content
        try (InputStream is = Files.newInputStream(outputPath);
             Jsonb jsonb = JsonbBuilder.create()) {
            RatingsContainer container = jsonb.fromJson(is, RatingsContainer.class);
            assertThat(container).isNotNull();
            assertThat(container.getRatings()).hasSize(2);

            RatingItem customer123 = container.getRatings().get(0);
            assertThat(customer123.getCustomerInfo().getId()).isEqualTo(123L);
            assertThat(customer123.getStatements()).hasSize(2);

            Statement s1 = customer123.getStatements().get(0);
            assertThat(s1.getId()).isEqualTo(456L);
            assertThat(s1.getStatus()).isEqualTo("PROCESSED");
            assertThat(s1.getAmount()).isNotNull();
            assertThat(s1.getCurrency()).isEqualTo("USD");

            Statement s2 = customer123.getStatements().get(1);
            assertThat(s2.getId()).isEqualTo(789L);
            assertThat(s2.getStatus()).isEqualTo("PROCESSED");

            RatingItem customer124 = container.getRatings().get(1);
            assertThat(customer124.getCustomerInfo().getId()).isEqualTo(124L);
            assertThat(customer124.getStatements()).hasSize(2);
        }
    }

    @Test
    @DisplayName("Runs full JSR-352 batch job end-to-end with a live HTTP mock server")
    void testRunBatchJobWithLiveHttpServer() throws Exception {
        // Start lightweight mock HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/statements", exchange -> {
            String path = exchange.getRequestURI().getPath();
            long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
            String responseBody = "{"
                    + "\"statementId\":" + id + ","
                    + "\"status\":\"REMOTE_VERIFIED\","
                    + "\"amount\":" + (id * 2.5) + ","
                    + "\"currency\":\"EUR\","
                    + "\"details\":\"Retrieved via HTTP for statement " + id + "\","
                    + "\"retrievedAt\":\"2026-10-13T15:30:00Z\""
                    + "}";
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        String apiBaseUrl = "http://localhost:" + port + "/statements";

        try {
            URL resourceUrl = getClass().getResource("/sample-ratings.json");
            Path outputPath = tempDir.resolve("finalized-ratings-http.json");

            BatchJobLauncher launcher = new BatchJobLauncher();
            long executionId = launcher.startJob(
                    resourceUrl.toExternalForm(),
                    outputPath.toString(),
                    apiBaseUrl
            );

            JobExecution execution = launcher.waitForCompletion(executionId, 10000);
            assertThat(execution.getBatchStatus()).isEqualTo(BatchStatus.COMPLETED);

            try (InputStream is = Files.newInputStream(outputPath);
                 Jsonb jsonb = JsonbBuilder.create()) {
                RatingsContainer container = jsonb.fromJson(is, RatingsContainer.class);
                assertThat(container.getRatings()).hasSize(2);

                Statement s1 = container.getRatings().get(0).getStatements().get(0);
                assertThat(s1.getId()).isEqualTo(456L);
                assertThat(s1.getStatus()).isEqualTo("REMOTE_VERIFIED");
                assertThat(s1.getCurrency()).isEqualTo("EUR");
                assertThat(s1.getDetails()).contains("Retrieved via HTTP for statement 456");
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Handles empty ratings input file cleanly")
    void testRunBatchJobEmptyInput() throws Exception {
        URL resourceUrl = getClass().getResource("/empty-ratings.json");
        Path outputPath = tempDir.resolve("finalized-ratings-empty.json");

        BatchJobLauncher launcher = new BatchJobLauncher();
        long executionId = launcher.startJob(
                resourceUrl.toExternalForm(),
                outputPath.toString(),
                "mock"
        );

        JobExecution execution = launcher.waitForCompletion(executionId, 10000);
        assertThat(execution.getBatchStatus()).isEqualTo(BatchStatus.COMPLETED);

        try (InputStream is = Files.newInputStream(outputPath);
             Jsonb jsonb = JsonbBuilder.create()) {
            RatingsContainer container = jsonb.fromJson(is, RatingsContainer.class);
            assertThat(container.getRatings()).isEmpty();
        }
    }

    @Test
    @DisplayName("Batch job marks execution as FAILED when input file does not exist")
    void testRunBatchJobFailsOnInvalidInput() throws Exception {
        Path outputPath = tempDir.resolve("finalized-ratings-fail.json");

        BatchJobLauncher launcher = new BatchJobLauncher();
        long executionId = launcher.startJob(
                "file:/non/existent/file.json",
                outputPath.toString(),
                "mock"
        );

        JobExecution execution = launcher.waitForCompletion(executionId, 10000);
        assertThat(execution.getBatchStatus()).isEqualTo(BatchStatus.FAILED);
    }
}
