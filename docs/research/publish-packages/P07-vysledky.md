# P07 — Plánovanie a dokončenie UI

Stav: **DONE na uvedenom POC profile**, 9. november 2026. Vetva `feature/58858-publishing-packages`, východisko `bf9cdf633` (P06). Rozsah iba P07; bez commitov, produkčného nasadenia a P08.

## Implementácia

- Detail balíka ponúka spoločný termín, jeho posun jednou operáciou a zrušenie plánovania. `SCHEDULED` je uzavretý pre WORK zápisy aj rozhodnutia LIVE/WORK. Zrušenie plánu vráti `DRAFT`, zachová obsah a rozhodnutia. Individuálne plány sa nemenia.
- `POST /schedule` používa `{expectedRevision, preparationHash, publishAt}` s časom v epoch milisekundách; podporuje `DRAFT` aj posun `SCHEDULED`. `/unschedule` a `/reopen` prijímajú `{expectedRevision}`. Revízia a zamknutie celého balíka chránia posun/zrušenie pred prevzatím schedulerom. Revízia sa pri naplánovaní zvýši; uzavretý obsah sa do spustenia nesmie meniť. Cron použije aktuálnu revíziu na podmienené prevzatie.
- `BranchScheduler.main` je bežná WebJET cron úloha každú minútu. Nevytvára ďalší časovač ani publisher. Výber uzlov používa existujúce `CronTask.clusterNode`: nastavte **`all-admin`**, prípadne konkrétny stabilný admin uzol. Presné meno sa nevyžaduje, takže skupina `all-admin` funguje aj s dynamickými názvami v Kubernetes. `ClusterDB.isPublicNode()` navyše odmietne beh na public uzle. Náhodné oneskorenie 0–10 sekúnd rozloží začiatky admin behov; samotná ochrana pred duplicitou zostáva v transakčnom prevzatí balíka podľa stavu a revízie. Funkcia zostáva predvolene vypnutá. MariaDB migrácia pridáva jedinú cron úlohu, ktorá pri vypnutom `branchesEnabled` nerobí nič.
- Pri splatnosti načíta `scheduled_by` cez existujúce DB API, preverí aktívneho administrátora a aktuálne práva, vytvorí explicitný publikačný kontext bez HTTP session a použije rovnaký `BranchMergeService`/`BranchPublishService` ako ručný merge. Opakuje kompletný preflight vrátane revízie, LIVE/WORK, súborov a individuálnych plánov. Zostáva krátka transakcia prevzatia a P06 ochrana pred dvojitým spustením.
- Problém pred prvým obsahovým zápisom nastaví `NEEDS_ATTENTION` s konkrétnym dôvodom. Aj zmena medzi prevzatím a prvým zápisom sa preverí. Po obsahovom/checkpointovanom súborovom kroku sa použije `MERGE_FAILED` a P06 pokračovanie. Technické NOOP checkpointy sa nepovažujú za čiastočne publikovaný obsah.
- Náhľad Noviniek používa jeden `previewAt` pre celý request: budúci termín `SCHEDULED`, inak aktuálny čas. Lišta zobrazuje čas/pásmo a rozsah predikcie. Posun sa prejaví po obnovení náhľadu; zrušenie a `NEEDS_ATTENTION` používajú aktuálny čas. Autor, SAVE_DATE a DATE sa publikovaním neprepisujú.
- SK/CZ/EN hlásenia, natívny označený dátum/čas, klávesnicové odoslanie a návrat fokusu, mobilné rozloženie. Detail naplánovaného balíka obnovuje stav každých päť sekúnd, počas formulára polling pozastaví.

## Prostredie a izolácia

MariaDB 12.2.2/InnoDB, Connector/J 3.5.10, Java 17.0.18, jedna inštancia `127.0.0.1:33316/webjet_p01`, `poolman-local-p01.xml`, lokálne súkromné súbory `/tmp/webjet-p03-private`, šablóna `aceintegration/jet/blank.jsp`. Pred štartom overené DB PID 76342 a WebJET PID 28347 na 8086/8446. Pri Java zápisových testoch je WebJET zastavený. Pracovná DB 3306 sa nepoužíva.

P06 demo #1315 a P05 #841/#842 sa nepoužívajú na P07 publikovanie. Kontrolný dump metadát a všetkých ich WORK riadkov pred testami: SHA-256 `09f01fa48a1256ac1733022b602a4c191316192eaa070fdbd188534dfc4464c9`.

