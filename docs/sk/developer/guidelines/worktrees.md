# Práca s Git worktrees

Git worktree je samostatný pracovný priečinok pre jednu vetvu (branch). Worktrees zdieľajú Git repozitár, ale každý má vlastné súbory a rozpracované zmeny. Vo WebJET CMS tak môžete súčasne riešiť viac úloh, každú vo vlastnom okne VS Code, bez prepínania vetiev a odkladania rozpracovanej práce.

**Pôvodný priečinok `webjetcms` nechávajte vždy na vetve `main`.** Slúži na aktualizáciu `main` a správu worktrees. Pre každú úlohu otvorte samostatný worktree a ani v ňom neprepínajte vetvu.

## Príprava

- Majte funkčný projekt v priečinku **`webjetcms`** podľa návodu [Inštalácia a spustenie](../install/README.md).
- Príkazy spúšťajte v termináli s `Bash, Git, Node.js, npm` a dostupným príkazom `code` na otvorenie VS Code.
- Vo VS Code si nainštalujte rozšírenie **Peacock**, aby boli okná jednotlivých worktrees farebne odlíšené.

**Paletu príkazov** otvoríte cez **Cmd+Shift+P** na macOS alebo **Ctrl+Shift+P** vo Windows/Linux. Napíšte názov príkazu, napríklad `Git: Pull`, a potvrďte Enter. Nižšie používame anglické názvy príkazov VS Code.

## Otvorenie vetvy

Vo VS Code otvorte pôvodný priečinok **`webjetcms`**. Vľavo dole overte vetvu **`main`**, otvorte paletu príkazov a zvoľte **Git: Pull**.

Terminálová alternatíva, ktorá povolí iba aktualizáciu bez merge commitu:

```shell
git pull --ff-only
```

Potom cez **Terminal → New Terminal** otvorte terminál v koreni pôvodného `webjetcms`. Ak vetva **už existuje** lokálne alebo na GitHube, spustite:

```shell
./worktree-add.sh feature/123-fix-menu
```

Ak zakladáte **novú vetvu**, použite namiesto toho:

```shell
./worktree-add.sh --new feature/123-fix-menu
```

Názov nahraďte svojou vetvou v tvare `feature/ID-short-description`. Z existujúceho pull request ho skopírujete v hlavičke na GitHube. Parameter `--new` vytvorí vetvu z aktuálneho `origin/main` stiahnutého zo servera.

Skript pripraví priečinok `../webjetcms-feature-123-fix-menu`, skopíruje nastavenia a potrebné súbory z `webjetcms`, nainštaluje závislosti, zostaví administráciu a otvorí VS Code. Prvé spustenie chvíľu trvá. Povolenie automatických úloh potvrďte cez **Allow**, aby sa otvorili pripravené terminály.

**Aj po zatvorení celého VS Code otvárajte worktree cez pôvodný `webjetcms` rovnakým príkazom bez `--new`, nie priamo otvorením priečinka.** Na otázku o kopírovaní lokálnych súborov odpovedzte **N** alebo stlačte Enter. Otvorí sa pripravený workspace. Odpoveď `Y` by prepísala jeho lokálne súbory kópiami z `webjetcms`.

## Práca a aktualizácia z main

Súbory upravujte a ukladajte v okne danej úlohy. Zmeny odošlete cez VS Code:

1. Otvorte **Source Control**, skontrolujte zmeny a kliknutím na **+** pri požadovaných súboroch ich pripravte na commit (**Stage Changes**).
2. Do poľa **Message** napíšte popis zmeny a v palete príkazov zvoľte **Git: Commit**.
3. Pri prvom odoslaní novej vetvy zvoľte **Git: Publish Branch...** a prípadne vyberte **origin**. Pri ďalších odoslaniach použite **Git: Push**.

Terminálová alternatíva prvého odoslania novej vetvy:

```shell
git push -u origin feature/123-fix-menu
```

