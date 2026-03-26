# OAuth2 Authentication in WebJET CMS

WebJET CMS supports OAuth2 authentication for user login via external providers such as Google, Facebook, GitHub, Keycloak and others. Configuration is read when WebJET CMS is initialized, after changing values or initial setup you need to restart the application server.

The pairing of users is done on the basis of an email, which must be unique. The first time a user logs in via OAuth2, a new user is created in the WebJET database with a set login, first name, last name and authorization. User groups and privilege groups are synchronized based on name matching. Administrators are automatically set based on the group membership defined in the configuration variable `NTLMAdminGroupName`.

## Configuration

### Basic configuration

To activate OAuth2, you need to set a configuration variable:

```properties
oauth2_clients=google,keycloak
```

If you need to view different providers in the administration or customer area, you can set `oauth2_clientsIncludeAdmin` a `oauth2_clientsIncludeUser`:

```properties
oauth2_clientsIncludeAdmin=keycloak
oauth2_clientsIncludeUser=google,facebook
```

in this case, only the `keycloak` and for the customer zone `google` a `facebook`. The specified providers must be configured in `oauth2_clients` to appear in the login page. By default, the value is set to `*`, which means that every configured provider will be displayed.

### Predefined providers

For popular providers just set `clientId` a `clientSecret`. Using the value `oauth2_{provider}DefaultGroups` You can define user groups that are assigned to a newly created user when he or she first logs on through the provider. The value is a comma-separated list of group IDs (e.g. `1,3`). These groups are not applied to existing users, so after the first login you can set the users rights as you need directly in the WebJET CMS administration, and these rights will be preserved for future logins. If you use multiple providers, you can define different groups for each provider to be able to distinguish users for providers.

#### Google

```properties
oauth2_googleClientId=your-google-client-id
oauth2_googleClientSecret=your-google-client-secret
oauth2_googleDefaultGroups=1,2
```

