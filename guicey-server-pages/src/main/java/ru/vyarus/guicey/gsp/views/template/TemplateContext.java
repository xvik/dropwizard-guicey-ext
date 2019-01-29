package ru.vyarus.guicey.gsp.views.template;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.app.util.ResourceLookup;

import javax.annotation.Nullable;
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
    private final Logger logger = LoggerFactory.getLogger(TemplateContext.class);

    private final String appName;
    private final String rootUrl;
    private final List<String> resourcePaths;
    private Class resourceClass;
    private String annotationTemplate;
    private final ErrorRedirect errorRedirect;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public TemplateContext(final String appName,
                           final String rootUrl,
                           final List<String> resourcePaths,
                           final ErrorRedirect errorRedirect,
                           final HttpServletRequest request,
                           final HttpServletResponse response) {
        this.appName = appName;
        this.rootUrl = rootUrl;
        this.resourcePaths = resourcePaths;
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
        return getRequest().getRequestURI();
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
     * Set resource class to check template relative to class.
     * Used by {@link ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter}.
     *
     * @param base resource class
     */
    public void setResourceClass(final Class base) {
        resourceClass = base;
    }

    /**
     * Used by {@link ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter} to set template file
     * declared in {@link Template} annotation on rest resource.
     *
     * @param template template file path
     */
    public void setAnnotationTemplate(final String template) {
        annotationTemplate = template;
    }

    /**
     * Lookup relative template path either relative to reosurce class (if annotated with {@link Template} or
     * in one of pre-configured classpath locations. If passed template is null it will be
     * taken from {@link Template} annotation from resource class.
     * <p>
     * When provided template path is absolute - it is searched by direct location only.
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
            path = annotationTemplate;
        }
        Preconditions.checkNotNull(path,
                "Template name not specified neither directly in model nor in @Template annotation");

        // search relative path relative to resource class
        if (!path.startsWith(PathUtils.SLASH) && resourceClass != null) {
            final String resourceBaseLocation = ResourceLookup.lookup(resourceClass, path);
            if (resourceBaseLocation != null) {
                path = PathUtils.prefixSlash(resourceBaseLocation);
                logger.debug("Relative template '{}' found relative to {} class: '{}'",
                        template, resourceClass.getSimpleName(), path);
            }
        }

        // search in configured locations
        if (!path.startsWith(PathUtils.SLASH)) {
            // search in configured folders
            path = PathUtils.prefixSlash(ResourceLookup.lookupOrFail(path, resourcePaths));
            logger.debug("Relative template '{}' resolved to '{}'", template, path);
        }

        // check direct absolute path
        ResourceLookup.existsOrFail(path);
        return path;
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
