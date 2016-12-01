package ru.vyarus.guicey.eventbus.module;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.lang.reflect.Method;

/**
 * Module binds orchestrator {@link EventBus} instance. Publishers should inject event bus for posting.
 * Listeners must only define method with event as argument and annotated with {@link Subscribe}. All guice beans
 * with annotated methods registered automatically.
 *
 * @author Vyacheslav Rusakov
 * @since 12.10.2016
 */
public class EventBusModule extends AbstractModule {

    private final String name;

    public EventBusModule(final String name) {
        this.name = name;
    }

    @Override
    protected void configure() {
        final EventBus eventbus = new EventBus(name);
        bind(EventBus.class).toInstance(eventbus);

        final EventsTracker tracker = new EventsTracker();
        bind(EventsTracker.class).toInstance(tracker);
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
                // todo only first level counted
                for (Method method : type.getRawType().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Subscribe.class)) {
                        encounter.register(new InjectionListener<I>() {
                            @Override
                            public void afterInjection(final I injectee) {
                                eventbus.register(injectee);
                                // not method because there may be more than one listeners
                                tracker.track(type.getRawType());
                            }
                        });
                    }
                }
            }
        });
    }
}
