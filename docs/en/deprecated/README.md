# Deprecated/unsupported features

## User interface

**Website**

- When saving a page, EditorForm is no longer used, if you are using [WebjetEvent](../developer/backend/events.md) you need to modify it from using ```EditorForm``` to ```DocDetails``` object.
- The "Require secure connection (https)" attribute can no longer be set for a website, the recommended solution is to set up a redirect for the entire domain to a secure (https) connection. There is no point in setting this individually for websites as in the past.
- The right ```Uložiť (pracovná verzia)``` is changed to the right ```Uložiť```. If the user does not have this right, they cannot save existing web pages. The original use only for the working version did not make practical sense.

## Backend API

- Support of the ```Struts``` framework.
- The ```itext``` library is no longer part of the standard WebJET CMS distribution because it contains unpatched vulnerabilities and the new version is commercial. The option to export PDF in ```DisplayTag``` and export a form to PDF (```/formtopdf.do```) has been removed. If you need it in your project, you need to manually add the ```itext``` library with the risk of possible vulnerability.
- The ```commons-httpclient-3.1``` library is no longer part of the standard WebJET CMS distribution because it contains unpatched vulnerabilities. [Version 4.5](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html) is integrated, we recommend using the [Fluent API](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/fluent.html).

