# Working with JPA

Our insights and guidance on the JPA.

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

## Succession in JPA entities

If two or more tables contain a larger number of identical columns that we do not want to declare multiple times, we can use Inheritance. An example is a table `documents` a `documents_history`that have most of the columns identical, but we want to work with them as two independent entities.

We will create a parent class in which we will store the common columns (we can name it basic, for example). This class must contain an annotation `@MappedSuperclass` (inheritance by annotation `@Inheritance` is not suitable for this type of inheritance).

```java
@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
```

It is important that we do not use any Lombok annotations in the parenting class as `@Getter / @Setter` so all methods need to be written manually. JPA is not adapted for such method inheritance which would cause code crash on error `NoSuchMethodError (Ljava/lang/String;)V`. Classes that inherit from such a class with annotation `@MappedSuperclass` do not need any editing or annotation other than expansion. Parent class methods can be overridden.

```java
@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocDetails extends DocBasic {
```
