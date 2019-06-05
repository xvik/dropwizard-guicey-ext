package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.support.app.SampleTemplateResource
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class ResourceMappingTest extends Specification {

    def "Chek custom resource mapping"() {

        when: "accessing template through resource"
        String res = new URL("http://localhost:8080/sample/tt").text
        then: "template mapped"
        res.contains("name: tt")

    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder().build())

            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    .indexPage("index.html")
                    .build())
            // register resource using guicey to check correct initialization
            bootstrap.addBundle(GuiceBundle.builder()
                    .extensions(SampleTemplateResource.class)
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
