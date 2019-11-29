package ru.vyarus.guicey.gsp.app.asset.servlet;

import io.dropwizard.servlets.assets.AssetServlet;
import ru.vyarus.guicey.gsp.app.asset.AssetLookup;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import javax.annotation.Nullable;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Customized dropwizard {@link AssetServlet} which is able to search assets in multiple classpath locations.
 *
 * @author Vyacheslav Rusakov
 * @since 04.12.2018
 */
public class AssetResolutionServlet extends AssetServlet {
    private static final long serialVersionUID = 6393345594784987909L;

    private final AssetLookup assets;

    public AssetResolutionServlet(final AssetLookup assets,
                                  final String uriPath,
                                  @Nullable final String indexFile,
                                  @Nullable final Charset defaultCharset) {
        // asset servlet will work with single (main) assets location
        // main assets location placed last for overrides (.extendApp())
        //resourceLocations.get(resourceLocations.size() - 1)
        super(assets.getPrimaryLocation(), uriPath, indexFile, defaultCharset);
        this.assets = assets;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected URL getResourceUrl(final String absolutePath) {
        String realPath = absolutePath;
        // do nothing on root request (wait while index page will be requested)
        // otherwise look for resource in all registered locations
        if (!PathUtils.endSlash(realPath).equals(assets.getPrimaryLocation())) {
            realPath = assets.lookup(realPath);
        }
        // mimic default behaviour when resource not found
        return super.getResourceUrl(realPath);
    }
}
