# Nomenklatura

- Názvy a ID tříd CSS v angličtině
- Nepoužívejte kombinaci angličtiny a slovenštiny
- Všechna jména malými písmeny
- Název by měl být popisný

## Metodika BEM

- Doporučuji použít metodiku BEM http://getbem.com/

# Editor stylů CSS

Ve výběrovém poli Styly editor zobrazí styly CSS nalezené v souboru CSS. Parser CSS je však velmi jednoduchý (neumí analyzovat minifikované CSS). Prakticky ani není žádoucí zobrazovat všechny styly CSS. Z tohoto důvodu je podporováno vyhledávání stylů CSS v souboru **editor.css**. Hledá se ve stejném adresáři jako hlavní styl CSS, např. `/templates/INSTALL_NAME/tempname/editor.css`.

V souboru editor.css **nemusíte definovat samotné vlastnosti CSS.**, stačí definovat názvy tříd CSS (v příkladu je pro příklad nastavena barva pozadí, ale ve skutečnosti byste měli mít vlastnosti CSS přímo v souboru. `.scss` soubory).

## Základní třídy CSS

Ve výběrovém poli se zobrazí všechny třídy CSS začínající znakem nového řádku ., tedy obecné třídy CSS. Ty lze použít na libovolný prvek.

```css
.styleYellow {
   background-color: yellow !important;
}
.styleBlueViolet {
   background-color: blueviolet !important;
}
```

## Třídy CSS vázané na značku

Podporováno je také vázání a nastavování tříd CSS podle konkrétní značky. Stačí před název tagu vložit předponu třídy CSS. Při výběru takového stylu editor automaticky dohledá značku v nadřazených prvcích a použije na ni styl CSS. Stačí tedy mít kurzor editoru kliknutý v podřízeném tagu a styl CSS se správně aplikuje.

```css
section.aqua {
   background-color: aqua !important;
}
section.azure {
   background-color: azure !important;
}
div.burlywood {
   background-color: burlywood !important;
}
div.chocolate {
   background-color: chocolate !important;
}
```

## Třídy CSS vázané na značku a třídu CSS

Často se používá prvek DIV s určitou třídou, např. `div.container`. Pro přímé použití tříd CSS na takový DIV je možné použít zápis se dvěma třídami CSS. Podle prvního z nich se načte rodičovský prvek s první třídou CSS a na něj se aplikuje druhá třída CSS.

```css
//aplikuje sa iba na div.container
div.container.red {
   background-color: red !important;
}
div.container.orange {
   background-color: orange !important;
}

//aplikuje sa iba na div.row
div.row.darkorange {
   background-color: darkorange;
}
div.row.orange {
   background-color: orange;
}

//aplikuje sa iba na div.col-12
div.col-12.chartreuse {
   background-color: chartreuse;
}
div.col-12.darkgreen {
   background-color: darkgreen;
}
```

## Označení třídy CSS

Stejně jako je třída CSS označena, může být i neoznačena. Pokud je vybraná třída CSS již nastavena a vy ji znovu vyberete, bude z prvku odstraněna.

**Poznámka:** při nastavování/změně třídy CSS musí editor vědět, jakou třídu CSS může odebrat (nemůže pouze nastavit atribut class, musí přidat/změnit vybranou třídu CSS a zachovat třídy CSS jako kontejner, řádek, pb-sekce atd.). Proto při nastavování hodnoty projde všechny třídy CSS v poli pro výběr stylů, odstraní je a pak nastaví vybranou třídu CSS.
