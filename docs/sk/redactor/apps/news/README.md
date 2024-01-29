# Novinky

Aplikácia Novinky, vloží do stránky zoznam web stránok v zadanom priečinku. Používa sa na vkladanie zoznamu noviniek, tlačových správ, ale aj iných podobných výpisov (zoznam kontaktných miest, osobných kontaktov, produktov a podobne).

![](news.png)

# Zoznam noviniek

Zoznam noviniek v administrácii je podobný zoznamu web stránok, ale neobsahuje stromovú štruktúru. Nachádza sa v menu Príspevky/Novinky. V hornej časti je možné vybrať priečinok pre zobrazenie v tabuľke.

![](admin-dt.png)

Hodnoty vo výberovom poli priečinku v hlavičke sa generujú:

- automaticky - ak je konf. premenná `newsAdminGroupIds` nastavená na prázdnu hodnotu získa sa zoznam ID priečinkov s novinkami vyhľadávaním výrazu `!INCLUDE(/components/news/` v telách stránok a dohľadaním nastaveného ID priečinka `groupIds`.
- podľa konf. premennej `newsAdminGroupIds`, kde je možné zadať čiadkou oddelený zoznam ID priečinkov, napr. `17,23*,72`, pričom ak ID priečinka končí na znak `*` načítajú sa pri výbere aj novinky (web stránky) z pod priečinkov.

Kliknutím na názov novinky sa otvorí editor zhodný s [editorom stránok](../../webpages/editor.md).

![](admin-edit.png)

# Nastavenie aplikácie vo web stránke

Aplikácia vložená do web stránky má nasledovné karty:

## Parametre aplikácie

V záložke parametre aplikácie nastavujete základné správanie aplikácie a jej nastavenia.

![](editor-dialog.png)

- Adresár - ID adresárov (priečinkov web stránok), z ktorých sa budú vyberať novinky (stránky). Tlačítkom Pridať, môžete vybrať viac ID adresárov.
- Zahrnúť podadresáre - zvolením tejto možnosti sa načítajú novinky aj z podadresárov zvolených adresárov z poľa Adresár.
- Typy stránok - výber stránok podľa dátumovej platnosti
    - Aktuálne – je platný dátum začiatku a konca - zobrazia sa len novinky, ktorých dátum platnosti (začiatok a koniec pulikovania) je v rozmedzí aktuálneho dátumu.
    - Staré – zobrazia sa novinky, ktoré majú dátum konca v minulosti (archív).
    - Všetky – zobrazia sa novinky bez ohľadu na dátum začiatku a konca ich publikovania.
    - Nasledujúce – zobrazia sa len novinky, ktoré majú dátum začiatku publikovania v budúcnosti.
    - Aktuálne platné - zobrazia sa len novinky s vyplneným dátumom začiatku (koniec nemusí byť vyplnený) a konca ktorých rozsah je platný v aktuálny dátum a čas.
- Usporiadať podľa - určuje spôsob usporiadania zoznamu noviniek
    - Priority
    - Dátumu začiatku publikovania
    - Dátumu konania
    - Dátumu poslednej zmeny
    - Názvu stránky
    - Miesta
    - ID stránky
    - Ratingu - hodnotenia stránky (napr. pri použití eshopu) - hodnotenie sa nastavuje pomocou aplikácie hodnotenie stránky.
