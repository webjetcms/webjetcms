# WebJET Udalosti

WebJET používa Spring na publikovanie a počúvanie udalostí. Základný opis nájdete na stránke [baeldung](https://www.baeldung.com/spring-events). Podporované sú synchrónne aj asynchrónne udalosti.

Udalosti sú typicky implementované genericky s využitím triedy `WebjetEvent`, ktorá je univerzálny nosič udalostí. Obsahuje nasledovné atribúty:

- `source` - zdrojový objekt udalosti (napr. `GroupDetails`, `DocDetails`)
- `eventType` - [typ udalosti](#typy-udalostí)
- `clazz` - meno triedy aj s package pre [filtrovanie udalostí](#počúvanie-udalosti) pri počúvaní

## Typy udalostí

Pre štandardné operácie sú typy udalostí implementované v `enum` triede `WebjetEventType`, aktuálne sú dostupné nasledovné typy:

- `ON_START` - vyvolané na začiatku metódy, v tomto momente cez synchrónnu udalosť môžete modifikovať údaje objektu
- `AFTER_SAVE` - vyvolané po uložení objektu, máte prístup k už uloženému objektu
- `ON_DELETE` - vyvolaná pred zmazaním objektu
- `AFTER_DELETE` - vyvolaná po zmazaní objektu
- `ON_XHR_FILE_UPLOAD` - vyvolaná po nahratí súboru cez URL adresu `/XhrFileUpload`
- `ON_END` - vyvolané na konci metódy, používa sa v prípade keď sa nevykonáva uloženie (čiže nevyvolá sa `AFTER_SAVE`), ale len nejaká akcia

## Aktuálne publikované udalosti

Aktuálne WebJET publikuje nasledovné udalosti:

- Web stránky - uloženie web stránky - publikovaný je objekt `DocDetails` pred aj po uložení v editore stránok pri volaní `EditorFacade.save`, podmienka: `#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`. Overiť, či sa jedná o publikovanie stránky, alebo len uloženie pracovnej verzie môžete v atribúte `doc.getEditorFields().isRequestPublish()`, ktorý vráti hodnotu `false` ak sa jedná o pracovnú verziu stránky.
- Web stránky - zmazanie web stránky - publikovaný je objekt `DocDetails` pred aj po zmazaní pri volaní `DeleteServlet.deleteDoc`, podmienka: `"event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`.
- Web stránky - uloženie a zmazanie adresára - publikovaný je objekt `GroupDetails` pred aj po uložení pri volaní `GroupsDB.setGroup` a `GroupsDB.deleteGroup`, ktoré by sa malo používať na štandardné operácie s adresárom web stránky. Podmienka: `#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'`.
- Web stránky - zobrazenie web stránky na frontende - publikovaný je objekt `ShowDocBean` po získaní `DocDetails` objektu (udalosť `ON_START`) a pred smerovaním na JSP šablónu je publikovaná udalosť `ON_END`. Pri `ON_START` je možné nastaviť atribút `forceShowDoc` na `DocDetails` objekt, ktorý sa použije pre zobrazenie stránky, atribút `doc` je zatiaľ prázdny. Nastavený je len `docId`. Pri udalosti `ON_END` je v atribúte `doc` zobrazený `DocDetails` objekt. Podmienka: `#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'`.
- Web stránky - pri časovom publikovaní stránky - publikovaný je objekt `DocumentPublishEvent`, ktorý obsahuje `DocDetails` publikovanej web stránky a atribút `oldVirtualPath` s informáciou o pôvodnej URL adrese stránky (pre detekciu, či sa pri publikovaní zmenila). Podmienka `#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'`, udalosť `ON_PUBLISH`.
- Konfigurácia - vytvorenie a zmena konfiguračnej premennej - publikovaný je objekt `ConfDetails` po uložení hodnoty cez používateľské rozhranie volaním `ConfDB.setName`, podmienka: `#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'`.
- Nahratie súboru - publikovaný je objekt `File` ako `WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);`, podmienka: `#event.clazz eq 'java.io.File'`.
- Aktualizácia kódov v texte - publikovaný je objekt `UpdateCodesEvent` po spracovaní štandardných kódov v metóde `DocTools.updateCodes`, umožňuje pridať vlastné kódy. Podmienka: `#event.clazz eq 'sk.iway.iwcm.system.spring.events.UpdateCodesEvent'`, udalosť `ON_START` aj `ON_END` pre možnosť nahradenia kódov pred WebJET spracovaním aj po spracovaní.

## Počúvanie udalosti

Pre počúvanie udalosti potrebujete implementovať triedu s anotáciou `@Component` a metódou s anotáciou `@EventListener`. S využitím atribútu `condition` sa filtrujú vyvolané udalosti. Teoreticky (podľa návodu) stačí korektne nastavený generický typ, prakticky ale toto nepomohlo a udalosť sa vyvolávala aj s iným ako nastaveným generickým typom.

### Synchrónne udalosti

Udalosti sú štandardne vyvolané ako synchrónne, čiže vrámci rovnakého vlákna sa vykoná vyvolanie udalosti aj metódy, ktoré počúvajú na udalosť. Efektívne je tak možné v udalostiach modifikovať údaje pred uložením.

Príklad počúvania synchrónnej udalosti:

```java
package sk.iway...;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class SaveListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
    public void handleGroupSave(final WebjetEvent<GroupDetails> event) {

    }
}
```

V atribúte `condition` je možné filtrovať udalosti podľa `clazz` aj podľa `eventType`, napríklad:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_SAVE' && event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
public void handleAfterSaveEditor2(final WebjetEvent<DocDetails> event) {
}
```

### Asynchrónne udalosti

Ak metóda implementujúca počúvanie udalosti nepotrebuje modifikovať dáta, alebo jej trvanie je dlhé je vhodné ju implementovať asynchrónne. V takom prípade sa vykoná v samostatnom vlákne, pôvodný spúšťač udalosti nečaká na jej dokončenie.

Nastavenie je zabezpečené v triede `BaseSpringConfig` WebJETu pomocou anotácie `@EnableAsync`. Metóda, ktorá počúva udalosť a má sa vykonať asynchrónne potrebuje anotáciu `@Async`.

Pri získaní `Thread.currentThread().getName()` vidno, že sa jedná o samostatné vlákno, iné od štandardného `http` vlákna.

Príklad:

```java
package sk.iway...;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class SaveListenerAsync {

   @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
   @Async
   public void handleGroupSaveAsync(final WebjetEvent<GroupDetails> event) {

      Logger.debug(SaveListenerAsync.class, "================================================= handleAfterSave GROUP ASYNC type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
   }
}

```

## Publikovanie udalosti

Publikovanie udalosti je zabezpečené triedou `WebjetEventPublisher`, pre jednoduchosť použitia ale priamo trieda `WebjetEvent` obsahuje metódu `publishEvent`. Príklad publikovania udalosti objektu `GroupDetails`:

```java
public boolean setGroup(GroupDetails group)
{
    (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();
    ...
    (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();
}
```

Typicky by vyvolanie udalosti typu `WebjetEventType.ON_START` malo byť na začiatku metódy a `WebjetEventType.AFTER_SAVE` na jej konci (po uložení údajov).

## Aktualizácia kódov v texte

Ak potrebujete pridať vlastné kódy do textu stránky (napr. `!CUSTOM_CODE!`), môžete využiť udalosť `ON_START` alebo `ON_END` pre `UpdateCodesEvent`. Tieto udalosti sú publikované pred a po spracovaní štandardných kódov v metóde `DocTools.updateCodes`.

Príklad implementácie vlastného `listener`:

```java
package sk.iway.custom;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UpdateCodesEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

@Component
public class CustomCodesListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.common.UpdateCodesEvent'")
    public void handleUpdateCodes(final WebjetEvent<UpdateCodesEvent> event) {
        if (event.getEventType() != WebjetEventType.ON_START) {
            return; // only process ON_START event, skip ON_END
        }
        UpdateCodesEvent updateCodesEvent = event.getSource();
        StringBuilder text = updateCodesEvent.getText();

        // Add custom code
        text = Tools.replace(text, "!CUSTOM_CODE!", "My Company VAT ID");
        text = Tools.replace(text, "!COMPANY_NAME!", "My Company Ltd.");

        // Set the processed text back
        updateCodesEvent.setText(text);
    }
}
```

V tomto príklade listener počúva na udalosť `ON_START` a nahrádza vlastné kódy v texte. Môžete pristupovať ku všetkým parametrom z `UpdateCodesEvent`:

- `text` - text stránky (modifikovateľný)
- `user` - aktuálne prihlásený používateľ
- `currentDocId` - ID aktuálnej stránky
- `request` - HTTP request
