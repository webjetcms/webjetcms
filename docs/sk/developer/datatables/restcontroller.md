# DatatableRestControllerV2.java

Java trieda [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) zapúzdruje komunikáciu s [datatabuľkou](README.md) a [editorom](../datatables-editor/README.md).

## Základná implementácia

Pri implementácii špecifickej REST služby je potrebné rozšíriť DatatableRestControllerV2. V **ideálnom prípade použitia Spring DATA repozitárov** môže implementačná Java trieda vyzerať nasledovne:

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectRestController extends DatatableRestControllerV2<RedirectBean, Long> {

    @Autowired
    public RedirectRestController(RedirectsRepository redirectsRepository) {
        super(redirectsRepository, RedirectBean.class);
    }
}
```

dôležitý je konštruktor, ktorý odovzdáva Spring DATA repozitár ```RedirectsRepository``` a triedu entity, ktorá sa použije pri vytvorení nového záznamu. Nastavia sa tak aj hodnoty pri vytvorení nového záznamu. Anotácia ```@Datatable``` zabezpečí korektné spracovanie chybových správ a anotácia ```PreAuthorize``` slúži na kontrolu práv:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Upozornenie:** anotácia `@PreAuthorize` na triede kontroléra chráni iba metódy deklarované v tejto triede. Pri metóde zdedenej z bázovej triedy Spring Security hľadá anotáciu od triedy, ktorá metódu deklaruje, smerom k jej predkom, nie v podtriede. Ak pridávate REST metódu (`@GetMapping`, `@PostMapping`…) do bázovej triedy, ktorú dedia kontroléry s rôznymi právami, dajte jej anotáciu priamo na metódu. Rovnako to robia metódy v `DatatableRestControllerV2`:

```java
@PreAuthorize(value = "@WebjetSecurityService.checkAccessAllowedOnController(this)")
@PostMapping("/preview/")
public String preview(@RequestBody DocDetails entity, HttpServletRequest request) {
```

Metóda `checkAccessAllowedOnController` overí právo z anotácie `@PreAuthorize` na triede konkrétneho kontroléra a jeho metódu `checkAccessAllowed`. Ak kontrolér nemá ani jedno, povolí prístup každému administrátorovi. Každý kontrolér odvodený od `DatatableRestControllerV2` preto potrebuje `@PreAuthorize` na triede, a to aj vtedy, keď ho používa iba vnorená tabuľka na stránke chránenej iným kontrolérom. Druhou možnosťou je vlastná metóda `checkAccessAllowed`. Test `InheritedHandlerPreAuthorizeTest` nájde kontroléry bez práva aj metódy kontrolérov s anotáciou na triede, ktoré zostali bez kontroly práv.

!>**Upozornenie:** Spring DATA entita musí mať ako PK ```Long id``` (nie napr. ```adminlogId``` a podobne), ak sa stĺpec v databáze volá inak, je potrebné mu nastaviť meno stĺpca:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Upozornenie:** v dátovej entite **NESMIETE používať primitívne typy** ako ```int```, ```long``` ale objekty ```Integer```, ```Long```, inak nebude fungovať vyhľadávanie. To používa ExampleMatcher, ktorý NULL objekty nevloží do DB dotazu. Pre primitívne typy ale nemôže použiť NULL, nastaví ich na hodnotu 0 a následne pridá do ```WHERE``` podmienky.

Komplexná ukážka aj s kontrolou práv, mazaním súborov a vykonaním špeciálnej akcie:

```java
package sk.iway.iwcm.components.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.upload.UploadService;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GalleryRestController
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuGallery')")
public class GalleryRestController extends DatatableRestControllerV2<GalleryEntity, Long> {

    private final HttpServletRequest request;

    @Autowired
    public GalleryRestController(GalleryRepository galleryRepository, HttpServletRequest request) {
        super(galleryRepository);
        this.request = request;
    }

    /**
     * Metoda na kontrolu prav pouzivatela
     *
     * @param request
     * @return
     */
    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        Identity currentUser = UsersDB.getCurrentUser(request);
        return currentUser.isEnabledItem("menuGallery");
    }


    /**
     * Prikladova metoda na validaciu dat odoslanych z datatables editora.
     * Metoda je volana pre kazdy odoslaby objekt.
     * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
     *
     * @param request
     * @param user
     * @param errors
     * @param id
     * @param entity
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryEntity> target, Identity user, Errors errors, Long id, GalleryEntity entity) {
        if (!user.isFolderWritable(entity.getImagePath())) {
            //objekt errorField je nase T, v tomto pripade GalleryEntity, setuje sa presna property kde sa ma error vypisat
            errors.rejectValue("errorField.imagePath", null, Prop.getInstance(request).getText("user.rights.no_folder_rights"));
            //pre globalnu chybu (netykajucu sa konkretneho fieldu) je mozne pouzit kod:
            //((BindingResult)errors).addError(new ObjectError("global", Prop.getInstance(request).getText("datatable.error.fieldErrorMessage")));
        }
    }

    /**
     * Vykonanie akcie otocenia obrazka
     */
    @Override
    public boolean processAction(GalleryEntity entity, String action) {
        String imageUrl = entity.getImagePath() + "/" + entity.getImageName();
        int status = -1;

        if ("rotate".equals(action)) {
            status = ImageTools.rotateImage(imageUrl, 90);
        }

        return status == 0;
    }

    /**
     * Pri zmazani entity z DB je potrebne zmazat aj subory z disku
     */
    public boolean beforeDelete(GalleryEntity entity) {
        UploadService uploadService = new UploadService(entity.getImagePath() + "/" + entity.getImageName(), request);
        return uploadService.processDelete();
    }
}
```

## AI_TOOLS

Anotáciou `@AiTool` sprístupníte REST operácie konkrétneho kontroléra cez katalóg AI nástrojov. Katalóg preberie zo Spring MVC zdedené aj vlastné mapovania. Metódy ako `afterSave` zostávajú súčasťou životného cyklu editora.

Kontrolér `ConfigurationController` sprístupňuje konfiguračné premenné používateľovi s právom `menuConfig`. Pri každom volaní zachováva obmedzenia prístupu ku konkrétnym premenným.

Kontroléry `WebpagesRestController` a `GroupsRestController` sprístupňujú správu webových stránok a priečinkov používateľovi s právom `menuWebpages`. Asistentovi ich priradíte pomocou `@group(sk.iway.iwcm.editor.rest.WebpagesRestController)` a `@group(sk.iway.iwcm.editor.rest.GroupsRestController)`. Nástroje používajú existujúce REST operácie vrátane kontrol oprávnení a ukladania cez editor.

Ak stačí overiť existenciu stránky alebo priečinka, priraďte asistentovi `@tool(webpages_exists)` alebo `@tool(groups_exists)`. Oba nástroje sú aj súčasťou príslušnej skupiny kontroléra. Volanie s argumentom `{"id":42}` vráti iba `true`, ak má používateľ k záznamu prístup. Pri chýbajúcom či neprístupnom zázname alebo nekladnom ID vráti `false`.

Pri vytváraní stránky nástroj `webpages_add` vyžaduje iba cieľový `groupId` a `body.title`. WebJET načíta predvolené nastavenia priečinka, doplní voliteľné `body.data`, `body.navbar` a `body.publish` podľa požiadavky a uloží stránku cez editor. Odpoveď obsahuje ID stránky (`id`), stav publikovania (`publicationStatus`) a pri uložení verzie do histórie aj `historyId`.

Pri vyhľadávaní podľa šablóny (`tempId`) alebo skupiny používateľov (`userGroupId`) vráti `groups_get_all` iba priečinky, ktoré môže používateľ upravovať v aktuálnej doméne. Ak zadáte aj `groupId`, zoznam obsahuje len priame podpriečinky vybraného priečinka.

Pred úpravou existujúceho záznamu načítajte úplný záznam pomocou `webpages_get_one` alebo `groups_get_one` a zachovajte jeho `editorFields`. Zoznam stránok nemusí obsahovať HTML. Katalóg vynecháva súčty a presúvanie riadkov, pri priečinkoch aj generické vyhľadávanie podľa stĺpcov, ktoré vyžaduje JPA repozitár.

```java
import sk.iway.iwcm.components.ai.tools.AiTool;

@RestController
@RequestMapping("/admin/rest/settings/configuration")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuConfig')")
@Datatable(clientSidePaging = true)
@AiTool(description = "components.ai.tools.configuration.context",
    exclude = {"findByColumns", "action", "getSum", "rowReorder"})