Keď potrebujete do svojej vetvy doplniť zmeny z `main`:

1. Vo worktree commitnite rozpracované zmeny podľa postupu vyššie.
2. V okne pôvodného `webjetcms` na vetve `main` otvorte paletu príkazov a zvoľte **Git: Pull**.
3. Vráťte sa do okna worktree, stlačte **Cmd+Shift+P** (Windows/Linux **Ctrl+Shift+P**), zvoľte **Git: Merge...** a zo zoznamu vyberte **main**.
4. Ak vzniknú konflikty, vyriešte ich v **Source Control → Merge Changes**, upravené súbory označte cez **+** a dokončite merge cez **Git: Commit**.

Terminálová alternatíva: v pôvodnom `webjetcms` spustite `git pull --ff-only`, potom vo worktree `git merge main`.

Aktualizovaná vetva `main` je spoločná pre všetky worktrees, jej zmeny do svojej vetvy prenesiete až cez merge.

V Codex pridajte každý priečinok worktree ako **samostatný projekt** a povoľte k nemu prístup. Pomenujte ho podľa úlohy, napríklad `123-fix-menu`, aby ste ľahko našli súvisiace chaty.

## Súbežné spustenie a testy

Vo VS Code vyberte v **Run and Debug** profil a stlačte **F5**. Súbežne spustené worktrees musia používať odlišné profily:

| Profil | HTTPS port | HTTP port |
| --- | --- | --- |
| `Debug Local DB` | 443 | 80 |
| `Debug Local DB 8443` | 8443 | 8080 |
| `Debug Local DB 9443` | 9443 | 9080 |

Do súboru `/etc/hosts` (vo Windows `C:\Windows\System32\drivers\etc\hosts`) pridajte:

```text
127.0.0.1 iwcm8443.interway.sk iwcm9443.interway.sk iwcm8080.interway.sk iwcm9080.interway.sk
```

V prehliadači používajte hostname podľa portu, napríklad `https://iwcm8443.interway.sk:8443/admin/`. Rozlíšite projekty aj ich prihlasovacie cookies, ktoré samotný port neoddeľuje. Súbor `hosts` port nenastavuje, preto ho uveďte aj v URL.

V Codex bez HTTPS certifikátu použite `http://iwcm8080.interway.sk:8080/admin/` alebo `http://iwcm9080.interway.sk:9080/admin/` podľa profilu. [Spustenie z terminálu](../install/README.md#súbežné-spustenie-vo-viacerých-worktree) umožňuje nastaviť rovnaké porty.

Aj testy smerujte na správny port: v `src/test/webapp` spustite napríklad `npm run all:8443` alebo `npm run all:9443`. Tieto skripty používajú hostname `iwcm.interway.sk` s príslušným portom.

Worktrees so skopírovanou rovnakou databázovou konfiguráciou pracujú nad rovnakými údajmi.

## Odstránenie dokončeného worktree

1. V okne worktree uložte zmeny cez **Git: Commit** a odošlite ich cez **Git: Push** (novú vetvu cez **Git: Publish Branch...**). Odložte si aj lokálne súbory, ktoré Git ignoruje a chcete ich zachovať.
2. Zastavte jeho server, testy a prípadný `npm run watch`, potom zatvorte jeho okno VS Code.
3. V okne pôvodného **`webjetcms` na vetve `main`** otvorte **Terminal → New Terminal** a z koreňa projektu spustite:

   ```shell
   ./worktree-close.sh feature/123-fix-menu
   ```

4. Skontrolujte vypísaný zoznam a potvrďte odstránenie veľkým **Y**.

Skript odstráni priečinok vrátane ignorovaných súborov, VS Code workspace a **lokálnu vetvu**. Vetvu na GitHube ponechá. Kontroluje necommitnuté a nesledované súbory aj to, či sú commity v sledovanej vzdialenej vetve (alebo v `main`, ak sledovanie nie je nastavené).
