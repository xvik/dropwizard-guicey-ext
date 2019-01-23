package ru.vyarus.guicey.gsp.app.rest;

import io.dropwizard.setup.Environment;
import ru.vyarus.guicey.gsp.app.GlobalConfig;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateExceptionMapper;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateExceptionMapperAlias;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateErrorResponseFilter;
import ru.vyarus.java.generics.resolver.GenericsResolver;

import javax.ws.rs.ext.ExceptionMapper;

/**
 * Install global exception handlers to intercept exceptions during template rendering.
 *
 * @author Vyacheslav Rusakov
 * @see TemplateExceptionMapper for description
 * @since 16.01.2019
 */
public final class RestErrorsSupport {

    private RestErrorsSupport() {
    }

    /**
     * Install exception handlers. On first (global) initialization install main exception handler and known
     * aliases. Next initializations could install additional aliases (registered in other bundles).
     *
     * @param config      global config
     * @param environment dropwizard environment
     */
    public static void setup(final GlobalConfig config, final Environment environment) {
        if (!config.isInitialized()) {
            globalInit(environment);
        }
        // due to registration order not all aliases could be visible in time of global registration
        installAliases(config, environment);
    }

    private static void globalInit(final Environment environment) {
        // one handler instance used
        environment.jersey().register(new TemplateExceptionMapper());

        // detect incorrect exception mapper used
        environment.jersey().register(TemplateErrorResponseFilter.class);
    }

    private static void installAliases(final GlobalConfig config, final Environment environment) {
        // aliases used used to override other registered exception mappers for template rest
        for (TemplateExceptionMapperAlias alias : config.mappedExceptions) {
            final Class ex = GenericsResolver.resolve(alias.getClass())
                    .type(ExceptionMapper.class).generic("E");
            if (!config.installedExceptions.contains(ex)) {
                environment.jersey().register(alias);
                config.installedExceptions.add(ex);
            }
        }

        // additional exceptions could eb registered after this point and will be processed separately
        config.mappedExceptions.clear();
    }
}
