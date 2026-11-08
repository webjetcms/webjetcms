# Pridanie poskytovateľa

Komunikácia s externou AI službou vrátane embeddingov patrí do samostatnej knižnice [webjet-ai](https://github.com/webjetcms/webjet-ai). Knižnica je nezávislá od frameworku a nesmie importovať triedy z `sk.iway.iwcm`, používať Spring ani čítať WebJET `Constants`.

Integrácia nového serverového poskytovateľa do projektu má tri časti:

- implementáciu `AiProvider`, ktorá zabezpečuje komunikáciu s API poskytovateľa,
- Spring bean typu `AiProvider`, cez ktorý sa implementácia pridá k vstavaným poskytovateľom,
- jednu CMS službu, ktorá rozširuje [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) a implementuje [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automaticky spojí vstavaných poskytovateľov knižnice so všetkými projektovými Spring beanmi typu `AiProvider`. Túto triedu pri pridávaní poskytovateľa neupravujte. Spracovanie domény, konfigurácia, auditovanie, štatistiky, perzistencia, makrá promptov a dočasné súbory zostávajú v správe WebJET CMS.

Identifikátor poskytovateľa je verejná konfiguračná hodnota. Musí byť stabilný, neprázdny, jedinečný a vo všetkých troch častiach úplne zhodný. Odporúčame použiť malé písmená, napríklad `acme`. Duplicitný identifikátor vrátane kolízie so vstavaným poskytovateľom spôsobí chybu pri štarte aplikácie.

## Rozdelenie zodpovedností

Pri zjednodušovaní adaptérov zachovajte nasledujúce úlohy v CMS:

| Úloha | Implementácia v CMS |
| --- | --- |
| Oprávnenia, výber asistenta a kontrola povolenej akcie | `AssistantController`, `AiService` |
| Konfigurácia poskytovateľa a doménový kontext | `WebjetAiConfigurationService`; `AiTaskRegistry` prenáša kontext do pracovného vlákna |
| Príprava CMS vstupov a makier, ochrana a obnovenie príkazov `INCLUDE` | `LibrarySupportLogic`, `IncludesHandler` |
| Audit a evidencia spotreby tokenov podľa používateľa a domény | `LibrarySupportLogic`, `AiStatService` |
| Ukladanie a odstránenie dočasných obrázkov, kontrola cieľového priečinka | `AiTempFileStorage`, `AiTempFileRemover` |
| Väzba pokračovania chatu na session, používateľa, doménu a asistenta | `AiChatSession`; prehliadač dostáva nepriehľadný token namiesto ID odpovede poskytovateľa |
| Evidencia a zastavenie úloh používateľa | `AiTaskRegistry`; knižnici odovzdáva `AiCancellationToken` |
| Vloženie odpovede do editora, práca s výberom a vrátenie zmien | `EditorAi`; prepis, koncept a obnovu rozhovoru spravuje `AiChat` |

Knižnica dostáva pripravené `AiRequest` a `AiProviderConfig`, vracia `AiResponse` a chyby oznamuje výnimkou `AiProviderException`. Zabezpečuje protokol poskytovateľa, jeho modely a metadáta schopností, prenos streamu a klasifikáciu chýb. CMS podľa týchto údajov rozhoduje o sprístupnení funkcií a zachováva nastavenia asistenta, napríklad súhlas s ukladaním rozhovoru cez `useTemporal=false`.

Spoločné JavaScriptové funkcie `requestAiText` a `parseAiResponse` zostávajú súčasťou CMS. Prvá komunikuje s REST službami CMS, druhá pripravuje obsah pre editor. Oprávnenia, session a ukladanie zmien naďalej spravujú príslušné CMS triedy uvedené v tabuľke.

## 1. Implementácia `AiProvider`

Implementáciu umiestnite do samostatnej knižnice nezávislej od WebJET CMS. Nasledujúci minimálny príklad je úplný a kompilovateľný; pevnú odpoveď nahraďte volaním API dodávateľa:

```java
package com.example.webjet.ai;

import java.util.List;

import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiProvider;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.AiStreamListener;
import com.webjetcms.ai.ModelInfo;

public final class AcmeProvider implements AiProvider {

    public static final String PROVIDER_ID = "acme";

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public List<ModelInfo> listModels(AiProviderConfig config) throws AiProviderException {
        requireConfigured(config);
        return List.of(new ModelInfo("acme-text-1", "Acme Text 1"));
    }

    @Override
    public AiResponse execute(AiRequest request, AiProviderConfig config) throws AiProviderException {
        requireConfigured(config);
        validateTextRequest(request);
        return AiResponse.text("Response from " + request.model());
    }

    @Override
    public AiResponse stream(
        AiRequest request,
        AiProviderConfig config,
        AiStreamListener listener
    ) throws AiProviderException {
        if (listener == null) {
            throw new AiProviderException(PROVIDER_ID, "Stream listener is required");
        }

        AiResponse response = execute(request, config);
        try {
            listener.onTextDelta(response.text());
        } catch (Exception exception) {
            throw new AiProviderException(PROVIDER_ID, "Stream listener failed", exception);
        }
        return response;
    }

    private static void requireConfigured(AiProviderConfig config) throws AiProviderException {
        if (config == null || config.isConfigured() == false) {
            throw new AiProviderException(PROVIDER_ID, "Acme API key is not configured");
        }
    }

    private static void validateTextRequest(AiRequest request) throws AiProviderException {
        if (request == null || request.operation() != AiOperation.TEXT) {
            throw new AiProviderException(PROVIDER_ID, "Only text requests are supported");
        }
        if (request.model() == null || request.model().isBlank()) {
            throw new AiProviderException(PROVIDER_ID, "Model is required");
        }
    }
}
```

API kľúč, koncový bod, časové limity a dôveryhodné hlavičky vstupujú do poskytovateľa iba cez nemenný objekt `AiProviderConfig`. Poskytovateľ nesmie priamo pristupovať k servletovej požiadavke, Spring službám, databáze ani ku konfigurácii CMS. Inštancia sa používa súbežne a počas celého životného cyklu `AiClient`, preto musí byť vláknovo bezpečná a má opakovane používať transportné zdroje. Vlastné zdroje uvoľnite v metóde `close()`.

Metóda `embed` má predvolenú implementáciu, ktorá oznámi, že poskytovateľ embeddingy nepodporuje. Ak má poskytovateľ fungovať so sémantickým vyhľadávaním, implementujte `AiProvider.embed(EmbeddingRequest, AiProviderConfig)`. Odpoveď musí obsahovať práve jeden `EmbeddingVector` pre každý vstup, v rovnakom poradí a s počtom dimenzií z `EmbeddingOptions`. [EmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) počet aj dimenziu vektorov kontroluje.

Úplný popis transportu, embeddingov, spracovania chýb a životného cyklu nájdete v dokumentácii [Implementing and using a custom AI provider](https://github.com/webjetcms/webjet-ai/blob/main/docs/custom-providers.md).

## 2. Registrácia Spring beanu

V projekte vytvorte konfiguračnú triedu, ktorú nájde Spring component scan, a sprístupnite implementáciu ako bean typu `AiProvider`:

```java
package com.example.webjet.cms.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webjetcms.ai.AiProvider;
import com.example.webjet.ai.AcmeProvider;

@Configuration
public class AcmeAiConfiguration {

    @Bean(destroyMethod = "")
    public AiProvider acmeAiProvider() {
        return new AcmeProvider();
    }
}
```

CMS odovzdá všetky takéto beany do `AiClient.discover(...)`, ktorý ich pridá k vstavaným poskytovateľom `openai`, `gemini` a `openrouter`. Hodnota `destroyMethod = ""` je dôležitá: životný cyklus poskytovateľa po úspešnom vytvorení klienta vlastní `AiClient`, preto Spring nemá volať `close()` druhýkrát.

Samotné vloženie JAR súboru na classpath poskytovateľa nezaregistruje. Projekt musí jeho inštanciu vždy explicitne vytvoriť ako Spring bean. Neupravujte jadrový bean `webjetAiClient` a nevytvárajte ďalší `AiClient`.

## 3. CMS adaptér a konfigurácia

Vytvorte jednu Spring službu, ktorá súčasne rozširuje `LibrarySupportLogic` a implementuje `AiAssitantsInterface`. Základná trieda zabezpečí volania `AiClient`, auditovanie, štatistiky, makrá promptov, dočasné súbory aj modelovo špecifické polia pre obrázky:

```java
package com.example.webjet.cms.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.example.webjet.ai.AcmeProvider;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

@Service
public class AcmeService extends LibrarySupportLogic implements AiAssitantsInterface {

    private static final String API_KEY = "ai_acmeAuthKey";
    private static final String IMAGE_NAME_MODEL = "ai_acme_generateFileNameModel";

    public AcmeService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService
    ) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return AcmeProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.acme.title";
    }

    @Override
    public String getApiKey() {
        return Constants.getString(API_KEY);
    }

    @Override
    public String getImageNameModel() {
        return Constants.getString(IMAGE_NAME_MODEL);
    }

    @Override
    public void configure(AiProviderConfig.Builder builder, String trustedReferer) {
        builder.trustedHeader("Referer", trustedReferer);
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) {
            return List.of("model", "useStreaming", "useTemporal");
        }
        return List.of();
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistant) {
        if (Tools.isEmpty(assistant.getModel())) {
            assistant.setModel("acme-text-1");
        }
    }

    @Override
    public void setProviderSpecificOptions(
        DatatablePageImpl<AssistantDefinitionEntity> page,
        Prop prop
    ) {
        // Add provider-specific editor options when needed.
    }
}
```

Metóda `getApiKey()` je zdroj prihlasovacích údajov aj podmienka dostupnosti poskytovateľa. Pre poskytovateľa bez API kľúča primerane prepíšte `isInit()`. `getImageNameModel()` je potrebná iba vtedy, keď CMS generuje názov uloženého obrázka cez daného poskytovateľa. Metóda `configure(...)` je voliteľná; používajte ju iba na bezpečné nastavenie vlastného koncového bodu alebo hlavičiek. Hodnota `trustedReferer` je overená v CMS. Hlavičky zadané používateľom nikdy neodovzdávajte priamo a tajné hodnoty nezapisujte do logu.

Konfiguračné premenné `ai_acmeAuthKey` a `ai_acme_generateFileNameModel` vytvorte v sekcii **Nastavenia → Konfigurácia**; API kľúč uložte zašifrovaný. Názov pridajte do všetkých prekladových súborov projektu, napríklad:

```properties
components.ai_assistants.provider.acme.title=Acme
```

[WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java) pridá spoločné časové limity, vytvorí nemenný `AiProviderConfig` a zavolá `configure(...)` na vybranom adaptéri. Nový poskytovateľ sa preto nepridáva do žiadneho `switch` bloku ani do centrálneho zoznamu konfiguračných kľúčov.

## 4. Embeddingy a sémantické vyhľadávanie

Knižnica definuje poskytovateľsky nezávislé typy `EmbeddingRequest`, `EmbeddingOptions`, `EmbeddingResponse` a `EmbeddingVector`. RAG modul CMS volá `AiClient.embed` cez [EmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) a neobsahuje HTTP klienta konkrétneho poskytovateľa.

Na použitie nového poskytovateľa pre RAG musia platiť všetky podmienky:

- `AcmeProvider` implementuje `embed(...)`,
- `AcmeProvider` je zaregistrovaný ako Spring bean,
- `AcmeService.getProviderId()` vracia rovnaký identifikátor,
- systémoví asistenti `RAG-EMB-INDEX` a `RAG-EMB-SEARCH` používajú poskytovateľa `acme`, presne rovnaký model a správnu hodnotu `ragEmbeddingDimensions`.

Predchádzajúce embedding SPI systému CMS bolo odstránené. Vlastný `EmbeddingProvider` nevytvárajte.

## 5. Možnosti generovania obrázkov

Možnosti obrázkov sa nedefinujú v CMS adaptéri. Poskytovateľ ich publikuje bez sieťového volania cez `AiProvider.imageOptions(model, operation)`. Napríklad:

```java
@Override
public Map<String, ImageOptionDefinition> imageOptions(
    String model,
    AiOperation operation
) {
    if ("acme-image-1".equals(model) && operation == AiOperation.GENERATE_IMAGE) {
        return Map.of(
            ImageOptions.COUNT, ImageOptionDefinition.integerRange(1, 4),
            ImageOptions.QUALITY, ImageOptionDefinition.choices("standard", "high"),
            "aspectRatio", ImageOptionDefinition.choices("1:1", "16:9", "9:16")
        );
    }
    return Map.of();
}
```

`LibrarySupportLogic` z metadát automaticky zobrazí iba podporované polia **Počet obrázkov**, **Rozmer**, **Kvalita** a **Pomer strán** a do požiadavky odošle iba ich podporované hodnoty. Pre prenosný rozmer použite kľúč `size`, podporovaný je aj poskytovateľský kľúč `resolution`. Pre pomer strán sú rozpoznané kľúče `aspectRatio` a `aspect_ratio`. Prázdna mapa znamená, že CMS pre daný model a operáciu doplnkové polia nezobrazí. Metadáta nesmú vyžadovať API kľúč ani sieťové volanie.

## Vstavané lokálne modely

Artefakt `com.webjetcms:webjet-ai-local` rozširuje základnú knižnicu o lokálnych poskytovateľov modelov. WebJET CMS registruje tri adaptéry:

- [LocalTextService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/text/LocalTextService.java) pre `utter-project/EuroLLM-1.7B-Instruct`,
- [LocalTranslateService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/translate/LocalTranslateService.java) pre `facebook/m2m100_418M`,
- [LocalEmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/embedding/LocalEmbeddingService.java) pre `intfloat/multilingual-e5-base`.

Každý adaptér používa samostatnú globálnu cestu k modelovému ZIP balíku. [ConfiguredLocalProvider](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/ConfiguredLocalProvider.java) načíta cestu bez doménového kontextu, poskytovateľa otvorí lenivo pri prvom volaní a jednu inštanciu bezpečne opakovane používa až do ukončenia aplikácie. Zmena cesty preto vyžaduje reštart.

Cesta môže byť absolútna cesta na serveri alebo cesta začínajúca `/WEB-INF/`, ktorú adaptér prevedie cez `Tools.getRealPath()` na fyzickú cestu v nasadenej aplikácii. Skripty opísané v dokumentácii [AI asistenti](../../../../redactor/ai/settings/README.md) vytvoria schválené balíky v priečinku `/WEB-INF/local-ai-models/`, napríklad `/WEB-INF/local-ai-models/eurollm-1.7b-instruct-q4-k-m.zip` a `/WEB-INF/local-ai-models/m2m100-418m-int8.zip`.

Lokálny preklad nepoužíva bežný prompt. [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) preň vytvorí požiadavku s `TranslationOptions` a odmietne HTML, `INCLUDE` príkazy, štruktúrovaný vstup, používateľský prompt aj makrá vstupu v inštrukciách. Inštrukcie musia obsahovať JSON s `sourceLanguage` a `targetLanguage`; voliteľná hodnota `maximumOutputTokens` je najviac `200`. Lokálne generovanie textu odmietne rozbalenie promptov, v ktorom ochrana deteguje pokus o `prompt injection`.

## Výnimka `AiInterface` iba pre prehliadač

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) priamo, pretože Chrome Built-in AI beží v prehliadači a nepoužíva serverovú komunikáciu s poskytovateľom. Toto je jediný spôsob priamej implementácie. Noví serveroví poskytovatelia musia používať `AiProvider` a `LibrarySupportLogic`.

## Lokálny vývoj

Pri súbežnom lokálnom vývoji CMS a susedného repozitára `webjet-ai` použite Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Rovnakú voľbu `--include-build ../webjet-ai` použite pri každej lokálnej zostavovacej a testovacej úlohe, ktorá má používať nezverejnené zmeny knižnice. Inak sa použije verzia `com.webjetcms:webjet-ai-local` určená premennou `webjetAiVersion` v `build.gradle`.

## `AiAssistantsService`

`AiAssistantsService` spracováva požiadavky súvisiace s dátovou tabuľkou [Asistenti](../../../../redactor/ai/settings/README.md). Pomocou dependency injection získava všetky implementácie `AiAssitantsInterface` a vyberá ich podľa identifikátora poskytovateľa.

Dôležité metódy:

- `getAssistantAndFieldFrom` – vráti asistentov, ktorí spĺňajú podmienky zobrazenia vo vybranom poli
- `getClassOptions` – vráti triedy, na ktoré môže byť asistent naviazaný
- `getFieldOptions` – vráti polia vybranej triedy
- `prepareBeforeSave` – pred uložením vyvolá zmeny špecifické pre poskytovateľa
- `getProviderSpecificOptions` – pridá do `DatatablePageImpl` možnosti špecifické pre poskytovateľa
- `getProviderFields` – vráti dodatočné polia zobrazené v editore tabuľky
- `getAssistantStatus` – oznámi, či je poskytovateľ nakonfigurovaný

## `AiService`

`AiService` spracováva požiadavky asistentov a pomocou dependency injection získava všetky CMS adaptéry `AiInterface`.

Dôležité metódy:

- `getProviders` – vráti všetkých dostupných poskytovateľov a označí nenakonfigurovaných
- `getModelOptions` – vráti modely poskytovateľa, voliteľne filtrované podľa reťazca
- `getAiResponse` – vráti úplnú textovú odpoveď
- `getAiImageResponse` – vráti obrázkovú odpoveď
- `getAiStreamResponse` – streamuje textovú odpoveď cez `BufferedWriter`
- `getBonusHtml` – vráti doplnkové polia podporované vybraným obrázkovým modelom

### Podpora chatu a streamovania

`LibrarySupportLogic` vyberá operáciu a overuje streamovanie podľa `AiClient.capabilities(providerId, model)`. Operáciu `CHAT` použije pre asistenta s akciou `LIVE_CHAT`, explicitným `useTemporal=false`, podporou chatu v adaptéri a modelom, ktorý chat výslovne neodmieta. V ostatných prípadoch zostáva operácia `TEXT`. Metadáta samy nepovoľujú ukladanie rozhovoru.

Streamovanie musí byť zapnuté v asistentovi a metadáta nesmú označovať zvolenú operáciu alebo jej streamovanie ako nepodporované. Výsledok sa odovzdá v existujúcom `DataTableAi.useStreaming` a rovnaká kontrola sa použije pri volaní streamovacej REST služby. Uložená hodnota `useStreaming` sa nemení. Neznáma podpora modelu alebo streamovania (`UNKNOWN`) požiadavku neblokuje.

Prehliadačový poskytovateľ zachováva pôvodné nastavenie. Lokálne adaptéry pri zisťovaní schopností nenačítavajú modely; naďalej platia ich obmedzenia nastavené pri ukladaní asistenta.

### Ukladané rozhovory

Globálny chat aj chat pri poli používajú `AiChatHost` zo súboru `ai/ai-chat-host.js`. Hostiteľ spravuje výber asistenta, históriu a panel súborov; jednotlivé rozhovory zobrazuje komponent `AiChat`. Hostiteľ načíta detail a odovzdá ho metóde `AiChat.restoreHistory(detail)`, ktorá obnoví správy a pravidlá pokračovania bez straty novšieho konceptu alebo príloh odmietnutej požiadavky. Spoločné štýly sú v `_md-ai-chat.scss` pod triedami `ai-chat` a `ai-chat-host`.

Zoznam dostupných asistentov dodáva `QuickSearch` cez callback `loadAssistants(signal)`, ktorý volá `GET /admin/rest/ai/assistant/chat-assistants/`. Hostiteľ zabezpečuje stav načítavania, opakovanie po chybe a zrušenie požiadavky pri zániku. Globálna ponuka vyžaduje akciu `live_chat` a explicitný token `CHAT_SEARCH` v `className`; token sa ponúka v autocomplete bez registrácie JPA entity. Možno ho kombinovať s inými entitami pomocou čiarky, bodkočiarky alebo nového riadka. Vzory ani `*` nenahrádzajú explicitnú väzbu. Pri samotnom `CHAT_SEARCH` sa prázdne `fieldTo` pri ukladaní doplní na `*`; zdrojové ani cieľové pole sa v globálnom chate nepoužíva.

Metadáta `persistentChat` povoľujú ukladanie iba aktívnym asistentom s akciou `live_chat`, explicitným `useTemporal=false`, poskytovateľom OpenAI alebo Gemini a podporou chatu v adaptéri a modeli. `AiChatService` kontroluje väzbu `CHAT_SEARCH` pri začatí aj pokračovaní globálneho rozhovoru a pri určovaní dostupnosti histórie. Po odstránení väzby zostáva história na čítanie. Rozhovory s kontextom poľa používajú pôvodné pravidlá dostupnosti v editore.

Ukladaný chat obsluhujú `AiChatController`, `AiChatService` a `AiChatStore` v CMS. Endpointy pod `/admin/rest/ai/chat` vyžadujú oprávnenie `cmp_ai_button`, prihláseného vlastníka rozhovoru a správnu doménu:

| Metóda a cesta | Účel |
| --- | --- |
| `GET /threads?page=0&query=...` | Stránkovaná história; vyhľadáva aj v texte správ, názve asistenta a poskytovateľa bez ohľadu na diakritiku. |
| `GET /threads/{id}` | Detail rozhovoru, správy, prílohy a dostupnosť pokračovania. |
| `POST /messages` | Nová správa alebo pokračovanie. JSON obsahuje `assistantId`, `requestId`, `userPrompt`, pri pokračovaní aj `threadId` a `revision` a pri opakovaní dokončeného pokusu `retryMessageId`. |
| `POST /threads/{id}/stop` | Zastavenie aktívnej požiadavky vlastníka rozhovoru. |
| `DELETE /threads/{id}` | Odstránenie odpovedí u poskytovateľa a následne lokálnej histórie s prílohami. |
| `GET /threads/{id}/attachments/{attachmentId}` | Autorizované načítanie prílohy; `download=true` vynúti stiahnutie. |

Pri prílohách používa `POST /messages` formát `multipart/form-data`: pole `message` obsahuje JSON požiadavky a polia `files` sú súbory. `AiChatAttachments` kontroluje formát a limity: 5 súborov, spolu 10 MB, 100 000 znakov extrahovaného textu a najviac 200 strán PDF. Podporované formáty sú uvedené v [používateľskom návode](../../../../redactor/ai/chat/README.md#prílohy). Text dokumentov a opis obrázkov sa použijú ako kontext; opis obrázka vytvára zvolený model. Spoločná metóda `LibrarySupportLogic.prepareChatAttachment()` vracia text jednej prílohy a spotrebu tokenov pre ukladaný aj dočasný chat. Volajúci načítava prílohy a sčítava spotrebu; pri ukladanom chate tak zostane zaznamenaná cena už opísaného obrázka aj po zlyhaní nasledujúcej prílohy.

Každý rozhovor má priečinok `/files/protected/ai-chat/<userId>/<threadId>/` so súborom `ai_chat.json`. Prílohy dostávajú poradové číslo a pôvodnú príponu, napríklad `1.docx`, `2.png`; číslovanie pokračuje naprieč správami. Pri sťahovaní sa obnoví pôvodný názov. Umiestnenie pod `/files` umožňuje zdieľanie histórie medzi nodmi clustra cez NFS. Priečinok `/files` môže byť symbolický odkaz na samostatný disk alebo zdieľané úložisko; chat používa kanonickú cestu úložiska aj pri prvom vytvorení histórie. Zápisy chráni súborový zámok vlastníka a atómová výmena JSON súboru. História a prílohy sú vylúčené z WAR, Gitu, ZIP archívov a offline exportu. Bežné súborové operácie ich nemajú sprístupňovať, premenovať ani prepísať; klient ich načítava cez autorizovaný endpoint s CSRF hlavičkou vrátane náhľadov obrázkov.

`AiChatStore` uchováva ID odpovedí poskytovateľa na serveri. `requestId` chráni pred duplicitným odoslaním a `revision` pred pokračovaním z neaktuálneho stavu. Po strate kontextu alebo zmene asistenta či konfigurácie poskytovateľa môže história zostať iba na čítanie. `AiChatService` maže uložené odpovede cez `AiClient.deleteResponse(providerId, responseId, config)` z knižnice `webjet-ai`. Výsledky `DELETED` aj `ALREADY_ABSENT` potvrdzujú, že odpoveď už u poskytovateľa nie je; až po potvrdení všetkých odpovedí CMS odstráni lokálnu históriu a prílohy. Pri výnimke zostane stav `DELETE_PENDING` a lokálne údaje sa zachovajú na opakovanie akcie, ktoré preskočí už potvrdené odpovede.

Knižnica `webjet-ai` od verzie `2.1.0` oznamuje ID cez `AiLifecycleListener` už počas spracovania odpovede. CMS ho uloží na neskoršie odstránenie aj pri chybe alebo zastavení generovania; pokračovanie rozhovoru posunie iba po úspešnom dokončení. Pri chybe používa `AiProviderException.lifecycle()`: stavy `NOT_SUBMITTED` a `REJECTED` potvrdzujú, že nevznikla odpoveď na odstránenie. Stav `UNKNOWN` alebo `RESPONSE_OBSERVED` bez dostupného ID zostáva nevyriešený a bráni odstráneniu lokálnej histórie, pokiaľ CMS nezachytil ID skôr. Samotný HTTP stav ani možnosť opakovania požiadavky nepotvrdzujú odmietnutie poskytovateľom.

Chat pri poli používa pre uložený záznam hostiteľský komponent `AiChatHost` s kontextom `AiChatContext`: `entityType` je cesta REST zdroja tabuľky bez koncového lomítka a query parametrov, `entityId` je ID upravovaného záznamu a `fieldName` je cieľové pole. `EditorAi` dodáva asistentov priamo z definície stĺpca, obsah poľa alebo označeného textu a akcie na vloženie odpovede. Globálny zoznam asistentov sa pri poli nenačítava. Nové záznamy a hromadná úprava používajú `AiChat` bez perzistencie. Editor pristupuje k obom variantom cez `conversation.view` s metódami `mount`, `unmount`, `destroy` a `isPending`; pri hostiteľovi zahŕňa `isPending` aj rozhovory bežiace na pozadí po prepnutí asistenta.

Požiadavka `POST /messages` pri poli obsahuje aj objekt `context` s týmito tromi hodnotami a `inputValue` so zdrojovým obsahom. Kontext je uložený pri rozhovore a pokračovanie ho nemôže zmeniť. Zdrojový obsah sa uloží pri otázke, aby opakovanie neúspešného pokusu použilo pôvodný vstup spolu s pôvodnými prílohami. Nová otázka dostane aktuálny obsah poľa.

História pri poli volá `GET /threads?page=0&entityType=...&entityId=...&fieldName=...`, prípadne s `query`. Server vyžaduje všetky tri filtre a porovnáva ich presne ešte pred stránkovaním. Bez filtrov vráti iba samostatné rozhovory bez kontextu. Kontext slúži na triedenie súkromnej histórie, neposkytuje prístup k samotnej entite; kontrola vlastníka a domény rozhovoru zostáva zachovaná. Pri zatvorení editora sa bežiace požiadavky zrušia a rozpísané správy zahodia, uložené rozhovory a prílohy zostanú dostupné.

Pre chatových asistentov OpenAI a Gemini podporujú endpointy `POST /admin/rest/ai/assistant/response/` a `POST /admin/rest/ai/assistant/response-stream/` aj `multipart/form-data`. Pole `message` obsahuje JSON `InputDataDTO` vrátane kontextu poľa, `userPrompt`, `assistantId`, `timestamp` a prípadného `conversationId`; opakované polia `files` obsahujú prílohy. Bez príloh zostáva požiadavka v JSON. Server používa rovnakú validáciu a extrakciu obsahu ako ukladaný chat, ale súbory drží iba počas spracovania požiadavky. Obsah príloh doplní ku kontextu poľa. Podpora príloh pri poli nevyžaduje vypnutý dočasný chat.

### Prenos priebežnej odpovede do administrácie

Textové generovanie v editore aj chat používajú `requestAiText` zo súboru `ai-rest-transport.js`. Funkcia odosiela požiadavku s CSRF tokenom, kontroluje HTTP a JSON odpoveď a pri streamovaní používa `readAiStream`. Vracia text a metadáta CMS bez úpravy obsahu. Funkcia `parseAiResponse` zo súboru `ai-response-parser.js` oddeľuje HTML a vysvetlenie pre generovanie v editore aj vloženie odpovede z chatu. Správu rozhovoru zabezpečuje `AiChat`. Pri HTTP chybách 401 a 403 zobrazí prenosová funkcia existujúce upozornenie na odhlásenie alebo chybu CSRF tokenu.

Generovanie v editore a neukladaný chat volajú `POST /admin/rest/ai/assistant/response-stream/` s hlavičkou `Accept: application/x-ndjson`. Ukladaný chat používa `POST /admin/rest/ai/chat/messages` s rovnakou hlavičkou; pred textom odošle aj správu `metadata` s identifikátormi rozhovoru a správ a aktuálnou revíziou. Server posiela samostatné JSON správy oddelené novým riadkom:

```json
{"type":"delta","text":"Prvá časť odpovede.\n"}
{"type":"delta","text":"Pokračovanie odpovede."}
{"type":"result","result":{"response":"Prvá časť odpovede.\nPokračovanie odpovede.","totalTokens":42,"error":null,"tempFiles":[],"generatedFileName":null}}
```

Správy `delta` obsahujú iba generovaný text. Záverečná správa `result` obsahuje `AssistantResponseDTO` vrátane spotreby tokenov a prípadnej chyby. Klient skladá neúplné riadky medzi sieťovými blokmi a spracuje aj viac správ v jednom bloku. Odpoveď považuje za úspešnú až po prijatí záverečnej správy bez chyby a ukončení prenosu.

Bez explicitnej požiadavky na `application/x-ndjson` zostáva na endpointoch `response-stream` pre starších klientov pôvodný formát `text/plain`; endpoint `/chat/messages` vracia bežnú JSON odpoveď. CMS adaptéry naďalej zapisujú obyčajný text do `BufferedWriter`; oddelenie správ zabezpečujú controllery cez `AssistantStreamWriter`, bez zmeny rozhrania knižnice `webjet-ai`.

### Chyby poskytovateľa

Chybová odpoveď zachováva textové pole `error` a môže obsahovať aj `errorCategory`, `providerStatusCode` a `retryable`. Rovnaké polia sú v bežnej JSON odpovedi aj v záverečnej správe `result` streamu. `providerStatusCode` je kladný HTTP stav externej služby, nie CMS endpointu; pri neznámej hodnote sa neposiela. `retryable` je odporúčanie poskytovateľa (`true` alebo `false`), pri chýbajúcej informácii sa neposiela. Pri hodnote `false` chat neponúkne opakovanie rovnakej požiadavky; zastavenú odpoveď môže používateľ zopakovať. Úplná odpoveď poskytovateľa ani príčina výnimky sa do prehliadača neposielajú.

| `errorCategory` | Správanie chatu |
| --- | --- |
| `CANCELLED` | Zobrazí zastavenú odpoveď a zachová posledné úspešné pokračovanie. |
| `INVALID_CONTINUATION` | Ponúkne začatie nového rozhovoru. Až po voľbe používateľa zruší neplatné pokračovanie; prepis správ a rozpracovaný text zachová. |
| `UNSUPPORTED_OPERATION` | Vysvetlí potrebu zmeniť asistenta alebo model namiesto opakovania rovnakej požiadavky. |
| `UNKNOWN` | Použije bežné spracovanie chyby; rovnako spracuje neznámu budúcu kategóriu. |

Chat požiadavky automaticky neopakuje ani nezačína nový rozhovor. Kategórie určuje knižnica podľa spoľahlivých údajov poskytovateľa, nie podľa textu chybovej správy. Vzdialené chyby pokračovania Gemini preto zatiaľ zostávajú `UNKNOWN`. Rovnako sa spracujú existujúce validačné chyby CMS, napríklad expirovaný alebo neplatný token rozhovoru v CMS session.

### Zastavenie požiadavky

`AiTaskRegistry` vytvára pre každú úlohu samostatný `AiCancellationToken`. Tlačidlo **Zastaviť**, začatie nového chatu alebo zatvorenie editora zruší cez existujúcu službu `stop-assistant` token aj `Future` príslušnej úlohy. `LibrarySupportLogic` odovzdáva token knižnici pri textových, chatových a obrázkových požiadavkách vrátane následného generovania názvu obrázka. Ostatné súbežné požiadavky zostávajú aktívne.

OpenAI, Gemini a OpenRouter prerušia HTTP požiadavku vrátane čakania na ďalšiu časť streamu. Zastavený chat si zachová posledné úspešné pokračovanie; čiastočná odpoveď ho nenahradí. Samotné zatvorenie okna chatu iba skryje jeho obsah a bežiacu požiadavku nezastaví.

Vlastný poskytovateľ môže prekryť varianty `execute` a `stream` s parametrom `AiCancellationToken` a pripojiť k nemu zrušenie svojho transportu. Predvolené implementácie vrátane lokálnych modelov kontrolujú zrušenie pred a po vykonaní a pri odovzdávaní častí odpovede; nedokážu vynútiť prerušenie blokovaného natívneho výpočtu. Zrušenie HTTP spojenia nezaručuje zastavenie spracovania alebo účtovania na strane externej služby a nemaže už uložený kontext rozhovoru.
