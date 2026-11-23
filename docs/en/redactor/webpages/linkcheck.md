# Checking links and blank pages

You can open the check in the list of websites via **More actions → Check links and empty pages**. It works with the current folder and its subfolders; you do not need to mark the pages.

The scan result will be displayed in 3 tabs:

- Broken links - links that do not exist. Only local links are checked, not links to external sites (starting with http).
- Pages with display disabled - pages that have their display disabled.
- Empty pages - pages that are empty (have less than 100 characters, this number can be set in the config variable ```linkCheckEmptyPageSize```).

![](linkcheck-datatable.png)

All tabs contain a table showing the website and a description of the error. The table contains the following columns:

- ID - ```id``` website (```docid```).
- Page - path (folder structure) and name of the web page. Clicking on the link will display the web page editing.
- Error - description of the error found when checking the link and page.
- Url address - URL address of the broken link, or URL address of the page (in the Empty Pages tab). Clicking on the link will open the displayed Url address in a new window.
