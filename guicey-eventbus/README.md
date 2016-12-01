# Guava EventBus integration

### About

Integrates [Guava EventBus](https://github.com/google/guava/wiki/EventBusExplained) with guice.
 
Features:

* EventBus available for injection (to publish events)
* Automatic registration of listener methods (annotated with `@Subscribe`)
* Console reporting of registered listeners
 
### Setup

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/dropwizard-guicey-ext.svg?label=jcenter)](https://bintray.com/vyarus/xvik/dropwizard-guicey-ext/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus.guicey/guicey-eventbus.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus.guicey/guicey-eventbus)

If you use [extensions BOM](../guicey-bom) then avoid version in dependency declaration. 

Maven:

```xml
<dependency>
  <groupId>ru.vyarus.guicey</groupId>
  <artifactId>guicey-eventbus</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```groovy
compile 'ru.vyarus.guicey:guicey-eventbus:0.1.0'
```

See the most recent version in the badge above.

### Usage

Register bundle:

```java
GuiceBundle.builder()        
        .bundles(new EventBusBundle())
        ...
```

Create event:

```java
public class MyEvent {
    // some state
}
```

Inject `EventBus` to publish new events.

```java
public class SomeService {
    @Inject
    private EVentBus eventbus;    
    
    public void inSomeMethod() {
        evetbus.post(new MyEvent());
    }
}
```

Listen for event:

```java
public class SomeOtherService {
    
    @Subscribe
    public void onEvent(MyEvent event) {
         // handle event   
    }
}
```

NOTE: SomeOtherService must be either explicitly declared in guice module, or some
extension or explicitly declared service depend on it. Without direct registration or eager usage
service will not be created on injector creation and service will not be subscribed (logica - it wasn't created - it's not subscribed).

After server start you should see all registered event listeners in log:

```
INFO  [2016-12-01 12:31:02,819] ru.vyarus.guicey.eventbus.EventsTracker: EventBus subscribers = 

    MyEvent
        com.foo.something.SomeOtherService        

```