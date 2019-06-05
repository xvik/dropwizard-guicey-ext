package ru.vyarus.guicey.gsp.error

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import ru.vyarus.guicey.gsp.views.template.Template
import spock.lang.Specification

import javax.ws.rs.GET
import javax.ws.rs.Path

/**
 * @author Vyacheslav Rusakov
 * @since 29.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class RestPathAsErrorPageTest extends Specification {

    def "Check index page as path"() {

        when: "accessing not existing asset"
        def res = new URL("http://localhost:8080/notexisting.html").text
        then: "error page"
        res.contains("error page from rest")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .errorPage("/error/")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(ErrorPage)
        }
    }

    @Template
    @Path("app/error")
    static class ErrorPage {

        @GET
        String get() {
            return "error page from rest"
        }

    }
}
