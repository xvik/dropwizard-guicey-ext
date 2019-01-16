package ru.vyarus.guicey.gsp.views.template;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.app.util.ResourceLookup;

import javax.annotation.Nullable;
import javax.inject.Provider;
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

    public TemplateContext(final String appName,
                           final String rootUrl,
                           final List<String> resourcePaths,
                           final String url,
                           final Provider<Injector> injectorProvider,
                           final ErrorRedirect errorRedirect) {
        this.appName = appName;
        this.rootUrl = rootUrl;
        this.resourcePaths = resourcePaths;
        this.url = url;
        this.injectorProvider = injectorProvider;
        this.errorRedirect = errorRedirect;
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
     * 
     * @return
     */
    public ErrorRedirect getErrorRedirect() {
        return errorRedirect;
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
     * Used by {@link TemplateAnnotationFilter} to set template file
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
    protected String lookupTemplatePath(@Nullable final String template) {
        String path = Strings.emptyToNull(template);
        if (path == null) {
            // from @Template annotation
            path = templatePath;
        }
        Preconditions.checkNotNull(path,
                "Template name not specified neither directly in model nor in @Template annotation");

        if (path.startsWith(PathUtils.SLASH)) {
            // absolute path
            return path;
        } else {
            final String lookup = ResourceLookup.lookup(path, resourcePaths);
            if (lookup == null) {
                throw new TemplateNotFoundException(String.format(
                        "Template '%s' not found in locations: %s", template, resourcePaths));
            }
            return PathUtils.prefixSlash(lookup);
        }
    }

    /**
     * @return thread bound template context instance
     */
    public static TemplateContext getInstance() {
        return Preconditions.checkNotNull(TemplateRedirect.templateContext(),
                "No template context found for current thread");
    }
}
