# Úvod

Ninja Starter Kit reprezentuje ukážkovú šablónu vo formáte JSP s využitím objektov Ninja z WebJET CMS.

# Inštalácia

- Skopíruj si **Ninja Starter KIT** zo [ZIP archívu](ninja-starter-kit.zip) do svojho projektu, do koreňového priečinka `/templates/installName`.
- Ak je potrebné, premenujte priečinok `ninja-starter-kit` na nový názov šablóny podľa vlastného uváženia.
- Nastav si správne cesty v `SCSS` súbore `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
    - `$wj-install-name`: názov hlavného priečinka v `/templates` pre daný projekt (rovanký ako konštanta `installName` nastavená vo webJete)
    - `$wj-template-name`: názov koreňového priečinka šablóny (ak sa menil názov)
- Skontroluj si nastavené `breakpointy` v `SCSS` súbore `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
    - Aktuálne sú `breakpointy` nastavené podľa Bootstrap 4
    - Tieto `breakpointy` sa preberajú aj do [Ninja pluginu](ninja-js/events/README.md)

## Legenda použitých emoji

- :muscle: Ninja **Starter KIT**
- :rocket: Ninja **Boilerplate**
- :gem: Ninja **Java**
- :tophat: Ninja **Script**
- :cherries: Ninja **Style**

**:muscle: = :rocket: + :gem: + :tophat: + :cherries:**

- :ghost: priečinok `/ninja-starter-kit/installName`
- :carousel_horse: Trieda **DocDetails.java**
- :european_castle: **Skupina** šablón