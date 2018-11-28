package ru.vyarus.guicey.annotations.lifecycle

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.injector.lookup.InjectorLookup
import ru.vyarus.dropwizard.guice.module.installer.feature.eager.EagerSingleton
import ru.vyarus.dropwizard.guice.test.GuiceyAppRule
import spock.lang.Specification

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * @author Vyacheslav Rusakov
 * @since 27.11.2018
 */
class MethodFailuresTest extends Specification {

    def "Check PostConstruct failure"() {

        setup:
        GuiceyAppRule rule = new GuiceyAppRule(FailedApp, null)

        when: 'start method throws exception'
        rule.before()
        rule.after() // unreachable point

        then: 'entire startup fails'
        def ex = thrown(IllegalStateException)
        ex.cause.message.startsWith('Failed to execute method StartFailure.start of instance ru.vyarus.guicey.annotations.lifecycle.MethodFailuresTest$StartFailure')
    }

    def "Check PostStartup failure"() {

        setup:
        DropwizardAppRule rule = new DropwizardAppRule(ServerFailedApp)

        when: 'start server method throws exception'
        rule.before()
        rule.after() // unreachable point

        then: 'entire startup fails'
        def ex = thrown(IllegalStateException)
        ex.message.startsWith('Failed to execute method StartupFailure.afterStartup of instance ru.vyarus.guicey.annotations.lifecycle.MethodFailuresTest$StartupFailure')
    }

    def "Check PreDestroy failure"() {

        setup:
        GuiceyAppRule rule = new GuiceyAppRule(DestroyFailedApp, null)

        when: 'destroy method throws exception'
        rule.before()
        DestroyFailure bean = InjectorLookup.getInjector(rule.getApplication()).get().getInstance(DestroyFailure)
        rule.after()

        then: 'exception suspended'
        bean.called
    }


    def "Check lazy PostConstruct failure"() {

        setup:
        GuiceyAppRule rule = new GuiceyAppRule(App, null)

        when: 'start method throws exception, but called after event processing'
        rule.before()
        StartFailure bean = InjectorLookup.getInjector(rule.getApplication()).get().getInstance(StartFailure)
        rule.after()

        then: 'method called, error suppressed'
        bean.called 
    }

    static class FailedApp extends App {
        FailedApp() {
            super(StartFailure)
        }
    }


    static class ServerFailedApp extends App {
        ServerFailedApp() {
            super(StartupFailure)
        }
    }

    static class DestroyFailedApp extends App {
        DestroyFailedApp() {
            super(DestroyFailure)
        }
    }

    static class App extends Application<Configuration> {

        Class[] exts

        App() {
            this(new Class[0])
        }

        App(Class... exts) {
            this.exts = exts
        }

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap
                    .addBundle(GuiceBundle.builder()
            // be sure bean initialized with the context and method would be processed when they appear
                    .extensions(exts)
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    @EagerSingleton
    static class StartFailure {
        boolean called

        @PostConstruct
        private void start() {
            called = true
            throw new IllegalStateException()
        }
    }

    @EagerSingleton
    static class StartupFailure {

        @PostStartup
        private void afterStartup() {
            throw new IllegalStateException()
        }
    }

    @EagerSingleton
    static class DestroyFailure {
        boolean called

        @PreDestroy
        private void stop() {
            called = true
            throw new IllegalStateException()
        }
    }
}
