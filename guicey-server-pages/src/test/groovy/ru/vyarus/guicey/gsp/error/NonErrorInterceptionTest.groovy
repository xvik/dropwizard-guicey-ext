package ru.vyarus.guicey.gsp.error

import groovyx.net.http.HTTPBuilder
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException

/**
 * @author Vyacheslav Rusakov
 * @since 27.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class NonErrorInterceptionTest extends Specification {

    def "Check non error forwarding"() {

        def http = new HTTPBuilder('http://localhost:8080/')

        expect: "calling for non 200 response"
        http.get(path: '/res') {
            resp, reader ->
                assert resp.status == 304
                true
        }

        and: "direct rest non 200 return"
        http.get(path: '/res/2') {
            resp, reader ->
                assert resp.status == 304
                true
        }
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .errorPage("error.html")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(Resource)
        }
    }

    @Path("/app/res")
    static class Resource {

        @GET
        @Path("/")
        void get() {
            throw new WebApplicationException(304)
        }


        @GET
        @Path("/2")
        javax.ws.rs.core.Response get2() {
            return javax.ws.rs.core.Response.status(304).build()
        }
    }
}
