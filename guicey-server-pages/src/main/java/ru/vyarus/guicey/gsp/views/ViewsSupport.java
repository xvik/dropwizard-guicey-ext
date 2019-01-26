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
     * @param configuration dropwizard configuration object
     * @param environment   dropwizard environment object
     * @throws Exception on error
     */
    public static void setup(final GlobalConfig config,
                             final Configuration configuration,
                             final Environment environment) throws Exception {
        // view bundle must be initialized just once
        if (!config.isInitialized()) {

            installViewBundle(config, configuration, environment);
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
                                          final Configuration configuration,
                                          final Environment environment) throws Exception {
        // automatically add engines from classpath lookup
        final Iterable<ViewRenderer> renderers = ServiceLoader.load(ViewRenderer.class);
        renderers.forEach(globalConfig::addRenderers);
        Preconditions.checkState(!globalConfig.getRenderers().isEmpty(),
                "No template engines found (dropwizard views renderer)");

        // configure views bundle
        // bundle can't be registered in bootstrap as this point is in run phase
        new ConfiguredViewBundle(globalConfig).run(configuration, environment);
    }
}