## Overenie

- **Java: 59 PASS / 0 zlyhaní / 4 zámerné fixture skipy** v spoločnom behu `BranchMergeTest`, `BranchNewsRenderTest`, `BranchFilterTest`, `BranchViewTest`, `BranchMergeLabelsTest`. Z toho 9 nových P07 testov; pri poslednej kontrole pribudol desiaty pre odobratie prístupu k doméne pri súborovom balíku. Následný finálny cielený beh **10/10 PASS**, vrátane explicitného zachovania neprázdneho `publish_start` (DATE), autora a SAVE_DATE. P06 regresie zahŕňajú schvaľovanie, kolíziu súboru s plánom mimo balíka, obsah/históriu/cache, zmenu produkcie, checkpoint a pokračovanie.
- **T12:** termín/posun/zrušenie bez zmeny WORK; zastaraná revízia/hash a zápis do uzavretej vetvy odmietnuté. Cron neprevezme budúci termín; pôvodný test presného uzla bol po review nahradený kontrolou zákazu behu na public uzle. Aktuálne DB odobratie `pageSave` alebo doménového prístupu (aj pri balíku iba so súborom), vypnutie účtu, nový konflikt a individuálny plán zastavia celý balík s nulovým obsahovým zápisom; individuálny plán zostal zachovaný.
- **T13/T15 v rozsahu P07:** rovnaké publikačné služby a P06 preflight; zmena po prevzatí/pred prvým zápisom dá `NEEDS_ATTENTION`. Chyba po troch stránkach dá `MERGE_FAILED`, pokračovanie dokončí balík bez druhej histórie prvej stránky. Druhé prevzatie a zmena už prevzatého plánu odmietnuté.
- **T16/N12:** skutočný SQL/NewsActionBean/Velocity test aj reálne UI. Posun cez hranicu začiatku/konca zmení ID v zozname a banner; zrušenie používa aktuálny čas. Všetky výskyty v requeste majú rovnaký čas; `NEEDS_ATTENTION` ani minulý termín neposúvajú čas. Publikovanie nemení autora/SAVE_DATE/DATE.
- **Skutočné UI a cron:** balík **#1786**, 20 stránok/25 položiek, cez UI naplánovaný na **9.11.2026 14:42 Europe/Bratislava** a jednou operáciou posunutý na **14:44**. V pôvodnom termíne mal `SCHEDULED` a **0 aplikovaných položiek vo všetkých štyroch WORK tabuľkách**; verejný súbor stále BASE. Existujúci cron ho dokončil **14:44:01**, bez online testovacej session. Nevolalo sa ručné `BranchScheduler.main` ani `/merge` pre tento balík. Všetkých 25 položiek DONE, stabilné ID a jediná história stránok overené.
- **Ručné porovnanie:** samostatný balík **#1787** sa publikoval cez existujúce UI o 14:39:32. Pre každý balík sa celé HTML Noviniek/menu pred merge a po merge presne zhoduje; časované a ručné výsledky sú ekvivalentné aj pre autora, SAVE_DATE, perex, odkazy a médiá. Pri porovnaní odlišných fixture sa normalizujú ich vlastné URL a ID v CSS triedach; pri porovnaní každej fixture pred/po zostávajú jej ID presné.
- **Zastavenie:** balík **#1788** bol cez UI naplánovaný na 14:42. Potom sa cez bežný produkčný editor zmenil názov stránky **#1947975**; editor upravil aj jej default priečinok. Cron nastavil `NEEDS_ATTENTION`, dôvod menuje `document:1947975` a `group:1943721`. Všetkých 25 položiek zostáva PENDING bez fázy/aplikovaného hashu, súbor BASE. CMS ukazuje dôvod a **Obnoviť prípravu**.
- **Finálny UI overovací beh: 2/2 PASS** na finálnom serveri, s pokračovaním už vykonaného cron behu. Natívny formulár sa overil aj na **390×844** a predtým 700×900; označenie, pásmo, hranice formulára, fokus, Enter a Escape. Preklady SK/CZ/EN sú doplnené; reálny browser bežal v SK. Frontend production build úspešný s 2 existujúcimi size warnings; Node syntax, XML migrácia a `git diff --check` prešli. Nezávislá read-only kontrola backendu nenašla blocker.
- **Izolácia:** dump P05 #841/#842 a P06 #1315 pred/po bajtovo zhodný, rovnaký SHA-256 uvedený vyššie. Demo #1315 zostáva `DRAFT`, revízia **25**, bez rozhodnutia/publikovania. Pracovná DB 3306 nedotknutá.

