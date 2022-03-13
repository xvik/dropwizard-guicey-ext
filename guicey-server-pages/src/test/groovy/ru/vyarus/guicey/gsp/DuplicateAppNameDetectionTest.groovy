package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.DropwizardTestSupport
import ru.vyarus.dropwizard.guice.GuiceBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 06.02.2019
 */
class DuplicateAppNameDetectionTest extends Specification {

    def "Check app collision detection"() {

        when: "starting app"
        // todo use guicey test support instead (after guidey release)
        // TestSupport.webApp(AppInit)
        new DropwizardTestSupport<>(AppInit, (String) null).before()
        then: "duplicate name error"
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Server pages application with name \'app\' is already registered'
    }

    static class AppInit extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/app")
                                    .indexPage("index.html")
                                    .build(),
                            ServerPagesBundle.app("app", "/app", "/app")
                                    .indexPage("index.html")
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
