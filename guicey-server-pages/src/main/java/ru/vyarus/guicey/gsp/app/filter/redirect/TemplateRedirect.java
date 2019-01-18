package ru.vyarus.guicey.gsp.app.filter.redirect;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.asset.LazyLocationProvider;
import ru.vyarus.guicey.gsp.app.rest.DirectTemplateResource;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.util.TemplateRequest;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.inject.Provider;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Performs redirection of template quest into rest context. Note that even if no special rest
 * mapped for template, it would be rendered with the default {@link DirectTemplateResource}.
 * <p>
 * Rest resource convention: /{rest prefix}/{app name}/{path from request}, where
 * {app name} is an application registration name.
 * <p>
 * If resource is annotated with {@link ru.vyarus.guicey.gsp.views.template.Template} annotation (it should!) then
 * {@link TemplateAnnotationFilter} will detect it and set specified template into context {@link TemplateContext}.
 * <p>
 * Important: resources must use {@link ru.vyarus.guicey.gsp.views.template.TemplateView} as base template model class
 * in order to properly support {@link ru.vyarus.guicey.gsp.views.template.Template} annotation.
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2018
 */
public class TemplateRedirect {

    private static final ThreadLocal<TemplateContext> CONTEXT_TEMPLATE = new ThreadLocal<>();

    private final Logger logger = LoggerFactory.getLogger(TemplateRedirect.class);

    private final Servlet restServlet;
    private final String app;
    private final String mapping;
    private final LazyLocationProvider locationProvider;
    private final Provider<Injector> injectorProvider;
    private final ErrorRedirect errorRedirect;
    private String rootPath;

    public TemplateRedirect(final Servlet restServlet,
                            final String app,
                            final String mapping,
                            final LazyLocationProvider locationProvider,
                            final Provider<Injector> injectorProvider,
                            final ErrorRedirect errorRedirect) {
        this.restServlet = restServlet;
        this.app = app;
        this.mapping = mapping;
        this.locationProvider = locationProvider;
        this.injectorProvider = injectorProvider;
        this.errorRedirect = errorRedirect;
    }

    /**
     * @param rootPath rest mapping path
     */
    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * @return rest mapping path
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Redirect template request into rest resource. Jersey will select appropriate resource by path, or
     * default {@link DirectTemplateResource} will be used.
     *
     * @param request  template request
     * @param response template response
     * @param page     requested template path (cleared for matching)
     * @throws IOException      on dispatching errors
     * @throws ServletException on dispatching errors
     */
    public void redirect(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final String page) throws IOException, ServletException {
        CONTEXT_TEMPLATE.set(new TemplateContext(app,
                mapping,
                locationProvider.get(),
                request.getRequestURI(),
                injectorProvider,
                errorRedirect));
        try {
            final String path = PathUtils.path(rootPath, app, page);
            logger.debug("Rendering template path: {}", path);
            // this moment is especially important for admin apps where context could be radically different
            restServlet.service(new TemplateRequest(request, app), response);
        } finally {
            CONTEXT_TEMPLATE.remove();
        }
    }

    /**
     * @return custom error pages support
     */
    public ErrorRedirect getErrorRedirect() {
        return errorRedirect;
    }

    /**
     * @return thread bound template context or null
     */
    public static TemplateContext templateContext() {
        return CONTEXT_TEMPLATE.get();
    }
}
