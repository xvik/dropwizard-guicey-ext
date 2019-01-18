package ru.vyarus.guicey.gsp.app;

import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.rest.log.ResourcePath;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import java.util.Map;
import java.util.Set;

import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.NEWLINE;
import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.TAB;

/**
 * Builds server page application console report.
 *
 * @author Vyacheslav Rusakov
 * @since 12.01.2019
 */
public final class AppReportBuilder {

    private AppReportBuilder() {
    }

    /**
     * Build application report.
     *
     * @param app   server pages application instance
     * @param paths rest template paths belonging for application
     * @return application configuration report
     */
    public static String build(final ServerPagesApp app, final Set<ResourcePath> paths) {
        final StringBuilder res = new StringBuilder(String.format(
                "Server pages app '%s' registered on uri '%s' in %s context",
                app.name, app.uriPath + '*', app.mainContext ? "main" : "admin"));

        reportStaticResources(res, app);
        reportRestPaths(res, app, paths);
        reportErrorPages(res, app);

        return res.toString();
    }

    private static void reportStaticResources(final StringBuilder res, final ServerPagesApp app) {
        res.append(NEWLINE).append(NEWLINE)
                .append(TAB).append("Static resources locations:").append(NEWLINE);
        // delayed compose of extended locations
        for (String path : app.locationsProvider.get()) {
            res.append(TAB).append(TAB).append(PathUtils.SLASH).append(path).append(NEWLINE);
        }
        res.append(NEWLINE);
    }

    private static void reportRestPaths(final StringBuilder res,
                                        final ServerPagesApp app,
                                        final Set<ResourcePath> paths) {
        res.append(TAB).append("Mapped handlers:").append(NEWLINE);
        for (ResourcePath handle : paths) {
            res.append(TAB).append(TAB).append(String.format("%-7s %s  (%s #%s)",
                    handle.getMethod().getHttpMethod(),
                    PathUtils.cleanUpPath(app.uriPath + handle.getUrl().substring(app.name.length() + 1)),
                    handle.getKlass().getName(),
                    handle.getMethod().getInvocable().getDefinitionMethod().getName()
            )).append(NEWLINE);
        }
    }

    private static void reportErrorPages(final StringBuilder res, final ServerPagesApp app) {
        final Map<Integer, String> errorPages = app.errorPages;
        if (!errorPages.isEmpty()) {
            res.append(NEWLINE).append(TAB).append("Error pages:").append(NEWLINE);
            final int defKey = ErrorRedirect.DEFAULT_ERROR_PAGE;
            for (Map.Entry<Integer, String> entry : errorPages.entrySet()) {
                if (entry.getKey() != defKey) {
                    printErrorPage(res, entry.getKey().toString(), entry.getValue());
                }
            }
            if (errorPages.containsKey(defKey)) {
                printErrorPage(res, "*", errorPages.get(defKey));
            }
        }
    }

    private static void printErrorPage(final StringBuilder res, final String code, final String page) {
        res.append(TAB).append(TAB)
                .append(String.format("%-7s %s", code, PathUtils.prefixSlash(page)))
                .append(NEWLINE);
    }

}
