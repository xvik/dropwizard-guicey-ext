package ru.vyarus.guicey.spa.err

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.junit.Rule
import ru.vyarus.dropwizard.guice.test.StartupErrorRule
import ru.vyarus.guicey.spa.SpaBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
 */
class SameNameTest extends Specification {

    @Rule
    StartupErrorRule rule = StartupErrorRule.create()

    def "Check duplicate spa names"() {

        when: "starting app"
        new App().run(['server'] as String[])
        then: "error"
        thrown(rule.indicatorExceptionType)
        rule.error.contains("SPA with name 'app' is already registered")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(SpaBundle.app("app", "/app", "/1").build())
            bootstrap.addBundle(SpaBundle.app("app", "/app", "/2").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}