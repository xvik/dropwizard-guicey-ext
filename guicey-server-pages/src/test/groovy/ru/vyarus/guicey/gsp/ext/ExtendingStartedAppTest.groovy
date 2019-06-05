package ru.vyarus.guicey.gsp.ext

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.lifecycle.ServerLifecycleListener
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import org.eclipse.jetty.server.Server
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 06.02.2019
 */
class ExtendingStartedAppTest extends Specification {

    def "Check app not started"() {

        when: "starting app"
        new DropwizardAppRule<>(App).before()
        then: "late registration error"
        def ex = thrown(IllegalStateException)
        ex.message == 'Can\'t extend already initialized server pages application app'
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .indexPage("index.html")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {
                @Override
                void serverStarted(Server server) {
                    // too late for extensions
                    ServerPagesBundle.extendApp("app", "/ext")
                }
            })
        }
    }
}
