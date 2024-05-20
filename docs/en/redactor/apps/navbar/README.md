# Navigation bar

The navigation bar (navbar / breadcrumb / breadcrumb navigation) displays a clickable path in the web page to the currently displayed web page. The directory names can be clicked on to easily get to the next level down. Example:

![](navbar.png)

The display of an item in the navigation bar depends on the setting of the Navigation Bar field in the Navigation tab of the Web Site Directory. It has the following options:
- Just like the menu - the display in the navigation bar behaves the same way as the display field is set in the menu.
- Show - the item will be displayed in the navigation bar.
- Don't show - the item will not appear in the navigation bar (including sub-folders).

For the display option, you can set the display option for the web page that is still displayed (typically, this is a scrolled item in the navigation bar). This is also in the Navigation tab and contains the options:
- View - the web page will be displayed in the navigation bar.
- Do not show - the web page will not be displayed in the navigation bar.

![](groups-dialog.png)

## Use

The navigation bar is inserted directly into the JSP template as a tag:

```html
<iwcm:write name="navbar" />
```

or it can be inserted directly into the web page as an expression:

```html
!REQUEST(navbar)!
```

![](editor-dialog.png)
