package ru.vyarus.guicey.gsp.app;

import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import ru.vyarus.guicey.gsp.app.rest.log.ResourcePath;
import ru.vyarus.guicey.gsp.app.rest.log.RestPathsAnalyzer;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import java.util.Set;

/**
 * Delayed applications initializer. Delaying is required to be sure that all configurations were performed
 * (most importantly, all applications extension paths registered).
 *
 * @author Vyacheslav Rusakov
 * @since 18.01.2019
 */
public class DelayedInitializer implements ApplicationEventListener {

    private final GlobalConfig config;
    private final Environment environment;

    public DelayedInitializer(final GlobalConfig config, final Environment environment) {
        this.config = config;
        this.environment = environment;

        environment.jersey().register(this);
    }

    @Override
    public RequestEventListener onRequest(final RequestEvent requestEvent) {
        return null;
    }

    @Override
    public void onEvent(final ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            init();
        } else if (event.getType() == ApplicationEvent.Type.DESTROY_FINISHED) {
            // mark global config so it could be replaced with new one (for tests)
            config.shutdown();
        }
    }

    private void init() {
        // server.applicationContextPath
        final String contextPath = environment.getJerseyServletContainer()
                .getServletConfig().getServletContext().getContextPath();
        // server.rootPath
        final String restMapping = PathUtils.endSlash(PathUtils.trimStars(environment.jersey().getUrlPattern()));
        final RestPathsAnalyzer analyzer = RestPathsAnalyzer.build(environment.jersey().getResourceConfig());
        for (ServerPagesApp app : config.apps) {
            final Set<ResourcePath> paths = analyzer.select(app.name);
            app.initialize(contextPath, restMapping, paths);
        }
    }
}
