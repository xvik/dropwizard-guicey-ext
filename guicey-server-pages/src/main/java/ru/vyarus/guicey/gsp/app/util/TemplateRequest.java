package ru.vyarus.guicey.gsp.app.util;

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

    private final String path;

    public TemplateRequest(final HttpServletRequest request, final String app) {
        super(request);
        path = PathUtils.prefixSlash(PathUtils.path(app, request.getRequestURI()));
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

    /**
     * For application logic it's often better to operate on original request (with original url, before
     * redirection to rest).
     *
     * @param request request to unwrap
     * @return original request if it's a template request or request itself
     */
    public static HttpServletRequest getOriginalRequest(final HttpServletRequest request) {
        return request instanceof TemplateRequest
                ? (HttpServletRequest) ((TemplateRequest) request).getRequest()
                : request;
    }
}
