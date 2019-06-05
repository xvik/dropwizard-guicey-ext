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
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

/**
 * @author Vyacheslav Rusakov
 * @since 24.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class ErrorCodesMappingTest extends Specification {

    def "Check error mapping"() {

        when: "error processing template"
        def res = new URL("http://localhost:8080/code/403").text
        then: "error page"
        res.contains("Error code: 403")

        when: "error processing template"
        res = new URL("http://localhost:8080/code/405").text
        then: "error page"
        res.contains("Error code2: 405")

        when: "accessing not existing asset"
        new URL("http://localhost:8080/notexisting.html").text
        then: "no error mapped"
        thrown(FileNotFoundException)

        when: "accessing not existing template"
        new URL("http://localhost:8080/notexisting.ftl").text
        then: "no error mapped"
        thrown(FileNotFoundException)

        when: "error processing template"
        new URL("http://localhost:8080/code/407").text
        then: "no error mapped"
        thrown(IOException)
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("err", "/err", "/")
                    .errorPage(403, "error.ftl")
                    .errorPage(405, "error2.ftl")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(ErrorResource)
        }
    }

    @Path('/err')
    @Template
    public static class ErrorResource {

        @GET
        @Path("/code/{code}")
        public Response get(@PathParam("code") Integer code) {
            return Response.status(code).build()
        }
    }
}
