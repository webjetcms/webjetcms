# Languages

- Conf. variable `languages` determines what languages are available when selecting a language in the cms, e.g. for setting the language of a folder or a template. It does not apply to the gallery application, which does not yet have a dynamic language model.
- Two-digit codes separated by commas are entered, the default value is `sk,cz,en,de,pl,hu,cho,ru,esp`.
- If you decide to add some language, e.g. `it`, in addition to adding it to the variable `languages` need to be supplemented:
  - Translation key `language.it` with value `Taliansky` in the Translation Keys app.
  - File with translation keys `text_it.properties`, or for a project `text_it-INSTALL_NAME.properties` where `INSTALL_NAME` is the name of the installation.
