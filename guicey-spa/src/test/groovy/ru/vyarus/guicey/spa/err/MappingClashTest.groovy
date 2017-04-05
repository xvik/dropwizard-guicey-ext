package ru.vyarus.guicey.spa.err

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.guicey.spa.SpaBundle
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
 */
class MappingClashTest extends Specification {

    def "Check uri paths clash"() {

        when: "starting app"
        new App().run(['server'] as String[])
        then: "error"
        def ex = thrown(IllegalStateException)
        ex.message == "Assets servlet app2 registration clash with already installed servlets on paths: /app/*"
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(SpaBundle.app("app1", "/app", "/app").build())
            bootstrap.addBundle(SpaBundle.app("app2", "/app", "/app").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}