# Settings

## Create a new template group

A new group is created by default for **default language** on the project (in most cases it is **Slovak**, but it is set in the constant `defaultLanguage`).

That is, all values entered (except Group Name and Folder) in the fields when creating a group are created by default in the text keys for the default language.

## Deleting a group

A group of templates can be **delete** just in case, **If** a given group of templates **not used** (no template has it set).

When you delete a group, the **does not physically delete the template** in the /template/ directory, **but all template group information stored in the database is deleted** + all defined text keys for the group.

## Text editing language

Table with list of template groups **displays texts by default for the set default language** on the project (in most cases it is Slovak, but it is set in constant `defaultLanguage`).

If another language is selected, the table is loaded with the new texts for the selected language.

- If no text is defined in this language, an empty cell (without text) is displayed.
- However, just because an empty cell is displayed does not mean that the front-end will display an empty value.
	- **The text display on the front-end works as follows:**
        1.  searches for text defined in **the currently displayed language** (for example in EN)

        2.  if the text does not exist, searches in the language set in the constant `defaultLanguage` (for example CZ)

        3.  if the text does not exist, looking hard in the **EN** language

## Modifying the group

Each group of templates can be customized.

When editing, a modal window will open, containing fields with default values that are set for the currently selected language.

**Example of modification:**
- the selected Language for text editing is English
- when editing a group, the fields will default to the values set for the English language

- When editing the template, it is not possible to change:
	- language (this is taken from the Language selection field for text editing)
	- fields Name and Folder are set globally for all languages, it is not possible to define fields individually for each language
## Use of the group

In the virtual template edit, the field **Template group** in which one group is set from the list of all template groups as required.

After selecting a group, the selection box **HTML Template** show all **available HTML templates for the selected group** (all **JSP files** in the physical directory of the selected group, in addition to the directory **/includes/**).

## Using text key prefixes

*Example of prefix usage:*
```properties
testujemPrefix.editor.field_a=GPS súradnice
```

For all templates in the group with the prefix set **I testPrefix** is set **name of the optional field A** in the page on "**GPS coordinates**".

## Group migration

If you are migrating a group to a different environment, you need to do the following:

1.  transfer the physical directory of the group in `/templates/` from environment A to environment B

	- if the JSP files of the migrated template contain directly linked modules and other JSP files that are not on the B environment, migrate them as well
2.  check set variables `$wj-install-name` a `$wj-template-name` in the file `_variables.scss`

3.  Creating and setting up a group on environment B based on a pattern from environment A

4.  Exporting text keys from environment A with the prefix set in the field **Prefix of text keys** and importing to environment B
