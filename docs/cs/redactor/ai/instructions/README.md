# Psaní instrukcí

Při psaní instrukcí pro OpenAI response API dodržujte tato doporučení:

1. **Buďte konkrétní** Jasně a přesně popište, co očekáváte od odpovědi. Vyhněte se nejednoznačnosti.

2. **Používejte jednoduchý jazyk** Formulujte instrukce srozumitelně, aby byly snadno pochopitelné pro systém i uživatele.

3. **Nedefinujte formát výstupu** O formát výstupu se stará implementace, která rozhodne na základě typu požadavku a jiných parametrů.

4. **Uveďte příklady** Přiložte ukázky požadovaného vstupu a výstupu, pokud je to možné.

5. **Vyhněte se zbytečným detailům** Zaměřte se na podstatné informace, které jsou důležité pro výsledek.

6. **Specifikujte jazyk odpovědi** Pokud má být odpověď v konkrétním jazyce, uveďte to explicitně.

## Příklad správné instrukce

> Vygeneruj seznam tří tipů pro zlepšení SEO pro webovou stránku v češtině ve formě očíslovaného seznamu.

Dodržováním těchto zásad zajistíte, že odpovědi budou relevantní a použitelné.

## Obrázky

Při instrukcích pro generování a úpravu obrázků je třeba do instrukcí vložit značku `{userPrompt}` pro vložení zadaného požadavku uživatele. Tato značka je nahrazena zadaným textem. Příklady instrukcí:

Vygenerovat obrázek

```json
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```

Upravit obrázek:

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

## Vylepšení HTML kódu

Při pokročilých úpravách HTML kódu je třeba získat upravený kód ale také vysvětlení co bylo upraveno. Pokud odpověď od AI obsahuje na začátku blok