Dôkazy: `build/test-results/p07/java-summary.json`, `java/`, `java-final/` (10 finálnych testov), `browser/`, `browser-preview-time.json`, `browser-scheduled-before-wait.json`, `browser-scheduler-results.json`, `original-term-db.txt`, `final-db.txt`. Logy `/tmp/webjet-p07-{java,java-final,browser-5,browser-cron-verify,browser-review-final,browser-delivery,java-domain,app,app-final,app-review,frontend}.log`.

### Rozlíšenie pokusov a obmedzenie anonymného renderu

Prvé browser pokusy odhalili chyby v testovacom ovládaní: `fillField` písal ISO dátum po segmentoch, fragmentová navigácia neobnovila stránku a názov LIVE stránky bol na neotvorenej základnej karte. Opravený test používa natívne Playwright `fill` iba pre `datetime-local`, obnovenie stránky a existujúcu kartu editora. Nedokončené termíny boli cez UI zrušené pred opakovaním. Naplánovanie/posun, LIVE konflikt a ručný merge v úspešnom prípravnom behu zostali uložené; finálny test na ne nadviazal cez `WEBJET_P07_RESUME=true`, bez opakovania publikovania.

Anonymné stránkové URL reprezentatívnej POC fixture vracali 404 aj pred publikovaním, hoci prihlásený LIVE render fungoval. Toto P07 nemení. Preto dôkaz behu po odhlásení tvorí reálna cron úloha, DB checkpointy/časy a anonymne dostupný súbor (BASE v pôvodnom termíne, WORK v novom). Až potom sa test znova prihlásil a porovnal kompletné LIVE News/menu s rovnakou identitou ako náhľad a ručný merge. **Anonymný HTML render tejto fixture nie je deklarovaný ako overený.**

## Ako si plánovanie vyskúšať

