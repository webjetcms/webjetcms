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

## Generovanie PageBuilder blokov

Nasledovná inštrukcia umožňuje generovať [PageBuilder bloky pre editor](../datatables/README.md#režim-chat), je potrebné ju nastaviť ako typ požiadavky `Chat`, v takom prípade sa korektne vyplní aj `{inputText}` pri editácii.

Pre nastavenie odporúčame model `gemini-2.5-pro`, v akcii nastavte entitu `sk.iway.iwcm.doc.DocDetails`, zdrojové a cieľové pole nastavte na `data`. V pokročilé je potrebné zapnúť možnosť Zachovať HTML kód a Požadovať vstup od používateľa.

```json
{
  "name": "WebJET PageBuilder — Bootstrap 5 HTML Generator",
  "version": "1.0",
  "purpose": "Convert the following user request to a clean HTML snippet suitable for WebJET CMS PageBuilder. HTML must be built exclusively on Bootstrap 5 grid system and components. Output must be only HTML (no Markdown, no explanation).",
  "input_contract": {
    "user_request": "{userPrompt}",
    "current_html_code": "{inputText}",
    "options_optional": {
      "language": "auto - use user_request language",
      "sections_hint": "insert sections: 'hero, services, pricing, contact'",
      "brand_tone": "professional and friendly",
      "image_placeholders": {
        "background-bokeh": "<img src=\"/components/htmlbox/objects/placeholder/background-bokeh.jpg\" alt=\"\" class=\"img-responsive img-fluid\" />",
        "background-sandstone": "<img src=\"/components/htmlbox/objects/placeholder/background-sandstone.jpg\" alt=\"\" class=\"img-responsive img-fluid\" />",
        "background-splashing": "<img src=\"/components/htmlbox/objects/placeholder/background-splashing.jpg\" alt=\"\" class=\"img-responsive img-fluid\" />",
        "content-computer": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-computer.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
        "content-lake": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-lake.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
        "content-wolf-howling": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-wolf-howling.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
        "content-woman-face": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-woman-face.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />"
      },
      "map_if_address": "if there is an address, insert the map through the code in which you replace ADDRESS_PLACEHOLDER with the real address: !INCLUDE(/components/map/map.jsp, object=&quot;ADDRESS_PLACEHOLDER&quot;, offsetX=0,offsetY=0, widthPercent=100, heightPercent=45, labelAddress=true, labelComment=false, zoom=13, label=&quot;&quot;,sizeInPercent=&quot;true&quot;, showControls=&quot;false&quot;, closeLabel=&quot;false&quot;, showContentString=&quot;true&quot;, scrollwheel=&quot;true&quot;, key=&quot;&quot;)!",
      "use_images": "use illustrative images for cards, headlines, heroes, services to improve the visual appearance of the page",
      "max_sections": 8
    }
  },
  "absolute_rules": [
    "OUTPUT = only pure HTML code, no Markdown, no comments and no additional text.",
    "Do not use any <style>, no inline 'style' attributes, or custom CSS classes. Use only Bootstrap 5 classes and necessary system attributes (e.g. data-bs-*).",
    "Each content is divided into blocks with the structure: <section> → <div class=\"container\"> → <div class=\"row\"> → one or more <div class=\"col-…\">.",
    "Never insert text outside of <div class=\"col-…\">. The <section>, .container and .row elements must not contain direct text.",
    "Do not use direct text-node in columns. Text must be inside elements (at least <p>, also suitable <h1>-<h6>, <ul>/<ol>, <table> etc.).",
    "Always use 'col-*' classes (e.g. col-12, col-md-6). Do not use bare 'col'.",
    "Use only Bootstrap 5 components and utilities (grid, spacing, typography, buttons, tables, ratio, forms).",
    "Format phone links as <a href=\"tel:+421XXXXXXXXX\">…</a>. Emails as <a href=\"mailto:…\">…</a>.",
    "Images: always add a meaningful alt. Add class=\"img-fluid\". Do not use fixed widths/heights.",
    "Titles: exactly one <h1> per page; other headings hierarchically (<h2>, <h3> …)..",
    "If the user_request contains an address and the map_if_address=true option is selected, insert the map into <div class=\"col-…\"> with <div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div>.",
    "If component requires an ID (e.g. accordion/carousel), use an ID with '__ID__' (e.g. id=\"accordion__ID__\").",
    "Do not use JavaScript outside of Bootstrap's data-bs-* attributes.",
    "Generate content in 'options_optional.language' or (if 'auto') in user_request language.",
    "Maintain accessibility: alt texts, clear heading structure, sufficient contrast using bootstrap colors, aria-* for interactive components if necessary."
  ],
  "allowed_children_inside_col": [
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "p",
    "small",
    "strong",
    "em",
    "ul",
    "ol",
    "li",
    "a",
    "img",
    "table",
    "thead",
    "tbody",
    "tr",
    "th",
    "td",
    "blockquote",
    "figure",
    "figcaption",
    "hr",
    "address",
    "form",
    "label",
    "input",
    "textarea",
    "select",
    "option",
    "button"
  ],
  "layout_guidelines": [
    "Use utility classes for padding/spacing (e.g. py-5, my-5).",
    "Do not use utility classes for gutter settings (e.g. g-4, g-5, g-6).",
    "If appropriate, align content vertically using .align-items-center on .row.",
    "For CTAs use .btn .btn-primary or .btn-outline-primary.",
    "For galleries and service lists use multiple columns (e.g. col-12 col-md-6 col-lg-4) in a single .row.",
    "If the image URL address starts with /thumb, keep the CSS class fixedSize from the example"
  ],
  "section_blueprints_optional": [
    {
      "name": "hero_with_cta",
      "description": "An introductory block with a title, subtitle, short description and button.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row align-items-center\"><div class=\"col-12 col-lg-6\"><h1>Main headline</h1><p>A brief description of the value for the customer.</p><p><a href=\"#contact\" class=\"btn btn-primary\">Call/Order</a></p></div><div class=\"col-12 col-lg-6\"><img src=\"\" alt=\"Illustrative image\" class=\"img-fluid\"></div></div></div></section>"
    },
    {
      "name": "services_list",
      "description": "Overview of services in two to three columns.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12 col-md-6 col-lg-4\"><h2>Services</h2><ul><li>Item services</li><li>Item services</li></ul></div><div class=\"col-12 col-md-6 col-lg-4\"><h2>&nbsp;</h2><ul><li>Item services</li><li>Item services</li></ul></div><div class=\"col-12 col-md-6 col-lg-4\"><h2>&nbsp;</h2><ul><li>Item services</li><li>Item services</li></ul></div></div></div></section>"
    },
    {
      "name": "services_card_with_image",
      "description": "Overview of services in two to three columns with an illustrative image.",
      "template": "<section class=\"py-5 bg-light\"><div class=\"container\"><div class=\"row text-center\"><div class=\"col-12\"><div aria-multiline=\"true\" aria-readonly=\"false\" class=\"column-content\"><h2 class=\"mb-5\">Services</h2></div></div></div><div class=\"row\"><div class=\"col-12 col-md-6 col-lg-4 mb-4\"><div aria-multiline=\"true\" aria-readonly=\"false\" class=\"column-content\"><div class=\"card h-100 text-center\"><img alt=\"\" class=\"card-img-top\" src=\"\" /><div class=\"card-body\"><h3 class=\"h5 card-title\">Title</h3><p class=\"card-text\">Description.</p></div></div></div></div></div></div></section>"
    },
    {
      "name": "pricing_table",
      "description": "Simple pricing in a table.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12\"><h2>Price list</h2><table class=\"table\"><thead><tr><th>Služba</th><th>Price</th></tr></thead><tbody><tr><td>Item</td><td>€</td></tr></tbody></table></div></div></div></section>"
    },
    {
      "name": "contact_and_map",
      "description": "Contact + map in two columns.",
      "template": "<section id=\"contact\" class=\"py-5\"><div class=\"container\"><div class=\"row align-items-start\"><div class=\"col-12 col-lg-5\"><h2>Contact</h2><p><strong>Address:</strong> …</p><p><strong>Phone:</strong> <a href=\"tel:+421XXXXXXXXX\">+421 XXX XXX XXX</a></p><p><strong>Opening hours:</strong> …</p></div><div class=\"col-12 col-lg-7\"><h2>Map</h2><div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div></div></div></div></section>"
    }
  ],
  "self_checklist_before_return": [
    "Is every text item inside <div class=\"col-…\">?",
    "Is 'class=\"col\"' used anywhere without a hyphen? (must be col-*)",
    "Does it contain output <style>, inline style, custom classes or JS?",
    "Is there exactly one <h1> and are the headings hierarchical?",
    "Does <img> have an alt attribute and class=\"img-fluid\"?",
    "If there are component IDs in the code, do they use '__ID__'?"
  ],
  "output_format": {
    "type": "html_fragment",
    "only_html_no_markdown": true,
    "no_explanations": true
  },
  "example": {
    "user_request": "Create a website for a hair salon in Bratislava. Open daily 8:00–15:00, specializing in men's haircuts and beard styling (barber). Address Mlynské Nivy 71, Bratislava. Phone 0903 123 456.",
    "html_result": "<section class=\"py-5\"><div class=\"container\"><div class=\"row align-items-center\"><div class=\"col-12 col-lg-6\"><h1>Hairdresser — Bratislava</h1><p>We specialize in men's haircuts and precise beard trimming.</p><p><strong>Opening hours:</strong> daily 8:00–15:00</p><p><strong>Address:</strong> Hatallova 13, Bratislava</p><p><strong>Phone:</strong> <a href=\"tel:+421903123456\">0903 123 456</a></p><p><a href=\"#contact\" class=\"btn btn-primary\">Make an appointment</a></p></div><div class=\"col-12 col-lg-6\"><img src=\"\" alt=\"Men's haircut and barber\" class=\"img-fluid\"></div></div></div></section><section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12 col-md-6\"><h2>Services</h2><ul><li>Men's haircut</li><li>Beard trim / barber</li><li>Hot towel</li></ul></div><div class=\"col-12 col-md-6\"><h2>Price list</h2><table class=\"table\"><thead><tr><th>Service</th><th>Price</th></tr></thead><tbody><tr><td>Men's haircut</td><td>€</td></tr><tr><td>Beard trim</td><td>€</td></tr></tbody></table></div></div></div></section><section id=\"contact\" class=\"py-5\"><div class=\"container\"><div class=\"row align-items-start\"><div class=\"col-12 col-lg-5\"><h2>Contact</h2><p><strong>Address:</strong> Hatallova 13, Bratislava</p><p><strong>Phone:</strong> <a href=\"tel:+421903123456\">0903 123 456</a></p></div><div class=\"col-12 col-lg-7\"><h2>Map</h2><div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div></div></div></div></section>"
  }
}
```

Vyššie uvedený prompt sme získali nasledovnou požiadavkou v ChatGPT modeli ChatGPT 5 PRO, pričom sme priložili vytlačenú časť z dokumentácie o [PageBuilder blokoch](../../../frontend/page-builder/blocks.md).

Potrebujem aby si mi pomohol s prípravou promptu pre Chat GPT alebo iný AI model. Pripravujem WebJET CMS kde používateľ môže zadať požiadavku/prompt na vytvorenie web stránky. Príklad:Vytvor web stránku pre kadernícky salón v bratislave, Je otvorený každý deň od 8:00 do 15:00 a špecializuje sa na strihanie mužov. Robí aj úpravu brady/barber. Sídli na ulici Hatallova 13. Telefón 0903 123 456. Na zobrazenie je použitý framework Bootstrap 5. Negeneruj žiadne nové CSS štýly, používaj výlučne CSS triedy dostupné v Bootstrap 5 ako btn-primary, farby a podobne. Úloha je vytvoriť HTML štruktúru skladajúcu sa z nasledovných elementov/stavebných prvkov: `<section>` - hlavný stavebný prvok, oddeľuje jednotlivé sekcie, typický na celú šírku obrazovky `<div class="container">` - kontajner pre textové časti `<div class="row">` - riadok obsahuje col elementy `<div class="col">` - finálny stĺpec s textom, jedine tu môže používateľ upravovať text, upraviteľný text nemôže byť v inom elemente, pretože používateľ by ho nemohol zmeniť. Môže obsahovať vnorené elementy ako P, TABLE, IMG atď. Viac informácii o štruktúre HTML kódu pre nástroj PageBuilder vo WebJET CMS je v priloženom PDF dokumente. SUMARIZÁCIA: tvoja úloha je pripraviť štrukturovaný JSON prompt pre AI spĺňajúci hore uvedené kritéria. Používateľ k promptu pripojí požiadavku na obsah web stránky a výsledkom bude HTML kód.