- Vzostupne - štandardne sa usporiada zoznam zostupne (napr. od najnovšej novinky po najstaršiu), zaškrtnutím tohto poľa bude usporiadanie naopak - od najstaršej po najnovšiu
- Stránkovanie - ak zaškrtnete zobrazí sa aj stránkovanie zoznamu noviniek (ak je počet noviniek väčší ako hodnota v poli Počet položiek na stránke)
- Počet položiek na stránke - počet zobrazených noviniek na jednej stránke, ak je stránkovanie nezaškrtnuté podľa tejto hodnoty sa načíta z databázy počet noviniek, vhodné napr. na úvodnú stránku kde chcete mať zobrazené napr. 3 novinky a odkaz na zoznam všetkých noviniek, ale stránkovanie nechcete zobraziť.
- Preskočiť prvých - počet záznamov, ktoré chcete preskočiť pri načítaní zoznamu (napr. ak máte v stránke dve aplikácie pod sebou s iným dizajnom a v druhej chcete preskočiť počet záznamov z prvej aplikácie)
- Nemusí byť vyplnený perex (anotácia) - štandardne sa zobrazia len novinky, ktoré majú vyplnenú anotáciu (perex), ak zaškrtnete toto pole, načítajú sa aj tie, ktoré anotáciu (perex) vyplnenú nemajú
- Načítanie s textom stránky (menej optimálne) - štandardne sa z databázy nenačítava text stránky, ak ho pre zobrazenie potrebujete, zaškrtnite toto pole. Načítanie ale bude pomalšie a náročnejšie na výkon databázy a servera.
- Kontrolovať duplicitu - ak stránka obsahuje viacero aplikácii novinky v jednej stránke, eviduje sa zoznam už zobrazených noviniek. Už existujúce sa vyradia so zoznamu. Nemusí ale následne sedieť počet zobrazených záznamov, zároveň sa ale nestane, že bude na jednej stránke zobrazená rovnaká novinka viac krát.
- Vložiť triedy do `Velocity` šablóny - špeciálne pole pre programátora, ktorým je možné zadefinovať Java triedu (program), ktorú je možné následne použiť v šablóne. Ak nemáte presné inštrukcie čo do tohto poľa vložiť ponechajte ho prázdne.
- Čas vyrovnávacej pamäte (minúty) - počet minút pamätania zoznamu noviniek. Načítanie zoznamu noviniek môže byť náročné na výkon databázy, odporúčame nastaviť vyrovnávaciu pamäť na minimálne 10 minút. Urýchli to zobrazenie stránky (hlavne ak je zoznam noviniek napr. na úvodnej stránke).

## Šablóna

V karte šablóna volíte vizuálny spôsob zobrazenia zoznamu noviniek.

![](editor-dialog-templates.png)

Ak máte právo Novinky a tlačové správy - Vytvorenie a úprava šablón môžete vytvoriť novú dizajnovú šablónu noviniek a upravovať existujúce. Dizajnové šablóny noviniek sa upravujú vo vlastnom editore. Štandardne odporúčame robiť len drobné úpravy v HTML kóde šablóny a využiť možnosti ktoré vám ponúka kontextové menu v editore šablón noviniek.

