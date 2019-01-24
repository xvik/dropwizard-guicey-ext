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
 * @since 24.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/*"),
        @ConfigOverride(key = "server.applicationContextPath", value = "/prefix/")
])
class RootRestMappingTest extends Specification {

    def "Check app mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/prefix/app/").text
        then: "index page"
        res.contains("page: /")

        when: "accessing direct file"
        res = new URL("http://localhost:8080/prefix/app/index.html").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing direct template"
        res = new URL("http://localhost:8080/prefix/app/template.ftl").text
        then: "rendered template"
        res.contains("page: /prefix/app/template.ftl")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .indexPage("template.ftl")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
