# Web stránky - API

Ukladanie a editácia web stránok je komplexný proces. Triedy sú v package ```sk.iway.iwcm.editor``` rozdelené nasledovne:

- [facade.EditorFacade](../../javadoc/sk/iway/iwcm/editor/facade/EditorFacade.html) - hlavná trieda zastrešujúca komplexnosť načítania a uloženia web stránky. Štandardne nie je dôvod, aby ste použili na prácu inú ako túto triedu.
- [rest.WebpagesRestController](../../javadoc/sk/iway/iwcm/editor/rest/WebpagesRestController.html) - rest rozhranie pre načítavanie tabuľky web stránok a jej editáciu.
- [service.ApproveService](../../javadoc/sk/iway/iwcm/editor/service/ApproveService.html) - služby spojené so schvaľovaním web stránok.
- [service.EditorService](../../javadoc/sk/iway/iwcm/editor/service/EditorService.html) - služba pre ukladanie web stránok (tabuľka ```documents``` a ```documents_history```).
- [service.MediaService](../../javadoc/sk/iway/iwcm/editor/service/MediaService.html) - služba pre ukladanie médií novo vytvorenej stránky. Keďže stránka ešte nemá priradenú ```doc_id``` hodnotu ukladajú sa média do databázy s hodnotou ```-user_id```.
- [service.MultigroupService](../../javadoc/sk/iway/iwcm/editor/service/MultigroupService.html) - služba pre web stránky uložené vo viacerých adresároch.
- [service.WebpagesService](../../javadoc/sk/iway/iwcm/editor/service/WebpagesService.html) - služba pre zoznam stránok v datatabuľke.
- [util.EditorUtils](../../javadoc/sk/iway/iwcm/editor/util/EditorUtils.html) - doplnkové metódy pre editor.

Ak potrebujete upravovať stránku vo vašom kóde použite výhradne triedu ```EditorFacade``` pre získanie existujúceho/nového ```DocDetails``` objektu volaním ```getDocForEditor(int docId, int historyId, int groupId)```, alebo pre uloženie zmien volaním ```save(DocDetails entity)```.

Pre rýchle vytvorenie novej stránky môžete použiť metódu ```EditorFacade.createEmptyWebPage(GroupDetails group, String title, boolean available)```. Ak je hodnota ```title``` prázdna použije sa ako názov stránky meno adresára. Atribút ```available``` určuje, či má byť stránka po vytvorení ihneď dostupná na zobrazenie, alebo nie.