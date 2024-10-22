# Configuration

The most commonly used configuration variables.

## Logging in

- `logLevel` - basic logging level, can have a value of `debug` for detailed logging, or `normal` for production deployment.

- `logLevels` - list of java packages with logging level (each on a new line), e.g:
```
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

## Sending emails

To send emails you need to set up the SMTP server correctly:
- `smtpServer` - SMTP server address for sending emails.
- `smtpUseSSL` - by setting it to `true` activate the use of SSL.
- `smtpUseTLS` - `TLS` authentication - if port 587, `smtpUseTLS` must be `true`.
- `smtpTLSVersion` - version `TLS` for smtp connection.
- `smtpUser` - login name.
- `smtpPassword` - password.
- `smtpPort` - port for connection to the SMTP server.
- `useSMTPServer` - disable email forwarding (e.g. for cluster nodes that do not have an SMTP server available).
- `smtpConnectionTimeoutMillis` - the number of milliseconds to wait for the SMTP connection to be established.
- `emailProtectionSenderEmail` - set the email address to be used as the sender's email for all emails if SMTP is not set `OPEN RELAY`. The typical value is `noreply@domena.sk`. When set up, the email address entered is set in the header of each email `FROM` and the originally set value of z `FROM` is set to `REPLY-TO`.

For bulk email you can still set up:
- `dmailWaitTimeout` - speed of sending emails from bulk email in milliseconds. By default it is set to 5000, which means that an email is sent once every 5 seconds. If you lower the value then the web server and SMTP server will be more loaded when sending emails. The value will only take effect after a server restart.
- `dmailBadEmailSmtpReplyStatuses` - A comma-separated list of expressions returned from the SMTP server for which the email will not attempt to be sent again.

## Cluster

In the case of a cluster installation, WebJET needs to know that it is running in a cluster and has to synchronize the internal memory cache.

Can run in fashion `auto`when it doesn't need a list of nodes, or in a mode where it has them listed exactly.

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
