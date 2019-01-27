package ru.vyarus.guicey.gsp.views.template;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.glassfish.jersey.server.internal.process.MappableException;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.app.util.ResourceLookup;

import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.List;

/**
 * Contains context information for rendered template. The most useful information is original request path:
 * each template rendering request is redirected into resource (rest) and so it's impossible to know
 * original path from the request object (inside rest resource).
 * <p>
 * Template context object is thread-bound and available during template rendering request processing.
 *
 * @author Vyacheslav Rusakov
 * @since 25.10.2018
 */
public class TemplateContext {

    private final String appName;
    private final String rootUrl;
    private final List<String> resourcePaths;
    private String templatePath;
    private final String url;
    private final Provider<Injector> injectorProvider;
    private final ErrorRedirect errorRedirect;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TemplateContext(final String appName,
                           final String rootUrl,
                           final List<String> resourcePaths,
                           final String url,
                           final Provider<Injector> injectorProvider,
                           final ErrorRedirect errorRedirect,
                           final HttpServletRequest request,
                           final HttpServletResponse response) {
        this.appName = appName;
        this.rootUrl = rootUrl;
        this.resourcePaths = resourcePaths;
        this.url = url;
        this.injectorProvider = injectorProvider;
        this.errorRedirect = errorRedirect;
        this.request = request;
        this.response = response;
    }

    /**
     * @return server pages application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * @return root url for server pages application
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Each template render is redirected to rest resource so it's impossible to obtain original uri from request
     * object inside the resource.
     *
     * @return original call url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Method may be used to access original request object (in edge cases).
     *
     * @return original request object (before any redirection)
     * @see #getUrl() for original request URI
     * @see #getRootUrl() for root mapping url
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Raw response is required for redirection logic to avoid response processing loops
     * due to hk wrappers (if hk injection were used for response object injection it would always be a proxy).
     * <p>
     * Method may be used to handle response directly (in edge cases)
     *
     * @return original response object
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @param service service type
     * @param <T>     service type
     * @return service instance obtained from guice context
     */
    public <T> T getService(final Class<T> service) {
        return injectorProvider.get().getInstance(service);
    }

    /**
     * Used by {@link ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter} to set template file
     * declared in {@link Template} annotation on rest resource.
     *
     * @param base     resource class (to search relative to; ignored if template path is absolute)
     * @param template template file relative path (or absolute)
     */
    public void setTemplate(final Class base, final String template) {
        // compute template relative to file
        templatePath = template.startsWith(PathUtils.SLASH)
                ? template : PathUtils.prefixSlash(PathUtils.path(PathUtils.getPath(base), template));
    }

    /**
     * Lookup template in one of pre-configured classpath locations. If passed template is null it will be
     * taken from {@link Template} annotation from resource class. Provided template path may be absolute
     * (in this cases template will be searched by direct location only).
     *
     * @param template template path or null
     * @return absolute path to template
     * @throws NullPointerException      if template path not set
     * @throws TemplateNotFoundException if template not found
     */
    public String lookupTemplatePath(@Nullable final String template) {
        String path = Strings.emptyToNull(template);
        if (path == null) {
            // from @Template annotation
            path = templatePath;
        }
        Preconditions.checkNotNull(path,
                "Template name not specified neither directly in model nor in @Template annotation");

        // do nothing for absolute path
        return path.startsWith(PathUtils.SLASH) ? path
                : PathUtils.prefixSlash(ResourceLookup.lookupOrFail(path, resourcePaths));
    }

    /**
     * Perform redirection to error page (if registered) or handle SPA route (if 404 response and SPA support enabled).
     * <p>
     * When only resulted status code is known use {@code WebApplicationException(code)} as argument for redirection.
     * <p>
     * It is safe to call redirection multiple times: only first call will be actually handled (assuming next errors
     * appear during error page rendering and can't be handled).
     * <p>
     * Method is not intended to be used directly, but could be in specific (maybe complex) edge cases.
     *
     * @param ex exception instance
     * @return true if redirect performed, false if no redirect performed
     */
    public boolean redirectError(final Throwable ex) {
        // use request with original uri instead of rest mapped and raw response (not hk proxy)
        return errorRedirect.redirect(getRequest(), getResponse(), wrap(ex));
    }

    /**
     * @return thread bound template context instance
     */
    public static TemplateContext getInstance() {
        return Preconditions.checkNotNull(TemplateRedirect.templateContext(),
                "No template context found for current thread");
    }

    private WebApplicationException wrap(final Throwable exception) {
        Throwable cause = exception;
        // compensate MappableException
        while (cause instanceof MappableException) {
            cause = cause.getCause();
        }
        return cause instanceof WebApplicationException
                ? (WebApplicationException) cause
                : new WebApplicationException(cause, 500);
    }
}
