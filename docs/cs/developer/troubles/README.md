# Řešení problémů

Zde naleznete možná řešení problémů, se kterými jsme se během vývoje setkali.

## java.lang.NoSuchFieldError: r$sfields

Pokud vám po stadru nefunguje SPRING (nezobrazí se přihlášení) a v logu najdete chybu typu:

```java
okt 14, 2020 10:39:19 AM org.apache.catalina.core.StandardContext loadOnStartup
SEVERE: Servlet [springDispatcher] in web application [] threw load() exception
java.lang.NoSuchFieldError: r$sfields
        at sk.iway.iwcm.system.audit.AuditNotifyRestController$$EnhancerBySpringCGLIB$$3177871d.<clinit>(<generated>)
        at java.lang.Class.forName0(Native Method)
        at java.lang.Class.forName(Class.java:348)
        at org.springframework.cglib.core.ReflectUtils.defineClass(ReflectUtils.java:537)
        at org.springframework.cglib.core.AbstractClassGenerator.generate(AbstractClassGenerator.java:359)
```

tak pravděpodobný problém je v použití stejného package pro spring třídu jako je již použit ve WebJET 8. Spring má nějaký problém s class loaderem třídy v classes adresáři a jaře archivu. Nepodařilo se nám najít jiné řešení, než použít pro WebJET 2021 jiný package.

## XXXrepository is not accessible

Pokud vám po startu nefunguje SPRING (nezobrazí se přihlášení) tak může být problém v tom, že repozitář není deklarován jako `public`:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
```

## Chyba Cannot find message resources pod key org.apache.struts.action.MESSAGE

Chyba se zobrazí, když na serveru máte v adresáři `/WEB-INF/lib/` knihovny `struts-core-1.3.8.jar` nebo `struts-taglib-1.3.8.jar`. Tyto knihovny jsou implementovány v `webjet-8.7-SNAPSHOT-struts.jar` a duplicita může způsobit nefunkčnost WebJETu.

## Zobrazení překladových klíčů

Pokud se vám zobrazují překladové klíče typu `components.monitoring.date_insert` místo textu typu **Datum vložení**, pravděpodobně nejsou definovány v překladovém souboru. Postupujte podle návodu na [definování překladových klíčů](../datatables-editor/datatable-columns.md#překlady-názvů-sloupců).

## Chyba NoSuchMethodError (Ljava/lang/String;)V

Chyba se může zobrazit pokud rodičovská třída, která obsahuje anotaci `@MappedSuperclass` má metody vygenerované pomocí Lombok anotací jako `@Data, @Getter alebo @Setter` vzhledem k tomu, že Lombok není uzpůsoben k dědění metod. V takovém případě je třeba odstranit Lombok anotace a všechny potřebné metody a konstruktory vygenerovat přímo do rodičovské třídy.

## StackOverflowError

Chyba se zobrazí při zacyklení kódu. Může to být způsobeno mapováním OneToMany a ManyToOne. Řešení při takové situaci je přidání JSON anotací z obou stran mapování.

```java
@JsonBackReference(value="")
```

Anotace `@JsonBackReference` se přidá na straně @ManyToOne, přičemž value je název dané proměnné.

```java
@JsonManagedReference(value="")
```

Anotace `@JsonManagedReference` se přidá na straně @OneToMany, přičemž value je název proměnné podle které mapujeme (název proměnné z ManyToOne strany).

## Padání na XssAttributeConverter

Pokud vám padá konverze na třídě `XssAttributeConverter` na konverzi např. `BigDecimal` a zdá se vám, že to je chyba `XssAttributeConverter` protože ten se má aplikovat jen na `String` objekt, tak si zkontrolujte v Java entitě daný atribut, zda náhodou není definován jako `String` a zkontrolujte si i typ v databázi, jestli ho náhodou nemáte jako `varchar`. Neboť vám se zdá, že to máte jako číslo, ale reálně někde máte atribut jako řetězec a tedy aplikuje se na něj `XssAttributeConverter`.

## Service/Data repozitář je NULL

Máte-li v `Controller` třídě `@Service` nebo Spring DATA repozitář a pro některé `@GetMapping` je najednou `NULL` ujistěte se, že metoda je nastavena jako `public`.

```java
public class FormsController extends DatatableRestControllerV2<FormsEntity, Long> {

    private final FormsService formsService;

    @Autowired
    public FormsController(FormsRepository formsRepository, FormsService formsService) {
        super(formsRepository);
        this.formsService = formsService;
    }

    @GetMapping(path = "/columns/{formName}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public FormColumns getColumnNames(@PathVariable String formName) {
        //ak by tato metoda nebola public, tak formsService bude NULL
        return formsService.getColumnNames(formName);
    }
```

## Problém s uložením entity

Pokud se při vytváření/úpravě entity setkáte s chybou `Content type 'application/json;charset=UTF-8' not supported` může to být způsobeno vnořenou tabulkou.

```java
    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "clicks",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/dmail/stat-clicks?campainId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.dmail.jpa.StatClicksEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit,duplicate"),
            }
        )
    })
    private List<StatClicksEntity> statClicksTab;
```

Chyba nastane pokud Entita vnořené tabulky (v tomto případě StatClicksEntity) obsahuje mapování `@OneToMany` nebo `@ManyToOne` a v tomto mapování je použita anotace `@JsonManagedReference`. Na první pohled pomůže použití anotace `@JsonIgnore` (přestane se zobrazovat chyba) ale nebude to fungovat.

Řešením je odstranění anotace `@JsonManagedReference`.
