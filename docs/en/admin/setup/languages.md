# Languages

- The conf. variable `languages` determines which languages ​​are available when selecting a language in the CMS, e.g. for setting the language of a folder or template. It does not apply to the gallery application, which does not yet have a dynamic language model.
- Two-digit codes separated by commas are entered, the default value is `sk,cz,en,de,pl,hu,cho,ru,esp`.
- If you decide to add a language, e.g. `it`, then in addition to adding it to the `languages` variable, you need to add:
  - Translation key `language.it` with value `Taliansky` in the Translation Keys application.
  - Translation key file `text_it.properties`, or for project `text_it-INSTALL_NAME.properties`, where `INSTALL_NAME` is the installation name.
