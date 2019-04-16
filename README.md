# Entity View Interfaces in CUBA framework - Proof of Concept

<a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>

## Introduction
The purpose of the project is to eliminate issues related to detached entities when a developer tries to access an 
attribute that is not included into CUBA view. Such a manipulation leads to a runtime exception "UnfetchedAttribute" which happens at runtime and it's pretty hard to find it at compile time. 
By wrapping entities into interfaces we add compile-time validation of attribute fetching and access control (read or write). 

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
1. View name is a string and compiler won't warn us if the name is misspelled. IDE can do it for you with the help of plugins, though, but it is not very reliable.
2. According to view description field ```user``` is not fetched, but nothing prevents us from trying to get it, it is a valid operation in terms of type system. Therefore we will get runtime exception in deployed application.

So how we can add type safety into this code? With entity views it's becoming easier. We've developes a special addon that will help you with it. 

For a starter, specify a repository and add the addon artifact as a dependency to your project build file
```
repositories {
    ...
    maven {
        url "https://cuba-platform.bintray.com/labs"
    }
}
...
dependencies {
    ...
	compile 'com.haulmont.scripting:spring-script-repositories:0.1.1'
}
```
 
Inject DataManager into your code:

```java
    @Inject
    private ViewSupportDataManager dataManager;
``` 

Now we can use Entity Views in the Core module of the application. Define the following entity view interface:
 ```java
public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);
}
```

Add package name(s) that contain entity view interfaces into spring XML configuration similar to what is shown here:

```xml
<beans ...
       xmlns:views="http://www.cuba-platform.org/schema/data/views"
       xsi:schemaLocation="...
        http://www.cuba-platform.org/schema/data/views http://www.cuba-platform.org/schema/cuba-entity-views.xsd">

    ...
    <views:views base-packages="com.company.playground.views.sample"/>
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
public interface SampleWithUserView extends BaseEntityView<SampleEntity, UUID> {

    String getName();
    
    void setName(String name);

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User, UUID>{

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
That's it. Now you will get an access to ```name``` and ```usr``` attributes exposed in the ```SampleWithUserView``` interface. 
Please note that you won't be able to change User's name and login because ```UserMinimalView``` contains getters only.

 
## Solution's Internals Description

The cornerstone of the solution is an interface 
```java
public interface BaseEntityView<T extends Entity<K>, K> extends Entity<K>, Serializable {

    T getOrigin();

    <V extends BaseEntityView<T, K>> Class<V> getInterfaceClass();

    <V extends BaseEntityView<T, K>> V reload(Class<V> targetView);

}
```  
If you need to create a view for an entity you need to extend the view stated above and add methods for exposing 
corresponding entity properties. Please note that views can be nested like in this example:
```java
public interface SampleWithUserView extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User, UUID>{

        String getName();

        String getLogin();
    }
}
```
Also interfaces can be extended (like an XML CUBA views).
```java
public interface SampleMinimalView extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    void setName(String name);
}

@ReplaceEntityView(SampleMinimalView.class)
public interface SampleMinimalWithUserView extends SampleMinimalView {

    UserMinimalView getUser();

    void setUser(UserMinimalView val);

    interface UserMinimalView extends BaseEntityView<User, UUID> {

        String getLogin();
        void setLogin(String val);

        String getName();
        void setName(String val);
    }
}
```
The PoC introduces interface view replacement by using ```@ReplaceEntityView``` (similar to "overwrite" attribute in XML views), 
so there is no problems with entities extended that are annotated with ```@Extends```.

Another Entity View feature is a possibility to define default interface methods to perform calculations using Entity View fileds:
```java
public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    @MetaProperty
    default String getNameLowercase() {
        return getName().toLowerCase();
    }

}
```
Please note that default interface methods must be annotated with ```@MetaProperty``` to avoid issues during Entity View instance creation.

We have implemented a special tag that should be specified in ```spring.xml``` to enable Entity View Interfaces support like in the following
example:
```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:views="http://www.cuba-platform.org/schema/data/views"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
        http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.3.xsd
        http://www.cuba-platform.org/schema/data/views http://www.cuba-platform.org/schema/cuba-entity-views.xsd">

    <views:views base-packages="com.company.playground.views.sample"/>

</beans>
```
 The tag has only one attribute - comma-separated list of packages that should be scanned. The scanning process includes subpackages too.  

### ViewsSupportDataManager internals

CUBA's DataManager was extended to support Entity View Interface in the following way:
```java
public interface ViewSupportDataManager extends DataManager {

    <E extends Entity<K>, V extends BaseEntityView<E>, K> V reload(E entity, Class<V> viewInterface);

