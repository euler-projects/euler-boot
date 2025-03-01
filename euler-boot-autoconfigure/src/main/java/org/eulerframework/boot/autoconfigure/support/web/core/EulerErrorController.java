package org.eulerframework.boot.autoconfigure.support.web.core;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eulerframework.web.core.exception.web.SystemWebError;
import org.eulerframework.web.core.exception.web.WebException;
import org.eulerframework.web.servlet.error.ErrorResponseResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class EulerErrorController extends AbstractErrorController {
    private final ErrorProperties errorProperties;
    private final ErrorAttributes errorAttributes;
    private final ErrorResponseResolver errorResponseResolver;

    /**
     * Create a new {@link EulerErrorController} instance.
     * @param errorAttributes the error attributes
     * @param errorProperties configuration properties
     */
    public EulerErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties) {
        this(errorAttributes, errorProperties, Collections.emptyList(), new DefaultErrorResponseResolver());
    }

    /**
     * Create a new {@link EulerErrorController} instance.
     * @param errorAttributes the error attributes
     * @param errorProperties configuration properties
     * @param errorViewResolvers error view resolvers
     */
    @Autowired
    public EulerErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties,
                                List<ErrorViewResolver> errorViewResolvers,
                                ErrorResponseResolver errorResponseResolver) {
        super(errorAttributes, errorViewResolvers);
        Assert.notNull(errorProperties, "ErrorProperties must not be null");
        this.errorProperties = errorProperties;
        this.errorAttributes = errorAttributes;
        this.errorResponseResolver = errorResponseResolver;
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        Map<String, Object> model = Collections
                .unmodifiableMap(getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.TEXT_HTML)));
        response.setStatus(status.value());
        ModelAndView modelAndView = resolveErrorView(request, response, status, model);
        return (modelAndView != null) ? modelAndView : new ModelAndView("error", model);
    }

    @RequestMapping
    public ResponseEntity<?> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Map<String, Object> model = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
        model.put("error", SystemWebError.UNDEFINED_ERROR.getReasonPhrase());
        model.put("status", SystemWebError.UNDEFINED_ERROR.value());
        return new ResponseEntity<>(
                this.errorResponseResolver.resolveErrorResponse(request, status, model),
                status);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> mediaTypeNotAcceptable(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        return ResponseEntity.status(status).build();
    }

    @Override
    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            WebRequest webRequest = new ServletWebRequest(request);
            Throwable actualError = this.getActualError(webRequest);
            if (actualError instanceof WebException webException && webException.getStatus() != null) {
                return webException.getStatus();
            }
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    protected ErrorAttributeOptions getErrorAttributeOptions(HttpServletRequest request, MediaType mediaType) {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        if (this.errorProperties.isIncludeException()) {
            options = options.including(ErrorAttributeOptions.Include.EXCEPTION);
        }
        if (isIncludeStackTrace(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.STACK_TRACE);
        }
        if (isIncludeMessage(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.MESSAGE);
        }
        if (isIncludeBindingErrors(request, mediaType)) {
            options = options.including(ErrorAttributeOptions.Include.BINDING_ERRORS);
        }
        options = isIncludePath(request, mediaType) ? options.including(ErrorAttributeOptions.Include.PATH) : options.excluding(ErrorAttributeOptions.Include.PATH);
        return options;
    }

    /**
     * Determine if the stacktrace attribute should be included.
     * @param request the source request
     * @param produces the media type produced (or {@code MediaType.ALL})
     * @return if the stacktrace attribute should be included
     */
    protected boolean isIncludeStackTrace(HttpServletRequest request, MediaType produces) {
        return switch (getErrorProperties().getIncludeStacktrace()) {
            case ALWAYS -> true;
            case ON_PARAM -> getTraceParameter(request);
            case NEVER -> false;
        };
    }

    /**
     * Determine if the message attribute should be included.
     * @param request the source request
     * @param produces the media type produced (or {@code MediaType.ALL})
     * @return if the message attribute should be included
     */
    protected boolean isIncludeMessage(HttpServletRequest request, MediaType produces) {
        return switch (getErrorProperties().getIncludeMessage()) {
            case ALWAYS -> true;
            case ON_PARAM -> getMessageParameter(request);
            case NEVER -> false;
        };
    }

    /**
     * Determine if the errors attribute should be included.
     * @param request the source request
     * @param produces the media type produced (or {@code MediaType.ALL})
     * @return if the errors attribute should be included
     */
    protected boolean isIncludeBindingErrors(HttpServletRequest request, MediaType produces) {
        return switch (getErrorProperties().getIncludeBindingErrors()) {
            case ALWAYS -> true;
            case ON_PARAM -> getErrorsParameter(request);
            case NEVER -> false;
        };
    }

    /**
     * Determine if the path attribute should be included.
     * @param request the source request
     * @param produces the media type produced (or {@code MediaType.ALL})
     * @return if the path attribute should be included
     * @since 3.3.0
     */
    protected boolean isIncludePath(HttpServletRequest request, MediaType produces) {
        return switch (getErrorProperties().getIncludePath()) {
            case ALWAYS -> true;
            case ON_PARAM -> getPathParameter(request);
            case NEVER -> false;
        };
    }

    /**
     * Provide access to the error properties.
     * @return the error properties
     */
    protected ErrorProperties getErrorProperties() {
        return this.errorProperties;
    }

    private Throwable getActualError(WebRequest webRequest) {
        Throwable error = this.errorAttributes.getError(webRequest);
        if (error != null) {
            while (error instanceof ServletException && error.getCause() != null) {
                error = error.getCause();
            }
        }
        return error;
    }
}
