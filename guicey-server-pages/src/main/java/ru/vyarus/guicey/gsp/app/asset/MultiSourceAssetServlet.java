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

    private final List<String> resourceLocations;

    public MultiSourceAssetServlet(final List<String> resourceLocations,
                                   final String uriPath,
                                   @Nullable final String indexFile,
                                   @Nullable final Charset defaultCharset) {
        // asset servlet will work with single (main) assets location
        // main assets location placed last for overrides (.extendApp())
        super(resourceLocations.get(resourceLocations.size() - 1), uriPath, indexFile, defaultCharset);
        this.resourceLocations = resourceLocations;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected URL getResourceUrl(final String absoluteRequestedResourcePath) {
        String realPath = absoluteRequestedResourcePath;
        // original registration location is always last (for potential overrides)
        final String primaryLocation = resourceLocations.get(resourceLocations.size() - 1);
        // do nothing on root request (wait while index page will be requested)
        // otherwise look for resource in all registered locations
        if (!PathUtils.endSlash(realPath).equals(primaryLocation) && resourceLocations.size() > 1) {
            final String path = absoluteRequestedResourcePath.substring(
                    primaryLocation.length());
            realPath = ResourceLookup.lookup(path, resourceLocations);
        }
        // mimic default behaviour when resource not found
        return super.getResourceUrl(realPath);
    }
}
