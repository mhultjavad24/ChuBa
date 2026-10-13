package org.example.chunkbatcher.batch;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service to launch and monitor the JSR-352 ratings batch job.
 */
@ApplicationScoped
public class BatchJobLauncher {

    private static final Logger LOGGER = Logger.getLogger(BatchJobLauncher.class.getName());
    public static final String JOB_NAME = "ratings-batch-job";

    /**
     * Starts the ratings batch job with the specified parameters.
     *
     * @param inputUrl   URL of the source JSON file
     * @param outputPath Path where the finalized JSON file should be saved (optional)
     * @param apiBaseUrl Base URL of the statement enrichment API (optional, defaults to mock)
     * @return the execution ID of the started batch job
     */
    public long startJob(String inputUrl, String outputPath, String apiBaseUrl) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParameters = new Properties();

        if (inputUrl != null && !inputUrl.isBlank()) {
            jobParameters.setProperty("inputUrl", inputUrl);
        }
        if (outputPath != null && !outputPath.isBlank()) {
            jobParameters.setProperty("outputPath", outputPath);
        }
        if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
            jobParameters.setProperty("apiBaseUrl", apiBaseUrl);
        }

        LOGGER.info(() -> "Starting batch job '" + JOB_NAME + "' with parameters: " + jobParameters);
        long executionId = jobOperator.start(JOB_NAME, jobParameters);
        LOGGER.info(() -> "Batch job '" + JOB_NAME + "' started with execution ID: " + executionId);
        return executionId;
    }

    /**
     * Retrieves the current BatchStatus for an execution ID.
     */
    public BatchStatus getStatus(long executionId) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        return jobExecution != null ? jobExecution.getBatchStatus() : null;
    }

    /**
     * Retrieves the JobExecution for an execution ID.
     */
    public JobExecution getJobExecution(long executionId) {
        return BatchRuntime.getJobOperator().getJobExecution(executionId);
    }

    /**
     * Waits for job execution to complete (reach a terminal state: COMPLETED, FAILED, STOPPED, ABANDONED).
     */
    public JobExecution waitForCompletion(long executionId, long timeoutMillis) throws InterruptedException {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            JobExecution execution = jobOperator.getJobExecution(executionId);
            if (execution != null) {
                BatchStatus status = execution.getBatchStatus();
                if (status == BatchStatus.COMPLETED ||
                        status == BatchStatus.FAILED ||
                        status == BatchStatus.STOPPED ||
                        status == BatchStatus.ABANDONED) {
                    return execution;
                }
            }
            Thread.sleep(100);
        }
        return jobOperator.getJobExecution(executionId);
    }
}
