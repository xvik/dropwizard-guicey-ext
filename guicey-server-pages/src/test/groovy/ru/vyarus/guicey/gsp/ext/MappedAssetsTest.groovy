package ru.vyarus.guicey.gsp.ext

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
 * @since 29.11.2019
 */
@UseDropwizardApp(value = App, config = 'src/test/resources/conf.yml')
class MappedAssetsTest extends Specification {

    def "Check assets mapped"() {

        when: "accessing mapped url"
        String res = new URL("http://localhost:8080/sample/ext.ftl").text
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
                                    .attachAssetsForUrl('/sample', 'ext')
                                    .build())
                    .build())

        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
