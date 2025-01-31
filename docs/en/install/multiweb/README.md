# MultiWeb

## Home

MultiWeb is a special type of multi-user/multi-tenant installation in a single WebJET. The domains are externally separate and each pretends to be a separate WebJET CMS installation.

The installation includes a control domain (the first one established), through which elevated permissions are available (e.g. for configuration, translation keys, etc.). Applications that impact all domains are not available in individual tenant domains.

## Restrictions

MultiWeb installations contain the following restrictions:
- Web pages
  - Media groups - only media groups with folder rights set to folders in the current domain are displayed and editable; newly added media groups are automatically given rights to the root folders in the domain. You can manage global media groups in the control domain without specifying the View For restriction.
- Applications
  - Domain redirects - only domain redirects pointing to the current domain can be managed.
- Templates
  - The list displays templates that have access set to the folders of the currently displayed domain, when a new template is created that would have no access set, access is automatically set to the root folders of the current domain.
  - Template Groups - displays the template groups used in the templates.
- Users
  - Each domain has its own list of users.
  - For a user who has no restrictions set on the tree structure of Web site folders, a list of domain root folders is set internally when checking permissions.

## Applications available only in the management domain

The following applications are only available in the management domain:
- Home
  - Audit
  - Server monitoring
- Applications
  - GDPR - Regular expressions
  - GDPR - Data erasure
  - Bulk Email - Domain Limits
- Users
  - User groups
  - Groups of rights
- Control panel
  - Configuration
  - Translation keys
  - HTTP headers
  - Data deletion
  - Automated tasks
  - WebJET CMS update
  - Restart

If you need to add some rights to a user in the domain, you can add a conf. variable `multiwebSpecialPerms-USERID` A comma-separated list of additional rights that the user is to receive.
