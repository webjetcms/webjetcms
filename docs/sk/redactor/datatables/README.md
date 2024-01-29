# Datatabuľky

Dátové tabuľky sú základom rozhrania v CMS WebJET, pozrite si inštruktážne video ako s tabuľkami pracovať.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/-NN6pMz_bKw" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Nové vlastnosti

Oproti vyššie uvedenému videu má štandardná datatabuľka nové vlastnosti.

### Zobrazenie názvu v hlavičke okna

Pri editácii existujúcej položky (ak sa edituje jeden záznam) sa v hlavičke dialógového okna editora zobrazuje názov editovanej položky (na ukážke text ```Produktová stránka```) namiesto všeobecného textu ```Upraviť```.

![](dt-header-title.png)

Podobne pri potvrdení zmazania sa zobrazí zoznam označených položiek pre potvrdenie záznamov, ktoré chcete zmazať.

![](dt-delete-confirm.png)

### Možnosť presúvať okno

Okno editora je možné presúvať (ak napr. potrebujete vidieť informáciu na stránke prekrytú oknom). Stačí keď v oblasti hlavičky začnete okno ťahať (ako štandardné okno vo windows).

### Pamätanie usporiadania

Ak tabuľke zmeníte spôsob usporiadania (kliknutím na názov stĺpca) tabuľka si bude tento spôsob usporiadania pamätať vo vašom prehliadači. Keď sa vrátite znova na danú časť bude tabuľka usporiadaná podľa vašej preferencie.

Kliknutím na ikonu <i class="far fa-wrench" role="presentation"></i>, následne na nastavenie Zobrazenie stĺpcov a následne na tlačidlo Obnoviť sa tabuľka nastaví do základnej podoby vrátane usporiadania.

### Zmena poradia stĺpcov

V tabuľke môžete presúvať stĺpce a meniť tak ich poradie podľa vašich potrieb. Stačí keď chytíte hlavičku (názov) stĺpca a začnete ho ťahať vľavo, alebo vpravo. Poradie stĺpcov sa zapamätá v prehliadači a keď sa znova vrátite na danú časť bude poradie stĺpcov zachované.

Kliknutím na ikonu <i class="far fa-wrench" role="presentation"></i>, následne na nastavenie Zobrazenie stĺpcov a následne na tlačidlo Obnoviť sa tabuľka nastaví do základnej podoby vrátane poradia stĺpcov.

## Nastavenie zobrazenia stĺpcov

Kliknutím na tlačidlo <i class="far fa-wrench" role="presentation"></i> nastavenia sa zobrazí možnosť nastavenia zobrazenia stĺpcov a počtu zobrazených záznamov na jednej strane.

Vo väčšine tabuliek sa štandardne zobrazujú všetky stĺpce, niektoré ako tabuľka v zozname web stránok ale obsahuje veľmi veľa stĺpcov, preto sú štandardne zobrazené len tie základné. Kliknutím na možnosť **Zobrazenie stĺpcov** sa otvorí dialógové okno, v ktorom môžete **zvoliť, ktoré stĺpce chcete zobraziť**. Ľubovoľne ich označíte a po kliknutí na **Uložiť**, sa zvolené stĺpce vo **vašom prehliadači zapamätajú**. Aj po obnovení stránky sa zobrazia zvolené stĺpce.

![](dt-colvis.png)

V okne sa zobrazujú nasledovné stĺpce:

- Meno karty - zobrazuje názov karty v ktorej sa pole nachádza v editore. Ak sa pole nezobrazuje v editore, hodnota je prázdna.
- Nadpis sekcie - zobrazuje nadpis nad poľami v editore (ak je zadaný), umožňuje rozlíšiť skupinu polí, napr. pre nastavenie zobrazenia pre prihláseného, alebo odhláseného používateľa.
- Meno stĺpca - meno poľa v editore, hodnota reprezentuje stĺpec, ktorý chcete zobraziť.

V nastavení zobrazenia stĺpcov je aj tlačidlo **Obnoviť**, ktoré obnoví **predvolené nastavenie zoznamu stĺpcov**. Okrem toho tam máme aj tlačidlá **Zobraziť všetky** a **Skryť všetky**, ktoré jedným kliknutím zapnú alebo vypnú zobrazenie všetkých stĺpcov.

**Upozornenie:** čím viac stĺpcov si necháte zobraziť, tým dlhšie bude trvať vášmu počítaču zobrazenie tabuľky.

## Počet záznamov na strane

Kliknutím na tlačidlo <i class="far fa-wrench" role="presentation"></i> nastavenia sa zobrazí možnosť nastavenia počtu záznamov na strane.

Aby okno obsahovalo ideálny počet riadkov tabuľky je vypočítaná jeho veľkosť a prednastavená hodnota podľa tohto výpočtu. V nastavení počtu záznamov sa zobrazí ako prvá možnosť Automaticky (X) kde X je vypočítaný počet riadkov podľa výšky okna.

![](dt-pagelength.png)

Pod tabuľkou riadkov sa zobrazuje informácia o zobrazenom počte záznamov, celkovom počte záznamov a prípadne stránkovanie (prechod na ďalšie strany).

Automatické nastavenie počtu riadkov tabuľky sa použije len v hlavnom okne, nie je použité vo vnorených tabuľkách v editore (napr. v karte História editácie web stránky).

## Klávesové skratky

Pre efektívnejšiu prácu môžete použiť nasledovné klávesové skratky (```Windows/MacOS```):

- ```CTRL+S/CMD+S``` - vykoná uloženie záznamu do databázy, ale zároveň ponechá otvorené okno editora. Funkcia nemusí byť dostupná, ak je otvorených viacero dialógových okien naraz.
