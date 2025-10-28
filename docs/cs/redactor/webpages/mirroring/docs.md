# Stránky

Sekce obsahuje přehled provázaných stránek pod společným synchronizačním identifikátorem `syncId`.

![](docs_datatable.png)

## Struktura tabulky

Abychom uměli v tabulce číst, musíme pochopit strukturu tabulky, kde:
- **Řádky**, každý řádek obsahují všechny stránky (přesnější cesty ke stránkám), které jsou navzájem provázány stejnou hodnotou parametru `syncId` (minimálně jedna stránka)
- **Sloupce** se dělí následovně:
  - **SyncID**, hodnota synchronizačního identifikátoru, kterým jsou stránky v řádku provázány
  - **Stav**, ikony, které upozorňují na speciální stavy (více v sekci [stav provázání](./docs#stav-provázání))
  - **cs, en, ...**, jsou automaticky generovány sloupce, kde každý sloupec obsahuje stránky pro danou jazykovou mutaci. Tento jazyk se získá z rodičovské složky stránky nebo její šablony. Počet sloupců v tabulce se dynamicky mění a závisí na tom, v kolika jazykových mutacích jsou stránky provázány. Pokud hodnota ve sloupci chybí, tak neexistuje pro daný `syncId` provázaná stránka v dané jazykové mutaci.

!>**Upozornění:** v případě existence více provázaných stránek se stejnou hodnotou `syncId` a ve stejné jazykové mutaci, jejich hodnoty se ve sloupci pro danou jazykovou mutaci spojí, takže hodnota ve sloupci bude obsahovat cesty k více stránkám.

## Stav provázání

Sloupec **Stav** nabízí pomocí ikon rychlý přehled o stavu převázání. Podporuje následující stavy:
- <i class="ti ti-exclamation-circle" style="color: #ff4b58;" ></i>, ikona zodpovědná stavu **Špatné mapování**. Provázání stránek nabude tento stav v případě objevení více provázaných stránek ve stejné jazykové mutaci.
- <i class="ti ti-alert-triangle" style="color: #fabd00;" ></i>, ikona zodpovědná stavu **Nerovnoměrné vnoření**. Provázání stránek nabude tento stav v případě rozdílné hloubky provázaných stránek od kořenové složky.
- **nic**, žádná ikona se nezobrazuje v případě pokud provázání je korektní (nespadá do předchozích stavů)

### Špatné mapování

Jelikož provázané by měly být pouze stránky se stejným obsahem jen v jiné jazykové mutaci, nedává smysl mít provázaných více stránek ve stejné jazykové mutaci. Proto se takové provázání vyhodnocují jako **špatné mapování**.

### Nerovnoměrné vnoření

Protože provázané by měly být stejné struktury, rozdílné hloubky rodičovských adresářů provázaných stránek indikují chybu mezi strukturami. Oproti **špatnému mapování** nemusí jít hned o chybu, taková provázání jsou označena pro lepší hledání případných chyb.

## Vymazání/zrušení převázání

Při vymazání/zrušení celého provázání zaniká stávající synchronizační parametr `syncId`, protože už nemá co provázat.

## Editace provázání

Při editaci provázání se zobrazí každá provázaná stránka jako výběrové pole stránek i se zkratkou jazyka jako štítkem.

Na následujícím obrázku můžeme vidět příklad **špatného mapování**, kde je provázáno více stránek ve stejné jazykové mutaci, konkrétně v případě `sk` jazyka.

![](doc_editor_A.png)

### Změna stránek

Při editaci lze provázané stránky změnit. V takovém případě bude nahrazené stránce odstraněn synchronizační parametr `syncId` a nově-zvolené stránce se přidá parametr `syncId`.

Pro zvolené stránky není povoleno:
- duplicitní zvolení téže stránky
- zvolení stránky s nastaveným `syncId` (samozřejmě jiným než právě upravované). Pokud stále budete trvat na provázání dané stránky, nejprve musíte zrušit její aktuální provázání (odstranit `syncId`) a až následně ji můžete provázat s jinými stránkami (přidat nové `syncId`).
- výběr více stránek ve stejné jazykové mutaci (chyba [špatné mapování](./docs#špatné-mapování))
- výběr stránek v různé hloubce (chyba [nerovnoměrné vnoření](./docs#nerovnoměrné-vnoření))

Každá z těchto chyb se kontroluje. Pokud se taková chyba objeví při pokusu o změnu, akce bude zablokována.

!>**Upozornění:** editor poskytuje možnost **Ignorovat problémy/upozornění**. Zvolením této možnosti víte uložit i záznamy, které obsahují chyby **špatné mapování** a **nerovnoměrné vnoření**. Tuto ochranu umíte vypnout na vlastní zodpovědnost, pokud to situace vyžaduje.

### Přidání stránky

Tabulka neumožňuje vytvoření zcela nového provázání (nového `syncId`) ale umožňuje přidání (provázání) nových stránek k již existujícím. V editoru při editaci záznamu se nachází tlačítko

<button id="add-sync-btn" class="btn btn-outline-secondary" onclick="showNewSelector(groupsMirroringTable)">
  <i class="ti ti-plus" ></i>

  <span> Připojit složku </span>
</button>

pomocí kterého umíte přidat nová pole pro výběr stránek. Když zobrazíte maximální povolený počet polí (povolených provázání) tlačítko se skryje.

![](groups_editor_B.png)

### Odstranění převázání

Akcí změny stránky umíte prakticky odstranit celé provázání. Tato situace nastane, když odstraníte všechny provázané stránky.
