# Migration between environments

Basic information that needs to be done when migrating between environments, such as production and development environments.

## Configuration variables

Check the following config variables:

- `cloudStaticFilesDir` - ​​path to the directory with static files, it needs to be set correctly for the given environment (or deleted from the environment if external static files are not used).
- `enableStaticFilesExternalDir` - ​​enable/disable use of external directory for static files.
- `smtpServer` - ​​SMTP server address + variables `smtpUsername, smtpPassword, smtpPort, smtpUseTLS` and so on.
- `emailProtectionSenderEmail` - ​​set the email address that will be used as the sender's email, it can be different for each environment, it must be enabled on the SMTP server.
- `proxyHost/proxyPort` - ​​proxy settings
- `webEnabledIPs` - ​​list of IP addresses from which the website is accessible, set by default to `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,62.168.118.90,62.168.118.67,#klient,` in the DEV environment
- `adminEnabledIPs` - ​​list of IP addresses from which administration can be accessed, set to `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,` by default
- `multidomainAdminHost` - ​​if set to verify domain, used to set the domain for the CMS on a multi-domain installation.
- `serverBeyoundProxy` - ​​setting the location of the application server behind the proxy/load balancer.
- `logLevel` - ​​leave it at `normal` on production, you can change it to `debug` on development environment.

Review all other config variables and consider their impact on the project and the appropriateness of the settings.

## Domain addresses

If the website uses `multidomain`, you need to set up domains correctly. When migrating between environments, check all root directories and set up the correct domain names.
Also check the Change domain redirects option, but if the domain is set up in multiple root folders (e.g. in sk and en), check it only the first time you change the domain.

## Users (when migrating to DEV environment)

To prevent you from accidentally sending, for example, a request for website approval from a development environment, I recommend changing the email addresses of existing accounts to appropriate development accounts.

Verify the settings of the user `admin`, set him/her permissions to change all directories (e.g. by setting the approval mode to none), permissions to modules, and set an appropriate password.

## Background tasks

Check background tasks, especially the set task URLs. On the DEV environment, delete unnecessary tasks if necessary, on the PROD environment in the case of a cluster, check the settings of the node on which the task is to be executed.

## Data deletion (when migrating to DEV environment)

To prevent the local database on the DEV environment from becoming unnecessarily large, we recommend deleting the data:

- keep statistics only for the last 3 months
- delete emails completely (ideally via `DELETE * FROM emails`)
- delete server monitoring

## Other

Consider the specific features of the given project and its settings and apply them. Think about whether it does not contain specific settings, for example in the directory structure (free fields) and the like.

## Logs

Check the application server logs and fix any errors during startup (e.g. due to bad autoupdate.xml, etc.).