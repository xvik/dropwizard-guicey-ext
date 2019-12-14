package ru.vyarus.guicey.gsp.app.asset;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import ru.vyarus.dropwizard.guice.module.installer.util.PathUtils;

/**
 * Application classpath resources configuration object. Assets may be merged from multiple classpath locations.
 * Moreover, classpath locations could be mapped to exact urls.
 *
 * @author Vyacheslav Rusakov
 * @since 28.11.2019
 */
public class AssetSources {

    private final Multimap<String, String> locations = LinkedHashMultimap.create();

    /**
     * Register one root asset location.
     *
     * @param location asset classpath location
     */
    public void attach(final String location) {
        attach("/", location);
    }

    /**
     * Register location for exact url path (path-mapped locations override root mappings too).
     * <p>
     * Internally, path used without first slash to simplify matching. Location could be declared as pure package
     * ('dot' separated path).
     *
     * @param url     sub url
     * @param location asset classpath location
     */
    public void attach(final String url, final String location) {
        locations.put(PathUtils.normalizeRelativePath(url), PathUtils.normalizeClasspathPath(location));
    }

    /**
     * @return configured assets classpath locations by url
     */
    public Multimap<String, String> getLocations() {
        return locations;
    }

    /**
     * Merge location configurations (in-app config with global extensions).
     *
     * @param locations other locations configuration
     */
    public void merge(final AssetSources locations) {
        this.locations.putAll(locations.locations);
    }
}
