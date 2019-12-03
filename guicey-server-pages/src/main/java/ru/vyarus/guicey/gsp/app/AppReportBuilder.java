package ru.vyarus.guicey.gsp.app;

import ru.vyarus.dropwizard.guice.debug.util.RenderUtils;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.rest.log.ResourcePath;
import ru.vyarus.guicey.gsp.app.rest.log.RestPathsAnalyzer;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.template.ManualErrorHandling;
import ru.vyarus.guicey.spa.SpaBundle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
     * @param app      server pages application instance
     * @param analyzer rest template paths analyser
     * @return application configuration report
     */
    public static String build(final ServerPagesApp app, final RestPathsAnalyzer analyzer) {
        final StringBuilder res = new StringBuilder(String.format(
                "Server pages app '%s' registered on uri '%s' in %s context",
                app.name, app.fullUriPath + '*', app.mainContext ? "main" : "admin"));

        reportStaticResources(res, app);
        reportViewMappings(res, app);
        reportRestPaths(res, app, analyzer);
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
                                        final ServerPagesApp app,
                                        final RestPathsAnalyzer analyzer) {
        res.append(TAB).append("Mapped handlers:").append(NEWLINE);
        final List<String> overrides = new ArrayList<>();
        final List<String> unreachable = new ArrayList<>();
        for (Map.Entry<String, String> entry : app.views.getPrefixes().entrySet()) {
            // sub url (related to application root)
            final String sub = entry.getKey();
            // mapping rest prefix for this sub url
            final String prefix = PathUtils.prefixSlash(entry.getValue());
            for (ResourcePath handle : analyzer.select(prefix)) {
                reportRestMappingPath(overrides, unreachable, sub, prefix, handle, res, app);
            }
            overrides.add(sub);
        }

        if (!unreachable.isEmpty()) {
            res.append(NEWLINE).append(TAB).append("(!) Unreachable handlers:").append(NEWLINE);
            for (String path : unreachable) {
                res.append(TAB).append(TAB).append(path).append(NEWLINE);
            }
        }
    }

    private static void reportRestMappingPath(final List<String> overrides,
                                              final List<String> unreachable,
                                              final String subUrl,
                                              final String prefix,
                                              final ResourcePath handle,
                                              final StringBuilder res,
                                              final ServerPagesApp app) {
        String relativeUrl = handle.getUrl().substring(prefix.length());

        // check if rest, registered on suburl overrides this resources, making it unreachable
        for (String url : overrides) {
            if (relativeUrl.startsWith(url)) {
                unreachable.add(String.format("%-7s %s (%s #%s) of %s mapping hidden by %s mapping",
                        handle.getMethod().getHttpMethod(),
                        handle.getUrl(),
                        RenderUtils.getClassName(handle.getKlass()),
                        handle.getMethod().getInvocable().getDefinitionMethod().getName(),
                        PathUtils.path(app.fullUriPath, subUrl) + STAR,
                        PathUtils.path(app.fullUriPath, url) + STAR));
                return;
            }
        }

        // add context mapping sub url
        relativeUrl = PathUtils.path(subUrl, relativeUrl);

        final Method handlingMethod = handle.getMethod().getInvocable().getHandlingMethod();
        final boolean disabledErrors = handle.getKlass().isAnnotationPresent(ManualErrorHandling.class)
                || (handlingMethod != null && handlingMethod.isAnnotationPresent(ManualErrorHandling.class));

        res.append(TAB).append(TAB).append(String.format("%-7s %s  (%s #%s)%s",
                handle.getMethod().getHttpMethod(),
                PathUtils.cleanUpPath(app.fullUriPath + relativeUrl),
                handle.getKlass().getName(),
                handle.getMethod().getInvocable().getDefinitionMethod().getName(),
                disabledErrors ? " [DISABLED ERRORS]" : ""
        )).append(NEWLINE);
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
