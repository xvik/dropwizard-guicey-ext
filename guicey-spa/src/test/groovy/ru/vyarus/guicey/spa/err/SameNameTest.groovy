package ru.vyarus.guicey.spa.err

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.GuiceyConfigurationInfo
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.guicey.spa.SpaBundle
import spock.lang.Specification

import javax.inject.Inject

/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
 */
@UseGuiceyApp(App)
class SameNameTest extends Specification {

    @Inject
    GuiceyConfigurationInfo info

    def "Check duplicate spa names"() {

        expect: "duplicate registration avoided"
        info.getInfos(SpaBundle).size() == 1
        info.getInfos(SpaBundle)[0].registrationAttempts == 2
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            SpaBundle.app("app", "/app", "/1").build(),
                            SpaBundle.app("app", "/app", "/2").build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}