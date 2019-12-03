package ru.vyarus.guicey.gsp.app.rest.mapping;

import com.google.common.base.CharMatcher;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import java.util.Map;

/**
 * View rest endpoints are mapped with a prefix: so gsp application call /something could be remapped to
 * [rest]/[prefix]/something. Special prefixes could be mapped to some urls: e.g. /sub/url -> prefix2 and so
 * when /sub/url/something will be called in gsp application it would redirect to [rest]/[prefix2]/something.
 *
 * @author Vyacheslav Rusakov
 * @since 02.12.2019
 */
public class ViewRestLookup {

    private final Map<String, String> prefixes;

    public ViewRestLookup(final Map<String, String> prefixes) {
        // assume immutable map, properly built: keys sorted from longest to smaller (root locations last),
        this.prefixes = prefixes;
    }

    /**
     * @return main mapping prefix
     */
    public String getPrimaryMapping() {
        return prefixes.get("");
    }

    /**
     * @return configured view rest prefixes
     */
    public Map<String, String> getPrefixes() {
        // immutable
        return prefixes;
    }

    /**
     * Lookup target rest path. Will select rest prefix either by sub-url mapping (if url starts with registered
     * sub url) or using root (main) prefix.
     *
     * @param path gsp application called url (relative to application mapping root)
     * @return target path, relative to rest root, to call
     */
    public String lookup(final String path) {
        final String relativePath = CharMatcher.is('/').trimLeadingFrom(path);
        // value will always match to default root mapping if special url mapping not found
        String res = null;
        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            final String url = entry.getKey();
            if (relativePath.startsWith(url)) {
                // cut off custom app mapping and add correct rest mapping part
                res = PathUtils.path(entry.getValue(), path.substring(url.length()));
                break;
            }
        }
        return res;
    }
}
