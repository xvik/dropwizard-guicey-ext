package ru.vyarus.guicey.gsp.app.rest.support;

import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Detect direct status response from rest (e.g. {@code Response.status(404).build()}) and tries to
 * redirect it into custom error page. Note that in this case dropwizard will log 404 rest response, but
 * actual response will return error page instead (so logs will be a bit misleading).
 *
 * @author Vyacheslav Rusakov
 * @since 15.01.2019
 */
@Provider
@Template
public class TemplateErrorResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() >= ErrorRedirect.CODE_400) {
            // redirect direct status return from rest into error page (e.g. when
            // Response.status(400).build() used as response)
            final TemplateContext context = TemplateRedirect.templateContext();
            if (context != null) {
                context.redirectError(new TemplateRestCodeError(requestContext, responseContext.getStatus()));
            }
        }
    }
}
