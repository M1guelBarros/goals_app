package com.example.goal.shared;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Converts domain exceptions into consistent JSON API errors.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        String code = exception.getStatus().name();
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(code, exception.getMessage()))
                .build();
    }
}