You can get the required values via [Google API Console](https://console.developers.google.com/) in the Credentials section. When registering an OAuth2 client with Google, you must set the redirect URI in the format `https://your-domain.com/login/oauth2/code/google`.

#### Facebook

```properties
oauth2_facebookClientId=your-facebook-client-id
oauth2_facebookClientSecret=your-facebook-client-secret
oauth2_facebookDefaultGroups=1,3
```

You can get the required values via [Facebook for Developers](https://developers.facebook.com/docs/facebook-login) in My Apps. Create an app, set the login actions. When registering the OAuth2 client with Facebook, you must set the redirect URI in the format `https://your-domain.com/login/oauth2/code/facebook`. In the section `App settings` See `App ID` a `App Secret` that you use to configure WebJET CMS.

#### GitHub

```properties
oauth2_githubClientId=your-github-client-id
oauth2_githubClientSecret=your-github-client-secret
oauth2_githubDefaultGroups=1,4
```

#### Okta

```properties
oauth2_oktaClientId=your-okta-client-id
oauth2_oktaClientSecret=your-okta-client-secret
oauth2_oktaDefaultGroups=1,5
```

### Other OAuth2 provider

For own providers (e.g. `Keycloak`) you need to set all `OAuth2` Parameters. The name of the configuration variable is formed dynamically from the name of the provider (for example `keycloak`):

```properties
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=your-client-secret
oauth2_keycloakAuthorizationUri=https://keycloak.local/realms/your-realm/protocol/openid-connect/auth
oauth2_keycloakTokenUri=https://keycloak.local/realms/your-realm/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=https://keycloak.local/realms/your-realm/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=https://keycloak.local/realms/your-realm/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=https://keycloak.local/realms/your-realm
oauth2_keycloakUserNameAttributeName=email
oauth2_keycloakScopes=openid,profile,email
oauth2_keycloakClientName=Keycloak
```

If you used a provider called `myprovider`, you would have to set the variables with the prefix `oauth2_myprovider...`. You can also have multiple OAuth2 providers set up in the system at the same time (e.g. `keycloak-admins`, `keycloak-users`), each with its own configuration.

### Supported OAuth2 attributes

WebJET CMS extracts the following attributes from the OAuth2/OIDC provider during OAuth2 authentication:

#### Basic user attributes

- `email` - **Required** - User email (must be unique)
- `given_name` - First name of the user
- `family_name` - Last name of the user
- `preferred_username` - **Default** attribute for login (standard OIDC attribute, configurable via `oauth2_usernameAttribute`)
- `picture` - URL of the profile picture (if provided)

#### Attributes for groups and rights

To synchronize groups (only for configured providers in `oauth2_clientsWithPermissions`):
- `groups` - A simple list of groups
- `roles` - A simple list of roles
- `group_membership` - Alternative name for groups
- `resource_access.<client>.roles` - Keycloak client-specific roles
- `realm_access.roles` - Keycloak realm roles
- Spring Security authorities - Automatically extracted from authentication

**Login name extraction rules:**

1. **Preferred attribute**: Value of the configuration variable `oauth2_usernameAttribute` (default `preferred_username`) - if provided by the OAuth2 server, it is used for login

2. **Fallback**: The part of the email before the call sign - if the attribute is not available

**Examples:**

- Keycloak provides `preferred_username: "john.doe"` - login will be `john.doe`
- Google does not provide `preferred_username` - login will be the part before the @ from the email (e.g. `john.doe` z `john.doe@gmail.com`)
- If set `oauth2_usernameAttribute=sub`, the attribute is used `sub` instead of `preferred_username`

If there is a login name conflict when creating a new user (e.g. `john.doe` already exists), the email address will be used as the login name to avoid an error and to allow logging in. If such an account also exists, a new account will be created `oauth2.google.XXXXXX` Where `XXXXXX` is a random alphanumeric string.

### Attribute configuration for login (username)

The name of the OAuth2 attribute used for the user login is configurable:

```properties
oauth2_usernameAttribute=preferred_username
```

If the variable is not set, the default value is used `preferred_username` (standard OIDC attribute). The value of this variable determines from which OAuth2 token attribute the login is retrieved. If the attribute does not exist in the token, the part of the email before the callback is used as the fallback.

### Redirect URI

The redirect URI is automatically set for all providers:

```text
{baseUrl}/login/oauth2/code/{registrationId}
```

Example: `https://your-domain.com/login/oauth2/code/google`

## Synchronising users

### Create a new user

When a user logs in for the first time via OAuth2:

1. **Data extraction** - Email, first name, last name and username are extracted from OAuth2 attributes

2. **Creating a user** - Creates a new user in the WebJET database

3. **Setting a login name** - The login name is preferably set from the attribute specified by the configuration variable `oauth2_usernameAttribute` (default `preferred_username`), if it is not available, the part of the email before the curly letter is used

4. **Authorization** - The user is marked as authorised

Creating a user takes place in `AbstractOAuth2SuccessHandler.createNewUserFromOAuth2()`:

```java
UserDetails userDetails = new UserDetails();
userDetails.setEmail(email);
userDetails.setFirstName(givenName);
userDetails.setLastName(familyName);

// Nastav prihlasovacie meno - prednostne z username atribútu, inak použij email pred zavináčom
String usernameAttr = getUsernameAttribute(); // oauth2_usernameAttribute alebo default "preferred_username"
String username = oauth2User.getAttribute(usernameAttr);
String login;
if (username != null && !username.trim().isEmpty()) {
    login = username;
} else {
    login = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
}
userDetails.setLogin(login);
userDetails.setAuthorized(true);
boolean isUserSaved = UsersDB.saveUser(userDetails);
```

**Supported attributes for login:**

- Configurable via `oauth2_usernameAttribute` (default `preferred_username` - standard OIDC attribute)
- Fallback: part of the email before the envelope

### Updating an existing user

For existing users, they are updated:
- First and last name (if changed in the OAuth2 provider)
- Login (if the OAuth2 provider provides the attribute defined in `oauth2_usernameAttribute` / default `preferred_username` and that has changed)
- Group assignments (only for configured providers in `oauth2_clientsWithPermissions`)

The update is taking place in `AbstractOAuth2SuccessHandler.updateExistingUserFromOAuth2()`.

## Synchronising groups

OAuth2 Success Handlers contain specialized logic for synchronizing groups from configured OAuth2 providers.

### Differences between Admin and User zone

**Admin Zone** (`OAuth2AdminSuccessHandler`):
- Synchronises **user groups** (user groups)
- Synchronises **permission groups** (groups of rights)
- Sets **admin flag** based on `NTLMAdminGroupName`
- Requires admin rights to log in successfully

**User zone** (`OAuth2UserSuccessHandler`):
- Synchronises only **user groups** (user groups)
- **Not synchronising** permission groups
- **Does not set** admin flag
- Does not require admin rights

### Configure rights synchronization

In order to synchronize groups and rights from the OAuth2 provider, a configuration variable needs to be set:

```properties
oauth2_clientsWithPermissions=keycloak,okta
```

This variable defines a comma-separated list of providers for which to perform group and rights synchronization. For other providers (e.g. Google, Facebook) synchronization is not performed.

The control takes place in `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`.

### Provider detection

Provider ID is obtained in `AbstractOAuth2SuccessHandler.getProviderId()`:

```java
protected String getProviderId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken) {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        return oauth2Token.getAuthorizedClientRegistrationId();
    }
    return null;
}
```

Checking whether to synchronize rights is done in `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`:

```java
protected boolean shouldSyncPermissions(String providerId) {
    if (providerId == null) return false;
    String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
    if (Tools.isEmpty(configuredProviders)) return false;
    List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
    return providers.contains(providerId);
}
```

### Extraction of groups

The extraction of the groups takes place in `AbstractOAuth2SuccessHandler.extractGroupsFromOAuth2()` and supports various formats:

1. **Basic attributes**: `groups`, `roles`, `group_membership`

2. **Keycloak realm\_access**: `realm_access.roles`

3. **Keycloak resource\_access**: `resource_access.{client}.roles`

4. **Spring Security authorities**: Automatically extracted roles (with prefix removed `ROLE_`)

```java
// Príklad štruktúry Keycloak tokenov
{
  "realm_access": {
    "roles": ["admin", "user"]
  },
  "resource_access": {
    "webjetcms-client": {
      "roles": ["webjet-admin", "content-editor"]
    }
  }
}
```

Auxiliary methods for extraction:
- `extractFromAttribute()` - extracts from simple attributes
- `extractFromResourceAccess()` - extracted from Keycloak `resource_access`
- `extractFromRealmAccess()` - extracted from Keycloak `realm_access`
- `extractRolesFromClientResource()` - extracts roles from a specific client
- `extractRolesFromRolesObject()` - auxiliary method for extraction from object roles

### Mapping groups

The mapping takes place in the method `applyOAuth2Permissions()` (admin) or `applyOAuth2UserGroups()` (user):

1. **User Groups** - User groups with a name matching the OAuth2 group are searched for

2. **Permission Groups** - Looking for privilege groups with a name matching the OAuth2 group (admin zone only)

3. **Full synchronisation** - All old groups are removed and new ones are added

**Admin Zone** (`OAuth2AdminSuccessHandler.synchronizeUserGroups()`):

```java
// Odstránenie zo všetkých existujúcich skupín
removeAllGroupAssignments(userDetails); // odstráni user groups + permission groups

// Pridanie do nových user groups
for (UserGroupDetails group : newUserGroups) {
    userDetails.addToGroup(group.getUserGroupId());
}
UsersDB.saveUser(userDetails);

// Pridanie do permission groups
for (PermissionGroupBean group : newPermissionGroups) {
    UsersDB.addUserToPermissionGroup(userDetails.getUserId(), group.getUserPermGroupId());
}
```

**User zone** (`OAuth2UserSuccessHandler.synchronizeUserGroups()`):

```java
// Odstránenie zo všetkých existujúcich skupín (iba user groups)
removeAllGroupAssignments(userDetails);

// Pridanie do nových user groups
for (UserGroupDetails group : newUserGroups) {
    userDetails.addToGroup(group.getUserGroupId());
}
UsersDB.saveUser(userDetails);
// Permission groups sa nesynchronizujú
```

## Admin rights

### Automatically set admin rights (admin zone only)

For configured OAuth2 providers (defined in `oauth2_clientsWithPermissions`), admin rights are set automatically in **admin area** based on the configuration variable:

```properties
NTLMAdminGroupName=admin
```

If OAuth2 groups contain a group named z `NTLMAdminGroupName`, the user is marked as admin.

The setup takes place in `OAuth2AdminSuccessHandler.applyOAuth2Permissions()`:

```java
String adminGroupName = Constants.getString("NTLMAdminGroupName");
boolean isAdmin = false;
if (adminGroupName != null && !adminGroupName.isEmpty() && oauth2Groups.contains(adminGroupName)) {
    isAdmin = true;
}
if (userDetails.isAdmin() != isAdmin) {
    userDetails.setAdmin(isAdmin);
    UsersDB.saveUser(userDetails);
}
```

**Important**: Admin rights are checked after synchronizing groups. If the user does not have admin rights, login to the admin zone is rejected with an error `accessDenied`.

**User zone**: `OAuth2UserSuccessHandler` does not se&#x74;**&#x20;admin rights - this is only for the admin zone.**&#x53;ecurity aspects

## Limitations of OAuth2 processes

### Configurable synchronisation

1. **&#x20;- Synchronization of groups and admin rights only works for OAuth2 providers configured in&#x20;** &#x45;mail validation`oauth2_clientsWithPermissions`

2. **&#x20;- Each user must have a valid email attribute** The uniqueness of email

3. **&#x20;- Email must be unique in the system** Error handling

### OAuth2 integration includes two types of error handling:

1\ . Authentication errors

**&#x20;(**):`OAuth2DynamicErrorHandler`Handles errors that occur during OAuth2 authentication (e.g. invalid token, server error):

Mapping errors to codes:

```java
public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                    AuthenticationException exception) throws IOException {
    // Určí či ide o admin alebo user prihlásenie
    boolean isAdminLogin = OAuth2LoginHelper.isAdminLogin(request);

    // Získa kód chyby z výnimky
    String errorCode = getErrorCodeFromException(exception);

    Logger.error(OAuth2DynamicErrorHandler.class, "OAuth2 authentication failed - isAdminLogin: " + isAdminLogin + ", error: " + errorCode);

    // Nastaví chybu a presmeruje
    OAuth2LoginHelper.handleError(request, response, errorCode, isAdminLogin);
}
```

**| Exception | Error code |
| ----------------------------------------------------------------- | ---------------------------------------- |
|&#x20;**

&#x20;                 | `OAuth2AuthorizationCodeRequestTypeNotSupported`         |
| `oauth2_provider_not_configured` s `OAuth2AuthenticationException` /`invalid_token` | `invalid_grant`                   |
| `oauth2_invalid_token` s `OAuth2AuthenticationException`                 | `access_denied`                   |
| `oauth2_access_denied` s `OAuth2AuthenticationException`                  | `server_error`                    |
| `oauth2_server_error`                           | `AuthorizationRequestNotFoundException` |
| `oauth2_authorization_request_not_found`                                    | `ClientAuthorizationException`     |
| Other | `oauth2_client_authorization_failed`           |`oauth2_authentication_failed`2. Bugs in Success Handlers

**&#x20;(**):`handleError`Common method in&#x20;

&#x20;using `AbstractOAuth2SuccessHandler`:`OAuth2LoginHelper`Admin Zone

```java
// Pomocná metóda pre spracovanie OAuth2 chýb
protected void handleError(HttpServletRequest request, HttpServletResponse response,
                          String errorCode, String redirectUrl) throws IOException {
    HttpSession session = request.getSession();
    session.setAttribute("oauth2_logon_error", errorCode);
    response.sendRedirect(redirectUrl);
}
```

**&#x20;(**):`OAuth2AdminSuccessHandler`User zone

```java
// Chyby sa uložia do session a používateľ je presmerovaný na /admin/logon/
handleError(request, response, "oauth2_email_not_found", "/admin/logon/");
handleError(request, response, "oauth2_user_create_failed", "/admin/logon/");
handleError(request, response, "oauth2_exception", "/admin/logon/");

// Špeciálna chyba pre chýbajúce admin práva
if (!userDetails.isAdmin()) {
    session.setAttribute("oauth2_logon_error", "accessDenied");
    response.sendRedirect("/admin/logon/");
}
```

**&#x20;(**):`OAuth2UserSuccessHandler`Error handling in AdminLogonController

```java
// Chyby sa uložia do session a používateľ je presmerovaný na /
handleError(request, response, "oauth2_email_not_found", "/");
handleError(request, response, "oauth2_user_create_failed", "/");
handleError(request, response, "oauth2_exception", "/");
```

### AdminLogonController reads errors from the session and displays the appropriate error messages:

Types of errors:

```java
// Spracuj OAuth2 chyby zo session
String oauth2LogonError = (String)session.getAttribute("oauth2_logon_error");
if (oauth2LogonError != null) {
    Prop prop = Prop.getInstance(request);
    String errorMessage = switch (oauth2LogonError) {
        case "accessDenied" -> prop.getText("logon.err.noadmin");
        case "oauth2_email_not_found" -> prop.getText("logon.err.oauth2_email_not_found");
        case "oauth2_user_create_failed" -> prop.getText("logon.err.oauth2_user_create_failed");
        case "oauth2_exception" -> prop.getText("logon.err.oauth2_exception");
        default -> prop.getText("logon.err.oauth2_unknown");
    };
    model.addAttribute("errors", errorMessage);
    session.removeAttribute("oauth2_logon_error"); // Odstránenie po zobrazení
}
```

**&#x20;- OAuth2 provider did not return email attribute**
- `oauth2_email_not_found` - Failed to create a new user in the database
- `oauth2_user_create_failed` - General error during OAuth2 process
- `oauth2_exception` - User does not have admin rights after synchronization of rights
- `accessDenied` Translation keys

### To display error messages correctly, you must have the following translation keys defined in&#x20;

&#x20;files:`text.properties`OAuth2 Login Helper

```properties
logon.err.noadmin=Zadaný používateľ nie je administrátor systému
logon.err.oauth2_email_not_found=OAuth2 prihlásenie zlyhalo: email sa nepodarilo získať
logon.err.oauth2_user_create_failed=OAuth2 prihlásenie zlyhalo: nepodarilo sa vytvoriť používateľa
logon.err.oauth2_exception=OAuth2 prihlásenie zlyhalo: vyskytla sa neočakávaná chyba
logon.err.oauth2_unknown=OAuth2 prihlásenie zlyhalo: neznáma chyba
```

## &#x20;is a helper class with shared functionality for OAuth2 processing. It provides:

`OAuth2LoginHelper`Detection of login type

### Error handling

```java
// Zistí, či ide o admin prihlásenie (na základe session atribútu)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLogin(request);

// Zistí typ prihlásenia a odstráni session atribút (jednorazové použitie)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);
```

### Redirect URL constants

```java
// Nastaví chybu do session a presmeruje podľa typu prihlásenia
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", isAdminLogin);

// Alebo s explicitnou redirect URL
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", "/custom/redirect/");
```

### OAuth2 Dynamic Success Handler

```java
OAuth2LoginHelper.getAdminRedirectUrl();  // "/admin/logon/"
OAuth2LoginHelper.getUserRedirectUrl();   // "/"
OAuth2LoginHelper.getErrorRedirectUrl(isAdminLogin);  // Podľa typu
```

## Principle of operation

### &#x20;decides which handler to use based on the session attribute&#x20;

`OAuth2DynamicSuccessHandler` via `oauth2_is_admin_section`:`OAuth2LoginHelper`Setting the session attribute

```java
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) throws IOException {
    // Použije helper na zistenie typu prihlásenia a vyčistí session atribút
    boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);

    Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);

    if (isAdminLogin) {
        adminHandler.onAuthenticationSuccess(request, response, authentication);
    } else {
        userHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
```

### Admin Zone

**:&#x20;**&#x20;sets:`AdminLogonController.showLogonForm()`User zone

```java
session.setAttribute("oauth2_is_admin_section", true);
```

**: Logon controller for customer zone sets&#x20;**&#x20;or sets nothing (default is user handler).`false`Login page

## Display OAuth2 buttons

### OAuth2 buttons are displayed automatically if a configuration variable is set&#x20;

:`oauth2_clients`If the configuration variable is set&#x20;

```jsp
<c:if test="${isOAuth2Enabled}">
    <c:forEach var="url" items="${logonUrls}">
        <div class="form-group">
            <button type="button" name="oauth2-login-submit"
                    class="btn btn-primary"
                    onclick="window.location.href='${url.value}'">
                <iwcm:text key="button.oauth2Login"/> ${url.key}
                <i class="ti ti-arrow-right"></i>
            </button>
        </div>
    </c:forEach>
</c:if>
```

&#x20;to the name of the provider (e.g. `oauth2_adminLogonAutoRedirect` or `Keycloak`), the administration login page automatically redirects to the OAuth2 provider. Thus, the standard WebJET CMS login form is not displayed.`Google`OAuth2 URL generation

### &#x20;automatically generates OAuth2 URLs for all configured providers:

`AdminLogonController`Diagnostics and tuning

```java
if (Tools.isNotEmpty(Constants.getString("oauth2_clients")) && clientRegistrationRepository != null) {
    // Nastav explicitný atribút pre OAuth2 admin login
    session.setAttribute("oauth2_is_admin_section", true);

    // Ak adminAfterLogonRedirect neexistuje, nastav defaultnú hodnotu
    if (session.getAttribute("adminAfterLogonRedirect") == null) {
        session.setAttribute("adminAfterLogonRedirect", "/admin/");
    }

    // Generuj URL pre všetkých OAuth2 providerov
    clientRegistrations.forEach(registration ->
        oauth2AuthenticationUrls.put(registration.getClientName(),
            authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
    model.addAttribute("logonUrls", oauth2AuthenticationUrls);
}
```

## Logging in

### OAuth2 Success Handlers include detailed logging for diagnostics:

OAuth2DynamicSuccessHandler

**:**&#x41;bstractOAuth2SuccessHandler

```java
Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);
```

**:**&#x4F;Auth2AdminSuccessHandler / OAuth2UserSuccessHandler

```java
Logger.debug(AbstractOAuth2SuccessHandler.class, "Detected OAuth2 provider ID: " + providerId);
Logger.debug(AbstractOAuth2SuccessHandler.class, "Provider '" + providerId + "' shouldSyncPermissions: " + shouldSync);
Logger.info(AbstractOAuth2SuccessHandler.class, "Final extracted groups: " + groups);
```

**:**&#x43;ommon problems

```java
Logger.info(class, "Created new user for email: " + email);
Logger.info(class, "Applying OAuth2 permissions for provider: " + providerId);
Logger.info(class, "Found OAuth2 groups for user " + email + ": " + groups);
Logger.info(class, "Synchronized user " + email + " to " + newUserGroups.size() + " user groups");
```

### Missing email

1. **&#x20;- OAuth2 provider does not return an email attribute** Check the scopes:&#x20;
    - Error: `openid,profile,email`
    - Invalid configuration`oauth2_email_not_found`

2. **&#x20;- Error OAuth2 endpoint URL** Check all URIs in the configuration
    - For custom providers, all parameters must be set
    - Groups do not synchronise

3. **&#x20;- Provider is not configured in&#x20;** &#x20;or erroneous group names`oauth2_clientsWithPermissions`Check&#x20;
    - &#x20;Configuration`oauth2_clientsWithPermissions`Verify group names in WebJET vs OAuth2 provider
    - See logs at DEBUG level for details of group extraction
    - Redirect URI

4. **&#x20;- Incorrectly set redirect URI at the provider** Format:&#x20;
    - Example: `{baseUrl}/login/oauth2/code/{registrationId}`
    - Admin zone vs User zone`https://your-domain.com/login/oauth2/code/keycloak`

5. **&#x20;- Login is pointing to the wrong handler** Check the settings&#x20;
    - &#x20;session attribute`oauth2_is_admin_section`Admin logon page must set this attribute
    - Missing admin rights

6. **&#x20;- The user cannot access the admin area** Check&#x20;
    - &#x20;Configuration`NTLMAdminGroupName`Verify the presence of the admin group in OAuth2 groups
    - Error:&#x20;
    - Configuration examples`accessDenied`

## Keycloak with WebJET

### Creating a client in Keycloak:

**Client ID:&#x20;**

- Client Protocol: `webjetcms-client`
- Access Type: `openid-connect`
- Valid Redirect URIs: `confidential`
- Standard Flow Enabled: `https://your-domain.com/login/oauth2/code/keycloak`
- Direct Access Grants Enabled: `ON`
- &#x20;(recommended)`OFF`Configuration in WebJET:

**Groups in Keycloak:**

```properties
oauth2_clients=keycloak
oauth2_clientsWithPermissions=keycloak
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=generated-secret-from-keycloak
oauth2_keycloakAuthorizationUri=https://keycloak.local/auth/realms/webjet/protocol/openid-connect/auth
oauth2_keycloakTokenUri=https://keycloak.local/auth/realms/webjet/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=https://keycloak.local/auth/realms/webjet/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=https://keycloak.local/auth/realms/webjet/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=https://keycloak.local/auth/realms/webjet
oauth2_keycloakUserNameAttributeName=email
oauth2_keycloakScopes=openid,profile,email
oauth2_keycloakClientName=Keycloak Login
NTLMAdminGroupName=webjet-admin
```

**Create groups with names identical to WebJET groups**

- Assign users to the appropriate groups
- Admin group:&#x20;
- &#x20;(by NTLMAdminGroupName)`webjet-admin`Map groups to a token (Client Scopes - dedicated scope - Add mapper - Group Membership)
- Testing:

**Log in via OAuth2 on the admin page:&#x20;**

- Verify group synchronization in the WebJET admin interface`https://your-domain.com/admin/logon/`
- Check the logs for detailed synchronization information
- Google OAuth2

### Note

```properties
oauth2_clients=google
oauth2_googleClientId=your-google-client-id.apps.googleusercontent.com
oauth2_googleClientSecret=your-google-client-secret
```

**: For Google OAuth2&#x20;**&#x6F;ut of syn&#x63;**&#x20;groups because Google is not in&#x20;**. You need to use a provider like Keycloak or Okta to sync groups.`oauth2_clientsWithPermissions`OAuth2 for customer zone (User logon)

### To implement OAuth2 login in the customer zone:

Controller configuration for user logon:

**Difference from the admin zone:**

```java
@GetMapping("logon/")
public String showLogonForm(ModelMap model, HttpServletRequest request, HttpSession session) {
    if (Tools.isNotEmpty(Constants.getString("oauth2_clients")) && clientRegistrationRepository != null) {
        // Nenastavujeme oauth2_is_admin_section (default je false/user handler)

        // Nastav redirect po prihlásení
        if (session.getAttribute("afterLogonRedirect") == null) {
            session.setAttribute("afterLogonRedirect", "/");
        }

        // Generuj URL pre všetkých OAuth2 providerov
        clientRegistrations.forEach(registration ->
            oauth2AuthenticationUrls.put(registration.getClientName(),
                authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
        model.addAttribute("logonUrls", oauth2AuthenticationUrls);
    }
    return USER_LOGON_FORM;
}
```

**Does not set**

- **&#x20;session attribute (or sets the&#x20;**)`oauth2_is_admin_section`Used by `false` instead of&#x20;
- OAuth2UserSuccessHandler:`afterLogonRedirect`Synchronizes only user groups`adminAfterLogonRedirect`
- Does not set permission groups
  - Does not set admin flag
  - Does not require admin rights
  - Overview of implementation
  - OAuth2 integration is implemented using the Spring Security OAuth2 module and includes:

## Spring Security configuration

&#x20;\- Configuring OAuth2 clients and endpoints
- **OAuth2DynamicSuccessHandler** - Dynamic decision making between admin and user processor after successful authentication
- **OAuth2DynamicErrorHandler** - Handling OAuth2 authentication errors with redirection to the correct logon page
- **OAuth2LoginHelper** - Helper class with shared functionality for OAuth2 processing (login type detection, error handling)
- **Two types of Success Handlers**:
- **OAuth2AdminSuccessHandler** - For admin zone (synchronizes user groups + permission groups + admin flag)
  - **OAuth2UserSuccessHandler** - For user zone (synchronizes only user groups)
  - **AbstractOAuth2SuccessHandler** - Common base class with functionality for both processors
- **Automatic group synchronisation** - Mapping groups from OAuth2 provider to WebJET groups (only for configured providers)
- **Admin rights from OAuth2** - Automatically set admin rights based on groups (admin zone only)
- **Architecture** Main components

## SpringSecurityConf

### Configuring OAuth2 clients

1. [Registering OAuth2DynamicSuccessHandler and OAuth2DynamicErrorHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java)

    - Configuring authorized client service
    - OAuth2LoginHelper
    - Helper class with shared functionality for OAuth2 processing

2. [Detection of login type (admin/user)](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2LoginHelper.java)

    - Error handling and redirection
    - OAuth2DynamicSuccessHandler
    - Dynamic decision making between admin and user processor

3. [Uses OAuth2LoginHelper to distinguish the login type](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicSuccessHandler.java)

    - OAuth2DynamicErrorHandler
    - Error handling in OAuth2 authentication

4. [Mapping exceptions to error codes](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicErrorHandler.java)

    - Redirect to the correct logon page (admin/user)
    - AbstractOAuth2SuccessHandler
    - Abstract base class for OAuth2 Success Handlers

5. [Common functionality for extracting groups and attributes from OAuth2](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/AbstractOAuth2SuccessHandler.java)

    - Error handling via session
    - OAuth2AdminSuccessHandler
    - Processing successful OAuth2 authentication for&#x20;

6. [admin area](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2AdminSuccessHandler.java)

    - Create or update a user**Synchronising groups and permissions (user groups + permission groups)**
    - Setting admin rights
    - OAuth2UserSuccessHandler
    - Processing successful OAuth2 authentication for&#x20;

7. [customer zone](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2UserSuccessHandler.java)

    - Sync only user groups (not permission groups)**Does not set admin rights**
    - AdminLogonController
    - Display OAuth2 login links on the logon page

8. [URL generation for OAuth2 providers](../../../../src/main/java/sk/iway/iwcm/logon/AdminLogonController.java)

    - Handling OAuth2 session errors
    - Settings&#x20;
    - &#x20;attribute
    - Logon Template`oauth2_is_admin_section`View OAuth2 login buttons

9. [API reference](../../../../src/main/webapp/admin/skins/webjet8/logon-spring.jsp)
    - OAuth2LoginHelper

## Auxiliary class with shared functionality:

### &#x20;- Determines if this is an admin login based on the session attribute

&#x20;\- Detects the login type and removes the session attribute
- `isAdminLogin(request)` - Returns redirect URL for error by login type
- `isAdminLoginAndClear(request)` - Returns the redirect URL for the admin zone (
- `getErrorRedirectUrl(isAdminLogin)`)
- `getAdminRedirectUrl()` - Returns the redirect URL for the user zone (`/admin/logon/`)
- `getUserRedirectUrl()` - Sets the error to session and redirects`/` - Sets the error and redirects by type
- `handleError(request, response, errorCode, redirectUrl)` OAuth2DynamicErrorHandler
- `handleError(request, response, errorCode, isAdminLogin)` Handler for OAuth2 authentication errors:

### &#x20;- Handles the error, detects the login type and redirects to the correct logon page

&#x20;\- Maps exceptions to error codes for the translation table
- `onAuthenticationFailure()` OAuth2DynamicSuccessHandler
- `getErrorCodeFromException()` Dynamic router for OAuth2 authentication:

### &#x20;- Decides between admin and user processor using&#x20;

AbstractOAuth2SuccessHandler

- `onAuthenticationSuccess()` Abstract base class with common functionality:`OAuth2LoginHelper.isAdminLoginAndClear()`

### &#x20;- Abstract method implemented in descendants

&#x20;\- Returns the attribute name for username from the configuration (
- `onAuthenticationSuccess()`), or the default value&#x20;
- `getUsernameAttribute()` - Create a new user from OAuth2 data`oauth2_usernameAttribute` - Updating an existing user`preferred_username`
- `createNewUserFromOAuth2()` - Getting the OAuth2 provider ID from authentication
- `updateExistingUserFromOAuth2()` - Check whether the provider has configured rights synchronization
- `getProviderId()` - Extracting groups from OAuth2 attributes (all formats)
- `shouldSyncPermissions()` - Extraction from simple attributes
- `extractGroupsFromOAuth2()` - Extraction from Keycloak&#x20;
- `extractFromAttribute()` - Extraction from Keycloak&#x20;
- `extractFromResourceAccess()` - Extracting roles from a specific client`resource_access`
- `extractFromRealmAccess()` - Auxiliary method for extraction from object roles`realm_access`
- `extractRolesFromClientResource()` - A helper method for handling OAuth2 errors over session
- `extractRolesFromRolesObject()` OAuth2AdminSuccessHandler (Admin zone)
- `handleError()` Handler for admin zone:

### &#x20;- Main entry point after successful authentication

&#x20;\- Applying rights from OAuth2 (user groups + permission groups + admin flag)
- `onAuthenticationSuccess()` - Remove from all groups (user + permission)
- `applyOAuth2Permissions()` - Synchronizing user groups and permission groups
- `removeAllGroupAssignments()` OAuth2UserSuccessHandler (User zone)
- `synchronizeUserGroups()` Handler for the customer zone:

### &#x20;- Main entry point after successful authentication

&#x20;\- Applying only user groups (not permission groups, not admin flag)
- `onAuthenticationSuccess()` - Remove from all user groups
- `applyOAuth2UserGroups()` - Synchronisation of user groups only
- `removeAllGroupAssignments()` SpringSecurityConf
- `synchronizeUserGroups()` Methods for OAuth2 configuration:

### &#x20;- Registers&#x20;

&#x20;as a success handler

- `filterChain()` - Bean for registering OAuth2 clients (returns empty implementation if there are no clients)`OAuth2DynamicSuccessHandler` - Create a configuration for a specific provider (supports Google, Facebook, GitHub, Okta + custom)
- `clientRegistrationRepository()` - Bean for managing authorized clients
- `buildClientRegistration()` AdminLogonController
- `authorizedClientService()` Methods for OAuth2 admin login:

### &#x20;- Sets the session attribute&#x20;

&#x20;and generates an OAuth2 URL

- `showLogonForm()` Handles OAuth2 errors from the session and displays the appropriate error messages`oauth2_is_admin_section`
- 