# Zálohování systému

Aplikace slouží k vytvoření archivu ZIP jednotlivých složek souborového systému WebJET. Můžete si zvolit, které složky mají být do archivu ZIP zahrnuty a ve které složce má být výsledný archiv ZIP vytvořen. Nevytváří se záloha databáze, tu je třeba vytvořit pomocí nástrojů pro zálohování databáze.

!> **Varování:** Množství dat ve vybraných složkách může být velké a soubor ZIP nemusí být vygenerován správně (omezení je na soubor o velikosti 2 GB). V případě potřeby můžete vytvářet zálohy po částech (jednotlivých složkách).

![](backup.png)

Tento proces může trvat několik desítek minut v závislosti na množství dat ve vybraných složkách. Počkejte na dokončení celého procesu. Během této doby byste měli v okně vidět informace o počtu již vygenerovaných stránek a celkovém počtu stránek.

Výsledkem je archiv zip vytvořený v zadané složce.
