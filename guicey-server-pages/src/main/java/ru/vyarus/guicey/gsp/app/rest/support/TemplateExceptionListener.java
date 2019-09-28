package ru.vyarus.guicey.gsp.app.rest.support;

import com.google.common.base.Throwables;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.app.filter.redirect.TemplateRedirect;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

/**
 * Application listener for template processing exceptions detection. Listener use request listener for template
 * processing templates in order to detect exceptions.
 * <p>
 * This listener doesn't detect direct not OK status return (e.g. {@code Response.status(404).build()}), but
 * {@link TemplateErrorResponseFilter} will handle such cases.
 *
 * @author Vyacheslav Rusakov
 * @since 24.01.2019
 */
@Provider
@Singleton
public class TemplateExceptionListener implements ApplicationEventListener {
    // use single instance
    private final RequestListener listener = new RequestListener();

    @Override
    public void onEvent(final ApplicationEvent event) {
        // not needed
    }

    @Override
    public RequestEventListener onRequest(final RequestEvent requestEvent) {
        // apply request audit only under templates processing
        return TemplateRedirect.templateContext() != null ? listener : null;
    }

    /**
     * Jersey request listener used to detect exceptions.
     */
    private static class RequestListener implements RequestEventListener {
        private final Logger logger = LoggerFactory.getLogger(TemplateExceptionListener.class);

        @Override
        public void onEvent(final RequestEvent event) {
            // event may be called multiple times, but redirect will detect it and do nothing
            if (event.getType() == RequestEvent.Type.ON_EXCEPTION) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Rest processing exception detected for path {}:\n{}",
                            event.getUriInfo().getPath(),
                            Throwables.getRootCause(event.getException()).toString());
                }
                // immediately perform redirect to error page (or do nothing)
                TemplateContext.getInstance().redirectError(event.getException());
            }
        }
    }
}
