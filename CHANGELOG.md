* Update to guicey 5.0.0
    - Ext version now aligned with guicey the same way as dropwizard modules: guiceyVersion-number (5.0.0-0)
* Java 11 compatibility. Automatic module name (in meta-inf): `dropwizard-guicey.$module`    
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
* [server-pages]
    - (breaking) ServerPagesBundle and application bundle are guicey bundles now
    - (breaking) `ServerPagesBundle.extendApp()` now return bundle and must be registered!
    - (breaking) Removed `ServerPagesBundle.register(GuiceyBundle)` as redundant
    - (breaking) Removed `ServerPagesBundle.resetGlobalConfig()` because guicey shared configs now used instead 
    - Add shortcut for multiple paths registration on application:
        `ServerPagesBundle.app(..).attachPaths(..)`     
* [jdbi3]
    - JdbiBundle declared as unique (it will not work with multiple instances)
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