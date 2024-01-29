# Spôsob zobrazenia web stránky

Web stránka je zvyčajne definovaná nielen samotným textom konkrétnej stránky, ale aj spoločnými prvkami s inými stránkami, ako je napr. hlavička, pätička alebo menu.

![](template_layout.png)

Šablóna teda určuje rozloženie objektov stránky. Vo vrchnej časti sa nachádza hlavička, vľavo je menu, v strednej časti je navigačná lišta a samotný text web stránky a na spodku je pätička.

Jednotlivé objekty ako hlavička, pätička sú technicky tiež web stránky vo WebJETe, aby ich bolo možné jednoducho upravovať. Uložené sú v karte Systém v zozname web stránok. Šablóna následne definuje ktorá web stránka sa vloží ako hlavička, ktorá ako pätička atď.

![](disp_process.png)

Okrem toho šablóna definuje aj technické vlastnosti ako použité CSS štýly, súbor s HTML (JSP) kódom stránky atď.

Pri zobrazení web stránky sa do html šablóny na určené miesta vložia web stránky definujúce hlavičku, pätičku a menu. Následne sa vloží navigačná lišta a samotný text web stránky, čím vznikne výsledná web stránka zaslaná do internetového prehliadača návštevníka web sídla.