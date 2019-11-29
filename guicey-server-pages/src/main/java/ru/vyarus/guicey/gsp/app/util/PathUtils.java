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
    private static final Pattern PATH_DIRTY_SLASHES = Pattern.compile("\\s*/\\s*(/+\\s*)?");
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
        // when no path to merge still apply same normalization rules for more predictable result
        return path == null || path.isEmpty() ? path(basePath)
                // empty base path will bring unexpected '/' prefix (due to join)
                : (basePath == null || basePath.isEmpty() ? path(path) : path(basePath, path));
    }

    /**
     * If parts contain slashes (leading / trailing) they will be cleaned out, so the resulted
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
     * @return path with canonical slashes
     */
    public static String cleanUpPath(final String path) {
        final String fixedBackslashes = path.replace('\\', '/');
        return PATH_DIRTY_SLASHES.matcher(fixedBackslashes).replaceAll(SLASH).trim();
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
     * Method used to cleanup leading slash to convert path into relative if it starts from slash. This is important
     * for html pages with base tag declared: relative paths correctly resolved relative to application root.
     *
     * @param path path to make relative
     * @return relative path (without leading slash)
     */
    public static String toRelativePath(final String path) {
        return TRIM_SLASH.trimLeadingFrom(cleanUpPath(path));
    }

    /**
     * Returned location path does not contain leading slash because its not needed for direct classpath resource
     * loading.
     *
     * @param cls class
     * @return class location path
     */
    public static String getPath(final Class cls) {
        return cls.getPackage().getName().replace(".", PathUtils.SLASH);
    }

    /**
     * Normalization for sub section (sub folder) url path. Rules:
     * <ul>
     *     <li>Backslashes replaced with '/'</li>
     *     <li>Url must not starts with '/'</li>
     *     <li>Url must end with '/' (to prevent false sub-string matches when path used for matches)</li>
     * </ul>
     *
     * @param path path
     * @return normalized url
     */
    public static String normalizeRelativePath(final String path) {
        final String cleanPath = cleanUpPath(path);
        // do not apply end slash to empty path to not confuse with leading slash
        // NOTE '/' will become '' - intentional!
        return cleanPath.isEmpty() ? cleanPath : endSlash(trimSlashes(cleanPath));
    }

    /**
     * Normalization for classpath resource path. Rules:
     * <ul>
     *     <li>Path use '/' as separator</li>
     *     <li>Backslashes replaced with '/'</li>
     *     <li>Path must not start with '/'</li>
     *     <li>Path ends with '/'</li>
     * </ul>
     *
     * @param path classpath path (with '.' or '/')
     * @return normalized classpath path
     */
    public static String normalizeClasspathPath(final String path) {
        return normalizeRelativePath(path.replace('.', '/'));
    }
}
