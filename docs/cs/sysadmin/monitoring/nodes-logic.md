# Výměna údajů uzlů clusteru

Stránky **Aplikace**, **WEB stránky** a **SQL dotazy** sdílejí stejnou logiku ohledně monitorování serveru podle aktuálně zvoleného uzlu. K výběru uzlu slouží pole, které se nachází v záhlaví stránky vedle názvu stránky.

![](sql.png)

Po otevření klikem, můžeme vidět všechny dostupné možnosti. Přednastavená hodnota je vždy aktuální uzel (uzel clusteru na který jste právě přihlášen), který je označen textem `(Aktuálny uzol)`.

![](select-options.png)

Pokud je zvolený uzel aktuální, zobrazují se lokálně uložená data. V tomto případě je také k dispozici i tlačítko vymazání, které odstraní tato lokálně uložená data (tlačítko vymazání je dostupné pouze v případě aktuálního uzlu). V případě jiného než aktuálního uzlu se data získávají z databázové tabulky.

## Obnovení dat - aktuální uzel

Pokud je zvolen aktuální uzel, tak po stisku tlačítka obnovení údajů se pouze získají aktuálně uložená data (nepracuje se zde s databázovými tabulkami). Pokud byla data dříve vymazána, může chvíli trvat než se tam objeví nové záznamy.

## Obnovení dat - vzdálený uzel

V případě jiných uzlů, než aktuálních, je obnovení dat složitější. Data jiných uzlů jsou uložena v tabulce `cluster_monitoring`. Proces obnovení dat začíná vymazáním těchto dat z tabulky, jelikož již nemusí být aktuální.

![](updating-data.png)

Jak je vidět na obrázku výše, data byla odstraněna a zobrazí se animace čekající na data. Také vidíme informační notifikaci, která nás upozorňuje, že tento proces může trvat +- několik vteřin. Tento interval se může lišit v závislosti na nastavené konfigurační proměnné `clusterRefreshTimeout`.

Proces získání aktuálních dat spočívá ve vytvoření požadavku o aktuální data pro nějaký uzel vytvořením záznamu v databázové tabulce `cluster_refresher`. Samotný cluster v intervalech zadaných konf. proměnnou `clusterRefreshTimeout` aktualizuje data v tabulce `cluster_monitoring` pro konkrétní uzel, existuje-li pro tento uzel žádost v tabulce `cluster_refresher`. Proto proces získávání dat může trvat i několik minut a může se lišit v závislosti na nastaveném intervalu obnovování clusteru (může nastat i situace, kdy cluster interval byl těsně před obnovením a aktuální data získáme za 10 sekund, i když interval byl nastaven na 5 minut).

Sice to není zobrazeno, ale stránka se bude každých 10 sekund dotazovat, zda do tabulky `cluster_monitoring` nebyla přidána nová data, která by mohla být zobrazena. V případě, že požadovaný uzel neobsahoval žádná data (ale tabulka již byla aktualizována), vytvoří se nový cluster požadavek o data, a opět budeme každých 10 vteřin kontrolovat, zda se tato data již aktualizovala. Celý proces se bude opakovat, dokud aktualizovaná tabulka `cluster_monitoring` nebude obsahovat alespoň jeden záznam k zobrazení. V té chvíli se skryje animace a aktuálně získaná data jiného uzlu jsou zobrazena.
