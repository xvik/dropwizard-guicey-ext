package ru.vyarus.guicey.gsp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewConfigurable;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap;
import ru.vyarus.dropwizard.guice.module.installer.util.Reporter;
import ru.vyarus.guicey.gsp.app.DelayedInitializer;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.ServerPagesApp;
import ru.vyarus.guicey.gsp.app.ServerPagesAppBundle;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorResponseFilter;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateExceptionListener;
import ru.vyarus.guicey.gsp.views.ConfiguredViewBundle;
import ru.vyarus.guicey.gsp.views.ViewRendererConfigurationModifier;
import ru.vyarus.guicey.gsp.views.template.ManualErrorHandling;
import ru.vyarus.guicey.spa.SpaBundle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static ru.vyarus.guicey.spa.SpaBundle.SLASH;

/**
 * Bundle unifies dropwizard-views and dropwizard-assets bundles in order to bring server templating
 * simplicity like with jsp. The main goal is to make views rendering through rest endpoints hidden and
 * make template calls by their files to simplify static resources references (css ,js, images etc.). Also,
 * errors handling is unified (like in usual servlets, but individually for server pages application).
 * <p>
 * First of all global server pages support bundle must be installed ({@link #builder()}, preferably directly in the
 * application class). This will activates dropwizard-views support ({@link ViewBundle}). Do not register
 * {@link ViewBundle} it manually!
 * <p>
 * Each server pages application is also registered
 * as separate bundle (using {@link #app(String, String, String)} or {@link #adminApp(String, String, String)}).
 * <p>
 * Views configuration could be mapped from yaml file in main bundle:
 * {@link ViewsBuilder#viewsConfiguration(ViewConfigurable)}. In order to fine tune configuration use
 * {@link AppBuilder#viewsConfigurationModifier(String, ViewRendererConfigurationModifier)} which could be used by
 * applications directly in order to apply required defaults. But pay attention that multiple apps could collide in
 * configuration (configure the same property)! Do manual properties merge instead of direct value set where possible
 * to maintain applications compatibility (e.g. you declare admin dashboard and main users app, which both use
 * freemarker and require default templates).
 * <p>
 * Renderers (pluggable template engines support) are loaded with service lookup mechanism (default for
 * dropwizard-views) but additional renderers could be registered with
 * {@link ViewsBuilder#addViewRenderers(ViewRenderer...)}. Most likely, server page apps will be bundled as 3rd party
 * bundles and so they can't be sure what template engines are installed in target application. Use
 * {@link AppBuilder#requireRenderers(String...)} to declare required template engines for each application and
 * fail fast if no required templates engine. Without required engines declaration template files will be served like
 * static files when direct template requested and rendering will fail for rest-mapped template.
 * <p>
 * Pay attention that application bundles are dropwizard bundles (not guicey bundles) so register it directly in
 * bootstrap object. This is required to be able to register multiple server applications. It could be also be
 * registered within {@link ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle} using builder register
 * method ({@link AppBuilder#register(GuiceyBootstrap)}).
 * <p>
 * Each application could be "extended" using {@link ServerPagesBundle#extendApp(String, String)}. This way extra
 * classpath location is mapped into application root. Pages from extended context could reference resources from
 * the main context (most likely common root template will be used). Also, extended mapping could override
 * resources from the primary location (but note that in case of multiple extensions order is not granted).
 * Obvious case for extensions feature is dashboards, when extensions add extra pages to common dashboard
 * application, but all new pages still use common master template.
 * <p>
 * Application work scheme: assets servlet is registered on the configured path in order to serve static assets
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
 * (or direct templates). But in order to add new pages, handled by rest resources you dont need to do anything -
 * they just must start with correct prefix (you can see all application resources in console just after startup).
 * <p>
 * In order to be able to render direct templates (without supporting rest endpoint) special rest
 * endpoint is registered which handles everything on application path (e.g. "ui/{file:.*}" for example application
 * above). Only POST and GET supported for direct templates.
 * <p>
 * Bundle unifies custom pages handling to be able to use default 404 or 500 pages (for both assets and resources).
 * Use builder {@link AppBuilder#errorPage(int, String)} method to map template (or pure html)
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
@SuppressWarnings("PMD.ExcessiveImports")
public class ServerPagesBundle implements ConfiguredBundle<Configuration> {

    /**
     * Default pattern for file request detection.
     *
     * @see AppBuilder#filePattern(String)
     */
    public static final String FILE_REQUEST_PATTERN = "(?:^|/)([^/]+\\.(?:[a-zA-Z\\d]+))(?:\\?.+)?$";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPagesBundle.class);

    // dropwizard initialization is single threaded so using thread local
    // to control asset uniqueness (important for filters registration) and view bundle configuration
    private static final ThreadLocal<GlobalConfig> GLOBAL_CONFIG = new ThreadLocal<>();

    private final GlobalConfig config;

    public ServerPagesBundle(final GlobalConfig config) {
        this.config = config;
        config.globalBundleCreated();
    }

    /**
     * Creates global server pages support bundle which must be registered in the application. Bundle
     * installs standard dropwizard views bundle ({@link ViewBundle}). If views bundle is manually declared in
     * application, it must be removed (to avoid duplicates). View bundle owning is required for proper configuration
     * and to know all used template engines (renderers).
     * <p>
     * After global support is registered, server pages applications may be declared with
     * {@link #app(String, String, String)} and {@link #adminApp(String, String, String)}.
     * <p>
     * It is assumed that global bundles support is registered directly in the dropwizard application
     * (and not transitively in some bundle) and server page applications themselves could be registered
     * nearby (in dropwizard application) or in any bundle (for example, some dashboard bundle just registers
     * dashboard application, assuming that global server pages support would be activated).
     *
     * @return global views bundle builder
     */
    public static ViewsBuilder builder() {
        return new ServerPagesBundle.ViewsBuilder(getOrCreateConfig());
    }

    /**
     * Register application in main context.
     * Application names must be unique (when you register multiple server pages applications).
     * <p>
     * Application could be extended with {@link AppBuilder#extendApp(String, String)} in another
     * bundle.
     * <p>
     * NOTE global server pages support bundle must be installed with {@link #builder()} in dropwizard application.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for server pages application configuration
     * @see #builder()  for server pages applications global support
     */
    public static AppBuilder app(final String name, final String resourcePath, final String uriPath) {
        LOGGER.debug("Registering server pages application {} on path {} with resources in {}",
                name, uriPath, resourcePath);
        return new AppBuilder(true, name, resourcePath, uriPath, getOrCreateConfig());
    }

    /**
     * Register application in admin context.
     * Application names must be unique (when you register multiple server pages applications).
     * <p>
     * Application could be extended with {@link AppBuilder#extendApp(String, String)} in another
     * bundle.
     * <p>
     * NOTE global server pages support bundle must be installed with {@link #builder()} in dropwizard application.
     *
     * @param name         application name (used as servlet name)
     * @param resourcePath path to application resources (classpath)
     * @param uriPath      mapping uri
     * @return builder instance for server pages application configuration
     * @see #builder()  for server pages applications global support
     */
    public static AppBuilder adminApp(final String name,
                                      final String resourcePath,
                                      final String uriPath) {
        LOGGER.debug("Registering admin server pages application {} on path {} with resources in {}",
                name, uriPath, resourcePath);
        return new AppBuilder(false, name, resourcePath, uriPath, getOrCreateConfig());
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
     * <p>
     * If extended application is not registered no error will be thrown. This behaviour support optional application
     * extension support (extension will work if extended application registered and will not harm if not).
     *
     * @param name         extended application name
     * @param resourcePath classpath location for additional resources
     * @throws IllegalStateException if target application is already initialized
     */
    public static void extendApp(final String name, final String resourcePath) {
        LOGGER.debug("Registering {} server pages application resources extension: {}", name, resourcePath);
        getOrCreateConfig().extendLocation(name, resourcePath);
    }

    /**
     * Remove current global configuration. This is required in tests when bundle initialization errors
     * are checked because otherwise global config is not marked as shutdown and so being re-used. In real application
     * bundle fails are not tested and so this method will not be required (after app startup global context will be
     * properly marked and so will not affect consequent tests).
     * <p>
     * WARNING: intended to be used by internal tests only (because global context listens for app shutdowns and so
     * not cause problems neither in usual run nor in tests).
     */
    @VisibleForTesting
    public static void resetGlobalConfig() {
        GLOBAL_CONFIG.remove();
    }

    private static GlobalConfig getOrCreateConfig() {
        // one config instance must be used for all server pages bundles initialized with single dw app
        if (GLOBAL_CONFIG.get() == null || GLOBAL_CONFIG.get().isShutdown()) {
            LOGGER.debug("Initializing global server pages configuration");
            GLOBAL_CONFIG.set(new GlobalConfig());
        }
        return GLOBAL_CONFIG.get();
    }

    /**
     * Method is available for custom template detection logic (similar that used inside server pages filter)
     * or to validate state in tests.
     *
     * @return list of used renderers (supported template engines)
     */
    public List<ViewRenderer> getRenderers() {
        return ImmutableList.copyOf(config.getRenderers());
    }

    /**
     * Method is available for custom views configuration state analysis logic (after startup) or to validate
     * state in tests.
     *
     * @return final views configuration object (unmodifiable)
     * @throws NullPointerException if views configuration is not yet created (views ot initialized)
     */
    public Map<String, Map<String, String>> getViewsConfig() {
        return ImmutableMap.copyOf(checkNotNull(config.getViewsConfig(),
                "Views configuration is not created yet"));
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        // not needed
    }

    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        LOGGER.debug("Perform global server pages initialization (views configuration)");
        // delayed apps init finalization (common for all registered apps)
        new DelayedInitializer(config, environment);

        // @Template annotation support (even with multiple registrations should be created just once)
        // note: applied only to annotated resources!
        environment.jersey().register(TemplateAnnotationFilter.class);

        // template rest errors interception (global handlers)
        environment.jersey().register(TemplateErrorResponseFilter.class);
        environment.jersey().register(TemplateExceptionListener.class);

        // automatically add engines from classpath lookup
        final Iterable<ViewRenderer> renderers = ServiceLoader.load(ViewRenderer.class);
        renderers.forEach(config::addRenderers);
        Preconditions.checkState(!config.getRenderers().isEmpty(),
                "No template engines found (dropwizard views renderer)");

        // configure views bundle (can't be registered in bootstrap as this point is in run phase)
        new ConfiguredViewBundle(config).run(configuration, environment);
        config.initialized();

        final StringBuilder res = new StringBuilder("Available dropwizard-views renderers:")
                .append(Reporter.NEWLINE).append(Reporter.NEWLINE);
        for (ViewRenderer renderer : config.getRenderers()) {
            res.append(Reporter.TAB).append(String.format(
                    "%-15s (%s)", renderer.getConfigurationKey(), renderer.getClass().getName()))
                    .append(Reporter.NEWLINE);
        }
        LOGGER.info(res.toString());
    }

    /**
     * Global server pages support bundle builder.
     */
    public static class ViewsBuilder {

        private final GlobalConfig config;

        protected ViewsBuilder(final GlobalConfig config) {
            this.config = config;
        }

        /**
         * Additional view renderers (template engines support) to use for {@link ViewBundle} configuration.
         * Duplicate renderers are checked by renderer key (e.g. "freemarker" or "mustache") and removed.
         * <p>
         * NOTE: default renderers are always loaded with service loader mechanism so registered listeners could only
         * extend the list of registered renderers (for those renderers which does not provide descriptor
         * for service loading).
         *
         * @param renderers renderers to use for global dropwizard views configuration
         * @return builder instance for chained calls
         * @see ViewBundle#ViewBundle(Iterable)
         */
        public ViewsBuilder addViewRenderers(final ViewRenderer... renderers) {
            config.addRenderers(renderers);
            return this;
        }

        /**
         * Configures configuration provider for {@link ViewBundle} (usually mapping from yaml configuration).
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
        public <T extends Configuration> ViewsBuilder viewsConfiguration(
                final ViewConfigurable<T> configurable) {
            config.setConfigurable(configurable);
            return this;
        }

        /**
         * Dropwizard views configuration modification. In contrast to views configuration object provider
         * ({@link #viewsConfiguration(ViewConfigurable)}), this method is not global and so modifications
         * from all registered server page applications will be applied.
         * <p>
         * The main use case is configuration of the exact template engine. For example, in case of freemarker
         * this could be used to apply auto includes:
         * <pre>{@code  .viewsConfigurationModifier("freemarker", config -> config
         *                         // expose master template
         *                         .put("auto_include", "/com/my/app/ui/master.ftl"))}</pre>
         * <p>
         * Note that configuration object is still global (because dropwizard views support is global) and so
         * multiple server page applications could modify configuration. For example, if multiple applications will
         * declare auto includes (example above) then only one include will be actually used. Use
         * {@link ViewsBuilder#printViewsConfiguration()} to see the final view configuration.
         *
         * @param name     renderer name (e.g. freemarker, mustache, etc.)
         * @param modifier modification callback
         * @return builder instance for chained calls
         */
        public ViewsBuilder viewsConfigurationModifier(
                final String name,
                final ViewRendererConfigurationModifier modifier) {
            // note: no need to log about it because it's global config (logs will appear if application register
            // configurer)
            config.addConfigModifier(name, modifier);
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
        public ViewsBuilder printViewsConfiguration() {
            config.printConfiguration();
            return this;
        }

        /**
         * @return configured dropwizard bundle instance
         */
        public ServerPagesBundle build() {
            return new ServerPagesBundle(config);
        }
    }

    /**
     * Server pages application bundle builder.
     */
    public static class AppBuilder {
        private final GlobalConfig config;
        private final ServerPagesApp app;

        protected AppBuilder(final boolean mainContext,
                             final String name,
                             final String path,
                             final String uri,
                             final GlobalConfig config) {
            this.config = config;
            this.app = config.createApp(name);

            app.mainContext = mainContext;
            app.name = checkNotNull(name, "Name is required");
            app.uriPath = uri.endsWith(SLASH) ? uri : (uri + SLASH);

            checkArgument(path.startsWith(SLASH), "%s is not an absolute path", path);
            checkArgument(!SLASH.equals(path), "%s is the classpath root", path);
            app.resourcePath = path.endsWith(SLASH) ? path : (path + SLASH);
        }

        /**
         * Specifies required template types (view renderes) for application. This setting is optional and used only for
         * immediate application startup failing when no required renderer is configured in global server pages bundle
         * ({@link ServerPagesBundle#builder()}).
         * <p>
         * Without declaring required renderer, application will simply serve template files "as is" when no
         * appropriate renderer found (because template file will not be recognized as template).
         * <p>
         * Renderer name is a renderer configuration key, defined in {@link ViewRenderer#getConfigurationKey()}.
         *
         * @param names required renderer names
         * @return builder instance for chained calls
         */
        public AppBuilder requireRenderers(final String... names) {
            app.requiredRenderers = Arrays.asList(names);
            return this;
        }

        /**
         * Shortcut for {@link #spaRouting(String)} with default regexp.
         *
         * @return builder instance for chained calls
         */
        public AppBuilder spaRouting() {
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
        public AppBuilder spaRouting(final String noRedirectRegex) {
            if (noRedirectRegex != null) {
                app.spaNoRedirectRegex = noRedirectRegex;
            }
            app.spaSupport = true;
            return this;
        }

        /**
         * Declares index page (served for "/" calls). Index page may also be a template. If index view is handled
         * with a rest then simply leave as "" (default): resource on path "{restPath}/{appMapping}/"
         * will be used as root page.
         * <p>
         * Pay attention that index is not set by default to "index.html" because most likely it would be some
         * template handled with rest resource (and so it would be too often necessary to override default).
         *
         * @param name index file name (by default "")
         * @return builder instance for chained calls
         */
        public AppBuilder indexPage(final String name) {
            app.indexFile = name;
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
        public AppBuilder errorPage(final String path) {
            return errorPage(ErrorRedirect.DEFAULT_ERROR_PAGE, path);
        }

        /**
         * Show special page instead of response with specified status code.
         * Errors are intercepted both for assets and template rendering. For templates, jersey request listener
         * used to intercept actual exceptions (to be able to access actual exception inside error page).
         * Default dropwizard exception mapper will log error (as for usual rest).
         * <p>
         * Error pages should use {@link ru.vyarus.guicey.gsp.views.template.ErrorTemplateView} as (base) model
         * class in order to get access to context exception. It is not required, if error object itself not required
         * during rendering.
         * <p>
         * NOTE that error page is returned only if original request accept html response and otherwise no
         * error page will be shown. Intention here is to show human readable errors only for humans.
         * <p>
         * IMPORTANT: GSP errors mechanism override ExceptionMapper and dropwizard-view ErrorEntityWriter mechanisms
         * because exception is detected before them and request is redirected to error page. Both ExceptionMapper
         * and EntityWriter would be called, but their result will be ignored (still, ExceptionMapper is useful
         * for errors logging). This was done to avoid influence of global ExceptionMapper's to be sure custom
         * error page used. It is possible to ignore GSP error mechanism for exact rest methods by using
         * {@link ManualErrorHandling} annotation.
         *
         * @param code error code to map page onto
         * @param path either path to static resource (inside registered classpath path) or resource url
         *             (without app name prefix)
         * @return builder instance for chained calls
         * @see #errorPage(String) for global errors page
         */
        public AppBuilder errorPage(final int code, final String path) {
            checkArgument(code >= ErrorRedirect.CODE_400 || code == ErrorRedirect.DEFAULT_ERROR_PAGE,
                    "Only error codes (4xx, 5xx) allowed for mapping");
            app.errorPages.put(code, path);
            return this;
        }

        /**
         * Differentiation of template call from static resource is based on fact: static resources
         * have extensions. So when "/something/some.ext" is requested and extension is not supported template
         * extension then it's direct asset. In case when you have static files without extension, you can
         * include them directly into detection regexp (using regex or (|) syntax).
         * <p>
         * Pattern must return detected file name as first matched group (so direct template could be detected).
         * Pattern is searched (find) inside path, not matched (so simple patterns will also work).
         *
         * @param regex regex for file request detection and file name extraction
         * @return builder instance for chained calls
         * @see #FILE_REQUEST_PATTERN default pattern
         */
        public AppBuilder filePattern(final String regex) {
            app.fileRequestPattern = checkNotNull(regex, "Regex can't be null");
            return this;
        }

        /**
         * Dropwizard views configuration modification. Views configuration could be bound only in global server pages
         * support bundle ({@link ViewsBuilder#viewsConfiguration(ViewConfigurable)}). But it's often required to
         * "tune" template engine specifically for application. This method allows global views configuration
         * modification for exact server pages application.
         * <p>
         * The main use case is configuration of the exact template engine. For example, in case of freemarker
         * this could be used to apply auto includes:
         * <pre>{@code  .viewsConfigurationModifier("freemarker", config -> config
         *                         // expose master template
         *                         .put("auto_include", "/com/my/app/ui/master.ftl"))}</pre>
         * <p>
         * Note that configuration object is still global (becuase dropwizard views support is global) and so
         * multiple server pages applications could modify configuration. For example, if multiple applications will
         * declare auto includes (example above) then only one include will be actually used. Use
         * {@link ViewsBuilder#printViewsConfiguration()} to see the final view configuration.
         *
         * @param name     renderer name (e.g. freemarker, mustache, etc.)
         * @param modifier modification callback
         * @return builder instance for chained calls
         */
        public AppBuilder viewsConfigurationModifier(
                final String name,
                final ViewRendererConfigurationModifier modifier) {
            // in case of multiple applications, it should be obvious from logs who changed config
            LOGGER.info("Server pages application '{}' modifies '{}' section of views configuration",
                    app.name, name);
            config.addConfigModifier(name, modifier);
            return this;
        }

        /**
         * Note: if bundle is used inside guicey bundle then use {@link #register(GuiceyBootstrap)} for
         * bundle installation.
         *
         * @return configured dropwizard bundle instance
         */
        public ServerPagesAppBundle build() {
            return new ServerPagesAppBundle(config, app);
        }

        /**
         * Use when server pages bundle must be registered within guicey bundle
         * (guicey bundle could register custom application, e.g. for administration or simply register
         * customizations for existing application).
         *
         * @param bootstrap guicey bootstrap object
         */
        public void register(final GuiceyBootstrap bootstrap) {
            final ServerPagesAppBundle bundle = build();
            bundle.initialize(bootstrap.bootstrap());
            try {
                bundle.run(bootstrap.configuration(), bootstrap.environment());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to start server pages application "
                        + app.name, ex);
            }
        }
    }
}
