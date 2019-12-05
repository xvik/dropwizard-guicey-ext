package ru.vyarus.guicey.gsp.app.rest.log;

import ru.vyarus.guicey.gsp.app.util.PathUtils;

/**
 * Gsp application hidden mapping. Appears when different rest prefix mapped to sub url (so root prefix resources
 * under the same sub url become hidden).
 *
 * @author Vyacheslav Rusakov
 * @since 05.12.2019
 */
public class HiddenViewPath extends MappedViewPath {
    private final String overridingMapping;

    public HiddenViewPath(final ViewPath path,
                          final String mapping,
                          final String prefix,
                          final String overridingMapping) {
        super(path, mapping, prefix);
        this.overridingMapping = overridingMapping;
    }

    /**
     * Returned url never starts with slash, but alwasys ends with slash.
     *
     * @return hiding mapping (sub url)
     */
    public String getOverridingMapping() {
        return overridingMapping;
    }

    @Override
    public String toString() {
        return super.toString() + " hidden by " + PathUtils.prefixSlash(overridingMapping);
    }
}
