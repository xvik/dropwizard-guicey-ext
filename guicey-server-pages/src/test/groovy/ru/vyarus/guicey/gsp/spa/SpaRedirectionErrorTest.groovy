package ru.vyarus.guicey.gsp.spa

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 06.02.2019
 */
@UseDropwizardApp(value = App, config = 'src/test/resources/conf.yml')
class SpaRedirectionErrorTest extends Specification {

    def "Check spa mapped"() {

        when: "accessing not existing page"
        def res = new URL("http://localhost:8080/some/").text
        then: "error page instead of index"
        res.contains("custom error page")

        when: "accessing not existing resource"
        res = new URL("http://localhost:8080/some.html").text
        then: "error page instead of index"
        res.contains("custom error page")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                            // bad index page
                                    .indexPage("/sample/error")
                                    .errorPage("error.html")
                                    .spaRouting()
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
