# View Interfaces in CUBA framework - Proof of Concept

<a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>

## Introduction
The purpose of the project is to eliminate issues related to detached entities when a developer tries to access an 
attribute that is not included into view which leads to a runtime exception. By wrapping entities into interfaces 
we add compile-time validation of attribute fetching and access control (read or write). 

Let's have a look at a simple entity:
```java
@Table(name = "PLAYGROUND_SAMPLE_ENTITY")
@Entity(name = "playground$SampleEntity")
public class SampleEntity extends StandardEntity {
    private static final long serialVersionUID = 6323743611817286101L;

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
If you want to expose only ``name`` attribute in CUBA you may want to create the following XML view:
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
1. View name is a string and compiler won't warn us about it. IDE can do it for you with the help of plugins, though, but it is not very reliable.
2. According to view description field ```user``` is not fetched, but nothing prevents us from trying to get it, it is a valid operation in terms of type system. Therefore we will get runtime exception in deployed application.

So how we can add type safety into this code? With the new approach you will need to create the following interface:
```java
public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);
}
```
And use this interface in your code instead of entity class. 
```java
SampleMinimalView sample = dataManager.loadWithView(SampleMinimalView.class).list().get(0);
sample.setName("Sample 1");//OK
User user = sample.getUser();//Won't compile
```
Please note that you won't be able to access ```user``` attribute and in addition you are able to 
restrict property change by declaring an interface only with getter.

 
## Solution Description

The cornerstone of the solution is an interface 
```java
public interface BaseEntityView<T extends Entity> extends Entity, Serializable {

    T getOrigin();

    <V extends BaseEntityView<T>> Class<V> getInterfaceClass();

    <V extends BaseEntityView<T>> V transform(Class<? extends BaseEntityView<T>> targetView);

}
```  
If you need to create a view for an entity you need to extend the view stated above and add methods for exposing 
corresponding entity properties. Please note that views can be nested like in this example:
```java
public interface SampleWithUserView extends BaseEntityView<SampleEntity> {

    String getName();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User>{

        String getName();

        String getLogin();
    }
}
```
Also interfaces can be extended (like an XML CUBA views).
```java
public interface SampleMinimalWithUserView extends SampleMinimalView {

    UserMinimalView getUser();

    void setUser(UserMinimalView val);

    interface UserMinimalView extends BaseEntityView<User>{

        String getLogin();

        String getName();
    }
}
```
The PoC introduces interface view replacement by using ```@ReplaceEntityView``` (similar to "overwrite" attribute in XML views), 
so there is no problems with entities extended that are annotated with ```@Extends```.

CUBA's DataManager was extended to support Entity Views in the following way:
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
that adds an Entity View support.

We have implemented a special tag that should be specified in ```spring.xml``` to enable Entity Views support
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
Using this tag you can specify more than one package using comma as a separator. 

The diagram below displays Entity Views initialization workflow and involved classes:

<img src="https://drive.google.com/uc?export=&id=1sQ1qBX9tz4vdb2UOBEZvMQFZM-YonEAY"/>

When Spring starts context initialization, it reads ```spring.schemas``` and ```spring.handlers``` files to figure out 
where to find custom tag definition schema and handler class.

Then Spring instantiates handler class. The handler extends ```org.springframework.beans.factory.xml.NamespaceHandlerSupport```
and in its ```init()``` method we register handler for tag ```views```. In our case the code is very simple because we 
define only one custom tag.  

```java
...
public static final String VIEWS = "views";
...

registerBeanDefinitionParser(VIEWS, new ViewsConfigurationParser());

```

That's it. Now Spring knows which parser will process custom tag defined in the config. When config processing starts, 
for each tag entry, Spring invokes ```com.company.playground.views.scan.ViewsConfigurationParser#parse()``` method.

When the method is invoked it scans packages specified in tag attribute and gathers all view interface definitions. 
Please look at ```com.company.playground.views.scan.ViewsConfigurationParser#scanForViewInterfaces()``` method for details.

When all view interface definitions are gathered we create and register view interface repository as a Spring bean:
```java
BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ViewsConfiguration.class)
        .addConstructorArgValue(viewInterfaceDefinitions);
AbstractBeanDefinition viewsConfigurationBean = builder.getBeanDefinition();
registry.registerBeanDefinition(ViewsConfiguration.NAME, viewsConfigurationBean);
```
The registry - ```com.company.playground.views.scan.ViewsConfiguration``` now can be injected into both core services or 
UI controllers. In the second case you should specify custom tag in ```web``` module's ```web-spring.xml``` configuration.

```ViewsConfiguration``` stores all entity views and performes views substitution automatically after initialization 
in ```com.company.playground.views.scan.ViewsConfiguration#buildViewSubstitutionChain()```. We need views substitution to 
implement entity extension mechanism properly. 

Entity Views are ready for use.

## Entity Views Usage
 

 
 

