package ru.vyarus.guicey.gsp.app;

import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle;
import ru.vyarus.guicey.gsp.ServerPagesBundle;

/**
 * Bundle for extending (or overriding) registered server pages app resources (through
 * {@link ServerPagesBundle#extendApp(String, String)}.
 * <p>
 * NOTE: global views support must be registered before this bundle!
 *
 * @author Vyacheslav Rusakov
 * @since 27.09.2019
 */
public class ServerPagesAppExtensionBundle implements GuiceyBundle {

    private final String name;
    private final String resourcePath;

    public ServerPagesAppExtensionBundle(final String name, final String resourcePath) {
        this.name = name;
        this.resourcePath = resourcePath;
    }

    @Override
    public void initialize(final GuiceyBootstrap bootstrap) {
        final GlobalConfig config = bootstrap.sharedStateOrFail(ServerPagesBundle.class,
                "Either server pages support bundle was not installed (use %s.builder() to create bundle) "
                        + " or it was installed after '%s' application extension bundle (%s)",
                ServerPagesBundle.class.getSimpleName(), name, resourcePath);
        config.extendLocation(name, resourcePath);
    }
}