# Spring framework

## EclipseLink JPA

At the outset, it is important to note that we use (for historical reasons) the framework for mapping DB entities [EclipseLink JPA](https://www.eclipse.org/eclipselink/). It implements the JPA standard and is fully integrated with Spring DATA.

As a basis for use, we recommend reading:
- [Baeldung: Defining JPA Entities](https://www.baeldung.com/jpa-entities)
- [JPA introduction](https://www.tutorialspoint.com/jpa/jpa_introduction.htm)
- [Introduction to the Java Persistence API](https://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html)

The following rules apply in WebJET:
- Attributes must be objects, i.e. Integer, Long (not primitive types int, long). This is because of "By Example" lookups through repositories, where it is not possible to set NULL for primitive types.
- The primary key must be of type Long
- For autoincrement columns, the IDENTITY strategy can be used with the sequence name set for the oracle (typically S\_meno\_tables)

  ```Java
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
  private Long id;
  ```

- You may also encounter the older use of writing via the PkeyGenerator class annotations `@GeneratedValue` a `@TableGenerator`, but this is already deprecated in WebJET 2021 and should not be used.
- An example of a JPA entity is a class [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java).

Note also the annotations `@Getter` a `@Setter`, these come from the project [Lombook](https://projectlombok.org) and provide automatic generation of getter and setter methods.

## Spring DATA

[Spring DATA JPA](https://spring.io/projects/spring-data-jpa) provides a simplified mapping between SQL database and Java entities. It works in a "magical" way by creating empty methods, where by the method name Spring DATA itself creates the SQL conditions and implements the data retrieval.

As a basis for use, we recommend reading:
- [Introduction to Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Spring Data Annotations](https://www.baeldung.com/spring-data-annotations)
- The whole Baeldung tutorial [Spring Persistence Tutorial](https://www.baeldung.com/persistence-with-spring-series) where you will find many examples
- [JPA Repositories - Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

An example of a Spring DATA repository is the class [FormsRepository](../../../src/main/java/sk/iway/iwcm/components/forms/FormsRepository.java) where there is an example of several methods of searching by attributes.

### Code samples

Special update of a column in the database according to conditions and `IN` conditions. Annotation is required `@Transactional` Also `@Modifying` (as no records are returned).

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
- [Build a REST API with Spring and Java Config](https://www.baeldung.com/building-a-restful-web-service-with-spring-and-java-based-configuration#controller), just read up from 5. The Controller
- Or rather the whole Baeldung titular [REST with Spring Tutorial](https://www.baeldung.com/rest-with-spring-series)

In WebJET for datatables the creation of REST services is simplified and described in [separate documentation](../datatables/restcontroller.md). So you just need to extend the class `DatatableRestControllerV2`.

The most basic example is in the class [RedirectRestController](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java) where you will notice the annotation `@PreAuthorize` for the control of rights. Methods `editItem` a `insertItem` are implemented only because they set the current date and time in the last modified date column. If you don't need to do anything special you don't need to implement these 2 methods either. Everything else is done automatically in the Spring DATA repository and the DatatableRestControllerV2 superclass.
