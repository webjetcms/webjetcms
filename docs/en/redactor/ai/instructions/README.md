# Writing instructions

When writing instructions for the OpenAI response API, follow these recommendations:

1. **Be specific** Describe clearly and precisely what you expect from the answer. Avoid ambiguity.

2. **Use plain language** Formulate instructions clearly so that they are easily understood by both the system and the user.

3. **Do not define the output format** The format of the output is handled by the implementation, which decides based on the request type and other parameters.

4. **Give examples** Attach samples of the required input and output if possible.

5. **Avoid unnecessary details** Focus on the essential information that is important to the outcome.

6. **Specify the language of the answer** If the answer is to be in a specific language, make this explicit.

## Example of correct instruction

> Generate a list of three tips to improve SEO for a website in English in the form of a numbered list.

By following these principles, you will ensure that the answers are relevant and usable.

## Images

For instructions for generating and editing images, you need to insert a tag in the instructions `{userPrompt}` to insert a user-specified request. This tag is replaced by the entered text. Instruction examples:

Generate image

```json
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```

Edit image:

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

## HTML code improvements

For advanced editing of HTML code it is necessary to get the modified code but also an explanation of what was modified. If the AI response contains a block at the beginning

````text
  ```html
````

so the content of this block is treated as HTML code, and everything after this block is displayed to the visitor in the assistant dialog as a response.

Example instruction:

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

Note the defining `output_format`, which defines the creation of 2 sections with HTML code and explanatory notes on the modifications.

## AI in the browser

Writing instructions for AI in the browser is specific to [API options available](https://developer.chrome.com/docs/ai/built-in-apis). The instruction is divided into the API name and the subsequent configuration. In the reference to `MDM` documentation you will find detailed information on the configuration options.

Although the API supports language settings, it usually only supports English language at the moment. You can add an option to the configuration `"translateOutputLanguage": "autodetect"` for subsequent translation. Value `autodetect` detects the language of the original text and uses it. Alternatively, you can specify a fixed value, e.g. `sk`, or the value `userLng` where the language is set according to the language of the currently logged in user.

### Summary of the text

API [Summarizer](https://developer.mozilla.org/en-US/docs/Web/API/Summarizer) is used to summarize the entered text. Using the attribute `type` you can set the generation range.

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

### Translator

The API is used for translation [Translator](https://developer.mozilla.org/en-US/docs/Web/API/Translator_and_Language_Detector_APIs).

```JavaScript
Translator:
{
  "sourceLanguage": "sk",
  "targetLanguage": "en"
}
```

As a value to `sourceLanguage` it is possible to enter an expression `autodetect` to automatically detect the language of the text you enter, or `userLng` to set the language of the currently logged in user. The value `userLng` can also be entered in the field `targetLanguage`. So you can create a Translate to current language assistant where the source language is automatically detected and the target language is set according to the language of the logged in user.

```JavaScript
Translator:
{
  "sourceLanguage": "autodetect",
  "targetLanguage": "userLng"
}
```

### Writing text

To write new text, you can use [Writer](https://developer.chrome.com/docs/ai/built-in-apis).

```JavaScript
Writer:
{
   "tone": "neutral",
   "format": "plain-text",
   "length": "medium",
   "sharedContext": "You are a skilled expert in marketing and SEO optimization. Odpovedaj v slovenskom jazyku"
}
```

### Text editing

To edit existing text, use [Rewriter](https://developer.chrome.com/docs/ai/built-in-apis). Using the attribute `tone` it is possible to change the formality of the text.

```JavaScript
Rewriter:
{
   "tone": "more-formal",
   "format": "as-is",
   "length": "as-is"
}
```

### Advanced expressions

For advanced expressions it is possible to use [LanguageModel API](https://developer.chrome.com/docs/ai/prompt-api) which allows you to formulate the request in natural language.

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

## Replacement of expressions

In the instructions, special marks are replaced before use, you can use these to insert the data in the exact place.

- `{userPrompt}` - inserts the text entered by the user from the input dialog (if the Require user input option is selected).

```json
{
   "task": "generate image",
   "imageDescription": "{userPrompt}",
   "returnFormat": "Return only generated image, without any other response. Image return as Base64."
}
```
- `{inputText}` - input text, the value of the field whose name is specified in the Source field in the Assistant settings.
- `{language}` - the language of the last viewed web page may be different from the language of the currently logged-in user. For example, you are logged in in the Slovak interface, but you are editing the English version of a web page.
- `{userLanguage}` - the language in which the user is logged in.

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

## Generating PageBuilder blocks

The following instruction allows you to generate [PageBuilder blocks for editor](../datatables/README.md#chat-mode), it needs to be set as a request type `Chat`, in which case it is also filled in correctly `{inputText}` when editing.

For the setup we recommend the model `gemini-2.5-pro`, set the entity in the action `sk.iway.iwcm.doc.DocDetails`, set the source and destination fields to `data`. In advanced you need to enable the option Preserve HTML code and Require user input.

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

We obtained the above prompt by making the following request in the ChatGPT model ChatGPT 5 PRO, attaching a printed section from the documentation on [PageBuilder blocks](../../../frontend/page-builder/blocks.md).

I need you to help me to prepare a prompt for Chat GPT or another AI model. I am preparing a WebJET CMS where user can enter a request/prompt to create a web page. Example:Create a website for a hairdressing salon in bratislava, It is open every day from 8:00 to 15:00 and specializes in men's haircuts. They also do beard grooming/barber. It is located at 13 Hatallova Street. Phone 0903 123 456. The Bootstrap 5 framework is used for display. Don't generate any new CSS styles, use only CSS classes available in Bootstrap 5 like btn-primaries, colors and so on. The task is to create an HTML structure consisting of the following elements/elements: `<section>` - main building element, separating the sections, typical for the whole width of the screen `<div class="container">` - container for text parts `<div class="row">` - row contains col elements `<div class="col">` - the final text column, only here the user can edit the text, the editable text cannot be in another element because the user would not be able to change it. It can contain nested elements like P, TABLE, IMG etc. More information about the HTML code structure for the PageBuilder tool in WebJET CMS is in the attached PDF document. SUMMARY: your task is to prepare a structured JSON prompt for the AI meeting the above criteria. The user will attach a request for web page content to the prompt and the result will be HTML code.
