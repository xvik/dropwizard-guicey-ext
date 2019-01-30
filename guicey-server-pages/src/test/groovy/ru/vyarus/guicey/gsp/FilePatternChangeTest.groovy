package ru.vyarus.guicey.gsp

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.test.spock.ConfigOverride
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 30.01.2019
 */
@UseDropwizardApp(value = App, configOverride = [
        @ConfigOverride(key = "server.rootPath", value = "/rest/*")
])
class FilePatternChangeTest extends Specification {

    def "Check changed file detection regex"() {

        when: "accessing css resource"
        String res = new URL("http://localhost:8080/css/style.css").text
        then: "ok"
        res.contains("sample page css")
        
        when: "accessing template page"
        res = new URL("http://localhost:8080/template.ftl").text
        then: "template rendered"
        res == "page: /template.ftl"

        when: "accessing html page"
        new URL("http://localhost:8080/index.html").text
        then: "failed to render template"
        def ex = thrown(IOException)
        ex.message == "Server returned HTTP response code: 500 for URL: http://localhost:8080/index.html"
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            // pure dropwizard bundle
            bootstrap.addBundle(ServerPagesBundle.app("app", "/app", "/")
                    // everything is a file, except direct .html files call
                    .filePattern("(?:^|/)([^/]+\\.(?:(?!html)|(?:css)|(?:ftl)))(?:\\?.+)?\$")
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }
}
