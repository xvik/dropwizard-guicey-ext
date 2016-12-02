package ru.vyarus.dropwizard.guicey

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.matcher.Matchers
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.dropwizard.guicey.support.*
import ru.vyarus.guicey.eventbus.EventBusBundle
import ru.vyarus.guicey.eventbus.service.EventSubscribersInfo
import spock.lang.Specification

import javax.inject.Inject

/**
 * @author Vyacheslav Rusakov
 * @since 02.12.2016
 */
@UseGuiceyApp(App.class)
class CustomMatcherTest extends Specification {
    @Inject
    EventBus bus
    @Inject
    Service service // trigger JIT binding
    @Inject
    ServiceNoEvents serviceNoEvents // trigger JIT binding
    @Inject
    EventSubscribersInfo info

    def "Check correct registration"() {

        expect: "listeners registered"
        info.getListenedEvents() == [Event1] as Set
        info.getListenerTypes(Event1) == [Service] as Set
        info.getListenerTypes(Event2).isEmpty()
        info.getListenerTypes(Event3).isEmpty()
        info.getListenerTypes(AbstractEvent).isEmpty()

    }

    def "Check publication"() {

        when: "publish event"
        bus.post(new Event1())
        then: "received"
        service.event1 == 1
        serviceNoEvents.event1 == 0
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(new EventBusBundle().withMatcher(Matchers.annotatedWith(HasEvents)))
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

    static class ServiceNoEvents {

        int event1

        @Subscribe
        void onEvent1(Event1 event) {
            event1++
        }
    }
}