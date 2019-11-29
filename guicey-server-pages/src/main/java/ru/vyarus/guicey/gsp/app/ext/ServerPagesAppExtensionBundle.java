package ru.vyarus.guicey.gsp.app.ext;

import com.google.common.base.Preconditions;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment;
import ru.vyarus.guicey.gsp.ServerPagesBundle;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.asset.AssetSources;

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
    private final AssetSources sources = new AssetSources();
    private AssetsConfigurationCallback assetsConfigurationCallback;

    protected ServerPagesAppExtensionBundle(final String name) {
        this.name = name;
    }

    @Override
    public void run(final GuiceyEnvironment environment) throws Exception {
        final GlobalConfig config = environment.sharedStateOrFail(ServerPagesBundle.class,
                "Either server pages support bundle was not installed (use %s.builder() to create bundle) "
                        + " or it was installed after '%s' application extension bundle",
                ServerPagesBundle.class.getSimpleName(), name);
        if (assetsConfigurationCallback != null) {
            assetsConfigurationCallback.configure(environment, sources);
        }
        config.extendAssets(name, sources);
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
         * Add additional assets location. Useful for adding new resources or overriding application assets.
         * <p>
         * Use delayed configuration if dropwizard configuration is required
         * {@link #assetsConfigurator(AssetsConfigurationCallback)}.
         *
         * @param path assets classpath path
         * @return builder instance for chained calls
         * @see ServerPagesBundle.AppBuilder#attachAssets(String)
         */
        public AppExtensionBuilder attachAssets(final String path) {
            bundle.sources.attach(path);
            return this;
        }

        /**
         * Essentially the same as {@link #attachAssets(String)}, but attach classpath assets to application
         * sub url. As with root assets, multiple packages could be attached to url. Registration order is important:
         * in case if multiple packages contains the same file, file from the latest registered package will be used.
         * <p>
         * Use delayed configuration if dropwizard configuration is required
         * {@link #assetsConfigurator(AssetsConfigurationCallback)}.
         *
         * @param subUrl sub url to serve assets from
         * @param path   assets classpath paths
         * @return builder instance for chained calls
         * @see ServerPagesBundle.AppBuilder#attachAssetsForUrl(String, String)
         */
        public AppExtensionBuilder attachAssetsForUrl(final String subUrl, final String path) {
            bundle.sources.attach(subUrl, path);
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
        public AppExtensionBuilder assetsConfigurator(final AssetsConfigurationCallback callback) {
            Preconditions.checkArgument(bundle.assetsConfigurationCallback == null,
                    "Only one delayed assets configuration could be registered");
            bundle.assetsConfigurationCallback = callback;
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
