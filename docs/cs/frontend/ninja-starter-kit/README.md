# Úvod

Ninja Starter Kit představuje ukázkovou šablonu ve formátu JSP využívající objekty Ninja z WebJET CMS.

# Instalace

- Kopírovat **Startovací sada Ninja** Z [Archiv ZIP](ninja-starter-kit.zip) do projektu, do kořenové složky `/templates/installName`.
- V případě potřeby složku přejmenujte `ninja-starter-kit` na nový název šablony podle vašeho výběru.
- Nastavení správných cest v `SCSS` ve složce na adrese `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
  - `$wj-install-name`: název hlavní složky v `/templates` pro daný projekt (rovná se konstantě `installName` nastaveno v aplikaci webJet)
  - `$wj-template-name`: název kořenové složky šablony (pokud se název změnil)
- Zkontrolujte nastavení `breakpointy` v `SCSS` ve složce na adrese `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
  - V současné době jsou `breakpointy` nastaveno pomocí Bootstrap 4
  - Tyto stránky `breakpointy` se rovněž přenášejí do [Zásuvný modul Ninja](ninja-js/events/README.md)

## Legenda použitých emoji

- :muscle: Ninja **Startovací sada**
- :rocket: Ninja **Šablona**
- :gem: Ninja **Java**
- :tophat: Ninja **Skript**
- :cherries: Ninja **Styl**

**:muscle: = :rocket: + :gem: + :tophat: + :cherries:**

- :ghost: složka `/ninja-starter-kit/installName`
- :carousel\_horse: Třída **DocDetails.java**
- :european\_castle: **Skupina** šablony
