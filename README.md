#View Interfaces in CUBA framework - Proof of Concept

<a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>

##Introduction
The purpose of the project is to eliminate issues connected with detached entities when a developer tries to access an 
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

 
##Solution Description

The cornerstone of the solution is an interface 
```java
public interface BaseEntityView<T extends Entity> extends Entity, Serializable {

    T getOrigin();

    <V extends BaseEntityView<T>> Class<V> getInterfaceClass();

    <V extends BaseEntityView<T>> V transform(Class<? extends BaseEntityView<T>> targetView);

}
```  
If you need to create a view for an entity you need to extend the view stated above and add methods for exposing 
corresponding entity properties. 