# Písanie inštrukcií

Pri písaní inštrukcií pre OpenAI response API dodržujte tieto odporúčania:

1. **Buďte konkrétny**
    Jasne a presne popíšte, čo očakávate od odpovede. Vyhnite sa nejednoznačnosti.

2. **Používajte jednoduchý jazyk**
    Formulujte inštrukcie zrozumiteľne, aby boli ľahko pochopiteľné pre systém aj používateľa.

3. **Nedefinujte formát výstupu**
    O formát výstupu sa stará implementácia, ktorá rozhodne na základe typu požiadavky a iných parametrov.

4. **Uveďte príklady**
    Priložte ukážky požadovaného vstupu a výstupu, ak je to možné.

5. **Vyhnite sa zbytočným detailom**
    Zamerajte sa na podstatné informácie, ktoré sú dôležité pre výsledok.

6. **Špecifikujte jazyk odpovede**
    Ak má byť odpoveď v konkrétnom jazyku, uveďte to explicitne.

## Príklad správnej inštrukcie

> Vygeneruj zoznam troch tipov na zlepšenie SEO pre webovú stránku v slovenčine vo forme očíslovaného zoznamu.

Dodržiavaním týchto zásad zabezpečíte, že odpovede budú relevantné a použiteľné.

## Obrázky

Pri inštrukciách pre generovanie a úpravu obrázkov je potrebné do inštrukcií vložiť značku `{userPrompt}` pre vloženie zadanej požiadavky používateľa. Táto značka je nahradená zadaným textom. Príklady inštrukcií:

Vygenerovať obrázok

```json
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```

Upraviť obrázok:

```json
"role": "Image editing specialist",
"task": "Analyze the provided image and apply the requested edits as specified in the user input. Return the edited image encoded in Base64 format without any additional response or explanation.",
"constraints": {
    "input": "User-provided image and specific edit instructions",
    "edit instructions": "{userPrompt}",
    "output": "Edited image in Base64 format",
    "avoid": "Providing any text, comments, or explanations",
    "format": "Ensure the output is a valid Base64-encoded image"
}
```

## Vylepšenia HTML kódu

Pri pokročilých úpravách HTML kódu je potrebné získať upravený kód ale aj vysvetlenie čo bolo upravené. Ak odpoveď od AI obsahuje na začiatku blok

```text
  ```html
