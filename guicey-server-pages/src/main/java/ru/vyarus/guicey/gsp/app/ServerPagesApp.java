package ru.vyarus.guicey.gsp.app;

import com.google.common.base.Joiner;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.injector.lookup.InjectorProvider;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.asset.LazyLocationProvider;
import ru.vyarus.guicey.gsp.app.asset.MultiSourceAssetServlet;
import ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.SpaSupport;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.rest.DirectTemplateResource;
import ru.vyarus.guicey.gsp.app.rest.log.ResourcePath;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.spa.SpaBundle;

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
 * </ul>
 *
 * @author Vyacheslav Rusakov
 * @since 11.01.2019
 */
@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:ClassDataAbstractionCoupling",
        "PMD.ExcessiveImports"})
public class ServerPagesApp {

    public boolean mainContext;
    public String name;
    public String resourcePath;
    public String uriPath;
    public String fullUriPath;
    public String indexFile = "";
    // regexp for file requests detection (to recognize asset or direct template render)
    public String fileRequestPattern = ServerPagesBundle.FILE_REQUEST_PATTERN;

    public boolean spaSupport;
    public String spaNoRedirectRegex = SpaBundle.DEFAULT_PATTERN;

    public final Map<Integer, String> errorPages = new TreeMap<>();
    public boolean logErrors;

    protected TemplateRedirect templateRedirect;
    protected LazyLocationProvider locationsProvider;

    private final GlobalConfig globalConfig;

    // intentionally use main bundle class for logging
    private final Logger logger = LoggerFactory.getLogger(ServerPagesBundle.class);

    public ServerPagesApp(final GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    /**
     * Install configured server page app.
     *
     * @param environment dropwizard environment object
     */
    public void setup(final Environment environment) {
        final ServletEnvironment context = mainContext ? environment.servlets() : environment.admin();

        // apply possible context (if servlet registered not to root, e.g. most likely in case of flat admin context)
        final String contextMapping = mainContext
                ? environment.getApplicationContext().getContextPath()
                : environment.getAdminContext().getContextPath();
        fullUriPath = PathUtils.path(contextMapping, uriPath);

        // application extensions could be registered a bit later so it is impossible to know all classpath
        // paths at that point
        locationsProvider = new LazyLocationProvider(resourcePath, name, globalConfig);
        installAssetsServlet(context, locationsProvider);

        // templates support
        final SpaSupport spa = new SpaSupport(spaSupport, fullUriPath, uriPath, spaNoRedirectRegex);
        templateRedirect = new TemplateRedirect(environment.getJerseyServletContainer(),
                name,
                fullUriPath,
                locationsProvider,
                new InjectorProvider(globalConfig.application),
                new ErrorRedirect(fullUriPath, errorPages, logErrors, spa));
        installTemplatesSupportFilter(context, templateRedirect, spa);

        // @Template annotation support (even with multiple registrations should be created just once)
        // note: applied only to annotated resources!
        environment.jersey().register(TemplateAnnotationFilter.class);
        // Default direct templates rendering rest (dynamically registered to handle "$appName/*")
        environment.jersey().getResourceConfig().registerResources(Resource.builder(DirectTemplateResource.class)
                .path(SLASH + name)
                .extended(false)
                .build());

        environment.jersey().register(this);
    }

    /**
     * Delayed initialization. Important to call when jersey initialization finished to get correct
     * rest path and make sure all extended registrations (other bundles extending app) are performed.
     *
     * @param restContext rest context mapping ( == main context mapping)
     * @param restMapping servlet mapping (under main context)
     * @param paths       rest template paths belonging to application
     */
    public void initialize(final String restContext, final String restMapping, final Set<ResourcePath> paths) {
        templateRedirect.setRootPath(restContext, restMapping);
        // delayed compose of extended locations
        locationsProvider.get();
        logger.info(AppReportBuilder.build(this, paths));
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
     * Install filter which recognize calls to templates and redirect to rest endpoint instead. This way
     * client dont know about rest and we use all benefits of rest parameters mapping.
     *
     * @param context          main or admin context
     * @param templateRedirect template redirection support
     */
    private void installTemplatesSupportFilter(final ServletEnvironment context,
                                               final TemplateRedirect templateRedirect,
                                               final SpaSupport spa) {
        final EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST);
        context.addFilter(name + "Templates",
                new ServerPagesFilter(
                        fullUriPath,
                        fileRequestPattern,
                        indexFile,
                        templateRedirect,
                        spa,
                        globalConfig.getRenderers()))
                .addMappingForServletNames(types, false, name);
    }
}
