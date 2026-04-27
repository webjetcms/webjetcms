# How sending emails works

Sending campaign emails is done in the background by the so-called ```Sender```. It works as follows:

- 50 emails are selected from the database (table ```emails```) to be sent.
- A random email will be selected from the selected emails.
- When selecting a random email, domain limits are checked; if the selected email cannot be sent, another one is searched for.
- If the selected sample of 50 emails contains all with the same domain (e.g. ```gmail.com``` or your company domain), a situation may arise where, due to domain limits, **no suitable one can be selected from the sample of 50 emails**.
- For the selected email, the send attempt counter will be incremented and the send date will be set to the current date and time (even if the email has not yet been sent, this reduces the likelihood of duplicate email sending in the case of a cluster installation).
- If the number of sendings exceeds the maximum number of attempts, the email will be marked as incorrect (the value ```retry``` in the table will have the value ```-1```).
- A web page with the text and design of the email is downloaded via the HTTP protocol.
- If the recipient is from the database of registered users in WebJET CMS, the given user will log in when downloading the web page. This allows the use of [tags for inserting user data](../campaings/README.md#basic) in the page.
- If the recipient is not from the database of registered users, only [basic tags](../campaings/README.md#basic) will be replaced.
- If the body of the email contains the text ```SENDER: DO NOT SEND THIS EMAIL```, the application can generate the text in the body, in which case the email will not be sent and will be marked as successfully sent. This can be used if the application checks the user's settings - e.g. they are only interested in receiving an offer for a 3-room apartment, which you currently do not have on offer.
- Images and attachments are added to the body of the email.
- The body of the email will be supplemented with a tag for tracking clicks on the link in the email.
- An image will be attached to the body of the email to track the opening of the email.
- The email will be sent.
- The SMTP server response determines whether the email was sent successfully (in addition to the status code, additional error responses can be set in the conf. variable ```dmailBadEmailSmtpReplyStatuses```)
- If sending is unsuccessful, the date of sending the email will be deleted from the database and sending will stop for the time set in the conf. variable ```dmailSleepTimeAfterException```.

## Correct setting

To send a mass email correctly, you need to have a correctly configured email server:

- Set up [DKIM](https://www.dkim.org) domain keys with a valid [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) record. We recommend using [Amazon SES](../../../../install/config/README.md#nastavenie-amazon-ses) for sending and set `DKIM` there, `SPF` will also be set automatically.
- [DMARC](https://dmarc.org) record set. Create a new `TXT` record in DNS for domain `_dmarc.vasadomena.sk` with a value of at least `v=DMARC1; p=none; sp=none`.

## Why is it taking so long to send?

If you feel like your upload is taking too long, here are some possible reasons:

- Sending works as a background task, it is initialized when the server starts. The email is sent regularly every 1000ms (set in the conf. variable ```dmailWaitTimeout```). This value is the wait between sending executions, so after sending the email, sending will be started again in the set number of ms. If you set the value 333, it does not mean that 3 emails will be sent per second (the entire sending process would have to take 0ms, which is certainly not reality).
- Sending emails is **blocking**, if the email is not sent during the interval set via the conf. variable ```dmailWaitTimeout```, the interval will be skipped and it will be sent in the next interval.
- Domain limits - for better delivery, [limit the number and speed of email delivery to a specific domain](../domain-limits/README.md) is checked. If the campaign contains many emails from the same domain (e.g. gmail.com or your company domain), there will be a delay in sending. If your company domain does not limit the number of emails, add it to the domain limits and set a high number of emails per time period.
- Database performance - as mentioned above, when sending, a random sample of emails to send is selected from the database, from which the email is selected. If the database table ```emails``` contains many records, this selection may take a long time, which slows down sending. You can delete old information about sent emails in the Settings->Data deletion->Emails application, which will reduce the load on the database.
- Server speed - as mentioned above, each email is downloaded as a web page via a local HTTP connection. If the server has insufficient performance, this download will also be delayed. You can monitor the Server Monitoring application in the Overview section to verify performance at the time of sending the campaign. It is ideal when the email is downloaded locally directly from the application server without going through the entire infrastructure (firewall, load balancer...). You can use the conf. variable ```natUrlTranslate``` through which you can set address translation (e.g. ```https://www.domena.sk/|http://localhost:8080/```).

The following configuration variables affect the upload speed:

- ```dmailWaitTimeout``` - ​​email sending trigger interval in milliseconds. After changing it, the server must be restarted (default 1000).
- ```dmailMaxRetryCount``` - ​​maximum number of email resends if an error occurs during sending (default 5).
- ```dmailSleepTimeAfterException``` - ​​waiting interval in ms after a sending error, e.g. if the SMTP server stops responding (default 20000),
- ```dmailBadEmailSmtpReplyStatuses``` - ​​a list of comma-separated expressions returned from the SMTP server for which the email will not be retried (default: Invalid Addresses, Recipient address rejected, Bad recipient address, Local address contains control or whitespace, Domain ends with dot in string, Domain contains illegal character in string).
- ```dmailDisableInlineImages``` - ​​allows you to disable attaching images to emails, which will increase the sending speed and reduce the size of the email. The disadvantage is that the recipient must confirm loading images from the server. If you have a multi-domain installation, you can set exceptions for attaching images to emails via the conf. variable ```dmailWhitelistImageDomains``` (images will be attached for the configured domains).

Other config variables that can be set:

- ```useSMTPServer``` - ​​enables/completely disables sending of all emails from the server (default ```true```).
- ```disableDMailSender``` - ​​only disables sending bulk emails (default ```false```).
- ```senderRunOnNode``` - ​​if you are using a cluster of multiple application servers, it allows you to set a comma-separated list of names ```nodu``` from which bulk emails will be sent. Warning: if emails are sent from multiple ```nodov```, duplicate email sending may occur.
- ```dmailTrackopenGif``` - ​​virtual path to the image that indicates email opens (by default ```/components/dmail/trackopen.gif```).
- ```dmailStatParam``` - ​​URL parameter name for click statistics (by default ```webjetDmsp```).
- ```replaceExternalLinks``` - ​​if set to ```true```, external links will also be replaced by redirecting through the server where the mass email is running for tracking statistics (by default ```false```).

## Acceleration settings

If you need to speed up sending, you can do the following:

- Increase domain limits, we recommend setting higher limits on ```gmail.com``` domains and your company domain.
- Change ```dmailWaitTimeout``` to ```500```, which will increase the speed of the email send call, due to blocking (see above). However, this does not mean that the email will be sent every 500ms.
- If the database contains many invalid emails, reduce ```dmailSleepTimeAfterException```. **Warning:**, if your SMTP server actually goes down, emails will be marked as sent very quickly because the ```dmailMaxRetryCount``` count will be exceeded.
- Set ```natUrlTranslate``` to directly download the email text from the local application server. If you have a multi-domain installation, there may be a problem with selecting the correct domain. We recommend setting all domains to the IP address 127.0.0.1 in the ```hosts``` file on the server, in which case you only set up port forwarding from 80 to 8080 (or whatever port your local application server is running on).
- Minimize images and attachments. They increase the load on the server and the volume of the email. Alternatively, set the config variable ```dmailDisableInlineImages``` to ```false``` to disable attaching images directly to the email body.
- If you have a cluster, you can enable sending from multiple nodes in parallel, but this increases the risk of sending multiple duplicate emails to the recipient. The list of nodes from which the email is sent is set in the config variable ```senderRunOnNode```.