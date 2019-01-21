package ru.vyarus.guicey.gsp.app.rest.support;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
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
 * Other errors, directly returned from rest resources will be recognized directly in response filter
 * {@link TemplateErrorValidationFilter}.
 * <p>
 * Single mapping registration is not enough because jersey selects the most closest exception according to
 * exception type which means in some cases different exception mapper could be selected. In order to properly
 * handle some exception types, additional {@link TemplateErrorHandlerAlias} must be registered with more specific
 * exception type.
 * <p>
 * Aliasing was done the same way as dropwizard own default exceptions mapping with
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
    private HttpServletResponse response;

    @Override
    public boolean isMappable(final Throwable exception) {
        // react only in if its template call or custom error page is configured
        final TemplateContext context = TemplateRedirect.templateContext();
        if (context == null) {
            return false;
        }
        final WebApplicationException ex = wrap(exception);

        final HttpServletRequest req = context.getOriginalRequest();
        final boolean res = context.getErrorRedirect().isRedirectableException(req, ex);
        if (res) {
            // exception mapping mechanism will check all mappers but select the one with the best type matching
            // so here special marker set that request MUST be processed by this handler
            // and response filter will check if it was actually processed
            req.setAttribute(ERROR_PROCESSING_STATE, true);
            // store exception instance for more complete message (in case of wrong mapper)
            req.setAttribute(ERROR, exception);
        }
        return res;
    }

    @Override
    public Response toResponse(final Throwable exception) {
        final TemplateContext context = TemplateContext.getInstance();
        // use request with original uri instead of rest mapped
        final HttpServletRequest req = context.getOriginalRequest();
        if (context.getErrorRedirect().redirect(req, response, wrap(exception))) {
            req.removeAttribute(ERROR_PROCESSING_STATE);
        }
        // have to specify code manually in order to prevent modifications (204 for null return)
        return Response.status(200).build();
    }

    /**
     * Used to check actual mapper usage in response filter. Jersey will always call {@link #isMappable(Throwable)}
     * during best mapper detection, but eventually may choose another mapper (and so marker will remain in request).
     *
     * @param request request instance
     * @return true when different exception mapper used for exception handling, false otherwise
     */
    public static boolean isWrongMapperUsed(final HttpServletRequest request) {
        // detect case when error was supposed to be processed by this mapper, but was handled by another one
        return request.getAttribute(ERROR_PROCESSING_STATE) != null;
    }

    /**
     * Used for detailed error message when different exception mapper used.
     *
     * @param request request instance
     * @return exception instance or null if no exception were thrown
     */
    public static Throwable getException(final HttpServletRequest request) {
        return (Throwable) request.getAttribute(ERROR);
    }

    private WebApplicationException wrap(final Throwable exception) {
        return exception instanceof WebApplicationException
                ? (WebApplicationException) exception
                : new WebApplicationException(exception, 500);
    }
}
