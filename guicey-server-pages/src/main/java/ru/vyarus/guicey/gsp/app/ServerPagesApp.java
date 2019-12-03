package ru.vyarus.guicey.gsp.app;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.server.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.asset.AssetLookup;
import ru.vyarus.guicey.gsp.app.asset.AssetSources;
import ru.vyarus.guicey.gsp.app.asset.servlet.AssetResolutionServlet;
import ru.vyarus.guicey.gsp.app.filter.ServerPagesFilter;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;
import ru.vyarus.guicey.gsp.app.filter.redirect.SpaSupport;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.app.rest.DirectTemplateResource;
import ru.vyarus.guicey.gsp.app.rest.log.RestPathsAnalyzer;
import ru.vyarus.guicey.gsp.app.rest.mapping.ViewRestLookup;
import ru.vyarus.guicey.gsp.app.rest.mapping.ViewRestSources;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.ViewRendererConfigurationModifier;
import ru.vyarus.guicey.spa.SpaBundle;

import javax.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        "PMD.ExcessiveImports", "PMD.TooManyFields"})
public class ServerPagesApp {

    // delayed modifiers registration
    public Map<String, ViewRendererConfigurationModifier> viewsConfigModifiers = new HashMap<>();
    // resources location registrations
    public AssetSources extendedAssetLocations = new AssetSources();
    // view rest prefixes mappings
    public ViewRestSources extendedViewPrefixes = new ViewRestSources();

    public final Map<Integer, String> errorPages = new TreeMap<>();
    public boolean mainContext;
    // application name
    public String name;
    // root assets location
    public String mainAssetsPath;
    // application mapping url
    public String uriPath;
    // context mapping + uriPath
    public String fullUriPath;
    public String indexFile = "";
    // regexp for file requests detection (to recognize asset or direct template render)
    public String fileRequestPattern = ServerPagesBundle.FILE_REQUEST_PATTERN;
    // required template renderer names
    public List<String> requiredRenderers;
    public boolean spaSupport;
    public String spaNoRedirectRegex = SpaBundle.DEFAULT_PATTERN;
    protected TemplateRedirect templateRedirect;
    // all locations, including all extensions
    protected AssetLookup assets;
    protected ViewRestLookup views;
    private boolean started;
    private final Logger logger = LoggerFactory.getLogger(ServerPagesApp.class);

    /**
     * Install configured server page app.
     *
     * @param environment dropwizard environment object
     * @param config      global configuration object
     */
    public void setup(final Environment environment, final GlobalConfig config) {
        final ServletEnvironment context = mainContext ? environment.servlets() : environment.admin();

        // apply possible context (if servlet registered not to root, e.g. most likely in case of flat admin context)
        final String contextMapping = mainContext
                ? environment.getApplicationContext().getContextPath()
                : environment.getAdminContext().getContextPath();
        fullUriPath = PathUtils.path(contextMapping, uriPath);

        assets = collectAssets(config);
        installAssetsServlet(context);
        views = collectViews(config);

        // templates support
        final SpaSupport spa = new SpaSupport(spaSupport, fullUriPath, uriPath, spaNoRedirectRegex);
        templateRedirect = new TemplateRedirect(environment.getJerseyServletContainer(),
                name,
                fullUriPath,
                views,
                assets,
                new ErrorRedirect(uriPath, errorPages, spa));
        installTemplatesSupportFilter(context, templateRedirect, spa, config.getRenderers());

        // Default direct templates rendering rest (dynamically registered to handle "$appName/*")
        registerDirectTemplateHandler(environment);
    }

    /**
     * Delayed initialization. Important to call when jersey initialization finished to get correct
     * rest path and make sure all extended registrations (other bundles extending app) are performed.
     *
     * @param restContext rest context mapping ( == main context mapping)
     * @param restMapping servlet mapping (under main context)
     * @param analyzer    rest analyzer
     */
    public void initialize(final String restContext, final String restMapping, final RestPathsAnalyzer analyzer) {
        templateRedirect.setRootPath(restContext, restMapping);
        logger.info(AppReportBuilder.build(this, analyzer));
        started = true;
    }

    /**
     * @return true if application already started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private AssetLookup collectAssets(final GlobalConfig config) {
        final AssetSources ext = config.getAssetExtensions(name);
        if (ext != null) {
            extendedAssetLocations.merge(ext);
        }

        final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.<String, String>builder()
                // order by size to correctly handle overlapped paths (e.g. /foo/bar checked before /foo)
                .orderKeysBy(Comparator.comparing(String::length).reversed());

        final Multimap<String, String> src = extendedAssetLocations.getLocations();
        for (String key : src.keySet()) {
            final String[] values = src.get(key).toArray(new String[0]);
            // reverse registered locations to preserve registration order priority during lookup
            ArrayUtils.reverse(values);
            builder.putAll(key, values);
        }

        // process paths the same way as assets servlet does
        return new AssetLookup(mainAssetsPath, builder.build());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private ViewRestLookup collectViews(final GlobalConfig config) {
        final ViewRestSources ext = config.getViewExtensions(name);
        if (ext != null) {
            extendedViewPrefixes.merge(ext);
        }

        // use application name as default mapping prefix if root mapping not configured
        // (neither directly nor by extension mechanism)
        if (!extendedViewPrefixes.getPrefixes().containsKey("")) {
            extendedViewPrefixes.map(name);
        }

        final ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap
                // order by size to correctly handle overlapped paths (e.g. /foo/bar checked before /foo)
                .<String, String>orderedBy(Comparator.comparing(String::length).reversed())
                .putAll(extendedViewPrefixes.getPrefixes());

        // process paths the same way as assets servlet does
        return new ViewRestLookup(builder.build());
    }

    private void registerDirectTemplateHandler(final Environment environment) {
        final String root = PathUtils.prefixSlash(views.getPrimaryMapping());
        // default handlers registered as instances, so it's enough to look only already registered instances
        for (Resource resource : environment.jersey().getResourceConfig().getResources()) {
            if (resource.getHandlerClasses().contains(DirectTemplateResource.class)
                    && resource.getPath().startsWith(root)) {
                // handler already registered (some other app use the same rest mapping as root)
                return;
            }
        }

        // register all missed default handlers
        environment.jersey().getResourceConfig().registerResources(Resource.builder(DirectTemplateResource.class)
                .path(root)
                .extended(false)
                .build());
    }

    /**
     * Special version of dropwizard {@link io.dropwizard.servlets.assets.AssetServlet} is used in order
     * to support resources lookup in multiple packages (required for app extensions mechanism).
     *
     * @param context main or admin context
     */
    private void installAssetsServlet(final ServletEnvironment context) {
        final Set<String> clash = context.addServlet(name,
                // note: if index file is template, it will be handled by filter
                new AssetResolutionServlet(assets, uriPath, indexFile, StandardCharsets.UTF_8))
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
                                               final SpaSupport spa,
                                               final List<ViewRenderer> renderers) {
        final EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);
        context.addFilter(name + "Templates",
                new ServerPagesFilter(
                        fullUriPath,
                        fileRequestPattern,
                        indexFile,
                        templateRedirect,
                        spa,
                        renderers))
                .addMappingForServletNames(types, false, name);
    }
}
