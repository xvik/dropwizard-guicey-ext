package ru.vyarus.guicey.gsp.app.rest.log;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import ru.vyarus.guicey.gsp.app.util.PathUtils;

import java.util.Comparator;

import static ru.vyarus.guicey.gsp.app.util.PathUtils.SLASH;

/**
 * Represents template rest method.
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2018
 */
public class ResourcePath implements Comparable<ResourcePath> {

    private final ResourceMethod method;
    private final Resource resource;
    private final Class klass;
    private final String restUrl;
    private final String pageUrl;

    public ResourcePath(final ResourceMethod method,
                        final Resource resource,
                        final Class klass,
                        final String restUrl,
                        final String app,
                        final String mapping) {
        this.method = method;
        this.resource = resource;
        this.klass = klass;
        this.restUrl = restUrl;
        this.pageUrl = buildPageUrl(restUrl, app, mapping);
    }

    /**
     * @return resource method
     */
    public ResourceMethod getMethod() {
        return method;
    }

    /**
     * @return resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @return resource type
     */
    public Class getKlass() {
        return klass;
    }

    /**
     * @return rest mapping url
     */
    public String getRestUrl() {
        return restUrl;
    }

    /**
     * @return page url (mapped to rest url)
     */
    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public String toString() {
        return pageUrl + " page (" + restUrl + ")";
    }

    @Override
    public int compareTo(final ResourcePath o) {
        return ComparisonChain.start()
                .compare(pageUrl, o.pageUrl)
                .compare(method.getHttpMethod(), o.method.getHttpMethod(), Comparator.nullsLast(Ordering.natural()))
                .result();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourcePath)) {
            return false;
        }
        final ResourcePath that = (ResourcePath) o;
        return method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    private String buildPageUrl(final String path, final String app, final String mapping) {
        final String marker = SLASH + app + SLASH;
        final int pos = path.indexOf(marker);
        return PathUtils.path(mapping, path.substring(pos + marker.length()));
    }
}
