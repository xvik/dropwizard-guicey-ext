package ru.vyarus.guicey.spa

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
 */
@UseDropwizardApp(App)
class AdminSpaMappingTest extends Specification {

    def "Check spa mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8081/app").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing not existing page"
        res = new URL("http://localhost:8081/app/some").text
        then: "error"
        res.contains("Sample page")

    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(SpaBundle.adminApp("app", "/app", "/app").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}