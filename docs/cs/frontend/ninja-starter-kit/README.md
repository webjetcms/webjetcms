# Úvod

Ninja Starter Kit reprezentuje ukázkovou šablonu ve formátu JSP s využitím objektů Ninja z WebJET CMS.

# Instalace

- Zkopíruj si **Ninja Starter KIT** ze [ZIP archivu](ninja-starter-kit.zip) do svého projektu, do kořenové složky `/templates/installName`.
- Pokud je třeba, přejmenujte složku `ninja-starter-kit` na nový název šablony dle vlastního uvážení.
- Nastav si správné cesty v `SCSS` souboru `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
  - `$wj-install-name`: název hlavní složky v `/templates` pro daný projekt (rovanký jako konstanta `installName` nastavena ve webJete)
  - `$wj-template-name`: název kořenové složky šablony (pokud se měnil název)
- Zkontroluj si nastaveno `breakpointy` v `SCSS` souboru `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
  - Aktuální jsou `breakpointy` nastaveno podle Bootstrap 4
  - Tyto `breakpointy` se přebírají i do [Ninja pluginu](ninja-js/events/README.md)

## Legenda použitých emoji

- :muscle: Ninja **Starter KIT**
- :rocket: Ninja **Boilerplate**
- :gem: Ninja **Java**
- :tophat: Ninja **Script**
- :cherries: Ninja **Style**

**:muscle: = :rocket: + :gem: + :tophat: + :cherries:**

- :ghost: složka `/ninja-starter-kit/installName`
- :carousel\_horse: Třída **DocDetails.java**
- :european\_castle: **Skupina** šablon
