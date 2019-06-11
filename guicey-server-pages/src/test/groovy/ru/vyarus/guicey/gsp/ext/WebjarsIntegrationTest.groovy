package ru.vyarus.guicey.gsp.ext

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 11.06.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class WebjarsIntegrationTest extends Specification {

    def "Check webjars binding"() {

        when: "accessing jquery script"
        def res = new URL("http://localhost:8080/jquery/3.4.1/dist/jquery.min.js").text
        then: "ok"
        res.contains("jQuery v3.4.1")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/").build())

            ServerPagesBundle.extendApp("app", "META-INF/resources/webjars/")
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
