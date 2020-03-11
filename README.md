# Projection Interfaces in CUBA framework - Proof of Concept

<a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>

## Introduction
The purpose of the project is to eliminate issues related to detached entities when a developer tries to access an 
attribute that is not included into CUBA view. Such a manipulation leads to a runtime exception "UnfetchedAttribute" 
which happens at runtime and it's pretty hard to find it at compile time. By wrapping entities into interfaces we 
add compile-time validation of attribute fetching and access control (read or write). 

Let's have a look at a simple entity:

```java
@Table(name = "PLAYGROUND_SAMPLE_ENTITY")
@Entity(name = "playground$SampleEntity")
public class SampleEntity extends StandardEntity {

    @NotNull
    @Column(name = "NAME", nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected User user;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
```  

If you want to expose only ``name`` attribute, in CUBA you may want to create the following XML view:
```xml
    <view class="com.company.playground.entity.SampleEntity"
          extends="_local"
          name="sample-entity-browse-view">
            <property name="name"/>
    </view>
```

And use it like this:
```java
SampleEntity sample = dataManager.load(SampleEntity.class).view("sample-entity-browse-view").list().get(0);
sample.setName("Sample 1");//OK
User user = sample.getUser;//IllegalEntityStateException is thrown
```

In this code we have two potential issues:
1. View name is a string and compiler won't warn us if the name is misspelled. IDE can do it for you with the help of 
plugins, though, but it is not very reliable.
2. According to view description field ```user``` is not fetched, but nothing prevents us from trying to get it, it is 
a valid operation in terms of type system. Therefore we will get runtime exception in deployed application.

So how we can add type safety into this code? With entity projections it's becoming easier. We've developed a special 
add-on that will help you with it. 

## Usage

For a starter, specify a repository as the last entry in your repositories list in the ```build.gradle``` file: 
```
repositories {
    ...
    maven {
        url "https://cuba-platform.bintray.com/labs"
    }
}
```
Add the component to the dependencies list (or specify it in project settings using GUI):
```
...
dependencies {
    ...
    appComponent("com.haulmont.addons.cuba.entity.projections:entity-projections-global:0.1.5")
}
```
 
Inject DataManager into your code:

```java
    @Inject
    private DataManager dataManager;
``` 

Now we can use Entity Views in the Core module of the application. Define the following entity view interface:
 ```java
public interface SampleMinimalView extends BaseProjection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

}
```

Add package name(s) that contain entity view interfaces into spring XML configuration similar to what is shown here:

```xml
<beans ...
       xmlns:projections="http://www.cuba-platform.org/schema/data/projections"
       xsi:schemaLocation="...
        http://www.cuba-platform.org/schema/data/projections http://www.cuba-platform.org/schema/cuba-projections.xsd">

    ...
    <projections:projections base-packages="com.haulmont.addons.cuba.entity.projections.test.app.views.sample"/>
    ...
</beans>
```

And now you use this interface in your code instead of entity class. 
```java
SampleMinimalView sample = dataManager.loadWithView(SampleMinimalView.class).list().get(0);
sample.setName("Sample 1");//OK
User user = sample.getUser();//Won't compile
```
Please note that you won't be able to access ```user``` attribute and in addition you are able to 
restrict access to a ```name``` property by declaring an interface only with getter.

If you want to use another entity view interface, e.g. if you want to expose ```User``` attribute, you can do the following:

Define an interface that exposes desired properties

```java
public interface SampleWithUserView extends BaseProjection<SampleEntity, UUID> {

    String getName();
    
    void setName(String name);

    UserMinimalView getUser();

    interface UserMinimalView extends BaseProjection<User, UUID>{

        String getName();

        String getLogin();
    }
}

```
And reload an entity using this new interface

```java
SampleMinimalView sample = dataManager.loadWithView(SampleMinimalView.class).list().get(0);
SampleWithUserView sampleMinimal = sample.reload(SampleWithUserView.class);
```
That's it. Now you will get an access to ```name``` and ```user``` attributes exposed in the ```SampleWithUserView``` interface. 
Please note that you won't be able to change User's name and login because ```UserMinimalView``` contains getters only.

 
## Solution's Internals Description

The cornerstone of the solution is an interface 
```java
public interface BaseProjection<T extends Entity<K>, K> extends Entity<K>, Serializable {

    T getOrigin();

    <V extends BaseProjection<T, K>> Class<V> getInterfaceClass();

    <V extends BaseProjection<T, K>> V reload(Class<V> targetView);

}
```  
If you need to create a projection for an entity you need to extend the projection stated above and add methods for exposing 
corresponding entity properties. Please note that projections can be nested like in this example:
```java
public interface SampleWithUserView extends BaseProjection<SampleEntity, UUID> {

    String getName();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseProjection<User, UUID>{

        String getName();

        String getLogin();
    }
}
```
Also interfaces can be extended (like an XML CUBA views).
```java
public interface SampleMinimalView extends BaseProjection<SampleEntity, UUID> {

    String getName();

    void setName(String name);
}

@ReplaceProjection(SampleMinimalView.class)
public interface SampleMinimalWithUserView extends SampleMinimalView {

    UserMinimalView getUser();

    void setUser(UserMinimalView val);

    interface UserMinimalView extends BaseProjection<User, UUID> {

        String getLogin();
        void setLogin(String val);

        String getName();
        void setName(String val);
    }
}
```
The PoC introduces interface view replacement by using ```@ReplaceProjection``` (similar to "overwrite" attribute in XML views), 
so there is no problems with entities extended that are annotated with ```@Extends```.

