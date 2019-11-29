package ru.vyarus.guicey.spa

import com.google.common.net.HttpHeaders
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp

/**
 * @author Vyacheslav Rusakov
 * @since 05.04.2017
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class MultipleBundlesMappingTest extends AbstractTest {

    def "Check spa mappings"() {

        when: "first"
        String res = get("/1")
        then: "index page"
        res.contains("Sample page")

        when: "second"
        res = get("/2")
        then: "index page"
        res.contains("Sample page")

        when: "admin first"
        res = adminGet("/a1")
        then: "index page"
        res.contains("Sample page")

        when: "admin second"
        res = adminGet("/a2")
        then: "index page"
        res.contains("Sample page")


        when: "accessing not existing page"
        res = get("/2/some/")
        then: "error"
        res.contains("Sample page")

        when: "accessing not existing admin page"
        res = adminGet("/a2/some/")
        then: "error"
        res.contains("Sample page")
    }

    def "Check cache header"() {
        def http = mainHttp

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
                            SpaBundle.app("app", "/app", "/1").build(),
                            SpaBundle.app("app2", "/app", "/2").build(),
                            SpaBundle.adminApp("aapp1", "/app", "/a1").build(),
                            SpaBundle.adminApp("aapp2", "/app", "/a2").build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}