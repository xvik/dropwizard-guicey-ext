package ru.vyarus.guicey.gsp;

import com.google.common.base.Throwables;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewConfigurable;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap;
import ru.vyarus.guicey.gsp.app.DelayedInitializer;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.ServerPagesApp;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorResponseFilter;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateExceptionListener;
import ru.vyarus.guicey.gsp.views.ViewRendererConfigurationModifier;
import ru.vyarus.guicey.gsp.views.ViewsSupport;
import ru.vyarus.guicey.spa.SpaBundle;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static ru.vyarus.guicey.spa.SpaBundle.SLASH;

/**
 * Bundle unifies dropwizard-views and dropwizard-assets bundles in order to bring serer templating
 * simplicity like with jsp. The main goal is to make views rendering through rest endpoints hidden and
 * make template calls by their files to simplify static resources references (css ,js, images etc.).
 * <p>
 * This is dropwizard bundle (not guicey bundle) so register it directly in bootstrap object. This is
 * required to be able to register multiple server applications. It could be also be registered within
 * {@link ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle} using builder register method
 * ({@link ServerPagesBundle.Builder#register(GuiceyBootstrap)}).
 * <p>
 * Bundle could be registered multiple times: one bundle per one server application. Also, each application
 * could be "extended" using {@link ServerPagesBundle#extendApp(String, String)}. This way extra
 * classpath location is mapped into application root. Pages from extended context could reference resources from
 * the main context (most likely common root template will be used). Also, extended mapping could override
 * resources from the primary location (but not that in case of multiple extensions order is not granted).
 * Obvious case for extensions feature is dashboards, when extensions adds extra pages to common dashboard
 * application, but all new pages still use common master template.
 * <p>
 * Bundle registers {@link ViewBundle} automatically. Do not register it manually! It is required in order to
 * control list of used renderers (supported template engines). Only one bundle could provide views configuration
 * (binding from main yaml configuration), but any bundle could modify this configuration (to tune exact
 * template engine). Renderers are loaded with service lookup mechanism (default for views) but additional
 * renderers could be registered in any bundle.
 * This conceptual inconsistency (single views bundle, many server pages bundles) should be kept in mind while
 * planning applications.
 * <p>
 * Work scheme: assets servlet is registered on the configured path in order to serve static assets
 * (customized version of dropwizard {@link io.dropwizard.servlets.assets.AssetServlet} used which could
 * recognize both primary and extended locations). Special filter above servlet detects file calls (by extension,
 * but checks if requested file is template (and that's why list of supported templates is required)). If request
 * is not for file, it's redirected to rest endpoint in order to render view (note that direct template files will
 * also be rendered). Redirection scheme use application name, defined during bundle creation:
 * {rest prefix}/{app name}/{path from request}.
 * For example,
 * {@code bootstrap.addBundle(SpaPageBundle.app("ui", "/com/assets/path/", "ui/").build())}
 * Register application in main context, mapped to  "ui/" path, with static resources in "/com/assets/path/"
 * classpath path. Internal application name is "ui". When browser request any file directly, e.g.
 * "ui/styles.css" then file "/com/assets/path/styles.css" will be served. Any other path is redirected to rest
 * endpoint: e.g. "ui/dashboard/" is redirected to "{rest mapping}/ui/dashboard.
 * <pre>
 * <code>
 *  {@literal @}Template("dashboard.ftl")
 *  {@literal @}Path("ui/dahboard")
 *  {@literal @}Produces(MediaType.TEXT_HTML)
 *  public class DashboardPage {
 *   {@literal @}GET
 *   public DashboardView get() {
 *      return new DashboardView();
 *   }
 *  }
 * </code>
 * </pre>
 * Note that {@link ru.vyarus.guicey.gsp.views.template.Template} annotation on resource is required. Without it,
 * bundle will not be able to show path in console reporting. Also, configured template automatically applied
 * into view (so you don't have to specify template path in all methods (note that custom template path could
 * still be specified directly, when required). View class must extend
 * {@link ru.vyarus.guicey.gsp.views.template.TemplateView}. In all other aspects, it's pure dropwizard views.
 * {@link ru.vyarus.guicey.gsp.views.template.Template} annotation is jersey {@link javax.ws.rs.NameBinding} marker
 * so you can apply request/response filters only (!) for template resources (see {@link TemplateAnnotationFilter}
 * as example).
 * <p>
 * Note that all resources, started from application name prefix are considered to be used in application.
 * {@link ServerPagesBundle#extendApp(String, String)} mechanism is used only to declare additional static resources
 * (or direct templates). But in order to add new pages, handled by resources you dont need to do anything -
 * they just must start with correct prefix (you can see all application resources in console just after startup).
 * <p>
 * In order to be able to render direct templates (without supporting rest endpoint) special rest
 * endpoint is registered which handles everything on application path (e.g. "ui/{file:.*}" for example application
 * above). Only POST and GET supported for direct templates. inside such template you can still access
 * guice beans using model {@link ru.vyarus.guicey.gsp.views.template.TemplateView#getService(Class)}.
 * <p>
 * Bundle unifies custom pages handling to be able to use default 404 or 500 pages (for both assets and resources).
 * Use builder {@link ServerPagesBundle.Builder#errorPage(int, String)} method to map template (or pure html)
 * to response code (to be shown instead).
 * <p>
 * Bundle could also enable filter from {@link ru.vyarus.guicey.spa.SpaBundle} in order to support single
 * page applications routing (for cases when root page must be template and not just html, which makes direct usage of
 * {@link ru.vyarus.guicey.spa.SpaBundle} useless).
 *
 * @author Vyacheslav Rusakov
 * @see <a href="https://www.dropwizard.io/1.3.5/docs/manual/views.html">dropwizard views</a>
 * @since 22.10.2018
 */
