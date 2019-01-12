package ru.vyarus.guicey.gsp.app.rest.support;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

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
 *
 * @author Vyacheslav Rusakov
 * @since 07.12.2018
 */
@Provider
public class TemplateErrorHandler implements ExtendedExceptionMapper<RuntimeException> {
    private final ErrorRedirect redirect;

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    public TemplateErrorHandler(final ErrorRedirect redirect) {
        this.redirect = redirect;
    }

    @Override
    public boolean isMappable(final RuntimeException exception) {
        // react only in if its template call or custom error page is configured
        final TemplateContext context = TemplateContext.getInstance();
        return context != null && redirect.isRedirectable(wrap(exception));
    }

    @Override
    public Response toResponse(final RuntimeException exception) {
        redirect.redirect(request, response, wrap(exception));
        // have to specify code manually in order to prevent modifications (204 for null return)
        return Response.status(200).build();
    }

    private WebApplicationException wrap(final Throwable exception) {
        return exception instanceof WebApplicationException
                ? (WebApplicationException) exception
                : new WebApplicationException(exception, 500);
    }
}
