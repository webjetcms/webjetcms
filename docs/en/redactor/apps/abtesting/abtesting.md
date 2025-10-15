# AB Testing

The AB Testing application provides you with an overview of the AB pages and the management of the relevant configuration variables:
- List of AB pages
- Configuration

## List of AB pages

The AB Site List section provides a list of sites that have a B variant created. These pages cannot be edited, added or deleted in any way.

![](ab_test_page_list.png)

You have the option to view the page and also view the Page Stats using the buttons on the table toolbar. The important thing in the statistics is to measure the results by percentage conversion according to the A/B version ratio:

![](stat-percent.png)

## Configuration

The Configuration section allows you to review and edit the relevant configuration variables for AB testing.

![](ab_test_config_page.png)

The actual modification of these configuration variables is limited to modifying the value of the variable and the encryption, no other changes or actions over the variables are allowed.

![](ab_test_config_editor.png)

### Possible configuration variables

- `ABTesting` (default `false`) - after setting to `true` AB website testing is activated.
- `ABTestingRatio` (default `50:50`) - the ratio of page generation between version A and B.
- `ABTestingName` (default `abtestvariant`) - the name that is added to the URL of the B version of the page to distinguish it - it is also used to trace the B version of the page.
- `ABTestingCookieName` (default `wjabtesting`) - name `cookie` used to remember the version of the test when `split` tests - if the display decides for option B, it is remembered in `cookie` with that name and the B versions of the pages will be provided accordingly.
- `ABTestingCookieDays` (default `30`) - number of days to remember the selected version in AB testing - cookie expiration time.
- `ABTestingAllowVariantUrl` (default `false`) - by setting to `true` enables direct display of the variant URL to non-administrators, e.g. calling `/investicie/abtestvariantb.html`.
- `ABTestingForLoggedUser` (default `false`) - by setting to `true` mode is started, where a visitor who is not logged in is always shown the A variant of the web page and a logged in visitor is always shown the B variant of the web page.
