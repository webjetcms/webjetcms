# Přidání poskytovatele

Komunikace s externí AI službou včetně embeddingů patří do samostatné knihovny [webjet-ai](https://github.com/webjetcms/webjet-ai). Knihovna je nezávislá na frameworku a nesmí importovat třídy z `sk.iway.iwcm`, používat Spring ani číst WebJET `Constants`.

Integrace nového serverového poskytovatele do projektu má tři části:

- implementaci `AiProvider`, která zajišťuje komunikaci s API poskytovatele,
- Spring bean typu `AiProvider`, přes který se implementace přidá k vestavěným poskytovatelům,
- jednu CMS službu, která rozšiřuje [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) a implementuje [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automaticky spojí vestavěné poskytovatele knihovny se všemi projektovými Spring beany typu `AiProvider`. Tuto třídu při přidávání poskytovatele neupravujte. Zpracování domény, konfigurace, auditování, statistiky, perzistence, makra promptů a dočasné soubory zůstávají ve zprávě WebJET CMS.

Identifikátor poskytovatele je veřejná konfigurační hodnota. Musí být stabilní, neprázdný, jedinečný a ve všech třech částech naprosto shodný. Doporučujeme použít malá písmena, například `acme`. Duplicitní identifikátor včetně kolize s vestavěným poskytovatelem způsobí chybu při startu aplikace.

## Rozdělení odpovědností

Při zjednodušování adaptérů zachovejte následující úlohy v CMS:

| Role | Implementace v CMS |
| --- | --- |
| Oprávnění, výběr asistenta a kontrola povolené akce | `AssistantController`, `AiService` |
| Konfigurace poskytovatele a doménový kontext | `WebjetAiConfigurationService` ; `AiTaskRegistry` přenáší kontext do pracovního vlákna |
| Příprava CMS vstupů a maker, ochrana a obnovení příkazů `INCLUDE` | `LibrarySupportLogic`, `IncludesHandler` |
| Audit a evidence spotřeby tokenů podle uživatele a domény | `LibrarySupportLogic`, `AiStatService` |
| Ukládání a odstranění dočasných obrázků, kontrola cílové složky | `AiTempFileStorage`, `AiTempFileRemover` |
| Vazba pokračování četu na session, uživatele, doménu a asistenta | `AiChatSession` ; prohlížeč dostává neprůhledný token namísto ID odpovědi poskytovatele |
| Evidence a zastavení úloh uživatele | `AiTaskRegistry` ; knihovně předává `AiCancellationToken` |
| Vložení odpovědi do editoru, práce s výběrem a vrácení změn | `EditorAi` ; přepis, koncept a obnovu rozhovoru spravuje `AiChat` |

Knihovna dostává připravené `AiRequest` a `AiProviderConfig`, vrací `AiResponse` a chyby oznamuje výjimkou `AiProviderException`. Zajišťuje protokol poskytovatele, jeho modely a metadata schopností, přenos streamu a klasifikaci chyb. CMS podle těchto údajů rozhoduje o zpřístupnění funkcí a zachovává nastavení asistenta, například souhlas s ukládáním rozhovoru přes `useTemporal=false`.

Společné JavaScript funkce `requestAiText` a `parseAiResponse` zůstávají součástí CMS. První komunikuje s REST službami CMS, druhá připravuje obsah pro editor. Oprávnění, session a ukládání změn nadále spravují příslušné CMS třídy uvedené v tabulce.

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

## Vestavěné lokální modely

Artefakt `com.webjetcms:webjet-ai-local` rozšiřuje základní knihovnu o lokální poskytovatele modelů. WebJET CMS registruje tři adaptéry:

- [LocalTextService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/text/LocalTextService.java) pro `utter-project/EuroLLM-1.7B-Instruct`,
- [LocalTranslateService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/translate/LocalTranslateService.java) pro `facebook/m2m100_418M`,
- [LocalEmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/embedding/LocalEmbeddingService.java) pro `intfloat/multilingual-e5-base`.

Každý adaptér používá samostatnou globální cestu k modelovému ZIP balíčku. [ConfiguredLocalProvider](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/ConfiguredLocalProvider.java) načte cestu bez doménového kontextu, poskytovatele otevře líně při prvním volání a jednu in. Změna cesty proto vyžaduje restart.

Cesta může být absolutní cesta na serveru nebo cesta začínající `/WEB-INF/`, kterou adaptér převede přes `Tools.getRealPath()` na fyzickou cestu v nasazené aplikaci. Skripty popsané v dokumentaci [AI asistenti](../../../../redactor/ai/settings/README.md) vytvoří schválené balíky ve složce `/WEB-INF/local-ai-models/`, například `/WEB-INF/local-ai-models/eurollm-1.7b-instruct-q4-k-m.zip` a `/WEB-INF/local-ai-models/m2m100-418m-int8.zip`.

Lokální překlad nepoužívá běžný prompt. [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) pro něj vytvoří požadavek s `TranslationOptions` a odmítne HTML, @@ykrá_k, @@CODE_1 v instrukcích. Instrukce musí obsahovat JSON s `sourceLanguage` a `targetLanguage` ; volitelná hodnota `maximumOutputTokens` je nejvíce `200`. Lokální generování textu odmítne rozbalení promptů, ve kterém ochrana detekuje pokus o `prompt injection`.

## Výjimka `AiInterface` pouze pro prohlížeč

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) přímo, protože Chrome Built-in AI běží v prohlížeči a nepoužívá serverovou komunikaci s poskytovatelem. Toto je jediný způsob přímé implementace. Noví serveroví poskytovatelé musí používat `AiProvider` a `LibrarySupportLogic`.

## Lokální vývoj

Při souběžném lokálním vývoji CMS a sousedního repozitáře `webjet-ai` použijte Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Stejnou volbu `--include-build ../webjet-ai` použijte při každé lokální sestavovací a testovací úloze, která má používat nezveřejněné změny knihovny. Jinak se použije verze `com.webjetcms:webjet-ai-local` určená proměnnou `webjetAiVersion` v `build.gradle`.

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

### Podpora četu a streamování

`LibrarySupportLogic` vybírá operaci a ověřuje streamování podle `AiClient.capabilities(providerId, model)`. Operaci `CHAT` použije pro asistenta s akcí `LIVE_CHAT`, explicitním `useTemporal=false`, podporou četu v adaptéru a modelem, který čet výslovně neodmítá. V ostatních případech zůstává operace `TEXT`. Metadata samy nepovolují ukládání rozhovoru.

Streamování musí být zapnuto v asistentovi a metadata nesmí označovat zvolenou operaci nebo její streamování jako nepodporované. Výsledek se předá ve stávajícím `DataTableAi.useStreaming` a stejná kontrola se použije při volání streamovací REST služby. Uložená hodnota `useStreaming` se nemění. Neznámá podpora modelu nebo streamování (`UNKNOWN`) požadavek neblokuje.

Prohlížečový poskytovatel zachovává původní nastavení. Lokální adaptéry při zjišťování schopností nenačítají modely; nadále platí jejich omezení nastavená při ukládání asistenta.

### Ukládané rozhovory

Globální čet i čet při poli používají `AiChatHost` ze souboru `ai/ai-chat-host.js`. Hostitel spravuje výběr asistenta, historii a panel souborů; jednotlivé rozhovory zobrazuje komponentu `AiChat`. Hostitel načte detail a předá jej metodě `AiChat.restoreHistory(detail)`, která obnoví zprávy a pravidla pokračování bez ztráty novějšího konceptu nebo příloh odmítnutého požadavku. Společné styly jsou v `_md-ai-chat.scss` pod třídami `ai-chat` a `ai-chat-host`.

Seznam dostupných asistentů dodává `QuickSearch` přes callback `loadAssistants(signal)`, který volá `GET /admin/rest/ai/assistant/chat-assistants/`. Hostitel zajišťuje stav načítání, opakování po chybě a zrušení požadavku při zániku. Globální nabídka vyžaduje akci `live_chat` a přesný token `top-search-field` ve `fieldTo` nebo `CHAT_SEARCH` v `className`. Hodnoty v obou seznamech lze oddělit čárkou, středníkem nebo novým řádkem. Při výběru asistentů pro pole editoru se `top-search-field` přeskočí a použijí se ostatní cílová pole. Při samotné entitě `CHAT_SEARCH` se prázdné `fieldTo` při ukládání doplní na `*`.

Metadata `persistentChat` povolují ukládání pouze aktivním asistentem s akcí `live_chat`, explicitním `useTemporal=false`, poskytovatelem OpenAI nebo Gemini a podporou četu v adaptéru a modelu. `AiChatService` kontroluje vazbu na rychlé vyhledávání při zahájení i pokračování globálního rozhovoru a při určování dostupnosti historie. Po odstranění poslední takové vazby zůstává historie ke čtení. Rozhovory s kontextem pole používají pravidla dostupnosti v editoru.

Ukládaný čet obsluhují `AiChatController`, `AiChatService` a `AiChatStore` v CMS. Endpointy pod `/admin/rest/ai/chat` vyžadují oprávnění `cmp_ai_button`, přihlášeného vlastníka rozhovoru a správnou doménu:

| Metoda a cesta | Účel |
| --- | --- |
| `GET /threads?page=0&query=...` | Stránkovaná historie; vyhledává také v textu zpráv, názvu asistenta a poskytovatele bez ohledu na diakritiku. |
| `GET /threads/{id}` | Detail rozhovoru, zprávy, přílohy a dostupnost pokračování. |
| `POST /messages` | Nová zpráva nebo pokračování. JSON obsahuje `assistantId`, `requestId`, `userPrompt`, při pokračování i `threadId` a `revision` a při opakování dokončeného pokusu `retryMessageId`. |
| `POST /threads/{id}/stop` | Zastavení aktivní požadavky vlastníka rozhovoru. |
| `DELETE /threads/{id}` | Odstranění odpovědí u poskytovatele a následně lokální historie s přílohami. |
| `GET /threads/{id}/attachments/{attachmentId}` | Autorizované načítání přílohy; `download=true` vynutí stažení. |

U příloh používá `POST /messages` formát `multipart/form-data`: pole `message` obsahuje JSON požadavky a pole `files` jsou soubory. `AiChatAttachments` kontroluje formát a limity: 5 souborů, celkem 10 MB, 100 000 znaků extrahovaného textu a nejvýše 200 stran PDF. Podporované formáty jsou uvedeny v [uživatelském návodu](../../../../redactor/ai/chat/README.md#přílohy). Text dokumentů a popis obrázků se použijí jako kontext; popis obrázku vytváří zvolený model. Společná metoda `LibrarySupportLogic.prepareChatAttachment()` vrací text jedné přílohy a spotřebu tokenů pro ukládaný i dočasný čet. Volající načítá přílohy a sčítá spotřebu; při ukládaném četě tak zůstane zaznamenaná cena již popsaného obrázku i po selhání následující přílohy.

Každý rozhovor má složku `/files/protected/ai-chat/<domainId>/<userId>/<threadId>/` se souborem `ai_chat.json`. Přílohy dostávají pořadové číslo a původní příponu, například `1.docx`, `2.png` ; číslování pokračuje napříč zprávami. Při stahování se obnoví původní název. Umístění pod `/files` umožňuje sdílení historie mezi uzly clusteru přes NFS. Složka `/files` může být symbolický odkaz na samostatný disk nebo sdílené úložiště; čet používá kanonickou cestu úložiště i při prvním vytvoření historie. Historie i přílohy jsou odděleny podle domény a uživatele. Zápisy chrání souborový zámek vlastníka v dané doméně a atomová výměna JSON souboru. Historie a přílohy jsou vyloučeny z WAR, Gitu, ZIP archivů a offline exportu. Uživatelské operace ve správci souborů je nemají zpřístupňovat, přejmenovat ani přepsat; klient je načítá přes autorizovaný endpoint s CSRF hlavičkou včetně náhledů obrázků.

`AiChatStore` uchovává ID odpovědí poskytovatele na serveru. `requestId` chrání před duplicitním odesláním a `revision` před pokračováním z neaktuálního stavu. Po ztrátě kontextu nebo změně asistenta či konfigurace poskytovatele může historie zůstat pouze pro čtení. `AiChatService` maže uložené odpovědi přes `AiClient.deleteResponse(providerId, responseId, config)` z knihovny `webjet-ai`. Výsledky `DELETED` i `ALREADY_ABSENT` potvrzují, že odpověď již u poskytovatele není; až po potvrzení všech odpovědí CMS odstraní lokální historii a přílohy. Při výjimce zůstane stav `DELETE_PENDING` a lokální údaje se zachovají pro opakování akce, které přeskočí již potvrzené odpovědi.

Pro pravidelné mazání starých rozhovorů přidejte do plánovaných úloh třídu `sk.iway.iwcm.components.ai.rest.AiChatCleanup`. Parametry mají pořadí `počet dní|ID domény|názov domény`, například `90|7|www.example.sk`. V běžné instalaci bez dělení dat podle domén je ID `1` ; název musí odpovídat doméně uložené při rozhovorech. Úloha zpracuje všechny vlastníky v zadané doméně včetně četů u polí. U externího úložiště vybere adresář zadané domény. Pro další doménu vytvořte samostatný úkol. Mazání se automaticky nezapíná a počet dní musí být kladný.

Úloha vybírá celé rozhovory podle `thread.updated`, ne podle věku jednotlivých příloh. Před označením k odstranění znovu ověří poslední aktivitu pod zámkem a přeskočí probíhající generování. Používá stejné odstranění odpovědí u poskytovatele jako ruční mazání a při dalším spuštění zopakuje i nedokončená mazání ve stavu `DELETE_PENDING`.

K čištění chatové historie používejte `AiChatCleanup` nebo `AiChatService.deleteOlderThan(scope, cutoff)`, aby se zachovaly zámky a odstranily i odpovědi u poskytovatele.

Knihovna `webjet-ai` od verze `2.1.0` oznamuje ID přes `AiLifecycleListener` již během zpracování odpovědi. CMS jej uloží pro pozdější odstranění i při chybě nebo zastavení generování; pokračování rozhovoru posune pouze po úspěšném dokončení. Při chybě používá `AiProviderException.lifecycle()`: stavy `NOT_SUBMITTED` a `REJECTED` potvrzují, že nevznikla odpověď na odstranění. Stav `UNKNOWN` nebo `RESPONSE_OBSERVED` bez dostupného ID zůstává nevyřešen a brání odstranění lokální historie, pokud CMS nezachytil ID dříve. Samotný HTTP stav ani možnost opakování požadavku nepotvrzují odmítnutí poskytovatelem.

Čet při poli používá pro uložený záznam hostitelskou komponentu `AiChatHost` s kontextem `AiChatContext`: `entityType` je cesta REST zdroje tabulky bez koncového lomítka a query parametrů, `entityId` je ID upravovaného záznamu a `fieldName` je cílové pole. `EditorAi` dodává asistenty přímo z definice sloupce, obsah pole nebo označeného textu a akce pro vložení odpovědi. Globální seznam asistentů se u pole nenačítá. Nové záznamy a hromadná úprava používají `AiChat` bez perzistence. Editor přistupuje k oběma variantám přes `conversation.view` s metodami `mount`, `unmount`, `destroy` a `isPending` ; u hostitele zahrnuje `isPending` i rozhovory běžící na pozadí po přepnutí asistenta.

Požadavek `POST /messages` u pole obsahuje také objekt `context` s těmito třemi hodnotami a `inputValue` se zdrojovým obsahem. Kontext je uložen při rozhovoru a pokračování jej nemůže změnit. Zdrojový obsah se uloží při otázce, aby opakování neúspěšného pokusu použilo původní vstup spolu s původními přílohami. Nová otázka obdrží aktuální obsah pole.

Při platném výběru v CKEditoru nebo textovém editoru PageBuildera posílá `EditorAi` i `inputIsSelectedTextInEditor=true`. `LibrarySupportLogic` při akci `live_chat` doplní k instrukcím pokyn pro úpravu samotného HTML fragmentu bez nových obalů či nevyžádaných nadpisů. Pokyn má přednost před pravidly pro generování celé stránky, takže funguje také se stávající definicí Website Buildera a nevyžaduje rozpoznávání konfigurovatelných obalů. Příznak se přenáší také přes `InputDataDTO` při dočasném čtení a ukládá se při otázce v `ai_chat.json` ; opakování používá původní hodnotu. Chybějící příznak znamená `false`.

Historie u pole volá `GET /threads?page=0&entityType=...&entityId=...&fieldName=...`, případně s `query`. Server vyžaduje všechny tři filtry a porovnává je přesně ještě před stránkováním. Bez filtrů vrátí pouze samostatné rozhovory bez kontextu. Kontext slouží ke třídění soukromé historie, neposkytuje přístup k samotné entitě; kontrola vlastníka a domény rozhovoru zůstává zachována. Při zavření editoru se běžící požadavky zruší a rozepsané zprávy zahodí, uložené rozhovory a přílohy zůstanou dostupné.

Pro četové asistenty OpenAI a Gemini podporují endpointy `POST /admin/rest/ai/assistant/response/` a `POST /admin/rest/ai/assistant/response-stream/` i `multipart/form-data`. Pole `message` obsahuje JSON `InputDataDTO` včetně kontextu pole, `userPrompt`, `assistantId`, `timestamp` a případného `conversationId` ; opakovaná pole `files` obsahují přílohy. Bez příloh zůstává požadavek v JSON. Server používá stejnou validaci a extrakci obsahu jako ukládaný čet, ale soubory drží pouze během zpracování požadavku. Obsah příloh doplní ke kontextu pole. Podpora příloh u pole nevyžaduje vypnutý dočasný čet.

### Přenos průběžné odpovědi do administrace

Textové generování v editoru i čet používají `requestAiText` ze souboru `ai-rest-transport.js`. Funkce odesílá požadavek s CSRF tokenem, kontroluje HTTP a JSON odpověď a při streamování používá `readAiStream`. Vrací text a metadata CMS bez úpravy obsahu. Funkce `parseAiResponse` ze souboru `ai-response-parser.js` odděluje HTML a vysvětlení pro generování v editoru i vložení odpovědi z četu. Zprávu rozhovoru zajišťuje `AiChat`. Při HTTP chybách 401 a 403 zobrazí přenosová funkce existující upozornění na odhlášení nebo chybu CSRF tokenu.

Generování v editoru a neukládaný čet volají `POST /admin/rest/ai/assistant/response-stream/` s hlavičkou `Accept: application/x-ndjson`. Ukládaný čet používá `POST /admin/rest/ai/chat/messages` se stejnou hlavičkou; před textem odešle také zprávu `metadata` s identifikátory rozhovoru a zpráv a aktuální revizí. Server posílá samostatné JSON zprávy oddělené novým řádkem:

```json
{"type":"delta","text":"Prvá časť odpovede.\n"}
{"type":"delta","text":"Pokračovanie odpovede."}
{"type":"result","result":{"response":"Prvá časť odpovede.\nPokračovanie odpovede.","totalTokens":42,"error":null,"tempFiles":[],"generatedFileName":null}}
```

Zprávy `delta` obsahují pouze generovaný text. Závěrečná zpráva `result` obsahuje `AssistantResponseDTO` včetně spotřeby tokenů a případné chyby. Klient skládá neúplné řádky mezi síťovými bloky a zpracuje i více zpráv v jednom bloku. Odpověď považuje za úspěšnou až po přijetí závěrečné zprávy bez chyby a ukončení přenosu.

Bez explicitního požadavku na `application/x-ndjson` zůstává na endpointech `response-stream` pro starší klienty původní formát `text/plain` ; endpoint `/chat/messages` vrací běžnou JSON odpověď. CMS adaptéry nadále zapisují obyčejný text do `BufferedWriter` ; oddělení zpráv zabezpečují controllery přes `AssistantStreamWriter`, bez změny rozhraní knihovny `webjet-ai`.

### Chyby poskytovatele

Chybová odpověď zachovává textové pole `error` a může obsahovat také `errorCategory`, `providerStatusCode` a `retryable`. Stejná pole jsou v běžné JSON odpovědi iv závěrečné zprávě `result` streamu. `providerStatusCode` je kladný HTTP stav externí služby, ne CMS endpointu; při neznámé hodnotě se neposílá. `retryable` je doporučení poskytovatele (`true` nebo `false`), při chybějící informaci se neposílá. Při hodnotě `false` čet nenabídne opakování stejného požadavku; zastavenou odpověď může uživatel zopakovat. Úplná odpověď poskytovatele ani příčina výjimky se do prohlížeče neposílají.

| `errorCategory` | Chování četu |
| --- | --- |
| `CANCELLED` | Zobrazí zastavenou odpověď a zachová poslední úspěšné pokračování. |
| `INVALID_CONTINUATION` | Nabídne zahájení nového rozhovoru. Teprve po volbě uživatele zruší neplatné pokračování; přepis zpráv a rozpracovaný text zachová. |
| `UNSUPPORTED_OPERATION` | Vysvětlí potřebu změnit asistenta nebo model namísto opakování stejného požadavku. |
| `UNKNOWN` | Použije běžné zpracování chyby; rovněž zpracuje neznámou budoucí kategorii. |

Čet požadavky automaticky neopakuje ani nezačíná nový rozhovor. Kategorie určuje knihovna podle spolehlivých údajů poskytovatele, nikoli podle textu chybové zprávy. Vzdálené chyby pokračování Gemini proto zatím zůstávají `UNKNOWN`. Stejně se zpracují existující validační chyby CMS, například expirovaný nebo neplatný token rozhovoru v CMS session.

### Zastavení požadavku

`AiTaskRegistry` vytváří pro každý úkol samostatný `AiCancellationToken`. Tlačítko **Zastavit**, zahájení nového četu nebo zavření editoru zruší přes existující službu `stop-assistant` token i `Future` příslušné úlohy. `LibrarySupportLogic` předává token knihovně při textových, četových a obrázkových požadavcích včetně následného generování názvu obrázku. Ostatní souběžné požadavky zůstávají aktivní.

OpenAI, Gemini a OpenRouter přeruší HTTP požadavek včetně čekání na další část streamu. Zastavený čet si zachová poslední úspěšné pokračování; částečná odpověď ho nenahradí. Samotné zavření okna četu pouze skryje jeho obsah a běžící požadavek nezastaví.

Vlastní poskytovatel může překrýt varianty `execute` a `stream` s parametrem `AiCancellationToken` a připojit k němu zrušení svého transportu. Výchozí implementace včetně lokálních modelů kontrolují zrušení před a po provedení a při předávání částí odpovědi; nedokážou vynutit přerušení blokovaného nativního výpočtu. Zrušení HTTP spojení nezaručuje zastavení zpracování nebo účtování na straně externí služby a nemaže již uložený kontext rozhovoru.
