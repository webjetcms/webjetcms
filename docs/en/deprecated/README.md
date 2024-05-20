# Deleted/unsupported properties

## User interface

**Website**

- Only directories are displayed in the tree structure, the option to set the display of web pages via conf. variable is not supported `webpagesTreeShowPages`.
- Page attributes (class `DocAtrDB`, tables `doc_atr`, `doc_atr_def`) is not supported, it is recommended to use [optional fields](../frontend/webpages/customfields/README.md).
- EditorForm is no longer used when saving a page if you use [WebjetEvent](../developer/backend/events.md) it needs to be adapted from the use of `EditorForm` at `DocDetails` object.
- The web page can no longer be set to "Require secure connection (https)", the recommended solution is to set the redirection of the entire domain to a secure (https) connection. There is no point in setting this individually for the site as in the past.
- Law `Uložiť (pracovná verzia)` is changed to the right `Uložiť`. If a user does not have this right, they cannot save existing web pages. The original use for the working version only did not make practical sense.

## Backend API

- Support `Struts` framework.
- Library `itext` is no longer part of the standard WebJET CMS distribution because it contains unpatched vulnerabilities and the new version is commercial, The ability to export PDFs in `DisplayTag` and export the form to PDF (`/formtopdf.do``itext`
- Library `commons-httpclient-3.1`[](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html)[](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/fluent.html).
