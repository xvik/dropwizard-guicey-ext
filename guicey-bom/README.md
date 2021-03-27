# Guicey extensions BOM module

Extension modules BOM. 

Provides:

* Guicey version
* Guice bom
* Dropwizard bom

### Setup

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/dropwizard-guicey-ext.svg?label=jcenter)](https://bintray.com/vyarus/xvik/dropwizard-guicey-ext/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus.guicey/guicey-bom.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus.guicey/guicey-bom)


Maven:

```xml
<!-- Implicitly imports Dropwizard and Guice BOMs -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>ru.vyarus.guicey</groupId>
            <artifactId>guicey-bom</artifactId>
            <version>0.7.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- declare guice and ext modules without versions -->
<dependencies>
    <dependency>
      <groupId>ru.vyarus</groupId>
      <artifactId>dropwizard-guicey</artifactId>
    </dependency>
    <!-- For example, using dropwizard module (without version) -->
    <dependency>
      <groupId>io.dropwizard</groupId>
      <artifactId>dropwizard-auth</artifactId>
    </dependency>
    <!-- Example of extension module usage -->
    <dependency>
          <groupId>ru.vyarus.guicey</groupId>
          <artifactId>guicey-eventbus</artifactId>
        </dependency>
</dependencies>
```

Gradle:

```groovy
plugins {
    id "io.spring.dependency-management" version "1.0.11.RELEASE"
}

dependencyManagement {
    // Implicitly imports Dropwizard and Guice BOMs 
    imports {
        mavenBom "ru.vyarus.guicey:guicey-bom:0.7.1"
    }
}

// declare guice and ext modules without versions 
dependencies {
    compile 'ru.vyarus:dropwizard-guicey'
    // For example, using dropwizard module (without version)
    compile 'io.dropwizard:dropwizard-auth'
    // Example of extension module usage
    compile 'ru.vyarus.guicey:guicey-eventbus' 
}
    
```

Spring's [dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin) is required to import BOM.

### Dependencies override

You may override BOM version for any dependency by simply specifying exact version in dependecy declaration section.

If you want to use newer version (then provided by guicey BOM) of dropwizard or guice then import also their BOMs directly:

* `io.dropwizard:dropwizard-bom:$VERSION` for dropwizard
* `com.google.inject:guice-bom:$VERSION` for guice
