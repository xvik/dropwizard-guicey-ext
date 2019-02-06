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
 * @since 26.01.2019
 */
class ViewsNonUniqueConfigurationTest extends Specification {

    void cleanup() {
        ServerPagesBundle.resetGlobalConfig()
    }

    def "Check duplicate views configuration detection"() {

        when: "start app"
        new DropwizardAppRule<>(App).before()
        then: "duplicate views config error"
        def ex = thrown(IllegalStateException)
        ex.message == 'Global views configuration must be performed by one bundle and \'app1\' already configured it.'

        cleanup:
        ServerPagesBundle.GLOBAL_CONFIG.remove()
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app1", "/app", "/app1")
                    .viewsConfiguration({ [:] })
                    .build())

            // duplicate views config
            bootstrap.addBundle(ServerPagesBundle.app("app2", "/app", "/app2")
                    .viewsConfiguration({ [:] })
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

}
