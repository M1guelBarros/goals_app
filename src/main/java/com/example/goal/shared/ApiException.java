package com.example.goal.shared;

import jakarta.ws.rs.core.Response;

/**
 * Domain-aware exception that cleanly maps business rule violations to HTTP responses.
 */
public class ApiException extends RuntimeException {

    private final Response.Status status;

    public ApiException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
