package ru.vyarus.guicey.eventbus;

import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap;
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle;
import ru.vyarus.guicey.eventbus.module.EventBusModule;
import ru.vyarus.guicey.eventbus.report.EventsReporter;

/**
 * @author Vyacheslav Rusakov
 * @since 12.10.2016
 */
public class EventBusBundle implements GuiceyBundle {

    private final String name;

    public EventBusBundle() {
        this("main");
    }

    public EventBusBundle(final String name) {
        this.name = name;
    }

    @Override
    public void initialize(final GuiceyBootstrap bootstrap) {
        bootstrap
                .modules(new EventBusModule(name))
                .extensions(EventsReporter.class);
    }
}
