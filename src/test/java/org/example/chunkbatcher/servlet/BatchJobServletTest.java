package org.example.chunkbatcher.servlet;

import jakarta.batch.runtime.BatchStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.chunkbatcher.batch.BatchJobLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchJobServletTest {

    @Mock
    private BatchJobLauncher batchJobLauncher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private BatchJobServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new BatchJobServlet();
        servlet.setBatchJobLauncher(batchJobLauncher);
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Returns welcome/info JSON when no action parameter is provided")
    void testDoGetDefaultInfo() throws Exception {
        when(request.getParameter("action")).thenReturn(null);

        servlet.doGet(request, response);
        printWriter.flush();

        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("JSR-352 Ratings Batch Service");
    }

    @Test
    @DisplayName("Starts batch job when action=start is called with valid inputUrl")
    void testDoGetStartSuccess() throws Exception {
        when(request.getParameter("action")).thenReturn("start");
        when(request.getParameter("inputUrl")).thenReturn("file:/ratings.json");
        when(request.getParameter("outputPath")).thenReturn("/output.json");
        when(request.getParameter("apiBaseUrl")).thenReturn("mock");
        when(batchJobLauncher.startJob("file:/ratings.json", "/output.json", "mock")).thenReturn(101L);

        servlet.doGet(request, response);
        printWriter.flush();

        assertThat(stringWriter.toString()).contains("\"executionId\":101");
        assertThat(stringWriter.toString()).contains("\"success\":true");
    }

    @Test
    @DisplayName("Returns error when action=start is called without inputUrl")
    void testDoGetStartMissingUrl() throws Exception {
        when(request.getParameter("action")).thenReturn("start");
        when(request.getParameter("inputUrl")).thenReturn(null);

        servlet.doGet(request, response);
        printWriter.flush();

        assertThat(stringWriter.toString()).contains("Parameter 'inputUrl' is required");
        verifyNoInteractions(batchJobLauncher);
    }

    @Test
    @DisplayName("Returns job status when action=status is requested")
    void testDoGetStatusSuccess() throws Exception {
        when(request.getParameter("action")).thenReturn("status");
        when(request.getParameter("executionId")).thenReturn("101");
        when(batchJobLauncher.getStatus(101L)).thenReturn(BatchStatus.COMPLETED);

        servlet.doGet(request, response);
        printWriter.flush();

        assertThat(stringWriter.toString()).contains("\"status\":\"COMPLETED\"");
        assertThat(stringWriter.toString()).contains("\"executionId\":101");
    }

    @Test
    @DisplayName("Handles invalid numeric format for executionId gracefully")
    void testDoGetStatusInvalidId() throws Exception {
        when(request.getParameter("action")).thenReturn("status");
        when(request.getParameter("executionId")).thenReturn("abc");

        servlet.doGet(request, response);
        printWriter.flush();

        assertThat(stringWriter.toString()).contains("Invalid executionId");
    }
}
