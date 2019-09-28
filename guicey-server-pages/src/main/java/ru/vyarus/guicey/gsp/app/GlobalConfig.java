package ru.vyarus.guicey.gsp.app;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.dropwizard.Configuration;
import io.dropwizard.views.ViewConfigurable;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.guicey.gsp.views.ViewRendererConfigurationModifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Global configuration object shared by all server page bundles. Contains configuration for global views creation.
 * Object used internally by {@link ru.vyarus.guicey.gsp.ServerPagesBundle}.
 *
 * @author Vyacheslav Rusakov
 * @since 06.12.2018
 */
public class GlobalConfig {

    private final List<String> names = new ArrayList<>();
    private final List<ServerPagesApp> apps = new ArrayList<>();
    private final List<ViewRenderer> renderers = new ArrayList<>();
    private final Multimap<String, ViewRendererConfigurationModifier> configModifiers = LinkedHashMultimap.create();
    // app name -- packages to search resources in
    private final Multimap<String, String> extensions = LinkedHashMultimap.create();
    private ViewConfigurable<Configuration> configurable;
    private boolean printConfig;
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private Map<String, Map<String, String>> viewsConfig;

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private boolean locked;

    /**
     * Register application globally.
     *
     * @param app server pages application
     */
    public void register(final ServerPagesApp app) {
        // important because name used for filter mapping
        checkArgument(!names.contains(app.name),
                "Server pages application with name '%s' is already registered", app.name);
        names.add(app.name);
        this.apps.add(app);

        // register configuration modifiers
        for (Map.Entry<String, ViewRendererConfigurationModifier> entry : app.viewsConfigModifiers.entrySet()) {
            addConfigModifier(entry.getKey(), entry.getValue());
        }

        // register extended locations
        for (String location : app.extendedResourceLocations) {
            extendLocation(app.name, location);
        }
    }

    /**
     * @return list of created server page applications
     */
    public List<ServerPagesApp> getApps() {
        return apps;
    }

    /**
     * @return view renderers to use (for global views configuration)
     */
    public List<ViewRenderer> getRenderers() {
        return renderers;
    }

    /**
     * @return final views configuration or null if not yet initialized
     */
    public Map<String, Map<String, String>> getViewsConfig() {
        return viewsConfig;
    }

    /**
     * @param viewsConfig final views config
     * @return provided view config
     */
    public Map<String, Map<String, String>> viewsConfig(final Map<String, Map<String, String>> viewsConfig) {
        this.viewsConfig = viewsConfig;
        return viewsConfig;
    }

    /**
     * Specifies additional template engines support (main engines are resolved with lookup).
     * Duplicates are removed automatically.
     *
     * @param renderers additional view renderers
     */
    public void addRenderers(final ViewRenderer... renderers) {
        checkLocked();
        for (ViewRenderer renderer : renderers) {
            final String key = renderer.getConfigurationKey();
            // prevent duplicates
            boolean add = true;
            for (ViewRenderer ren : this.renderers) {
                if (ren.getConfigurationKey().equals(key)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                this.renderers.add(renderer);
            }
        }
    }

    /**
     * @return dropwizard views configuration binding to use (for global views configuration)
     */
    public ViewConfigurable<Configuration> getConfigurable() {
        return configurable;
    }

    /**
     * Specifies global views configuration binding (usually from application configuration object).
     * Could be configured only by one bundle in order to simplify configuration.
     *
     * @param configurable dropwizard views configuration binding
     * @param <T>          configuration type
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> void setConfigurable(final ViewConfigurable<T> configurable) {
        this.configurable = (ViewConfigurable<Configuration>) configurable;
    }

    /**
     * @param name     view renderer name to apply to
     * @param modifier modifier for exact renderer config
     */
    public void addConfigModifier(final String name, final ViewRendererConfigurationModifier modifier) {
        // it is possible to throw this check if server pages application is registered inside GuiceyBundle
        // and ServerPagesBundle is installed before GuiceBundle. In this case simply move ServerPagesBundle
        // registration after GuiceBundle
        checkLocked();
        configModifiers.put(name, modifier);
    }

    /**
     * @return modifiers for global views configuration
     */
    public Multimap<String, ViewRendererConfigurationModifier> getConfigModifiers() {
        return configModifiers;
    }

    /**
     * @return true to log global views config (to see how it was modified), false to not log
     */
    public boolean isPrintConfiguration() {
        return printConfig;
    }

    /**
     * Enable global views configuration logging to console.
     */
    public void printConfiguration() {
        this.printConfig = true;
    }

    /**
     * Called after dropwizard views initialization to prevent configuration after initialization.
     */
    public void lock() {
        this.locked = true;
    }

    /**
     * Register application resources extension.
     *
     * @param app      application name to apply new resources to
     * @param location classpath location
     */
    public void extendLocation(final String app, final String location) {
        // if application itself is already registered check its not initialized (extension could be applied)
        for (ServerPagesApp spa : apps) {
            if (spa.name.equals(app)) {
                checkState(!spa.isStarted(),
                        "Can't extend already initialized server pages application %s", app);
                break;
            }
        }
        extensions.put(app, location);
    }

    /**
     * @param app application name
     * @return all configured extended locations
     */
    public Collection<String> getExtensions(final String app) {
        return extensions.get(app);
    }


    private void checkLocked() {
        checkState(!locked, "Global initialization already performed");
    }
}