public class ConfigurationController extends DatatableRestControllerV2<ConfDetailsDto, Long> {
    // ...
}
```

Samotné `@AiTool` zahrnie aj zápisové operácie. Voľba `readOnly = true` obmedzí výpis aj volanie na HTTP GET. Pomocou `exclude = {"findByColumns"}` môžete v oboch režimoch vyradiť všetky mapovania konkrétnej Java metódy, vrátane zdedenej metódy. Pri vlastných mapovaniach bez určenej HTTP metódy katalóg použije GET, v plnom režime pri vstupe s telom požiadavky použije POST.

Ak kontrolér vracia celý zoznam a nepodporuje parametre `page` a `size`, nastavte `@Datatable(clientSidePaging = true)`. Takto sú označené kontroléry konfigurácie a priečinkov, ktorých frontend používa `serverSide: false`. Predvolená hodnota je `false` pre serverové stránkovanie.

Pri metódach s parametrom `Pageable` katalóg vynechá `page` a `size` zo vstupnej schémy aj príkladov, prestane dopĺňať `size=20` a odmietne tieto argumenty aj vo filtroch. Do opisu automaticky pridá informáciu o stránkovaní v prehliadači a vrátení celého zodpovedajúceho zoznamu. Upozornenie sa preloží aj v administrácii a doplní sa aj k vlastnému opisu z `@AiToolDescription`. Parameter `sort` zostáva dostupný. Anotácia opisuje existujúce správanie; nemení priame REST volania ani nastavenie frontendu. Metódy bez `Pageable` neovplyvňuje.

Voľbou `include` môžete vymenovať Java metódy, ktoré chcete sprístupniť. Pre kontrolér, ktorý nededí z `DatatableRestControllerV2`, je tento zoznam povinný. Napríklad `AiToolsController` používa `@AiTool(readOnly = true, include = {"preview", "details"})`.

Editor asistenta zoskupuje nástroje podľa názvu Java balíka za `.components.`. Napríklad kontrolér v balíku `sk.iway.iwcm.components.configuration` patrí do skupiny `configuration`. Jej názov môžete preložiť kľúčom `components.ai_assistants.tools.group.configuration`. Mimo balíkov `components.<názov>` má každý kontrolér vlastnú skupinu. Asistent ukladá názvy vybraných nástrojov.

Vlastný endpoint môžete opísať anotáciou `@AiToolDescription` na mapovanej metóde. Ako hodnotu zadajte anglický text alebo prekladový kľúč. Napríklad opis metódy `getAutocompleteDetail` vysvetľuje, ako interpretovať bežné a skryté konfiguračné hodnoty.

```java
import sk.iway.iwcm.components.ai.tools.AiToolDescription;

// In ConfigurationController
@GetMapping("/autocomplete/detail")
@AiToolDescription("components.ai.tools.configuration_get_autocomplete_detail.description")
public ConfDetailsDto getAutocompleteDetail(@RequestParam String name) {
    // ...
}
```

Ak anotáciu vynecháte alebo zadáte prázdny text, katalóg použije preklad podľa vzoru `components.ai.tools.<kontrolér>_<metóda>.description`, potom opis štandardnej operácie alebo text odvodený z názvu metódy. API pre AI používa anglické preklady. V administrácii sa opis zobrazí v jazyku používateľa, pri chýbajúcom preklade v angličtine.

Na konkrétnom kontroléri môžete pomocou `@AiToolEndpoint` opísať aj zdedený handler. V `method` uveďte názov metódy so Spring mapovaním (napríklad `getAll`). Katalóg ponúkne parametre z `query` v schéme a nástroj ich odovzdá pôvodnej REST metóde v URL. Vynechaný parameter si zachová pôvodné správanie kontroléra. V `requiredBody` uveďte JSON cestu ku vnorenému poľu, ktoré handler potrebuje pri ukladaní.

```java
@AiToolEndpoint(method = "getAll", query = @AiToolEndpoint.Query(
    name = "recursive", type = boolean.class, description = "Include pages in subfolders."))
