# Webové stránky - API

Ukládání a úprava webových stránek je složitý proces. Třídy jsou v balíčku `sk.iway.iwcm.editor` rozděleny takto:
- [facade.EditorFacade](../../../../javadoc/sk/iway/iwcm/editor/facade/EditorFacade.html) - hlavní třída pokrývající složitost načítání a ukládání webové stránky. Ve výchozím nastavení není důvod používat k této práci jinou třídu než tuto.
- [rest.WebpagesRestController](../../../../javadoc/sk/iway/iwcm/editor/rest/WebpagesRestController.html) - klidové rozhraní pro načítání tabulky webové stránky a její úpravy.
- [service.ApproveService](../../../../javadoc/sk/iway/iwcm/editor/service/ApproveService.html) - služby schvalování webových stránek.
- [service.EditorService](../../../../javadoc/sk/iway/iwcm/editor/service/EditorService.html) - služba pro ukládání webových stránek (tabulka `documents` a `documents_history`).
- [service.MediaService](../../../../javadoc/sk/iway/iwcm/editor/service/MediaService.html) - služba pro ukládání médií nově vytvořené stránky. Protože stránka ještě nemá přidružené `doc_id` hodnota je uložena v databázi s hodnotou `-user_id`.
- [service.MultigroupService](../../../../javadoc/sk/iway/iwcm/editor/service/MultigroupService.html) - pro webové stránky uložené ve více adresářích.
- [service.WebpagesService](../../../../javadoc/sk/iway/iwcm/editor/service/WebpagesService.html) - služba pro seznam stránek v datové tabulce.
- [util.EditorUtils](../../../../javadoc/sk/iway/iwcm/editor/util/EditorUtils.html) - další metody pro editor.

Pokud potřebujete upravit stránku v kódu, použijte výhradně třídu `EditorFacade` na pořízení stávajícího/nového `DocDetails` objektu voláním `getDocForEditor(int docId, int historyId, int groupId)` nebo uložit změny voláním `save(DocDetails entity)`.

Chcete-li rychle vytvořit novou stránku, můžete použít metodu `EditorFacade.createEmptyWebPage(GroupDetails group, String title, boolean available)`. Pokud je hodnota `title` prázdný název adresáře se použije jako název stránky. Atribut `available` určuje, zda má být stránka po vytvoření okamžitě k dispozici k zobrazení.
