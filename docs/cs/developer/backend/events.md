# Události

WebJET používá k publikování a naslouchání událostem Spring. Základní popis naleznete na adrese [baeldung](https://www.baeldung.com/spring-events). Podporovány jsou synchronní i asynchronní události.

Události se obvykle implementují obecně pomocí třídy `WebjetEvent` který je univerzálním nositelem událostí. Obsahuje následující atributy:
- `source` - zdrojový objekt události (např. `GroupDetails`, `DocDetails`)
- `eventType` - [typ události](#typy-událostí)
- `clazz` - název třídy i s balíčkem pro [filtrování událostí](#poslechová-akce) při poslechu

## Typy událostí

Pro standardní operace jsou typy událostí implementovány v `enum` Třída `WebjetEventType`, v současné době jsou k dispozici tyto typy:
- `ON_START` - zavolána na začátku metody, v tomto okamžiku můžete měnit data objektu prostřednictvím synchronní události
- `AFTER_SAVE` - vyvolané po uložení objektu, máte přístup k již uloženému objektu.
- `ON_DELETE` - vyvolán před odstraněním objektu
- `AFTER_DELETE` - vyvolaný po smazání objektu
- `ON_XHR_FILE_UPLOAD` - vyvolaný po nahrání souboru prostřednictvím adresy URL `/XhrFileUpload`
- `ON_END` - volána na konci metody, je použita v případě, že se neprovádí žádné ukládání (tj. není volána `AFTER_SAVE`), ale jen některé akce

## Aktuální zveřejněné události

V současné době WebJET zveřejňuje následující události:
- Webová stránka - uložení webové stránky - objekt je zveřejněn `DocDetails` před a po uložení v editoru stránky při volání `EditorFacade.save`, stav: `#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`. V atributu můžete zkontrolovat, zda stránku publikujete, nebo jen ukládáte její pracovní verzi. `doc.getEditorFields().isRequestPublish()` který vrací hodnotu `false` pokud se jedná o funkční verzi stránky.
- Webová stránka - smazat webovou stránku - objekt je zveřejněn `DocDetails` před a po smazání při volání `DeleteServlet.deleteDoc`, stav: `"event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`.
- Webové stránky - ukládání a mazání adresáře - objekt je zveřejněn `GroupDetails` před a po uložení při volání `GroupsDB.setGroup` a `GroupsDB.deleteGroup` který by se měl používat pro standardní operace s adresáři webových stránek. Předpoklad: `#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'`.
- Webové stránky - zobrazení webové stránky na frontendu - objekt je publikován `ShowDocBean` po získání `DocDetails` objekt (událost `ON_START`) a událost je publikována před směrováním do šablony JSP. `ON_END`. Na adrese `ON_START` lze nastavit atribut `forceShowDoc` na adrese `DocDetails` objekt, který se má použít k zobrazení stránky, atribut `doc` je prozatím prázdný. Je pouze nastaven `docId`. V případě `ON_END` je v atributu `doc` Převzato z `DocDetails` objekt. Stav: `#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'`.
- Webová stránka - když je stránka zveřejněna v čase - objekt je zveřejněn `DocumentPublishEvent` který obsahuje `DocDetails` zveřejněná webová stránka a atribut `oldVirtualPath` s informacemi o původní adrese URL stránky (aby bylo možné zjistit, zda se během publikování změnila). Předpoklad `#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'`, událost `ON_PUBLISH`.
- Konfigurace - vytvoření a změna konfigurační proměnné - objekt je publikován `ConfDetails` po uložení hodnoty prostřednictvím uživatelského rozhraní voláním `ConfDB.setName`, stav: `#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'`.
- Nahrání souboru - objekt je zveřejněn `File` Stejně jako `WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);`, stav: `#event.clazz eq 'java.io.File'`.

## Poslechová akce

Chcete-li naslouchat události, musíte implementovat třídu s anotací `@Component` a metoda s anotací `@EventListener`. Použití atributu `condition` spuštěné události jsou filtrovány. Teoreticky (podle návodu) stačí správně nastavený generický typ, ale prakticky to nepomohlo a událost byla vyvolána s jiným generickým typem, než byl nastaven.

### Synchronní události

Ve výchozím nastavení jsou události vyvolávány synchronně, to znamená, že v rámci jednoho vlákna je provedeno jak vyvolání události, tak metody, které události naslouchají. Efektivně to umožňuje měnit data v událostech před uložením.

Příklad naslouchání synchronní události:

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

V atributu `condition` je možné filtrovat události podle `clazz` také podle `eventType`, například:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_SAVE' && event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
public void handleAfterSaveEditor2(final WebjetEvent<DocDetails> event) {
}
```

### Asynchronní události

Pokud metoda implementující naslouchání událostem nepotřebuje měnit data nebo její trvání je dlouhé, je vhodné ji implementovat asynchronně. V takovém případě se provádí v samostatném vlákně, původní spouštěč události nečeká na její dokončení.

Nastavení je zajištěno v učebně. `BaseSpringConfig` WebJET pomocí anotací `@EnableAsync`. Metoda, která naslouchá události a má být prováděna asynchronně, potřebuje anotaci `@Async`.

Při získávání `Thread.currentThread().getName()` je patrné, že se jedná o samostatné vlákno, odlišné od standardního vlákna `http` vlákna.

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

## Zveřejnění události

Zveřejňování událostí zajišťuje třída `WebjetEventPublisher`, pro snadné použití, ale přímo třída `WebjetEvent` obsahuje metodu `publishEvent`. Příklad publikování události objektu `GroupDetails`:

```java
public boolean setGroup(GroupDetails group)
{
    (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();
    ...
    (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();
}
```

Typicky vyvolání události typu `WebjetEventType.ON_START` by měl být na začátku metody a `WebjetEventType.AFTER_SAVE` na konci (po uložení dat).
