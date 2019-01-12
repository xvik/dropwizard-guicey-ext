package ru.vyarus.guicey.gsp.app.rest.log;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import io.dropwizard.jersey.DropwizardResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import ru.vyarus.guicey.gsp.app.util.PathUtils;
import ru.vyarus.guicey.gsp.views.template.Template;

import java.util.Set;
import java.util.TreeSet;

/**
 * Collects template resources for logging. Implementation is a copy of dropwizard's own logging from
 * {@link DropwizardResourceConfig} (which can't be used directly).
 *
 * @author Vyacheslav Rusakov
 * @since 06.12.2018
 */
public final class ResourcePathsAnalyzer {

    private static final TypeResolver TYPE_RESOLVER = new TypeResolver();

    private ResourcePathsAnalyzer() {
    }

    /**
     * Collects all registered template resource paths for console logging.
     *
     * @param app         server pages application name
     * @param appMapping  application mapping
     * @param restMapping rest mapping
     * @param config      dropwizard resources configuration object
     * @return all template resource methods
     */
    public static Set<ResourcePath> analyze(
            final String app,
            final String appMapping,
            final String restMapping,
            final DropwizardResourceConfig config) {
        final Set<ResourcePath> handles = new TreeSet<>();
        // resource name must start with application name in order to differentiate applications
        final String appResourcePrefix = PathUtils.prefixSlash(PathUtils.endSlash(app));
        for (Class<?> cls : config.getClasses()) {
            if (!cls.isAnnotationPresent(Template.class)) {
                continue;
            }
            final Resource resource = Resource.from(cls);
            // other template resources will be processed by other applications or not used at all
            if (resource != null && resource.getPath().startsWith(appResourcePrefix)) {
                populate(app, appMapping, restMapping, cls, false, resource, handles);
            }
        }
        // manually added resources
        for (Resource resource : config.getResources()) {
            for (Resource childRes : resource.getChildResources()) {
                for (Class<?> childResHandlerClass : childRes.getHandlerClasses()) {
                    if (childResHandlerClass.isAnnotationPresent(Template.class)) {
                        populate(app, appMapping, PathUtils.cleanUpPath(restMapping + resource.getPath()),
                                childResHandlerClass, false, childRes, handles);
                    }
                }
            }
        }
        return handles;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static void populate(final String app,
                                 final String mapping,
                                 final String rootPath,
                                 final Class<?> klass,
                                 final boolean isLocator,
                                 final Resource resource,
                                 final Set<ResourcePath> handles) {
        String basePath = rootPath;
        if (!isLocator) {
            basePath = PathUtils.normalizePath(rootPath, resource.getPath());
        }

        for (ResourceMethod method : resource.getResourceMethods()) {
            // map direct resource methods
            handles.add(new ResourcePath(method, resource, klass, basePath, app, mapping));
        }

        for (Resource childResource : resource.getChildResources()) {
            for (ResourceMethod method : childResource.getAllMethods()) {
                if (method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
                    final String path = PathUtils.normalizePath(basePath, childResource.getPath());
                    handles.add(new ResourcePath(method, childResource, klass, path, app, mapping));
                } else if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR) {
                    final String path = PathUtils.normalizePath(basePath, childResource.getPath());
                    final ResolvedType responseType = TYPE_RESOLVER
                            .resolve(method.getInvocable().getResponseType());
                    final Class<?> erasedType = !responseType.getTypeBindings().isEmpty()
                            ? responseType.getTypeBindings().getBoundType(0).getErasedType()
                            : responseType.getErasedType();
                    final Resource erasedTypeResource = Resource.from(erasedType);
                    if (erasedTypeResource == null) {
                        handles.add(new ResourcePath(method, childResource, erasedType, path, app, mapping));
                    } else {
                        populate(app, mapping, path, erasedType, true, erasedTypeResource, handles);
                    }
                }
            }
        }
    }

}
