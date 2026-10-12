# Main controls

The administration layout is standard. The header is at the top, the menu is on the left. The third level of the menu is displayed as navigation tabs in the header.

![](welcome.png)

## Header

At the top is the header:

![](header.png)

contains the following options:

- **Help** menu with help for the current page, documentation, news and contacts.
- **Search…** button to open [quick search](search/README.md). You can also open the dialog with the shortcut **Ctrl+K** on Windows or **⌘K** on Mac.
- Name of the currently logged in user, click on the name to display the following options:
  - Profile - edit your own profile (name, email... - after changing your profile, you need to log out and log in again).
  - Two-step verification - the ability to activate two-step verification using the ```Google Authenticate``` application when logging into the administration. This increases the security of your account, because in addition to the password, you also need to enter a code from your mobile device to log in. We recommend setting it for all accounts through which user accounts and rights can be managed. If you use authentication against the `ActiveDirectory/SSO` server, you can disable the menu item by setting the conf. variable `2factorAuthEnabled` to the value `false`.
  - Manage encryption keys - allows you to create a new encryption key for encrypting forms and enter an existing key for decrypting them. Requires the Forms right.
  - Logout - log out of the administration.
- ![](icon-logoff.png ":no-zoom") Icon for logging out of the administration.

Tabs to navigate to the third menu level can be displayed in the header.

## Helper

The **Help** menu is available in the header on all administration pages. It offers:

- **Help for this page** – instructions for the open part of the administration. Below the link is the name of the current page. If the page does not have instructions associated with it, the item will not be displayed.
- **WebJET CMS Documentation** – the home page of the documentation in the language of the logged-in user.
- **Keyboard shortcuts** – [overview of shortcuts](keyboard-shortcuts.md) and where they work.
- **What's new in version…** – a specific version in the documentation. The number of new features and the **New** label correspond to the tab on the home screen. The label remains for 30 days from the first time the given content is displayed in the menu or on the home screen; editing the content starts a new period.
- **List of all changes** – complete changelog.
- **Send Feedback** – a dialog for an idea, problem or compliment. It also works outside the home screen. The back arrow will take you back to the menu; closing with the cross will keep the written message while this page is open.
- **Contact Support** – support page set for the user's language.

Links to documentation and support open in a new tab. At the bottom of the menu is the edition, version, and server of WebJET CMS. Use the copy icon to copy the information for support.

The menu can be controlled with the keyboard: **Enter** or the spacebar opens it, the arrows scroll through the items, and **Esc** closes it and returns focus to the **Help** button.

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