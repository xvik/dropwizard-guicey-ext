package ru.vyarus.guicey.gsp.info;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.ServerPagesApp;
import ru.vyarus.guicey.gsp.info.model.GspApp;
import ru.vyarus.guicey.spa.SpaBundle;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Guicey service providing information about registered server pages applications. Useful for reporting or diagnostic.
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2019
 */
@Singleton
public class GspInfoService {

    private final GlobalConfig config;

    public GspInfoService(final GlobalConfig config) {
        this.config = config;
    }

    /**
     * @return names of registered dropwizard-views renderers
     */
    public List<String> getViewRendererNames() {
        checkLock();
        return config.getRenderers().stream().map(ViewRenderer::getConfigurationKey).collect(Collectors.toList());
    }

    /**
     * @return list or registered dropwizard-views registered renderers
     */
    public List<ViewRenderer> getViewRenderers() {
        checkLock();
        return ImmutableList.copyOf(config.getRenderers());
    }

    /**
     * @return views configuration (including all customizations)
     */
    public Map<String, Map<String, String>> getViewsConfig() {
        checkLock();
        return ImmutableMap.copyOf(config.getViewsConfig());
    }

    /**
     * @return registered gsp applications info
     */
    public List<GspApp> getApplications() {
        checkLock();
        final List<GspApp> res = new ArrayList<>();
        for (ServerPagesApp app : config.getApps()) {
            res.add(map(app));
        }
        return res;
    }

    /**
     * @param name application name
     * @return application info or null if no application with provided name registered
     */
    public GspApp getApplication(final String name) {
        checkLock();
        for (ServerPagesApp app : config.getApps()) {
            if (name.equals(app.name)) {
                return map(app);
            }
        }
        return null;
    }

    private void checkLock() {
        // configuration is locked in view bundle (dw), which is called after guice bundle,
        // so info will become available after guice context start
        Preconditions.checkArgument(config.isLocked(), "GSP bundle is not yet initialized");
    }

    private GspApp map(final ServerPagesApp app) {
        final GspApp res = new GspApp();
        res.setName(app.name);
        res.setMainContext(app.mainContext);
        res.setMappingUrl(app.uriPath);
        res.setRootUrl(app.fullUriPath);
        res.setRequiredRenderers(app.requiredRenderers == null ? Collections.emptyList() : app.requiredRenderers);

        res.setMainAssetsLocation(app.mainAssetsPath);
        res.setAssets(app.assets.getLocations());
        res.setHasAssetExtensions(config.getAssetExtensions(app.name) != null);
        res.setViews(app.views.getPrefixes());
        res.setHasViewsExtensions(config.getViewExtensions(app.name) != null);
        res.setRestRootUrl(app.templateRedirect.getRootPath());

        res.setIndexFile(app.indexFile);
        res.setFilesRegex(app.fileRequestPattern);
        res.setHasDefaultFilesRegex(app.fileRequestPattern.equals(ServerPagesBundle.FILE_REQUEST_PATTERN));

        res.setSpa(app.spaSupport);
        res.setSpaRegex(app.spaNoRedirectRegex);
        res.setHasDefaultSpaRegex(app.spaNoRedirectRegex.equals(SpaBundle.DEFAULT_PATTERN));

        res.setErrorPages(app.errorPages);
        return res;
    }
}
