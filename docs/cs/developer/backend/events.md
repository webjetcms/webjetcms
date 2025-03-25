# Události

WebJET používá Spring k publikování a poslech událostí. Základní popis naleznete na stránce [baeldung](https://www.baeldung.com/spring-events). Podporovány jsou synchronní i asynchronní události.

Události jsou typicky implementovány genericky s využitím třídy `WebjetEvent`, která je univerzální nosič událostí. Obsahuje následující atributy:
- `source` - zdrojový objekt události (např. `GroupDetails`, `DocDetails`)
- `eventType` - [typ události](#typy-událostí)
- `clazz` - jméno třídy is package pro [filtrování událostí](#poslech-události) při poslechu

## Typy událostí

Pro standardní operace jsou typy událostí implementovány v `enum` třídě `WebjetEventType`, aktuálně jsou dostupné následující typy:
- `ON_START` - vyvolané na začátku metody, v tomto momentě přes synchronní událost můžete modifikovat údaje objektu
- `AFTER_SAVE` - vyvolané po uložení objektu, máte přístup k již uloženému objektu
- `ON_DELETE` - vyvolaná před smazáním objektu
- `AFTER_DELETE` - vyvolaná po smazání objektu
- `ON_XHR_FILE_UPLOAD` - vyvolaná po nahrání souboru přes URL adresu `/XhrFileUpload`
- `ON_END` - vyvolané na konci metody, používá se v případě, kdy se neprovádí uložení (tedy nevyvolá se `AFTER_SAVE`), ale jen nějaká akce

## Aktuálně publikované události

Aktuálně WebJET publikuje následující události:
- Web stránky - uložení web stránky - publikován je objekt `DocDetails` před i po uložení v editoru stránek při volání `EditorFacade.save`, podmínka: `#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`. Ověřit, zda se jedná o publikování stránky, nebo jen uložení pracovní verze můžete v atributu `doc.getEditorFields().isRequestPublish()`, který vrátí hodnotu `false` pokud se jedná o pracovní verzi stránky.
- Web stránky - smazání web stránky - publikován je objekt `DocDetails` před i po smazání při volání `DeleteServlet.deleteDoc`, podmínka: `"event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`.
- Web stránky - uložení a smazání adresáře - publikován je objekt `GroupDetails` před i po uložení při volání `GroupsDB.setGroup` a `GroupsDB.deleteGroup`, které by se mělo používat pro standardní operace s adresářem web stránky. Podmínka: `#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'`.
- Web stránky - zobrazení web stránky na frontendu - publikován je objekt `ShowDocBean` po získání `DocDetails` objektu (událost `ON_START`) a před směřováním na JSP šablonu je publikována událost `ON_END`. Při `ON_START` lze nastavit atribut `forceShowDoc` na `DocDetails` objekt, který se použije pro zobrazení stránky, atribut `doc` je zatím prázdný. Nastaven je jen `docId`. Při události `ON_END` je v atributu `doc` zobrazen `DocDetails` objekt. Podmínka: `#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'`.
- Web stránky - při časovém publikování stránky - publikován je objekt `DocumentPublishEvent`, který obsahuje `DocDetails` publikované web stránky a atribut `oldVirtualPath` s informací o původní URL adrese stránky (pro detekci, zda se při publikování změnila). Podmínka `#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'`, událost `ON_PUBLISH`.
- Konfigurace - vytvoření a změna konfigurační proměnné - publikován je objekt `ConfDetails` po uložení hodnoty přes uživatelské rozhraní voláním `ConfDB.setName`, podmínka: `#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'`.
- Nahrání souboru - publikován je objekt `File` jak `WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);`, podmínka: `#event.clazz eq 'java.io.File'`.

## Poslech události

Pro poslech události potřebujete implementovat třídu s anotací `@Component` a metodou s anotací `@EventListener`. S využitím atributu `condition` se filtrují vyvolané události. Teoreticky (dle návodu) stačí korektně nastavený generický typ, prakticky ale toto nepomohlo a událost se vyvolávala is jiným než nastaveným generickým typem.

### Synchronní události

Události jsou standardně vyvolány jako synchronní, čili v rámci stejného vlákna se provede vyvolání události i metody, které poslouchají na událost. Efektivní je tak možné v událostech modifikovat data před uložením.

Příklad poslechu synchronní události:

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

V atributu `condition` je možné filtrovat události podle `clazz` i podle `eventType`, například:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_SAVE' && event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
public void handleAfterSaveEditor2(final WebjetEvent<DocDetails> event) {
}
```

### Asynchronní události

Pokud metoda implementující poslech události nepotřebuje modifikovat data, nebo její trvání je dlouhé je vhodné ji implementovat asynchronně. V takovém případě se provede v samostatném vláknu, původní spouštěč události nečeká na její dokončení.

Nastavení je zajištěno ve třídě `BaseSpringConfig` WebJETu pomocí anotace `@EnableAsync`. Metoda, která poslouchá událost a má být provedena asynchronně potřebuje anotaci `@Async`.

Při získání `Thread.currentThread().getName()` vidět, že se jedná o samostatné vlákno, jiné od standardního `http` vlákna.

Příklad:

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

## Publikování události

Publikování události je zajištěno třídou `WebjetEventPublisher`, pro snadnost použití ale přímo třída `WebjetEvent` obsahuje metodu `publishEvent`. Příklad publikování události objektu `GroupDetails`:

```java
public boolean setGroup(GroupDetails group)
{
    (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();
    ...
    (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();
}
```

Typicky by vyvolání události typu `WebjetEventType.ON_START` mělo být na začátku metody a `WebjetEventType.AFTER_SAVE` na jejím konci (po uložení údajů).
