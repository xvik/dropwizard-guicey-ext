package ru.vyarus.guicey.gsp.views.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.views.View;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import java.nio.charset.Charset;

/**
 * View template rendering model. Must be used as base class for models instead of pure {@link View}.
 * <p>
 * Template name may be specified directly (within constructor) or automatically detected from {@link Template}
 * resource annotation. If template path starts with "/" it's considered absolute and searched directly
 * within classpath, otherwise template is considered relative to one of configured classpath locations.
 * Note that {@link Template} annotation defines templates relative to annotated class.
 * <p>
 * Provides additional information about server pages application and current request through
 * {@link #getContextInfo()}. Error pages could access actual exception with {@link #getContextError()}.
 * <p>
 * It is also possible to reference any guice bean directly through model with {@link #getService(Class)}.
 * This may be useful in cases when custom model classes could not be used (error pages) or for quick prototyping.
 *
 * @author Vyacheslav Rusakov
 * @since 22.10.2018
 */
public class TemplateView extends View {

    private final TemplateContext contextInfo;
    private final WebApplicationException contextError;

    /**
     * Template obtained from {@link Template} annotation on resource.
     */
    public TemplateView() {
        this(null);
    }

    /**
     * If template name is null, it will be obtained from {@link Template} annotation on resource.
     *
     * @param templatePath template path or null (to use annotation value)
     */
    public TemplateView(@Nullable final String templatePath) {
        this(templatePath, null);
    }

    /**
     * If template name is null, it will be obtained from {@link Template} annotation on resource.
     *
     * @param templatePath template path or null (to use annotation value)
     * @param charset      charset or null
     */
    public TemplateView(@Nullable final String templatePath, @Nullable final Charset charset) {
        // template could be either absolute or relative
        super(TemplateContext.getInstance().lookupTemplatePath(templatePath), charset);
        this.contextInfo = TemplateContext.getInstance();
        this.contextError = ErrorRedirect.getContextError();
    }

    /**
     * Note that this object is the only way to get original request path because templates are always rendered
     * in rest endpoints after server redirect.
     *
     * @return additional info about current template.
     */
    @JsonIgnore
    public TemplateContext getContextInfo() {
        return contextInfo;
    }

    /**
     * Returns exception object only during rendering of configured error page
     * (from {@link ru.vyarus.guicey.gsp.ServerPagesBundle.Builder#errorPage(int, String)}).
     * For all other cases (from error pages) method is useless.
     *
     * @return exception object or null (for normal template rendering)
     */
    @JsonIgnore
    public WebApplicationException getContextError() {
        return contextError;
    }

    /**
     * Access for any guice bean directly from model. Useful for very special cases only when custom model could
     * not be used.
     *
     * @param service guice service type
     * @param <T>     service type
     * @return service instance
     */
    public <T> T getService(final Class<T> service) {
        return getContextInfo().getService(service);
    }
}
