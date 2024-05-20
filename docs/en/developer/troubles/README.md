# Problem solving

Here you will find possible solutions to the problems we encountered during development.

## java.lang.NoSuchFieldError: r$sfields

If SPRING does not work after the boot (login does not appear) and you find an error in the log like:

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

so the likely problem is in using the same package for the spring class as is already used in WebJET 8. Spring has some problem with the class loader in the classes directory and the spring archive. We could not find a solution other than using a different package for WebJET 2021.

## XXXrepository is not accessible

If SPRING doesn't work after boot (no login), the problem may be that the repository is not declared as `public`:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
```

## Error Cannot find message resources under key org.apache.struts.action.MESSAGE

The error appears when you have a directory on the server `/WEB-INF/lib/` Libraries `struts-core-1.3.8.jar` or `struts-taglib-1.3.8.jar`. These libraries are implemented in `webjet-8.7-SNAPSHOT-struts.jar` and duplication can cause WebJET to become inoperable.

## Display translation keys

If you are seeing translation keys of type `components.monitoring.date_insert` instead of text like **Date of insertion**, probably not defined in the translation file. Follow the instructions on [defining translation keys](../datatables-editor/datatable-columns.md#preklady-názvov-stĺpcov).

## Error NoSuchMethodError (Ljava/lang/String;)V

An error may appear if a parent class that contains an annotation `@MappedSuperclass` has methods generated using Lombok annotations as `@Data, @Getter alebo @Setter`as Lombok is not adapted to inheritance methods. In this case, you need to remove the Lombok annotations and generate all the necessary methods and constructors directly in the parent class.

## StackOverflowError

The error is displayed when the code is looped. This may be caused by the mapping of OneToMany and ManyToOne. The workaround for this situation is to add JSON annotations from both sides of the mapping.

```java
@JsonBackReference(value="")
```

Annotation `@JsonBackReference` is added on the @ManyToOne side, where value is the name of the variable.

```java
@JsonManagedReference(value="")
```

Annotation `@JsonManagedReference` is added on the @OneToMany side, where value is the name of the variable we are mapping by (the name of the variable from the ManyToOne side).

## Crashing on XssAttributeConverter

If you are failing a conversion on a class `XssAttributeConverter` on the conversion e.g. `BigDecimal` and it seems to you that this is a mistake `XssAttributeConverter` because the latter is to be applied only to `String` object, then check the Java entity for the attribute to see if it is defined as `String` and check the type in the database to see if you have it as `varchar`. Because it seems to you that you have it as a number, but actually somewhere you have an attribute as a string and thus it is applied to it `XssAttributeConverter`.

## Service/Data repository is NULL

If you have in `Controller` Classroom `@Service` or Spring DATA repository and for some `@GetMapping` is suddenly `NULL` make sure the method is set as `public`.

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

## Problem with entity saving

If you encounter an error when creating/modifying an entity `Content type 'application/json;charset=UTF-8' not supported`

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

`@OneToMany` or `@ManyToOne``@JsonManagedReference``@JsonIgnore`

`@JsonManagedReference`.
