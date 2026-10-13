package org.example.chunkbatcher;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.example.chunkbatcher.batch.BatchJobLauncher;

/**
 * Standalone command-line runner for the JSR-352 ratings batch job.
 */
public class RatingsBatchMain {

    public static void main(String[] args) throws Exception {
        String inputUrl = args.length > 0 ? args[0] : "classpath:/sample-ratings.json";
        String outputPath = args.length > 1 ? args[1] : "target/finalized-ratings.json";
        String apiBaseUrl = args.length > 2 ? args[2] : "mock";

        System.out.println("Starting Ratings Batch Job via Standalone Runner...");
        System.out.println("  Input URL:    " + inputUrl);
        System.out.println("  Output Path:  " + outputPath);
        System.out.println("  API Base URL: " + apiBaseUrl);

        BatchJobLauncher launcher = new BatchJobLauncher();
        long executionId = launcher.startJob(inputUrl, outputPath, apiBaseUrl);

        JobExecution execution = launcher.waitForCompletion(executionId, 30000);
        System.out.println("Batch Job finished with status: " + execution.getBatchStatus());

        if (execution.getBatchStatus() != BatchStatus.COMPLETED) {
            System.err.println("Job did not complete successfully: " + execution.getBatchStatus());
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
