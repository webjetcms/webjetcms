# Nastavenia kariet

Karta **Nastavenia → Editor → Nastavenia kariet** umožňuje premenovať a zoradiť karty editora, nastaviť predvolenú kartu alebo pridať vlastnú kartu. Nastavenie platí pre všetky záznamy vybranej entity v aktuálne zvolenej doméne. Na prístup potrebujete právo **Editor** (`dataTablesConfig`).

![](tabs-settings-datatable.png)

## Výber alebo vytvorenie karty

Najprv vyberte **Použiť pre entitu**, napr. `sk.iway.iwcm.doc.DocDetails` pre webové stránky. Do poľa **Použiť pre kartu** vyberte identifikátor existujúcej karty, napr. `basic`, alebo zadajte nový identifikátor, napr. `details`. Existujúca karta sa upraví; nový identifikátor vytvorí novú kartu.

Automatické dopĺňanie ponúka entity a karty po zadaní aspoň jedného znaku. Zadaním `*` alebo `%` zobrazíte všetky dostupné možnosti. Pri karte sa zobrazuje názov aj identifikátor, do nastavenia sa uloží identifikátor.

![](tabs-settings-editor.png)

## Nastavenia

| Pole | Význam |
| --- | --- |
| **Názov karty** | Zobrazený názov alebo prekladový kľúč. Pri novej karte bez názvu sa použije jej identifikátor. |
| **Predvolená karta** | Hodnota **Áno** vyberie kartu pri otvorení editora. |
| **Skryť pri vytváraní** | Hodnota **Áno** skryje kartu pri vytváraní nového záznamu; pri úprave existujúceho záznamu zostane dostupná. |
| **Požadované oprávnenie** | Kód práva, ktoré používateľ potrebuje na zobrazenie karty. |
| **CSS triedy** | CSS triedy na úpravu vzhľadu karty. |
| **HTML obsah** | Doplnkový obsah karty, napr. vysvetľujúci text. Samotné polia sa do karty priraďujú cez [Nastavenia polí](fields-settings.md). |
| **Zobraziť za kartou** | Identifikátor karty, za ktorú sa táto karta zaradí. Hodnota `FIRST` ju zaradí na začiatok. |

Prepínače **Predvolená karta** a **Skryť pri vytváraní** ponúkajú **Áno**, **Nie** a **Bez zmeny**. Hodnota **Bez zmeny** ponechá pôvodné správanie. Aj prázdne textové nastavenia ponechávajú pôvodné hodnoty existujúcej karty.

Ako predvolenú odporúčame označiť jednu kartu. Ak je ich označených viac, vyberie sa prvá z nich vo výslednom poradí. Ak sa predvolená karta pri vytváraní nezobrazuje, editor vyberie dostupnú kartu.

## Vlastná karta s voliteľnými poľami

1. V **Nastaveniach kariet** vytvorte záznam pre `sk.iway.iwcm.doc.DocDetails` s identifikátorom `details`, názvom `Doplňujúce údaje` a hodnotou **Zobraziť za kartou** `basic`.
2. Na karte [Voliteľné polia](custom-fields-settings.md) nastavte pre rovnakú entitu polia **A** a **B**: ich typ, popis a prípadnú povinnosť.
3. V [Nastaveniach polí](fields-settings.md) vytvorte samostatný záznam pre `fieldA` a `fieldB`. V oboch nastavte **Vložiť do karty** na `details`. Pri poli `fieldB` môžete nastaviť **Zobraziť za poľom** na `fieldA`.
4. Znovu načítajte zoznam webových stránok a otvorte editor. Polia sa zobrazia v karte **Doplňujúce údaje**, pričom si zachovajú typ, možnosti aj povinnosť z nastavení voliteľných polí.

## Validácia a zrušenie nastavenia

Kombinácia entity a identifikátora karty musí byť v jednej doméne jedinečná. Kartu nemožno zaradiť za seba samú. Hodnota **Zobraziť za kartou** musí odkazovať na existujúcu kartu vybranej entity alebo obsahovať `FIRST`.

Odstránením nastavenia pôvodnej karty sa obnoví jej pôvodná definícia. Pri rušení vlastnej karty najprv presuňte jej polia do inej karty alebo odstráňte ich nastavenia presunu, potom odstráňte kartu. Zmenu overte po opätovnom načítaní stránky administrácie.
