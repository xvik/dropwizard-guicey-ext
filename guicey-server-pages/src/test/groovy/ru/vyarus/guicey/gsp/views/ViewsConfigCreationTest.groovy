package ru.vyarus.guicey.gsp.views

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.ServerPagesBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 07.02.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class ViewsConfigCreationTest extends Specification {

    def "Check null views configuration binding"() {

        expect: "created main config map and sub map for freemarker"
        true
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(ServerPagesBundle.builder()
                    .viewsConfiguration({ null })
                    .viewsConfigurationModifier('freemarker', { assert it != null })
                    .printViewsConfiguration()
                    .build())
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/").build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
