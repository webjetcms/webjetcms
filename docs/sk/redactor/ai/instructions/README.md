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
  "purpose": "Premeň nasledujúcu požiadavku používateľa na čistý HTML úryvok vhodný do WebJET CMS PageBuilder. HTML musí byť postavené výhradne na Bootstrap 5 grid systéme a komponentoch. Výstup musí byť len HTML (žiadny Markdown, žiadne vysvetlenie).",
  "input_contract": {
    "user_request": "{userPrompt}",
    "current_html_code": "{inputText}",
    "options_optional": {
      "language": "auto - použi jazyk user_requestu",
      "sections_hint": "vlož sekcie: 'hero, služby, cenník, kontakt'",
      "brand_tone": "profesionálny a priateľský",
      "image_placeholders": {
          "background-bokeh": "<img src=\"/components/htmlbox/objects/placeholder/background-bokeh.jpg\" alt="" class=\"img-responsive img-fluid\" />",
          "background-sandstone": "<img src=\"/components/htmlbox/objects/placeholder/background-sandstone.jpg\" alt="" class=\"img-responsive img-fluid\" />",
          "background-splashing": "<img src=\"/components/htmlbox/objects/placeholder/background-splashing.jpg\" alt="" class=\"img-responsive img-fluid\" />",
          "content-computer": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-computer.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
          "content-lake": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-lake.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
          "content-wolf-howling": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-wolf-howling.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
          "content-woman-face": "<img src=\"/thumb/components/htmlbox/objects/placeholder/content-woman-face.jpg?w=320&h=240&ip=5\" alt=\"\" class=\"fixedSize-320-240-5\" />",
      },
      "map_if_address": "ak existuje adresa, vlož mapu cez kód v ktorom nahradíš ADDRESS_PLACEHOLDER za skutočnú adresu: !INCLUDE(/components/map/map.jsp, object=&quot;ADDRESS_PLACEHOLDER&quot;, offsetX=0,offsetY=0, widthPercent=100, heightPercent=45, labelAddress=true, labelComment=false, zoom=13, label=&quot;&quot;,sizeInPercent=&quot;true&quot;, showControls=&quot;false&quot;, closeLabel=&quot;false&quot;, showContentString=&quot;true&quot;, scrollwheel=&quot;true&quot;, key=&quot;&quot;)!",
      "use_images": "používaj ilustračné obrázky pre karty, headline, hero, services na zlepšenie vizuálnej podoby stránky",
      "max_sections": 8
    }
  },
  "absolute_rules": [
    "VÝSTUP = iba čistý HTML kód, bez Markdownu, bez komentárov a bez dodatočného textu.",
    "Nepoužívaj žiadne <style>, žiadne inline 'style' atribúty, ani vlastné CSS triedy. Používaj výlučne triedy Bootstrapu 5 a nevyhnutné systémové atribúty (napr. data-bs-*).",
    "Každý obsah je rozdelený do blokov so štruktúrou: <section> → <div class=\"container\"> → <div class=\"row\"> → jeden alebo viac <div class=\"col-…\">.",
    "Nikdy nevkladaj text mimo <div class=\"col-…\">. Prvky <section>, .container a .row nesmú obsahovať priamy text.",
    "V stĺpcoch nepoužívaj priamy text-node. Text musí byť vnútri elementov (minimálne <p>, vhodné aj <h1>-<h6>, <ul>/<ol>, <table> atď.).",
    "Vždy používaj 'col-*' triedy (napr. col-12, col-md-6). Nepoužívaj holé 'col'.",
    "Používaj len Bootstrap 5 komponenty a utility (grid, spacing, typography, buttons, tables, ratio, forms).",
    "Odkazy na telefón formátuj ako <a href=\"tel:+421XXXXXXXXX\">…</a>. E-maily ako <a href=\"mailto:…\">…</a>.",
    "Obrázky: vždy pridaj výstižný alt. Pridaj class=\"img-fluid\". Nepoužívaj pevné šírky/výšky.",
    "Tituly: presne jeden <h1> na celú stránku; ostatné nadpisy hierarchicky (<h2>, <h3> …).",
    "Ak je v user_requeste adresa a voľba map_if_address=true, vlož mapu do <div class=\"col-…\"> s <div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div>.",
    "Ak komponent vyžaduje ID (napr. accordion/carousel), použi ID s '__ID__' (napr. id=\"accordion__ID__\").",
    "Nepoužívaj JavaScript mimo data-bs-* atribútov Bootstrapu.",
    "Obsah generuj v jazyku 'options_optional.language' alebo (ak je 'auto') v jazyku user_requestu.",
    "Dodržuj prístupnosť: alt texty, zrozumiteľná štruktúra nadpisov, dostatočné kontrasty využitím bootstrap farieb, aria-* pre interaktívne komponenty ak treba."
  ],
  "allowed_children_inside_col": [
    "h1","h2","h3","h4","h5","h6",
    "p","small","strong","em",
    "ul","ol","li",
    "a",
    "img",
    "table","thead","tbody","tr","th","td",
    "blockquote","figure","figcaption",
    "hr",
    "address",
    "form","label","input","textarea","select","option","button",
  ],
  "layout_guidelines": [
    "Používaj utility triedy pre odsadenia/medzery (napr. py-5, my-5).",
    "Nepoužívaj utility triedu na nastavenie gutter (napr. g-4, g-5, g-6)."
    "Ak je to vhodné, zarovnávaj obsah vertikálne pomocou .align-items-center na .row.",
    "Pre CTA používaj .btn .btn-primary alebo .btn-outline-primary.",
    "Pre galérie a zoznamy služieb používaj viac stĺpcov (napr. col-12 col-md-6 col-lg-4) v jednom .row.",
    "ak URL adresa obrázka začína na /thumb zachovaj CSS triedu fixedSize z príkladu"
  ],
  "section_blueprints_optional": [
    {
      "name": "hero_with_cta",
      "description": "Úvodný blok s titulkom, podtitulom, krátkym opisom a tlačidlom.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row align-items-center\"><div class=\"col-12 col-lg-6\"><h1>Hlavný titulok</h1><p>Stručný opis hodnoty pre zákazníka.</p><p><a href=\"#kontakt\" class=\"btn btn-primary\">Zavolať/Objednať</a></p></div><div class=\"col-12 col-lg-6\"><img src=\"\" alt=\"Ilustračný obrázok\" class=\"img-fluid\"></div></div></div></section>"
    },
    {
      "name": "services_list",
      "description": "Prehľad služieb v dvoch až troch stĺpcoch.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12 col-md-6 col-lg-4\"><h2>Služby</h2><ul><li>Položka služby</li><li>Položka služby</li></ul></div><div class=\"col-12 col-md-6 col-lg-4\"><h2>&nbsp;</h2><ul><li>Položka služby</li><li>Položka služby</li></ul></div><div class=\"col-12 col-md-6 col-lg-4\"><h2>&nbsp;</h2><ul><li>Položka služby</li><li>Položka služby</li></ul></div></div></div></section>"
    },
    {
      "name": "services_card_with_image",
      "description": "Prehľad služieb v dvoch až troch stĺpcoch s ilustračným obrázkom.",
      "template": "<section class=\"py-5 bg-light\"><div class=\"container\"><div class=\"row text-center\"><div class=\"col-12\"><div aria-multiline=\"true\" aria-readonly=\"false\" class=\"column-content\"><h2 class=\"mb-5\">Služby</h2></div></div></div><div class=\"row\"><div class=\"col-12 col-md-6 col-lg-4 mb-4\"><div aria-multiline=\"true\" aria-readonly=\"false\" class=\"column-content\"><div class=\"card h-100 text-center\"><img alt=\"\" class=\"card-img-top\" src=\"\" /><div class=\"card-body\"><h3 class=\"h5 card-title\">Title</h3><p class=\"card-text\">Description.</p></div></div></div></div></div></div></section>"
    },
    {
      "name": "pricing_table",
      "description": "Jednoduchý cenník v tabuľke.",
      "template": "<section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12\"><h2>Cenník</h2><table class=\"table\"><thead><tr><th>Služba</th><th>Cena</th></tr></thead><tbody><tr><td>Položka</td><td>€</td></tr></tbody></table></div></div></div></section>"
    },
    {
      "name": "contact_and_map",
      "description": "Kontakt + mapa v dvoch stĺpcoch.",
      "template": "<section id=\"kontakt\" class=\"py-5\"><div class=\"container\"><div class=\"row align-items-start\"><div class=\"col-12 col-lg-5\"><h2>Kontakt</h2><p><strong>Adresa:</strong> …</p><p><strong>Telefón:</strong> <a href=\"tel:+421XXXXXXXXX\">+421 XXX XXX XXX</a></p><p><strong>Otváracie hodiny:</strong> …</p></div><div class=\"col-12 col-lg-7\"><h2>Mapa</h2><div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div></div></div></div></section>"
    }
  ],
  "self_checklist_before_return": [
    "Je každá textová položka vo vnútri <div class=\"col-…\">?",
    "Nepoužil sa nikde 'class=\"col\"' bez pomlčky? (musí byť col-*)",
    "Neobsahuje výstup <style>, inline style, vlastné triedy ani JS?",
    "Je presne jeden <h1> a nadpisy sú hierarchické?",
    "Majú <img> atribút alt a class=\"img-fluid\"?",
    "Ak sú v kóde ID pre komponenty, používajú '__ID__'?"
  ],
  "output_format": {
    "type": "html_fragment",
    "only_html_no_markdown": true,
    "no_explanations": true
  },
  "example": {
    "user_request": "Vytvor web stránku pre kadernícky salón v Bratislave. Otvorené denne 8:00–15:00, špecializácia na pánsky strih a úpravu brady (barber). Adresa Mlynské Nivy 71, Bratislava. Telefón 0903 123 456.",
    "html_result": "<section class=\"py-5\"><div class=\"container\"><div class=\"row align-items-center\"><div class=\"col-12 col-lg-6\"><h1>Kadernícky salón — Bratislava</h1><p>Špecializujeme sa na pánske strihy a precíznu úpravu brady.</p><p><strong>Otváracie hodiny:</strong> denne 8:00–15:00</p><p><strong>Adresa:</strong> Hatallova 13, Bratislava</p><p><strong>Telefón:</strong> <a href=\"tel:+421903123456\">0903 123 456</a></p><p><a href=\"#kontakt\" class=\"btn btn-primary\">Objednať sa</a></p></div><div class=\"col-12 col-lg-6\"><img src=\"\" alt=\"Pánsky strih a barber\" class=\"img-fluid\"></div></div></div></section><section class=\"py-5\"><div class=\"container\"><div class=\"row\"><div class=\"col-12 col-md-6\"><h2>Služby</h2><ul><li>Pánsky strih</li><li>Úprava brady / barber</li><li>Horúci uterák</li></ul></div><div class=\"col-12 col-md-6\"><h2>Cenník</h2><table class=\"table\"><thead><tr><th>Služba</th><th>Cena</th></tr></thead><tbody><tr><td>Pánsky strih</td><td>€</td></tr><tr><td>Úprava brady</td><td>€</td></tr></tbody></table></div></div></div></section><section id=\"kontakt\" class=\"py-5\"><div class=\"container\"><div class=\"row align-items-start\"><div class=\"col-12 col-lg-5\"><h2>Kontakt</h2><p><strong>Adresa:</strong> Hatallova 13, Bratislava</p><p><strong>Telefón:</strong> <a href=\"tel:+421903123456\">0903 123 456</a></p></div><div class=\"col-12 col-lg-7\"><h2>Mapa</h2><div class=\"ratio ratio-16x9\">!INCLUDE(/components/map/map.jsp...)!</div></div></div></div></section>"
  }
}
```

Vyššie uvedený prompt sme získali nasledovnou požiadavkou v ChatGPT modeli ChatGPT 5 PRO, pričom sme priložili vytlačenú časť z dokumentácie o [PageBuilder blokoch](../../../frontend/page-builder/blocks.md).

Potrebujem aby si mi pomohol s prípravou promptu pre Chat GPT alebo iný AI model. Pripravujem WebJET CMS kde používateľ môže zadať požiadavku/prompt na vytvorenie web stránky. Príklad:Vytvor web stránku pre kadernícky salón v bratislave, Je otvorený každý deň od 8:00 do 15:00 a špecializuje sa na strihanie mužov. Robí aj úpravu brady/barber. Sídli na ulici Hatallova 13. Telefón 0903 123 456. Na zobrazenie je použitý framework Bootstrap 5. Negeneruj žiadne nové CSS štýly, používaj výlučne CSS triedy dostupné v Bootstrap 5 ako btn-primary, farby a podobne. Úloha je vytvoriť HTML štruktúru skladajúcu sa z nasledovných elementov/stavebných prvkov: `<section>` - hlavný stavebný prvok, oddeľuje jednotlivé sekcie, typický na celú šírku obrazovky `<div class="container">` - kontajner pre textové časti `<div class="row">` - riadok obsahuje col elementy `<div class="col">` - finálny stĺpec s textom, jedine tu môže používateľ upravovať text, upraviteľný text nemôže byť v inom elemente, pretože používateľ by ho nemohol zmeniť. Môže obsahovať vnorené elementy ako P, TABLE, IMG atď. Viac informácii o štruktúre HTML kódu pre nástroj PageBuilder vo WebJET CMS je v priloženom PDF dokumente. SUMARIZÁCIA: tvoja úloha je pripraviť štrukturovaný JSON prompt pre AI spĺňajúci hore uvedené kritéria. Používateľ k promptu pripojí požiadavku na obsah web stránky a výsledkom bude HTML kód.
