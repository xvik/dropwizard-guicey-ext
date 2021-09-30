* Module poms use direct dependencies (without dependencyManagement section as before)
* BOM does not declare properties from guicey BOM anymore: dropwizard.version, guice.version and hk2.version
* [validation]
  - Add strictGroupsDeclaration() for ValidationBundle (avoid implicit Default group usage) (#69)

IMPORTANT: Since dropwizard 2.0.22 dropwizard-jdbi3 module [dropped java 8 compatibility](https://github.com/dropwizard/dropwizard/releases/tag/v2.0.22)
due to caffeine library upgrade (see [caffeine author explanation](https://github.com/jdbi/jdbi/issues/1853#issuecomment-819101724))
(actually, new jdbi3 version depends on caffeine 3 while dropwizard itself still depends on caffeine 2).

To bring back jdk 8 compatibility you must manually force caffeine version:

    implementation com.github.ben-manes.caffeine:caffeine:2.9.2

### 5.3.0-1 (2021-03-06)
* No changes, except BOM versions

### 5.2.0-1 (2020-11-29)
* [gsp]
    - Auto append leading slash to provided app mapping uri (to prevent hard to understand configuration error)
    - Fix app reporting for view paths not started with slash (e.g. @Path("views/something")) 

### 5.1.0-2 (2020-06-23)
* [jdbi3]
    - Fix unit of work closing after connection error (preventing application recover) (#35)
    - Add eager jdbi proxies initialization option: `JdbiBundle.withEagerInitialization()` (#33)
* [jdbi2]
    - Fix unit of work closing after connection error (preventing application recover) (#35)    

### 5.1.0-1 (2020-06-02)
* [gsp]
    - Fix template index page recognition when root context called without trailing slash
    - Support assets loading from custom class loaders (very specialized case):
        - Alternative methods added with class loader parameter: `ServerPagesBundle.app(..., class loader)`, 
          `ServerPagesBundle.adminApp(..., class loader)` and `ServerPagesBundle.extendApp(..., class loader)`.
          Also, in extended bundle dynamic configuration callback could specify loaders directly.
        - NOTE: by default this will work ONLY for static resources, template engines in most cases would not
          be aware of custom class loaders (but in some cases would be able to resolve resources with view resource class loader).
          It is only possible to properly support custom loaders in freemarker. Support activation shortcut
          added to global builder: `ServerPagesBundle.builder().enableFreemarkerCustomClassLoadersSupport()`
          (which will register custom freemarker template loader). 
* Fix invalid Automatic-Module-Name to "ru.vyarus.dropwizard.guicey.<module>"                                 

### 5.0.1-1 (2020-03-13)
* Update dropwizard-flyway to 2.0.2-1
* [gsp]
    - Move configuration initialization after guicey complete startup to be able to use guicey extensions
        via delayedConfiguration (change does not affect current usages)
* [jdbi]
    - Switch to externalized dropwizard-jdbi module (2.0.2)             
* Add validation module: applies validation annotations support on all guice-managed service methods
    (the same way as dropwizard allows validation annotations on rest services)     

### 5.0.0-0 (2019-12-15)
* Update to guicey 5.0.0
    - Ext version now aligned with guicey the same way as dropwizard modules: guiceyVersion-number (5.0.0-0)
* Java 11 compatibility. Automatic module name (in meta-inf): `dropwizard-guicey.$module`
* Add admin-rest bundle (moved from guicey core)    
* [bom]
    - Add com.h2database:h2:1.4.199 
    - Add io.dropwizard.modules:dropwizard-flyway:1.3.0-4     
* [lifecycle-annotations]
    - LifecycleAnnotationsBundle declared as unique to prevent default bundle registration by lookup in case of manual customization
* [eventbus]
    - EventBusBundle declared as unique to prevent side effects of duplicate registrations    
* [spa]
    - (breaking) SpaBundle is guicey bundle now
    - (breaking) Removed `SpaBundle.register(GuiceyBundle)` as redundant
    - (breaking) Fix spa redirection detection for wildcard (\*/*) Accept requests: 
        html request reqcognized now only by direct text/html 
        (ignoring wildcard used by browsers for resources loading)
    - Allow package notion for resources registration (com.company.project)                 
* [server-pages]
    - (breaking) ServerPagesBundle and application bundle are guicey bundles now
    - (breaking) `ServerPagesBundle.extendApp()` returns bundle builder used to extend application and resulted 
        bundle and must be registered!
         - Extensions may be configured under run phase with special callback
    - (breaking) Removed `ServerPagesBundle.register(GuiceyBundle)` as redundant
    - (breaking) Removed `ServerPagesBundle.resetGlobalConfig()` because guicey shared configs now used instead 
    - Add shortcut for multiple paths registration on application:
        `ServerPagesBundle.app(..).attachAssets(..)`     
    - Allow package notion for resources registration (com.company.project)
    - Allow resources registration on specific sub url
    - Allow views mapping configuration (by default, application name prefix used)
    - Allow views mapping to sub urls
    - Fix error page render instead of asset 404 error (during browser resource call)
    - Set UTF-8 encoding by default for views rendering
    - Fix return 404 instead of 500 for not found view rest path 
        (try to render template only if it looks like a template, otherwise consider wrong url)
    - Direct templates support now use ExceptionMapper instead of dynamically mapped wildcard resource
        This will fix not served direct templates issue (appeared due to jersey paths matching algorithms)         
* [jdbi3]
    - JdbiBundle declared as unique (it will not work with multiple instances)
    - Detect when repository base class is also annotated with @JdbiRepository to prevent confusing errors (#4)
    - Prevent repositories declaration in bindings
* [jdbi]
    - JdbiBundle declared as unique (it will not work with multiple instances)    
    - module deprecated (because dropwizard-jdbi was deprecated and moved to separate repo https://github.com/dropwizard/dropwizard-jdbi)    

### 0.7.0 (2019-06-17)
* Add Guicey Server Pages module (bringing JSP like usage simplicity to dropwizard-views)

### 0.6.0 (2018-11-28)
* Add lifecycle-annotations module
* Update to guicey 4.2.2

### 0.5.0 (2018-10-01)
* Update to guicey 4.2.1
* Add jdbi3 module (#2)
* Remove possible warnings about synthetic methods for jdbi2 repositories

### 0.4.0 (2018-06-25)
* Update to guicey 4.2.0 (dropwizard 1.3.5)
* Fix jdbi compatibility with guice 4.2.0 

### 0.3.0 (2017-05-10)
* Update to guicey 4.1.0 (dropwizard 1.1.0)
* Fix jdbi compatibility with dropwizard 1.1.0 (#1)
* Add single pages application module (html5 routing support)

### 0.2.1 (2016-12-06)
* Fix jdbi module pom

### 0.2.0 (2016-12-06)
* Add JDBI integration module

### 0.1.0 (2016-12-04)
* Initial release