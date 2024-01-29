# Klonovanie štruktúry

Pomocou Klonovania štruktúry môžeme naklonovať celý obsah adresára v stránkach do iného adresára bez potreby znovu vytvárania celej adresárovej štruktúry. Táto možnosť je dostupná v sekcií **Web stránky** ako **Klonovanie štruktúry**. Po výbere tejto možnosti sa zobrazí okno k akcií klonovania. Typicky sa používa na vytvorenie novej jazykovej mutácie web sídla z existujúcej verzie. Jazyk sa preberie z nastavenia zdrojového a cieľového priečinka.

![](clone_structure.png)

Pre vykonanie akcie klonovania je potrebné zadať ID zdrojového priečinka (ktorý priečinok klonujeme) a ID cieľového priečinka (kam na naklonuje zdrojový priečinok). ID priečinkov môžete zadať priamo, ak si ich pamätáte, alebo môže využiť možnosť ```Vybrať```, ktorá otvorí nové okno so stromovou štruktúrou priečinkov, kde si konkrétny priečinok volíte kliknutím na jeho názov.

Samotné klonovanie využíva [Zrkadlenie štruktúry](../docmirroring/README.md) a [Automatický preklad](../../../admin/setup/translation.md). To znamená, že pri spustení klonovania sa vybrané priečinky (ak už nie sú) automatický prepoja konfiguračnou premennou ```structureMirroringConfig```. Zo zdrojového priečinka sa naklonujú všetky pod-priečinky (aj ich všetky vnorenia) aj s web stránkami do cieľového priečinka s tým, že originálne a klonované priečinky/stránky sa medzi sebou prepoja. Jazyk sa preberie z nastavenia zdrojového a cieľového priečinka. Taktiež sa tieto priečinky/stránky aj automatický preložia, ak je nastavený prekladač.

![](clone_structure_result.png)

## Rozpojenie stránok

Ak používate klonovanie na zriadenie kópie web sídla pre napr. dcérsku spoločnosť, alebo podriadenú organizáciu je nežiadúce, aby sa zmeny prenášali medzi týmito verziami. V takom prípade stačí po vytvorení klonu upraviť konf. premennú `structureMirroringConfig` z ktorej vymažete riadok s nastavenými ID priečinkov.