@AiToolEndpoint(method = "edit", requiredBody = "editorFields.groupDetails.groupId")
```

Spoločný opis z `@AiTool(description = "...")` sa zobrazí v riadku kontroléra po zapnutí voľby **Zoskupiť podľa REST kontroléra**. V odpovedi `GET /admin/rest/settings/ai-tools/controllers/all` ho nájdete v poli `controllerDescription`. Pole `pathPrefix` obsahuje spoločný začiatok REST ciest v zozname `endpoints`, napríklad `/admin/rest/settings/configuration`.

`GET /admin/rest/ai/tools` vráti v poli `tools` definície nástrojov dostupných prihlásenému používateľovi. Interaktívny asistent dostáva v prompte iba stručný zoznam svojich priradených nástrojov a vstupné schémy načítavacích nástrojov. Úplné definície vybraných operácií načíta až podľa potreby:

- `ai_tools_preview` (`GET /admin/rest/ai/tools/preview`) vráti polia `name`, `description`, `httpMethod`, `path`, `write` a `version`. Opis je skrátený na najviac 160 znakov s prípadnou výpustkou. Rovnaké údaje dostane model v úvodnom zozname. Tento nástroj možno priradiť odkazom `@tool(ai_tools_preview)` alebo `@group(ai)`.
- `ai_tools_details` (`GET /admin/rest/ai/tools/details`) sa interaktívnemu asistentovi s dostupnými nástrojmi priradí automaticky. Prijíma názvy vybraných nástrojov, napríklad `{"names":["configuration_get_autocomplete","configuration_get_autocomplete_detail"]}`. Pri priamom GET volaní môžete zopakovať parameter `names`. V poli `tools` dostanete definície v poradí výberu vrátane vstupnej schémy, cieľového mapovania, typu odpovede a verzie. Výstupnú schému pridáte parametrom `includeOutputSchema=true` v katalógu aj pri načítaní detailov. Neznáme a neprístupné názvy sa vynechajú, opakovaný názov sa vráti len raz.

Pri požiadavke s hlavičkou `X-WebJET-AI-Assistant` oba nástroje vracajú iba definície priradené tomuto asistentovi a povolené prihlásenému používateľovi. Bez hlavičky zostáva administratívny katalóg obmedzený oprávneniami používateľa. Načítanie definície nemení priradenia ani oprávnenia na vykonanie. Prehliadač prijme najviac 1 MiB na odpoveď katalógu. Ak vybrané definície presiahnu limit, vyžiadajte si ich v menších skupinách.

Verzia nástroja je SHA-256 odtlačok jeho úplnej definície vrátane vstupnej a výstupnej schémy. Poradie kľúčov JSON objektov ho nemení. Model má opätovne použiť načítanú definíciu pri zhodnom názve a verzii; po zmene verzie alebo strate textu z kontextu ju načíta znova. Server uchováva úplné schémy a overuje podľa nich argumenty každého volania bez ohľadu na obsah odoslaný modelu.

Odkazy `@skill(...)` odovzdávajú modelu stručný zoznam zručností: názov, ukážku najviac 160 znakov a verziu odvodenú z obsahu. Úplný text zručnosti sa nevkladá automaticky do inštrukcií každej požiadavky. Asistent si ho podľa potreby vyžiada nástrojom `ai_tools_load_skill` s argumentom `{"name":"PageBuilder_1.0"}`. Nástroj sa priradí automaticky, ak má asistent odkaz na dostupnú zručnosť; samostatný odkaz `@tool(...)` ani `@group(ai)` nie je potrebný. Vráti názov, verziu a inštrukcie s rozbalenými odkazmi. Pole `tools` obsahuje pre každý povolený nástroj uvedený priamo cez `@tool(...)` jeho `name`, `description`, `inputSchema`, `write` a `version`. Schémy ostatných členov skupiny `@group(...)` si asistent vyžiada podľa potreby cez `ai_tools_details`. Ak zručnosť nemá žiadny takýto priamy odkaz alebo by pridaním schém odpoveď prekročila limit prehliadača 1 MiB, server pole `tools` vynechá.

Načítanie zručnosti overuje aktuálneho asistenta, jeho priradené zručnosti a doménu. Neumožňuje čítať ľubovoľnú zručnosť podľa názvu. Pri priamom `GET /admin/rest/ai/tools/load-skill?name=PageBuilder_1.0` je potrebná hlavička `X-WebJET-AI-Assistant` s ID asistenta a oprávnenie `cmp_ai_button`; volanie nástroja hlavičku doplní automaticky. Priradenia nástrojov zo zručností sa na serveri vyhodnocujú aj pred načítaním ich textu modelom, aby zostali zachované kontroly oprávnení. Server preto môže čítať celé inštrukcie z databázy pri zostavovaní priradení; úspora sa týka textu odosielaného modelu.

Načítaný text pokračuje ako výsledok nástroja. Pri čete s kontextom u poskytovateľa zostáva v histórii a ďalší krok odošle iba nový výsledok nástroja. Uložený čet pri pokračovaní z potvrdenej odpovede v tej istej správe použije aj obsah príloh z histórie. Ak takejto odpovedi chýba identifikátor, odošle pôvodný obsah príloh spolu s výsledkami nástrojov. Bez tohto kontextu sa odovzdajú výsledky aktuálnej požiadavky vrátane načítaných zručností; nová samostatná požiadavka ich načíta podľa potreby znova. Asistent má zručnosť opätovne použiť len pri zhodnej verzii v zozname. Zmena verzie alebo chýbajúci text v kontexte znamená, že si má vyžiadať aktuálny obsah. Tým sa vynechajú nepoužité zručnosti, no text ponechaný v histórii môže naďalej vstupovať do účtovanej spotreby tokenov.

Backendové volania bez interaktívneho cyklu nástrojov, napríklad RAG, dostávajú úplné inštrukcie priradených zručností priamo v prompte. Nevyžadujú oprávnenie na načítavací nástroj; priradenia zručností zostávajú obmedzené na doménu asistenta.

Text zručnosti zostáva opakovateľne použiteľným návodom. Odkazy `@tool(...)` a `@group(...)` sa rozbalia, ale makrá vstupu, napríklad `{inputText}`, `{userPrompt}`, jazykové či doplnkové parametre, sa v načítanom texte nenahrádzajú hodnotami aktuálnej požiadavky. Makrá vyžadujúce automatické dosadenie ponechajte v hlavných inštrukciách asistenta, aby sa pri ďalšom použití zručnosti nepreberali staré hodnoty. Stručný zoznam zručností sa pripája až po rozbalení hlavných inštrukcií, takže ani ukážky makier v jeho textoch nevložia používateľský vstup do zoznamu.

Nasledujúci príklad priameho volania môžete spustiť v konzole prehliadača v administrácii pod účtom s právom `menuConfig`.

```javascript
const url = "/admin/rest/ai/tools";
const headers = { "X-CSRF-Token": window.csrfToken };
const { tools } = await (await fetch(url + "/preview", { headers })).json();
if (tools.some(tool => tool.name === "configuration_get_autocomplete_detail")) {
    const details = await (await fetch(url + "/details?names=configuration_get_autocomplete_detail", { headers })).json();
    const [configuration] = details.tools;
    console.log(configuration.inputSchema);
    const response = await fetch(url + "/" + configuration.name + "/call", {
        method: "POST",
        headers: { ...headers, "Content-Type": "application/json" },
        body: JSON.stringify({ name: "smtpPort" })
    });
    if (response.ok) console.log(await response.json());
}
```

Nástroj `configuration_get_autocomplete_detail` vyžaduje názov premennej v poli `name`. Pre hodnotu `"smtpPort"` vykoná `GET /admin/rest/settings/configuration/autocomplete/detail?name=smtpPort`. Pri každom volaní WebJET znovu overí prístup používateľa. Pri citlivej premennej vynechá hodnoty a vráti `redacted: true`; pole `configured` uvádza, či má premenná neprázdnu hodnotu v bežiacej aplikácii.

Argumenty ďalších nástrojov zostavte podľa ich `inputSchema`. Štandardný zdedený handler `/all` pri serverovom stránkovaní predvolene použije veľkosť stránky 20. Nástroj `configuration_get_all` vracia celý dostupný zoznam a argumenty `page` aj `size` odmietne podľa anotácie `@Datatable(clientSidePaging = true)`. Pri väčšom zozname použite `configuration_get_autocomplete` s argumentom `{"term":"smtp"}` a potom načítajte detail podľa presného názvu.

Spoločné inštrukcie pre asistentov pri známom ID uprednostňujú dostupný nástroj `exists` na overenie existencie, `get_one` na načítanie údajov a filtrované `find_by_columns`, vyhľadávanie alebo autocomplete pri hľadaní podľa názvu. Na vyhľadanie jedného záznamu nemajú používať `get_all`: niektoré implementácie ignorujú stránkovanie a môžu vrátiť milióny záznamov. Výnimkou je požiadavka na zoznam, ktorú nemožno splniť cieleným nástrojom a pri ktorej opis nástroja zaručuje obmedzený rozsah odpovede. Ak cielený nástroj chýba, asistent si vyžiada presné ID alebo vysvetlí chýbajúcu možnosť.

Pre operáciu s `@RequestBody` vložte údaje do vlastnosti `body`. Telo editora tak môže obsahovať rovnaké polia `action` a `data` ako pri priamom volaní `/editor`. Limit jedného JSON volania je 1 MiB. Pri väčšej požiadavke API vráti HTTP 413.

Pole `required` platí vždy pre objekt, v ktorého schéme je uvedené. Napríklad `webpages_edit` vyžaduje argument `body` a v ňom polia `title`, `navbar` a `editorFields.groupDetails.groupId`. Schéma môže spoločné alebo rekurzívne typy uložiť do `$defs` a odkázať na ne cez `$ref`; tieto odkazy sú súčasťou opisu typu a nevkladajú sa do argumentov volania. Administrácia odkazy rozbalí v náhľade **Schéma** a v zozname **Povinné argumenty** zobrazí aj cesty `body.title` a `body.navbar`. Voľba **JSON** zachováva presnú strojovú schému. Asistent dostáva úplnú JSON schému vrátane definícií a vnorených požiadaviek.

Povinnosť polí sa odvodzuje z anotácií `@NotBlank`, `@NotEmpty` a `@NotNull`, rovnako ako v `DataTableColumnEditor` pre frontend. Generátor číta aj zdedené a `transient` polia sprístupnené cez getter/setter a rešpektuje názvy JSON vlastností. Zoznam povinných polí nenahrádza inicializáciu editora: pred vytvorením má asistent načítať podporované predvolené údaje nového záznamu, pred úpravou úplný záznam a zachovať vnorené údaje editora.

Ďalšie pravidlá overuje existujúca backendová validácia. Asistent dostane odpoveď vrátane `fieldErrors`, `error` a notifikácií, aj keď má HTTP stav 200. Pri odmietnutom uložení má podľa chyby opraviť argumenty a zopakovať volanie s opravenými údajmi. Nezmenený zápis sa neopakuje; pri neistom výsledku alebo možnom čiastočnom uložení dávky sa najprv overí stav cieleným čítaním.

Prehliadač vykoná volanie nástroja a odošle WebJETu jeho názov, argumenty, HTTP stav a odpoveď backendu. WebJET určí výsledok `success`, `validation_error`, `rejected`, `unknown` alebo `partial`. Argumenty overuje pred každým novým volaním. Asistent tak dostane aj chybu už vykonaného pokusu.

Pri natívnom volaní v trvalom čete WebJET uloží identifikátor odpovede a identifikátor volania OpenAI alebo Gemini. Výsledok priradí k volaniu podľa identifikátora. Poskytovateľ už má pôvodné argumenty v histórii, takže ich WebJET pri odoslaní výsledku vynechá. Pri chybe validácie odošle dôvod odmietnutia a `executed=false`. Záverečná textová odpoveď môže obsahovať JSON.

Ak OpenAI vráti neplatný JSON v argumentoch ponúknutej funkcie, WebJET požiada model o opravu ešte pred spustením nástroja. Ak vo volaní chýba identifikátor, WebJET zopakuje poslednú požiadavku na model vrátane už získaného výsledku nástroja. Na jedno rozhodnutie pripadajú najviac dve opravy. Spotreba tokenov sa započíta za každý pokus.

Pri textovom protokole musí volanie nástroja obsahovať práve jedno rozhodnutie vo formáte JSON. Otázka alebo záverečná odpoveď môže byť obyčajný text. Ak model spojí volanie s predpovedaným výsledkom, WebJET odmietne celú odpoveď. Pri neplatnom JSON alebo argumentoch dostane asistent štruktúrovanú spätnú väzbu s chybou, odmietnutým rozhodnutím a údajom `executed=false`. Obsah odmietnutého rozhodnutia je obmedzený na 32 768 znakov a prípadné skrátenie označuje `responseTruncated`. Spätná väzba je vstupným údajom, nie novou požiadavkou používateľa ani systémovou inštrukciou.

Ochrana pred opakovaným zápisom neodosiela rovnakú požiadavku druhýkrát. Namiesto ukončenia úlohy poskytne asistentovi dôvod odmietnutia, aby mohol opraviť argumenty alebo zvoliť čítanie. To isté platí pre ďalší zápis po neistom alebo čiastočnom výsledku. Počet opráv rozhodnutia a počet vykonaných nástrojov zostávajú obmedzené. Opakovanie zachová pôvodnú požiadavku, prílohy a skutočné výsledky nástrojov v histórii poskytovateľa alebo vo vstupe pri opakovaní požiadavky. Samostatný model na posudzovanie záverečnej odpovede sa nevolá; rozhranie zobrazuje aj skutočné výsledky zápisov z backendu.

V uloženom čete sa extrahovaný obsah príloh uchová pri pôvodnej správe používateľa. Nadväzujúce požiadavky a volania nástrojov dostanú aj obsah poslednej priloženej sady súborov a požiadavku, s ktorou boli odoslané. Asistent tak môže použiť priložený článok aj po doplnení cieľového priečinka. Obsah zostáva zdrojovými údajmi; nenahrádza inštrukcie asistenta ani aktuálnu požiadavku používateľa. Opakované použitie nevyvolá novú analýzu toho istého obrázka.

Audit rozlišuje `TOOL_REQUESTED` (pripravené volanie), `ANSWER_COMPLETED` (dokončená odpoveď) a `TOOL_DECISION_REJECTED` (odmietnuté rozhodnutie). Dokončená odpoveď sama osebe neznamená úspešný zápis. Pri odmietnutí sa zaznamená dôvod a pôvodná odpoveď v rozsahu nastaveného limitu auditovaného obsahu; pri limite `0` sa obsah nezapisuje.

Pri chybe poskytovateľa audit obsahuje aj fázu zlyhania (`prepareInput` alebo `generation`) a jeho diagnostickú správu s odstránenými prihlasovacími údajmi. Diagnostika rešpektuje limit auditovaného obsahu; používateľovi sa naďalej zobrazí lokalizovaná chyba.

Pri volaní cez `/call` uvádza hlavička odpovede `X-WebJET-AI-Tool-Response` typ výstupu nástroja. Pri type `empty` môže úspešná odpoveď HTTP 200 prísť bez tela, pri type `text` môže obsahovať prázdny reťazec. Ak argumenty asistenta porušia vstupnú schému, WebJET vráti HTTP 400 s hodnotou hlavičky `invalid-arguments` a v tele uvedie dôvod odmietnutia. K tejto kontrole dochádza pred spustením cieľovej metódy.

Ak projekt chráni cieľovú URL vlastným servletovým filtrom, jeho URL vzor musí pokrývať aj volanie príslušného nástroja. Spoločným vzorom môže byť napríklad `/admin/rest/*`. Ak by pri preklade požiadavky kontajner vynechal filter cieľovej URL alebo vybral iný servlet, WebJET volanie odmietne.

## Filtrovanie podľa domény

Ak Spring DATA repozitár implementuje rozhranie `DomainIdRepository`, trieda `DatatableRestControllerV2` pracuje s hodnotou `domainId`. Pri čítaní sa automatické filtrovanie aktivuje pre MultiWeb/Cloud inštaláciu alebo pri zapnutej konfiguračnej premennej `enableStaticFilesExternalDir`. Pri zápise sa pre takýto repozitár vždy doplní chýbajúce `domainId` a overí sa, že zaslaná hodnota zodpovedá očakávanej doméne.

Očakávanú doménu vracia chránená metóda `getDomainId()`. Jej predvolená implementácia používa aktuálnu doménu z `CloudToolsForCore.getDomainId()`. Hodnota sa používa pri štandardnom získaní jedného alebo všetkých záznamov, mazaní, vyhľadaní záznamu cez `findItemBy`, výpočte súm a pri doplnení alebo kontrole `domainId` počas uloženia.

Ak má controller pracovať s inou než aktuálnou doménou, prepíšte metódu `getDomainId()`. Rovnakú metódu použite aj vo vlastných repozitárových dotazoch controllera:

```java
@Override
protected int getDomainId() {
    return DomainIdScopeResolver.resolve(EnumerationDataBean.class);
}
```

V príklade controller spravuje pomocné záznamy `CustomFieldsEntity`, ale ich doménový rozsah určuje `EnumerationDataBean`. Resolveru sa preto odovzdáva anotovaná kontextová trieda, nie nevyhnutne typ entity použitej v repozitári.

Anotácia [`@DomainIdCommon`](../../../../src/main/java/sk/iway/iwcm/system/multidomain/DomainIdCommon.java) označuje entitu, ktorej dáta sú spoločné pre všetky domény. Je dostupná počas behu aplikácie a vzťahuje sa aj na podtriedy. Vyhodnocuje ju trieda [`DomainIdScopeResolver`](../../../../src/main/java/sk/iway/iwcm/system/multidomain/DomainIdScopeResolver.java):

- pre anotovanú triedu vráti hodnotu konfiguračnej premennej `domainIdCommon`; ak nie je nastavená na kladné číslo, použije doménu `1`,
- pre neanotovanú alebo neznámu triedu vráti aktuálnu doménu,
- triedu je možné zadať ako `Class<?>` alebo jej úplné meno (`FQDN`).

Entitu označíte nasledovne:

```java
@Entity
@DomainIdCommon
public class EnumerationDataBean {
    // ...
}
```

Samotná anotácia nemení databázové dotazy automaticky. Controller musí použiť `DomainIdScopeResolver` v prepísanej metóde `getDomainId()` a služby s vlastnými dotazmi musia použiť výsledok resolvera explicitne. Označenie preto používajte iba pre dáta, ktoré majú byť skutočne zdieľané a upravované zo všetkých domén.

## Metódy pre manipuláciu s dátami

Triedu môžete konštruovať aj s ```NULL``` repozitárom, v takom prípade je potrebné implementovať metódy na prácu s dátami:

```java
/**
 * Vlozi NOVU entitu do databazy
 * @param entity
 * @return
 */
