package ru.vyarus.guicey.gsp.app.util;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper used to prefix app name into original request url during redirection to rest.
 * For example, original url like '/some/url/' trasformed into '{app}/some/url' where {app} is server pages
 * application registration name.
 *
 * @author Vyacheslav Rusakov
 * @since 18.01.2019
 */
public class TemplateRequest extends HttpServletRequestWrapper {

    private final Servlet restServlet;
    private final String path;

    public TemplateRequest(
            final Servlet restServlet,
            final HttpServletRequest request,
            final String app,
            final String mapping) {
        super(request);
        this.restServlet = restServlet;
        path = PathUtils.prefixSlash(PathUtils.path(app, request.getRequestURI().substring(mapping.length())));
    }

    @Override
    public String getRequestURI() {
        return path;
    }

    @Override
    public StringBuffer getRequestURL() {
        // it's not efficient because original buffer is overridden, but at least correct
        final String originalPath = super.getRequestURI();
        final String res = super.getRequestURL().toString()
                .replace(originalPath, path);
        return new StringBuffer(res);
    }

    // overrides below are required for proper handling inside admin context (with flat mapping)

    @Override
    public String getContextPath() {
        // (main) context mapping path
        return "/";
    }

    @Override
    public String getServletPath() {
        // (main) servlet mapping path
        return restServlet.getServletConfig().getServletContext().getContextPath();
    }
}
