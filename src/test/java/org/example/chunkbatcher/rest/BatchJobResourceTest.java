package org.example.chunkbatcher.rest;

import jakarta.batch.runtime.BatchStatus;
import jakarta.ws.rs.core.Response;
import org.example.chunkbatcher.batch.BatchJobLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchJobResourceTest {

    @Mock
    private BatchJobLauncher batchJobLauncher;

    private BatchJobResource resource;

    @BeforeEach
    void setUp() {
        resource = new BatchJobResource(batchJobLauncher);
    }

    @Test
    @DisplayName("Starts batch job successfully when inputUrl is provided")
    void testStartBatchJobSuccess() {
        when(batchJobLauncher.startJob("file:/data.json", "/output.json", "mock")).thenReturn(42L);

        Response response = resource.startBatchJob("file:/data.json", "/output.json", "mock");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertThat(entity.get("success")).isEqualTo(true);
        assertThat(entity.get("executionId")).isEqualTo(42L);
        assertThat(entity.get("jobName")).isEqualTo(BatchJobLauncher.JOB_NAME);

        verify(batchJobLauncher).startJob("file:/data.json", "/output.json", "mock");
    }

    @Test
    @DisplayName("Returns 400 Bad Request when inputUrl is missing or blank")
    void testStartBatchJobMissingUrl() {
        Response responseNull = resource.startBatchJob(null, null, null);
        assertThat(responseNull.getStatus()).isEqualTo(400);

        Response responseBlank = resource.startBatchJob("   ", null, null);
        assertThat(responseBlank.getStatus()).isEqualTo(400);

        verifyNoInteractions(batchJobLauncher);
    }

    @Test
    @DisplayName("Returns job status for valid execution ID")
    void testGetStatusSuccess() {
        when(batchJobLauncher.getStatus(42L)).thenReturn(BatchStatus.COMPLETED);

        Response response = resource.getStatus(42L);
        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertThat(entity.get("status")).isEqualTo("COMPLETED");
        assertThat(entity.get("executionId")).isEqualTo(42L);
    }

    @Test
    @DisplayName("Returns 404 Not Found when execution ID does not exist")
    void testGetStatusNotFound() {
        when(batchJobLauncher.getStatus(999L)).thenReturn(null);

        Response response = resource.getStatus(999L);
        assertThat(response.getStatus()).isEqualTo(404);
    }
}
