package ru.vyarus.guicey.spa

import com.google.common.net.HttpHeaders
import groovyx.net.http.HTTPBuilder
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
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
            bootstrap.addBundle(SpaBundle.app("app1", "/app", "/1").build())
            bootstrap.addBundle(SpaBundle.app("app2", "/app","/2").build())

            bootstrap.addBundle(SpaBundle.adminApp("aapp1", "/app","/a1").build())
            bootstrap.addBundle(SpaBundle.adminApp("aapp2", "/app", "/a2").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}