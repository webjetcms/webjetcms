# Řešení problémů

Zde najdete možná řešení problémů, na které jsme během vývoje narazili.

## java.lang.NoSuchFieldError: r$sfields

Pokud SPRING po spuštění nefunguje (nezobrazí se přihlášení) a v protokolu najdete chybu typu:

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

takže pravděpodobný problém spočívá v použití stejného balíčku pro třídu spring, jaký se již používá ve WebJET 8. Spring má nějaký problém se zavaděčem tříd v adresáři classes a archivem spring. Nenašli jsme jiné řešení než použití jiného balíčku pro WebJET 2021.

## XXXrepository is not accessible

Pokud SPRING po spuštění nefunguje (nedojde k přihlášení), může být problém v tom, že úložiště není deklarováno jako `public`:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
```

## Chyba Cannot find message resources under key org.apache.struts.action.MESSAGE

Chyba se objeví, když máte na serveru adresář. `/WEB-INF/lib/` Knihovny `struts-core-1.3.8.jar` nebo `struts-taglib-1.3.8.jar`. Tyto knihovny jsou implementovány v `webjet-8.7-SNAPSHOT-struts.jar` a duplikace může způsobit nefunkčnost systému WebJET.

## Zobrazení překladových klíčů

Pokud se zobrazují překladové klíče typu `components.monitoring.date_insert` místo textu jako **Datum vložení**, pravděpodobně není definován v souboru překladu. Postupujte podle pokynů na [definování překladových klíčů](../datatables-editor/datatable-columns.md#překlady-nadpisů-sloupců).

## Chyba NoSuchMethodError (Ljava/lang/String;)V

Chyba se může objevit, pokud nadřazená třída obsahující anotaci `@MappedSuperclass` má metody generované pomocí anotací Lombok jako `@Data, @Getter alebo @Setter` protože Lombok není přizpůsoben metodám dědičnosti. V takovém případě je třeba odstranit anotace Lomboku a vygenerovat všechny potřebné metody a konstruktory přímo v nadřazené třídě.

## StackOverflowError

Chyba se zobrazí, když se kód zacyklí. Příčinou může být mapování OneToMany a ManyToOne. Řešením této situace je přidání anotací JSON z obou stran mapování.

```java
@JsonBackReference(value="")
```

Anotace `@JsonBackReference` se přidá na stranu @ManyToOne, kde value je název proměnné.

```java
@JsonManagedReference(value="")
```

Anotace `@JsonManagedReference` se přidá na stranu @OneToMany, kde value je název proměnné, podle které mapujeme (název proměnné ze strany ManyToOne).

## Pád při použití XssAttributeConverter

Pokud se vám nepodaří provést konverzi ve třídě. `XssAttributeConverter` při převodu např. `BigDecimal` a zdá se vám, že je to chyba. `XssAttributeConverter` protože ten se použije pouze na `String` pak zkontrolujte, zda je atribut v entitě Java definován jako `String` a zkontrolujte typ v databázi, zda je zadán jako `varchar`. Protože se vám zdá, že to máte jako číslo, ale ve skutečnosti máte někde atribut jako řetězec, a proto se na něj aplikuje. `XssAttributeConverter`.

## Service/Data repozitář je NULL

Pokud máte v `Controller` Třída `@Service` nebo úložiště Spring DATA a pro některé `@GetMapping` je náhle `NULL` zkontrolujte, zda je metoda nastavena jako `public`.

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

## Problém s ukládáním entit

Pokud při vytváření/úpravě entity narazíte na chybu. `Content type 'application/json;charset=UTF-8' not supported` to může být způsobeno vnořenou tabulkou.

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

K chybě dojde, pokud entita vnořené tabulky (v tomto případě StatClicksEntity) obsahuje mapování `@OneToMany` nebo `@ManyToOne` a v tomto mapování se používá anotace `@JsonManagedReference`. Na první pohled pomáhá používat anotace `@JsonIgnore` (přestane se zobrazovat chyba), ale nebude to fungovat.

Řešením je odstranění anotace `@JsonManagedReference`.
