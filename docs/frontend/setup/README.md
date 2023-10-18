# Nastavenie šablón

## Jedno web sídlo

Pri použití pre jedno web sídlo/doménu nie je potrebné nastavovať nič špeciálne/rozdielne voči WebJET 8

## Správa viacerých domén

Pri správe viacerých domén je potrebné zadefinovať nasledovné konfiguračné premenné:

- ```multiDomainEnabled=true``` - zapnutie správy viacerých domén
- ```enableStaticFilesExternalDir=true``` - zapnutie delenia dát aplikácii pre domény samostatne a použitie externého adresára na statické súbory.
- ```cloudStaticFilesDir=/cesta/na/disku/``` - nastavenie cesty k externým súborom domén. Jedná sa o cestu na disku (napr. ```/mnt/cluster/static-files/```), ktorá môže byť aj mimo priečinku s web aplikáciou (napr. na sieťovom disku zdieľanom medzi uzlami clustra). V prípade potreby môžete ale doménové súbory mať v priečinku s web aplikáciou, vtedy môžete nastaviť hodnotu ```{FILE_ROOT}static-files``` pri ktorom sa makro ```{FILE_ROOT}``` nahradí za priečinok, z ktorého je web aplikácia spustená.
- ```templatesUseDomainLocalSystemFolder=true``` - zapnutie používania lokálneho ```System``` adresára pre domény

Po nastavení týchto premenných odporúčame reštart servera, alebo minimálne zmazanie všetkých cache objektov.

Takéto nastavenie zabezpečí oddelenie údajov a súborov jednotlivých domén. Ak potrebujete zdieľať súbory medzi doménami môžete použiť priečinok ```/shared```, ktorý je medzi doménami zdieľaný.

**POZOR:** pri použití externého adresára WebJET potrebuje mať aj prázdne priečinky ```/images, /files``` a prípadne ```/shared``` v koreňovom adresári, aby ich zobrazil v sekcii Všetky súbory. Technicky do GIT repozitára nejde push-núť prázdny adresár, vytvorte teda v týchto adresároch nejaký prázdny súbor (ideálne niečo ako ```velmi-dlhy-nahodny-text.txt```).

WebJET 2021 zobrazuje priečinky v zozname web stránok len pre zvolenú doménu. Pri uložení koreňového priečinku domény sa automaticky vytvorí lokálny ```System``` priečinok pre stránky hlavičiek, pätičiek, menu atď. Pri uložení koreňového priečinka domény je preto potrebné obnoviť celú stránku, aby sa korektne načítal odkaz na priečinok ```System```.

Priečinok ```System``` je na rozdiel od WebJET 8 uložený v koreni (môžete ho zobraziť v štruktúre priečinkov kliknutím na list Priečinky pri stlačenej shift klávese) a nie v prvom adresári zvolenej domény (ak ale existuje v doménovom adresári, použije sa ten).

Výberové polia pre hlavičku, pätičku, menu, voľné polia v zozname web stránok, alebo v úprave šablóny zobrazujú stránky v priečinku ```System``` a to vrátane prvej úrovne pod-priečinkov (na rozdiel od WebJET 8). V priečinku ```System``` si teda môžete sami vytvárať pod priečinky pre hlavičky, pätičky, menu, prípadne podľa šablón alebo iného kľúča. Meno stránky má prefix meno adresára, čiže napr. ```Hlavičky/Homepage```.

![](header-footer.png)