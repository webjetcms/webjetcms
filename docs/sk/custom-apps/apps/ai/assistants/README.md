# Pridanie poskytovateľa

Aby ste pridali podporu pre nového poskytovateľa (`provider`), musíte splniť nasledujúce podmienky:

- vytvoriť 2 triedy s anotáciou `@Service`, najlepšie v lokalite `sk.iway.iwcm.components.ai.providers.PROVIDER_NAME`, kde sú už implementácie pre `openai` a `gemini`
- jedna trieda by mala implementovať rozhranie `sk.iway.iwcm.components.ai.providers.AiInterface` a teda implementovať všetky povinné metódy
- druhá trieda by mala implementovať rozhranie `sk.iway.iwcm.components.ai.providers.AiAssitantsInterface` a teda implementovať všetky povinné metódy

O všetko ostatné sa už postarajú triedy `sk.iway.iwcm.components.ai.rest.AiAssistantsService` a `sk.iway.iwcm.components.ai.rest.AiService`, ktoré dynamicky podľa identifikátora poskytovateľa spracujú požiadavku.

## AiAssistantsService

`AiAssistantsService` sa primárne stará o potreby spojené s dátovou tabuľkou [Assistenti](../../../../redactor/ai/settings/README.md). Trieda poskytuje podporné metódy pre rôzne výberové polia a pod. Využíva princíp automatického `dependency injection`, aby získala zoznam všetkých tried, ktoré implementujú rozhranie `AiAssitantsInterface`. Pri požiadavke na dáta, ktoré sú špecifické pre konkrétneho poskytovateľa, prejde zoznam a vyvolá potrebnú akciu nad správnou implementáciou podľa identifikátora poskytovateľa.

Dôležité metódy, ktoré trieda poskytuje:

- **getAssistantAndFieldFrom** – vráti asistentov z DB, ktorí spĺňajú podmienky zobrazenia na zvolenom poli
- **getClassOptions** – vráti zoznam všetkých tried, na ktoré môže byť asistent naviazaný (filter podľa reťazca v plnom názve triedy)
- **getFieldOptions** – vráti zoznam všetkých polí zvolenej triedy (z predchádzajúceho zoznamu), ktorých názov obsahuje hľadaný reťazec
- **prepareBeforeSave** – nájde poskytovateľa pre daného asistenta a vyvolá na ňom rovnako pomenovanú povinnú metódu na špecifické úpravy pred uložením
- **getProviderSpecificOptions** – nájde poskytovateľa a vyvolá metódu na doplnenie špecifických možností do `DatatablePageImpl`
- **getProviderFields** – nájde poskytovateľa a vyvolá metódu, ktorá vráti zoznam dodatočných polí na zobrazenie pre editor tabuľky
- **getAssistantStatus** – podľa identifikátora poskytovateľa vráti jeho status (či je nakonfigurovaný alebo nie)

## AiService

`AiService` sa primárne stará o potreby spojené so spracovaním AI žiadostí od vyvolaných asistentov. Poskytuje metódy na získanie odpovede od poskytovateľa zvoleného asistentom. Využíva princíp automatického `dependency injection`, aby získala zoznam všetkých tried, ktoré implementujú rozhranie `AiInterface`.

Dôležité metódy, ktoré trieda poskytuje:

- **getProviders** – vráti zoznam všetkých nakonfigurovaných poskytovateľov
- **getModelOptions** – vráti zoznam všetkých modelov daného poskytovateľa; existuje aj verzia tejto metódy, ktorá tieto modely filtruje na základe požadovaného reťazca v názve
- **getAiResponse** – vráti TEXTOVÚ odpoveď od daného poskytovateľa ako celok
- **getAiImageResponse** – vráti OBRÁZKOVÚ odpoveď od daného poskytovateľa
- **getAiStreamResponse** – vráti TEXTOVÚ odpoveď od daného poskytovateľa, pričom odpoveď je streamovaná cez zadaný `PrintWriter`
- **getBonusHtml** – podľa identifikátora poskytovateľa vráti HTML kód, ktorý sa použije ako bonusový obsah v okne spusteného asistenta