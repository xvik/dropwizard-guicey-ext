package ru.vyarus.guicey.gsp.views

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 26.01.2019
 */
@UseDropwizardApp(value = App, config = 'src/test/resources/views.yml')
class ViewsConfigurationTest extends Specification {

    def "Check views configuration binding"() {

        when: "accessing direct template"
        def res = new URL("http://localhost:8080/template.ftl").text
        then: "rendered template"
        res.contains("page: /template.ftl")
    }

    static class App extends Application<Config> {

        ServerPagesBundle bundle

        @Override
        void initialize(Bootstrap<Config> bootstrap) {
            bundle = ServerPagesBundle.builder()
                    .viewsConfiguration({ it.views })
                    // used to assert global config binding
                    .viewsConfigurationModifier('freemarker', { assert it['cache_storage'] != null })
                    .printViewsConfiguration()
                    .build()
            bootstrap.addBundle(bundle)
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/").build())
        }

        @Override
        void run(Config configuration, Environment environment) throws Exception {
            assert bundle.getRenderers().size() == 2
        }
    }

    static class Config extends Configuration {

        @JsonProperty
        Map<String, Map<String, String>> views;

    }
}
