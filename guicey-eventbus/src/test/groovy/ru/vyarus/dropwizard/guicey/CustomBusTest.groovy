package ru.vyarus.dropwizard.guicey

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.dropwizard.guicey.support.Event1
import ru.vyarus.dropwizard.guicey.support.Event2
import ru.vyarus.dropwizard.guicey.support.HasEvents
import ru.vyarus.guicey.eventbus.EventBusBundle
import ru.vyarus.guicey.eventbus.service.EventSubscribersInfo
import spock.lang.Specification

import javax.inject.Inject
import java.util.concurrent.Executors

/**
 * @author Vyacheslav Rusakov
 * @since 02.12.2016
 */
@UseGuiceyApp(App)
class CustomBusTest extends Specification {

    @Inject
    EventBus bus
    @Inject
    Service service // trigger JIT binding
    @Inject
    EventSubscribersInfo info

    def "Check correct registration"() {

        expect: "listeners registered"
        bus instanceof AsyncEventBus
        info.getListenedEvents() == [Event1] as Set
        info.getListenerTypes(Event1) == [Service] as Set
        info.getListenerTypes(Event2).isEmpty()

    }

    def "Check publication"() {

        when: "publish event"
        bus.post(new Event1())
        sleep(100)
        then: "received"
        service.event1 == 1
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(new EventBusBundle(new AsyncEventBus(Executors.newSingleThreadExecutor())))
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    @HasEvents
    static class Service {

        int event1

        @Subscribe
        void onEvent1(Event1 event) {
            event1++
        }
    }
}
