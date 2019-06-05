package ru.vyarus.guicey.gsp.views

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.06.2019
 */
class RendererDetectionTest extends Specification {

    void cleanup() {
        ServerPagesBundle.resetGlobalConfig()
    }

    def "Check renderer requirement check"() {

        when: "starting app"
        new DropwizardAppRule<>(App).before()
        then: "absent renderer detected"
        def ex = thrown(IllegalStateException)
        ex.message == 'Required template engines are missed for server pages application \'app\': fooo (available engines: freemarker, mustache)'
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .requireRenderers("fooo")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
