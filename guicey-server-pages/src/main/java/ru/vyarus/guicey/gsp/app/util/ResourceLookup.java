package ru.vyarus.guicey.gsp.app.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;

import java.util.Iterator;
import java.util.List;

/**
 * Utility used to lookup static resources in multiple locations. This is used for applications extensions
 * mechanism when additional resources could be mapped into application from different classpath location.
 *
 * @author Vyacheslav Rusakov
 * @since 04.12.2018
 */
public final class ResourceLookup {

    private ResourceLookup() {
    }

    /**
     * Searches provided resource in multiple classpath locations.
     *
     * @param path      static resource path
     * @param rootPaths classpath folders to search resource in
     * @return resource location path (first occurrence) or null if not found
     */
    public static String lookup(final String path, final List<String> rootPaths) {
        final ClassLoader loader =
                MoreObjects.firstNonNull(
                        Thread.currentThread().getContextClassLoader(), ResourceLookup.class.getClassLoader());
        final String templatePath = CharMatcher.is('/').trimLeadingFrom(path);
        final Iterator<String> it = rootPaths.iterator();
        String location;
        String res = null;
        while (res == null && it.hasNext()) {
            location = it.next();
            if (loader.getResource(location + templatePath) != null) {
                res = location + templatePath;
            }
        }
        return res;
    }
}
