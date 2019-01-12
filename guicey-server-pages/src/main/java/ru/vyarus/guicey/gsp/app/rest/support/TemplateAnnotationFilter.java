package ru.vyarus.guicey.gsp.app.rest.support;

import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Request filter for {@link Template} annotated resources read configured template path (to be used in model).
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2018
 */
@Template
@Provider
public class TemplateAnnotationFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo info;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final Class<?> resourceClass = info.getResourceClass();
        final Template template = resourceClass.getAnnotation(Template.class);
        if (template != null) {
            final TemplateContext context = TemplateContext.getInstance();
            final String tpl = template.value();
            // when empty, it means direct template rendering
            if (!tpl.isEmpty()) {
                context.setTemplate(resourceClass, tpl);
            }
        }
    }
}
