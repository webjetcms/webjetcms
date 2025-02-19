# Main controls

The administration layout is standard. There is a header at the top and a menu on the left. The third level of the menu is shown as navigation tabs in the header.

![](welcome.png)

## Header

The header is located at the top:

![](header.png)

contains the following options:
- Link to open the help.
- ![](icon-search.png ":no-zoom") An icon to open the page [Search](search/README.md)
- The name of the currently logged in user, click on the name to display the following options:
  - Profile - edit your own profile (name, email... - after changing the profile you need to log out and log back in).
  - Two-Step Authentication - the ability to activate two-step authentication using the app `Google Authenticate` when logging into the administration. This increases the security of your account, because in addition to the password, you also need to enter a code from your mobile device to log in. We recommend setting this on all accounts through which user accounts and rights can be managed. If you use authentication against `ActiveDirectory/SSO` server, you can disable the menu item by setting the conf. variable `2factorAuthEnabled` to the value of `false`.
  - Encryption Key Management - allows you to create a new encryption key for encrypting forms and specify an existing key for decrypting them. Requires the Forms right.
  - Logout - logout from the administration.
- ![](icon-logoff.png ":no-zoom") An icon to log out of the administration.

The header can display tabs to move to the third level of the menu.

## Menu

In the left part under the WebJET logo there are icons representing the main menu sections. We chose to represent the first level of the menu this way so that we don't have to have deeply nested menu items:

![](menu-main-sections.png)

Click on the main section icon to display the menu items of the selected section:

![](menu-items.png)

Select the domain you are working with (for a multi-domain installation) at the bottom of the left menu.

![](domain-selector.png)

## Display on mobile devices

Administration adapts to mobile devices. When the window width is less than 1200 pixels, the page header and the left menu are hidden:

![](welcome-tablet.png)

header and menu can be viewed by clicking on the hamburger menu icon ![](icon-hamburger.png ":no-zoom") top left. Then the menu and header will appear above the page. The navigation tabs appear as selection menus in the header.

![](welcome-tablet-showmenu.png)

to close the menu, click on the close menu icon ![](icon-hamburger-show.png ":no-zoom").

The editor in the datatable when the window width is less than 992 pixels (tablet view) will be displayed at the full window size:

![](editor-tablet.png)

For a window narrower than 576 pixels, the field names will also be moved from the left side above the field for better display, e.g. on a mobile phone:

![](editor-phone.png)
