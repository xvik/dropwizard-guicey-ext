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
 * @since 30.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class FilePatternChangeTest extends AbstractTest {

    def "Check changed file detection regex"() {

        when: "accessing css resource"
        String res = get("/css/style.css")
        then: "ok"
        res.contains("sample page css")

        when: "accessing template page"
        res = getHtml("/template.ftl")
        then: "template rendered"
        res == "page: /template.ftl"

        when: "accessing html page"
        getHtml("/index.html")
        then: "failed to render template"
        def ex = thrown(IOException)
        ex.message == "status code: 500, reason phrase: Internal Server Error"
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(
                            ServerPagesBundle.builder().build(),
                            ServerPagesBundle.app("app", "/app", "/")
                            // everything is a file, except direct .html files call
                                    .filePattern("(?:^|/)([^/]+\\.(?:(?!html)|(?:css)|(?:ftl)))(?:\\?.+)?\$")
                                    .build())
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
