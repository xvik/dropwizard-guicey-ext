package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class SpaRoutingTest extends Specification {

    def "Check spa mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing not existing page"
        res = new URL("http://localhost:8080/some/").text
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
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .indexPage("index.html")
                    .spaRouting()
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

}
