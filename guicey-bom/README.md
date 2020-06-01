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
            <version>5.1.0-1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- uncomment to override dropwizard and its dependencies versions  
        <dependency>
            <groupId>io.dropwizard/groupId>
            <artifactId>dropwizard-dependencies</artifactId>
            <version>2.0.10</version>
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
dependencies {
    implementation platform('ru.vyarus.guicey:guicey-bom:5.1.0-1')
    // uncomment to override dropwizard and its dependencies versions    
    //implementation platform('io.dropwizard:dropwizard-dependencies:2.0.10')
    
    // declare guice and ext modules without versions 
    implementation 'ru.vyarus:dropwizard-guicey'
    implementation 'io.dropwizard:dropwizard-auth'
    implementation 'ru.vyarus.guicey:guicey-eventbus' 
}
    
```

### Dependencies override

You may override BOM version for any dependency by simply specifying exact version in dependecy declaration section.

If you want to use newer version (then provided by guicey BOM) of dropwizard or guice then import also their BOMs directly:

* `io.dropwizard:dropwizard-dependencies:$VERSION` for dropwizard
* `com.google.inject:guice-bom:$VERSION` for guice
