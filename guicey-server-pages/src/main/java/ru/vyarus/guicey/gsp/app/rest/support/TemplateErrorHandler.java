package ru.vyarus.guicey.gsp.app.rest.support;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Exception mapper for {@link ru.vyarus.guicey.gsp.views.template.Template} annotated resources (dropwizard views).
 * Mapping works only when thread bound context template is set (
 * {@link ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter} detected template url) so will not affect other
 * (not template) resources.
 * <p>
 * Other errors, directly returned from rest resources will be recognized directly in
 * {@link ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter}.
 * <p>
 * Single mapping registration is not enough because jersey selects the most closest exception according to
 * exception type which means in some cases different exception mapper could be selected. In order to properly
 * handle some exception types, additional {@link TemplateErrorHandlerAlias} must be registered with more specific
 * exception type.
 * <p>
 * Aliasing was done the same way as dropwizard own defautl exceptions mapping with
 * {@link io.dropwizard.jersey.errors.LoggingExceptionMapper} which is registered with {@link IllegalStateException}
 * to handle the most common exception and {@link Throwable} to handle all other generic cases.
 * By default, only one alias is registered to override dropwizard mapping for {@link IllegalStateException}.
 * <p>
 * When some other mapper is used instead of this one, special error message will be printed and new
 * mapper instance must be registered for the problematic type (note that it will work ONLY for template
 * resources processing and will not override behaviour for application rest).
 *
 * @author Vyacheslav Rusakov
 * @since 07.12.2018
 */
@Provider
@Singleton
public class TemplateErrorHandler implements ExtendedExceptionMapper<Throwable> {
    private static final String ERROR_PROCESSING_STATE = "TemplateExceptionHandler.ERROR_PROCESSING_STATE";
    private static final String ERROR = "TemplateExceptionHandler.ERROR";

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    @Override
    public boolean isMappable(final Throwable exception) {
        // react only in if its template call or custom error page is configured
        final TemplateContext context = TemplateContext.getInstance();
        if (context == null) {
            return false;
        }
        final WebApplicationException ex = wrap(exception);

        final boolean res = context.getErrorRedirect().isRedirectable(ex);
        if (res) {
            // exception mapping mechanism will check all mappers but select the one with the best type matching
            // so here we will set marker that request MUST be processed by this handler
            // and in response filter will check if it was actually processed
            request.setAttribute(ERROR_PROCESSING_STATE, RestErrorHandlingState.TO_PROCESS);
            request.setAttribute(ERROR, exception);
        }
        return res;
    }

    @Override
    public Response toResponse(final Throwable exception) {
        final TemplateContext context = TemplateContext.getInstance();
        if (context.getErrorRedirect().redirect(request, response, wrap(exception))) {
            request.setAttribute(ERROR_PROCESSING_STATE, RestErrorHandlingState.PROCESSED);
        }
        // have to specify code manually in order to prevent modifications (204 for null return)
        return Response.status(200).build();
    }

    public static boolean isErrorPageNotRendered(final HttpServletRequest request) {
        // detect case when error was supposed to be processed by this mapper, but was handled by another one
        final RestErrorHandlingState state = (RestErrorHandlingState) request.getAttribute(ERROR_PROCESSING_STATE);
        return state != null && state.ordinal() < RestErrorHandlingState.PROCESSED.ordinal();
    }

    public static Throwable getExceptionType(final HttpServletRequest request) {
        return (Throwable) request.getAttribute(ERROR);
    }

    private WebApplicationException wrap(final Throwable exception) {
        return exception instanceof WebApplicationException
                ? (WebApplicationException) exception
                : new WebApplicationException(exception, 500);
    }

    private enum RestErrorHandlingState {
        TO_PROCESS,
        PROCESSED
    }
}