public T insertItem(T entity)

/**
 * Ulozi existujucu entitu do databazy
 * @param entity
 * @param id
 * @return
 */
public T editItem(T entity, long id)

/**
 * metoda pre ziskanie entity s rovnakou hodnotou v stlci propertyName ako hodnota v obj
 * @param propertyName
 * @param obj
 * @return
 * @throws IllegalAccessException
 * @throws NoSuchMethodException
 * @throws InvocationTargetException
 */
public List<T> findItemBy(String propertyName, T original)

/**
 * Zmaze danu entitu z databazy
 * @param entity
 * @param id
 * @return
 */
public boolean deleteItem(T entity, long id)

/**
 * Ziska z databazy entitu so zadanym id
 * @param id
 * @return
 */
public T getOneItem(long id)

/**
 * Ziska z databazy vsetky zaznamy
 * @param pageable
 * @return
 */
public Page<T> getAllItems(Pageable pageable)

/**
 * Vrati vsetky zaznamy, pricom vykona volanie metody addSpecSearch,
 * cize je mozne pouzit URL parametre na filtrovanie vsetkych zaznamov.
 * @param empty - prazdny objekt (je potrebny kvoli vytvoreniu instance)
 * @param pageable
 * @return
 */
public Page<T> getAllItemsIncludeSpecSearch(T empty, Pageable pageable) {

/**
 * Do objektu searchProperties naplni hladane vyrazy, vrati pripadne upraveny ExampleMatcher
 * @param params
 * @param searchProperties - vratena mapa request parametrov pre vyhladavanie
 * @param searchWrapped
 * @param matcher - ak sa jedna o exampleMatcher, moze byt null
 * @param isExampleSearch
 * @return
 */
public ExampleMatcher getSearchProperties(Map<String, String> params, Map<String, String> searchProperties, BeanWrapperImpl searchWrapped, ExampleMatcher matcher, boolean isExampleSearch) {

/**
 * Vyhlada objekty podla zadaneho search objektu a pripadnych parametrov z requestu
 * @param params
 * @param pageable
 * @param search
 * @return
 */
public Page<T> searchItem(@RequestParam Map<String, String> params, Pageable pageable, T search)
```

v implementácii môžete využiť už existujúce API, napr.:

```java
@Override
public Page<DomainRedirectBean> getAllItems(Pageable pageable) {
    List<DomainRedirectBean> listedBeans = DomainRedirectDB.getAllRedirects();
    return new DatatablePageImpl<>(listedBeans);
}

@Override
public DomainRedirectBean insertItem(DomainRedirectBean entity) {
    DomainRedirectDB.insert(entity);
    return entity;
}

@Override
public DomainRedirectBean editItem(DomainRedirectBean entity, long id) {
    entity.setRedirectId((int)id);
    return DomainRedirectDB.update(entity);
}

@Override
public boolean deleteItem(DomainRedirectBean entity, long id) {
    DomainRedirectDB.delete((int)id);
    return true;
}

@Override
public Page<T> searchItem(Map<String, String> params, Pageable pageable, T search) {

}
```

Ak potrebujete upraviť dáta pred uložením, alebo vykonať akciu po uložením do databázy (napr. nastaviť dátum uloženia, alebo ```domainId```) použite metódu ```beforeSave``` alebo ```afterSave```:

```java

    /**
	 * Metoda volana pred insert/save danej entity,
	 * da sa pouzit na nastavenie udajov, napr. datum ulozenia, domainId a podobne
	 * @param entity
	 */
    @Override
    public void beforeSave(InsertScriptBean entity) {
        //nastav datum ulozenia
        entity.setSaveDate(new Date());

        //nastav korektne domainId
        int domainId = CloudToolsForCore.getDomainId();
        entity.setDomainId(domainId);
        //nastav aj na inner objekty
        if (entity.getGroupIds()!=null) {
            for (InsertScriptGroupBean isg : entity.getGroupIds()) {
                isg.setDomainId(domainId);
            }
        }
    }

    /**
	 * Metoda volana pred duplikovanim danej entity,
	 * da sa pouzit na resetovanie udajov, napr. priradena default stranka adresara a podobne
	 * @param entity
	 */
	public void beforeDuplicate(T entity) {

	}

    /**
	 * Called before duplicating an entity, with the ID of the source entity.
	 * The default implementation preserves compatibility with controllers that
	 * override {@link #beforeDuplicate(Object)}.
	 * @param entity entity prepared for insertion
	 * @param originalId ID of the source entity
	 */
	public void beforeDuplicate(T entity, Long originalId) {

	}

    /**
	 * Metoda volana po duplikovanim danej entity,
	 * da sa pouzit na dokopirovanie udajov, napr. media web stranky
	 * @param entity - novo ulozena (zduplikovana) entita
	 * @param originalId - ID povodneho zaznamu ktory sa duplikoval
	 */
	public void afterDuplicate(T entity, Long originalId) {

	}

    /**
	 * Metoda volana po ulozeni entity.
	 * POZOR: pre novo vytvaranu entitu bude jej ID ulozene len v saved entite, povodna entity bude mat ID=0
	 * @param entity - povodna odoslana entita
	 * @param saved - uz ulozena verzia entity
	 */
    @Override
    public void afterSave(UserDetailsEntity entity, UserDetails saved) {
        Integer userId = saved.getId().intValue();
        ...
    }

    /**
	 * Metoda volana po zmazanim enity z DB, moze vykonat dodatocne akcie
	 * napr. zmazanie suborov z disku, ulozenie do archivu,
	 * alebo obnovu cache objektov
	 * @param entity
	 * @return
	 */
    @Override
    public void afterDelete(UserGroupsEntity entity, long id) {
        UserGroupsDB.getInstance(true);
    }

    /**
	 * Metoda sa vola pri importe po kazdom chunku
	 * @param chunk - aktualny chunk
	 * @param totalChunks - celkovy pocet chunkov
	 */
	public void afterImportChunk(int chunk, int totalChunks) {

	}
```

Metóda ```public void afterSave(T entity, T saved)``` je volaná po uložení entity - objekt ```entity``` je pôvodný odoslaný objekt, ```saved``` je uložená verzia. Pri novom zázname sa ```ID``` nachádza len v ```saved``` entite. Ak používate metódy pre aktualizáciu cache nezabudnite implementovať aj metódu public ```void afterDelete(T entity, long id)``` vyvolanú po zmazaní záznamu.

!>**Upozornenie:** neodporúčame prepísať cez anotáciu ```@Override``` REST metódy, vždy prepisujte vo vašej triede ```xxxItem``` metódy.

Pri duplikovaní záznamu je potrebné prijatým dátam "zmazať" hodnotu ID atribútu. Typicky sa jedná o atribút s menom ```id```, ale nemusí to platiť vždy. Meno atribútu sa teda hľadá podľa nastavenej anotácie ```DataTableColumnType.ID```.

Pri použití [vnorených/doplnkových atribútov](../datatables-editor/datatable-columns.md#vnorené-atribúty) vo forme ```editorFields``` je možné implementovať metódy ```processFromEntity``` pre nastavenie ```editorFields``` atribútov alebo ```processToEntity``` pre nastavenie atribútov v entite z ```editorFields```. Metódy sa automaticky volajú pri čítaní všetkých záznamov, pri získaní jedného záznamu, vyhľadávaní alebo pri ukladaní údajov.

```java
	/**
	 * Vykona upravy v entite pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall...
	 */
	public T processFromEntity(T entity, ProcessItemAction action) {
		return entity;
	}

	/**
	 * Vykona upravy v entite pri zapise cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from editorFields to entity)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall,
	 */
	public T processToEntity(T entity, ProcessItemAction action) {
		return entity;
	}
```

Pri prepísaní metód ```getAllItems``` alebo ```searchItem``` je potrebné vyvolať metódu ```processFromEntity``` na každom elemente vráteného zoznamu. Efektívne môžete zavolať metódu ```processFromEntity(Page<T> page, ProcessItemAction action)``` nad ```Page``` objektom, ktorá pre všetky záznamy z ```Page``` objektu zavolá metódu ```processFromEntity(T entity, ProcessItemAction action)```.

## Filtrovanie pri zobrazení všetkých záznamov

Niekedy je potrebné aj pri zobrazení všetkých záznamov filtrovať dáta (napr. pri vnorenej datatabuľke podľa nejakej skupiny, alebo podľa práv používateľa).

Existuje metóda ```Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)```, ktorú môžete použiť vo vašej implementácii. Jej volanie zabezpečí vykonanie metód ```addSpecSearch``` aj pri získaní všetkých záznamov. Tam môžete implementovať dodatočné podmienky.

```java
@Override
public Page<UserDetailsEntity> getAllItems(Pageable pageable) {

    DatatablePageImpl<UserDetailsEntity> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new UserDetailsEntity(), pageable));

    page.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
    page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
    page.addOptions("editorFields.enabledItems", moduleItems, "itemKey", "itemKey", false);
    page.addOptions("editorFields.permGroups", (new PermissionGroupDB()).getAll(), "title", "id", false);

    return page;
}

