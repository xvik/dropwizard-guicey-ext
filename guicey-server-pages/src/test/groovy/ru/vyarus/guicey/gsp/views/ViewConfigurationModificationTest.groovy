package ru.vyarus.guicey.gsp.views

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
 * @since 05.06.2019
 */
@UseDropwizardApp(value = App)
class ViewConfigurationModificationTest extends Specification {

    def "Check views configuration modification in app"() {

        expect: "application started without errors"
        true
    }

    static class App extends Application<Configuration> {

        ServerPagesBundle bundle

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bundle = ServerPagesBundle.builder()
                    .printViewsConfiguration()
                    .build()
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            bundle,
                            ServerPagesBundle.app("app", "/app", "/app")
                                    .viewsConfigurationModifier('freemarker', { it['cache_storage'] = "yes" })
                                    .viewsConfigurationModifier('test', {})
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
            assert bundle.getViewsConfig()['freemarker']['cache_storage'] == 'yes'
            assert bundle.getViewsConfig()['test'].isEmpty()
        }
    }
}
