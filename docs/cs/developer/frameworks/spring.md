# Spring framework

## EclipseLink JPA

Na začátku je důležité poznamenat, že (z historických důvodů) používáme rámec pro mapování entit DB [EclipseLink JPA](https://www.eclipse.org/eclipselink/). Implementuje standard JPA a je plně integrován se Spring DATA.

Jako základ pro použití doporučujeme přečíst si:
- [Baeldung: Definování entit JPA](https://www.baeldung.com/jpa-entities)
- [Úvod do JPA](https://www.tutorialspoint.com/jpa/jpa_introduction.htm)
- [Úvod do rozhraní Java Persistence API](https://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html)

V systému WebJET platí následující pravidla:
- Atributy musí být objekty, tj. Integer, Long (nikoli primitivní typy int, long). Důvodem je vyhledávání "Podle příkladu" prostřednictvím úložišť, kde není možné nastavit NULL pro primitivní typy.
- Primární klíč musí být typu Long
- Pro sloupce s automatickým navýšením lze použít strategii IDENTITY s nastaveným názvem sekvence pro Oracle (typicky S\_meno\_tables).

  ```Java
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_quiz_questions")
  private Long id;
  ```

- Můžete se také setkat se starším použitím zápisu pomocí anotací třídy PkeyGenerator `@GeneratedValue` a `@TableGenerator`, která je však ve WebJET 2021 již zastaralá a neměla by se používat.
- Příkladem entity JPA je třída [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java).

Všimněte si také poznámek `@Getter` a `@Setter`, které pocházejí z projektu [Lombook](https://projectlombok.org) a zajistit automatické generování metod getter a setter.

## Spring DATA

[Spring DATA JPA](https://spring.io/projects/spring-data-jpa) poskytuje zjednodušené mapování mezi databází SQL a entitami jazyka Java. Funguje "kouzelným" způsobem vytváření prázdných metod, kde podle názvu metody Spring DATA sám vytvoří podmínky SQL a realizuje načtení dat.

Jako základ pro použití doporučujeme přečíst si:
- [Úvod do Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Spring Data Annotations](https://www.baeldung.com/spring-data-annotations)
- Celý výukový program Baeldung [Výukový kurz perzistence jara](https://www.baeldung.com/persistence-with-spring-series) kde najdete mnoho příkladů
- [Úložiště JPA - Vytváření dotazů](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

Příkladem úložiště Spring DATA je třída [FormsRepository](../../../src/main/java/sk/iway/iwcm/components/forms/FormsRepository.java) kde je uveden příklad několika metod vyhledávání podle atributů.

### Ukázky kódu

Speciální aktualizace sloupce v databázi podle podmínek a `IN` podmínky. Je vyžadována anotace `@Transactional` Také `@Modifying` (protože nejsou vráceny žádné záznamy).

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

Používání stránek [Spring REST](https://spring.io/guides/tutorials/rest/) můžete snadno vytvářet služby REST.

Jako základ pro použití doporučujeme přečíst si:
- [Vytvoření rozhraní REST API pomocí Spring a Java Config](https://www.baeldung.com/building-a-restful-web-service-with-spring-and-java-based-configuration#controller), stačí si přečíst od 5. Kontrolor
- Nebo spíše celý titulní Baeldung [Výukový program REST s jarem](https://www.baeldung.com/rest-with-spring-series)

Ve WebJET pro datové tabulky je vytváření služeb REST zjednodušeno a popsáno v části [samostatná dokumentace](../datatables/restcontroller.md). Stačí tedy rozšířit třídu `DatatableRestControllerV2`.

Nejzákladnější příklad je ve třídě [RedirectRestController](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java) kde si všimnete poznámky `@PreAuthorize` pro kontrolu práv. Metody `editItem` a `insertItem` jsou implementovány pouze proto, že ve sloupci datum poslední změny nastavují aktuální datum a čas. Pokud nepotřebujete dělat nic zvláštního, nemusíte implementovat ani tyto 2 metody. Vše ostatní se provádí automaticky v úložišti Spring DATA a v nadtřídě DatatableRestControllerV2.
