package ru.vyarus.guicey.gsp.app.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.asset.AssetLookup;
import ru.vyarus.guicey.gsp.views.template.TemplateNotFoundException;

import java.util.Iterator;

/**
 * Utility used to lookup static resources in multiple locations. This is used for applications extensions
 * mechanism when additional resources could be mapped into application from different classpath location.
 *
 * @author Vyacheslav Rusakov
 * @since 04.12.2018
 */
public final class ResourceLookup {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceLookup.class);

    private ResourceLookup() {
    }

    /**
     * Lookup path relative to class.
     *
     * @param base class to search path relative to
     * @param path relative path
     * @return found full path or null
     */
    public static String lookup(final Class base, final String path) {
        final String resourceBaseLocation = PathUtils.path(PathUtils.getPath(base),
                CharMatcher.is('/').trimLeadingFrom(path));
        return exists(resourceBaseLocation) ? resourceBaseLocation : null;
    }

    /**
     * Searches provided resource in multiple classpath locations.
     *
     * @param path      static resource path
     * @param rootPaths classpath folders to search resource in
     * @return resource location path (first occurrence) or null if not found
     */
    public static String lookup(final String path, final Iterable<String> rootPaths) {

        final String templatePath = CharMatcher.is('/').trimLeadingFrom(path);
        final Iterator<String> it = rootPaths.iterator();
        String location;
        String res = null;
        while (res == null && it.hasNext()) {
            location = it.next();
            if (exists(location + templatePath)) {
                res = location + templatePath;
            }
        }
        return res;
    }

    /**
     * Shortcut for {@link #lookup(String, Iterable)} with fail in case of not found template.
     *
     * @param path   static resource path
     * @param assets assets resolution object
     * @return resource location path (first occurrence)
     * @throws TemplateNotFoundException if template not found
     */
    public static String lookupOrFail(final String path, final AssetLookup assets)
            throws TemplateNotFoundException {
        final String lookup = assets.lookup(path);
        if (lookup == null) {
            final String err = String.format(
                    "Template %s not found in locations: %s", path, assets.getMatchingLocations(path));
            // logged here because exception most likely will be handled as 404 response
            LOGGER.info(err);
            throw new TemplateNotFoundException(err);
        }
        return lookup;
    }

    /**
     * Checks if absolute resource path exists in the classpath.
     *
     * @param path absolute path to check (assumed as absolute event if not starts with /)
     * @return true if resource exists, false otherwise
     */
    public static boolean exists(final String path) {
        final ClassLoader loader =
                MoreObjects.firstNonNull(
                        Thread.currentThread().getContextClassLoader(), ResourceLookup.class.getClassLoader());
        return loader.getResource(CharMatcher.is('/').trimLeadingFrom(path)) != null;
    }

    /**
     * Shortcut for {@link #exists(String)} with fail in case of not found template.
     *
     * @param path absolute path to check (assumed as absolute event if not starts with /)
     * @throws TemplateNotFoundException if template not found
     */
    public static void existsOrFail(final String path) throws TemplateNotFoundException {
        if (!exists(path)) {
            final String err = String.format("Template not found on path %s", path);
            // logged here because exception most likely will be handled as 404 response
            LOGGER.info(err);
            throw new TemplateNotFoundException(err);
        }
    }
}
