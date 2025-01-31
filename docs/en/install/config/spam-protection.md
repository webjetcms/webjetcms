# Spam protection

WebJET includes integrated SPAM protection, which limits the rate of requests and their number to avoid overloading the server. Such protection is also referred to as `Rate Limiting`, i.e. limiting the number of requests.

The protection is tied to the visitor's IP address, so the limits apply to each IP address separately. It is therefore necessary to have the visitor's IP address retrieval set up correctly. For example, if the application server is predefined before the `Load Balancer/Proxy` it is necessary to set the configuration variable `serverBeyoundProxy` to the value of `true` and on `Load Balancer/Proxy` set HTTP headers `x-forwarded-for` with the IP address of the visitor and `x-forwarded-proto` with the value of the protocol used `http/https`. You can verify the correctness of the IP address in [audit](../../sysadmin/audit/README.md) in the IP address column when you perform an action, such as logging in or submitting a form.

!> **Warning:** if you are using a cluster, the limiting happens separately on each application server/node of the cluster - data is not shared between nodes.

## Basic settings

The following configuration variables are used to set the basic limiting of the number of requests:
- `spamProtectionHourlyLimit` - The maximum number of requests from one IP address per hour, set to 20 by default.
- `spamProtectionTimeout` - The minimum number of seconds between two requests from the same IP address, set to 30 seconds by default.
- `spamProtectionIgnoreFirstRequests` - The number of first requests from an IP address that are not capped, set to 0 by default, i.e. no capping exception.

### Settings for module/application

Limits can be modified specifically for certain modules/applications by adding `-appName` to the end of the configuration variable, e.g. `spamProtectionHourlyLimit-appName` or `spamProtectionTimeout-appName`. The values can be set for the following applications:
- `dmail` - [bulk email](../../redactor/apps/dmail/form/README.md) - limits the number of registrations/unsubscriptions from the bulk email.
- `form` - [forms](../../redactor/apps/form/README.md) and sending feedback from the administration homepage.
- `forum` - [discussion forum](../../redactor/apps/forum/README.md), limits the number of posts added.
- `HtmlToPdfAction` - generation of PDF documents.
- `inquiry` - [survey](../../redactor/apps/inquiry/README.md), limits the number of votes in the poll.
- `passwordSend` - [sending a forgotten password](../../redactor/admin/password-recovery/README.md), limits the number of times a forgotten password can be sent.
- `qa` - [questions and answers](../../redactor/apps/qa/README.md), limits the addition of new questions.
- `quiz` - [questionnaires](../../redactor/apps/quiz/README.md), limits the number of questionnaires sent.
- `search` - [Search](../../redactor/apps/search/README.md), limits the number of searches.
- `ThumbServlet` - [preview images](../../frontend/thumb-servlet/README.md), limits the number of times a new preview image is generated, the image is saved to disk and used for further requests.
- `userform` - [new user registration](../../redactor/zaheslovana-zona/README.md), limits the number of new registrations and modifications to a user's profile in the password-protected zone.

By default, the following values are already adjusted:
- `spamProtectionHourlyLimit-ThumbServlet` - number of requests for generation [image thumbnails](../../frontend/thumb-servlet/README.md), `/thumb` set to 300.
- `spamProtectionTimeout-ThumbServlet` - set to `-2`, which means that limiting the spacing between the requirements for `/thumb` is not applied, this is because there can be several such images in the page at the same time and HTTP requests are executed in parallel.
- `spamProtectionHourlyLimit-search` - The number of search requests through the search application, set to 200.
- `spamProtectionTimeout-search` - value reduced to 10 to allow faster search/advance to the next page of results.

## Exceptions

The protection can be partially disabled by setting the following variables:
- `spamProtectionDisabledIPs` - a comma-separated list of IP address beginnings (or a character `*` for all) for which spam protection is disabled.

Reset status is possible in the application [Deleting data - Cache objects](../../sysadmin/data-deleting/README.md), where the list of the number of calls and intervals for all IP addresses is reset using the action Delete all cache objects.
