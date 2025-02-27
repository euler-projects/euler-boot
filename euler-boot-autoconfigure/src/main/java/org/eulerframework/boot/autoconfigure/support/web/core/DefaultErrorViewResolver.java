package org.eulerframework.boot.autoconfigure.support.web.core;

import jakarta.servlet.http.HttpServletRequest;
import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.eulerframework.web.core.base.response.ErrorResponse;
import org.eulerframework.web.servlet.error.ErrorResponseResolver;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public class DefaultErrorViewResolver extends PageSupportWebController implements ErrorViewResolver {
    private final ErrorResponseResolver errorResponseResolver;

    public DefaultErrorViewResolver(PageRender pageRender, ErrorResponseResolver errorResponseResolver) {
        super(pageRender);
        this.errorResponseResolver = errorResponseResolver;
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ErrorResponse errorResponse = this.errorResponseResolver.resolveErrorResponse(request, status, model);
        return this.error(errorResponse);
    }
}