Another Projection feature is a possibility to define default interface methods to perform calculations using Projection's fields:
```java
public interface SampleMinimalView extends BaseProjection<SampleEntity> {

    String getName();

    void setName(String name);

    @MetaProperty
    default String getNameLowercase() {
        return getName().toLowerCase();
    }

}
```
Please note that default interface methods must be annotated with ```@MetaProperty``` to avoid issues during Projection instance creation.

We have implemented a special tag that should be specified in ```spring.xml``` to enable Projection Interfaces support like in the following
example:
```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:projections="http://www.cuba-platform.org/schema/data/projections"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
        http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.3.xsd
        http://www.cuba-platform.org/schema/data/projections http://www.cuba-platform.org/schema/cuba-projections.xsd">

    <projections:projections base-packages="com.company.playground.views.sample"/>

</beans>
```
 The tag has only one attribute - comma-separated list of packages that should be scanned. The scanning process includes subpackages too.  

### ProjectionSupportDataManagerBean internals

CUBA's DataManager was extended to support Projection Interface. Its implementation ```ProjectionSupportDataManagerBean``` 
extends CUBA's ```DataManagerBean``` class and uses its data manipulation methods internally. Also there is a custom 
```FluentLoader``` implementation named ```ViewsSupportFluentLoader``` that adds an Entity Views support.

Internally, these classes use ```ProjectionsConfiguration``` to get CUBA views by Projection Interface definition. 
We generate CUBA views when the application context is initialized, that's why the ```ProjectionsConfiguration``` class 
implements ```ApplicationListener```. CUBA views creation is implemented in a recursive manner and we perform checks for 
cyclic references. If there is a cyclic reference in the Projections hierarchy, the application fails on a Spring context 
initialization phase.

Class that performes Entity to Projection instance conversion is ```EntityProjectionWrapper```. You need to use
its ```wrap()``` method to wrap Entity into projection. 
 
In most of the cases you won't need neither ```ViewsConfiguration``` nor ```EntityViewWrapper```, but you can do if needed. 

If you need to apply a different view to an entity ```BaseProjection``` provides ```reload()``` method that lazily reloads the entity from the 
database (if needed) with another view losing all changes that were made previously. 
```java
//...
SampleMinimalView sampleMinimalView = dataManager.load(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

SampleWithParentView sampleWithParent = sampleMinimalView.reload(SampleWithParentView.class);
//...
``` 


### Projections Initialization Process

The diagram below displays Projections initialization workflow and involved classes:

<img src="https://drive.google.com/uc?export=&id=1sQ1qBX9tz4vdb2UOBEZvMQFZM-YonEAY"/>

When Spring starts context initialization, it reads ```spring.schemas``` and ```spring.handlers``` files to figure out 
where to find custom tag definition schema and handler class.

Then Spring instantiates handler class. The handler extends ```org.springframework.beans.factory.xml.NamespaceHandlerSupport```
and in its ```init()``` method we register handler for tag ```projections```. In our case the code is very simple because we 
define only one custom tag.  

```java
public class ProjectionsNamespaceHandler extends NamespaceHandlerSupport {

    public static final String PROJECTIONS = "projections";

    @Override
    public void init() {
        registerBeanDefinitionParser(PROJECTIONS, new ProjectionConfigurationParser());
    }
}
```

That's it. Now Spring knows which parser will process custom tag defined in the config. When config processing starts, 
for each tag entry, Spring invokes ```ProjectionConfigurationParser#parse()``` method.

When the method is invoked it scans packages specified in tag attribute and gathers all projection definitions. 
Please look at ```ProjectionConfigurationParser#scanForProjections()``` method for details.

When all view projection definitions are gathered we create and register projections repository as a Spring bean:
```java
BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ProjectionsConfigurationBean.class)
        .addConstructorArgValue(projectionDefinitions);
AbstractBeanDefinition projectionsConfigurationBean = builder.getBeanDefinition();
registry.registerBeanDefinition(ProjectionsConfigurationBean.NAME, projectionsConfigurationBean);
```
The registry - ```ProjectionsConfiguration``` now can be injected into both core services or 
UI controllers. In the second case you should specify custom tag in ```web``` module's ```web-spring.xml``` configuration.

```ProjectionsConfiguration``` stores all Projection Interfaces and performs Projections substitution automatically after initialization 
in ```ProjectionsConfiguration#buildProjectionSubstitutionChain()```. We need projection substitution to 
implement entity extension mechanism properly. 

Projection Interfaces are ready for use. They will be used to build CUBA views, so this concept do not break existing 
codebase and you can use old XML-defined views as well as Projections.  

### Running Tests
To run tests you need to run HSQLDB first and then execute tests:
```
gradlew clean startDb createDb test stopDb
```

## Conclusion
Projection Interfaces is a new feature of the platform which is backward-compatible with existing programming model, but adds strong
typing to entity manipulation processes and prevents developers from accessing unfetched attributes.
