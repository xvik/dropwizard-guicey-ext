package ru.vyarus.guicey.gsp.spa

import com.google.common.net.HttpHeaders
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
import ru.vyarus.guicey.gsp.AbstractTest
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class SpaRoutingTest extends AbstractTest {

    def "Check spa mapped"() {

        when: "accessing app"
        String res = getHtml("/")
        then: "index page"
        res.contains("Sample page")

        when: "accessing not existing page"
        res = getHtml("/some/")
        then: "ok"
        res.contains("Sample page")

        when: "accessing not existing resource"
        getHtml("/some.html")
        then: "error"
        thrown(FileNotFoundException)
    }

    def "Check no cache header"() {

        def http = new HTTPBuilder('http://localhost:8080/', ContentType.HTML)

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
        http.get(path: '/css/style.css') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == null
            true
        }
    }

    def "Chck different mime type"() {

        def http = new HTTPBuilder('http://localhost:8080/')

        when: "calling with html type"
        http.get(path: '/same', contentType: ContentType.HTML)

        then: "redirect"
        true

        when: "calling with text type"
        http.get(path: '/same', contentType: ContentType.TEXT)

        then: "no redirect"
        def ex = thrown(HttpResponseException)
        ex.response.status == 404

        when: "calling with unknown content type"
        http.get(path: '/same', contentType: "abrakadabra")

        then: "no redirect"
        ex = thrown(HttpResponseException)
        ex.response.status == 404

        when: "calling with empty type"
        http.get(path: '/same', contentType: " ")

        then: "no redirect"
        ex = thrown(HttpResponseException)
        ex.response.status == 404


    }

    def "Check non 404 error"() {

        def http = new HTTPBuilder('http://localhost:8080/', ContentType.HTML)

        expect: "calling for cached content"
        http.get(path: '/index.html', headers: ['If-Modified-Since': 'Wed, 21 Oct 2215 07:28:00 GMT']) {
            resp, reader ->
                assert resp.status == 304
                true
        }

    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .indexPage("index.html")
                                    .spaRouting()
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

}
