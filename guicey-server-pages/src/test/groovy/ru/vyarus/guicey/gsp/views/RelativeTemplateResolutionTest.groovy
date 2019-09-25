package ru.vyarus.guicey.gsp.views

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import ru.vyarus.guicey.gsp.support.relative.RelativeTemplateResource
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 27.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class RelativeTemplateResolutionTest extends Specification {

    def "Check relative templates"() {

        when: "template from annotation"
        String res = new URL("http://localhost:8080/relative/direct").text
        then: "found"
        res.contains("name: app")

        when: "template relative to class"
        res = new URL("http://localhost:8080/relative/relative").text
        then: "found"
        res.contains("root name: app")

        when: "template relative to dir"
        res = new URL("http://localhost:8080/relative/dir").text
        then: "found"
        res.contains("page: /relative/dir")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(RelativeTemplateResource)
        }
    }
}
