package ru.vyarus.guicey.gsp.admin

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
 * @since 21.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*"),
        @ConfigOverride(key = "server.applicationContextPath", value = "/prefix"),
        @ConfigOverride(key = "server.adminContextPath", value = "/admin")
])
class ComplexFlatMappingTest extends Specification {

    def "Check app mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8081/admin/ap/").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing direct file"
        res = new URL("http://localhost:8081/admin/ap/index.html").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing resource"
        res = new URL("http://localhost:8081/admin/ap/css/style.css").text
        then: "css"
        res.contains("/* sample page css */")

        when: "accessing direct template"
        res = new URL("http://localhost:8081/admin/ap/template.ftl").text
        then: "rendered template"
        res.contains("page: /admin/ap/template.ftl")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.adminApp("app", "/app", "/ap")
                    .indexPage("index.html")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
