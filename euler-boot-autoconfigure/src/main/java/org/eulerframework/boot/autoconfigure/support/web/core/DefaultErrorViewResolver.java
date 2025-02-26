package org.eulerframework.boot.autoconfigure.support.web.core;

import jakarta.servlet.http.HttpServletRequest;
import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.PageSupportWebController;
import org.eulerframework.web.core.base.response.ErrorResponse;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.Map;

public class DefaultErrorViewResolver extends PageSupportWebController implements ErrorViewResolver {
    public DefaultErrorViewResolver(PageRender pageRender) {
        super(pageRender);
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ErrorResponse errorResponse = new ErrorResponse(
                (Date) model.get("timestamp"),
                (String) model.get("error"),
                (int) model.get("status"),
                (String) model.get("message"),
                (String) model.get("exception"),
                (String) model.get("trace"));
        return this.error(errorResponse);
    }
}
