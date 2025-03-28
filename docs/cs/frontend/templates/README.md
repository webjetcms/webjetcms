# Způsob zobrazení web stránky

Web stránka je obvykle definována nejen samotným textem konkrétní stránky, ale také společnými prvky s jinými stránkami, jako je např. hlavička, patička nebo menu.

![](template_layout.png)

Šablona tedy určuje rozložení objektů stránky. Ve vrchní části se nachází hlavička, vlevo je menu, ve střední části je navigační lišta a samotný text webové stránky a na spodku je patička.

Jednotlivé objekty jako hlavička, patička jsou technicky také web stránky ve WebJETu, aby je bylo možno snadno upravovat. Uloženy jsou v kartě Systém v seznamu web stránek. Šablona následně definuje která web stránka se vloží jako hlavička, která jako patička atp.

![](disp_process.png)

Kromě toho šablona definuje i technické vlastnosti jako použité CSS styly, soubor s HTML (JSP) kódem stránky atp.

Při zobrazení web stránky se do html šablony na určená místa vloží web stránky definující hlavičku, patičku a menu. Následně se vloží navigační lišta a samotný text web stránky, čímž vznikne výsledná web stránka zaslaná do internetového prohlížeče návštěvníka web sídla.
