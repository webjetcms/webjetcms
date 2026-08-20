# Template groups

Template groups allow you to **set metadata for multiple templates**, such as project name, author, copyright, etc. Template grouping is especially useful in projects where there are multiple domains with different designs/templates.

## List of template groups

The list of template groups shows an overview of the created groups, by default there is a group unassigned.

![](temps-groups.png)

## Template Group Editor

## Basic tab

![](temps-groups-edit.png)

- Template group name - unique name of the template group.
- Folder - the main folder containing the template files (JSP/HTML design file, CSS styles, JavaScript files).
- Page editor type:
  - Standard - the main page editor with text formatting, inserting images, links, etc.
  - HTML editor - used for special pages that need to use a precise HTML editor.
  - Page Builder - page editing mode consisting of [prepared blocks](../page-builder/README.md).
  - `Grid Editor` - ​​predecessor of Page Builder mode, will be removed in 2024.
- Number of uses - the number of templates in this group.

## Metadata tab

![](temps-groups-edit-metadata.png)

- Allows you to set the metadata used in the [template header](../thymeleaf/webjet-objects.md#ninja-template).
- Prefix of text keys - by entering a prefix, it is possible to modify the translation keys of applications embedded in the page. Example: if the application uses the translation key `components.inquiry.answers` and you set the prefix `jetportal` in the template group, then the key `jetportal.components.inquiry.answers` will be searched for when displaying the application and if it exists, it will be used. If there is no translation for this key, the original value from the key `components.inquiry.answers` will of course be used. This way you can easily change the text displayed in the application in a certain template group.

## SEO tab

![](temps-groups-edit-seo.png)

The tab allows you to set default SEO values ​​for pages using a template from a given group:

- Default SEO description - used if the page does not have an SEO description or perex specified.
- Default SEO image - used if the page does not have a specified SEO image or perex image. If it is not set in the template group, the value `defaultSeoImage` from the template configuration file will be used.
- Default SEO image alt text - used if the page does not have its own alt text specified in the P field.

The SEO description and alternative text are stored separately for the language selected in the list header. The SEO image is common to all languages ​​in the group. The exact order of values ​​is described in the [Page object API](../ninja-starter-kit/ninja-jv/page/README.md).

## Optional fields tab

![](temps-groups-edit-fields.png)

It is possible to define [optional fields](../webpages/customfields/README.md) for a group, the use of which depends on the template designer.
