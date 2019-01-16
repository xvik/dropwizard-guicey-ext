package ru.vyarus.guicey.gsp.app.filter;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorHandler;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.spa.filter.ResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main filter, implementing server pages support. Filter is applied above assets servlet. For each request,
 * file calls detected using pre-defined regexp (by default, looking if request end with en extensions - most
 * likely file). Detected file extension checked if it's a template file. If not template then redirected to
 * assets servlet (normal dropwizard assets processing). In all other cases, request is redirected into
 * rest (with "{app name}" prefix) to be handled by rest resource (dropwizard views).
 * <p>
 * Such logic is required in order to merge assets and views worlds so overall it could be used as good old JSP.
 * <p>
 * Filter detect direct http errors from assets or views and if custom error page registered for current
 * error code - redirect to error page (which could also be a template). If no special page registered - server
 * error response as is.
 * <p>
 * Note that exceptions inside rest resources are also tracked by
 * {@link TemplateErrorHandler} (exception mapper) which allows to
 * use more informative exception objects in error page.
 *
 * @author Vyacheslav Rusakov
 * @since 22.10.2018
 */
public class ServerPagesFilter implements Filter {

    // server app mapping
    private final String uriPath;
    // file requets detection regexp
    private final Pattern filePattern;
    // index page
    private final String index;
    private final TemplateRedirect redirect;
    private final Iterable<ViewRenderer> renderers;

    public ServerPagesFilter(final String uriPath,
                             final String filePattern,
                             final String index,
                             final TemplateRedirect redirect,
                             final Iterable<ViewRenderer> renderers) {
        this.uriPath = uriPath;
        this.filePattern = Pattern.compile(filePattern);
        this.index = index;
        this.redirect = redirect;
        this.renderers = renderers;
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

        // look if request ends with file (name.ext pattern, maybe followed by query params (?) part)
        // e.g. /some/url/file.txt?start=1 -> file.txt
        // file request could be either asset or direct template call
        final String pathFile = findFileInPath(req);
        if (pathFile != null && !isTemplate(pathFile)) {
            // delegate to asset servlet
            serveAsset(req, resp, chain);
            return;
        }

        // cut of application mapping prefix to get page url (same as in rest url, but without app prefix)
        // (it may be direct path to template file under classpath)
        String page = req.getRequestURI().substring(uriPath.length());
        if (page.isEmpty()) {
            page = index;
        }
        // redirect to rest handling (dropwizard-view template)
        serveTemplate(req, resp, page);
    }

    @Override
    public void destroy() {
        // not needed
    }

    private String findFileInPath(final HttpServletRequest req) {
        if (isRoot(req)) {
            // check if index page is a file (and not some path)
            return filePattern.matcher(index).find() ? index : null;
        }

        final Matcher matcher = filePattern.matcher(req.getRequestURI());
        // extracting template name
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isRoot(final HttpServletRequest req) {
        final String uri = req.getRequestURI();
        final String path = PathUtils.endSlash(uri);
        return path.equals(uriPath);
    }

    private boolean isTemplate(final String file) {
        final View view = new DummyView(file);
        for (ViewRenderer renderer : renderers) {
            if (renderer.isRenderable(view)) {
                return true;
            }
        }
        return false;
    }

    private void serveAsset(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final FilterChain chain) throws IOException, ServletException {
        // wrap request to intercept errors
        final ResponseWrapper wrapper = new ResponseWrapper(resp);
        chain.doFilter(req, wrapper);
        handleError(req, resp, wrapper.getError());
    }

    private void serveTemplate(final HttpServletRequest req,
                               final HttpServletResponse resp,
                               final String page) throws IOException, ServletException {
        // wrap request to intercept errors
        final ResponseWrapper wrapper = new ResponseWrapper(resp);
        redirect.redirect(req, resp, page);
        handleError(req, resp, wrapper.getError());
    }

    private void handleError(final HttpServletRequest req,
                             final HttpServletResponse resp,
                             final int error) throws IOException {
        // handle only error codes, preserving redirects (3xx)
        if (error >= ErrorRedirect.CODE_400
                && !redirect.getErrorRedirect().redirect(req, resp, new WebApplicationException(error))) {
            // if no mapped error page - return error as is
            resp.sendError(error);
        }
    }

    /**
     * Dummy view for class used to re-use dropwizard renderer selection logic.
     */
    private static class DummyView extends View {
        DummyView(final String templateName) {
            super(templateName);
        }
    }
}
