# Súbory v S3 úložisku

WebJET CMS môže ukladať súbory a obrázky do úložiska s rozhraním S3. Redaktori používajú existujúci prieskumník, upload aj galériu. Aplikácia pristupuje k súborom cez `IwcmFile`, `IwcmInputStream` a `IwcmOutputStream`; URL súborov zostávajú rovnaké.

## Lokálny server

Pre vývoj je pripravený [SeaweedFS](https://github.com/seaweedfs/seaweedfs) s licenciou Apache-2.0. Ide o samostatný projekt kompatibilný s S3. Docker konfigurácia používa verziu `4.55` a vytvorí bucket `webjet-autotest`:

```sh
docker compose -f .devcontainer/s3/docker-compose-s3.yml up -d --wait
```

API je dostupné na `http://localhost:9000`, prístupový kľúč je `webjet-local` a tajný kľúč `webjet-local-secret`. Port je publikovaný len na lokálnom rozhraní. Dáta zostávajú v pomenovanom Docker volume aj po zastavení kontajnera.

Integračná vrstva používa [MinIO Java SDK](https://github.com/minio/minio-java) s licenciou Apache-2.0. Klient nevyžaduje MinIO server. MinIO server má samostatný licenčný a distribučný režim; jeho aktuálny stav je uvedený v [repozitári MinIO](https://github.com/minio/minio).

## Konfigurácia WebJETu

Bucket vytvorte pred zapnutím integrácie. Prihlasovacie údaje odporúčame dodávať cez [externú konfiguráciu](external-configuration.md). Príklad hodnôt pre lokálny Docker:

```java
Constants.setString("s3Url", "http://localhost:9000");
Constants.setString("s3AccessKey", "webjet-local");
Constants.setString("s3SecretKey", "webjet-local-secret");
Constants.setString("s3BucketName", "webjet-autotest");
Constants.setString("s3UseForDirs", "/files,/images,/shared,/WEB-INF/imgcache/,/WEB-INF/formfiles/");
Constants.setBoolean("s3Enabled", true);
```

| Premenná | Predvolená hodnota | Význam |
| --- | --- | --- |
| `s3Enabled` | `false` | Zapne úložisko pre vybrané priečinky. |
| `s3UseForDirs` | `/files,/images,/shared,/WEB-INF/imgcache/,/WEB-INF/formfiles/` | Zoznam virtuálnych priečinkov oddelených čiarkou. Platí pre priečinok aj jeho potomkov, nie pre podobné prefixy, napr. `/files-old`. |
| `s3Url` | prázdna | URL S3 API vrátane protokolu a prípadného portu. |
| `s3AccessKey` | prázdna | Prístupový kľúč pri statickom prihlasovaní. |
| `s3SecretKey` | prázdna | Tajný kľúč pri statickom prihlasovaní. |
| `s3BucketName` | prázdna | Existujúci bucket. |
| `s3AwsEnable` | `false` | Použije AWS IAM poskytovateľa prihlasovacích údajov z SDK namiesto statických kľúčov. |
| `s3AwsRegion` | `eu-central-1` | Región S3 API. |
| `s3MetaCacheTtlSec` | `5` | Platnosť metadát priečinka v sekundách. Nula vypne cache. |
| `s3MetaCacheMaxDirs` | `500` | Maximálny počet priečinkov v cache. Nula vypne cache. |

Po zmene pripojenia, prihlasovacích údajov alebo parametrov cache reštartujte WebJET. Dočasné uploady v `/WEB-INF/tmp`, administrácia a spustiteľné JSP/class súbory zostávajú lokálne. Neukladajte celé webové sídlo zadaním koreňa `/`.

Zapnutie S3 nevyžaduje `enableStaticFilesExternalDir` ani zástupnú hodnotu `cloudStaticFilesDir`. Tieto existujúce premenné nemeníme: ovplyvňujú aj oddelenie databázových údajov podľa domén. Ich zmenu posudzujte samostatne, aby existujúce záznamy galérie či iných aplikácií zostali viditeľné.

### Domény a kľúče objektov

Pri vypnutom `multiDomainEnabled` sa `/files/manual.pdf` uloží ako `defaulthost/files/manual.pdf`. Pri zapnutom režime viacerých domén používa WebJET existujúce pravidlo názvu doménového priečinka: malé písmená, bez úvodného `www.`; cloudová inštalácia zachováva aj delenie podľa prvých troch písmen.

Pri viacerých doménach priečinok `/shared` a jeho cache náhľadov používajú spoločný priestor `shared`. Otvorený súbor a stream si zapamätajú doménu pri vytvorení. Úlohy spustené mimo HTTP požiadavky musia pri zapnutom `multiDomainEnabled` nastaviť `RequestBean` s doménou; chýbajúci kontext spôsobí chybu.

### Prázdny bucket

Pri migrácii sa priečinky odvodia z existujúcich kľúčov. V úplne prázdnom buckete vytvorte základné priečinky cez WebJET API po nastavení doménového kontextu, aby ich prieskumník mohol ponúknuť:

```java
for (String path : new String[] {"/files", "/images", "/images/gallery", "/shared"}) {
    IwcmFile directory = IwcmFile.fromVirtualPath(path);
    if (!directory.exists()) directory.mkdirs();
}
```

Pri viacerých doménach vykonajte inicializáciu pre každú doménu. Pri vlastnom `s3UseForDirs` inicializujte iba vybrané priečinky. Súbory sa tým nekopírujú z disku.

### Prevádzka

Účet potrebuje minimálne oprávnenia na zoznam objektov v buckete a na čítanie, zápis a mazanie objektov používaných WebJETom. Presun v S3 kopíruje objekty na nové kľúče a až potom maže zdroj. Presun celého priečinka nie je transakcia: pri výpadku môžu zostať aj čiastočné cieľové kópie, pričom chyba sa oznámi volajúcemu.

Zápis sa najprv uloží do jedinečného dočasného súboru JVM a publikuje pri úspešnom `close()`. Lokálny dočasný disk musí mať kapacitu na súbežné uploady. RAM sa nepoužíva na celý súbor. Kód, ktorý pri kopírovaní zachytí chybu vstupu, musí zavolať `IwcmOutputStream.abort()`, aby nepublikoval neúplný obsah; `FileTools.copyFile` to rieši automaticky.

Zmeny vykonané týmto uzlom rušia cache metadát. Iný uzol alebo externý S3 klient môže zmenu vidieť až po uplynutí `s3MetaCacheTtlSec`. Chyba spojenia alebo oprávnení sa nesmie považovať za prázdny priečinok či chýbajúci súbor. WebJET v nakonfigurovanej S3 vetve nehľadá starú lokálnu kópiu. HTTP sťahovanie podporuje `HEAD`, `ETag`, podmienené požiadavky a bajtové rozsahy.

Vlastné rozšírenia používajúce priamo `java.io.File`, `FileInputStream` alebo `FileOutputStream` treba upraviť na WebJET API. Externé nástroje, napr. ImageMagick, potrebujú dočasnú lokálnu kópiu; jadro pri obrazových operáciách túto kópiu vytvorí a výsledok uloží späť.

## Testovanie

```sh
./gradlew test --tests 'sk.iway.iwcm.io.s3.*'
./gradlew s3IntegrationTest
```

Úloha `s3IntegrationTest` vyžaduje spustený S3 server; bežné `test` ju nespúšťa. Predvolené pripojenie zodpovedá uvedenému Dockeru. Iné testovacie úložisko zadáte premennými `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`. Používajte vyhradený testovací bucket. Testy vytvárajú a mažú jedinečné priečinky `autotest-s3-*`.

Testy kontrolujú reálne objekty v buckete, diakritiku, nulové súbory, multipart zápis, viac ako 1 000 objektov, implicitné aj prázdne priečinky, presuny, domény, chyby čítania a prístupu, HTTP rozsahy, chunk upload, úpravu obrázkov a náhľady. Overenie proti SeaweedFS nenahrádza akceptačné testy konkrétneho produkčného S3 poskytovateľa ani nasadenia AWS IAM.

Prehliadačové scenáre sú v priečinku `src/test/webapp/tests/s3`. Pri spustení testov sa pred prvým S3 scenárom overí aktuálna hodnota `s3Enabled` z konfigurácie WebJETu. Ak je `false`, testy z tohto priečinka sa preskočia. Pri hodnote `true` použijú nakonfigurované S3 pripojenie a bucket, vytvoria izolované testovacie priečinky a po overení obnovia pôvodné `s3UseForDirs`. Spúšťajte ich na lokálnom testovacom WebJETe, celý súbor vrátane čistiacich scenárov, bez súbežnej kompilácie servera:

```sh
cd src/test/webapp
CODECEPT_URL='http://iwcm.interway.sk:8080' CODECEPT_SHOW=false npx codeceptjs run tests/s3/s3-storage.js
```

Prihlásenie používa štandardnú premennú `CODECEPT_DEFAULT_PASSWORD`. Po testoch reštartujte WebJET, aby sa obnovila konfigurácia z databázy. Test presunu DB ↔ S3 používa dočasné tabuľky `file_fat` a `file_data` v jednej relácii lokálnej testovacej MariaDB; nemení schému inštalácie. IAM poskytovateľ sa overuje proti lokálnej simulácii IMDSv2.
