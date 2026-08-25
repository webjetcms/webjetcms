# Pridanie poskytovateľa

Komunikácia s externou AI službou vrátane embeddingov patrí do samostatnej knižnice [webjet-ai](https://github.com/webjetcms/webjet-ai). Knižnica je nezávislá od frameworku a nesmie importovať triedy z `sk.iway.iwcm`, používať Spring ani čítať WebJET `Constants`.

Integrácia nového serverového poskytovateľa do projektu má tri časti:

- implementáciu `AiProvider`, ktorá zabezpečuje komunikáciu s API poskytovateľa,
- Spring bean typu `AiProvider`, cez ktorý sa implementácia pridá k vstavaným poskytovateľom,
- jednu CMS službu, ktorá rozširuje [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) a implementuje [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automaticky spojí vstavaných poskytovateľov knižnice so všetkými projektovými Spring beanmi typu `AiProvider`. Túto triedu pri pridávaní poskytovateľa neupravujte. Spracovanie domény, konfigurácia, auditovanie, štatistiky, perzistencia, makrá promptov a dočasné súbory zostávajú v správe WebJET CMS.

Identifikátor poskytovateľa je verejná konfiguračná hodnota. Musí byť stabilný, neprázdny, jedinečný a vo všetkých troch častiach úplne zhodný. Odporúčame použiť malé písmená, napríklad `acme`. Duplicitný identifikátor vrátane kolízie so vstavaným poskytovateľom spôsobí chybu pri štarte aplikácie.

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

## Výnimka `AiInterface` iba pre prehliadač

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) priamo, pretože Chrome Built-in AI beží v prehliadači a nepoužíva serverovú komunikáciu s poskytovateľom. Toto je jediný spôsob priamej implementácie. Noví serveroví poskytovatelia musia používať `AiProvider` a `LibrarySupportLogic`.

## Lokálny vývoj

Pri súbežnom lokálnom vývoji CMS a susedného repozitára `webjet-ai` použite Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Rovnakú voľbu `--include-build ../webjet-ai` použite pri každej lokálnej zostavovacej a testovacej úlohe, ktorá má používať nezverejnené zmeny knižnice. Inak sa použije verzia `com.webjetcms:webjet-ai` určená premennou `webjetAiVersion` v `build.gradle`.

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
