# Guicey extensions BOM module

Extension modules BOM. 

Provides:

* Guicey bom
* Guice bom
* Dropwizard boms
* Additional dependencies used by modules 

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
            <version>5.0.1-1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- uncomment to override dropwizard and its dependencies versions  
        <dependency>
            <groupId>io.dropwizard/groupId>
            <artifactId>dropwizard-dependencies</artifactId>
            <version>2.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency> --> 
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
    id "io.spring.dependency-management" version "1.0.9.RELEASE"
}

dependencyManagement {
    // Implicitly imports Dropwizard and Guice BOMs 
    imports {
        mavenBom "ru.vyarus.guicey:guicey-bom:5.0.1-1"
        // uncomment to override dropwizard version    
        // mavenBom 'io.dropwizard:dropwizard-dependencies:2.0.2' 
    }
}

// declare guice and ext modules without versions 
dependencies {
    implementation 'ru.vyarus:dropwizard-guicey'
    // For example, using dropwizard module (without version)
    implementation 'io.dropwizard:dropwizard-auth'
    // Example of extension module usage
    implementation 'ru.vyarus.guicey:guicey-eventbus' 
}
    
```

Spring's [dependency management plugin](https://github.com/spring-gradle-plugins/dependency-management-plugin) is required to import BOM.
It is recommended to use it instead of [built-in gradle bom support](https://docs.gradle.org/current/userguide/migrating_from_maven.html#migmvn:using_boms)
because of [more correct spring plugin behaviour](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/211#issuecomment-387362326)

### Dependencies override

You may override BOM version for any dependency by simply specifying exact version in dependecy declaration section.

If you want to use newer version (then provided by guicey BOM) of dropwizard or guice then import also their BOMs directly:

* `io.dropwizard:dropwizard-bom:$VERSION` and `io.dropwizard:dropwizard-dependencies:$VERSION` for dropwizard
* `com.google.inject:guice-bom:$VERSION` for guice
