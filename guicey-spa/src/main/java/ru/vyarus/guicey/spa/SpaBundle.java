package ru.vyarus.guicey.spa;

import com.google.common.base.Joiner;
import io.dropwizard.Bundle;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.spa.filter.SpaRoutingFilter;

import javax.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides support for SPA (for example, angular apps).
 * Such applications often use html5 pretty url, like "/app/someroute/subroute".
 * When user bookmark such url or simply refresh page, browser requests complete url on server
 * and server must support it: redirect to main index page (without changing url, so client could handle routing).
 * <p>
 * Bundle is pure dropwizard bundle.
 * <p>
 * Use dropwizard-assets servlet internally, but wraps it with special filter, which reacts on resource not found
 * errors (by default, all calls pass to assets filter!).
 * <p>
 * You can register multiple SPA applications on main or admin contexts (or both).
 * All applications must have unique names. In case of duplicate names or mapping on the same path, error
 * will be thrown.
 *
 * @author Vyacheslav Rusakov
 * @since 02.04.2017
 */
@SuppressWarnings("PMD.ImmutableField")
public class SpaBundle implements Bundle {

    public static final String SLASH = "/";
    public static final String DEFAULT_PATTERN =
            "\\.(html|css|js|png|jpg|jpeg|gif|ico|xml|rss|txt|eot|svg|ttf|woff|woff2|cur)"
                    + "(\\?((r|v|rel|rev)=[\\-\\.\\w]*)?)?$";

    // dropwizard initialization is single threaded so using thread local
    // to control asset uniqueness (important for filters registration)
    private static final ThreadLocal<List<String>> USED_NAMES = new ThreadLocal<>();

    private final Logger logger = LoggerFactory.getLogger(SpaBundle.class);

    private boolean mainContext;
    private String assetName;
    private String resourcePath;
    private String uriPath;
    private String indexFile = "index.html";
    private String noRedirectRegex = DEFAULT_PATTERN;


    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        USED_NAMES.remove();
    }

    @Override
    public void run(final Environment environment) {

        List<String> used = USED_NAMES.get();
        if (used == null) {
            used = new ArrayList<>();
            USED_NAMES.set(used);
        }
        // important because name used for filter mapping
        checkArgument(!used.contains(assetName),
                "SPA with name '%s' is already registered", assetName);
        used.add(assetName);

        init(environment);
    }

    private void init(final Environment environment) {
        final ServletEnvironment context = mainContext ? environment.servlets() : environment.admin();

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
         * @return configured bundle instance
         */
        public SpaBundle build() {
            return bundle;
        }
    }
}
