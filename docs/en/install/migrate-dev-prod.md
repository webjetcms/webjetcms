# Migration between environments

Basic information that needs to be done when migrating between environments, e.g. production and development environments.

## Configuration variables

Check the following conf. variables:
- `cloudStaticFilesDir` - the path to the static files directory, it needs to be set correctly on the environment (or deleted on the environment if external static files are not used).
- `enableStaticFilesExternalDir` - enabling/disabling the use of an external directory for static files.
- `smtpServer` - SMTP server address + variables `smtpUsername, smtpPassword, smtpPort, smtpUseTLS` and the like.
- `emailProtectionSenderEmail` - set the email address to be used as the sender's email, it can be different for each environment, it must be enabled on the SMTP server.
- `proxyHost/proxyPort` - proxy settings
- `webEnabledIPs` - the list of IP addresses from which the web is accessible, on the DEV environment set by default to `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,62.168.118.90,62.168.118.67,#klient,`
- `adminEnabledIPs` - the list of IP addresses from which it is possible to access the administration, by default set to `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,`
- `multidomainAdminHost` - if set to verify domain, it is used to set the domain for the CMS on a multi-domain installation.
- `serverBeyoundProxy` - Set the location of the application server behind the proxy/load balancer.
- `logLevel` - on production to leave on `normal`, on the development environment you can change to `debug`.

Check all other conf. variables and consider their impact on the project and the appropriateness of the setting.

## Domain addresses

If the site uses `multidomain` you need to set up the domains correctly. When migrating between environments, check all root directories and set the correct domain names. Also check the option Change domain redirects, but if the domain is set in multiple root directories (e.g. both en and en) check this only when changing the domain for the first time.

## Users (when migrating to a DEV environment)

To avoid accidentally sending a request for approval of a website from the developer environment, I recommend changing the email addresses of existing accounts to appropriate developer accounts.

Verify user settings `admin`, set its permissions on all directories (e.g. by setting the approval mode to none), permissions on modules, and set an appropriate password.

## Background tasks

Check the background tasks, especially the set task URLs. On the DEV environment, delete unnecessary tasks if necessary, on the PROD environment in case of a cluster, check the settings of the node on which the task is to be executed.

## Data deletion (when migrating to DEV environment)

To prevent the local database on the DEV environment from becoming unnecessarily large, we recommend that you perform a data deletion:
- keep statistics only for the last 3 months
- delete emails completely (ideally via `DELETE * FROM emails`)
- delete server monitoring

## Other

Consider the specific features of the project and its settings and apply them. Consider whether it contains any more specific settings, e.g. in the directory structure (free fields) and so on.

## Logos

Check the application server logs and fix any errors during startup (e.g. due to bad autoupdate.xml, etc.).