public class ServerPagesBundle implements ConfiguredBundle<Configuration> {
    /**
     * Default pattern for file request detection.
     *
     * @see ServerPagesBundle.Builder#filePattern(String)
     */
    public static final String FILE_REQUEST_PATTERN = "(?:^|/)([^/]+\\.(?:[a-zA-Z\\d]+))(?:\\?.+)?$";

    // dropwizard initialization is single threaded so using thread local
    // to control asset uniqueness (important for filters registration) and view bundle configuration
    private static final ThreadLocal<GlobalConfig> GLOBAL_CONFIG = new ThreadLocal<>();

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final ServerPagesApp app = new ServerPagesApp(GLOBAL_CONFIG.get());

    /**
     * Method is available for custom template detection logic (similar that used inside server pages filter)
     * or to validate state in tests.
     * <p>
     * Returned list represent global set of listeners (not only registered by this bundle, but by all bundles).
     * <p>
     * NOTE: the full list of bundles will be available only after startup! Before start, only currently
     * registered renderes will be available (or even no at all).
     *
     * @return list of used renderers (supported template engines)
     */
    public List<ViewRenderer> getRenderers() {
        return new ArrayList<>(GLOBAL_CONFIG.get().getRenderers());
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        GLOBAL_CONFIG.get().application = bootstrap.getApplication();
        GLOBAL_CONFIG.get().apps.add(app);
    }

    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        final GlobalConfig config = GLOBAL_CONFIG.get();
        if (!config.isInitialized()) {
            // delayed apps init finalization (common for all registered apps)
            new DelayedInitializer(config, environment);

            // template rest errors interception (global handlers)
            environment.jersey().register(TemplateErrorResponseFilter.class);
            environment.jersey().register(TemplateExceptionListener.class);

            // global dropwizard ViewBundle installation
            ViewsSupport.setup(config, configuration, environment);
        }

