package org.example.chunkbatcher.client;

/**
 * Exception thrown when fetching statement details fails.
 */
public class StatementApiException extends RuntimeException {
    private final int statusCode;

    public StatementApiException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public StatementApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public StatementApiException(int statusCode, String message) {
        super("HTTP " + statusCode + ": " + message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
