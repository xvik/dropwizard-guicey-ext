package ru.vyarus.guicey.spa;

import com.google.common.base.Joiner;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment;
import ru.vyarus.guicey.spa.filter.SpaRoutingFilter;

import javax.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides support for SPA (for example, angular apps).
 * Such applications often use html5 pretty url, like "/app/someroute/subroute".
 * When user bookmark such url or simply refresh page, browser requests complete url on server
 * and server must support it: redirect to main index page (without changing url, so client could handle routing).
 * <p>
 * Use dropwizard-assets servlet internally, but wraps it with special filter, which reacts on resource not found
 * errors (by default, all calls pass to assets filter!). Applies no-cache header for index page.
 * <p>
 * You can register multiple SPA applications on main or admin contexts (or both).
 * All applications must have unique names. In case of duplicate names only one application will be registered
 * (not registered bundle would be visible on
 * {@link ru.vyarus.dropwizard.guice.GuiceBundle.Builder#printDiagnosticInfo()} report).
 * Error will be thrown if multiple applications would mapped on the same path.
 *
 * @author Vyacheslav Rusakov
 * @since 02.04.2017
 */
@SuppressWarnings("PMD.ImmutableField")
public class SpaBundle implements GuiceyBundle {

    public static final String SLASH = "/";
    public static final String DEFAULT_PATTERN =
            "\\.(html|css|js|png|jpg|jpeg|gif|ico|xml|rss|txt|eot|svg|ttf|woff|woff2|cur)"
                    + "(\\?((r|v|rel|rev)=[\\-\\.\\w]*)?)?$";

    private final Logger logger = LoggerFactory.getLogger(SpaBundle.class);

    private boolean mainContext;
    private String assetName;
    private String resourcePath;
    private String uriPath;
    private String indexFile = "index.html";
    private String noRedirectRegex = DEFAULT_PATTERN;

    @Override
    public void run(final GuiceyEnvironment environment) {
        final Environment env = environment.environment();
        final ServletEnvironment context = mainContext ? env.servlets() : env.admin();

        final Set<String> clash = context.addServlet(assetName,
                new AssetServlet(resourcePath, uriPath, indexFile, StandardCharsets.UTF_8))
                .addMapping(uriPath + '*');

        if (clash != null && !clash.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Assets servlet %s registration clash with already installed servlets on paths: %s",
                    assetName, Joiner.on(',').join(clash)));
        }

        final EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST);
        context.addFilter(assetName + "Routing", new SpaRoutingFilter(uriPath, noRedirectRegex))
                .addMappingForServletNames(types, false, assetName);

        logger.info("SPA '{}' for source '{}' registered on uri '{}' in {} context",
                assetName, resourcePath, uriPath + '*', mainContext ? "main" : "admin");
    }

    @Override
    public int hashCode() {
        return assetName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        // consider bundles with the same name as equal and register only one of them
        return obj instanceof SpaBundle && assetName.equals(((SpaBundle) obj).assetName);
    }

    /**
     * Register SPA application in main context.
     * Note: application names must be unique (when you register multiple SPA applications.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for SPA configuration
     */
    public static Builder app(final String name, final String resourcePath, final String uriPath) {
        return new Builder(true, name, resourcePath, uriPath);
    }

    /**
     * Register SPA application in admin context.
     * Note: application names must be unique (when you register multiple SPA applications.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for SPA configuration
     */
    public static Builder adminApp(final String name, final String resourcePath, final String uriPath) {
        return new Builder(false, name, resourcePath, uriPath);
    }

    /**
     * Spa bundle builder.
     */
    public static class Builder {
        private final SpaBundle bundle = new SpaBundle();

        public Builder(final boolean mainContext,
                       final String name,
                       final String path,
                       final String uri) {
            bundle.mainContext = mainContext;
            bundle.assetName = checkNotNull(name, "Name is required");
            bundle.uriPath = uri.endsWith(SLASH) ? uri : (uri + SLASH);

            checkArgument(path.startsWith(SLASH), "%s is not an absolute path", path);
            checkArgument(!SLASH.equals(path), "%s is the classpath root", path);
            bundle.resourcePath = path.endsWith(SLASH) ? path : (path + SLASH);
        }

        /**
         * @param name index file name (by default "index.html")
         * @return builder instance
         */
        public Builder indexPage(final String name) {
            bundle.indexFile = name;
            return this;
        }

        /**
         * Redirect filter will prevent redirection when accept header is not compatible with text/html.
         * As this may not be enough, default regex {@link #DEFAULT_PATTERN} is used to prevent redirection in
         * some cases. By default it's all common web files (html, css, js) with possible version markers
         * (e.g. ?ver=1214324).
         * <p>
         * NOTE: regex is applied with "find", so use ^ or $ to apply boundaries.
         *
         * @param regex regular expression to prevent redirection to root (prevent when regex matched)
         * @return builder instance
         */
        public Builder preventRedirectRegex(final String regex) {
            bundle.noRedirectRegex = checkNotNull(regex, "Regex can't be null");
            return this;
        }

        /**
         * @return configured dropwizard bundle instance
         */
        public SpaBundle build() {
            return bundle;
        }
    }
}
