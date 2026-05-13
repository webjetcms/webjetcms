# Web stránky - API

Ukládání a editace web stránek je komplexní proces. Třídy jsou v package ```sk.iway.iwcm.editor``` rozděleny následovně:

- [facade.EditorFacade](../../../../javadoc/cs/iway/iwcm/editor/facade/EditorFacade.html) - hlavní třída zastřešující komplexnost načítání a uložení web stránky. Standardně není důvod, abyste použili pro práci jinou než tuto třídu.
- [rest.WebpagesRestController](../../../../javadoc/cs/iway/iwcm/editor/rest/WebpagesRestController.html) - rest rozhraní pro načítání tabulky web stránek a její editaci.
- [service.ApproveService](../../../../javadoc/cs/iway/iwcm/editor/service/ApproveService.html) - služby spojené se schvalováním webových stránek.
- [service.EditorService](../../../../javadoc/cs/iway/iwcm/editor/service/EditorService.html) - služba pro ukládání webových stránek (tabulka ```documents``` a ```documents_history```).
- [service.MediaService](../../../../javadoc/cs/iway/iwcm/editor/service/MediaService.html) - služba pro ukládání médií nově vytvořené stránky. Jelikož stránka ještě nemá přiřazenou ```doc_id``` hodnotu ukládají se média do databáze s hodnotou ```-user_id```.
- [service.MultigroupService](../../../../javadoc/cs/iway/iwcm/editor/service/MultigroupService.html) - služba pro web stránky uložené ve více adresářích.
- [service.WebpagesService](../../../../javadoc/cs/iway/iwcm/editor/service/WebpagesService.html) - služba pro seznam stránek v datatabulce.
- [util.EditorUtils](../../../../javadoc/cs/iway/iwcm/editor/util/EditorUtils.html) - doplňkové metody pro editor.

Pokud potřebujete upravovat stránku ve vašem kódu použijte výhradně třídu ```EditorFacade``` pro získání existujícího/nového ```DocDetails``` objektu voláním ```getDocForEditor(int docId, int historyId, int groupId)```, nebo pro uložení změn voláním ```save(DocDetails entity)```.

Pro rychlé vytvoření nové stránky můžete použít metodu ```EditorFacade.createEmptyWebPage(GroupDetails group, String title, boolean available)```. Pokud je hodnota ```title``` prázdná použije se jako název stránky jméno adresáře. Atribut ```available``` určuje, zda má být stránka po vytvoření ihned dostupná k zobrazení, nebo ne.