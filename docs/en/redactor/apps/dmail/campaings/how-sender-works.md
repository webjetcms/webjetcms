# How sending emails works

Sending email campaigns done in the background so called. `Sender`. It works as follows:
- From the database (table `emails`), 50 emails will be selected for sending.
- A random email is selected from the selected emails.
- When selecting a random email, domain limits are checked, if the selected email cannot be sent, another one is searched for.
- If the selected sample of 50 emails contains all emails with the same domain (e.g. `gmail.com` or your company domain), you may find that due to domain limits **can't select any suitable emails from a sample of 50**.
- For the selected email, the send attempt counter is incremented and the send date is set to the current date and time (even if the email is not yet sent, this reduces the likelihood of duplicate email sending in the case of a clustered installation).
- If the number of sends exceeds the maximum number of attempts the email is marked as incorrect (value `retry` in the table will have the value `-1`).
- A web page with the text and design of the email is downloaded over the HTTP protocol.
- If the recipient is from the database of registered users in the WebJET CMS the user is logged in when downloading the web page. In this way it is possible to use [tags for inserting user data](../campaings/README.md#Basic).
- If the recipient is not from the database of registered users, only [basic marks](../campaings/README.md#Basic).
- If there is an application in the body of the email, it can generate text in the body `SENDER: DO NOT SEND THIS EMAIL`, in which case the email will not be sent and will be marked as successfully sent. This can be used if the application checks the user's settings - e.g. they are only interested in receiving an offer for a 3-bedroom apartment, but you don't currently have one on offer.
- The body of the email will be accompanied by images and attachments.
- The body of the email will be updated with a tag for tracking clicks on the link in the email.
- An image will be attached to the body of the email to track the opening of the email.
- The email will be sent.
- From the SMTP server response it is evaluated whether the email is sent successfully (in addition to the status code it is possible to set additional error responses in the conf. variable `dmailBadEmailSmtpReplyStatuses`)
- If the sending is unsuccessful, the date of sending the email is deleted in the database and the sending is stopped for the time set in the conf. variable `dmailSleepTimeAfterException`.

## Correct setting

To send a bulk email correctly, it is necessary to have a properly configured email server:
- Settings [DKIM](https://www.dkim.org) domain keys with valid [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) of record. We recommend to use for sending [Amazon SES](../../../../install/config/README.md#amazon-ses-settings) a `DKIM` set there, it will also automatically set `SPF`.
- Settings [DMARC](https://dmarc.org) record. In DNS, create a new `TXT` record for the domain `_dmarc.vasadomena.sk` with a value of at least `v=DMARC1; p=none; sp=none`.

## Why it takes a long time to send

If you feel that the upload is taking too long, these are possible reasons:
- Sending works as a background task, initialized when the server starts. It sends e-mail periodically every 1000ms (set in conf. variable `dmailWaitTimeout`). This value is the wait between send executions, that is, after the email is sent, it will start sending again in the set number of ms. If you set the value to 333 it doesn't mean that 3 emails will be sent per second (that would make the whole sending process take 0ms, which is certainly not reality).
- Sending emails is **blocking** if during the interval set via the conf. variable `dmailWaitTimeout` misses to send the email, the interval will be skipped and it will be sent in the next interval.
- Domain limits - for better delivery are checked [limit on the number and speed of email delivery to a specific domain](../domain-limits/README.md). If the campaign contains many emails of the same domain (e.g. gmail.com or your business domain) there will be a delay in sending. If your corporate domain does not limit the number of emails add it to the domain limits and set a high number of emails per time slot.
- Database performance - as mentioned above, when sending, a random sample of emails is selected from the database to send, from which an email is selected. If the database table `emails` contains a lot of records, this selection can take a long time, which slows down the sending. You can in Settings->Data Delete->E-mails to delete old information about sent emails, which will reduce the load on the database.
- Server speed - as mentioned above, each email is downloaded as a web page over a local HTTP connection. If the server is underperforming, this download will also experience delays. You can follow the Overview section of the Server Monitoring application to verify the performance at the time of sending the campaign. Ideally, the email is downloaded locally directly from the application server without going through the whole infrastructure (firewall, load balancer...). You can use the conf. variable `natUrlTranslate` through which you can set the address translation (e.g. `https://www.domena.sk/|http://localhost:8080/`).

The following configuration variables affect the upload speed:
- `dmailWaitTimeout` - the email send trigger interval in milliseconds. After the change it is necessary to restart the server (default 1000).
- `dmailMaxRetryCount` - the maximum number of times the email can be sent if an error occurs during sending (default 5).
- `dmailSleepTimeAfterException` - Wait interval in ms after a send error, e.g. if the SMTP server stops responding (default 20000),
- `dmailBadEmailSmtpReplyStatuses` - list of comma-separated expressions returned from the SMTP server for which the email will not try to send again (default: Invalid Addresses,Recipient address rejected,Bad recipient address,Local address contains control or whitespace,Domain ends with dot in string,Domain contains illegal character in string).
- `dmailDisableInlineImages` - allows you to disable the attachment of images to the email, which will increase the speed of sending and reduce the size of the email. The downside is that the recipient has to confirm that the images have been retrieved from the server. If you have a multi-domain installation you can set exceptions for attaching images to email via a conf. variable `dmailWhitelistImageDomains` (for set domains the images are attached).

Other conf. variables that can be set:
- `useSMTPServer` - enables/disables sending of all emails from the server (by default `true`).
- `disableDMailSender` - disables only sending bulk emails (by default `false`).
- `senderRunOnNode` - if you are using a cluster of multiple application servers allows you to set a comma-separated list of names `nodu` from which bulk emails will be sent. Note: if emails are sent from multiple `nodov` Duplicate emails may be sent.
- `dmailTrackopenGif` - a virtual path to the image that indicates the email has been opened (by default `/components/dmail/trackopen.gif`).
- `dmailStatParam` - name of the URL parameter for click statistics (default `webjetDmsp`).
- `replaceExternalLinks` - if set to `true` external links will also be replaced by redirecting them through the server where the bulk email for tracking statistics is running (by default `false`).

## Settings for acceleration

If you need to speed up your upload, you can do the following:
- Increase domain limits, we recommend setting higher limits on domains `gmail.com` and your company domain.
- Edit by `dmailWaitTimeout` to the value of `500`, which will increase the speed of the call to send the email, due to blocking (see above). This does not mean that an email will be sent every 500ms.
- If the database contains a lot of invalid emails reduce `dmailSleepTimeAfterException`. **Warning:** &#x69;f your SMTP server actually goes down, the emails will be marked as sent very quickly because the number of `dmailMaxRetryCount`.
- Set `natUrlTranslate` for direct downloading of the email text from the local application server. If you have a multi-domain installation there may be a problem selecting the correct domain. We recommend in `hosts` set all domains to IP address 127.0.0.1 in the server file, in which case you only set port forwarding from 80 to 8080 (or whatever port you are running the local application server on).
- Minimize images and attachments. These increase the load on the server and the volume of the email. Alternatively, set a conf. variable `dmailDisableInlineImages` at `false` to disable attaching images directly to the body of the email.
- If you have a cluster you can enable sending from multiple nodes in parallel, but this increases the risk of multiple duplicate emails being sent to the recipient. The list of nodes from which the email is sent is set in the conf. variable `senderRunOnNode`.
