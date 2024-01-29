# Štatistika bannerov

Aplikácia Štatistika bannerov zobrazuje prehľad Top 10 bannerov za zvolené obdobie. Predvolené obdobie je posledný mesiac. Toto obdobie sa dá zmeniť pomocou dátumového filtra v hlavičke stránky. 

![](header.png)

Aplikácia obsahuje datatabuľku Top 10 bannerov ako aj 2 čiarové grafy, zobrazujúce "Top štatistika videní bannerov" a "Top štatistika kliknutí bannerov". Pri zmene dátumového rozsahu pomocou filtra v hlavičke stránky sa aktualizuje datatabuľka ako aj obe grafy.

![](stat-table.png)

Hodnota v stĺpci "Názov" zobrazenej tabuľky je klikateľný link, ktorý Vás presmeruje na detail daného bannera.

## Detail bannera

Detail bannera je podsekcia aplikácie "Štatistika bannerov" a taktiež zobrazuje datatabuľku a 2 čiarové grafy. Rozdiel je v tom že datatabuľka zobrazuje iba práve prezeraný banner a čiarové grafy štatistiku kliknutí a videní tiež iba pre tento banner. 

Aj táto podsekcia obsahuje dátumový filter v hlavičke stránky, ktorý sa správa rovnako ako u štatistiky bannerov, s tým rozdielom, že základný rozsah nie je posledný mesiac, ale prevezme sa nastavený rozsah zo štatitiky bannerov.

![](detail-table.png)

Aj v tomto prípade je hodnota stĺpca "Názov" klikateľný link. Po kliknutí budete presmerovaný na stránku "Zozname bannerov" kde sa otvorí editor daného bannera.

![](editor.png)