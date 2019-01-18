package ru.vyarus.guicey.gsp.app.rest;


import io.dropwizard.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateNotFoundException;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.ws.rs.*;

/**
 * Default template handling resources. Every server pages app register such resource to match all template requests
 * not matched by custom resources (direct templates rendering case). Support only GET and POST requests.
 *
 * @author Vyacheslav Rusakov
 * @since 22.10.2018
 */
// empty annotation to correctly show in console reporting and to participate in template-specific extensions
@Template
public class DirectTemplateResource {

    private final Logger logger = LoggerFactory.getLogger(DirectTemplateResource.class);

    @GET
    @Path("/{file:.*}")
    public View get(@PathParam("file") final String file) {
        return handle(file);
    }

    @POST
    @Path("/{file:.*}")
    public View post(@PathParam("file") final String file) {
        return handle(file);
    }

    private View handle(final String path) {
        logger.debug("Direct template rendering: {}", path);
        try {
            return new TemplateView(path);
        } catch (TemplateNotFoundException ex) {
            throw new NotFoundException(ex);
        }
    }

}
