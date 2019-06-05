package ru.vyarus.guicey.gsp.app;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.dropwizard.Configuration;
import io.dropwizard.views.ViewConfigurable;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.views.ViewRendererConfigurationModifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Global configuration object shared by all server page bundles. Contains configuration for global views creation.
 * Object used internally by {@link ru.vyarus.guicey.gsp.ServerPagesBundle}.
 *
 * @author Vyacheslav Rusakov
 * @since 06.12.2018
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public class GlobalConfig {

    public List<ServerPagesApp> apps = new ArrayList<>();

    private final List<String> names = new ArrayList<>();
    private final List<ViewRenderer> renderers = new ArrayList<>();
    private final Multimap<String, ViewRendererConfigurationModifier> configModifiers = LinkedHashMultimap.create();
    // app name -- packages to search resources in
    private final Multimap<String, String> extensions = LinkedHashMultimap.create();
    private ViewConfigurable<Configuration> configurable;
    private boolean viewsSupportRegistered;
    private boolean printConfig;

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private boolean initialized;

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private boolean shutdown;

    public void globalBundleCreated() {
        checkState(!viewsSupportRegistered,
                "Duplicate server pages support initialization (%s.builder())",
                ServerPagesBundle.class.getSimpleName());
        viewsSupportRegistered = true;
    }

    /**
     * @return true if global bundle was created (assuming its registered), false otherwise
     */
    public boolean isViewsSupportRegistered() {
        return viewsSupportRegistered;
    }

    /**
     * Used to reveal registered application with the same name.
     *
     * @param name server pages application name
     */
    public void addAppName(final String name) {
        // important because name used for filter mapping
        checkArgument(!names.contains(name),
                "Server pages application with name '%s' is already registered", name);
        names.add(name);
    }

    /**
     * @return view renderers to use (for global views configuration)
     */
    public List<ViewRenderer> getRenderers() {
        return renderers;
    }

    /**
     * Specifies additional template engines support (main engines are resolved with lookup).
     * Duplicates are removed automatically.
     *
     * @param renderers additional view renderers
     */
    public void addRenderers(final ViewRenderer... renderers) {
        checkAlreadyInitialized();
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
        checkAlreadyInitialized();
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
     * Called after dropwizard views initialization to prevent duplicate initializations.
     */
    public void initialized() {
        this.initialized = true;
    }

    /**
     * @return true if application was shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Mark global config as belonging to shutdown application (used to re-create config).
     */
    public void shutdown() {
        this.shutdown = true;
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


    private void checkAlreadyInitialized() {
        checkState(!initialized, "Global initialization already performed");
    }
}
