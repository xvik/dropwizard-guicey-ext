package ru.vyarus.dropwizard.guicey

import com.google.common.eventbus.Subscribe
import com.google.inject.AbstractModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.dropwizard.guicey.support.AbstractEvent
import ru.vyarus.dropwizard.guicey.support.Event1
import ru.vyarus.dropwizard.guicey.support.Event3
import ru.vyarus.guicey.eventbus.EventBusBundle
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 02.12.2016
 */
@UseGuiceyApp(App)
class ReportingLogTest extends Specification {

    def "Check correct registration"() {

        expect: "expecting log called"
        true

    }


    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(new EventBusBundle())
                    // need to force listeners registration to actually call reporting to console
                    .modules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(SubscribersInfoTest.Service).asEagerSingleton()
                }
            })
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    static class Service {

        @Subscribe
        void onEvent1(Event1 event) {
        }

        @Subscribe
        void onEvent3(Event3 event) {
        }

        @Subscribe
        void onEvent21(AbstractEvent event) {
        }
    }
}