Šablóny noviniek používajú [Velocity Engine](https://velocity.apache.org/engine/2.3/vtl-reference.html) pre zobrazenie, je teda možné definovať cykly, podmienky a iný programový kód. Pripravené sú šablóny s jedným, dvoma aj troma stĺpcami. Šablóny odporúčame editovať len používateľmi, ktorý vedia čo robia a poznajú syntax `Velocity Engine`. Odporúčame vychádzať z pripravených šablón a prípadne ich len upravovať. Štandardný redaktor by nemal mať právo na editáciu šablón noviniek, mal by ich len používať.

Pri úprave šablón je v dialógovom okne dostupné kontextové menu (keď kliknete pravým tlačidlom do poľa HTML kód alebo HTML kód stránkovania) pomocou ktorého ľahko vložíte programové bloky. Šablóny je možné duplikovať, odporúčame vám teda začať vytvorením kópie existujúcej šablóny a následne len upraviť HTML kód.

Náhľadový obrázok pre šablónu uložte do `/components/news/images/MENO-SABLONY.png`.

Niekoľko ukážok práce s pokročilými objektami:

```velocity
//nastavenie premennej podla pageParams objektu:
#set ($anonymousQuestions = $pageParams.getBooleanValue("anonymousQuestions", false))

//nastavenie premennej:
#set ($fileType = $media.mediaLink.split("[.]"))

//prechod cez zoznam perex skupin a nastavenie CSS triedy podla mena perex skupiny
<div class="grid-item grid-item-$doc.docId
#foreach($perexGroup in $doc.perexGroupNames)
    #if ($perexGroup == "news-red")
    grid-item-red
    #elseif ($perexGroup == "news-green")
    grid-item-green
    #elseif ($perexGroup == "news-blue")
    grid-item-blue
    #end
#end
" data-doc-id="$doc.docId">

//nacitanie medii a vypis
#foreach($media in $MediaDB.getMedia($doc, "files"))
    #set ($fileType = $media.mediaLink.split("[.]"))
    #if($fileType[1].equals('jpg') || $fileType[1].equals('png') || $fileType[1].equals('gif')) <a rel='wjimageviewer' href="$media.mediaLink"  > <img  src="$media.mediaLink " alt="" class="media-img myModalImg" style="height: 100%;" /></a> #end
#end

//nacitanie medii a vypis
<div class="row"> #foreach($media in $MediaDB.getMedia($doc, "files"))
    #set ($fileType = $media.mediaLink.split("[.]"))
    #if(!$fileType[1].equals('jpg') && !$fileType[1].equals('png') && !$fileType[1].equals('gif')) <a href="$media.mediaLink" class="col-md-4 text-truncate icon-$fileType[1]" target="_blank"> $media.mediaTitleSk</a> #end
#end </div>

//vypis diskusnych prispevkov
//vyzaduje pridanie sk.iway.iwcm.forum.ForumDB do parametra Vlozit triedu do Velocity sablony
#set($forumDb = $ForumDB.getForumFieldsForDoc(null, $doc.docId))
#set($commentCount = $forumDb.size())
#set($showComment = 3)
#set($e = $commentCount - $showComment)
#foreach($forum in $forumDb)
    <div class="comment" #if($foreach.count > $e)style="display:block;"#end>
    <div class="comment-header"> <img src="/thumb$forum.getAuthorPhoto('/templates/intranet/assets/images/css/avatar.png')?w=35&h=35&ip=5" class="mr-3" alt="Fotka používateľa $forum.autorFullName"/>$forum.autorFullName <span>$forum.questionDateDisplayDate $forum.questionDateDisplayTime</span> </div>
    <p>$forum.question</p>
</div>
#end

//vypis texu podla prihlaseneho/neprihlaseneho pouzivatela
#if ($actionBean.getCurrentUser()) LOGGED #end
#if (!$actionBean.getCurrentUser()) NOT-LOGGED #end

//zoznam vsetkych stranok ako odkazy - standardne $pages pouziva format 1 2 3 ... 7 8 9, pagesAll obsahuje 1 2 3 4 5 6 7 8 9
//v pages je objekt PaginationInfo, obsahuje property label, pageNumber, url, active, actual, first, last, link a getLi() pre ziskanie celeho HTML kodu LI elementu
$pagesAll
//celkovy pocet stran strankovania, napr 23, da sa ziskat aj z $lastPage.pageNumber
$totalPages
```

## Perex skupiny

V karte Perex skupiny môžete vytvárať podmienky pre zobrazenie noviniek len zo zvolených perex skupín. Používajú sa na označenie napr. Top správy na úvodnej stránke a podobne.

Zároveň ak potrebujete so zoznamu vylúčiť perex skupinu nastavte ju do poľa Nezobraziť zvolené perex skupiny.

![](editor-dialog-perex.png)

Používa sa to v prípade, ak máte na úvodnej stránke v hornej časti sekciu TOP Novinky kde zobrazujete novinky označené príznakom TOP a následne pod tým máte zoznam ostatných noviniek. Vylúčením perex skupiny TOP z druhého zoznamu noviniek zamedzíte duplicite.

## Filter

V karte filter môžete definovať pokročilé možnosti zobrazenia noviniek podľa databázových atribútov a podmienok. Medzi jednotlivými podmienkami sa používa `A/AND`, teda musia byť splnené všetky zadané podmienky filtra.

![](editor-dialog-filter.png)

## Novinky

V karte novinky sa zobrazí zoznam noviniek, ktoré sa načítajú podľa zvolených adresárov z karty Parametre aplikácie. Vidíte tak zoznam noviniek a môžete jednoducho existujúce novinky upravovať (upraviť nadpis, fotografiu, prípadne text novinky). Rovnako môžete vytvoriť novú novinku.

![](editor-dialog-newslist.png)

# Možné konfiguračné premenné

- ```newsAdminGroupIds``` - Zoznam ID priečinkov s novinkami. ID sú oddelené čiarkami.