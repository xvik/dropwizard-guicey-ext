package ru.vyarus.guicey.gsp.app.asset;

import io.dropwizard.servlets.assets.AssetServlet;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.app.util.ResourceLookup;

import javax.annotation.Nullable;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Customized dropwizard {@link AssetServlet} which is able to search assets in multiple classpath locations.
 *
 * @author Vyacheslav Rusakov
 * @since 04.12.2018
 */
public class MultiSourceAssetServlet extends AssetServlet {
    private static final long serialVersionUID = 6393345594784987909L;

    private final transient LazyLocationProvider locationsProvider;

    public MultiSourceAssetServlet(final LazyLocationProvider locationProvider,
                                   final String uriPath,
                                   @Nullable final String indexFile,
                                   @Nullable final Charset defaultCharset) {
        // asset servlet will work with single (main) assets location
        // main assets location placed last for overrides
        super(locationProvider.getPrimaryLocation(), uriPath, indexFile, defaultCharset);
        this.locationsProvider = locationProvider;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected URL getResourceUrl(final String absoluteRequestedResourcePath) {
        String realPath = absoluteRequestedResourcePath;
        // do nothing on root request (wait while index page will be requested)
        if (!PathUtils.endSlash(realPath).equals(this.locationsProvider.getPrimaryLocation())) {
            final List<String> locations = this.locationsProvider.get();
            // look for resource in all registered locations
            if (locations.size() > 1) {
                final String path = absoluteRequestedResourcePath.substring(
                        locationsProvider.getPrimaryLocation().length());
                realPath = ResourceLookup.lookup(path, locations);
            }
        }
        // mimic default behaviour when resource not found
        return super.getResourceUrl(realPath);
    }
}
