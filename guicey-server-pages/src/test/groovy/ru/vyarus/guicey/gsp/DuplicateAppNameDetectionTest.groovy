package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 06.02.2019
 */
class DuplicateAppNameDetectionTest extends Specification {

    void cleanup() {
        ServerPagesBundle.resetGlobalConfig()
    }

    def "Check app collision detection"() {

        when: "starting app"
        new DropwizardAppRule<>(AppInit).before()
        then: "duplicate name error"
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Server pages application with name \'app\' is already registered'

        when: "starting app"
        new DropwizardAppRule<>(GAppInit).before()
        then: "duplicate name error"
        ex = thrown(IllegalStateException)
        ex.message == 'Duplicate server pages support initialization (ServerPagesBundle.builder())'
    }

    static class AppInit extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .indexPage("index.html").build())

            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .indexPage("index.html").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    static class GAppInit extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(new GuiceyBundle() {
                @Override
                void initialize(GuiceyBootstrap gb) {
                    ServerPagesBundle.app("app", "/app", "/app")
                            .indexPage("index.html").register(gb)

                    ServerPagesBundle.app("app", "/app", "/app")
                            .indexPage("index.html").register(gb)
                }
            })
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
