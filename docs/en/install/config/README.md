# Basic configuration

The most commonly used [configuration variables](../../admin/setup/configuration/README.md).

## Logging in

The library is used for logging [Logback](http://logback.qos.ch/), which allows detailed logging settings for individual java packages. It is used [ConsoleAppender](https://logback.qos.ch/manual/appenders.html#ConsoleAppender), but it is also possible to set [FileAppender](https://logback.qos.ch/manual/appenders.html#FileAppender) or [RollingFileAppender](https://logback.qos.ch/manual/appenders.html#RollingFileAppender). The configuration file is in `WEB-INF/classes/logback.xml`, but the following configuration variables can be used for basic setup:
- `logLevel` - basic logging level, can have a value of `debug` for detailed logging, or `normal` for production deployment.
- `logLevels` - list of java packages with logging level (each on a new line), e.g:

```txt
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

If necessary, it is possible to set/implement your own [Appender](https://logback.qos.ch/manual/appenders.html). It can send logs to an external log analysis system, for example. Directly `Logback` provides, among others `Appender` For [Syslog](https://logback.qos.ch/manual/appenders.html#SyslogAppender) or [SMTP](https://logback.qos.ch/manual/appenders.html#SMTPAppender). If necessary, it is possible to create [Custom Appender](https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender).

## Sending emails

To send emails you need to set up the SMTP server correctly:
- `smtpServer` - SMTP server address for sending emails.
- `smtpUseSSL` - by setting it to `true` activate the use of SSL.
- `smtpUseTLS` - `TLS` authentication - if port 587, `smtpUseTLS` must be `true`.
- `smtpTLSVersion` - version `TLS` for smtp connection.
- `smtpAuthMechanism` - authorization mechanism for the SMTP server (e.g. `NTLM XOAUTH2`), if empty, the default mechanism of the JavaMail library is used. Sets the value `mail.smtp.auth.mechanisms`. To enforce the use of `NTLM/v2` Before `BASIC` set the value to `NTLM`. For support `OAuth` set the value to `XOAUTH2`.
- `smtpUser` - login name.
- `smtpPassword` - password.
- `smtpPort` - port for connection to the SMTP server.
- `useSMTPServer` - disable email forwarding (e.g. for cluster nodes that do not have an SMTP server available).
- `smtpConnectionTimeoutMillis` - the number of milliseconds to wait for the SMTP connection to be established.
- `emailProtectionSenderEmail` - set the email address to be used as the sender's email for all emails if SMTP is not set `OPEN RELAY`. The typical value is `noreply@domena.sk`. When set up, the email address entered is set in the header of each email `FROM` and the originally set value of z `FROM` is set to `REPLY-TO`.

For bulk email you can still set up:
- `dmailWaitTimeout` - speed of sending emails from bulk email in milliseconds. By default it is set to 5000, which means that an email is sent once every 5 seconds. If you lower the value then the web server and SMTP server will be more loaded when sending emails. The value will only take effect after a server restart.
- `dmailBadEmailSmtpReplyStatuses` - A comma-separated list of expressions returned from the SMTP server for which the email will not attempt to be sent again.

You can set the sender's name and email for different system notifications:
- `defaultSenderName` - the name of the sender - for example, the name of the company
- `defaultSenderEmail` - email address of the sender - for example `no-reply@company-name.com`

the configuration value can be set specifically for modules using a prefix, such as `reservationDefaultSenderEmail`. If such a value exists, it shall be used in preference to the value `defaultSenderEmail`. The following prefixes can be used:
- `dmail` - the sender of the new bulk email.
- `formmail` - the sender of the notification to the visitor who filled in the form.
- `reservation` - the sender of the booking approval/denial.
- `passwordReset` - the sender of the email to change the user's password.

### Setting up Amazon SES

For bulk email, we recommend using [Amazon Simple Email Services/SES](https://aws.amazon.com/ses/) for better email delivery. Originally WebJET CMS used API access, which was activated by setting the conf. variable `useAmazonSES` to the value of `true`, but currently it is already in use [standard SMTP protocol](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html) in Amazon SES:
- Select [the address of the SMTP server](https://docs.aws.amazon.com/general/latest/gr/ses.html) for your region and set it in the conf. variable `smtpServer`, e.g. `email-smtp.eu-west-1.amazonaws.com`. The tables are scrollable on the page, only the US region is visible at first, don't be afraid to scroll the table.
- Create [login details](https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html) to the SMTP server and set them to conf. variables `smtpUser` a `smtpPassword`, select Encrypt for the password.
- On the page [Amazon SES](https://console.aws.amazon.com/ses/) in section `SMTP settings` the individual ports through which it communicates are also visible for the selected region, typically it is necessary to `firewall` enable communication on port 587.
- For a new project, after testing, ask for an increase in email sending limits, they are set low by default.
- Set conf. variable `smtpUseTLS` at `true`.
- V [Amazon SES](https://console.aws.amazon.com/ses/) in section `Identities` you need to verify the identity of the domain and set `DKIM` Keys.
- Delete conf. variable `useAmazonSES` if you have it set (for older projects where API access was originally used).
- Restart the application server.
- Try sending an email, verify in the email headers that it was actually sent via Amazon SES.

Sending via Amazon SES sets `DKIM` headers and ensure higher deliverability of emails.

## Cluster

In the case of a cluster installation, WebJET needs to know that it is running in a cluster and has to synchronize the internal memory cache.

Can run in fashion `auto` when it doesn't need a list of nodes, or in a mode where it has them listed exactly.

### Auto mode

The easiest is to run in auto mode, conf. variable `clusterNames` set to `auto` and restart the server. In this case, the node name/`nodu` the first found value of z is used:
- ENV variable `HOSTNAME` server
- call value `InetAddress.getLocalHost().getHostName()` - the domain name of the computer
- call value `InetAddress.getLocalHost().getHostAddress()` - IP address of the computer
- Value `"auto-"+Tools.getNow()`

The value is truncated to the first 16 characters. If the variable is `clusterHostnameTrimFromEnd` set to `true`, the trailing 16 characters are used (e.g. kubernetes creates `hostname` with a random value at the end).

### Exact list of nodes

If you have a stable configuration of running nodes/`nodes` set the conf. variable:
- `clusterNames=node1,node2,node3` - comma separated list of nodes from 1 to N

You need to externally define individual nodes `ID` node, you can't do it via Settings->Configuration, because all nodes would have the same name.

It is recommended to set the value via the parameter `-DwebjetNodeId=1`, or otherwise via [external configuration](../external-configuration.md).

### Other conf. variables

- `clusterRefreshTimeout` - node synchronization interval in ms, default `5000ms`. For production, a value of 60000 (1 minute) is usually sufficient to reduce the load.
- `clusterMyNodeType` - node type - `full` = administration and presentation part, `public`=only the presentation part without administration.
- `senderRunOnNode` - if set to a non-empty value contains a list of cluster nodes on which bulk email sending is triggered (e.g. node1 or node1,node2). It is recommended to trigger the send only on the administration node.

## Primary key generator

For some parts the primary key generator has historically been used, the following values can be set:
- `pkeyGenIncrement` - the value by which it increases.
- `pkeyGenOffset` - shift value for the cluster.
- `pkeyGenBlockSize` - the size of the block selection for the primary key generator. By default set to 10, for server with high load we recommend to set it to a higher value (100 - 1000).

To avoid conflicts in the cluster configuration, the value `pkeyGenOffset` for nodeshift. E.g. value `pkeyGenIncrement` is set to 5 and `offset` to 0-5 for individual nodes. In mode `auto` clustra is the automatically set value `pkeyGenBlockSize=1` to always read the last value from the database. This has a slight impact on server performance.

## Licences

Some libraries may need to purchase additional licenses for their use:
- `amchartLicense` - license number for the library [amCharts](https://www.amcharts.com) to display charts, after setting the license key, the amCharts logo will not be displayed in the chart.