@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UserDetailsEntity> root, CriteriaBuilder builder) {

    Identity user = UsersDB.getCurrentUser(getRequest());

    //Check user permissions and return for this user editable entities
    if(!user.isEnabledItem("users.edit_admins")  && user.isEnabledItem("users.edit_public_users")) {
        predicates.add(builder.isFalse(root.get("admin")));
    } else if(user.isEnabledItem("users.edit_admins")  && !user.isEnabledItem("users.edit_public_users")) {
        predicates.add(builder.isTrue(root.get("admin")));
    } else if(!user.isEnabledItem("users.edit_admins")  && !user.isEnabledItem("users.edit_public_users")) {
        //If user doesnt have admin and public perm in same time
        predicates.add(builder.isFalse(root.get("admin")));
        predicates.add(builder.isTrue(root.get("admin")));
    }

    SpecSearch<UserDetailsEntity> specSearch = new SpecSearch<>();
    String permissions = params.get("searchEditorFields.permisions");
    if (permissions!=null) {
        specSearch.addSpecSearchPasswordProtected(permissions, "userGroupsIds", predicates, root, builder);
    }
    String emails = params.get("searchEditorFields.emails");
    if (emails!=null) {
        specSearch.addSpecSearchPasswordProtected(emails, "userGroupsIds", predicates, root, builder);
    }
    int userGroupId = Tools.getIntValue(params.get("userGroupId"), -1);
    if (userGroupId>0) {
        specSearch.addSpecSearchPasswordProtected(userGroupId, "userGroupsIds", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);
}
```

## Neexistujúce atribúty v editore

Štandardne z editora nemusia prichádzať všetky atribúty entity, preto sa pred uložením spájajú hodnoty existujúcej entity a údajov zaslaných z editora. Prepíšu sa všetky atribúty s hodnotou odlišnou od ```null```.

Automatický prenos hodnoty ```null``` sa týka iba atribútov označených anotáciou ```@DataTableColumn```. V rámci týchto atribútov sa prenesie pre Java typy ```java.util.Date```, ```java.sql.Date```, ```LocalDate```, ```LocalDateTime``` a objektové polia s ```inputType = DataTableColumnType.NUMBER```, ktoré podporujú ```null```, aby ich bolo možné v editore vyprázdniť. Pri atribútoch bez anotácie ```@DataTableColumn``` sa vstupná hodnota ```null``` ignoruje a zachová sa pôvodná hodnota entity, aj keď ide o dátumový typ. Primitívne číselné typy hodnotu ```null``` nepodporujú.

Pri importe sa hodnota ```null``` pre tieto dátumové atribúty a ```NUMBER``` automaticky prenesie iba vtedy, keď sa ich stĺpec nachádza v hlavičke importovaného Excel súboru. Spoločný zoznam ```importedColumns``` sa vytvára z hlavičky nezávisle od hodnôt v jednotlivých riadkoch. Vynechaný stĺpec zachová hodnotu existujúcej entity; prázdna bunka alebo explicitná hodnota ```NULL``` v importovanom stĺpci ju vynuluje. Povinné polia naďalej podliehajú validácii, napríklad ```@NotNull```.

Pre dátum uložený v inom type, napríklad ```Instant```, ```Timestamp``` alebo ```Long```, prípadne pre ```TEXT_NUMBER```, je možné prenos ```null``` zapnúť pomocou ```alwaysCopyProperties = { true }```. Toto nastavenie vynúti prenos aj pri vynechanom stĺpci importu. Hodnotou ```false``` sa automatický prenos pre podporované dátumové typy a ```NUMBER``` vypne. Toto spojenie sa vykonáva v metóde ```public T editItem(T entity, long id)``` s využitím ```NullAwareBeanUtils.copyProperties(entity, one);```.

## Obnovenie údajov po uložení

Volaním metódy ```setForceReload(true);``` je možné vynútiť obnovenie údajov datatabuľky po uložení.

Je to potrebné ak sa uložený objekt presunie do iného adresára a podobne. Ukážka je vo [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

Pri datatabuľke s `serverSide: false` sa po úprave jedného záznamu zobrazí strana, na ktorej sa nachádza podľa aktuálneho triedenia a filtrovania. Záznam sa vyhľadá podľa poľa `reloadRowKey`, predvolene podľa `editorId`. Ak po úprave prestane vyhovovať filtru alebo už nie je v načítaných údajoch, zachová sa pôvodná strana, prípadne posledná platná strana. Rovnako sa zachová strana pri hromadnej editácii a mazaní. Ak zmazaním posledného riadka zanikne posledná strana, zobrazí sa predchádzajúca platná strana. Pri prázdnom zozname sa zobrazí prvá strana s prázdnym stavom. Po vytvorení záznamu sa vynúteným obnovením stránkovanie resetuje na prvú stranu.

## Číselníky pre select boxy

Pre select boxy v editore a automatickú konverziu ID na hodnota (napr. ```templateId``` na meno šablóny) je vo WebJETe pridané rozšírenie ```PageImpl``` objektu [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java), ktoré umožňuje posielať číselníkové dáta.

Tieto sa automaticky nastavia do ```options``` objektu v columns definícii editor objektu a automaticky sa aj použijú pre prevod ID-hodnota.

Príklad je v [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) a [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

```java
    //najlepsie riesenie je prepisat metodu getOptions a doplnit page.addOptions metody
    @Override
    getOptions(DatatablePageImpl<T> page) {
        LayoutService ls = new LayoutService(getRequest());
        page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
    }

    //Ukazka pouzitia so standardnym Spring DATA repozitarom a prepisanim metody getAllItems
    @Override
    public Page<TranslationKeyEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
        LayoutService ls = new LayoutService(getRequest());
        page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
        return page;
    }

    //Ukazka pouzitia bez Spring DATA repozitara a prepisanim metody getAll
    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        int groupId = Tools.getIntValue(request.getParameter("groupId"), Constants.getInt("rootGroupId"));

        Prop prop = Prop.getInstance(request);
        WebpagesService ws = new WebpagesService(groupId, prop);

        DatatablePageImpl<DocDetails> page = new DatatablePageImpl<>(ws.getPages());
        page.addOptions("tempId", ws.getTemplates(), "tempName", "tempId", true);
        page.addOptions("menuDocId,rightMenuDocId", ws.getMenuList(), "title", "docId", false);
        page.addOptions("headerDocId,footerDocId", ws.getDocList(), "title", "docId", false);

        return page;
    }
