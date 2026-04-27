# Spring framework

## EclipseLink JPA

First of all, it is important to note that for DB entity mapping we use (for historical reasons) the [EclipseLink JPA](https://www.eclipse.org/eclipselink/) framework. It implements the JPA standard and is fully integrated with Spring DATA.

As a basis for use, we recommend reading:

- [Baeldung: Defining JPA Entities](https://www.baeldung.com/jpa-entities)
- [JPA introduction](https://www.tutorialspoint.com/jpa/jpa_introduction.htm)
- [Introduction to the Java Persistence API](https://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html)

The following rules apply in WebJET:

- Attributes must be objects, i.e. Integer, Long (not primitive types int, long). This is due to the "By Example" search through repositories, where it is not possible to set NULL for primitive types.
- Primary key must be of type Long
- For autoincrement columns, the IDENTITY strategy can be used with a sequence name set for Oracle (typically S_table_name)
    ```Java
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
    private Long id;
    ```

- You may also encounter older usage of the PkeyGenerator class with annotations ```@GeneratedValue``` and ```@TableGenerator```, but this is deprecated in WebJET 2021 and should not be used.
- An example of a JPA entity is the class [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java).

Also note the annotations ```@Getter``` and ```@Setter```, they come from the [Lombook](https://projectlombok.org) project and ensure automatic generation of getter and setter methods.

## Spring DATA

[Spring DATA JPA](https://spring.io/projects/spring-data-jpa) simply provides a mapping between a SQL database and Java entities. It works in a "miraculous" way by creating empty methods, where according to the method name, Spring DATA itself creates the SQL conditions and implements the data retrieval.

As a basis for use, we recommend reading:

- [Introduction to Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Spring Data Annotations](https://www.baeldung.com/spring-data-annotations)
- Or the entire Baeldung tutorial [Spring Persistence Tutorial](https://www.baeldung.com/persistence-with-spring-series) where you can find many examples
- [JPA Repositories - Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

An example of a Spring DATA repository is the class [FormsRepository](../../../src/main/java/sk/iway/iwcm/components/forms/FormsRepository.java) which demonstrates multiple methods for searching by attributes.

### Code samples

Special update of a column in the database based on conditions and ```IN``` conditions. Both ```@Transactional``` and ```@Modifying``` annotations are required (since no records are returned).

```java
@Repository
public interface DocHistoryRepository extends JpaRepository<DocHistory, Long>, JpaSpecificationExecutor<DocHistory> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE DocHistory SET actual = :actual, awaitingApprove = :awaitingApprove, syncStatus = 1 WHERE id IN :historyIds")
    public void updateActualHistory(@Param("actual")boolean actual, @Param("awaitingApprove")String awaitingApprove, @Param("historyIds")List<Integer> historyIds);
}
```

## Spring REST

Using [Spring REST](https://spring.io/guides/tutorials/rest/) you can easily create REST services.

As a basis for use, we recommend reading:

- [Build a REST API with Spring and Java Config](https://www.baeldung.com/building-a-restful-web-service-with-spring-and-java-based-configuration#controller), just read from 5. The Controller
- Or the entire Baeldung tutorial [REST with Spring Tutorial](https://www.baeldung.com/rest-with-spring-series)

In WebJET for datatables, creating REST services is simplified and described in [separate documentation](../datatables/restcontroller.md). So you just need to extend the ```DatatableRestControllerV2``` class.

The most basic example is in the [RedirectRestController](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java) class where you will notice the ```@PreAuthorize``` annotation for rights control. The ```editItem``` and ```insertItem``` methods are implemented only because they set the current date and time to the last modified date column. If you do not need to do anything special, you do not need to implement these 2 methods. Everything else is done automatically in the Spring DATA repository and the DatatableRestControllerV2 superclass.