# Page Builder

Page Builder je speciální režim pro úpravu stránek. V tomto režimu se neupravuje celá stránka, ale pouze její vybrané části.

Viz příručka pro [webdesignér](../../../frontend/page-builder/README.md), nebo pro [editora](../../../redactor/webpages/pagebuilder.md).

![](../../../redactor/webpages/pagebuilder.png)

## Implementační detaily

Režim se aktivuje nastavením atributu `editingMode=pagebuilder` na objektu [DocEditorFields](../../../../javadoc/sk/iway/iwcm/doc/DocEditorFields.html). V atributu `editingModeLink` je odkaz, který se načte do iframe. Nastavují se pomocí metody `setEditingMode`.

Tento odkaz obsahuje parametr URL `inlineEditorAdmin=true`, podle něj můžete najít místa v kódu implementující funkci Page Builder. Pokud je tento parametr nastaven, přesměrování stránky se neprovádí (pokud má stránka atribut `externalLink`).

Technicky se karta Obsah v editoru skládá ze dvou částí. `div` prvků, z nichž jeden obsahuje standardní editor CK a druhý iframe pro nástroj Page Builder. Kód je vytvořen v `field-type-wysiwyg.js`. Kromě toho se zobrazí výběrové pole, které umožňuje přepínání mezi editory (zajišťuje je nástroj `switchEditingMode`).

## Nová stránka

Nástroj Page Builder se otevírá v iframe jako zobrazená webová stránka, což ztěžuje jeho zobrazení pro novou webovou stránku. V tomto případě se otevře hlavní stránka adresáře, samozřejmě s nastavením prázdných dat. Po uložení je nová webová stránka již správně vytvořena a lze ji upravovat. Nastavení adresy URL pro hlavní stránku se provádí v položce [DocEditorFields.setEditingMode](../../../../javadoc/sk/iway/iwcm/doc/DocEditorFields.html).

## Optimalizace rychlosti načítání

Při inicializaci nástroje Page Builder se editor CK přepne do režimu kódu HTML, aby se zabránilo zbytečnému načítání obrázků, stylů CSS atd. pro editor, který není zobrazen. Naopak ve standardním režimu je iframe pro nástroj Page Builder nastaven na adresu URL `about: blank`.

Objekty vložené do stránky jsou v `ShowDoc.fixDataForInlineEditingAdmin` nastavit na prázdné hodnoty, aby se zabránilo zbytečnému načítání záhlaví, zápatí, menu atd. Ve výchozím nastavení načítá nástroj Page Builder kód HTML pomocí volání AJAX. `/admin/inline/get_page.jsp` pro uložení jednoho volání je hodnota pro `doc_data` atribut přenesen v `inline_page_toolbar.jsp` prostřednictvím objektu JSON `window.inlineEditorDocData` který, pokud existuje, se použije místo AJAXu k načtení dat.