Pri pôvodnom odovzdaní bežal WebJET PID **38836**, HTTP 8086 / HTTPS 8446. Pred review 10. novembra boli WebJET aj izolovaná DB vypnuté; po testoch zostávajú opäť vypnuté. Štart je v [P04 výsledkoch](P04-vysledky.md#opakovanie-testov-a-štart). Login `tester-p05`; heslo iba v existujúcom `/tmp/webjet-p05-login.properties`. Host `127.0.0.1` oddeľuje browser cookies od pracovného WebJETu na štandardnom porte.

1. Otvorte [P07 demo #1789](http://127.0.0.1:8086/admin/v9/webpages/web-pages-list/?groupid=1943724#branchCompare=1789), prihláste sa a prípadne odkaz otvorte znova. Demo je `DRAFT`, má vlastné P07 údaje a nepublikovalo sa.
2. Kliknite **Skontrolovať zmeny → Naplánovať publikovanie**. Pre zmenu zoznamu Noviniek zvoľte **1.12.2026 08:59**. [Náhľad](http://127.0.0.1:8086/admin/webpages/branch-preview/1789/p06-autotest-p07-preview-6aec1a97/) ukáže čas/pásmo; novinka WORK 0 ešte chýba, WORK 1 je prítomná.
3. Cez **Zmeniť termín** posuňte na **1.12.2026 09:01** a náhľad obnovte. WORK 0 pribudne, WORK 1 zmizne. Na zmenu obsahu treba najprv zrušiť plánovanie.
4. **Zrušiť plánovanie** vráti prípravu a aktuálny čas náhľadu so zachovanými zmenami. Pre rýchly skutočný cron pokus možno namiesto decembra zvoliť termín o niekoľko minút a odhlásiť sa; kontrola beží každú minútu. Tým sa publikuje iba táto P07 fixture na izolovanej DB.
5. [Zastavený balík #1788](http://127.0.0.1:8086/admin/v9/webpages/web-pages-list/?groupid=1943720#branchCompare=1788) ukáže konkrétny dôvod. [Dokončený časovaný balík #1786](http://127.0.0.1:8086/admin/v9/webpages/web-pages-list/?groupid=1943712#branchCompare=1786) ukáže priebeh. P06 demo #1315 ostalo pripravené na pôvodné ručné vyskúšanie.

## Opakovanie testov

Java iba pri zastavenom POC WebJETe, s bežiacou DB 33316:

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem WEBJET_P07_DB=true WEBJET_P03_DB=true \
  ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchMergeTest' \
  --tests 'sk.iway.iwcm.branch.BranchNewsRenderTest' \
  --tests 'sk.iway.iwcm.branch.BranchFilterTest' \
  --tests 'sk.iway.iwcm.branch.BranchViewTest' \
  --tests 'sk.iway.iwcm.branch.BranchMergeLabelsTest'
```

Browser zo `src/test/webapp`, s dostupným serverom:

```sh
WEBJET_P07_BROWSER=true WEBJET_P07_RESUME=true \
CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p07.js --steps
```

Tento príkaz znova overí zachovanú DRAFT preview fixture a výsledky už vykonaného cron behu. Nové skutočné plánovanie vyžaduje nové P07 fixture a beh bez `WEBJET_P07_RESUME`. Jednorazovú prípravu `BranchMergeTest.prepareP07BrowserFixtures` povoľuje `WEBJET_P07_FIXTURE=true`; existujúce manifesty zámerne neprepisuje. Archivovať možno iba štyri P07 manifesty; P06 demo ani P05 manifesty neregenerovať.

## Hranice

Iba uvedený MariaDB/InnoDB profil, jeden publikačný uzol a lokálny filesystem. Cluster/failover, iné DB, externé súborové úložiská/CDN a ľubovoľné zákaznícke hooky nie sú runtime overené. Cron kontroluje splatné balíky každú minútu; termín nie je záruka zverejnenia všetkých položiek v jednej sekunde. Publikovanie je postupné, bez globálneho rollbacku. Po páde procesu v `MERGING` zostáva manuálna kontrola a pokračovanie P06; nepridáva sa automatický failover ani retry framework. Pred začatím si plán môže vyžiadať nové porovnanie po zmene produkcie.

Hlavný agent a najviac dvaja pomocníci so zdedeným nastavením; presný model/reasoning neoverený.


## Úprava po review: admin cron bez presného mena uzla (10. november 2026)

Výber uzlov teraz patrí iba existujúcemu nastaveniu cron úlohy **Beží na uzle** (`CronTask.clusterNode`). Nastavte **`all-admin`**, prípadne konkrétny stabilný admin uzol. Aj pri Kubernetes/dynamických názvoch stačí `all-admin` a správny `clusterMyNodeType`; presné meno nie je podmienkou naplánovania balíka. Odstránená samostatná premenná pre publikačný uzol, obe jej kontroly a nepoužitý preklad hlásenia. Predvolený záznam migrácie aj existujúca POC úloha #14 používajú `all-admin`.

`BranchScheduler` navyše cez `ClusterDB.isPublicNode()` skončí na public uzle ešte pred DB operáciami. Podľa existujúceho `FileArchivatorInsertLater` začne na admin uzle po náhodnom oneskorení 0–10 sekúnd. Prerušenie čakania zachová interrupt flag a beh ukončí. Oneskorenie iba rozloží začiatky behov; duplicitnému vykonaniu toho istého balíka bráni existujúce transakčné prevzatie `SCHEDULED → MERGING` podľa stavu a revízie.

**Overenie: 22/22 PASS, 0 zlyhaní/preskočení.** Jedenásť DB P07 testov vrátane dvoch súbežných kandidátov s nezávislými DB spojeniami (práve jedno úspešné prevzatie a jedna história stránky), tri testy hraníc scheduleru (public uzol, vypnutá funkcia, prerušené čakanie) a osem existujúcich testov cron filtra/cluster príkazov. Profil iba `127.0.0.1:33316/webjet_p01`, bez app servera; produkčná DB 3306 nedotknutá. Nejde o runtime overenie celého Kubernetes/viacuzlového nasadenia ani filesystem/cache účinkov medzi uzlami. P06 demo #1315 zostáva DRAFT/revízia 25.

Príkaz: `WEBJET_P07_DB=true ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml --tests 'sk.iway.iwcm.branch.BranchSchedulerTest' --tests 'sk.iway.iwcm.branch.BranchMergeTest.p07*' --tests 'sk.iway.iwcm.system.cluster.ClusterCronTaskJUnitTest'` s rovnakým Java 17. Reporty `build/test-results/p07/cron-review/`, log `/tmp/webjet-p07-cron-review.log`. UI sa nemenilo a browser testy sa pri tejto backendovej úprave znovu nespúšťali. Zmeny zostávajú necommitnuté; P08 sa nezačal.
