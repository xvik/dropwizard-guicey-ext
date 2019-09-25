package ru.vyarus.guicey.gsp.spa

import com.google.common.net.HttpHeaders
import groovyx.net.http.HTTPBuilder
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 18.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class MultipleBundlesMappingTest extends Specification {

    def "Check spa mappings"() {

        when: "first"
        String res = new URL("http://localhost:8080/1").text
        then: "index page"
        res.contains("Sample page")

        when: "second"
        res = new URL("http://localhost:8080/2").text
        then: "index page"
        res.contains("Sample page")

        when: "admin first"
        res = new URL("http://localhost:8081/a1").text
        then: "index page"
        res.contains("Sample page")

        when: "admin second"
        res = new URL("http://localhost:8081/a2").text
        then: "index page"
        res.contains("Sample page")


        when: "accessing not existing page"
        res = new URL("http://localhost:8080/2/some/").text
        then: "error"
        res.contains("Sample page")

        when: "accessing not existing admin page"
        res = new URL("http://localhost:8081/a2/some/").text
        then: "error"
        res.contains("Sample page")
    }

    def "Check cache header"() {
        def http = new HTTPBuilder('http://localhost:8080/')

        expect: "calling index"
        http.get(path: '/1') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }
        http.get(path: '/2') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }

        and: "force redirect"
        http.get(path: '/1/some') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }
        http.get(path: '/2/some') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == 'must-revalidate,no-cache,no-store'
            true
        }


        and: "direct index page"
        http.get(path: '/1/index.html') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == null
            true
        }
        http.get(path: '/2/index.html') { resp, reader ->
            assert resp.headers.(HttpHeaders.CACHE_CONTROL) == null
            true
        }
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app1", "/app", "/1")
                                    .indexPage('index.html')
                                    .spaRouting()
                                    .build(),
                            ServerPagesBundle.app("app2", "/app", "/2")
                                    .indexPage('index.html')
                                    .spaRouting()
                                    .build(),
                            ServerPagesBundle.adminApp("aapp1", "/app", "/a1")
                                    .indexPage('index.html')
                                    .spaRouting()
                                    .build(),
                            ServerPagesBundle.adminApp("aapp2", "/app", "/a2")
                                    .indexPage('index.html')
                                    .spaRouting()
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}