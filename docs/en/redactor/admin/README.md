# Main controls

The administration layout is standard. The header is at the top, the menu is on the left. The third level of the menu is displayed as navigation tabs in the header.

![](welcome.png)

## Header

At the top is the header:

![](header.png)

contains the following options:

- The **Help** menu with help for the current page, documentation, release news and contacts.
- **Search…** button to open [quick search](search/README.md). You can also open the dialog with the shortcut **Ctrl+K** on Windows or **⌘K** on Mac.
- Name of the currently logged in user, click on the name to display the following options:
  - Profile - edit your own profile (name, email... - after changing your profile, you need to log out and log in again).
  - Two-step verification - the ability to activate two-step verification using the ```Google Authenticate``` application when logging into the administration. This increases the security of your account, because in addition to the password, you also need to enter a code from your mobile device to log in. We recommend setting it for all accounts through which user accounts and rights can be managed. If you use authentication against the `ActiveDirectory/SSO` server, you can disable the menu item by setting the conf. variable `2factorAuthEnabled` to the value `false`.
  - Manage encryption keys - allows you to create a new encryption key for encrypting forms and enter an existing key for decrypting them. Requires the Forms right.
  - Logout - log out of the administration.
- ![](icon-logoff.png ":no-zoom") Icon for logging out of the administration.

Tabs to navigate to the third menu level can be displayed in the header.

## Help

The **Help** menu is available in the header throughout the administration. It offers:

- **Help for this page** – instructions for the current administration page, with its name below the link. This item is hidden when the page has no assigned help article.
- **WebJET CMS documentation** – the documentation home page in the signed-in user's language.
- **Keyboard shortcuts** – an [overview of shortcuts](keyboard-shortcuts.md) and where they work.
- **What’s new in version…** – the specific release in the documentation. The highlight count and **New** badge match the home-page card. The badge lasts 30 days from the first time this content appears in the menu or on the home page; changes to the content start a new period.
- **All changes** – the complete changelog.
- **Send feedback** – a dialog for an idea, problem or praise, available from any administration page. The back arrow returns to the menu; closing with the cross retains the draft while this page remains open.
- **Contact support** – the support page configured for the user's language.

Documentation and support links open in a new tab. The menu footer shows the WebJET CMS edition, version and server. Use the copy icon to copy this information for support.

Use **Enter** or Space to open the menu, arrow keys to move between items, and **Esc** to close it and return focus to the **Help** button.

## Menu

On the left side under the WebJET logo are icons representing the main menu sections. We decided to represent the first level menu like this so that we don't have to have deeply nested menu items:

![](menu-main-sections.png)

Clicking on the main section icon will display the menu items of the selected section:

![](menu-items.png)

The selection of the domain you are working with (for a multi-domain installation) is located at the bottom of the left menu.

![](domain-selector.png)

## Viewing on mobile devices

The administration is adapted to mobile devices. If the window width is less than 1200 points, the page header and left menu will be hidden:

![](welcome-tablet.png)

To display the header and menu, click the hamburger menu icon ![](icon-hamburger.png ":no-zoom") in the top left. The menu and header will then appear above the page. The navigation tabs will appear as a drop-down menu in the header.

![](welcome-tablet-showmenu.png)

To close the menu, click the close menu icon ![](icon-hamburger-show.png ":no-zoom").

The editor in the data table will be displayed at the full window size when the window width is less than 992 points (tablet view):

![](editor-tablet.png)

For a window narrower than 576 points, the field names will also be moved from the left side above the field for better viewing, e.g. on a mobile phone:

![](editor-phone.png)