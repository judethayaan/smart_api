package com.smartcampus.exception;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ThrowableExceptionMapper extends AbstractJsonExceptionMapper
        implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(ThrowableExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Unhandled server error", exception);
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred. Please contact the API administrator.");
    }
}

