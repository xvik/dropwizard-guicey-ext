package ru.vyarus.guicey.gsp.app;

import ru.vyarus.dropwizard.guice.debug.util.RenderUtils;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.rest.log.HiddenViewPath;
import ru.vyarus.guicey.gsp.app.rest.log.MappedViewPath;
import ru.vyarus.guicey.gsp.app.rest.log.ViewPath;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.template.ManualErrorHandling;
import ru.vyarus.guicey.spa.SpaBundle;

import java.lang.reflect.Method;
import java.util.Map;

import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.NEWLINE;
import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.TAB;

/**
 * Builds server page application console report.
 *
 * @author Vyacheslav Rusakov
 * @since 12.01.2019
 */
public final class AppReportBuilder {

    private static final String STAR = "*";

    private AppReportBuilder() {
    }

    /**
     * Build application report.
     *
     * @param app server pages application instance
     * @return application configuration report
     */
    public static String build(final ServerPagesApp app) {
        final StringBuilder res = new StringBuilder(String.format(
                "Server pages app '%s' registered on uri '%s' in %s context",
                app.name, app.fullUriPath + '*', app.mainContext ? "main" : "admin"));

        reportStaticResources(res, app);
        reportViewMappings(res, app);
        reportRestPaths(res, app);
        reportErrorPages(res, app);
        reportSpaSupport(res, app);

        return res.toString();
    }

    private static void reportStaticResources(final StringBuilder res, final ServerPagesApp app) {
        res.append(NEWLINE).append(NEWLINE)
                .append(TAB).append("Static resources locations:").append(NEWLINE);
        for (String url : app.assets.getLocations().keySet()) {
            res.append(TAB).append(TAB)
                    .append(PathUtils.cleanUpPath(app.fullUriPath + PathUtils.prefixSlash(url))).append(NEWLINE);
            for (String path : app.assets.getLocations().get(url)) {
                res.append(TAB).append(TAB).append(TAB)
                        .append(PathUtils.trimSlashes(path).replace('/', '.')).append(NEWLINE);
            }
            res.append(NEWLINE);
        }
    }

    private static void reportViewMappings(final StringBuilder res, final ServerPagesApp app) {
        res.append(TAB).append("View rest mappings:").append(NEWLINE);
        for (Map.Entry<String, String> entry : app.views.getPrefixes().entrySet()) {
            res.append(TAB).append(TAB)
                    .append(String.format("%-20s %s*",
                            PathUtils.path(app.fullUriPath, entry.getKey()) + STAR,
                            PathUtils.prefixSlash(
                                    PathUtils.normalizePath(app.templateRedirect.getRootPath(), entry.getValue()))))
                    .append(NEWLINE);
        }
        res.append(NEWLINE);
    }

    private static void reportRestPaths(final StringBuilder res,
                                        final ServerPagesApp app) {
        res.append(TAB).append("Mapped handlers:").append(NEWLINE);
        for (MappedViewPath path : app.viewPaths) {
            final ViewPath handle = path.getPath();

            final Method handlingMethod = handle.getMethod().getInvocable().getHandlingMethod();
            final boolean disabledErrors = handle.getResourceType().isAnnotationPresent(ManualErrorHandling.class)
                    || (handlingMethod != null && handlingMethod.isAnnotationPresent(ManualErrorHandling.class));

            res.append(TAB).append(TAB).append(String.format("%-7s %s  (%s #%s)%s",
                    handle.getMethod().getHttpMethod(),
                    PathUtils.cleanUpPath(app.fullUriPath + path.getMappedUrl()),
                    handle.getResourceType().getName(),
                    handle.getMethod().getInvocable().getDefinitionMethod().getName(),
                    disabledErrors ? " [DISABLED ERRORS]" : ""
            )).append(NEWLINE);
        }

        if (!app.hiddenViewPaths.isEmpty()) {
            res.append(NEWLINE).append(TAB).append("(!) Unreachable handlers:").append(NEWLINE);
            for (HiddenViewPath path : app.hiddenViewPaths) {
                final ViewPath handle = path.getPath();
                res.append(TAB).append(TAB)
                        .append(String.format("%-7s %s (%s #%s) of %s mapping hidden by %s mapping",
                                handle.getMethod().getHttpMethod(),
                                PathUtils.path(app.fullUriPath, path.getMappedUrl()),
                                RenderUtils.getClassName(handle.getResourceType()),
                                handle.getMethod().getInvocable().getDefinitionMethod().getName(),
                                PathUtils.path(app.fullUriPath, path.getMapping()) + STAR,
                                PathUtils.path(app.fullUriPath, path.getOverridingMapping()) + STAR))
                        .append(NEWLINE);
            }
        }
    }

    private static void reportErrorPages(final StringBuilder res, final ServerPagesApp app) {
        final Map<Integer, String> errorPages = app.errorPages;
        if (!errorPages.isEmpty()) {
            res.append(NEWLINE).append(TAB).append("Error pages:").append(NEWLINE);
            final int defKey = ErrorRedirect.DEFAULT_ERROR_PAGE;
            for (Map.Entry<Integer, String> entry : errorPages.entrySet()) {
                if (entry.getKey() != defKey) {
                    printErrorPage(res, entry.getKey().toString(), PathUtils.path(app.fullUriPath, entry.getValue()));
                }
            }
            if (errorPages.containsKey(defKey)) {
                printErrorPage(res, STAR, PathUtils.path(app.fullUriPath, errorPages.get(defKey)));
            }
        }
    }

    private static void printErrorPage(final StringBuilder res, final String code, final String page) {
        res.append(TAB).append(TAB)
                .append(String.format("%-7s %s", code, PathUtils.prefixSlash(page)))
                .append(NEWLINE);
    }

    private static void reportSpaSupport(final StringBuilder res, final ServerPagesApp app) {
        if (app.spaSupport) {
            res.append(NEWLINE).append(TAB).append("SPA routing enabled");
            if (!app.spaNoRedirectRegex.equals(SpaBundle.DEFAULT_PATTERN)) {
                res.append(" (with custom pattern)");
            }
            res.append(NEWLINE);
        }
    }

}
