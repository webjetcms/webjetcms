# Přidání poskytovatele

Komunikace s externí AI službou včetně embeddingů patří do samostatné knihovny [webjet-ai](https://github.com/webjetcms/webjet-ai). Knihovna je nezávislá na frameworku a nesmí importovat třídy z `sk.iway.iwcm`, používat Spring ani číst WebJET `Constants`.

Integrace nového serverového poskytovatele do projektu má tři části:

- implementaci `AiProvider`, která zajišťuje komunikaci s API poskytovatele,
- Spring bean typu `AiProvider`, přes který se implementace přidá k vestavěným poskytovatelům,
- jednu CMS službu, která rozšiřuje [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) a implementuje [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automaticky spojí vestavěné poskytovatele knihovny se všemi projektovými Spring beany typu `AiProvider`. Tuto třídu při přidávání poskytovatele neupravujte. Zpracování domény, konfigurace, auditování, statistiky, perzistence, makra promptů a dočasné soubory zůstávají ve zprávě WebJET CMS.

Identifikátor poskytovatele je veřejná konfigurační hodnota. Musí být stabilní, neprázdný, jedinečný a ve všech třech částech naprosto shodný. Doporučujeme použít malá písmena, například `acme`. Duplicitní identifikátor včetně kolize s vestavěným poskytovatelem způsobí chybu při startu aplikace.

## 1. Provádění `AiProvider`

Implementaci umístěte do samostatné knihovny nezávislé na WebJET CMS. Následující minimální příklad je úplný a kompilovatelný; pevnou odpověď nahraďte voláním API dodavatele:

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

API klíč, koncový bod, časové limity a důvěryhodné hlavičky vstupují do poskytovatele pouze přes neměnný objekt `AiProviderConfig`. Poskytovatel nesmí přímo přistupovat k servletovému požadavku, Spring službám, databázi ani ke konfiguraci CMS. Instance se používá souběžně a během celého životního cyklu `AiClient`, proto musí být vláknově bezpečná a má opakovaně používat transportní zdroje. Vlastní zdroje uvolněte v metodě `close()`.

Metoda `embed` má výchozí implementaci, která oznámí, že poskytovatel embeddingy nepodporuje. Pokud má poskytovatel fungovat se sémantickým vyhledáváním, implementujte `AiProvider.embed(EmbeddingRequest, AiProviderConfig)`. Odpověď musí obsahovat právě jeden `EmbeddingVector` pro každý vstup, ve stejném pořadí as počtem dimenzí z `EmbeddingOptions`. [EmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) počet i dimenzi vektorů kontroluje.

Úplný popis transportu, embeddingů, zpracování chyb a životního cyklu naleznete v dokumentaci [Implementing and using a custom AI provider](https://github.com/webjetcms/webjet-ai/blob/main/docs/custom-providers.md).

## 2. Registrace Spring beanu

V projektu vytvořte konfigurační třídu, kterou najde Spring component scan, a zpřístupněte implementaci jako bean typu `AiProvider`:

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

CMS předá všechny takové beany do `AiClient.discover(...)`, který je přidá k vestavěným poskytovatelům `openai`, `gemini` a `openrouter`. Hodnota `destroyMethod = ""` je důležitá: životní cyklus poskytovatele po úspěšném vytvoření klienta vlastní `AiClient`, proto Spring nemá volat `close()` podruhé.

Samotné vložení JAR souboru na classpath poskytovatele nezaregistruje. Projekt musí jeho instanci vždy explicitně vytvořit jako Spring bean. Neupravujte jaderný bean `webjetAiClient` a nevytvářejte další `AiClient`.

## 3. CMS adaptér a konfigurace

Vytvořte jednu Spring službu, která současně rozšiřuje `LibrarySupportLogic` a implementuje `AiAssitantsInterface`. Základní třída zajistí volání `AiClient`, auditování, statistiky, makra promptů, dočasné soubory i modelově specifická pole pro obrázky:

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

Metoda `getApiKey()` je zdroj přihlašovacích údajů i podmínka dostupnosti poskytovatele. Pro poskytovatele bez API klíče přiměřeně přepište `isInit()`. `getImageNameModel()` je nutná pouze tehdy, když CMS generuje název uloženého obrázku přes daného poskytovatele. Metoda `configure(...)` je volitelná; používejte ji pouze pro bezpečné nastavení vlastního koncového bodu nebo hlaviček. Hodnota `trustedReferer` je ověřena v CMS. Hlavičky zadané uživatelem nikdy nepředávejte přímo a tajné hodnoty nezapisujte do logu.

Konfigurační proměnné `ai_acmeAuthKey` a `ai_acme_generateFileNameModel` vytvořte v sekci **Nastavení → Konfigurace** ; API klíč uložte zašifrovaný. Název přidejte do všech překladových souborů projektu, například:

```properties
components.ai_assistants.provider.acme.title=Acme
```

[WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java) přidá společné časové limity, vytvoří neměnný `AiProviderConfig` a zavolá @@CODE. Nový poskytovatel se proto nepřidává do žádného `switch` bloku ani do centrálního seznamu konfiguračních klíčů.

## 4. Embeddingy a sémantické vyhledávání

Knihovna definuje poskytovatelsky nezávislé typy `EmbeddingRequest`, `EmbeddingOptions`, `EmbeddingResponse` a `EmbeddingVector`. RAG modul CMS volá `AiClient.embed` přes [EmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) a neobsahuje HTTP klienta konkrétního poskytovatele.

Pro použití nového poskytovatele pro RAG musí platit všechny podmínky:

- `AcmeProvider` implementuje `embed(...)`,
- `AcmeProvider` je zaregistrován jako Spring bean,
- `AcmeService.getProviderId()` vrací stejný identifikátor,
- systémoví asistenti `RAG-EMB-INDEX` a `RAG-EMB-SEARCH` používají poskytovatele `acme`, přesně stejný model a správnou hodnotu `ragEmbeddingDimensions`.

Předchozí embedding SPI systému CMS bylo odstraněno. Vlastní `EmbeddingProvider` nevytvářejte.

## 5. Možnosti generování obrázků

Možnosti obrázků se nedefinují v CMS adaptéru. Poskytovatel je publikuje bez síťového volání přes `AiProvider.imageOptions(model, operation)`. Například:

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

`LibrarySupportLogic` z metadat automaticky zobrazí pouze podporovaná pole **Počet obrázků**, **Rozměr**, **Kvalita** a **Poměr stran** a do požadavku odešle pouze jejich podporované hodnoty. Pro přenosný rozměr použijte klíč `size`, podporován je i poskytovatelský klíč `resolution`. Pro poměr stran jsou rozpoznány klíče `aspectRatio` a `aspect_ratio`. Prázdná mapa znamená, že CMS pro daný model a operaci doplňková pole nezobrazí. Metadata nesmí vyžadovat API klíč ani síťové volání.

## Výjimka `AiInterface` pouze pro prohlížeč

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) přímo, protože Chrome Built-in AI běží v prohlížeči a nepoužívá serverovou komunikaci s poskytovatelem. Toto je jediný způsob přímé implementace. Noví serveroví poskytovatelé musí používat `AiProvider` a `LibrarySupportLogic`.

## Lokální vývoj

Při souběžném lokálním vývoji CMS a sousedního repozitáře `webjet-ai` použijte Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Stejnou volbu `--include-build ../webjet-ai` použijte při každé lokální sestavovací a testovací úloze, která má používat nezveřejněné změny knihovny. Jinak se použije verze `com.webjetcms:webjet-ai` určená proměnnou `webjetAiVersion` v `build.gradle`.

## `AiAssistantsService`

`AiAssistantsService` zpracovává požadavky související s datovou tabulkou [Asistenti](../../../../redactor/ai/settings/README.md). Pomocí dependency injection získává všechny implementace `AiAssitantsInterface` a vybírá je podle identifikátoru poskytovatele.

Důležité metody:

- `getAssistantAndFieldFrom` – vrátí asistenty, kteří splňují podmínky zobrazení ve vybraném poli
- `getClassOptions` – vrátí třídy, na které může být asistent navázán
- `getFieldOptions` – vrátí pole vybrané třídy
- `prepareBeforeSave` – před uložením vyvolá změny specifické pro poskytovatele
- `getProviderSpecificOptions` – přidá do `DatatablePageImpl` možnosti specifické pro poskytovatele
- `getProviderFields` – vrátí dodatečná pole zobrazená v editoru tabulky
- `getAssistantStatus` – oznámí, zda je poskytovatel nakonfigurován

## `AiService`

`AiService` zpracovává požadavky asistentů a pomocí dependency injection získává všechny CMS adaptéry `AiInterface`.

Důležité metody:

- `getProviders` – vrátí všechny dostupné poskytovatele a označí nenakonfigurované
- `getModelOptions` – vrátí modely poskytovatele, volitelně filtrované podle řetězce
- `getAiResponse` – vrátí úplnou textovou odpověď
- `getAiImageResponse` – vrátí obrázkovou odpověď
- `getAiStreamResponse` – streamuje textovou odpověď přes `BufferedWriter`
- `getBonusHtml` – vrátí doplňková pole podporovaná vybraným obrázkovým modelem
