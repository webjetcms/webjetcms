# Troubleshooting

Here you will find possible solutions to problems we encountered during development.

## java.lang.NoSuchFieldError: r$sfields

If SPRING does not work for you (login does not appear) and you find an error like this in the log:

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

so the likely problem is using the same package for the spring class as is already used in WebJET 8. Spring has some problem with the class loader of the class in the classes directory and the jar archive. We were unable to find a solution other than using a different package for WebJET 2021.

## XXXrepository is not accessible

If SPRING doesn't work after starting (login is not displayed), the problem may be that the repository is not declared as ```public```:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetailsEntity, Long> {
```

## Error Cannot find message resources under key org.apache.struts.action.MESSAGE

The error appears when you have libraries ```struts-core-1.3.8.jar``` or ```struts-taglib-1.3.8.jar``` in the ```/WEB-INF/lib/``` directory on your server. These libraries are implemented in ```webjet-8.7-SNAPSHOT-struts.jar``` and duplication can cause WebJET to not work.

## View translation keys

If you see translation keys of the type ```components.monitoring.date_insert``` instead of text of the type **Date inserted**, they are probably not defined in the translation file. Follow the instructions for [defining translation keys](../datatables-editor/datatable-columns.md#translations-of-column-names).

## NoSuchMethodError (Ljava/lang/String;)In error

The error may appear if the parent class that contains the ```@MappedSuperclass``` annotation has methods generated using Lombok annotations like ```@Data, @Getter alebo @Setter```, since Lombok is not designed for method inheritance. In this case, it is necessary to remove the Lombok annotations and generate all necessary methods and constructors directly into the parent class.

## StackOverflowError

The error appears when the code loops. This can be caused by OneToMany and ManyToOne mappings. The solution to this situation is to add JSON annotations on both sides of the mapping.

```java
@JsonBackReference(value="")
```

The annotation ```@JsonBackReference``` is added on the @ManyToOne side, where value is the name of the given variable.

```java
@JsonManagedReference(value="")
```

The annotation ```@JsonManagedReference``` is added on the @OneToMany side, where value is the name of the variable we are mapping against (the name of the variable from the ManyToOne side).

## Crashing on XssAttributeConverter

If you are getting a conversion error on class ```XssAttributeConverter``` on conversion e.g. ```BigDecimal``` and it seems to you that it is an error ```XssAttributeConverter``` because it should only be applied to the ```String``` object, then check the attribute in the Java entity to see if it is defined as ```String``` and also check the type in the database to see if it is not defined as ```varchar```. Because it seems to you that you have it as a number, but in reality you have the attribute as a string somewhere and therefore ```XssAttributeConverter``` is applied to it.

## Service/Data repository is NULL

If you have a ```Controller``` class in ```@Service``` or a Spring DATA repository and for some ```@GetMapping``` it is suddenly ```NULL```, make sure the method is set as ```public```.

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

## Problem saving entity

If you encounter the ```Content type 'application/json;charset=UTF-8' not supported``` error when creating/editing an entity, it may be caused by a nested table.

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

The error occurs if the Entity of the nested table (in this case StatClicksEntity) contains the mapping ```@OneToMany``` or ```@ManyToOne``` and the annotation ```@JsonManagedReference``` is used in this mapping. At first glance, using the annotation ```@JsonIgnore``` will help (the error will stop appearing), but it will not work.

The solution is to remove the ```@JsonManagedReference``` annotation. 