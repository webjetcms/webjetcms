# Názvosloví

- Názvy CSS tříd a ID psát v anglickém jazyce
- Nepoužívat kombinace anglického a slovenského jazyce
- Všechny názvy psát malým písmenem
- Název by měl být samopopisný

## BEM metodika

- Doporučuji používat BEM metodiku http://getbem.com/

# CSS styly editoru

Editor ve výběrovém poli Styly zobrazuje CSS styly nalezené v CSS souboru. Parser CSS je ale velmi jednoduchý (nedokáže zparsovat minifikované CSS). Prakticky ani není žádoucí zobrazovat všechny CSS styly. Z toho důvodu je podporováno hledání CSS stylů v souboru **editor.css**. Tento je hledaný ve stejném adresáři jako hlavní CSS styl. `/templates/INSTALL_NAME/tempname/editor.css`.

V editor.css **nepotřebujete definovat samotné CSS vlastnosti**, stačí definovat názvy CSS tříd (v příkladu je pro ukázku nastavena barva pozadí, v realitě byste ale měli mít CSS vlastnosti přímo v `.scss` souborech).

## Základní CSS třídy

Ve výběrovém poli se zobrazí všechny CSS třídy začínající na novém řádku znakem ., tedy obecné CSS třídy. Ty lze aplikovat na libovolný element.

```css
.styleYellow {
   background-color: yellow !important;
}
.styleBlueViolet {
   background-color: blueviolet !important;
}
```

## CSS třídy navázané na tag

Podporováno je i navázání a nastavení CSS třídy podle specifického tagu. Stačí CSS třídu přefixovat jménem tagu. Při zvolení takového stylu editor automaticky dohledá v parent elementech daný tag a CSS styl aplikuje na tento tag. Stačí mít tedy kurzor v editoru kliknutý do potomka a CSS styl se korektně aplikuje.

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

## CSS třídy navázané na tag a CSS třídu

Často se používá DIV element s určitou třídou. `div.container`. Pro aplikování CSS tříd přímo na takový DIV lze využít zápis se dvěma CSS třídami. Podle první se vyhledá parent element s CSS první třídou a na něj se aplikuje druhá CSS třída.

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

## Odznačení CSS třídy

Stejně jako se CSS třída označuje je možné ji odznačit. Pokud je zvolená CSS třída již nastavena a znovu ji zvolíte, tak se z elementu odstraní.

**Poznámka:** při nastavení/změně CSS třídy potřebuje editor vědět, jakou CSS třídu může odstranit (nemůže jen nastavit class atribut, musí zvolenou CSS třídu přidat/zaměnit a ponechat CSS třídy jako container, row, pb-section a podobně). Při nastavení hodnoty tedy přeiteruje všechny CSS třídy ve výběrovém poli Styly, ty odstraní a následně nastaví vybranou CSS třídu.
