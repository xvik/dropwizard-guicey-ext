package ru.vyarus.dropwizard.guice.test.hook

import com.google.inject.Binder
import com.google.inject.Module
import io.dropwizard.core.Application
import io.dropwizard.core.Configuration
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook
import ru.vyarus.dropwizard.guice.module.GuiceyConfigurationInfo
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyHooks
import spock.lang.Specification

import javax.inject.Inject

/**
 * @author Vyacheslav Rusakov
 * @since 13.04.2018
 */
@UseGuiceyApp(value = App, hooks = Hook)
@UseGuiceyHooks(Hook2)
class UseHookExtAndAttribute extends Specification {

    @Inject
    GuiceyConfigurationInfo info

    def "Check hook attribute works"() {

        expect: "module registered"
        info.getModules().containsAll(XMod, XMod2)
    }

    static class App extends Application<Configuration> {
        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder().build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    static class Hook implements GuiceyConfigurationHook {
        @Override
        void configure(GuiceBundle.Builder builder) {
            builder.modules(new XMod())
        }
    }

    static class XMod implements Module {
        @Override
        void configure(Binder binder) {

        }
    }


    static class Hook2 implements GuiceyConfigurationHook {
        @Override
        void configure(GuiceBundle.Builder builder) {
            builder.modules(new XMod2())
        }
    }

    static class XMod2 implements Module {
        @Override
        void configure(Binder binder) {

        }
    }
}

