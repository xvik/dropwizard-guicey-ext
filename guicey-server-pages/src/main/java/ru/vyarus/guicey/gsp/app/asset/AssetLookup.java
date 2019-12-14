package ru.vyarus.guicey.gsp.app.asset;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Multimap;
import ru.vyarus.dropwizard.guice.module.installer.util.PathUtils;
import ru.vyarus.guicey.gsp.app.util.ResourceLookup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Assets are stored in classpath. Multiple classpath locations could be mapped for assets
 * (overriding default location). Some assets could be mapped into exact urls (with ability to override too).
 *
 * @author Vyacheslav Rusakov
 * @since 26.11.2019
 */
public class AssetLookup implements Serializable {

    // primary location is important because assets servlet by default will compute path relative to it
    private final String primaryLocation;
    private final Multimap<String, String> locations;

    public AssetLookup(final String primaryLocation, final Multimap<String, String> locations) {
        // primary location without leading slash!
        this.primaryLocation = primaryLocation;
        // assume immutable map, properly built: keys sorted from longest to smaller (root locations last),
        // locations for each path are reversed to logically preserve registration order (resource registered later
        // overrides other resources)
        this.locations = locations;
    }

    /**
     * @return main application assets classpath path
     */
    public String getPrimaryLocation() {
        return primaryLocation;
    }

    /**
     * @return assets mapping paths
     */
    public Multimap<String, String> getLocations() {
        // immutable
        return locations;
    }

    /**
     * Checks if provided path is absolute path into primary location and returns relative url instead.
     *
     * @param path possibly absolute path
     * @return relative path (without primary loctation part if detected)
     */
    public String getRelativePath(final String path) {
        String relativePath = CharMatcher.is('/').trimLeadingFrom(path);
        if (path.startsWith(primaryLocation)) {
            relativePath = path.substring(primaryLocation.length());
        }
        return relativePath;
    }

    /**
     * Lookup asset in classpath by url (relative to application root).
     * Assets, registered to exact url are processed in priority. For example, if assets registered for '/foo/bar/'
     * path then url '/foo/bar/sample.css' will be checked first in url-specific assets. Multiple asset packages
     * copuld be configured on each url: assets checked in registration-reverse order to grant regitstration
     * order priority (resources from package, registered later are prioritized).
     *
     * @param path path to find asset for
     * @return classpath path to resource or null if resource not found
     */
    public String lookup(final String path) {
        final String relativePath = getRelativePath(path);
        String res = null;
        final String assetPath = CharMatcher.is('/').trimLeadingFrom(relativePath);
        for (String url : locations.keySet()) {
            if (assetPath.startsWith(url)) {
                // root locations url will go last and will be ''
                res = ResourceLookup.lookup(url.length() > 0 ? assetPath.substring(url.length()) : assetPath,
                        locations.get(url));
                if (res != null) {
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Supposed to be used for error reporting. Return locations in package notion.
     *
     * @param path url relative to application root
     * @return matching classpath locations (not where resource is found, but where it is searched)
     */
    public List<String> getMatchingLocations(final String path) {
        final List<String> matches = new ArrayList<>();
        final String relativePath = getRelativePath(path);
        final String assetPath = CharMatcher.is('/').trimLeadingFrom(relativePath);
        for (String url : locations.keySet()) {
            if (assetPath.startsWith(url)) {
                for (String loc : locations.get(url)) {
                    // prefix with folder for better understanding context
                    matches.add(PathUtils.trimSlashes(url + loc).replace("/", "."));
                }
            }
        }
        return matches;
    }
}
