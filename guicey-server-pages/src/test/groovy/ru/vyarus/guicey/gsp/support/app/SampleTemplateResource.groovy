package ru.vyarus.guicey.gsp.support.app

import ru.vyarus.guicey.gsp.views.template.Template
import ru.vyarus.guicey.gsp.views.template.TemplateView

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam

/**
 * @author Vyacheslav Rusakov
 * @since 14.01.2019
 */
@Path("/app/sample/")
@Template("/app/sample.ftl")
class SampleTemplateResource {

    @Path("/{name}")
    @GET
    SampleModel get(@PathParam("name") String name) {
        return new SampleModel(name: name);
    }

    public static class SampleModel extends TemplateView {
        String name
    }
}
