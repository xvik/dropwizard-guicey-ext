# Dropwizard-guicey extensions
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/dropwizard-guicey-ext.svg?style=flat)](https://travis-ci.org/xvik/dropwizard-guicey-ext)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/dropwizard-guicey-ext?svg=true&branch=master)](https://ci.appveyor.com/project/xvik/dropwizard-guicey-ext)
[![codecov](https://codecov.io/gh/xvik/dropwizard-guicey-ext/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/dropwizard-guicey-ext)

### About

[Dropwizard-guicey 5.x](https://github.com/xvik/dropwizard-guicey) extensions and integrations. 
Provided modules may be used directly and for educational purposes (as examples for custom integrations).

NOTE: Extension modules version is derived from guicey version: guiceyVersion-Number 
(the same convention as for dropwizard modules). For example version 5.0.0-0 means
first extensions release (0) for guicey 5.0.0. 

Also, guicey base package `ru.vyarus.dropwizard.guice` is different from extensions base package `ru.vyarus.guicey`.

Older versions:

* [Guicey 4.x extensions branch](https://github.com/xvik/dropwizard-guicey-ext/tree/guicey-4)

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

Jdbi3 integration, based on dropwizard module. Introduce thread bound transactions, defined with annotations. 
Sql proxies could be used as usual guice beans without extra efforts to use them in the same transaction.

#### [Single page applications](guicey-spa)

Correct redirect to index page for html5 client routing urls (e.g. html5 routing enabled by default in Angular 2). 

#### [Guicey Server Pages](guicey-server-pages)

JSP like simplicity for dropwizard-views.

---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)