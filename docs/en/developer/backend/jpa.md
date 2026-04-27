# Working with JPA

Our observations and instructions for JPA.

## Inheritance in JPA entities

In case two or more tables contain a large number of identical columns that we do not want to declare multiple times, we can use inheritance. An example is the table ```documents``` and ```documents_history```, which have most of the columns identical, but we want to work with them as two independent entities.

We will create a parent class in which we will store the common columns (we can name it basic, for example). This class must contain the ```@MappedSuperclass``` annotation (inheritance using the ```@Inheritance``` annotation is not suitable for this type of inheritance).

```java
@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
```

It is important that we do not use any Lombok annotations like ```@Getter / @Setter``` in the parent class, so all methods need to be written manually. JPA is not adapted for such inheritance of methods, which would cause the code to crash on the ```NoSuchMethodError (Ljava/lang/String;)V``` error. Classes that inherit from such a class with the ```@MappedSuperclass``` annotation do not need any other modifications or annotations except for extension. The methods of the parent class can be overridden.


```java
@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocDetails extends DocBasic {
```