```

Pri volaní ```page.addOptions``` nastavuje tretí ```true/false``` parameter pridanie pôvodného ```Java Beanu``` do výstupného objektu. Ak je nastavené na ```true```, v JSON objekte bude pôvodný Bean dostupný v objekte original. Je možné to využiť na získanie dát napr. šablóny:

```javascript
setCssStyle() {
    //console.log("this.json.options=", this.datatable.DATA.json);
    for (let template of this.datatable.DATA.json.options.tempId) {
        //console.log("Testing temp: ", template.label, "id=", template.original.tempId, " json.tempId=", this.json.tempId);
        if (template.original.tempId == this.json.tempId) {
            ...
        }
    }
}
```

## Vyhľadávanie

Existujú 2 režimy vyhľadávania:

- `ByExample` - využíva metódu `Example<T> exampleQuery = Example.of(search, matcher);` ktorá nastaví vyhľadávacie podmienky do `search` entity a následne ju použije pre vyhľadávanie. V entite musia byť všetky atribúty ako objekty, tie čo majú `NULL` hodnotu sa do vyhľadávania nepoužijú. Takéto vyhľadávanie nepodporuje všetky možnosti.
- `Specification` - využíva dynamické vytvorenie SQL podmienok, repozitár musí rozširovať `JpaSpecificationExecutor<T>`. Toto je **odporúčané riešenie** a umožňuje vyhľadávanie podľa rozsahu dátumov a aj použitie špeciálneho vyhľadávania.

Upozornenie: pre **Oracle** databázu je potrebné nastaviť vyhľadávanie bez ohľadu na veľkosť písma a diakritiku nastavením SQL. Štandardne to zabezpečuje `trigger tgg_after_logon_ci_ai`, ktorý WebJET automaticky vytvorí. Ak vyhľadávanie nefunguje správne, overte, že je správne definovaný:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Oracle nepodporuje vyhľadávanie bez ohľadu na veľkosť písma a diakritiky v `clob` stĺpcoch. Pre tento prípad WebJET ale použije funkciu `LOWER` pred vyhľadávaním, čo umožní vyhľadávanie bez ohľadu na veľkosť písmen. Zoznam názvov JPA atribútov, pre ktoré sa funkcia použije je definovaný v konf. premennej `jpaToLowerFields`. Podmienkou je použitie repozitára, ktorý rozširuje `JpaSpecificationExecutor<T>`.

### Vyhľadávanie podľa rozsahu dátumov

Pre vyhľadávanie podľa rozsahu dátumov je potrebné skombinovať vyhľadávanie by Example a špecifickú podmienku. DT v ```app.js``` posiela hodnotu s prefixom ```daterange:```, podľa ktorej sa následne použije vyhľadávanie v ```DatatableRestControllerV2.searchItem```. Využije sa skombinovanie hodnôt v metóde:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

Podmienkou je, aby Spring DATA repozitár rozširoval aj ```JpaSpecificationExecutor```, príklad je v [RedirectsRepository](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RedirectsRepository extends JpaRepository<RedirectBean, Long>, JpaSpecificationExecutor<RedirectBean> {
}
```

!>**Upozornenie:** všetky JPA repozitáre je nutné definovať ako public, inak nebudú dostupné v klientskych projektoch.

### Špeciálne vyhľadávanie

V prípade potreby vykonania špeciálneho vyhľadávania je možné prepísať metódu `addSpecSearch` v ktorej je možné implementovať špecifickú podmienku. Príklad je v [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) kde sa vyhľadáva podľa mena používateľa aj podľa parametra:

```java
@Override
@SuppressWarnings("unchecked")
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<AuditLogEntity> root, CriteriaBuilder builder) {
    String searchUserFullName = params.get("searchUserFullName");
    if (Tools.isNotEmpty(searchUserFullName)) {
        //ziskaj zoznam IDecok userov, ktory maju dane meno
        List<Integer> userIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT user_id FROM users WHERE first_name LIKE ? OR last_name LIKE ?", "%"+searchUserFullName+"%", "%"+searchUserFullName+"%");
        if (userIds.size()>0) predicates.add(root.get("userId").in(userIds));
    }
    //pridaj vyhladavanie podla modu
    String mode = getRequest().getParameter("mode");
    if ("history".equals(mode)) predicates.add(builder.isNull(root.get("datePrepared")));
    else predicates.add(builder.isNotNull(root.get("datePrepared")));
}
```

!>**Upozornenie:** JPA Repozitár musí dediť aj z ```JpaSpecificationExecutor```, príklad:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Podporné metódy

Okrem všeobecnej metódy ```addSpecSearch``` (ktorá interne volá nasledovné špeciálne metódy) je možné použiť metódy z triedy [SpecSearch](../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Hľadanie podľa mena/priezviska**

Vyhľadávanie podľa zadaného mena/priezviska v ```paramValue```, pričom nájdené ID používateľov sa hľadajú ako IN v ```jpaProperty```:

```addSpecSearchUserFullName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```

príklad použitia:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {

    SpecSearch<DocDetails> specSearch = new SpecSearch<>();

    //vyhladanie na zaklade Meno autora, hladane v DB tabulke nasledne v stlpci authorId
    String searchAuthorName = params.get("searchAuthorName");
    if (searchAuthorName != null) {
        specSearch.addSpecSearchUserFullName(searchAuthorName, "authorId", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);

}
```

**Hľadanie podľa čiarkou oddeleného zoznamu**

Pre hľadanie podľa čiarkou oddeleného zoznamu ID (napr. skupiny používateľov) je možné použiť metódu ```addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` pre hľadanie podľa mena skupiny používateľov, alebo metódu ```addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` pre hľadanie podľa zadaného ID:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UserDetailsEntity> root, CriteriaBuilder builder) {

    SpecSearch<UserDetailsEntity> specSearch = new SpecSearch<>();
    String permissions = params.get("searchEditorFields.permisions");
    if (permissions!=null) {
        specSearch.addSpecSearchPasswordProtected(permissions, "userGroupsIds", predicates, root, builder);
    }
    String emails = params.get("searchEditorFields.emails");
    if (emails!=null) {
        specSearch.addSpecSearchPasswordProtected(emails, "userGroupsIds", predicates, root, builder);
    }
    int userGroupId = Tools.getIntValue(params.get("userGroupId"), -1);
    if (userGroupId>0) {
        specSearch.addSpecSearchPasswordProtected(userGroupId, "userGroupsIds", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);
}
```

Zoznam je v databáze uložený ako ```id1,id2,id3```, preto hľadanie skladá všetky možnosti výskytu: ```id OR id,% OR %,id,% OR %,id```.

**Hľadanie podľa mena značky (perex skupiny)**

Pre hľadanie podľa čiarkou oddeleného zoznamu ID značiek je možné použiť metódu ```addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```.

Toto vyhľadávanie sa automaticky použije ak vyhľadávanie obsahuje parameter ```searchPerexGroups```, je volané priamo v ```DatatableRestControllerV2.addSpecSearch```.

```java
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
    //vyhladavanie podla perexSkupiny
    String searchPerexGroups = params.get("searchPerexGroups");
    if (Tools.isNotEmpty(searchPerexGroups)) {
        SpecSearch<T> specSearch = new SpecSearch<>();
        //ziskaj zoznam IDecok userov, ktory maju dane meno
        specSearch.addSpecSearchPerexGroup(searchPerexGroups, "perexGroups", predicates, root, builder);
    }
}
```

**Hľadanie podľa hodnoty v cudzej tabuľke**

Ak máte vzťah medzi tabuľkou a inou tabuľkou prepojením cez ID a hľadáte podľa textu môžete použiť ```addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```. To vykoná hľadanie výrazu ```paramValue``` v zadanej tabuľke a stĺpci, pričom nájdené hodnoty ID použije ako ```IN``` výraz pri filtrovaní. Vykoná sa SQL dotaz typu: ```"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append```.

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<Media> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    String docTitle = params.get("searchEditorFields.docDetails");
    if (Tools.isNotEmpty(docTitle)) {
        SpecSearch<Media> specSearch = new SpecSearch<>();
        specSearch.addSpecSearchIdInForeignTable(docTitle, "documents", "doc_id", "title", "mediaFkId", predicates, root, builder);
    }
}
```

**Hľadanie docid podľa zadanej cesty**

Ak máte zobrazenú cestu k ```DocDetails``` objektu (typicky pri poli typu json) je možné na filtrovanie podľa cesty použiť metódu ```addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<Media> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    int groupId = Tools.getIntValue(params.get("searchEditorFields.groups"), -1);
    if (groupId > 0) {
        predicates.add(builder.equal(root.join("groups").get("id"), groupId));
    }
    String docTitle = params.get("searchEditorFields.docDetails");
    if (Tools.isNotEmpty(docTitle)) {
        SpecSearch<Media> specSearch = new SpecSearch<>();
        specSearch.addSpecSearchDocFullPath(docTitle, "mediaFkId", predicates, root, builder);
    }
}
```

**Hľadanie pri mapovaní entity**

Ak má vaša entita mapovanú ďalšiu entitu (typu `ContactCategoryEntity` v príklade):

```java
@Getter
@Setter
@Entity
@Table(name = "CONTACT_PLACE")
public class ContactPlaceEntity {
    @Id
    @GeneratedValue(generator = "WJGen_ContactPlace")
    @TableGenerator(name = "WJGen_ContactPlace", pkColumnValue = "ContactPlace")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @JoinColumn(name = "CATEGORY_ID")
    @ManyToOne
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.contact_places.label.category")
    private ContactCategoryEntity category;

