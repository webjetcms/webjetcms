# General settings

Below you will find a list of general settings that can be used to change the behavior of primary web pages. Individual configuration variables are set in the [Configuration] section (../../admin/setup/configuration/README.md).

We recommend setting the values ​​at the beginning of creating a website and not changing them afterwards, as they may affect the behavior of already created websites.

## URL addresses

The behavior of website URLs can be set with the following configuration variables:

- `virtualPathLastSlash` defaults to `true` - sets the last `/` option for **main page** URLs. When set to `true`, the Products page URL will be created as `/products/`, when set to `false`, it will be created as `/products`.
- `editorPageExtension` defaults to `.html` - sets the extension for **other pages in the folder**. When set to `.html`, the URL of the page `iPhone` will be created as `/products/iphone.html`, when set to `/` it will be created as `/products/iphone/`.

After changing the values, it is necessary to restart the application server.