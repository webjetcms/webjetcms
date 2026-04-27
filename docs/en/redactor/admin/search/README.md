# Search

## Overview

The **Search** section in the administration allows users to search for content across multiple areas, such as web pages, files, and text keywords. Search provides tools to filter results and narrow the scope of the search, improving the efficiency of working with content.

![](search.png)

## Access to the section:

We can access the **Search** section in the administration using the magnifying glass icon ![](../icon-search.png ":no-zoom") in the header.

See also [Header](../README#header) for more information.

## Key features:

1. **Full-text search**:
   - The search is performed not only in the titles of the pages, but also in their text content.
   - The user can also find content based on keywords found in the text of documents or files.

2. **Filtering results**:
   - In addition to full-text search, column filters can be used for a more precise display of results.
   - Tables allow filtering based on columns such as `Názov web stránky`, `Meno autora` or `Kľúč`.

3. **Support for multiple content types**:
   - Websites
   - Files
   - Text keys

## Using the Search section

### Search Interface:

The section is divided into several tabs. Each tab allows for specific searching and filtering:

#### 1. Website

- **Full-text search:** Enter a keyword that is found in the text of the page or its title.
- **Restriction by tree structure** ![](tree-filter.png ":no-zoom") You can limit the search by selecting the folder in which to search (it also searches in subfolders).
- **Filtering:** Use columns like `Názov web stránky` and `Meno autora` to narrow down your results.
- **User rights:** The results displayed depend on the rights of each user. The user can only see the pages to which they have permission.

#### 2. Files

- **Full-text search:** Search for text found in file contents as well as file names.
- **Filtering:** Columns like `Názov súboru` and `URL adresa` help to precisely specify results.
- **Eye symbol:** Click on the eye icon ![](icon-eye.png ":no-zoom") next to the file name to view the file.
- **URL address:** Links ![](link.png ":no-zoom") allow quick navigation to a directory in Explorer.

!> Warning: [full-text file index](../../files/fbrowser/folder-settings/README.md#indexing) is used to search files, so only files that are indexed are found.

#### 3. Text keys

- **Full-text search:** Find text keys by content in all available languages.
- **Filtering:** Use columns like `Kľúč` or `Jazyk` for more precise searches.

## Usage examples:

- **Website search:**
  - You are searching for the phrase `naštartoval obchodnú stránku`.
  - The results contain pages where this phrase is found in the text. To narrow down the results, use the `sales` filter in the column for `Názov stránky`.

- **File search:**
  - You are looking for `only for users in Bankari group`.
  - The results contain all files where this phrase is found in the text.

- **Text key search:**
  - You are looking for `Pridať adresár`.
  - The results contain keys with Slovak text `Pridať adresár`. You can then use the filter `addGroup` in the column `Kľúč` to narrow down the results.

