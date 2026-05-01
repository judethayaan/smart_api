package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.Context;

public abstract class AbstractJsonExceptionMapper {

    @Context
    private UriInfo uriInfo;

    protected Response buildResponse(Response.StatusType status, String message) {
        String path = uriInfo == null || uriInfo.getRequestUri() == null
                ? ""
                : uriInfo.getRequestUri().getPath();

        ErrorResponse payload = new ErrorResponse(
                System.currentTimeMillis(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                message,
                path
        );

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}

