# Folder settings

To view the folder settings, right-click the folder and select Folder Settings. The window contains the following tabs:
- Basic
- Indexing (**Warning:** shown only in special circumstances)
- Use

## Basic

Card **Basic** provides basic information about the folder as well as the ability to restrict access rights using the User Group.

Use the "Index files for search" option to enable indexing of the files in the folder.

![](folder_settings_basic.png)

## Indexing

Card **Indexing** will be displayed ONLY if the URL of the folder starts with the value `/files`. Used for file indexing actions. Indexing is performed ONLY if enabled in the tab [Basic](#Basic).

![](folder_settings_index.png)

After pressing the "Index" button, the indexing will start, which can take several minutes.

| Indexing is not enabled or files to index were not found | Indexing is enabled and files to index were found |
| :----------------------------------------------------------------: | :-----------------------------------------------------: |
| ![](folder_settings_index_empty.png)                | ![](folder_settings_index_not-empty.png)         |

## Use

Card **Use** shows the use of a folder in the form of a nested datatable. Each record represents a web page. The datatable contains columns:
- Name, website
- URL address, website

The two values are simultaneously lines pointing to different locations.

**Name** web site is a link to [List of web pages](../../../../redactor/webpages/README.md), where the web page is searched and the editor is automatically opened.

![](folder_link_A.png)

**URL address** web page is a link directly to the web page.

![](folder_link_B.png)
