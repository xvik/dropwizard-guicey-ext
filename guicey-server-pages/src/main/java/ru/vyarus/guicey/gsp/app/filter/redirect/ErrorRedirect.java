package ru.vyarus.guicey.gsp.app.filter.redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.spa.filter.SpaUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redirects response error to the configured error page
 * ({@link ru.vyarus.guicey.gsp.ServerPagesBundle.Builder#errorPage(String)}).
 * Only response codes &gt;= 400 (errors) are handled, everything else considered as normal flow.
 * <p>
 * When SPA support is enabled, also intercept all 404 errors and checks if it could be SPA route (and do home redirect
 * instead of error).
 * <p>
 * Asset errors are intercepted directly inside {@link ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter}.
 * Rest errors are intercepted with {@link ru.vyarus.guicey.gsp.app.rest.support.TemplateExceptionListener}
 * and {@link ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorResponseFilter}.
 *
 * @author Vyacheslav Rusakov
 * @since 07.12.2018
 */
public class ErrorRedirect {
    /**
     * Special code for default error page registration
     * ({@link ru.vyarus.guicey.gsp.ServerPagesBundle.Builder#errorPage(String)}).
     */
    public static final int DEFAULT_ERROR_PAGE = -1;
    public static final int CODE_400 = 400;

    private static final ThreadLocal<WebApplicationException> CONTEXT_ERROR = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(ErrorRedirect.class);

    private final Map<Integer, String> errorPages;
    private final SpaSupport spa;

    public ErrorRedirect(final String appMapping,
                         final Map<Integer, String> pages,
                         final SpaSupport spa) {
        // copy for modifications
        this.errorPages = new HashMap<>(pages);
        this.spa = spa;
        // normalize paths to be absolute
        for (int code : pages.keySet()) {
            errorPages.put(code, PathUtils.normalizePath(appMapping, errorPages.get(code)));
        }
    }

    /**
     * Try to redirect error to configured error page.
     *
     * @param request   request
     * @param response  response
     * @param exception error (either simple wrapping for error code or complete stacktrace from rest)
     * @return true if error page found and false if no page configured (no special handling required)
     */
    public boolean redirect(final HttpServletRequest request,
                            final HttpServletResponse response,
                            final WebApplicationException exception) {
        return spa.redirect(request, response, exception.getResponse().getStatus())
                || (SpaUtils.isHtmlRequest(request) && doRedirect(request, response, exception));
    }

    /**
     * Note: method is not supposed to be used directly as error object is directly available in model:
     * {@link ru.vyarus.guicey.gsp.views.template.TemplateView#getContextError()}.
     *
     * @return thread bound exception to use in error page rendering
     */
    public static WebApplicationException getContextError() {
        return CONTEXT_ERROR.get();
    }

    private String selectErrorPage(final WebApplicationException exception) {
        final int status = exception.getResponse().getStatus();
        if (status >= CODE_400) {
            final String res = errorPages.get(status);
            return res == null ? errorPages.get(DEFAULT_ERROR_PAGE) : res;
        }
        return null;
    }

    private boolean doRedirect(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final WebApplicationException exception) {
        final String path = selectErrorPage(exception);
        // do not redirect errors of error page rendering (prevent loops)
        if (path != null && !response.isCommitted() && CONTEXT_ERROR.get() == null) {
            logger.debug("Redirecting to error page: {}", path);
            // to be able to access exception in error view
            CONTEXT_ERROR.set(exception);
            try {
                request.getRequestDispatcher(path).forward(request, response);
                // always log 500 error exceptions and other errors if configured
                logger.info("Serving error page '{}' instead of '{}' response error {}",
                        path, request.getRequestURL(), exception.getResponse().getStatus());
                return true;
            } catch (Exception ex) {
                // assume normal processing for original request (without error page)
                logger.error("Failed to serve error page '" + path + "' for request '"
                        + request.getRequestURL() + "' instead of rest exception:", ex);
            } finally {
                CONTEXT_ERROR.remove();
            }
        }
        return false;
    }
}
