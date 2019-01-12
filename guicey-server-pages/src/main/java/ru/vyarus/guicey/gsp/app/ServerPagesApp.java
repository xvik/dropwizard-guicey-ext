package ru.vyarus.guicey.gsp.app;

import com.google.common.base.Joiner;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.injector.lookup.InjectorProvider;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.asset.LazyLocationProvider;
import ru.vyarus.guicey.gsp.app.asset.MultiSourceAssetServlet;
import ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.rest.DirectTemplateResource;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorHandler;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.spa.SpaBundle;
import ru.vyarus.guicey.spa.filter.SpaRoutingFilter;

import javax.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static ru.vyarus.guicey.spa.SpaBundle.SLASH;

/**
 * Server pages application initialization logic.
 * Application register:
 * <ul>
 * <li>Special assets servlet (with multiple classpath locations support)</li>
 * <li>Main {@link ServerPagesFilter} around assets servlet which differentiate asset and template requests
 * (and handle error pages)</li>
 * <li>if required, {@link SpaRoutingFilter} could be applied in order to support SPA routing.</li>
 * </ul>
 *
 * @author Vyacheslav Rusakov
 * @since 11.01.2019
 */
@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:ClassDataAbstractionCoupling",
        "PMD.ExcessiveImports"})
public class ServerPagesApp implements ApplicationEventListener {

    public boolean mainContext;
    public String name;
    public String resourcePath;
    public String uriPath;
    public String indexFile = "";
    // regexp for file requests detection (to recognize asset or direct template render)
    public String fileRequestPattern = ServerPagesBundle.FILE_REQUEST_PATTERN;

    public boolean spaSupport;
    public String spaNoRedirectRegex = SpaBundle.DEFAULT_PATTERN;

    public final Map<Integer, String> errorPages = new TreeMap<>();
    public boolean logErrors;

    protected Environment environment;
    protected TemplateRedirect templateRedirect;
    protected LazyLocationProvider locationsProvider;

    private final GlobalConfig globalConfig;

    // intentionally use main bundle class for logging
    private final Logger logger = LoggerFactory.getLogger(ServerPagesBundle.class);

    public ServerPagesApp(final GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    @Override
    public void onEvent(final ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            completeInitialization();
        }
    }

    @Override
    public RequestEventListener onRequest(final RequestEvent requestEvent) {
        return null;
    }

    /**
     * Install configured server page app.
     *
     * @param environment dropwizard environment object
     */
    public void setup(final Environment environment) {
        final ServletEnvironment context = mainContext ? environment.servlets() : environment.admin();

        // application extensions could be registered a bit later so it is impossible tokow all classpath
        // paths at that point
        locationsProvider = new LazyLocationProvider(
                resourcePath, name, globalConfig);

        installAssetsServlet(context, locationsProvider);

        if (spaSupport) {
            installSpaRoutingFilter(context);
        }

        // customizable error pages support
        final ErrorRedirect errorRedirect = new ErrorRedirect(uriPath, errorPages, logErrors);
        if (!errorPages.isEmpty()) {
            // global rest exception handler
            environment.jersey().register(new TemplateErrorHandler(errorRedirect));
        }

        // Templates support (filter must be above spaSupport filter)
        templateRedirect = new TemplateRedirect(name, uriPath, locationsProvider,
                new InjectorProvider(globalConfig.getApplication()));
        installTemplatesSupportFilter(context, templateRedirect, errorRedirect);
        // @Template annotation support (even with multiple registrations should be created just once)
        // note: applied only to annotated resources!
        environment.jersey().register(TemplateAnnotationFilter.class);
        // Default direct templates rendering rest (dynamically registered to handle "$appName/*")
        environment.jersey().getResourceConfig().registerResources(Resource.builder(DirectTemplateResource.class)
                .path(SLASH + name)
                .build());

        // Finalize initialization after server startup and print console report
        // delay is required to collect information from all app extensions
        this.environment = environment;
        environment.jersey().register(this);
    }

    /**
     * Special version of dropwizard {@link io.dropwizard.servlets.assets.AssetServlet} is used in order
     * to support resources lookup in multiple packages (required for app extensions mechanism).
     *
     * @param context           main or admin context
     * @param locationsProvider lazy resources location provider
     */
    private void installAssetsServlet(final ServletEnvironment context,
                                      final LazyLocationProvider locationsProvider) {
        final Set<String> clash = context.addServlet(name,
                // note: if index file is template, it will be handled by filter
                new MultiSourceAssetServlet(locationsProvider, uriPath, indexFile, StandardCharsets.UTF_8))
                .addMapping(uriPath + '*');

        if (clash != null && !clash.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Assets servlet %s registration clash with already installed servlets on paths: %s",
                    name, Joiner.on(',').join(clash)));
        }
    }

    /**
     * Install {@link SpaRoutingFilter} from {@link SpaBundle} to support SPA routing (return index page on html5
     * client routing calls). Filter installed above assets servlet. SPA index page may be a template.
     *
     * @param context main or admin context
     */
    private void installSpaRoutingFilter(final ServletEnvironment context) {
        final EnumSet<DispatcherType> spaTypes = EnumSet.of(DispatcherType.REQUEST);
        context.addFilter(name + "Routing", new SpaRoutingFilter(uriPath, spaNoRedirectRegex))
                .addMappingForServletNames(spaTypes, false, name);
    }

    /**
     * Install filter which recognize calls to templates and redirect to rest endpoint instead. This way
     * client dont know about rest and we use all benefits of rest parameters mapping.
     *
     * @param context          main or admin context
     * @param templateRedirect template redirection support
     * @param errorRedirect    error redirection support
     */
    private void installTemplatesSupportFilter(final ServletEnvironment context,
                                               final TemplateRedirect templateRedirect,
                                               final ErrorRedirect errorRedirect) {
        final EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST);
        context.addFilter(name + "Templates",
                new ServerPagesFilter(
                        uriPath,
                        fileRequestPattern,
                        indexFile,
                        templateRedirect,
                        errorRedirect,
                        globalConfig.getRenderers()))
                .addMappingForServletNames(types, false, name);
    }


    private void completeInitialization() {
        templateRedirect.setRootPath(PathUtils.endSlash(PathUtils.trimStars(environment.jersey().getUrlPattern())));

        // delayed compose of extended locations
        locationsProvider.get();

        logger.info(AppReportBuilder.build(this));
    }
}
