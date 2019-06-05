package ru.vyarus.guicey.gsp.app;

import com.google.common.base.Preconditions;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewRenderer;
import ru.vyarus.guicey.gsp.ServerPagesBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundle for server pages application installation (initialized with either
 * {@link ServerPagesBundle#app(String, String, String)} or
 * {@link ServerPagesBundle#adminApp(String, String, String)}).
 * <p>
 * Additional bundle is required because in some cases global {@link ServerPagesBundle} will be initialized
 * before application is registered (e.g. when application is registered from guicey bundle).
 *
 * @author Vyacheslav Rusakov
 * @since 05.06.2019
 */
public class ServerPagesAppBundle implements ConfiguredBundle<Configuration> {

    private static final String COMMA = ", ";
    
    private final GlobalConfig config;
    private final ServerPagesApp app;

    public ServerPagesAppBundle(final GlobalConfig config, final ServerPagesApp app) {
        this.config = config;
        this.app = app;
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        Preconditions.checkState(config.isViewsSupportRegistered(),
                "Server pages support bundle was not installed: use %s.builder() to create bundle",
                ServerPagesBundle.class.getSimpleName());
    }

    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        validateRequirements();
        app.setup(environment);
    }

    private void validateRequirements() {
        if (app.requiredRenderers == null) {
            return;
        }
        final List<String> available = new ArrayList<>();
        final List<String> required = new ArrayList<>(app.requiredRenderers);
        for (ViewRenderer renderer : config.getRenderers()) {
            final String key = renderer.getConfigurationKey();
            available.add(key);
            required.remove(key);
        }
        Preconditions.checkState(required.isEmpty(),
                "Required template engines are missed for server pages application '%s': %s "
                        + "(available engines: %s)",
                app.name, String.join(COMMA, required), String.join(COMMA, available));
    }
}
