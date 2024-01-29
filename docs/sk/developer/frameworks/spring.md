# Spring framework

## EclipseLink JPA

Na úvod je dôležité upozorniť, že na mapovanie DB entít používame (z historických dôvodov) framework [EclipseLink JPA](https://www.eclipse.org/eclipselink/). Ten implementuje štandard JPA, je plne integrovaný so Spring DATA.

Ako základ použitia odporúčame prečítať:

- [Baeldung: Defining JPA Entities](https://www.baeldung.com/jpa-entities)
- [JPA introduction](https://www.tutorialspoint.com/jpa/jpa_introduction.htm)
- [Introduction to the Java Persistence API](https://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html)

Vo WebJETe platia nasledovné pravidlá:

- Atribúty musia byť objekty, čiže Integer, Long (nie primitívne typy int, long). Je to z dôvodu vyhľadávania "By Example" cez repozitáre, kde pre primitívne typy nie je možné nastaviť NULL.
- Primárny kľúč musí byť typu Long
- Pre autoincrement stĺpce je možné použiť stratégiu IDENTITY s nastaveným menom sekvencie pre Oracle (typicky S_meno_tabulky)
    ```Java
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
    private Long id;
    ```
- Môžete sa stretnúť aj so starším použitím zápisu cez triedu PkeyGenerator anotáciami ```@GeneratedValue``` a ```@TableGenerator```, to už je ale vo WebJET 2021 deprecated a nemalo by sa používať.
- Príkladom JPA entity je trieda [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java).

Všimnite si aj anotácie ```@Getter``` a ```@Setter```, tie pochádzajú z projektu [Lombook](https://projectlombok.org) a zabezpečujú automatické generovanie getter a setter metód.

## Spring DATA

[Spring DATA JPA](https://spring.io/projects/spring-data-jpa) zjednodušene poskytuje mapovanie medzi SQL databázou a Java entitami. Funguje to "zázračným" spôsobom vytvorením prázdnych metód, kde podľa názvu metódy Spring DATA samé vytvorí SQL podmienky a implementuje získanie dát.

Ako základ použitia odporúčame prečítať:

- [Introduction to Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Spring Data Annotations](https://www.baeldung.com/spring-data-annotations)
- Respektíve celý Baeldung tutoriál [Spring Persistence Tutorial](https://www.baeldung.com/persistence-with-spring-series) kde nájdete veľa príkladov
- [JPA Repositories - Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

Príkladom Spring DATA repozitára je trieda [FormsRepository](../../../src/main/java/sk/iway/iwcm/components/forms/FormsRepository.java) kde je ukážka viacerých metód vyhľadávania podľa atribútov.

### Ukážky kódu

Špeciálna aktualizácia stĺpca v databáze podľa podmienok a ```IN``` podmienky. Potrebná je anotácia ```@Transactional``` aj ```@Modifying``` (keďže sa nevracajú žiadne záznamy).

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

Pomocou [Spring REST](https://spring.io/guides/tutorials/rest/) môžete ľahko vytvárať REST služby.

Ako základ použitia odporúčame prečítať:

- [Build a REST API with Spring and Java Config](https://www.baeldung.com/building-a-restful-web-service-with-spring-and-java-based-configuration#controller), stačí čítať až od 5. The Controller
- Respektíve celý Baeldung tituriál [REST with Spring Tutorial](https://www.baeldung.com/rest-with-spring-series)

Vo WebJETe pre datatabuľky je tvorba REST služieb zjednodušená a opísaná v [samostatnej dokumentácii](../datatables/restcontroller.md). Stačí vám teda rozšíriť triedu ```DatatableRestControllerV2```.

Najzákladnejší príklad je v triede [RedirectRestController](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java) kde si všimnete anotáciu ```@PreAuthorize``` pre kontrolu práv. Metódy ```editItem``` a ```insertItem``` sú implementované len z dôvodu, že nastavujú aktuálny dátum a čas do stĺpca s dátumom poslednej zmeny. Ak nepotrebujete vykonávať nič špeciálne ani tieto 2 metódy nepotrebujete implementovať. Všetko sa inak udeje automaticky v Spring DATA repozitári a supertriede DatatableRestControllerV2.