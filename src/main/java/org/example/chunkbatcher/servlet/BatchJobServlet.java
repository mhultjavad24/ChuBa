package org.example.chunkbatcher.servlet;

import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.chunkbatcher.batch.BatchJobLauncher;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet endpoint allowing clients to trigger the JSR-352 batch job
 * and query job execution status.
 */
@WebServlet(name = "batchJobServlet", urlPatterns = {"/batch", "/batch/*"})
public class BatchJobServlet extends HttpServlet {

    @Inject
    private BatchJobLauncher batchJobLauncher;

    @Override
    public void init() {
        if (batchJobLauncher == null) {
            batchJobLauncher = new BatchJobLauncher();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if ("status".equalsIgnoreCase(action)) {
            handleStatus(request, out);
        } else if ("start".equalsIgnoreCase(action)) {
            handleStart(request, out);
        } else {
            out.println("{\"message\":\"JSR-352 Ratings Batch Service\","
                    + "\"endpoints\":{"
                    + "\"start\":\"/batch?action=start&inputUrl=<URL>&outputPath=<PATH>&apiBaseUrl=<API>\","
                    + "\"status\":\"/batch?action=status&executionId=<ID>\""
                    + "}}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        handleStart(request, out);
    }

    private void handleStart(HttpServletRequest request, PrintWriter out) {
        String inputUrl = request.getParameter("inputUrl");
        String outputPath = request.getParameter("outputPath");
        String apiBaseUrl = request.getParameter("apiBaseUrl");

        if (inputUrl == null || inputUrl.isBlank()) {
            out.println("{\"error\":\"Parameter 'inputUrl' is required.\"}");
            return;
        }

        try {
            long executionId = batchJobLauncher.startJob(inputUrl, outputPath, apiBaseUrl);
            out.println("{\"success\":true,\"executionId\":" + executionId + ",\"jobName\":\"" + BatchJobLauncher.JOB_NAME + "\"}");
        } catch (Exception e) {
            out.println("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleStatus(HttpServletRequest request, PrintWriter out) {
        String idStr = request.getParameter("executionId");
        if (idStr == null || idStr.isBlank()) {
            out.println("{\"error\":\"Parameter 'executionId' is required.\"}");
            return;
        }

        try {
            long executionId = Long.parseLong(idStr.trim());
            BatchStatus status = batchJobLauncher.getStatus(executionId);
            out.println("{\"executionId\":" + executionId + ",\"status\":\"" + (status != null ? status.name() : "UNKNOWN") + "\"}");
        } catch (NumberFormatException e) {
            out.println("{\"error\":\"Invalid executionId: " + idStr + "\"}");
        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    public void setBatchJobLauncher(BatchJobLauncher batchJobLauncher) {
        this.batchJobLauncher = batchJobLauncher;
    }
}
