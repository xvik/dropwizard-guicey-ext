package ru.vyarus.guicey.gsp.app.rest.support;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Alias handler for main template error handler ({@link TemplateErrorHandler}) used to intercept all exceptions
 * thrown during template rendering. Aliases are important because jersey always selects the most suitable mapper
 * which means that mapper with the closest exception type is selected. In order to override such mappers
 * behaviour, custom mapper with the same exception type must be registered.
 * <p>
 * Exception mapper can't be register with multiple types, because jersey detect mapped exception type ONLY
 * by mapper class.
 * <p>
 * Dropwizard itself use the same approach with {@link io.dropwizard.jersey.errors.LoggingExceptionMapper},
 * registered multiple times.
 * <p>
 * NOTE: registered alias will not change main rest behaviour! Only template resources will be affected.
 *
 * @param <T> exception type
 * @author Vyacheslav Rusakov
 * @see ru.vyarus.guicey.gsp.ServerPagesBundle.Builder#handleTemplateException(TemplateErrorHandlerAlias)
 * @since 16.01.2019
 */
@Provider
public abstract class TemplateErrorHandlerAlias<T extends Throwable> implements ExtendedExceptionMapper<T> {

    @Inject
    private TemplateErrorHandler handler;

    @Override
    public boolean isMappable(final T exception) {
        return handler.isMappable(exception);
    }

    @Override
    public Response toResponse(final T exception) {
        return handler.toResponse(exception);
    }
}
