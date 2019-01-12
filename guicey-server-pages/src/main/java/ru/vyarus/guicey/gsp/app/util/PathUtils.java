package ru.vyarus.guicey.gsp.app.util;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Url path utils.
 *
 * @author Vyacheslav Rusakov
 * @since 06.12.2018
 */
public final class PathUtils {

    public static final String SLASH = "/";
    private static final Pattern PATH_DIRTY_SLASHES = Pattern.compile("\\s*/\\s*/+\\s*");
    private static final CharMatcher TRIM_SLASH = CharMatcher.is('/');
    private static final CharMatcher TRIM_STAR = CharMatcher.is('*');

    private PathUtils() {
    }

    /**
     * Combine path with base. If path is null simply returns prefix.
     *
     * @param basePath base path to match with relative part
     * @param path     relative path (may be null)
     * @return path combined with base path
     */
    public static String normalizePath(final String basePath, final String path) {
        if (path == null || path.isEmpty()) {
            return basePath;
        }
        return path(basePath, path);
    }

    /**
     * If parts may contain slashes (leading / trailing) they will be cleaned out, so the resulted
     * path will not contain double slashes.
     *
     * @param parts path parts
     * @return combined path from supplied parts
     */
    public static String path(final String... parts) {
        return cleanUpPath(StringUtils.join(parts, SLASH));
    }

    /**
     * Cleanup duplicate slashes and replace backward slashes.
     *
     * @param path path to cleanup
     * @return path with canonical slashed
     */
    public static String cleanUpPath(final String path) {
        return PATH_DIRTY_SLASHES.matcher(path).replaceAll(SLASH).trim();
    }

    /**
     * @param path path
     * @return path started with slash (original path if it already starts with slash)
     */
    public static String prefixSlash(final String path) {
        return path.startsWith(SLASH) ? path : SLASH + path;
    }

    /**
     * Exception: slash is not applied to empty string because in this case it would become leading slash too
     * (may not be desired behaviour).
     *
     * @param path path
     * @return path ended with slash (original path if it already ends with slash)
     */
    public static String endSlash(final String path) {
        if (path.isEmpty()) {
            return path;
        }
        return path.endsWith(SLASH) ? path : path + SLASH;
    }

    /**
     * Method used to cleanup wildcard paths like "/*" into "/".
     *
     * @param path path
     * @return path without leading / trailing stars
     */
    public static String trimStars(final String path) {
        return TRIM_STAR.trimFrom(path);
    }

    /**
     * Method used to cleanup leading or trailing slashes.
     *
     * @param path path
     * @return path without leading / trailing slashes
     */
    public static String trimSlashes(final String path) {
        return TRIM_SLASH.trimFrom(path);
    }

    /**
     * @param cls class
     * @return class location path
     */
    public static String getPath(final Class cls) {
        return cls.getPackage().getName().replace(".", PathUtils.SLASH);
    }
}
