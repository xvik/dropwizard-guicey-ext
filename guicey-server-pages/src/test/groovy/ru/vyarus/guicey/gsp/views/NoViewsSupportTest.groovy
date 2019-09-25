package ru.vyarus.guicey.gsp.views

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.06.2019
 */
class NoViewsSupportTest extends Specification {

    void cleanup() {
        ServerPagesBundle.resetGlobalConfig()
    }

    def "Check view support absence detection"() {

        when: "starting app"
        new DropwizardAppRule<>(App).before()
        then: "no views support detected"
        def ex = thrown(IllegalStateException)
        ex.message == 'Server pages support bundle was not installed: use ServerPagesBundle.builder() to create bundle'

    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {


            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            // NO global setup
                            ServerPagesBundle.app("app", "/app", "/").build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
