package ru.vyarus.guicey.gsp.error

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import ru.vyarus.guicey.gsp.support.app.SampleTemplateResource
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 27.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class MimeTypeRecognitionTest extends Specification {

    def "Check error mapping"() {

        def http = new HTTPBuilder('http://localhost:8080/')

        when: "accessing not existing asset"
        def res = http.get(path: '/notexisting.html', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "accessing not existing asset with text result"
        http.get(path: '/notexisting.html', contentType: ContentType.TEXT)
        then: "no error page"
        def ex = thrown(HttpResponseException)
        ex.response.status == 404


        when: "accessing not existing template"
        res = http.get(path: '/notexisting.ftl', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "accessing not existing template with text result"
        http.get(path: '/notexisting.ftl', contentType: ContentType.TEXT)
        then: "no error page"
        ex = thrown(HttpResponseException)
        ex.response.status == 404


        when: "accessing not existing path"
        res = http.get(path: '/notexisting/', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "accessing not existing path with text result"
        http.get(path: '/notexisting/', contentType: ContentType.TEXT)
        then: "no error page"
        ex = thrown(HttpResponseException)
        ex.response.status == 404


        when: "error processing template"
        res = http.get(path: '/sample/error', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "error processing template with text result"
        http.get(path: '/sample/error', contentType: ContentType.TEXT)
        then: "no error page"
        ex = thrown(HttpResponseException)
        ex.response.status == 500


        when: "error processing template"
        res = http.get(path: '/sample/error2', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "error processing template with text result"
        http.get(path: '/sample/error2', contentType: ContentType.TEXT)
        then: "no error page"
        ex = thrown(HttpResponseException)
        ex.response.status == 500


        when: "direct 404 rest response"
        res = http.get(path: '/sample/notfound', contentType: ContentType.HTML)
        then: "error page"
        res == "custom error page"

        when: "direct 404 rest response with text result"
        http.get(path: '/sample/notfound', contentType: ContentType.TEXT)
        then: "error page"
        ex = thrown(HttpResponseException)
        ex.response.status == 404
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .extensions(SampleTemplateResource)
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .indexPage("index.html")
                                    .errorPage("error.html")
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

}