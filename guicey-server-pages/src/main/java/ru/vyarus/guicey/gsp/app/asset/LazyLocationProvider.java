package ru.vyarus.guicey.gsp.app.asset;

import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides all classpath locations to search static resources in (registered during app registration and
 * all extensions ({@link ru.vyarus.guicey.gsp.ServerPagesBundle#extendApp(String, String)})).
 * Required because extensions initialization is not synchronized with main bundle and so extensions could
 * be registered after application bundle initialization.
 *
 * @author Vyacheslav Rusakov
 * @since 04.12.2018
 */
public class LazyLocationProvider implements Provider<List<String>> {

    private final String primaryLocation;
    private final String app;
    private final GlobalConfig config;
    private List<String> locations;

    public LazyLocationProvider(final String primaryLocation, final String app, final GlobalConfig config) {
        // converting in order to align with assets servlet conversion to be able to correctly parse
        // file path
        this.primaryLocation = convert(primaryLocation);
        this.app = app;
        this.config = config;
    }

    /**
     * @return all configured classpath locations for static assets
     */
    @Override
    public List<String> get() {
        if (locations != null) {
            return locations;
        }

        final List<String> locations = new ArrayList<>();
        locations.add(primaryLocation);
        locations.addAll(config.getExtensions(app));

        // reverse order to allow resource overriding (but note that multiple overrides will have unpredictable
        // results because extended apps registration order is hardly predictable)
        Collections.reverse(locations);

        // process paths the same way as assets servlet does
        this.locations = locations.stream().map(this::convert).collect(Collectors.toList());
        return this.locations;
    }

    /**
     * @return classpath location for static assets registered by application (main path without extensions)
     */
    public String getPrimaryLocation() {
        return primaryLocation;
    }

    private String convert(final String path) {
        // note: trailing slash is also removed!
        return PathUtils.endSlash(PathUtils.trimSlashes(path));
    }
}
