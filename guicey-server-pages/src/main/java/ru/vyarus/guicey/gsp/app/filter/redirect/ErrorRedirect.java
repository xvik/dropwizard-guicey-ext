package ru.vyarus.guicey.gsp.app.filter.redirect;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redirects response error to the configured error page
 * ({@link ru.vyarus.guicey.gsp.ServerPagesBundle.Builder#errorPage(String)}).
 * Only response codes >= 400 (errors) are handled, everything else considered as normal flow.
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
    public static final int CODE_500 = 500;

    private static final ThreadLocal<WebApplicationException> CONTEXT_ERROR = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(ErrorRedirect.class);

    private final Map<Integer, String> errorPages;
    private final boolean logErrors;

    public ErrorRedirect(final String appMapping,
                         final Map<Integer, String> pages,
                         final boolean logErrors) {
        // copy for modifications
        this.errorPages = new HashMap<>(pages);
        this.logErrors = logErrors;
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
        final String path = selectErrorPage(exception);
        if (path != null) {
            // to be able to access exception in error view
            CONTEXT_ERROR.set(exception);
            try {
                request.getRequestDispatcher(path).forward(request, response);
                // always log 500 error exceptions and other errors if configured
                if (logErrors || exception.getResponse().getStatus() == CODE_500) {
                    logger.error("Error serving response for '" + TemplateContext.getInstance().getUrl()
                            + "' (handled as '" + request.getRequestURI() + "'). "
                            + "Custom error page '" + path + "' rendered.", exception);
                }
            } catch (Exception ex) {
                // important to log original exception because it will be overridden
                logger.error("Failed to redirect to error page '" + path + "' instead of rest exception:",
                        exception);
                Throwables.throwIfUnchecked(ex);
                throw new IllegalStateException("Failed to redirect to error page '" + path + "'", ex);
            } finally {
                CONTEXT_ERROR.remove();
            }
            return true;
        }
        return false;
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

    public boolean isRedirectable(final WebApplicationException exception) {
        return selectErrorPage(exception) != null;
    }

    private String selectErrorPage(final WebApplicationException exception) {
        final int status = exception.getResponse().getStatus();
        if (status >= CODE_400) {
            final String res = errorPages.get(status);
            return res == null ? errorPages.get(DEFAULT_ERROR_PAGE) : res;
        }
        return null;
    }
}