        // app specific initialization (create servlets, filters, etc)
        app.setup(environment);
    }

    /**
     * Register SPA application in main context.
     * Note: application names must be unique (when you register multiple server pages applications).
     * <p>
     * Application could be extended with {@link ServerPagesBundle.Builder#extendApp(String, String)} in another
     * bundle.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for SPA configuration
     */
    public static ServerPagesBundle.Builder app(final String name, final String resourcePath, final String uriPath) {
        initGlobalConfig();
        return new ServerPagesBundle.Builder(true, name, resourcePath, uriPath);
    }

    /**
     * Register SPA application in admin context.
     * Note: application names must be unique (when you register multiple server pages applications).
     * <p>
     * Application could be extended with {@link ServerPagesBundle.Builder#extendApp(String, String)} in another
     * bundle.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for SPA configuration
     */
    public static ServerPagesBundle.Builder adminApp(final String name,
                                                     final String resourcePath,
                                                     final String uriPath) {
        initGlobalConfig();
        return new ServerPagesBundle.Builder(false, name, resourcePath, uriPath);
    }

    /**
     * Extend application resources (classpath) with new location. May be used by bundles to add custom resources
     * into application or override existing resources. For example, if we register application like this
     * {@code ServerPagesBundle.app("ui", "/com/path/assets/", "/ui")} it will server static resources only from
     * "/com/path/assets/" package. Suppose we want to add another page (with direct template) into the app:
     * {@code ServerPagesBundle.extendApp("ui", "/com/another/assets/")}. Now assets will be searched in both packages
     * and if we have "/com/another/assets/page.tpl" then calling url "/ui/page.tpl" will render template.
     * Resource in extended location could override original app resource: e.g. if we have
     * "/com/another/assets/style.css" (extended) and "/com/path/assets/style.css" (original app) then
     * "/ui/style.css" will return extended resource file.
     * <p>
     * Note that if you just want to add new rest resources then simply prefix resource paths with application name
     * and they will be included automatically (in example above app name is "ui" and note that name is completely
     * internal and may not be the same as path mapping ("/ui" in example above).
     *
     * @param name         extended application name
     * @param resourcePath classpath location for additional resources
     */
    public static void extendApp(final String name, final String resourcePath) {
        initGlobalConfig();
        GLOBAL_CONFIG.get().extendLocation(name, resourcePath);
    }

    private static void initGlobalConfig() {
        // one config instance must be used for all server pages bundles initialized with single dw app
        if (GLOBAL_CONFIG.get() == null || GLOBAL_CONFIG.get().isInitialized()) {
            GLOBAL_CONFIG.set(new GlobalConfig());
        }
    }

    /**
     * Server pages bundle builder.
     */
    public static class Builder {
        private final ServerPagesBundle bundle = new ServerPagesBundle();

        protected Builder(final boolean mainContext,
                          final String name,
                          final String path,
                          final String uri) {
            GLOBAL_CONFIG.get().addAppName(name);

            bundle.app.mainContext = mainContext;
            bundle.app.name = checkNotNull(name, "Name is required");
            bundle.app.uriPath = uri.endsWith(SLASH) ? uri : (uri + SLASH);

            checkArgument(path.startsWith(SLASH), "%s is not an absolute path", path);
            checkArgument(!SLASH.equals(path), "%s is the classpath root", path);
            bundle.app.resourcePath = path.endsWith(SLASH) ? path : (path + SLASH);
        }

        /**
         * Shortcut for {@link #spaRouting(String)} with default regexp.
         *
         * @return builder instance for chained calls
         */
        public ServerPagesBundle.Builder spaRouting() {
            return spaRouting(null);
        }

        /**
         * Enable single page application html5 routing support.
         *
         * @param noRedirectRegex regex to match all cases when redirection not needed
         * @return builder instance for chained calls
         * @see SpaBundle for more info how it works
         * @see SpaBundle.Builder#preventRedirectRegex(String) for more info about regexp
         */
        public ServerPagesBundle.Builder spaRouting(final String noRedirectRegex) {
            if (noRedirectRegex != null) {
                bundle.app.spaNoRedirectRegex = noRedirectRegex;
            }
            bundle.app.spaSupport = true;
            return this;
        }

        /**
         * Index page may also be a template. If index view is handled with a rest then simply leave as "" (default).
         * In this case resource on path "{restPath}/{appMapping}/" will be used as root page.
         * <p>
         * Pay attention that index is not set by default to "index.html" because most likely it would be some
         * template handled with rest resource (and so it would be too often necessary to override default).
         *
         * @param name index file name (by default "")
         * @return builder instance for chained calls
         */
        public ServerPagesBundle.Builder indexPage(final String name) {
            bundle.app.indexFile = name;
            return this;
        }

        /**
         * Default error page (shown in case of exceptions and for all error return codes (&gt;=400)).
         *
         * @param path either path to static resource (inside registered classpath path) or resource url
         *             (without app name prefix)
         * @return builder instance for chained calls
         * @see #errorPage(int, String) for registereing error page on exact return code
         */
        public ServerPagesBundle.Builder errorPage(final String path) {
            return errorPage(ErrorRedirect.DEFAULT_ERROR_PAGE, path);
        }

        /**
         * Show special page instead of response with specified status code.
         * Errors are intercepted both for assets and template rendering. For templates, jersey request listener
         * used to intercept actual exceptions (to be able to access actual exception inside error page).
         * Default dropwizard exception mapper will log error (as for usual rest).
         * <p>
         * NOTE that error page is returned only if original request accept html response and otherwise no
         * error page will be shown. Intention here is to show human readable errors only for humans.
         *
         * @param code error code to map page onto
         * @param path either path to static resource (inside registered classpath path) or resource url
         *             (without app name prefix)
         * @return builder instance for chained calls
         * @see #errorPage(String) for global errors page
         */
        public ServerPagesBundle.Builder errorPage(final int code, final String path) {
            checkArgument(code >= ErrorRedirect.CODE_400 || code == ErrorRedirect.DEFAULT_ERROR_PAGE,
                    "Only error codes (4xx, 5xx) allowed for mapping");
            bundle.app.errorPages.put(code, path);
            return this;
        }

        /**
         * Differentiation of template call from static resource is based on fact: static resources
         * have extensions. So when "/something/some.ext" is requested and extension is not supported template
         * extension then it's direct asset. In case when you have static files without extension, you can
         * include them directly into detection regexp (using regex or (|) syntax).
         * <p>
         * Pattern must return detected file name as first matched group (so direct template could be detected).
         *
         * @param regex regex for file request detection and file name extraction
         * @return builder instance for chained calls
         * @see #FILE_REQUEST_PATTERN default pattern
         */
        public ServerPagesBundle.Builder filePattern(final String regex) {
            bundle.app.fileRequestPattern = checkNotNull(regex, "Regex can't be null");
            return this;
        }

        /**
         * Additional view renderers (template engines support) to use for {@link ViewBundle} configuration.
         * Duplicate renderers are checked by renderer key (e.g. "freemarker" or "mustache") and removed.
         * <p>
         * NOTE: default renderers are always loaded with service loader mechanism so registered listeners could only
         * extend the list of registered renderers (for those renderers which does not provide descriptor
         * for service loading).
         * <p>
         * Option is global and if two or more server page bundles will configure it,
         * all registered renderers will be used.
         *
         * @param renderers renderers to use for global dropwizard views configuration
         * @return builder instance for chained calls
         * @see ViewBundle#ViewBundle(Iterable)
         */
        public ServerPagesBundle.Builder addViewRenderers(final ViewRenderer... renderers) {
            GLOBAL_CONFIG.get().addRenderers(renderers);
            return this;
        }

        /**
         * Configures configuration provider for {@link ViewBundle} (usually mapping from yaml configuration).
         * <p>
         * Only one bundle could perform this configuration (usually the one directly in application). If
         * two multiple bundles try to configure this then error will be thrown.
         * <p>
         * Note that if you need to just modify configuration in one of server pages bundles, you can do this
         * with {@link #viewsConfigurationModifier(String, ViewRendererConfigurationModifier)} - special mechanism
         * to overcome global views limitation.
         *
         * @param configurable views configuration lookup.
         * @param <T>          configuration object type
         * @return builder instance for chained calls
         * @see ViewBundle#getViewConfiguration(Configuration)
         * @see #viewsConfigurationModifier(String, ViewRendererConfigurationModifier)
         * @see #printViewsConfiguration()
         */
        public <T extends Configuration> ServerPagesBundle.Builder viewsConfiguration(
                final ViewConfigurable<T> configurable) {
            GLOBAL_CONFIG.get().setConfigurable(configurable, bundle.app.name);
            return this;
        }

        /**
         * Dropwizard views configuration modification. In contrast to views configuration object provider
         * ({@link #viewsConfiguration(ViewConfigurable)}), this method is not global and so modifications
         * from all registered server bundles will be applied.
         * <p>
         * The main use case is configuration of the exact template engine. For example, in case of freemarker
         * this could be used to apply auto includes:
         * <pre>{@code  .viewsConfigurationModifier("freemarker", config -> config
         *                         // expose master template
         *                         .put("auto_include", "/com/my/app/ui/master.ftl"))}</pre>
         * <p>
         * Note: it may be useful to print the resulted configuration with {@link #printViewsConfiguration()}
         * in order to see all such changes.
         *
         * @param name     renderer name (e.g. freemarker, mustache, etc.)
         * @param modifier modification callback
         * @return builder instance for chained calls
         */
        public ServerPagesBundle.Builder viewsConfigurationModifier(
                final String name,
                final ViewRendererConfigurationModifier modifier) {
            GLOBAL_CONFIG.get().addConfigModifier(name, modifier);
            return this;
        }

        /**
         * Prints configuration object used for dropwizard views bundle ({@link ViewBundle}). Note that
         * initial views configuration object binding is configured with
         * {@link #viewsConfiguration(ViewConfigurable)} and it could be modified with
         * {@link #viewsConfigurationModifier(String, ViewRendererConfigurationModifier)}. Printing of the final
         * configuration (after all modification) could be useful for debugging.
         *
         * @return builder instance for chained calls
         */
        public ServerPagesBundle.Builder printViewsConfiguration() {
            GLOBAL_CONFIG.get().printConfiguration();
            return this;
        }

        /**
         * Note: if bundle is used inside guicey bundle then use {@link #register(GuiceyBootstrap)} for
         * bundle installation.
         *
         * @return configured dropwizard bundle instance
         */
        public ServerPagesBundle build() {
            return bundle;
        }

        /**
         * Use when server pages bundle must be registered within guicey bundle
         * (guicey bundle could register custom application, e.g. for administration or simply register
         * customizations for existing application).
         *
         * @param bootstrap guicey bootstrap object
         */
        public void register(final GuiceyBootstrap bootstrap) {
            bundle.initialize(bootstrap.bootstrap());
            try {
                bundle.run(bootstrap.configuration(), bootstrap.environment());
            } catch (Exception ex) {
                Throwables.throwIfUnchecked(ex);
                throw new IllegalStateException("Failed to initialize server pages module", ex);
            }
        }
    }
}
