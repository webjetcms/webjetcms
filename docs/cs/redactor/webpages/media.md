# Média

## Zobrazení na webové stránce

Média slouží k přiřazení souvisejících souborů/obrázků/odkazů k aktuální stránce.

Zobrazují se na kartě Média v editoru webové stránky, kde můžete zadat Název, Skupinu (média lze podle potřeby roztřídit do více skupin), odkaz na soubor/stránku, náhledový obrázek (je-li třeba) a prioritu rozložení.

Média lze použít k různým účelům:
- seznam souvisejících souborů se stránkou
- seznam souvisejících stránek s aktuální stránkou
- seznam audio/video souborů pro stránku (obrázky, animace, videa).

![](media.png)

Na stránce/šabloně můžete v aplikaci Média získat seznam médií podle ID stránky a skupiny médií.

Poznámka: aplikace Média zobrazuje pouze média:
- Odkazování na existující soubor (pokud je soubor odstraněn, médium se automaticky přestane zobrazovat).
- Vloženo před datum a čas uložení webové stránky (po přidání nových médií uložte webovou stránku, abyste ji mohli zobrazit). To umožňuje načasovat zobrazení nových médií - stačí je přidat na webovou stránku, nastavit časové zobrazení do budoucna a přidaná média se zobrazí po včasném zveřejnění webové stránky.

## Správa všech médií

V nabídce Webové stránky/Média/Správa všech médií můžete vyhledávat a spravovat všechna média. **na všech webových stránkách** v aktuálně zobrazené doméně. Při filtrování můžete zadat název webové stránky, celou cestu nebo dokonce ID stránky (docid) přímo do pole Webová stránka.

![](media-all.png)

Při úpravě/vytváření nového média je nutné zadat název média a vybrat webovou stránku pomocí stromové struktury.

![](media-all-editor.png)

Zobrazit tuto možnost **vyžadováno zákonem** "Média - Správa všech médií".

## Správa mediálních skupin

Zadaná média můžete uspořádat do skupin pomocí Skupin médií. Například "Ke stažení" nebo "Související odkazy". Spravují se v nabídce Webové stránky/Média/Správa skupin a **vyžadují právo** "Média - řízení skupiny".

Pomocí aplikace Média zobrazíte média webové stránky podle vybrané skupiny médií (nebo v šabloně, např. v pravém menu nebo pod textem webové stránky).

Skupina médií může mít nastaveno omezení, aby se skupina zobrazovala pouze v určitém adresáři webových stránek.

![](media-groups.png)

## Implementační detaily

Všechny záznamy médií jsou filtrovány podle aktuálně vybrané domény.

V případě záznamů z **Správa všech médií** parametr název tabulky je nastaven na hodnotu **dokumenty** automaticky na pozadí. K určení, zda je datová tabulka volána z této sekce, slouží parametr url `isCalledFromTable=true`.
