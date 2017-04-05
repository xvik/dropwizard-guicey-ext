package ru.vyarus.guicey.spa.filter;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filter must be mapped to assets servlet, serving spa application.
 * Bypass all calls to servlet, but if servlet returns 404, tries to redirect to application main page.
 * <p>
 * This is important to properly handle html5 client routing (without hashbang).
 * <p>
 * In order to route, filter checks request accept header: if it's compatible with "text/html" - routing is performed.
 * If not, 404 error sent. Also, regex pattern is used to prevent routing (for example, for html templates).
 * This is important for all other assets, which absence must be indicated.
 *
 * @author Vyacheslav Rusakov
 * @since 02.04.2017
 */
public class SpaRoutingFilter implements Filter {

    public static final String SLASH = "/";
    private final Logger logger = LoggerFactory.getLogger(SpaRoutingFilter.class);

    private final String target;
    private final Pattern noRedirect;

    public SpaRoutingFilter(final String target, final String noRedirectRegex) {
        this.target = target;
        noRedirect = Pattern.compile(noRedirectRegex);
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not needed
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) servletRequest;
        final HttpServletResponse resp = (HttpServletResponse) servletResponse;

        if (isRoot(req)) {
            directCall(req, resp, chain);
        } else {
            checkRedirect(req, resp, chain);
        }
    }

    @Override
    public void destroy() {
        // not needed
    }


    private void directCall(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final FilterChain chain) throws IOException, ServletException {
        noCache(resp);
        chain.doFilter(req, resp);
    }


    private void checkRedirect(final HttpServletRequest req,
                               final HttpServletResponse resp,
                               final FilterChain chain) throws IOException, ServletException {
        // wrap request to intercept errors
        final ResponseWrapper wrapper = new ResponseWrapper(resp);
        chain.doFilter(req, wrapper);

        final int error = wrapper.getError();

        if (error != HttpServletResponse.SC_NOT_FOUND) {
            if (error != 0) {
                // if error
                resp.sendError(error);
            }
            return;
        }

        if (isRedirectAllowed(req)) {
            // redirect to root
            noCache(resp);
            req.getRequestDispatcher(target).forward(req, resp);
        } else {
            // bypass resource not found
            ((HttpServletResponse) resp).sendError(error);
        }
    }

    private boolean isRoot(final HttpServletRequest req) {
        final String uri = req.getRequestURI();
        final String path = uri.endsWith(SLASH) ? uri : uri + SLASH;
        return path.equals(target);
    }

    private void noCache(final HttpServletResponse resp) {
        resp.setHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate,no-cache,no-store");
    }

    private boolean isRedirectAllowed(final HttpServletRequest req) {
        final String accept = req.getHeader(HttpHeaders.ACCEPT);
        if (Strings.emptyToNull(accept) == null) {
            return false;
        }

        boolean compatible = false;
        // accept header could contain multiple mime types
        for (String type : accept.split(",")) {
            try {
                if (MediaType.valueOf(type).isCompatible(MediaType.TEXT_HTML_TYPE)) {
                    compatible = true;
                    break;
                }
            } catch (Exception ex) {
                // ignore errors for better behaviour
                logger.debug("Failed to parse media type '{}':", type, ex.getMessage());
            }
        }

        return compatible && !noRedirect.matcher(req.getRequestURI()).find();
    }
}
