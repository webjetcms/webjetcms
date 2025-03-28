# Spring framework

## EclipseLink JPA

Na úvod je důležité upozornit, že k mapování DB entit používáme (z historických důvodů) framework [EclipseLink JPA](https://www.eclipse.org/eclipselink/). Ten implementuje standard JPA, je plně integrován se Spring DATA.

Jako základ použití doporučujeme přečíst:
- [Baeldung: Defining JPA Entities](https://www.baeldung.com/jpa-entities)
- [JPA introduction](https://www.tutorialspoint.com/jpa/jpa_introduction.htm)
- [Introduction to the Java Persistence API](https://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html)

Ve WebJETu platí následující pravidla:
- Atributy musí být objekty, tedy Integer, Long (ne primitivní typy int, long). Je to z důvodu vyhledávání "By Example" přes repozitáře, kde pro primitivní typy nelze nastavit NULL.
- Primární klíč musí být typu Long
- Pro autoincrement sloupce lze použít strategii IDENTITY s nastaveným jménem sekvence pro Oracle (typicky S\_jméno\_tabulky)

  ```Java
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
  private Long id;
  ```

- Můžete se setkat i se starším použitím zápisu přes třídu PkeyGenerator anotacemi `@GeneratedValue` a `@TableGenerator`, to už je ale ve WebJET 2021 deprecated a nemělo by se používat.
- Příkladem JPA entity je třída [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java).

Všimněte si také anotace `@Getter` a `@Setter`, ty pocházejí z projektu [Lombook](https://projectlombok.org) a zajišťují automatické generování getter a setter metod.

## Spring DATA

[Spring DATA JPA](https://spring.io/projects/spring-data-jpa) zjednodušeně poskytuje mapování mezi SQL databází a Java entitami. Funguje to „zázračným“ způsobem vytvořením prázdných metod, kde podle názvu metody Spring DATA samy vytvoří SQL podmínky a implementuje získání dat.

Jako základ použití doporučujeme přečíst:
- [Introduction to Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Spring Data Annotations](https://www.baeldung.com/spring-data-annotations)
- Respektive celý Baeldung tutoriál [Spring Persistence Tutorial](https://www.baeldung.com/persistence-with-spring-series) kde najdete mnoho příkladů
- [JPA Repositories - Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

Příkladem Spring DATA repozitáře je třída [FormsRepository](../../../src/main/java/sk/iway/iwcm/components/forms/FormsRepository.java) kde je ukázka více metod vyhledávání podle atributů.

### Ukázky kódu

Speciální aktualizace sloupce v databázi podle podmínek a `IN` podmínky. Potřebná je anotace `@Transactional` i `@Modifying` (jelikož se nevracejí žádné záznamy).

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

Pomocí [Spring REST](https://spring.io/guides/tutorials/rest/) můžete snadno vytvářet REST služby.

Jako základ použití doporučujeme přečíst:
- [Build a REST API with Spring and Java Config](https://www.baeldung.com/building-a-restful-web-service-with-spring-and-java-based-configuration#controller), stačí číst až od 5. The Controller
- Respektive celý Baeldung tituriál [REST with Spring Tutorial](https://www.baeldung.com/rest-with-spring-series)

Ve WebJETu pro datatabulky je tvorba REST služeb zjednodušená a popsaná v [samostatné dokumentaci](../datatables/restcontroller.md). Stačí vám tedy rozšířit třídu `DatatableRestControllerV2`.

Nejzákladnější příklad je ve třídě [RedirectRestController](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java) kde si všimnete anotace `@PreAuthorize` pro kontrolu práv. Metody `editItem` a `insertItem` jsou implementovány pouze z důvodu, že nastavují aktuální datum a čas do sloupce s datem poslední změny. Pokud nepotřebujete provádět nic speciálního ani tyto 2 metody nepotřebujete implementovat. Vše se jinak děje automaticky ve Spring DATA repozitáři a supertřídě DatatableRestControllerV2.
