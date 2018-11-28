package ru.vyarus.guicey.annotations.lifecycle

import com.google.inject.TypeLiteral
import com.google.inject.matcher.AbstractMatcher
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.junit.DropwizardAppRule
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.injector.lookup.InjectorLookup
import spock.lang.Specification

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Singleton

/**
 * @author Vyacheslav Rusakov
 * @since 27.11.2018
 */
class ConfigurationTest extends Specification {

    def "Check bundle configuration"() {

        setup:
        DropwizardAppRule rule = new DropwizardAppRule(App)

        when:
        rule.before()
        SampleBean bean = InjectorLookup.getInjector(rule.getApplication()).get().getInstance(SampleBean)
        SampleBean2 bean2 = InjectorLookup.getInjector(rule.getApplication()).get().getInstance(SampleBean2)
        rule.after()

        then:
        !bean.initialized
        !bean.started
        !bean.destroyed

        and:
        bean2.initialized
        bean2.started
        bean2.destroyed
    }

    static class App extends Application<Configuration> {

        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap
                    .addBundle(GuiceBundle.builder()
            // SampleBean will not be processed
                    .bundles(new LifecycleAnnotationsBundle(new AbstractMatcher<TypeLiteral<?>>() {

                @Override
                boolean matches(TypeLiteral<?> o) {
                    return o.getRawType() != SampleBean
                }
            }))
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    @Singleton
    static class SampleBean {
        boolean initialized
        boolean started
        boolean destroyed

        @PostConstruct
        private void start() {
            initialized = true
        }

        @PostStartup
        private void afterStartup() {
            started = true
        }

        @PreDestroy
        private void stop() {
            destroyed = true
        }
    }

    @Singleton
    static class SampleBean2 {
        boolean initialized
        boolean started
        boolean destroyed

        @PostConstruct
        private void start() {
            initialized = true
        }

        @PostStartup
        private void afterStartup() {
            started = true
        }

        @PreDestroy
        private void stop() {
            destroyed = true
        }
    }
}
