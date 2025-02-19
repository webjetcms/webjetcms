# WebJET CMS 2024

Welcome to the documentation for WebJET CMS version 2024. We recommend to read [list of changes](CHANGELOG-2024.md) a [roadmap](ROADMAP.md).

# List of changes in the latest version

## 2025.0

> In the version **2025.0** we brought **new administration design** for even better clarity and user experience.
>
> One of the main changes is the transfer of **second level menu** to **tabs in the page header**, which simplifies navigation. In the website we also **merged folder and website tabs** to keep everything in one place. If the header does not contain tabs, the tables are automatically adjusted and displayed **extra line**.
>
> Please provide feedback via **Feedback form** if you identify when using the new version **any display problem**. You can also add a reminder **screenshot** to help us identify and resolve any deficiencies more quickly.
>
> Thank you for your cooperation and help in improving WebJET CMS!

## Groundbreaking changes

- Web pages - inline editing cancelled. The ability to edit the page directly in view mode has been removed as it used an older version of the editor that is no longer supported. As an alternative, the toolbar displayed in the top right corner of the web page can be activated. This toolbar allows quick access to the web page editor, folder or template. You can turn it off or on using the configuration variable `disableWebJETToolbar`. Once activated, it will start to appear on the web page after entering the Web Pages section in the administration (#57629).
- Login - set for administrators [password change request](sysadmin/pentests/README.md#password-rules) once a year. The value can be modified in the configuration variable `passwordAdminExpiryDays`, setting it to 0 disables the check (#57629).
- Introduction - added requirement to activate two-factor authentication to increase login security. Prompt is not displayed if authentication is handled via `LDAP` or if the translation key is `overview.2fa.warning` set to empty (#57629).

### Design

In the version **2025.0** we brought an improved **administration design** which is clearer and more efficient.

- **Modified login dialogue** - new background and moving the login dialog to the right side. At **Login** it is possible to use not only the login name but **already have an email address**. ![](redactor/admin/logon.png)
- **Clearer header** - the name of the current page or section is now displayed directly in the header.
- **New navigation in the left menu** - under items are no longer part of the left menu, but are displayed **as cards at the top** Pages. ![](redactor/admin/welcome.png)
- **Merged tabs in the Websites section** - Switching folder types and web page types are now displayed in a common section, simplifying navigation. **Choosing a domain** has been moved to the bottom of the left menu. ![](redactor/webpages/domain-select.png)
- **Reorganised menu items**:
  - **SEO** moved to section **Views**.
  - **GDPR and Scripts** moved to section **Templates**.
  - **Gallery** is now in the section **Files**.
  - Some item names have been modified to better describe their function.

The rest of the list of changes to the changes is identical to the version [2024.52](CHANGELOG-2024.md).

## Web pages

- Added the ability to set increment order for folders in a configuration variable `sortPriorityIncrementGroup` and web pages in the configuration variable `sortPriorityIncrementDoc`. The default values are 10 (#57667-0).

### Testing

- Standard password for `e2e` tests are obtained from `ENV` variable `CODECEPT_DEFAULT_PASSWORD` (#57629).

### Error correction

- Web pages - inserting links to a file in PageBuilder (#57649).
- Web pages - added link information (file type, size) to the Auxiliary caption attribute `alt` (#57649).
- Web pages - corrected order of web pages when used `Drag&Drop` in tree structure (#57657-1).
- Web pages - when duplicating a web page/folder, the value is set `-1` in the Order of Arrangement field for inclusion at the end of the list. The value `-1` can also be entered manually to obtain a new value for the order of the arrangement (#57657-1).
- Websites - importing web pages - fixed media group settings when importing pages containing media. When importing, all Media Groups (even unused ones) are automatically created due to the fact that the media group set for the media application is also translated when importing pages `/components/media/media.jsp` in the page (which may also contain the media ID of a group outside the imported pages) (#57657-1).
- Firefox - reduced version of the set `Tabler Icons` at `3.0.1` because Firefox puts a significant load on the processor when using newer versions. Optimised CSS style reading `vendor-inline.style.css` (#56393-19).

![meme](_media/meme/2025-0.jpg ":no-zoom")
