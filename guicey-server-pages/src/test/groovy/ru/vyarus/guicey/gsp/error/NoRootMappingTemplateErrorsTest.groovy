package ru.vyarus.guicey.gsp.error

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import ru.vyarus.guicey.gsp.support.app.SampleTemplateResource
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 23.01.2019
 */
@UseDropwizardApp(value = App, config = 'src/test/resources/conf.yml',
        configOverride = [
                @ConfigOverride(key = "server.applicationContextPath", value = "/prefix/")
        ])
class NoRootMappingTemplateErrorsTest extends Specification {

    def "Check error mapping"() {

        when: "accessing not existing asset"
        def res = new URL("http://localhost:8080/prefix/notexisting.html").text
        then: "error page"
        res.contains("Error: AssetError")

        when: "accessing not existing template"
        res = new URL("http://localhost:8080/prefix/notexisting.ftl").text
        then: "error page"
        res.contains("Error: NotFoundException")

        when: "accessing not existing path"
        res = new URL("http://localhost:8080/prefix/notexisting/").text
        then: "error page"
        res.contains("Error: NotFoundException")

        when: "error processing template"
        res = new URL("http://localhost:8080/prefix/sample/error").text
        then: "error page"
        res.contains("Error: WebApplicationException")

        when: "error processing template"
        res = new URL("http://localhost:8080/prefix/sample/error2").text
        then: "error page"
        res.contains("Error: WebApplicationException")

        when: "direct 404 rest response"
        res = new URL("http://localhost:8080/prefix/sample/notfound").text
        then: "error page"
        res.contains("Error: TemplateRestCodeError")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .errorPage("error.ftl")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(SampleTemplateResource)
        }
    }
}