    <E extends Entity<K>, V extends BaseEntityView<E>, K> ViewsSupportFluentLoader<E, V, K> loadWithView(Class<V> entityView);

    <V extends BaseEntityView> V create(Class<V> viewInterface);

    <E extends Entity, V extends BaseEntityView<E>> V commit(V entityView);

    <E extends Entity, V extends BaseEntityView<E>, K extends BaseEntityView<E>> K commit(V entityView, Class<K> targetView);
}
```
Its implementation ```ViewsSupportDataManagerBean``` extends CUBA's ```DataManagerBean``` class and uses its data 
manipulation methods internally. Also there is a custom ```FluentLoader``` implementation named ```ViewsSupportFluentLoader``` 
that adds an Entity Views support.

Internally, these classes use ```ViewsConfiguration``` to get CUBA 
views by Entity View Interface definition. We generate CUBA views when the application context is initialized, that's why 
the ```ViewsConfiguration``` class implements ```ApplicationListener```. CUBA views creation is implemented in a recursive manner and we 
perform checks for cyclic references. If there is a cyclic reference in the Entity Views hierarchy, the application fails 
on a Spring context initialization phase.

Class that performes Entity to Entity View instance conversion is ```EntityViewWrapper```. You need to use
its ```wrap()``` method to wrap Entity into view. The method creates proxy for an Entity View Interface. The proxy processes all 
Entity View's method invocations using ```EntityViewWrapper.ViewInterfaceInvocationHandler``` class. The handler 
tries to find a proper method in the following order:
1. Methods defined in ```BaseEntityView``` interface.
2. Methods defined in the entity that is wrapped into the view.
3. Default interface methods defined in the Entity View Interface.
If a method is not found we get runtime exception. 
 
In most of the cases you won't need neither ```ViewsConfiguration``` nor ```EntityViewWrapper```, but you can do if needed. 

If you need to apply a different view to an entity ```BaseEntityView``` provides ```transform()``` method that reloads the entity from the 
database (if needed) with another view losing all changes that were made previously. 
```java
//...
SampleMinimalView sampleMinimalView = dataManager.loadWithView(SampleMinimalView.class)
                .query("select e from playground$SampleEntity e where e.name = :name")
                .parameter("name", "Data2")
                .list()
                .get(0);

SampleWithParentView sampleWithParent = sampleMinimalView.transform(SampleWithParentView.class);
//...
``` 


### Views Initialization Process

The diagram below displays Entity Views initialization workflow and involved classes:

<img src="https://drive.google.com/uc?export=&id=1sQ1qBX9tz4vdb2UOBEZvMQFZM-YonEAY"/>

When Spring starts context initialization, it reads ```spring.schemas``` and ```spring.handlers``` files to figure out 
where to find custom tag definition schema and handler class.

Then Spring instantiates handler class. The handler extends ```org.springframework.beans.factory.xml.NamespaceHandlerSupport```
and in its ```init()``` method we register handler for tag ```views```. In our case the code is very simple because we 
define only one custom tag.  

```java
public class ViewsNamespaceHandler extends NamespaceHandlerSupport {

    public static final String VIEWS = "views";

    @Override
    public void init() {
        registerBeanDefinitionParser(VIEWS, new ViewsConfigurationParser());
    }
}
```

That's it. Now Spring knows which parser will process custom tag defined in the config. When config processing starts, 
for each tag entry, Spring invokes ```ViewsConfigurationParser#parse()``` method.

When the method is invoked it scans packages specified in tag attribute and gathers all view interface definitions. 
Please look at ```ViewsConfigurationParser#scanForViewInterfaces()``` method for details.

When all view interface definitions are gathered we create and register view interface repository as a Spring bean:
```java
BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ViewsConfiguration.class)
        .addConstructorArgValue(viewInterfaceDefinitions);
AbstractBeanDefinition viewsConfigurationBean = builder.getBeanDefinition();
registry.registerBeanDefinition(ViewsConfiguration.NAME, viewsConfigurationBean);
```
The registry - ```ViewsConfiguration``` now can be injected into both core services or 
UI controllers. In the second case you should specify custom tag in ```web``` module's ```web-spring.xml``` configuration.

```ViewsConfiguration``` stores all VEntity iew Interfaces and performs views substitution automatically after initialization 
in ```ViewsConfiguration#buildViewSubstitutionChain()```. We need views substitution to 
implement entity extension mechanism properly. 

Entity View Interfaces are ready for use. They will be used to build CUBA views, so this concept do not break existing 
codebase and you can use old XML-defined views as well as Entity Views.  

## Conclusion
Entity View Interfaces is a new feature of the platform which is backward-compatible with existing programming model, but adds strong
typing to entity manipulation processes and prevents developers from accessing unfetched attributes.
