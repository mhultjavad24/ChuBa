package org.example.chunkbatcher.rest;

import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.chunkbatcher.batch.BatchJobLauncher;

import java.util.Map;

/**
 * REST API for initiating and monitoring the JSR-352 batch job.
 */
@Path("/batch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BatchJobResource {

    @Inject
    private BatchJobLauncher batchJobLauncher;

    public BatchJobResource() {
    }

    public BatchJobResource(BatchJobLauncher batchJobLauncher) {
        this.batchJobLauncher = batchJobLauncher;
    }

    @POST
    @Path("/start")
    public Response startBatchJob(@QueryParam("inputUrl") String inputUrl,
                                  @QueryParam("outputPath") String outputPath,
                                  @QueryParam("apiBaseUrl") String apiBaseUrl) {
        if (inputUrl == null || inputUrl.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Query parameter 'inputUrl' is required."))
                    .build();
        }

        try {
            ensureLauncher();
            long executionId = batchJobLauncher.startJob(inputUrl, outputPath, apiBaseUrl);
            return Response.ok(Map.of(
                    "success", true,
                    "executionId", executionId,
                    "jobName", BatchJobLauncher.JOB_NAME
            )).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/status/{executionId}")
    public Response getStatus(@PathParam("executionId") long executionId) {
        try {
            ensureLauncher();
            BatchStatus status = batchJobLauncher.getStatus(executionId);
            if (status == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Execution ID not found: " + executionId))
                        .build();
            }
            return Response.ok(Map.of(
                    "executionId", executionId,
                    "status", status.name()
            )).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private void ensureLauncher() {
        if (batchJobLauncher == null) {
            batchJobLauncher = new BatchJobLauncher();
        }
    }
}
