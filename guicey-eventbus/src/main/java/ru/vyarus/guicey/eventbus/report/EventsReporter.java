package ru.vyarus.guicey.eventbus.report;

import io.dropwizard.lifecycle.Managed;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;
import ru.vyarus.guicey.eventbus.module.EventsTracker;

import javax.inject.Inject;

/**
 * Prints registered event listeners.
 *
 * @author Vyacheslav Rusakov
 * @since 12.10.2016
 */
@Order(200)
public class EventsReporter implements Managed {

    private final EventsTracker tracker;

    @Inject
    public EventsReporter(final EventsTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void start() throws Exception {
        tracker.report();
    }

    @Override
    public void stop() throws Exception {

    }
}
