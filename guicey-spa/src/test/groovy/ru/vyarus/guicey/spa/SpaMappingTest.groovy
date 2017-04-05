package ru.vyarus.guicey.spa

import com.google.common.net.HttpHeaders
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 02.04.2017
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class SpaMappingTest extends Specification {

    def "Check spa mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/").text
        then: "index page"
        res.contains("Sample page")

        when: "accessing not existing page"
        new URL("http://localhost:8080/some/").text
        then: "ok"
        res.contains("Sample page")

        when: "accessing not existing resource"
        new URL("http://localhost:8080/some.html").text
        then: "error"
        thrown(FileNotFoundException)
    }

    def "Check no cache header"() {

        def http = new HTTPBuilder('http://localhost:8080/')

        expect: "calling index"
        http.get(path: '/') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }

        and: "force redirect"
        http.get(path: '/some') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }

        and: "direct index page"
        http.get(path: '/index.html') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == null
            true
        }

        and: "resource"
        http.get(path: '/css/some.css') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == null
            true
        }
    }

    def "Chck different mime type"() {

        def http = new HTTPBuilder('http://localhost:8080/')

        when: "calling with html type"
        http.get(path: '/same', contentType : ContentType.HTML)

        then: "redirect"
        true

        when: "calling with text type"
        http.get(path: '/same', contentType : ContentType.TEXT)

        then: "no redirect"
        def ex = thrown(HttpResponseException)
        ex.response.status == 404

    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(SpaBundle.app("app", "/app", "/").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}