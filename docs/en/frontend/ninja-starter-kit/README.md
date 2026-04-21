# Introduction

The Ninja Starter Kit represents a sample template in JSP format using Ninja objects from WebJET CMS.

# Installation

- Copy the **Ninja Starter KIT** from the [ZIP archive](ninja-starter-kit.zip) to your project, in the root folder `/templates/installName`.
- If necessary, rename the `ninja-starter-kit` folder to a new template name of your choice.
- Set the correct paths in the `SCSS` file `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
    - `$wj-install-name`: name of the main folder in `/templates` for the given project (same as the constant `installName` set in webJet)
    - `$wj-template-name`: name of the template root folder (if the name was changed)
- Check the settings of `breakpointy` in `SCSS` file `ninja-starter-kit/assets/scss/2-helpers/_variables.scss`:
    - Currently `breakpointy` is set according to Bootstrap 4
    - These `breakpointy` are also included in the [Ninja plugin](ninja-js/events/README.md)

## Legend of emojis used

- :muscle: Ninja **Starter KIT**
- :rocket: Ninja **Boilerplate**
- :gem: Ninja **Java**
- :tophat: Ninja **Script**
- :cherries: Ninja **Style**

**:muscle: = :rocket: + :gem: + :tophat: + :cherries:**

- :ghost: folder `/ninja-starter-kit/installName`
- :carousel_horse: Class **DocDetails.java**
- :european_castle: **Group** of templates