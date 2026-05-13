# Basic configuration

Most commonly used [configuration variables](../../admin/setup/configuration/README.md).

## Logging

The [Logback](http://logback.qos.ch/) library is used for logging, which allows detailed logging settings for individual java packages. [ConsoleAppender](https://logback.qos.ch/manual/appenders.html#ConsoleAppender) is used, but it is also possible to set [FileAppender](https://logback.qos.ch/manual/appenders.html#FileAppender) or [RollingFileAppender](https://logback.qos.ch/manual/appenders.html#RollingFileAppender). The configuration file is in `WEB-INF/classes/logback.xml`, but for basic settings the following configuration variables can be used:

- `logLevel` - ​​basic logging level, can have the value `debug` for detailed logging, or `normal` for production deployment.
- `logLevels` - ​​list of java packages with logging level (each on a new line), e.g.:

```txt
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

If necessary, it is possible to set up/implement your own [Appender](https://logback.qos.ch/manual/appenders.html). This can send logs, for example, to an external system for log analysis. Directly `Logback` provides `Appender` among others for [Syslog](https://logback.qos.ch/manual/appenders.html#SyslogAppender) or [SMTP](https://logback.qos.ch/manual/appenders.html#SMTPAppender). If necessary, it is possible to create your own [Appender](https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender).

## Sending emails

To send emails, you need to set up the SMTP server correctly:

- `smtpServer` - ​​SMTP server address for sending emails.
- `smtpUseSSL` - ​​setting to `true` will enable the use of SSL.
- `smtpUseTLS` - ​​`TLS` authentication - if port is 587, `smtpUseTLS` must be `true`.
- `smtpTLSVersion` - ​​version `TLS` for smtp connection.
- `smtpAuthMechanism` - ​​authorization mechanism for the SMTP server (e.g. `NTLM XOAUTH2`), if empty, the default mechanism of the JavaMail library will be used. Sets the value to `mail.smtp.auth.mechanisms`. To force the use of `NTLM/v2` before `BASIC`, set the value to `NTLM`. To support `OAuth`, set the value to `XOAUTH2`.
- `smtpUser` - ​​login name.
- `smtpPassword` - ​​password.
- `smtpPort` - ​​port for connecting to the SMTP server.
- `useSMTPServer` - ​​disable sending emails (e.g. for cluster nodes that do not have an available SMTP server).
- `smtpConnectionTimeoutMillis` - ​​number of milliseconds to wait for an SMTP connection to be established.
- `emailProtectionSenderEmail` - ​​set the email address that will be used as the sender email for all emails if SMTP does not have `OPEN RELAY` set. The typical value is `noreply@domena.sk`. When set, the entered email address is set to the header of each email in `FROM` and the originally set value from `FROM` is set to `REPLY-TO`.

For mass email, you can also set:

- `dmailWaitTimeout` - ​​speed of sending emails from bulk email in milliseconds. By default set to 5000, which means that the email is sent once every 5 seconds. If you reduce the value, the web server and SMTP server will be more loaded when sending emails. The value will only take effect after the server is restarted.
- `dmailBadEmailSmtpReplyStatuses` - ​​a comma-separated list of terms returned from the SMTP server for which the email will not be retried.

For various system notifications, it is possible to set the sender's name and email:

- `defaultSenderName` - ​​sender name - for example, company name
- `defaultSenderEmail` - ​​sender's email address - for example `no-reply@company-name.com`

The configuration value can be set specifically for modules using a prefix, such as `reservationDefaultSenderEmail`. If such a value exists, it will be used in preference to the value `defaultSenderEmail`. The following prefixes can be used:

- `dmail` - ​​sender of the new mass email.
- `formmail` - ​​notification sender for the visitor who filled out the form.
- `reservation` - ​​sender of reservation approval/rejection.
- `passwordReset` - ​​sender of the email for changing the user's password.

### Amazon SES setup

For bulk email, we recommend using [Amazon Simple Email Services/SES](https://aws.amazon.com/ses/) for better email delivery. Originally, WebJET CMS used API access, which was activated by setting the config variable `useAmazonSES` to the value `true`, but now the [standard SMTP protocol](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html) in Amazon SES is used:

- Select the [SMTP server address](https://docs.aws.amazon.com/general/latest/gr/ses.html) for your region and set it to the config variable `smtpServer`, e.g. `email-smtp.eu-west-1.amazonaws.com`. The tables on the page are scrollable, only the US region is visible at first, don't be afraid to scroll the table.
- Create [credentials](https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html) for the SMTP server and set them to the config variables `smtpUser` and `smtpPassword`, select Encrypt for the password.
- On the [Amazon SES](https://console.aws.amazon.com/ses/) page, in the `SMTP settings` section, you can also see the individual ports through which it communicates for the selected region. Typically, it is necessary to enable communication on port 587 at `firewall`.
- For a new project, after testing, request an increase in email sending limits, they are set low by default.
- Set config variable `smtpUseTLS` to `true`.
- In [Amazon SES](https://console.aws.amazon.com/ses/) in the `Identities` section, you need to verify the domain identity and set the `DKIM` keys.
- Delete the config variable `useAmazonSES` if you have it set (for older projects where API access was originally used).
- Restart the application server.
- Try sending an email, verify in the email headers that it was actually sent via Amazon SES.

Sending via Amazon SES sets `DKIM` headers and ensures higher email deliverability.

## Cluster

In the case of a cluster installation, WebJET needs to know that it is running in a cluster and synchronize its internal caches.

It can run in `auto` mode, when it does not need a list of nodes, or in a mode where it has them listed exactly.

### Car mode

The easiest way is to run in auto mode, set the config variable `clusterNames` to the value `auto` and restart the server. In this case, the first value found from: will be used as the node name/`nodu`

- ENV variable `HOSTNAME` server
- call value `InetAddress.getLocalHost().getHostName()` - ​​computer domain name
- call value `InetAddress.getLocalHost().getHostAddress()` - ​​computer IP address
- value `"auto-"+Tools.getNow()`

The value is truncated to the first 16 characters. If the `clusterHostnameTrimFromEnd` variable is set to `true`, the last 16 characters are used (e.g. Kubernetes creates `hostname` with a random value at the end).

The configuration variable `clusterAutoRandomDelay` sets the maximum random delay time in milliseconds added in `clusterNames=auto` mode when starting `ClusterRefresher`, `Sender` and before starting `CRON` tasks. It helps to distribute the start of tasks across cluster nodes and reduce the risk of database `deadlock`. The value `0` disables the delay completely.

### Exact list of nodes

If you have a stable configuration of running nodes/`nodes` set the conf. variable:

- `clusterNames=node1,node2,node3` - ​​comma-separated list of nodes from 1 to N

You need to define the `ID` node externally for each node, you cannot do this via Settings->Configuration because all nodes would have the same name.

We recommend setting the value via the `-DwebjetNodeId=1` parameter, or in another way via [external configuration](../external-configuration.md).

### Other conf. variables

- `clusterRefreshTimeout` - ​​node synchronization interval in ms, default `5000ms`. For production, a value of 60000 (1 minute) is usually sufficient to reduce load.
- `clusterMyNodeType` - ​​node type - `full` = administration and presentation part, `public`=only presentation part without administration.
- `senderRunOnNode` - ​​if set to a non-empty value, it contains a list of cluster nodes on which mass email sending is triggered (e.g. node1 or node1,node2). We recommend triggering sending only on the administration node.

## Primary key generator

For some parts, a primary key generator has historically been used, the following values ​​can be set:

- `pkeyGenIncrement` - ​​the value by which it is increased.
- `pkeyGenOffset` - ​​offset value for the cluster.
- `pkeyGenBlockSize` - ​​block selection size for the primary key generator. Default value is 10, for a server with high load we recommend setting it to a higher value (100 - 1000).

To avoid conflicts in the cluster configuration, the value `pkeyGenOffset` is used for the offset by nodes. For example, the value `pkeyGenIncrement` is set to 5 and `offset` to 0-5 for individual nodes.

In `auto` cluster mode, it is not possible to use `pkeyGenOffset`, therefore the specified block is allocated in the transaction and if `deadlock` occurs, the attempt is repeated a maximum of 5 times with an exponentially increasing delay from 50 ms (`50 * 2^attempt`) and with a random component (jitter).

## Licenses

For some libraries, you may need to purchase additional licenses to use them:

- `amchartLicense` - ​​license number for the [amCharts](https://www.amcharts.com) library for displaying charts, after setting the license key, the amCharts logo will not be displayed in the chart.