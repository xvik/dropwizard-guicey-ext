# Dropwizard-guicey extensions
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/dropwizard-guicey-ext.svg?style=flat)](https://travis-ci.org/xvik/dropwizard-guicey-ext)
[![Coverage Status](https://img.shields.io/coveralls/xvik/dropwizard-guicey-ext.svg?style=flat)](https://coveralls.io/r/xvik/dropwizard-guicey-ext?branch=master)

### About

[Dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) extensions and integrations. 
Provided modules may be used directly and for educational purposes (as examples for custom integrations).

NOTE: Guicey and extension modules use *different* versions because release cycles are not unified (obviously, extensions would release more often, at least at first).
But all modules use the same version. Provided [BOM](guicey-bom) simplifies version management.

Also, note that guicey base package (`ru.vyarus.dropwizard.guice`) is different from extensions base package (`ru.vyarus.guicey`)

In versions prior to 1.0.0 semantic visioning is not guaranteed (it does not relate to quality, just some breaking changes are possible).

### Setup
 
Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/dropwizard-guicey-ext/) and 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus.guicey/guicey-bom) 

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/dropwizard-guicey-ext.svg?label=jcenter)](https://bintray.com/vyarus/xvik/dropwizard-guicey-ext/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus.guicey/guicey-bom.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus.guicey/guicey-bom)

You can either use [modules](#modules) directly (in this case see module page for setup) or use provided [BOM](guicey-bom)
to unify versions management.

[BOM usage](guicey-bom#setup) is recommended.

Note that BOM will also provide guice and dropwizard BOMs, so you can avoid declaring versions of these modules too. 

<!--
##### Snapshots

You can use snapshot versions through [JitPack](https://jitpack.io):

* Go to [JitPack project page](https://jitpack.io/#xvik/dropwizard-guicey-ext)
* Select `Commits` section and click `Get it` on commit you want to use (top one - the most recent)
* Follow displayed instruction: add repository and change dependency (NOTE: due to JitPack convention artifact group will be different)
-->

### Modules

#### [Lifecycle annotations](guicey-lifecycle-annotations)

Support for @PostConstruct, @PreDestroy, @PostStartup annotations on guice beans. 

#### [Guava EventBus integration](guicey-eventbus) 

Module provides integration with Guava EventBus: automates subscriptions, report events with subscriptions and registers EventBus for inject.

#### [JDBI integration](guicey-jdbi) 

Based on dropwizard integration. Introduce thread bound transactions, defined with annotations. 
Sql proxies could be used as usual guice beans without extra efforts to use them in the same transaction. 

#### [JDBI3 integration](guicey-jdbi3)

Jdbi3 integrtation, based on dropwizard module. Introduce thread bound transactions, defined with annotations. 
Sql proxies could be used as usual guice beans without extra efforts to use them in the same transaction.

#### [Single page applications](guicey-spa)

Correct redirect to index page for html5 client routing urls (e.g. html5 routing enabled by default in Angular 2). 

---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)