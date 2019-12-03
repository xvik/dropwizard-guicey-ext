package ru.vyarus.guicey.gsp.ext

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import ru.vyarus.guicey.gsp.AbstractTest
import ru.vyarus.guicey.gsp.ServerPagesBundle
import ru.vyarus.guicey.gsp.support.app.OverridableTemplateResource
import ru.vyarus.guicey.gsp.support.app.SubTemplateResource

/**
 * @author Vyacheslav Rusakov
 * @since 03.12.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class ViewsExtensionTest extends AbstractTest {

    def "Check app mapped"() {

        when: "accessing path"
        String res = getHtml("/sample")
        then: "index page"
        res.contains("page: /sample")

        when: "accessing sub mapped path"
        res = getHtml("/sub/sample")
        then: "index page"
        res.contains("page: /sub/sample")
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .extensions(OverridableTemplateResource, SubTemplateResource)
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                                    .indexPage("index.html")
                                    .build(),
                            ServerPagesBundle.extendApp("app")
                                    .mapViews("/sub", "/sub")
                                    .build())
                    .build())

        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
