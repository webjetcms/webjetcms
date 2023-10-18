# Riešenie problémov

Tu nájdete možné riešenia problémov, s ktorými sme sa počas vývoja stretli.

## java.lang.NoSuchFieldError: r$sfields

Ak vám po štadre nefunguje SPRING (nezobrazí sa prihlásenie) a v logu nájdete chybu typu:

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

tak pravdepodobný problém je v použití rovnakého package pre spring triedu ako je už použitý vo WebJET 8. Spring má nejaký problém s class loaderom triedy v classes adresári a jar archíve. Nepodarilo sa nám nájsť iné riešenie ako použiť pre WebJET 2021 iný package.

## XXXrepository is not accessible

Ak vám po štarte nefunguje SPRING (nezobrazí sa prihlásenie) tak môže byť problém v tom, že repozitár nie je deklarovaný ako ```public```:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
```

## Chyba Cannot find message resources under key org.apache.struts.action.MESSAGE

Chyba sa zobrazí, keď na serveri máte v adresári ```/WEB-INF/lib/``` knižnice ```struts-core-1.3.8.jar``` alebo ```struts-taglib-1.3.8.jar```. Tieto knižnice sú implementované v ```webjet-8.7-SNAPSHOT-struts.jar``` a duplicita môže spôsobiť nefunkčnosť WebJETu.

## Zobrazenie prekladových kľúčov

Ak sa vám zobrazujú prekladové kľúče typu ```components.monitoring.date_insert``` namiesto textu typu **Dátum vloženia**, pravdepodobne nie sú definované v prekladovom súbore. Postupujte podľa návodu na [definovanie prekladových kľúčov](../datatables-editor/datatable-columns.md#preklady-názvov-stĺpcov).

## Chyba NoSuchMethodError (Ljava/lang/String;)V

Chyba sa môže zobraziť ak rodičovská trieda, ktorá obsahuje anotáciu ```@MappedSuperclass``` má metódy vygenerované pomocou Lombok anotácií ako ```@Data, @Getter alebo @Setter```, keďže Lombok nie je prispôsobený na dedenie metód. V takom prípade je potrebné odstrániť Lombok anotácie a všetky potrebné metódy a konštruktory vygenerovať priamo do rodičovskej triedy.

## StackOverflowError

Chyba sa zobrazí pri zacyklení kódu. Môže to byť spôsobené mapovaním OneToMany a ManyToOne. Riešenie pri takejto situácií je pridanie JSON anotácií z oboch strán mapovania.

```java
@JsonBackReference(value="")
```

Anotácia ```@JsonBackReference``` sa pridá na strane @ManyToOne, pričom value je názov danej premennej.

```java
@JsonManagedReference(value="")
```

Anotácia ```@JsonManagedReference``` sa pridá na strane @OneToMany, pričom value je názov premennej podľa ktorej mapujeme (názov premennej z ManyToOne strany).

## Padanie na XssAttributeConverter

Ak vám padá konverzia na triede ```XssAttributeConverter``` na konverzii napr. ```BigDecimal``` a zdá sa vám, že to je chyba ```XssAttributeConverter``` pretože ten sa má aplikovať len na ```String``` objekt, tak si skontrolujte v Java entite daný atribút, či náhodou nie je definovaný ako ```String``` a skontrolujte si aj typ v databáze, či ho náhodou nemáte ako ```varchar```. Lebo vám sa zdá, že to máte ako číslo, ale reálne niekde máte atribút ako reťazec a teda aplikuje sa naň ```XssAttributeConverter```.

## Service/Data repozitár je NULL

Ak máte v ```Controller``` triede ```@Service``` alebo Spring DATA repozitár a pre niektoré ```@GetMapping``` je zrazu ```NULL``` uistite sa, že metóda je nastavená ako ```public```.

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

Ak sa pri vytváraní/úprave entity stretnete s chybou ```Content type 'application/json;charset=UTF-8' not supported``` môže to byť spôsobené vnorenou tabuľkou.

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

Chyba nastane ak Entita vnorenej tabuľky (v tomto prípade StatClicksEntity) obsahuje mapovanie ```@OneToMany``` alebo ```@ManyToOne``` a v tomto mapovaní je použitá anotácia ```@JsonManagedReference```. Na prvý pohľad pomôže použitie anotácie ```@JsonIgnore``` (prestane sa zobrazovať chyba) ale nebude to fungovať.

Riešením je odstránenie anotácie  ```@JsonManagedReference```. 