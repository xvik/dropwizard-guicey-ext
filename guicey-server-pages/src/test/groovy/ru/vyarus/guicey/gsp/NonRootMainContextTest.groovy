package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp

/**
 * @author Vyacheslav Rusakov
 * @since 21.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*"),
        @ConfigOverride(key = "server.applicationContextPath", value = "/prefix/")
])
class NonRootMainContextTest extends AbstractTest {

    def "Check app mapped"() {

        when: "accessing app"
        String res = getHtml("/prefix/")
        then: "index page"
        res.contains("Sample page")

        when: "accessing direct file"
        res = getHtml("/prefix/index.html")
        then: "index page"
        res.contains("Sample page")

        when: "accessing resource"
        res = get("/prefix/css/style.css")
        then: "css"
        res.contains("/* sample page css */")

        when: "accessing direct template"
        res = getHtml("/prefix/template.ftl")
        then: "rendered template"
        res.contains("page: /prefix/template.ftl")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .indexPage("index.html")
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
