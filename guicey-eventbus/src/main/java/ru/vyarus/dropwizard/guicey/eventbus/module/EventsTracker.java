package ru.vyarus.dropwizard.guicey.eventbus.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.NEWLINE;
import static ru.vyarus.dropwizard.guice.module.installer.util.Reporter.TAB;

/**
 * Tracks used events.
 *
 * @author Vyacheslav Rusakov
 * @since 12.10.2016
 */
public class EventsTracker {

    private final Logger logger = LoggerFactory.getLogger(EventsTracker.class);

    // event - listeners
    private final Multimap<Class, Class> subscribers = HashMultimap.create();

    public void track(final Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class event = method.getParameterTypes()[0];
                subscribers.put(event, type);
            }
        }
    }

    public void report() {
        final StringBuilder res = new StringBuilder("EventBus subscribers = ")
                .append(NEWLINE);
        for (Class event : subscribers.keys()) {
            res.append(NEWLINE).append(TAB).append(event.getSimpleName()).append(NEWLINE);
            for (Class subs : subscribers.get(event)) {
                res.append(TAB).append(TAB).append(subs.getName()).append(NEWLINE);
            }
        }
        logger.info(res.toString());
    }
}
