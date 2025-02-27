package org.eulerframework.boot.autoconfigure.support.web.core;

import jakarta.servlet.http.HttpServletRequest;
import org.eulerframework.web.core.base.response.ErrorResponse;
import org.eulerframework.web.servlet.error.ErrorResponseResolver;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.Map;

public class DefaultErrorResponseResolver implements ErrorResponseResolver {
    @Override
    public ErrorResponse resolveErrorResponse(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        return new ErrorResponse(
                (Date) model.get("timestamp"),
                (String) model.get("error"),
                (int) model.get("status"),
                (String) model.get("message"),
                (String) model.get("exception"),
                (String) model.get("trace"));
    }
}
