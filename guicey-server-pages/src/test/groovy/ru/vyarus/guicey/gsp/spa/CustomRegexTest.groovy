package ru.vyarus.guicey.gsp.spa

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
 * @since 18.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class CustomRegexTest extends Specification {

    def "Check custom regex"() {

        when: "accessing html"
        String res = new URL("http://localhost:8080/some/some.html").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing js"
        new URL("http://localhost:8080/some/some.js").text
        then: "index page"
        thrown(FileNotFoundException)
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle
                    .app("app", "/app", "/")
                    .indexPage("index.html")
                    .spaRouting("\\.js\$")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}