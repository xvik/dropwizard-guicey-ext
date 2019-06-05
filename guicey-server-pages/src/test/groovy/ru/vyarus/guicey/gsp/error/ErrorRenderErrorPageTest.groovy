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
 * @since 30.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class ErrorRenderErrorPageTest extends Specification {

    def "Check error mapping"() {

        when: "accessing not existing asset"
        new URL("http://localhost:8080/notexisting.html").text
        then: "error page failed to render"
        thrown(FileNotFoundException)

        when: "accessing not existing template"
        new URL("http://localhost:8080/notexisting.ftl").text
        then: "error page failed to render"
        thrown(FileNotFoundException)

        when: "accessing not existing path"
        new URL("http://localhost:8080/notexisting/").text
        then: "error page failed to render"
        thrown(FileNotFoundException)

        when: "error processing template"
        new URL("http://localhost:8080/sample/error").text
        then: "error page failed to render (500)"
        thrown(IOException)

        when: "error processing template"
        new URL("http://localhost:8080/sample/error2").text
        then: "error page failed to render (500)"
        thrown(IOException)

        when: "direct 404 rest response"
        new URL("http://localhost:8080/sample/notfound").text
        then: "error page failed to render"
        thrown(FileNotFoundException)
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .errorPage("/sample/error")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(SampleTemplateResource)
        }
    }
}
