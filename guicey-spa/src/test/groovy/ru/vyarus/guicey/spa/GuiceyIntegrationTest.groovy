package ru.vyarus.guicey.spa

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2017
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class GuiceyIntegrationTest extends Specification {

    def "Check spa mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing not existing page"
        new URL("http://localhost:8080/some/").text
        then: "ok"
        res.contains("Sample page")

        when: "accessing not existing resource"
        new URL("http://localhost:8080/some.html").text
        then: "error"
        thrown(FileNotFoundException)
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder().bundles(new AppBundle()).build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    static class AppBundle implements GuiceyBundle {
        @Override
        void initialize(GuiceyBootstrap bootstrap) {
            SpaBundle.app('app', '/app', '/').register(bootstrap)
        }
    }
}