# Setting

## Create a new template group

A new group is created by default for the **source language** of the project (in most cases this is **Slovak**, but this is set in the constant `defaultLanguage`).

This means that all entered values ​​(except Group Name and Folder) in the fields when creating a group are created by default in text keys for the default language.

## Deleting a group

A template group can only be **deleted** if** the given template group is **not used** (no template has it set).

When deleting a group, the template is **not physically deleted** in the /template/ directory, **but all information about the template group stored in the database** + all defined text keys for the group are deleted.

## Text editing language

The table with the list of template groups **displays texts by default for the set default language** in the project (in most cases this is Slovak, but this is set in the constant `defaultLanguage`).

If you select a different language, the table will be loaded with new texts for the selected language.

- If text is not defined in this language, an empty cell (without text) is displayed.
- Just because an empty cell is displayed does not mean that an empty value will be displayed on the front-end.
    - **Text display on the front-end works as follows:**
        - searches for text defined in **currently displayed language** (for example, in EN)
        - if the text does not exist, it searches in the language set in the constant `defaultLanguage` (for example, CZ)
        - if the text does not exist, it searches hard in **SK** language

## Group editing

Each template group can be edited.

When editing, a modal window opens, containing fields with default values ​​that are set for the currently selected language.

**Edit example:**

- The selected language for editing texts is English
- when editing a group, the fields are pre-selected with the values ​​set for the English language
- When editing a template, it is not possible to change:
    - language (this is taken from the Language for text editing selection field)
    - the Name and Folder fields are set globally for all languages, the field cannot be defined individually for each language

## Using a group

In the virtual template editor, there is a **Template group** field in which one group is set as needed from the list of all template groups.

After selecting a group, all **available HTML templates for the selected group** will be displayed in the **HTML Template** selection field (all **JSP files** in the physical directory of the selected group will be displayed except for the **/includes/** directory).

## Using a text key prefix

*Example of prefix usage:*
```properties
testujemPrefix.editor.field_a=GPS súradnice
```

For all templates in the group with the prefix **testujemPrefix** set, the **name of the optional field A** in the page is set to "**GPS coordinates**".

## Group migration

In case of migrating a group to another environment, the following steps must be taken:

- transfer the physical directory of the group in `/templates/` from environment A to environment B
    - if the JSP files of the migrated template contain directly linked modules and other JSP files that are not on environment B, transfer those as well
- check the set variables `$wj-install-name` and `$wj-template-name` in the file `_variables.scss`
- Create and set up a group on environment B based on the model from environment A
- Exporting text keys from environment A with the prefix set in the **Text key prefix** field and importing to environment B