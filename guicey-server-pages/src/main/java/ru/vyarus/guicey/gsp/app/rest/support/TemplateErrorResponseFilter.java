package ru.vyarus.guicey.gsp.app.rest.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Detects cases when different exception mapper was used instead of {@link TemplateExceptionMapper}.
 * In this case expected error page is not rendered.
 * <p>
 * Detection mechanism use the fact that main template exception mapper is
 * {@link org.glassfish.jersey.spi.ExtendedExceptionMapper} and jersey mapper selection mechanism always call
 * it's {@link org.glassfish.jersey.spi.ExtendedExceptionMapper#isMappable(Throwable)} method, but later could
 * select different mapper (and the closest).
 * <p>
 * Also, detects direct status response from rest (e.g. {@code Response.status(404).build()}) and tries to
 * redirect it into custom error page. IMPORTANT: in this case dropwizard will log 404 rest response, but
 * actual response will return error page.
 *
 * @author Vyacheslav Rusakov
 * @since 15.01.2019
 */
@Provider
@Template
public class TemplateErrorResponseFilter implements ContainerResponseFilter {
    private static final String DBL_NL = "\n\n";

    private final Logger logger = LoggerFactory.getLogger(TemplateErrorResponseFilter.class);

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws IOException {
        // check if incorrect exception mapper was used
        if (TemplateExceptionMapper.isWrongMapperUsed(request)) {
            final Throwable error = TemplateExceptionMapper.getException(request);
            logger.error(DBL_NL
                    + "\tCustom error page was not shown because jersey used more specific "
                    + "exception mapper for error:\n"
                    + "\t\t" + error.getClass().getName() + ": " + error.getMessage() + DBL_NL

                    + "\tIn order to override this behaviour register template handler alias:\n "
                    + "\t\t(ServerPagesBundle)\n"
                    + "\t\t\t.handleTemplateException(new TemplateErrorHandlerAlias<"
                    + error.getClass().getSimpleName() + ">(){})\n");
        } else if (responseContext.getStatus() >= ErrorRedirect.CODE_400) {
            // redirect direct status return from rest into error page (e.g. when
            // Response.status(400).build() used as response)
            final TemplateContext context = TemplateRedirect.templateContext();
            if (context != null) {
                final WebApplicationException exception = new WebApplicationException(responseContext.getStatus());
                // use request with original uri instead of rest mapped and raw response (not hk proxy)
                context.getErrorRedirect().redirect(
                        context.getOriginalRequest(),
                        context.getOriginalResponse(),
                        exception);
            }
        }
    }
}
