package ru.vyarus.guicey.gsp.views;

import com.google.common.base.Preconditions;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.util.Reporter;
import ru.vyarus.guicey.gsp.app.GlobalConfig;

import java.util.ServiceLoader;

/**
 * Dropwizard views bundle initialization logic. Views must be initialized once for dropwizard application,
 * no matter how many server pages applications used.
 *
 * @author Vyacheslav Rusakov
 * @since 11.01.2019
 */
public final class ViewsSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewsSupport.class);

    private ViewsSupport() {
    }

    /**
     * Install dropwizard view bundle.
     *
     * @param config        global configuration (for all server page apps)
     * @param appName       application name (which initiate registration)
     * @param configuration dropwizard configuration object
     * @param environment   dropwizard environment object
     * @throws Exception on error
     */
    public static void setup(final GlobalConfig config,
                             final String appName,
                             final Configuration configuration,
                             final Environment environment) throws Exception {
        // view bundle must be initialized just once
        if (!config.isInitialized()) {

            installViewBundle(config, appName, configuration, environment);
            config.initialized();

            final StringBuilder res = new StringBuilder("Available dropwizard-views renderers:")
                    .append(Reporter.NEWLINE).append(Reporter.NEWLINE);
            for (ViewRenderer renderer : config.getRenderers()) {
                res.append(Reporter.TAB).append(String.format(
                        "%-15s (%s)", renderer.getConfigurationKey(), renderer.getClass().getName()))
                        .append(Reporter.NEWLINE);
            }
            LOGGER.info(res.toString());
        }
    }

    private static void installViewBundle(final GlobalConfig globalConfig,
                                          final String appName,
                                          final Configuration configuration,
                                          final Environment environment) throws Exception {
        if (globalConfig.getRenderers() == null) {
            final Iterable<ViewRenderer> renderers = ServiceLoader.load(ViewRenderer.class);
            Preconditions.checkState(renderers.iterator().hasNext(),
                    "No template engines found (dropwizard views renderer)");
            globalConfig.setRenderers(renderers, appName);
        }

        // configure views bundle
        // bundle can't be registered in bootstrap as this point is in run phase
        new ConfiguredViewBundle(globalConfig).run(configuration, environment);
    }
}
