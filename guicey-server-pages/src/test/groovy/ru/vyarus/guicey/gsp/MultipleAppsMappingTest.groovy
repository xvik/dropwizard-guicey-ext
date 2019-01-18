package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.support.app.App2SampleResource
import ru.vyarus.guicey.gsp.support.app.SampleTemplateResource
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 18.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class MultipleAppsMappingTest extends Specification {

    def "Check app mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/app").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing direct file"
        res = new URL("http://localhost:8080/app/index.html").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing resource"
        res = new URL("http://localhost:8080/app/css/style.css").text
        then: "css"
        res.contains("/* sample page css */")

        when: "accessing direct template"
        res = new URL("http://localhost:8080/app/template.ftl").text
        then: "rendered template"
        res.contains("page: /app/template.ftl")

        when: "accessing template through resource"
        res = new URL("http://localhost:8080/app/sample/tt").text
        then: "template mapped"
        res.contains("name: tt")
    }

    def "Check app2 mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/app2").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing direct file"
        res = new URL("http://localhost:8080/app2/index.html").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing resource"
        res = new URL("http://localhost:8080/app2/css/style.css").text
        then: "css"
        res.contains("/* sample page css */")

        when: "accessing direct template"
        res = new URL("http://localhost:8080/app2/template.ftl").text
        then: "rendered template"
        res.contains("page: /app2/template.ftl")

        when: "accessing template through resource"
        res = new URL("http://localhost:8080/app2/sample/tt").text
        then: "template mapped"
        res.contains("name: tt")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/app")
                    .indexPage("index.html")
                    .build())

            bootstrap.addBundle(ServerPagesBundle.app("app2", "/app", "/app2")
                    .indexPage("index.html")
                    .build())

            // register resource using guicey to check correct initialization
            bootstrap.addBundle(GuiceBundle.builder()
                    .extensions(SampleTemplateResource, App2SampleResource)
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
