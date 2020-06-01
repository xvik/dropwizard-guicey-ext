# Dropwizard-guicey extensions
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/dropwizard-guicey-ext.svg?style=flat)](https://travis-ci.org/xvik/dropwizard-guicey-ext)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/dropwizard-guicey-ext?svg=true&branch=master)](https://ci.appveyor.com/project/xvik/dropwizard-guicey-ext)
[![codecov](https://codecov.io/gh/xvik/dropwizard-guicey-ext/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/dropwizard-guicey-ext)

### About

[Dropwizard-guicey 5.x](https://github.com/xvik/dropwizard-guicey) extensions and integrations. 
Provided modules may be used directly and for educational purposes (as examples for custom integrations).

NOTE: Extension modules version is derived from guicey version: guiceyVersion-Number 
(the same convention as for dropwizard modules). For example version 5.0.0-1 means
first extensions release (1) for guicey 5.0.0. 

Also, guicey base package `ru.vyarus.dropwizard.guice` is different from extensions base package `ru.vyarus.guicey`.

Older versions:

* [Guicey 4.x extensions branch](https://github.com/xvik/dropwizard-guicey-ext/tree/guicey-4)

### Setup
 
[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/dropwizard-guicey-ext.svg?label=jcenter)](https://bintray.com/vyarus/xvik/dropwizard-guicey-ext/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus.guicey/guicey-bom.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus.guicey/guicey-bom)

You can either use [modules](#modules) directly (in this case see module page for setup) or use provided [BOM](guicey-bom)
to unify versions management.

Note that BOM will also provide guice and dropwizard BOMs, so you can avoid declaring versions of these modules too.

[BOM usage](guicey-bom#setup) is recommended as it allows correct dropwizard dependencies update. 

### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

WARNING: snapshot may not contain today's commits due to ~1day publication lag!

WARNING2: master-SHAPSHOT versions are not working due to incorrect pom generation (yet unkown reason), use exact commits instead

Add [JitPack](https://jitpack.io/#ru.vyarus.guicey/dropwizard-guicey-ext) repository:

```groovy
repositories { maven { url 'https://jitpack.io' } }
```

For spring dependencies plugin:

```groovy
dependencyManagement {
    resolutionStrategy {
        cacheChangingModulesFor 0, 'seconds'
    }
    imports {
        mavenBom "ru.vyarus.guicey:guicey-bom:COMMIT-HASH"
    }
}   

dependencies {
    implementation 'ru.vyarus.guicey:guicey-validation'
}
``` 

If you don't use BOM:

```groovy
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}

dependencies {
    implementation 'ru.vyarus.guicey:guicey-validation:COMMIT-HASH'
}
```

Note that in both cases `resolutionStrategy` setting required for correct updating snapshot with recent commits
(without it you will not always have up-to-date snapshot)

OR you can depend on exact commit:

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus.guicey/dropwizard-guicey-ext)
* Select `Commits` section and click `Get it` on commit you want to use and 
 use commit hash as version: `ru.vyarus.guicey:guicey-bom:8585300d12`


Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>  

<dependencyManagement> 
    <dependencies>
        <dependency>
            <groupId>ru.vyarus.guicey</groupId>
            <artifactId>guicey-bom</artifactId>
            <version>COMMIT-HASH</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>  
 
<dependencies>
    <dependency>
        <groupId>ru.vyarus.guicey</groupId>
        <artifactId>guicey-validation</artifactId>
    </dependency>
</dependencies>
```     

Or simply change dependency version if BOM not used (repository definition is still required).

</details>    

### Modules

#### [Admin REST](guicey-admin-rest)

Admin context rest support (mirror main rest).

#### [Lifecycle annotations](guicey-lifecycle-annotations)

Support for @PostConstruct, @PreDestroy, @PostStartup annotations on guice beans.

#### [Validation](guicey-validation)

Allows using validation annotations on any guice bean method (the same way as [dropwizard rest validation](https://www.dropwizard.io/en/stable/manual/validation.html)) 

#### [Guava EventBus integration](guicey-eventbus) 

Module provides integration with Guava EventBus: automates subscriptions, report events with subscriptions and registers EventBus for inject.

#### [JDBI integration](guicey-jdbi) 

Based on dropwizard integration. Introduce thread bound transactions, defined with annotations. 
Sql proxies could be used as usual guice beans without extra efforts to use them in the same transaction. 

#### [JDBI3 integration](guicey-jdbi3)

Jdbi3 integration, based on dropwizard module. Introduce thread bound transactions, defined with annotations. 
Sql proxies could be used as usual guice beans without extra efforts to use them in the same transaction.

#### [Single page applications](guicey-spa)

Correct redirect to index page for html5 client routing urls (e.g. html5 routing enabled by default in Angular 2). 

#### [Guicey Server Pages](guicey-server-pages)

JSP like simplicity for dropwizard-views.

---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)