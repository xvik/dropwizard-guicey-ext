package ru.vyarus.guicey.gsp.info.model;

import com.google.common.collect.Multimap;
import ru.vyarus.guicey.gsp.app.filter.redirect.ErrorRedirect;

import java.util.List;
import java.util.Map;

/**
 * Information model for registered gsp application.
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2019
 */
@SuppressWarnings("PMD.TooManyFields")
public class GspApp {

    private String name;
    private boolean mainContext;
    // mapping url relative to context
    private String mappingUrl;
    // mapping url relative to server root (including context mapping path)
    private String rootUrl;
    private List<String> requiredRenderers;

    private String mainAssetsLocation;
    // relative path (sub/) --> package mappings (com/foo/bar/)
    // "" --> root mappings
    private Multimap<String, String> assets;
    // relative path (sub/) --> rest mapping prefix (some/prefix/)
    // "" --> main application rest prefix
    private Map<String, String> views;
    private boolean hasAssetExtensions;
    private boolean hasViewsExtensions;
    private String restRootUrl;

    private String indexFile;
    private String filesRegex;
    private boolean hasDefaultFilesRegex;

    private boolean spa;
    private String spaRegex;
    private boolean hasDefaultSpaRegex;

    private Map<Integer, String> errorPages;

    /**
     * @return application name
     */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return true for main context, false for admin
     */
    public boolean isMainContext() {
        return mainContext;
    }

    public void setMainContext(final boolean mainContext) {
        this.mainContext = mainContext;
    }

    /**
     * @return application mapping url (relative to context)
     */
    public String getMappingUrl() {
        return mappingUrl;
    }

    public void setMappingUrl(final String mappingUrl) {
        this.mappingUrl = mappingUrl;
    }

    /**
     * @return application mapping url prefixed with context (full url relative to server root)
     */
    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(final String rootUrl) {
        this.rootUrl = rootUrl;
    }

    /**
     * @return list of required renderers or empty list
     */
    public List<String> getRequiredRenderers() {
        return requiredRenderers;
    }

    public void setRequiredRenderers(final List<String> requiredRenderers) {
        this.requiredRenderers = requiredRenderers;
    }

    /**
     * Never starts with slash and always ends with slash.
     *
     * @return main assets classpath location
     */
    public String getMainAssetsLocation() {
        return mainAssetsLocation;
    }

    public void setMainAssetsLocation(final String mainAssetsLocation) {
        this.mainAssetsLocation = mainAssetsLocation;
    }

    /**
     * Returned assets are sorted for resolution: registration order of packages is preserved in order to be able
     * to override resources from previous registrations. In returned model registration order is reversed,
     * so the latest registered package will be the first (simply to check it first - correctly handle overrides).
     * <p>
     * Contexts never starts with slash, but always ends (for proper matching). Root context is empty string.
     * Classpath locations never starts with slash and always ends with slash.
     *
     * @return all application asset mappings (including extensions)
     */
    public Multimap<String, String> getAssets() {
        return assets;
    }

    public void setAssets(final Multimap<String, String> assets) {
        this.assets = assets;
    }

    /**
     * When root mapping is not explicitly declared, its set to application name (so map will never be empty).
     * <p>
     * Contexts never starts with slash, but always ends (for proper matching). Root context is empty string.
     * Rest prefixes never starts with slash and always ends with slash (for proper matching).
     *
     * @return all application view mappings (including extensions)
     */
    public Map<String, String> getViews() {
        return views;
    }

    public void setViews(final Map<String, String> views) {
        this.views = views;
    }

    /**
     * @return main rest mapping prefix
     */
    public String getMainRestPrefix() {
        return views.get("");
    }

    /**
     * @return true if application assets were extended (with {@code extendApp()}, false if not
     */
    public boolean isHasAssetExtensions() {
        return hasAssetExtensions;
    }

    public void setHasAssetExtensions(final boolean hasAssetExtensions) {
        this.hasAssetExtensions = hasAssetExtensions;
    }

    /**
     * @return true if application view rest prefixes were extended (with {@code extendApp()}, false if not
     */
    public boolean isHasViewsExtensions() {
        return hasViewsExtensions;
    }

    public void setHasViewsExtensions(final boolean hasViewsExtensions) {
        this.hasViewsExtensions = hasViewsExtensions;
    }

    /**
     * May be used to construct full rest urls (relative to server root).
     *
     * @return rest context mapping url or null when jetty not yet started
     */
    public String getRestRootUrl() {
        return restRootUrl;
    }

    public void setRestRootUrl(final String restRootUrl) {
        this.restRootUrl = restRootUrl;
    }

    /**
     * @return configured index file (empty string by default, meaning index.html)
     */
    public String getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(final String indexFile) {
        this.indexFile = indexFile;
    }

    /**
     * @return asset (static resources) calls detection regexp
     */
    public String getFilesRegex() {
        return filesRegex;
    }

    public void setFilesRegex(final String filesRegex) {
        this.filesRegex = filesRegex;
    }

    /**
     * @return true when default assets detection regexp used, false otherwise
     */
    public boolean isHasDefaultFilesRegex() {
        return hasDefaultFilesRegex;
    }

    public void setHasDefaultFilesRegex(final boolean hasDefaultFilesRegex) {
        this.hasDefaultFilesRegex = hasDefaultFilesRegex;
    }

    /**
     * @return true when spa routing enabled (serve index page for all requested paths, except assets), false
     * when disabled (default)
     */
    public boolean isSpa() {
        return spa;
    }

    public void setSpa(final boolean spa) {
        this.spa = spa;
    }

    /**
     * @return non spa route path recognition regexp
     */
    public String getSpaRegex() {
        return spaRegex;
    }

    public void setSpaRegex(final String spaRegex) {
        this.spaRegex = spaRegex;
    }

    /**
     * @return true when defaul regexp used, false otherwise
     */
    public boolean isHasDefaultSpaRegex() {
        return hasDefaultSpaRegex;
    }

    public void setHasDefaultSpaRegex(final boolean hasDefaultSpaRegex) {
        this.hasDefaultSpaRegex = hasDefaultSpaRegex;
    }

    /**
     * Default error page has -1 code.
     *
     * @return configured error pages (mapping by code) or empty map if nothing configured
     */
    public Map<Integer, String> getErrorPages() {
        return errorPages;
    }

    public void setErrorPages(final Map<Integer, String> errorPages) {
        this.errorPages = errorPages;
    }

    /**
     * @return default error page (for all non mapped error codes) or null if not declared
     */
    public String getDefaultErrorPage() {
        return errorPages.get(ErrorRedirect.DEFAULT_ERROR_PAGE);
    }
}