```

tak je obsah tohto bloku je považovaný za HTML kód a všetko čo je následne za týmto blokom sa zobrazí návštevníkovi v dialógu asistenta ako odpoveď.

Príklad inštrukcie:

```json
"role": "Expert web copywriter",
"task": "Critically review the provided website content in its original language. Evaluate clarity, conciseness, tone, and messaging alignment with the brand's voice and value proposition. Identify areas for improvement and recommend specific changes to enhance user engagement, conversion, and SEO performance.",
"constraints": {
    "language": "Detect and use the original language of the provided content, do not change language of the text elements",
    "tone": "Professional, constructive, and actionable",
    "output_format": "Response in markdown format, in TWO section - first with updated and valid HTML code, second with comments and explanatory of changes. Use same language as content for explanatory of changes.",
    "focus": "Clarity, conciseness, brand alignment, user engagement, conversion, and SEO best practices",
    "avoid": "Altering the core meaning of the content or adding unrelated suggestions"
}
```

Všimnite si definovanie `output_format`, ktorý definuje vytvorenie 2 sekcií s HTML kódom a vysvetlivkami k úpravám.

## AI v prehliadači

Písanie inštrukcií pre AI v prehliadači je špecifické podľa [možností dostupného API](https://developer.chrome.com/docs/ai/built-in-apis). Inštrukcia je rozdelená na názov API a následnú konfiguráciu. V odkaze na `MDM` dokumentáciu nájdete podrobné informácie o možnostiach konfigurácie.

Aj keď API podporuje nastavenie jazyka, aktuálne zvyčajne podporuje len anglický jazyk. Do konfigurácie môžete pridať možnosť `"translateOutputLanguage": "autodetect"` pre následné vykonanie prekladu. Hodnota `autodetect` deteguje jazyk pôvodného textu a tento následne použije. Môžete prípadne zadať aj fixnú hodnotu, napr. `sk`, alebo hodnotu `userLng` kde sa jazyk nastaví podľa jazyka aktuálne prihláseného používateľa.

### Sumarizácia textu

API [Summarizer](https://developer.mozilla.org/en-US/docs/Web/API/Summarizer) slúži na sumarizáciu zadaného textu. Pomocou atribútu `type` môžete nastaviť rozsah generovania.

```JavaScript
Summarizer:
{
   "type": "tldr",
   "format": "plain-text",
   "length": "short",
   "translateOutputLanguage": "autodetect",
   "sharedContext": "Dôležité: odpovedaj v rovnakom jazyku ako je zadaný text, nemeň jazyk!"
}
```

### Prekladač

Pre preklad sa používa API [Translator](https://developer.mozilla.org/en-US/docs/Web/API/Translator_and_Language_Detector_APIs).

```JavaScript
Translator:
{
  "sourceLanguage": "sk",
  "targetLanguage": "en"
}
```

Ako hodnotu do `sourceLanguage` je možné zadať výraz `autodetect` pre automatickú detekciu jazyka zadaného textu, alebo `userLng` pre nastavenie jazyka aktuálne prihláseného používateľa. Hodnotu `userLng` je možné zadať aj do poľa `targetLanguage`. Môžete teda vytvoriť asistenta Preložiť do aktuálneho jazyka kde sa zdrojový jazyk automaticky deteguje a cieľový sa nastaví podľa jazyka prihláseného používateľa.

```JavaScript
Translator:
{
  "sourceLanguage": "autodetect",
  "targetLanguage": "userLng"
}
```

### Písanie textu

Na písanie nového textu je možné použiť [Writer](https://developer.chrome.com/docs/ai/built-in-apis).

```JavaScript
Writer:
{
   "tone": "neutral",
   "format": "plain-text",
   "length": "medium",
   "sharedContext": "You are a skilled expert in marketing and SEO optimization. Odpovedaj v slovenskom jazyku"
}
```

### Úprava textu

Pre úpravu existujúceho textu sa používa [Rewriter](https://developer.chrome.com/docs/ai/built-in-apis). Pomocou atribútu `tone` je možné zmeniť formálnosť textu.

```JavaScript
Rewriter:
{
   "tone": "more-formal",
   "format": "as-is",
   "length": "as-is"
}
```

### Pokročilé výrazy

Pre pokročilé výrazy je možné použiť [LanguageModel API](https://developer.chrome.com/docs/ai/prompt-api), ktoré umožňuje formulovať požiadavku v prirodzenom jazyku.

```JavaScript
LanguageModel:
{
   "temperature": 1.0,
   "topK": 1.0,
   "initialPrompts": [
    {
      "role": "system",
      "content": "Si expert na gramatiku a pravopis v slovenskom jazyku. Oprav gramatické, pravopisné a sémantické chyby v texte. Doplň chýbajúce čiarky a interupčné znamienka ako mäkčene a dĺžne. Odpovedaj v slovenskom jazyku a odpovedz iba upraveným textom bez doplňujúcich vysvetlení."
    }
  ]
}
```

## Náhrada výrazov

V inštrukciách sú pred použitím nahradené špeciálne značky, tie môžete použiť na vloženie údajov na presné miesto.

- `{userPrompt}` - vloží zadaný text používateľom zo vstupného dialógu (ak je zvolená možnosť Požadovať vstup od používateľa).

```JavaScript
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```

- `{inputText}` - vstupný text, hodnota poľa, ktorého meno je zadané v Zdrojové pole v nastaveniach asistenta.