    @Column(name = "NAME")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.contact_places.label.name")
    private String name;
    ...
```

vo forme výberového menu (číselníková hodnota) a prenášate cez `page.addOptions` ID hodnoty a názov z REST služby je potrebné pri vyhľadávaní podľa ID nastaviť hľadanie vo vnorenej entite:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ContactPlaceEntity> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    SpecSearch<ContactPlaceEntity> specSearch = new SpecSearch<>();
    if (params.containsKey("searchCategory")) {
        JpaTools.removePredicateWithName("category", predicates);
        Predicate predicate = builder.equal(root.get("category").get("id"), params.get("searchCategory"));
        predicates.add(predicate);
    }

    if (params.containsKey("searchCity")) {
        JpaTools.removePredicateWithName("city", predicates);
        Predicate predicate = builder.equal(root.get("city").get("id"), params.get("searchCity"));
        predicates.add(predicate);
    }
}
```

V HTML súbore je potrebné definovať mapovanie textu z entity na hodnotu, to zabezpečíte nasledovným kódom:

```JavaScript
let columns = [(${layout.getDataTableColumns("sk.iway.projekt.model.ContactPlaceEntity")})];

$.each(columns, function(i, v){
    if (v.data === 'category') {
        v.className = "";
        v.render = function (data, type, row) {
            return row.category.name;
        }
    }

    if (v.data === 'city') {
        v.render = function (data, type, row) {
            return row.city.name;
        }
    }
});
```

### Zobrazenie mena používateľa a vyhľadávanie

Keďže v databáze často ukladáme len ID používateľa, typicky v poli ```user_id``` a zobrazovať a vyhľadávať chceme podľa mena používateľa môžete použiť nasledovné riešenie. Je potrebné použiť generované/```@Transient``` (neukladá sa do databázy) pole ```userFullName```, pričom pôvodné pole ```userId``` nemusíte anotovať. Príklad v ```DTO/Bean``` objekte:

```java
    @Transient
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", title="components.audit_log.user_full_name", orderable = false, editor = {
			@DataTableColumnEditor(type = "text", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private String userFullName;

	public String getUserFullName() {
		if (userFullName == null && userId != null && userId.intValue()>0) {
			UserDetails user = UsersDB.getUserCached(userId.intValue());
			if (user!=null)	userFullName = user.getFullName();
			else userFullName = "";
		}
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}
```

Spring repozitár musí rozširovať ```JpaSpecificationExecutor```, ako je uvedené vyššie. Štandardná implementácia v ```DatatableRestControllerV2.addSpecSearch``` už obsahuje hľadanie podľa parametra ```searchUserFullName```, ktoré je typické pre takéto prípady. Ak teda nepotrebujete iné špeciálne hľadanie, bude vám zobrazenie a vyhľadanie podľa mena používateľa fungovať automaticky.

## Usporiadanie

Štandardne sa stĺpec usporiada podľa definovaného stĺpca. Možnosť nastaviť usporiadanie je možné vypnúť nastavením atribútu `@DataTableColumn(orderable = false)`. Štandardne je tento atribút nastavený na `true`, ale pre vnorené atribúty `@DataTableColumnNested editorFields` je vypnutý.

Niekedy je potrebné nastaviť iný stĺpec pre usporiadanie, prípadne pre kompozitné stĺpce nastaviť usporiadanie podľa viacerých stĺpcov. Príklad:

```java
public class BasketInvoiceEditorFields extends BaseEditorFields {
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice_email.surname",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactLastName,deliverySurName"
    )
    private String lastName;

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.basket.invoice.name",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactFirstName,deliveryName"
    )
    private String firstName;
}
```

Ak potrebujete špeciálne usporiadať výsledky je možné prepísať metódu `public Pageable addSpecSort(Map<String, String> params, Pageable pageable)` v ktorej upravíte `Pageable` objekt. Príklad:

```java
public class BasketInvoiceRestController extends DatatableRestControllerV2<BasketInvoiceEntity, Long> {
    ...
    @Override
    public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {

        Sort modifiedSort = pageable.getSort();

        String[] sortList = Tools.getTokens(params.get("sort"), "\n", true);
        for (String sort : sortList ) {
            String[] data = Tools.getTokens(sort, ",", true);
            if (data.length!=2) continue;
            String field = data[0];
            Direction direction;
            if ("asc".equals(data[1])) {
                direction = Direction.ASC;
            } else if ("desc".equals(data[1])) {
                direction = Direction.DESC;
            } else {
                continue;
            }
            if ("editorFields.firstName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactFirstName", "deliveryName"));
            } else if ("editorFields.lastName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactLastName", "deliverySurName"));
            }
        }

        // create new Pageable object with modified sort
        Pageable modifiedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), modifiedSort);
        return super.addSpecSort(params, modifiedPageable);
    }
}
```

## Validácia / povinné polia

Do editora je integrovaná štandardná Java validácia [jakarta.validation](https://www.baeldung.com/javax-validation). Je teda potrebné v ```JPA Beane``` nastaviť požadované validačné anotácie:

```java
@Entity
@Table(name="url_redirect")
public class UrlRedirectBean extends ActiveRecordRepository implements Serializable
{
    @Column(name="old_url")
	@NotBlank
	@Size(max=255)
    String oldUrl;
}
```

Najčastejšie anotácie:

```java
@NotBlank //nepovoli ani whitespace
@NotEmpty //povoli whitespace
@Size(min=5, max=255) //povolene 5-255 znakov
@Email //validuje email adresu
@Pattern(regexp = "^.+@.+\\.") //validuje podla zadaneho regularneho vyrazu
@DecimalMax(value="200") //akceptuje maximalnu hodnotu 200 vratane
@DecimalMin(value="100") //akceptuje miminalnu hodnoty 100 vratane
@Future //zadany datum musi byt v buducnosti, aplikovatelne na Date objekty
@Past //zadamy datum musi byt v minulosti, aplikovatelne na Date objekty
```

Zachytenie chybového hlásenia je implementované v triede [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) kde sa konvertuje objekt ```ConstraintViolation``` na text. Pre možnosť úpravy chybového hlásenia cez WebJET je hľadaný prekladový kľúč v metóde ```getErrorMessage```. Ak sa nájde, použije sa, inak sa použije štandardné hlásenie z ```jakarta.validation```.

V prekladovom kľúči je možné použiť atribúty anotácie, napr. ```{min}``` alebo zadanú hodnotu ako ```${validatedValue}```.

Anotáciám môžete cez atribút ```message``` nastaviť text chybového hlásenia. Odporúčame ale zadať prekladový kľúč, nie priamo text v slovenskom jazyku:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

Pri použití ```@Convert``` anotácie s využitím objektu namiesto primitívnej hodnoty (napr. ```GroupDetailsConverter```) je na entite potrebné použiť anotáciu ```@Valid```. Tá následne vyvolá validáciu vnoreného objektu. Technický problém je v tom, že chyba sa vyvolá v ```group.navbarName```, ale takéto pole neexistuje v datatabuľke. Trieda ```DatatableExceptionHandlerV2``` toto ošetruje tak, že pre pole obsahujúce znak ```.``` v názve do vystupného JSON objektu generuje ako meno pola hodnotu len pred znakom ```.```, čiže napr. ```group```. Výnimka je pole s názvom ```editorFields```, ktoré sa ponechá v pôvodnom/plnom tvare.

Ak vám nestačí jednoduchá validácia môžete implementovať metódu ```validateEditor```:

```java
    /**
     * Prikladova metoda na validaciu dat odoslanych z datatables editora.
     * Metoda je volana pre kazdy odoslaby objekt.
     * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
     *
     * @param request
     * @param user
     * @param errors
     * @param id
     * @param entity
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryEntity> target, Identity user, Errors errors, Long id, GalleryEntity entity) {
        if (!user.isFolderWritable(entity.getImagePath())) {
            //objekt errorField je nase T, v tomto pripade GalleryEntity, setuje sa presna property kde sa ma error vypisat
            errors.rejectValue("errorField.imagePath", null, Prop.getInstance(request).getText("user.rights.no_folder_rights"));
            //pre globalnu chybu (netykajucu sa konkretneho fieldu) je mozne pouzit kod:
            //((BindingResult)errors).addError(new ObjectError("global", Prop.getInstance(request).getText("datatable.error.fieldErrorMessage")));
        }
    }
```

Ak potrebujete špeciálne kontrolovať práva (napr. pri web stránkach povolenie na priečinky) môžete implementovať metódu `public boolean checkItemPerms(T entity, Long id)`. Metóda sa štandardne volá pri operáciach editácia/vytvorenie/zmazanie/vykonanie akcie/získanie záznamu:

```java
    @Override
    public boolean checkItemPerms(MediaGroupBean entity, Long id) {
        if (InitServlet.isTypeCloud() && entity.getId() != null && entity.getId().longValue()>0) {
            if (GroupsDB.isGroupsEditable(getUser(), entity.getAvailableGroups())==false) return false;
            MediaGroupBean old = getOneItem(entity.getId());
            if (old != null && GroupsDB.isGroupsEditable(getUser(), old.getAvailableGroups())==false) return false;
        }
        return true;
    }
```

## Kontrola rozsahu pri zmene poradia

Koncový bod `/row-reorder` načíta z repozitára celú požadovanú dávku a pre každý záznam zavolá `checkItemPerms`. Ak je však zoznam tabuľky obmedzený aj parametrom požiadavky alebo nadradeným záznamom, napríklad `formName`, `stepId`, `parentId` alebo kategóriou, samotná kontrola jednotlivých záznamov nemusí zaručiť, že všetky patria do aktuálne zobrazeného rozsahu.

V takom kontroléri implementujte metódu:

```java
protected boolean checkRowReorderScope(HttpServletRequest request, List<T> entities)
```

Metóda sa volá po načítaní kompletnej dávky z repozitára a pred validáciou meneného poľa alebo zmenou entít. Parameter `entities` preto obsahuje uložené entity, nie hodnoty z tela požiadavky. Predvolená implementácia vracia `true`, aby zostalo zachované správanie existujúcich kontrolérov.

Implementácia musí postupovať bezpečne a pri neplatnom vstupe vrátiť `false`:

- overiť prítomnosť a platnosť všetkých parametrov určujúcich rozsah,
- porovnať tento rozsah s každou entitou v zozname,
- overiť aktuálnu doménu, ak ju daná entita používa,
- overiť existenciu a vzťah nadradeného záznamu,
- overiť, že aktuálny používateľ môže daný rozsah upravovať.

Napríklad položky viac-krokového formulára overujú názov formulára, krok, doménu aj oprávnenie používateľa:

```java
@Override
protected boolean checkRowReorderScope(HttpServletRequest request, List<FormItemEntity> entities) {
    String formName = MultistepFormsService.getFormName(request);
    int stepId = Tools.getIntValue(request.getParameter("stepId"), -1);
    if (Tools.isEmpty(formName) || stepId < 1 || entities == null || entities.isEmpty() || getUser() == null) return false;

    int domainId = CloudToolsForCore.getDomainId();
    for (FormItemEntity entity : entities) {
        if (entity == null || Objects.equals(formName, entity.getFormName()) == false ||
            entity.getStepId() == null || entity.getStepId().intValue() != stepId ||
            entity.getDomainId() == null || entity.getDomainId().intValue() != domainId) return false;
    }

    if (formStepsRepository.getValidStep(formName, Long.valueOf(stepId), domainId).isEmpty()) return false;
    return formsService.isFormAccessible(formName, getUser());
}
```

Klient pri zostavení URL pre `/row-reorder` zachová parametre z `DATA.url`. Tieto parametre sú však ovládané klientom, preto ich nemožno považovať za dôkaz oprávnenia. Rovnako nestačí rozsah definovať iba cez `addSpecSearch`, pretože ten sa na koncový bod `/row-reorder` automaticky neaplikuje.

Ak metóda vráti `false`, celá operácia sa odmietne a nič sa neuloží. `checkRowReorderScope` je kontrola celej dávky a nenahrádza `checkItemPerms`, ktorá sa naďalej používa pre oprávnenia ku každému jednotlivému záznamu.

## Vyvolanie chyby

Programovo kontrolované chyby je potrebné ošetriť preťažením metódy ```validateEditor``` (viď príklad vyššie), kde môžete vykonať validácie pred uložením záznamu. Z parametra ```target.getAction()``` (DatatableRequest)  môžete identifikovať typ akcie.

!>**Upozornenie:** metóda `validateEditor` sa volá aj pre vymazanie, môžete ho testovať ako ```if ("remove".equals(target.getAction()) ...```.

V prípade chyby detegovanej až pri uložení (napr. v metóde ```editItem```) môžete vyvolať všeobecné chybové hlásenie volaním metódy ```throwError(String error)``` alebo ```throwError(List<String> errors)```. Príklad je v [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Zabránenie zmazania / editácie záznamu

Pre niektoré prípady je potrebné zamedziť editácii alebo zmazaniu záznamu. Je možné preťažiť metódy ```beforeSave``` alebo ```beforeDelete```, príklad:

```java
@Override
public void beforeSave(ConfPreparedEntity entity) {
	throwError("datatables.error.recordIsNotEditable");
}

@Override
public boolean beforeDelete(ConfPreparedEntity entity) {
    //zmazat je mozne len planovany zaznam v buducnosti
    if (entity.getId()>0 && entity.getDatePrepared()!=null && entity.getDatePrepared().getTime()>Tools.getNow()) return true;

    throwError("admin.cong_editor.youCanOnlyDeleteFutureRecords");
    return false;
}
```

## Export a import dát

Pre test exportu dát je možné volať metódu `isExporting()`, ktorá vráti hodnotu `true` ak je práve vykonávaný export dát.

Pre import je možné modifikovať/validovať údaje implementáciou metódy `preImportDataEdit`. Táto metóda je volaná pred samotným importom a je možné v nej dáta modifikovať. Príklad je v triede [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Presmerovanie po uložení

Ak potrebujete presmerovať používateľa na inú stránku po úspešnom uložení záznamu je možné nastaviť URL adresu volaním `setRedirect(String redirect)`:

```java
    @Override
    public void afterSave(FormsEntity entity, FormsEntity saved) {
        if(entity.getFormSettings().getId() == null || entity.getFormSettings().getId() == -1L) {
            if ("multistep".equals(entity.getFormType())) {
                setRedirect("/apps/form/admin/form-content/?formName=" + Tools.URLEncode(saved.getFormName()));
            }
        }
    }
```

## Rozširujúce verzie

Pre špeciálne prípady existujú rozširujúce triedy.

### DatatableRestControllerAvailableGroups.java

Trieda implementuje kontrolu práv pre aplikácie, ktorých práva sú založené na štruktúre web stránok (používateľovi sa majú zobraziť len záznamy, ktoré vyhovujú jemu nastaveným právam na stromovú štruktúru), prípadne sa používajú v MultiWeb inštalácii. Príkladom je nastavenie média skupín, kde je potrebné filtrovať skupiny médií podľa práv používateľa na štruktúru web stránok. Ak používateľ má napr. právo len na priečinok "/Slovensky/Novinky" majú sa zobraziť len skupiny ktoré sa majú nastavené zobrazenie v tomto priečinku (a prípadne všetky bez obmedzení).

Základné použitie je podobné ako štandardný `DatatableRestControllerV2`, ale do konštruktora je potrebné zadať aj meno stĺpca s ID hodnotou a meno stĺpca so zoznamom práv na štruktúru web stránok:

```java
@RestController
@Datatable
@RequestMapping("/admin/rest/media-group")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_media_group')")
public class MediaGroupRestController extends DatatableRestControllerAvailableGroups<MediaGroupBean, Long> {

    @Autowired
    public MediaGroupRestController(MediaGroupRepository mediaGroupRepository) {
        super(mediaGroupRepository, "id", "availableGroups");
    }

}
```

trieda `DatatableRestControllerAvailableGroups` v metóde `public boolean checkItemPerms(T entity, Long id)` kontroluje práva na aktuálnu aj pôvodnú entitu, aby nebolo možné modifikovať existujúce entity, na ktoré používateľ nemá práva.

```java
public abstract class DatatableRestControllerAvailableGroups<T, ID extends Serializable> extends DatatableRestControllerV2<T, ID> {
    ...
    @Override
    public boolean checkItemPerms(T entity, Long id) {

        BeanWrapperImpl bw = new BeanWrapperImpl(entity);
        Number entityId = (Number) bw.getPropertyValue(idColumnName);
        String availableGroups = (String) bw.getPropertyValue(availableGroupsColumnName);

        if ((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) && entityId != null && entityId.longValue()>0) {
            //if it's empty AND it's NOT multiweb then it is available for all domains
            if (InitServlet.isTypeCloud()==false && Tools.isEmpty(availableGroups)) return true;

            if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            T old = getOneItem(entityId.longValue());
            if (old != null) {
                //check also if original entity is editable, you can't just remove perms and edit entity which not belongs to you
                BeanWrapperImpl bwOld = new BeanWrapperImpl(old);
                availableGroups = (String) bwOld.getPropertyValue(availableGroupsColumnName);
                if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            }
        }
        return true;
    }
}
```
