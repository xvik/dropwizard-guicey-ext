package ru.vyarus.guicey.gsp.ext

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 06.02.2019
 */
@UseDropwizardApp(value = App, config = 'src/test/resources/conf.yml')
class ExtendedFromGuiceyTest extends Specification {

    def "Check app mapped"() {

        when: "accessing app"
        String res = new URL("http://localhost:8080/").text
        then: "index page"
        res.contains("overridden sample page")

        when: "accessing direct file"
        res = new URL("http://localhost:8080/index.html").text
        then: "index page"
        res.contains("overridden sample page")

        when: "accessing resource"
        res = new URL("http://localhost:8080/css/style.css").text
        then: "css"
        res.contains("/* sample page css */")

        when: "accessing direct template"
        res = new URL("http://localhost:8080/template.ftl").text
        then: "rendered template"
        res.contains("page: /template.ftl")

        when: "accessing direct ext template"
        res = new URL("http://localhost:8080/ext.ftl").text
        then: "rendered template"
        res.contains("ext template")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .indexPage("index.html")
                                    .build(),
                            new GuiceyBundle() {
                                @Override
                                void initialize(GuiceyBootstrap gb) {
                                    gb.bundles(ServerPagesBundle.extendApp("app", "/ext"))
                                }
                            })
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
