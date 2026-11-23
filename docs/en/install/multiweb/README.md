# MultiWeb

## Introduction

MultiWeb is a special type of multi-user/multi-tenant installation in a single WebJET. The domains are externally separate and each appears as a separate WebJET CMS installation.

The installation includes a management domain (the first one created) through which elevated permissions are available (e.g. for configuration, translation keys, etc.). Applications that impact all domains are not available in individual tenant domains.

## Restrictions

MultiWeb installations have the following limitations:

- Website
  - Media groups - only media groups with set folder rights in the current domain are displayed and editable, newly added media groups are automatically granted rights to the root folders in the domain. In the control domain, it is possible to manage global media groups without the specified Show for restriction.
- Applications
  - Domain redirects - only domain redirects pointing to the current domain can be managed.
- Templates
  - The list displays templates that have access to the folders of the currently displayed domain. When creating a new template that does not have any access set, access to the root folders of the current domain is automatically set.
  - Template groups - a user with limited folder access will see a group if at least one of the following conditions is met:
    - the template group id is used in some template available to the user,
    - or in a MultiWeb installation outside the control domain, there is a non-empty alias of the current domain and at least one of the following conditions applies:
      - `directory` contains a folder matching the alias,
      - `keyPrefix` exactly matches the alias,
      - `name` contains an alias in square brackets in the format `[alias]`,
      - `name` starts with `alias` + space.
  - The domain alias is obtained from the configuration variable `multiDomainAlias:DOMAIN-NAME`, where `DOMAIN-NAME` is the domain name. When [creating a new domain](config.md#creating-a-new-domain) via MultiWeb, the variable is automatically set to the last part of the domain directory name shortened to the first dot, for example, for `www.example.sk` to the value `example`. For an existing domain, the alias can be set manually in the configuration.
  - All template groups are displayed in the management domain; they are also visible to the user without folder access restrictions.
- Users
  - Each domain has its own list of users.
  - For a user who does not have any restrictions set on the tree structure of website folders, a list of domain root folders is set internally during rights checking.

## Applications available only in the management domain

The following applications are only available in the management domain:

- Introduction
  - Audit
  - Server monitoring
- Applications
  - GDPR - Regular expressions
  - GDPR - Data Deletion
  - Bulk email - Domain limits
- Users
  - User groups
  - Rights groups
- Control panel
  - Configuration
  - Translation keys
  - HTTP headers
  - Data deletion
  - Automated tasks
  - WebJET CMS update
  - Restart

If you need to add some rights to a user in the domain, you can add the config variable `multiwebSpecialPerms-USERID` with a comma-separated list of rights that the user should additionally obtain.
