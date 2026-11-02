# Práce s Git worktrees

Git worktree je samostatná pracovní složka pro jednu větev (branch). Worktrees sdílejí Git repozitář, ale každý má vlastní soubory a rozpracované změny. Ve WebJET CMS tak můžete současně řešit více úkolů, každý ve vlastním okně VS Code, bez přepínání větví a odkládání rozpracované práce.

**Původní složku `webjetcms` nechávejte vždy na větvi `main`.** Slouží k aktualizaci `main` a správě worktrees. Pro každý úkol otevřete samostatný worktree a ani v něm nepřepínejte větev.

## Příprava

- Mějte funkční projekt ve složce **`webjetcms`** podle návodu [Instalace a spuštění](../install/README.md).
- Příkazy spouštějte v terminálu s `Bash, Git, Node.js, npm` a dostupným příkazem `code` k otevření VS Code.
- Ve VS Code si nainstalujte rozšíření **Peacock**, aby byla okna jednotlivých worktrees barevně odlišena.

**Paletu příkazů** otevřete přes **Cmd+Shift+P** na macOS nebo **Ctrl+Shift+P** ve Windows/Linux. Napište název příkazu, například `Git: Pull`, a potvrďte Enter. Níže používáme anglické názvy příkazů VS Code.

## Otevření větve

Ve VS Code otevřete původní složku **`webjetcms`**. Vlevo dole ověřte větev **`main`**, otevřete paletu příkazů a zvolte **Git: Pull**.

Terminálová alternativa, která povolí pouze aktualizaci bez merge commitu:

```shell
git pull --ff-only
```

Potom přes **Terminal → New Terminal** otevřete terminál v kořeni původního `webjetcms`. Pokud větev **již existuje** lokálně nebo na GitHube, spusťte:

```shell
./worktree-add.sh feature/123-fix-menu
```

Pokud zakládáte **novou větev**, použijte místo toho:

```shell
./worktree-add.sh --new feature/123-fix-menu
```

Název nahraďte svou větví ve tvaru `feature/ID-short-description`. Ze stávajícího pull request jej zkopírujete v hlavičce na GitHube. Parametr `--new` vytvoří větev z aktuálního `origin/main` staženého ze serveru.

Skript připraví složku `../webjetcms-feature-123-fix-menu`, zkopíruje nastavení a potřebné soubory z `webjetcms`, nainstaluje závislosti, sestaví administraci a otevře VS Code. První spuštění chvíli trvá. Povolení automatických úloh potvrďte přes **Allow**, aby se otevřely připravené terminály.

**I po zavření celého VS Code otevírejte worktree přes původní `webjetcms` stejným příkazem bez `--new`, ne přímo otevřením složky.** Na otázku o kopírování lokálních souborů odpovězte **N** nebo stiskněte Enter. Otevře se připravený workspace. Odpověď `Y` by přepsala jeho lokální soubory kopiemi z `webjetcms`.

## Práce a aktualizace z main

Soubory upravujte a ukládejte v okně daného úkolu. Změny odešlete přes VS Code:

1. Otevřete **Source Control**, zkontrolujte změny a kliknutím na **+** u požadovaných souborů je připravte na commit (**Stage Changes**).
2. Do pole **Message** napište popis změny a v paletě příkazů zvolte **Git: Commit**.
3. Při prvním odeslání nové větve zvolte **Git: Publish Branch...** a případně vyberte **origin**. Při dalších odesláních použijte **Git: Push**.

Terminálová alternativa prvního odeslání nové větve:

```shell
git push -u origin feature/123-fix-menu
```

Když potřebujete do své větve doplnit změny z `main`:

1. Ve worktree commitnite rozpracované změny podle postupu výše.
2. V okně původního `webjetcms` na větvi `main` otevřete paletu příkazů a zvolte **Git: Pull**.
3. Vraťte se do okna worktree, stiskněte **Cmd+Shift+P** (Windows/Linux **Ctrl+Shift+P**), zvolte **Git: Merge...** a ze seznamu vyberte **main**.
4. Pokud vzniknou konflikty, vyřešte je v **Source Control → Merge Changes**, upravené soubory označte přes **+** a dokončete merge přes **Git: Commit**.

Terminálová alternativa: v původním `webjetcms` spusťte `git pull --ff-only`, poté ve worktree `git merge main`.

Aktualizovaná větev `main` je společná pro všechny worktrees, její změny do své větve přenesete až přes merge.

V Codex přidejte každou složku worktree jako **samostatný projekt** a povolte k ní přístup. Pojmenujte jej podle úkolu, například `123-fix-menu`, abyste snadno našli související chaty.

## Souběžné spuštění a testy

Ve VS Code vyberte v **Run and Debug** profil a stiskněte **F5**. Souběžně spuštěné worktrees musí používat odlišné profily:

| Profil | HTTPS port | HTTP port |
| --- | --- | --- |
| `Debug Local DB` | 443 | 80 |
| `Debug Local DB 8443` | 8443 | 8080 |
| `Debug Local DB 9443` | 9443 | 9080 |

Do souboru `/etc/hosts` (ve Windows `C:\Windows\System32\drivers\etc\hosts`) přidejte:

```text
127.0.0.1 iwcm8443.interway.sk iwcm9443.interway.sk iwcm8080.interway.sk iwcm9080.interway.sk
```

V prohlížeči používejte hostname podle portu, například `https://iwcm8443.interway.sk:8443/admin/`. Rozlišíte projekty i jejich přihlašovací cookies, které samotný port neodděluje. Soubor `hosts` port nenastavuje, proto jej uveďte iv URL.

V Codex bez HTTPS certifikátu použijte `http://iwcm8080.interway.sk:8080/admin/` nebo `http://iwcm9080.interway.sk:9080/admin/` podle profilu. [Spuštění z terminálu](../install/README.md#souběžné-spuštění-ve-více-worktree) umožňuje nastavit stejné porty.

Také testy směřujte na správný port: v `src/test/webapp` spusťte například `npm run all:8443` nebo `npm run all:9443`. Tyto skripty používají hostname `iwcm.interway.sk` s příslušným portem.

Worktrees se zkopírovanou stejnou databázovou konfigurací pracují nad stejnými údaji.

## Odstranění dokončeného worktree

1. V okně worktree uložte změny přes **Git: Commit** a odešlete je přes **Git: Push** (novou větev přes **Git: Publish Branch...**). Odložte si také lokální soubory, které Git ignoruje a chcete je zachovat.
2. Zastavte jeho server, testy a případný `npm run watch`, poté zavřete jeho okno VS Code.
3. V okně původního **`webjetcms` na větvi `main`** otevřete **Terminal → New Terminal** az kořene projektu spusťte:

   ```shell
   ./worktree-close.sh feature/123-fix-menu
   ```

4. Zkontrolujte vypsaný seznam a potvrďte odstranění velkým **Y**.

Skript odstraní složku včetně ignorovaných souborů, VS Code workspace a **lokální větev**. Větev na GitHube ponechá. Kontroluje necommitnuté a nesledované soubory i to, zda jsou commity ve sledované vzdálené větvi (nebo v `main`, pokud sledování není nastaveno).
