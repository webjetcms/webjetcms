# Spam protection

WebJET includes integrated SPAM protection, which limits the speed and number of requests to prevent server overload. This protection is also referred to as `Rate Limiting`, or request limit.

The protection is tied to the visitor's IP address, so the limits apply to each IP address separately. Therefore, it is necessary to have the visitor's IP address acquisition set correctly. For example, if `Load Balancer/Proxy` is prefixed to the application server, it is necessary to set the configuration variable `serverBeyoundProxy` to the value `true` and to `Load Balancer/Proxy` set the HTTP headers `x-forwarded-for` with the visitor's IP address and `x-forwarded-proto` with the value of the protocol used `http/https`. You can verify the correctness of the IP address in [audit](../../sysadmin/audit/README.md) in the IP address column when performing an action, such as logging in or submitting a form.

!> **Warning:** if you are using a cluster, throttling happens separately on each application server/cluster node - data is not shared between nodes.

## Basic settings

The following configuration variables are used for the basic setting of limiting the number of requests:

- `spamProtectionHourlyLimit` - ​​maximum number of requests from one IP address per hour, set to 20 by default.
- `spamProtectionTimeout` - ​​minimum number of seconds between two requests from one IP address, set to 30 seconds by default.
- `spamProtectionIgnoreFirstRequests` - ​​number of first requests from an IP address that are not limited, set to 0 by default, i.e. without any limitation exception.

### Settings for the module/application

Limits can be adjusted specifically for some modules/applications by adding `-appName` to the end of the configuration variable, e.g. `spamProtectionHourlyLimit-appName` or `spamProtectionTimeout-appName`. Values ​​can be set for the following applications:

- `dmail` - ​​[bulk email](../../redactor/apps/dmail/form/README.md) - limits the number of registrations/unregistrations from bulk email.
- `form` - ​​[forms](../../redactor/apps/form/README.md) and sending feedback from the administration homepage.
- `forum` - ​​[discussion forum](../../redactor/apps/forum/README.md), limits the number of posts added.
- `HtmlToPdfAction` - ​​generating PDF documents.
- `inquiry` - ​​[poll](../../redactor/apps/inquiry/README.md), limits the number of votes in the poll.
- `passwordSend` - ​​[sending a forgotten password](../../redactor/admin/password-recovery/README.md), limits the number of times a forgotten password can be sent.
- `qa` - ​​[questions and answers](../../redactor/apps/qa/README.md), limits adding a new question.
- `quiz` - ​​[questionnaires](../../redactor/apps/quiz/README.md), limits the number of questionnaires sent.
- `search` - ​​[search](../../redactor/apps/search/README.md), limits the number of searches.
- `ThumbServlet` - ​​[thumbnails](../../frontend/thumb-servlet/README.md), limits the number of times a new thumbnail image is generated, the images are saved to disk and used for subsequent requests.
- `userform` - ​​[new user registration](../../redactor/password-protected-zone/README.md), limits the number of new registrations and user profile edits in the password-protected zone.

The following values ​​are already adjusted by default:

- `spamProtectionHourlyLimit-ThumbServlet` - ​​number of requests to generate [image thumbnails](../../frontend/thumb-servlet/README.md), `/thumb` set to 300.
- `spamProtectionTimeout-ThumbServlet` - ​​set to the value `-2`, which means that the spacing limitation between requests does not apply for `/thumb`, this is because there can be multiple such images in the page at once and HTTP requests are made in parallel.
- `spamProtectionHourlyLimit-search` - ​​number of search requests via the search application, set to 200.
- `spamProtectionTimeout-search` - ​​value reduced to 10 to make it faster to perform another search/go to the next page of results.

## Exceptions

The protection can be partially disabled by setting the following variables:

- `spamProtectionDisabledIPs` - ​​list of IP address beginnings separated by a comma (or `*` character for all) for which spam protection is disabled.

The status can be reset in the [Data deletion - Cache objects] application (../../sysadmin/data-deleting/README.md), where the Delete all cache objects action also resets the list of call counts and intervals for all IP addresses.