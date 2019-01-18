package ru.vyarus.guicey.gsp.app.rest.log;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import java.util.Comparator;

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
    private final String url;

    public ResourcePath(final ResourceMethod method,
                        final Resource resource,
                        final Class klass,
                        final String url) {
        this.method = method;
        this.resource = resource;
        this.klass = klass;
        this.url = url;
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
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "template (" + url + ")";
    }

    @Override
    public int compareTo(final ResourcePath o) {
        return ComparisonChain.start()
                .compare(url, o.url)
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
}