````text
  ```html
````

tak je obsah tohoto bloku je považován za HTML kód a vše co je následně za tímto blokem se zobrazí návštěvníkovi v dialogu asistenta jako odpověď.

Příklad instrukce:

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

Všimněte si definování `output_format`, který definuje vytvoření 2 sekcí s HTML kódem a vysvětlivkami k úpravám.

## AI v prohlížeči

Psaní instrukcí pro AI v prohlížeči je specifické podle [možností dostupného API](https://developer.chrome.com/docs/ai/built-in-apis). Instrukce je rozdělena na název API a následnou konfiguraci. V odkazu na `MDM` dokumentaci naleznete podrobné informace o možnostech konfigurace.

I když API podporuje nastavení jazyka, aktuálně obvykle podporuje pouze anglický jazyk. Do konfigurace můžete přidat možnost `"translateOutputLanguage": "autodetect"` pro následné provedení překladu. Hodnota `autodetect` detekuje jazyk původního textu a tento následně použije. Můžete případně zadat fixní hodnotu. `sk`, nebo hodnotu `userLng` kde se jazyk nastaví podle jazyka aktuálně přihlášeného uživatele.

### Sumarizace textu

API [Summarizer](https://developer.mozilla.org/en-US/docs/Web/API/Summarizer) slouží k sumarizaci zadaného textu. Pomocí atributu `type` můžete nastavit rozsah generování.

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

### Překladač

Pro překlad se používá API [Translator](https://developer.mozilla.org/en-US/docs/Web/API/Translator_and_Language_Detector_APIs).

```JavaScript
Translator:
{
  "sourceLanguage": "sk",
  "targetLanguage": "en"
}
```

Jako hodnotu do `sourceLanguage` lze zadat výraz `autodetect` pro automatickou detekci jazyka zadaného textu, nebo `userLng` pro nastavení jazyka aktuálně přihlášeného uživatele. Hodnotu `userLng` lze zadat i do pole `targetLanguage`. Můžete tedy vytvořit asistenta Přeložit do aktuálního jazyka, kde se zdrojový jazyk automaticky detekuje a cílový se nastaví podle jazyka přihlášeného uživatele.

```JavaScript
Translator:
{
  "sourceLanguage": "autodetect",
  "targetLanguage": "userLng"
}
```

### Psaní textu

Pro psaní nového textu lze použít [Writer](https://developer.chrome.com/docs/ai/built-in-apis).

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

Pro úpravu stávajícího textu se používá [Rewriter](https://developer.chrome.com/docs/ai/built-in-apis). Pomocí atributu `tone` je možné změnit formálnost textu.

```JavaScript
Rewriter:
{
   "tone": "more-formal",
   "format": "as-is",
   "length": "as-is"
}
```

### Pokročilé výrazy

Pro pokročilé výrazy lze použít [LanguageModel API](https://developer.chrome.com/docs/ai/prompt-api), které umožňuje formulovat požadavek v přirozeném jazyce.

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

## Náhrada výrazů

V instrukcích jsou před použitím nahrazeny speciální značky, ty můžete použít pro vložení údajů na přesné místo.

- `{userPrompt}` - vloží zadaný text uživatelem ze vstupního dialogu (je-li zvolena možnost Požadovat vstup od uživatele).

```json
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```
- `{inputText}` - vstupní text, hodnota pole, jehož jméno je zadáno v Zdrojové pole v nastaveních asistenta.
- `{language}` - jazyk naposledy zobrazené web stránky, může být odlišný od jazyka právě přihlášeného uživatele. Například jste přihlášeni v českém rozhraní, ale upravujete anglickou verzi web stránky.
- `{userLanguage}` - jazyk, ve kterém je přihlášen uživatel.

```json
{
  "role": "Web accessibility specialist",
  "task": "Analyze the provided Base64-encoded image and generate a concise, descriptive ALT tag that accurately describes the image content for accessibility purposes. The ALT tag should align with WCAG 2.1 guidelines and be suitable for web use. Output only the ALT text as plain text.",
  "constraints": {
    "language": "{language}",
    "length": "Maximum 100 characters",
    "focus": "Descriptive, relevant, and compliant with WCAG 2.1",
    "avoid": "Generic phrases or irrelevant details",
    "output": "Only the ALT text as plain text, no additional explanations"
  }
}
```

## Generování PageBuilder bloků

Následující instrukce umožňuje generovat [PageBuilder bloky pro editor](../datatables/README.md#režim-chat), je třeba ji nastavit jako typ požadavku `Chat`, v takovém případě se korektně vyplní i `{inputText}` při editaci.

Pro nastavení doporučujeme model `gemini-2.5-pro`, v akci nastavte entitu `sk.iway.iwcm.doc.DocDetails`, zdrojové a cílové pole nastavte na `data`. V pokročilé je třeba zapnout možnost Zachovat HTML kód a Požadovat vstup od uživatele.

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

Výše uvedený prompt jsme získali následujícím požadavkem v ChatGPT modelu ChatGPT 5 PRO, přičemž jsme přiložili vytištěnou část z dokumentace o [PageBuilder blocích](../../../frontend/page-builder/blocks.md).

Potřebuji abys mi pomohl s přípravou promptu pro Chat GPT nebo jiný AI model. Připravuji WebJET CMS kde uživatel může zadat požadavek/prompt na vytvoření web stránky. Příklad: Vytvoř web stránku pro kadeřnický salon v bratislavě, Je otevřen každý den od 8:00 do 15:00 a specializuje se na stříhání mužů. Dělá také úpravu brady/barber. Sídlí na ulici Hatallova 13. Telefon 0903 123 456. K zobrazení je použit framework Bootstrap 5. Negeneruj žádné nové CSS styly, používej výhradně CSS třídy dostupné v Bootstrap 5 jako btn-primary, barvy a podobně. Úloha je vytvořit HTML strukturu skládající se z následujících elementů/stavebních prvků: `<section>` - hlavní stavební prvek, odděluje jednotlivé sekce, typický na celou šířku obrazovky `<div class="container">` - kontejner pro textové části `<div class="row">` - řádek obsahuje col elementy `<div class="col">` - finální sloupec s textem, jedině zde může uživatel upravovat text, upravitelný text nemůže být v jiném elementu, protože uživatel by jej nemohl změnit. Může obsahovat vnořené elementy jako P, TABLE, IMG atp. Více informací o struktuře HTML kódu pro nástroj PageBuilder ve WebJET CMS je v přiloženém PDF dokumentu. SUMARIZACE: tvůj úkol je připravit strukturovaný JSON prompt pro AI splňující výše uvedená kritéria. Uživatel k promptu připojí požadavek na obsah web stránky a výsledkem bude HTML kód.
