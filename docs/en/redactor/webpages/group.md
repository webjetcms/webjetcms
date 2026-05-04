# Website folder

All pages are organized into folders and form a tree structure of the website. The folders are arranged according to the order that they have set as one of their parameters. The basic structure consists of a main folder, often divided according to the language mutations of the website and can be arbitrarily divided using subfolders according to the logical structure of the website.

After clicking on a folder in the tree structure, the web pages in the selected folder will be displayed.

# Basic tab

![](../../frontend/examples/template-bare/group-editor.png)

- Folder name
- Menu item name - the name that will be displayed in the navigation bar (breadcrumb navigation) and in the generated website menu.
- URL address - the name that will be used to generate the URL for this directory. If it contains a / at the beginning, the generated URL will start with the specified address regardless of the parent folder. If the value - is specified, this folder will be skipped when generating URLs.
  - Apply to all existing subfolders and subpages - if you check this box and save the folder, a new URL will be generated for all subpages of that folder and subfolders.
- Domain - if you are using Multi Domain WebJET and editing the root directory, enter here the domain for which pages are displayed in this directory (and subdirectories).
  - Change domain redirects, configuration variables and translation texts with domain prefix - if you change the domain and select this option, the domain will also be changed in existing redirects, configuration variables and translation keys with the prefix of the original domain.
- Parent folder - the folder that is the parent of this folder. It determines where this folder will be located in the tree structure.
- Folder main page - the web page that will be used as the folder's main web page when you click on the folder name in the menu.
- Public folder availability - determines whether it is a public or private (internal) folder. A private folder is not displayed in the navigation tree structure to regular visitors (it is displayed only in the admin section). Pages in a private folder cannot be searched and are not displayed in the menu.

## Template tab

![](../../frontend/examples/template-bare/group-editor-temp.png)

- Template - template for the given folder, used to set the template when creating a new page in this folder.
  - Apply to all existing subfolders and subpages - if you check and save the folder, the selected template will be set to all subpages and subfolders of this folder.
  - Force folder template when displaying page - if this option is selected, the template set on the web page is ignored and the template set in the folder is forced to be used.
- Language - the language of the pages in this folder. The default option is Language is taken from template, in which the language setting is taken from the definition of the used page template.
  - The language affects the embedded applications - the texts that the application displays are controlled by the selected language.
  - WebJET will also automatically search for language mutations of assigned headers, footers and menus. If the template has a header named "default header" or "SK-default header", WebJET will automatically search for the "EN-default header" page when displaying a page with the EN language set.
- HTML code of the new page - determines what will be displayed in the editor after clicking the Add web page icon. It is either a blank page or a ready-made page from the /System/Templates folder.
- HTML code in subpage headers - optionally you can enter HTML code that will be directly inserted into the HTML code of the web page in this directory. For example, a specific META tag for search engines, or JavaScript code needed for this web page.

## Navigation tab

- Sort order - determines the order in which folders are displayed within the parent folder (in the tree structure).
- Regenerate the order of pages and folders in this directory (including subfolders) - if selected, it will adjust the order of pages and folders so that they automatically have an increasing sort order value of 10.
- Menu - information on how the directory should be displayed in the menu:
    - View - shows the main page and subfolders
    - Do not show - the folder and subfolders will not be displayed in the menu
    - No subfolders - only the main page of the folder is displayed in the menu
    - Show including web pages - both pages and folders will be displayed in the menu. For all pages in the folder, the Basic data -> Show in menu checkbox will be displayed.
- Navigation bar - how to display a folder in the navigation bar (navbar/breadcrumb):
  - Same as menu - the display method is identical to the display in the navigation menu.
  - Show - the folder will always be displayed.
  - Do not show - the folder will not be displayed.
- Sitemap - how to display a folder in the sitemap:
    - Same as menu - the display method is identical to the display in the navigation menu.
    - Show - the folder will always be displayed.
    - Do not show - the folder will not be displayed.
- Apply to all subfolders - the set value is also applied to existing subfolders.

The display method is divided into logged in and logged out visitors. You can hide individual menu items if the user is not logged in, or vice versa, for example, not display the Login item if the user is logged in.

## Access card

In the access tab, you set permissions for accessing pages in this folder. If you select a user group, the folder's contents will not be publicly available. They will only be available to logged-in visitors who belong to one of the selected user groups.

!>**Warning:** this is not about setting permissions for administrators, but about creating a so-called password-protected section on the website (for website visitors)

- Allow access only for user group - if you select a user group, a user from that group will be required to log in to view the page.
Assign page to mass email - the page will be available for mass email of users in the selected group. This allows you to categorize sent mass emails according to visitors' preferences.
- Login form page - if you have created a special login form page, set it here. The page is searched recursively to the root directory, so it does not need to be set for all folders, just for the root directory.
- Use basic settings - deletes the configured login page and uses the standard/basic login form.

## Publishing tab

In this tab, you can set the publishing of changes to save the folder in the future. If you set the date and time of the change and select the Schedule changes option and save the folder, the changes you made will not be reflected immediately, but only after the specified date and time. For example, you can publish a new section of the website at a certain time. The published version will have the Menu value set to Do not display, so it will not be displayed in the menu for now. In a timed version, for example, you set the value to Show for tomorrow morning. So the directory will start to appear in the menu tomorrow morning.

If there is a version scheduled for publication, you will see it in this tab and you can delete it or load its values ​​into the form.

## Optional Fields tab

Field A - Field D: [optional field](../../frontend/webpages/customfields/README.md) which can be used to pass values ​​to the HTML template.

## History tab

Displays the history of changes to the folder settings with the date and name of the editor who made the change. Click the pencil icon to load the change into the folder editor.

## Folder name and website synchronization

The configuration variable `syncGroupAndWebpageTitle` set to the value `true` ensures synchronization of the folder name and the **main** website name. This means that if the folder name changes, the main website name will automatically change to be the same and vice versa.

However, there are exceptions where this synchronization does not occur even if this configuration variable is enabled.

### Exception 1

The exception occurs for root folders, where the folder name and page name are not synchronized.

Example: We have a root folder SK and a page called Home in it, the folder name and the website name are not synchronized.

### Exception 2

This exception occurs if the page is set as the **main** page for **multiple folders**.

Example: We have a folder Language. This folder contains a subfolder Slovak. Both folders have the same page set as the main web page (its location is not important). In this case, an exception will occur.

### Exception 3

This exception occurs if the **main** page of a folder is not located directly in the given folder.

Example: We have a Language folder that contains the pages Slovak and English. We have a second folder News. The exception occurs if the main page of the News folder is the Slovak page, which is located in another folder.

