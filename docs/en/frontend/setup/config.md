# General settings

Below you will find a list of general settings that can be used to change the behavior of the primary website. Individual configuration variables are set in the [Configuration](../../admin/setup/configuration/README.md).

It is recommended to set the values at the beginning of creating a website and not to change them afterwards, as they can affect the behaviour of already created websites.

## URL addresses

The behavior of URLs of web pages can be set by the following configuration variables:
- `virtualPathLastSlash` Default `true` - sets the option of the last `/` for URLs **main page**. When set to `true` the URL of the Products page will be created as `/products/`, when set to `false` Like `/products`.
- `editorPageExtension` Default `.html` - sets the suffix for **other pages in the folder**. When set to `.html` will be the URL address of the page `iPhone` created as `/products/iphone.html`, when set to `/` will be created as `/products/iphone/`.

After changing the values, you must restart the application server.
