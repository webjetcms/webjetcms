# Zrkadlenie štruktúry

Používateľský opis zrkadlenia štruktúry je v [dokumentácii pre redaktora](../../redactor/apps/docmirroring/README.md). V tejto dokumentácii sa nachádza len technický opis.

Základný java package je ```sk.iway.iwcm.components.structuremirroring``` a adresár pre JSP komponenty ```/components/structuremirroring/```.

## Spôsob prepojenia

Prepojenie jednotlivých adresárov a stránok využíva databázový atribút ```sync_id```, pôvodne používaný pre synchronizáciu medzi WebJETmi. Takáto funkcionalita sa ale aktuálne nepoužíva (využíva sa export a import v XML formáte) a daný atribút nebol využívaný.

Do atribútu ```sync_id``` sa prepojeným adresárom a stránkam nastavuje hodnota z generátora kľúčov volaním ```PkeyGenerator.getNextValue("structuremirroring")```. Hodnota ```sync_id``` navzájom prepojených adresárov alebo stránok musí samozrejme byť rovnaká. Podľa hodnoty v ```sync_id``` sa dajú v databáze vyhľadať prepojené adresáre/stránky.

## Inicializácia prepojenia

Prepojenie sa nastavuje v konfiguračnej premennej ```structureMirroringConfig``` kde na každom riadku je zoznam ID adresárov (hlavné adresáre danej jazykovej mutácie), ktoré majú byť prepojené. Nastavenie konfiguračnej premennej vyvolá udalosť, ktorú počúva ```SaveListener.handleConfSave``` a následne vyvolá ```MirroringService.checkRootGroupsConfig()```. Tu sa skontroluje atribút ```syncId``` zadefinovaných adresárov, ak nie je nastavený automaticky sa nastaví.

Volanie ```MirroringService.isEnabled(int groupId)``` overuje, či pre zadané ID adresára je zapnuté zrkadlenie štruktúry.

## Prepojenie adresárov

Prepojenie adresárov je realizované počúvaním udalosti zmeny adresára ```SaveListener.handleGroupSave```, čo následne volá ```GroupMirroringServiceV9.handleGroupSave```.

Poznámka: výnimka je pre adresár s názvom **Nový podadresár** (prekladový kľúč ```editor.dir.new_dir```), ktorý vznikne v stromovej štruktúre kliknutím na pridať nový adresár v kontextovom menu. Takto by vznikli v ostatných jazykových mutáciách adresáre ```Nový podadresár```, čo by nemalo praktický význam (keďže zmena názvu adresára už nemení vytvorené zrkadlené kópie).

Pri udalosti ```WebjetEventType.ON_START``` sa kontroluje, či adresár už má nastavené ```syncId```. Ak nie, vieme, že sa jedná o nový adresár, ktorý ešte nie je zrkadlený a nastavíme mu novú hodnotu ```syncId```.

Pri udalosti ```WebjetEventType.AFTER_SAVE```, čiže po uložení adresára podľa ```syncId``` hľadáme zrkadlené adresáre. Ak žiadne neexistujú, vytvoria sa nové adresáre. Je využité API ```groupsDB.setGroup(mirror, false);``` s druhým ```false``` atribútom, ktorý **nevyvolá** znova udalosť po uložení adresára (čo by spôsobilo rekurziu).

Pri existencii prepojenia sa overuje, či nastala zmena rodičovského adresára. To je pomerne komplikovaná detekcia. Najskôr sa získa zoznam prepojených adresárov pre aktuálny adresár. Následne sa overuje, či skutočne prepojené adresáre majú rodičov z uvedeného zoznamu. Ak nie, tak sa vykoná presun adresára do korektného.

Overenie priority usporiadania je jednoduché - overujú sa prepojené adresáre a zhoda hodnoty priority usporiadania. Ak nie je rovnaká, nastaví sa.

Pri udalosti ```WebjetEventType.AFTER_DELETE``` sa volaním ```GroupsDB.deleteGroup``` vymažú aj zrkadlené adresáre. Tie sa typicky presunú do koša. Zostane im síce nastavené ```syncId```, ale keďže sa nachádzajú mimo zrkadlenej štruktúry, tak ich následné zmeny už nie sú zrkadlené.

## Prepojenie web stránok

Prepojenie web stránok je implementované v ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, čo rieši uloženie web stránky v editore stránok. Objekt ```DocDetails``` ale neobsahuje hodnotu ```syncId```, preto je v prvom kroku hodnota do objektu nastavená (podľa ```docId```).

Podobne ako pre adresár pri udalosti ```WebjetEventType.ON_START``` sa pre stránku, ktorá ```syncId``` nemá nastavené vygeneruje nová hodnota.

Pri udalosti ```WebjetEventType.AFTER_SAVE``` sa získajú prepojené stránky. Ak žiadne neexistujú vytvoria sa volaním ```DocDB.saveDoc(mirror, false)```, atribút ```false``` rieši problém s rekurziou (nevyvolá znova udalosti). Jediný problém, ktorý môže nastať sú práva a schvaľovanie, to aktuálna implementácia nerieši. Ak redaktor nemá práva na inú jazykovú mutáciu web stránka sa aj tak vytvorí.

Ak už aktuálne ukladaná stránka má prepojené stránky podobne ako pre adresár sa overuje správnosť rodičovského adresára a priority usporiadania.

Mazanie stránky sa deteguje v ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. Pri udalosti ```WebjetEventType.AFTER_DELETE``` sa získa zoznam zrkadlených stránok a vyvolá sa ```DeleteServlet.deleteDoc```.

## Vynútenie obnovenia stromovej štruktúry

Ak zmena v štruktúre vyžaduje obnovenie stromovej štruktúry je možné nastaviť v udalostiach atribút:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

Tento je následne v REST službách po dokončení uloženia overený a je vyvolané obnovenie stromovej štruktúry. Volanie cez ```RequestBean``` bolo použité z dôvodu, že je dostupný počas celého ```requestu``` a je staticky dostupný.
