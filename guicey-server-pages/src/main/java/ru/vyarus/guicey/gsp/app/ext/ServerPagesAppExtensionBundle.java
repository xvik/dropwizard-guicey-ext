package ru.vyarus.guicey.gsp.app.ext;

import com.google.common.base.Preconditions;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.asset.AssetSources;
import ru.vyarus.guicey.gsp.app.rest.mapping.ViewRestSources;

/**
 * Bundle for extending (or overriding) registered server pages app resources (through
 * {@link ServerPagesBundle#extendApp(String)}).
 * <p>
 * Bundle is registered in initialization phase, but with delayed configuration callback, actual configuration
 * could be delayed to run phase.
 *
 * @author Vyacheslav Rusakov
 * @since 27.09.2019
 */
public class ServerPagesAppExtensionBundle implements GuiceyBundle {

    private final String name;
    private final AssetSources assets = new AssetSources();
    private DelayedConfigurationCallback delayedConfigCallback;
    private final ViewRestSources views = new ViewRestSources();

    protected ServerPagesAppExtensionBundle(final String name) {
        this.name = name;
    }

    @Override
    public void run(final GuiceyEnvironment environment) throws Exception {
        final GlobalConfig config = environment.sharedStateOrFail(ServerPagesBundle.class,
                "Either server pages support bundle was not installed (use %s.builder() to create bundle) "
                        + " or it was installed after '%s' application extension bundle",
                ServerPagesBundle.class.getSimpleName(), name);

        if (delayedConfigCallback != null) {
            delayedConfigCallback.configure(environment, assets, views);
        }
        config.extendAssets(name, assets);
    }

    /**
     * Extensions bundle builder.
     */
    public static class AppExtensionBuilder {

        private final ServerPagesAppExtensionBundle bundle;

        public AppExtensionBuilder(final String name) {
            this.bundle = new ServerPagesAppExtensionBundle(name);
        }

        /**
         * Map view rest to sub url. May be used to map additional rest endpoints with different prefix.
         * <p>
         * Only one mapping is allowed per url (otherwise error will be thrown)! But mappings for larger sub urls
         * are always allowed (partial override).
         * <p>
         * Normally, application configures root views mapping, but if not, then extension could register root
         * mapping using "/" as url. Direct shortcut not provided because such usage case considered as very rare,
         * <p>
         * Use delayed configuration if dropwizard configuration object is required
         * {@link #delayedConfiguration(DelayedConfigurationCallback)}.
         * <p>
         * Pay attention that additional asset locations may be required ({@link #attachAssets(String, String)},
         * because only templates relative to view class will be correctly resolved, but direct templates may fail
         * to resolve.
         *
         * @param subUrl sub url to map views to
         * @param prefix rest prefix to map as root views
         * @return builder instance for chained calls
         * @see ServerPagesBundle.AppBuilder#mapViews(String, String)
         */
        public AppExtensionBuilder mapViews(final String subUrl, final String prefix) {
            bundle.views.map(subUrl, prefix);
            return this;
        }

        /**
         * Add additional assets location. Useful for adding new resources or overriding application assets.
         * <p>
         * Use delayed configuration if dropwizard configuration object is required
         * {@link #delayedConfiguration(DelayedConfigurationCallback)}.
         *
         * @param path assets classpath path
         * @return builder instance for chained calls
         * @see ServerPagesBundle.AppBuilder#attachAssets(String)
         */
        public AppExtensionBuilder attachAssets(final String path) {
            bundle.assets.attach(path);
            return this;
        }

        /**
         * Essentially the same as {@link #attachAssets(String)}, but attach classpath assets to application
         * sub url. As with root assets, multiple packages could be attached to url. Registration order is important:
         * in case if multiple packages contains the same file, file from the latest registered package will be used.
         * <p>
         * Use delayed configuration if dropwizard configuration object is required
         * {@link #delayedConfiguration(DelayedConfigurationCallback)}.
         *
         * @param subUrl sub url to serve assets from
         * @param path   assets classpath paths
         * @return builder instance for chained calls
         * @see ServerPagesBundle.AppBuilder#attachAssets(String, String)
         */
        public AppExtensionBuilder attachAssets(final String subUrl, final String path) {
            bundle.assets.attach(subUrl, path);
            return this;
        }

        /**
         * Used to delay actual configuration till runtime phase, when dropwizard configuration will be available
         * (or, in case of complex setup, other bundles will perform all required initializations).
         * <p>
         * Only one callback may be registered.
         *
         * @param callback callback for extensions configuration under run phase
         * @return builder instance for chained calls
         */
        public AppExtensionBuilder delayedConfiguration(final DelayedConfigurationCallback callback) {
            Preconditions.checkArgument(bundle.delayedConfigCallback == null,
                    "Only one delayed configuration could be registered");
            bundle.delayedConfigCallback = callback;
            return this;
        }

        /**
         * @return bundle instance
         */
        public ServerPagesAppExtensionBundle build() {
            return bundle;
        }
    }
}
