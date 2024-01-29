# Názvoslovie
- Názvy CSS tried a ID písať v anglickom jazyku
- Nepoužívať kombinácie anglického a slovenského jazyku
- Všetky názvy písať malým písmenom
- Názov by mal byť samopopisný

## BEM metodika
- Odporúčam používať BEM metodiku http://getbem.com/


# CSS štýly editora

Editor vo výberovom poli Štýly zobrazuje CSS štýly nájdené v CSS súbore. Parser CSS je ale veľmi jednoduchý (nedokáže sparsovať minifikované CSS). Prakticky ani nie je žiadúce zobrazovať všetky CSS štýly. Z toho dôvodu je podporované hľadanie CSS štýlov v súbore **editor.css**. Tento je hľadaný v rovnakom adresári ako hlavný CSS štýl, napr. ```/templates/INSTALL_NAME/tempname/editor.css```.

V editor.css **nepotrebujete definovať samotné CSS vlastnosti**, stačí definovať názvy CSS tried (v príklade je pre ukážku nastavená farba pozadia, v realite by ste ale mali mať CSS vlastnosti priamo v ```.scss``` súboroch).

## Základné CSS triedy

Vo výberovom poli sa zobrazia všetky CSS triedy začínajúce na novom riadku znakom ., čiže všeobecné CSS triedy. Tie je možné aplikovať na ľubovoľný element.

```css
.styleYellow {
   background-color: yellow !important;
}
.styleBlueViolet {
   background-color: blueviolet !important;
}
```

## CSS triedy naviazané na tag

Podporované je aj naviazanie a nastavenie CSS triedy podľa špecifického tagu. Stačí CSS triedu prefixovať menom tagu. Pri zvolení takéhoto štýlu editor automaticky dohľadá v parent elementoch daný tag a CSS štýl aplikuje na tento tag. Stačí mať teda kurzor v editore kliknutý do potomka a CSS štýl sa korektne aplikuje.

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

## CSS triedy naviazané na tag a CSS triedu

Často sa používa DIV element s určitou triedou, napr. ```div.container```. Pre aplikovanie CSS tried priamo na takýto DIV je možné využiť zápis s dvoma CSS triedami. Podľa prvej sa vyhľadá parent element s CSS prvou triedou a naň sa aplikuje druhá CSS trieda.

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
## Odznačenie CSS triedy

Rovnako ako sa CSS trieda označuje je možné ju odznačiť. Ak je zvolená CSS trieda už nastavená a znova ju zvolíte, tak sa z elementu odstráni.

**Poznámka:** pri nastavení/zmene CSS triedy potrebuje editor vedieť, akú CSS triedu môže odstrániť (nemôže len nastaviť class atribút, musí zvolenú CSS triedu pridať/zameniť a ponechať CSS triedy ako container, row, pb-section a podobne). Pri nastavení hodnoty teda preiteruje všetky CSS triedy vo výberovom poli Štýly, tie odstráni a následne nastaví vybranú CSS triedu.





