package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper extends AbstractJsonExceptionMapper
        implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response response = exception.getResponse();
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = response.getStatusInfo().getReasonPhrase();
        }

        return buildResponse(response.getStatusInfo(), message);
    }
}

