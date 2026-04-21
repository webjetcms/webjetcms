# OAuth2 Authentication in WebJET CMS

WebJET CMS supports OAuth2 authentication for user login through external providers such as Google, Facebook, GitHub, Keycloak and others. The configuration is read when WebJET CMS is initialized, after changing values ​​or initial settings, it is necessary to restart the application server.

User pairing is done based on email, which must be unique. When logging in for the first time via OAuth2, a new user is created in the WebJET database with a set login name, first name, last name, and authorization. User groups and rights groups are synchronized based on name matching. Administrators are set up automatically based on group membership defined in the `NTLMAdminGroupName` configuration variable.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/q8xs3qDq-G4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Configuration

### Basic configuration

To activate OAuth2, you need to set a configuration variable:

```properties
oauth2_clients=google,keycloak
```

If you need to display different providers in the administration or customer zone, you can set `oauth2_clientsIncludeAdmin` and `oauth2_clientsIncludeUser`:

```properties
oauth2_clientsIncludeAdmin=keycloak
oauth2_clientsIncludeUser=google,facebook
```

in this case only `keycloak` will be displayed for the administration and `google` and `facebook` for the customer zone. The specified providers must be configured in `oauth2_clients` to be displayed on the login page. The default value is `*`, which means that every configured provider will be displayed.

### Predefined providers

For popular providers, just set `clientId` and `clientSecret`. Using the value `oauth2_{provider}DefaultGroups`, you can define user groups that will be assigned to a newly created user upon first login through the given provider. The value is a comma-separated list of group IDs (e.g. `1,3`). These groups do not apply to existing users, so after the first login you can set the rights for users as you need directly in the WebJET CMS administration and these rights will be preserved for subsequent logins. If you use multiple providers, you can define different groups for each provider to be able to distinguish users for providers.

#### Google

```properties
oauth2_googleClientId=your-google-client-id
oauth2_googleClientSecret=your-google-client-secret
oauth2_googleDefaultGroups=1,2
```

You can get the required values ​​via the [Google API Console](https://console.developers.google.com/) in the Credentials section. When registering an OAuth2 client with Google, it is necessary to set a redirect URI in the format `https://your-domain.com/login/oauth2/code/google`.

#### Facebook

```properties
oauth2_facebookClientId=your-facebook-client-id
oauth2_facebookClientSecret=your-facebook-client-secret
oauth2_facebookDefaultGroups=1,3
```

You can get the required values ​​via [Facebook for Developers](https://developers.facebook.com/docs/facebook-login) in the My Apps section. Create an application, set up login actions. When registering an OAuth2 client with Facebook, you must set a redirect URI in the format `https://your-domain.com/login/oauth2/code/facebook`. In the `App settings` section, you will find `App ID` and `App Secret`, which you will use to configure WebJET CMS.

#### GitHub

```properties
oauth2_githubClientId=your-github-client-id
oauth2_githubClientSecret=your-github-client-secret
oauth2_githubDefaultGroups=1,4
```

#### Octave

```properties
oauth2_oktaClientId=your-okta-client-id
oauth2_oktaClientSecret=your-okta-client-secret
oauth2_oktaDefaultGroups=1,5
```

### Other OAuth2 provider

For custom providers (for example `Keycloak`), all `OAuth2` parameters need to be set. The configuration variable name is dynamically generated from the provider name (for example `keycloak`):

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

If you were to use a provider named `myprovider`, you would need to set variables with the prefix `oauth2_myprovider...`. This way you can have multiple OAuth2 providers set up in the system at the same time (for example `keycloak-admins`, `keycloak-users`), each with its own configuration.

### Supported OAuth2 attributes

WebJET CMS extracts the following attributes from the OAuth2/OIDC provider during OAuth2 authentication:

#### Basic user attributes

- `email` - ​​**Required** - User email (must be unique)
- `given_name` - ​​User's first name
- `family_name` - ​​User's last name
- `preferred_username` - ​​**Default** attribute for login (standard OIDC attribute, configurable via `oauth2_usernameAttribute`)
- `picture` - ​​Profile image URL (if provided)

#### Attributes for groups and rights

To synchronize groups (only for configured providers in `oauth2_clientsWithPermissions`):

- `groups` - ​​Simple group list
- `roles` - ​​Simple role list
- `group_membership` - ​​Alternative name for groups
- `resource_access.<client>.roles` - ​​Keycloak client-specific roles
- `realm_access.roles` - ​​Keycloak realm roles
- Spring Security authorities - Automatically extracted from authentication

**Login name extraction rules:**

1. **Preferred attribute**: Value of configuration variable `oauth2_usernameAttribute` (default `preferred_username`) - if provided by OAuth2 server, will be used for login
2. **Fallback**: Part of the email before the at-sign - if the attribute is not available

**Examples:**

- Keycloak provides `preferred_username: "john.doe"` - ​​login will be `john.doe`
- Google does not provide `preferred_username` - ​​login will be the part before @ of the email (e.g. `john.doe` from `john.doe@gmail.com`)
- If `oauth2_usernameAttribute=sub` is set, the `sub` attribute will be used instead of `preferred_username`

If there is a login conflict when creating a new user (e.g. `john.doe` already exists), the email address will be used as the login to avoid an error and allow logging in. If such an account also exists, a new account `oauth2.google.XXXXXX` will be created where `XXXXXX` is a random alphanumeric string.

### Configuring the login (username) attribute

The name of the OAuth2 attribute used for user login is configurable:

```properties
oauth2_usernameAttribute=preferred_username
```

If the variable is not set, the default value `preferred_username` (standard OIDC attribute) is used. The value of this variable determines which attribute of the OAuth2 token the login is retrieved from. If the given attribute does not exist in the token, the part of the email before the at-sign is used as a fallback.

### Redirect URI

The redirect URI is automatically set for all providers:

```text
{baseUrl}/login/oauth2/code/{registrationId}
```

Example: `https://your-domain.com/login/oauth2/code/google`

## User synchronization

### Create a new user

When a user logs in for the first time via OAuth2:

1. **Data Extraction** - Email, first name, last name, and username are extracted from OAuth2 attributes
2. **Create User** - A new user will be created in the WebJET database
3. **Setting the login name** - The login name is preferably set from the attribute specified by the configuration variable `oauth2_usernameAttribute` (by default `preferred_username`), if it is not available, the part of the email before the at sign will be used.
4. **Authorization** - The user is marked as authorized

User creation is done in `AbstractOAuth2SuccessHandler.createNewUserFromOAuth2()`:

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

**Supported login attributes:**

- Configurable via `oauth2_usernameAttribute` (default `preferred_username` - standard OIDC attribute)
- Fallback: part of the email before the at-sign

### Updating an existing user

For existing users, the following are updated:

- First and last name (if changed in the OAuth2 provider)
- Login (if the OAuth2 provider provides the attribute defined in `oauth2_usernameAttribute` / default `preferred_username` and it has changed)
- Group assignments (only for configured providers in `oauth2_clientsWithPermissions`)

The update is in progress at `AbstractOAuth2SuccessHandler.updateExistingUserFromOAuth2()`.

## Group synchronization

OAuth2 Success Handlers contain specialized logic for synchronizing groups from configured OAuth2 providers.

### Differences between Admin and User zones

**Admin zone** (`OAuth2AdminSuccessHandler`):

- Synchronizes **user groups**
- Synchronizes **permission groups**
- Sets **admin flag** based on `NTLMAdminGroupName`
- Requires admin rights for successful login

**User zone** (`OAuth2UserSuccessHandler`):

- Only synchronizes **user groups**
- **Does not synchronize** permission groups
- **Does not set** admin flag
- Does not require admin rights

### Configuring rights synchronization

To synchronize groups and permissions from the OAuth2 provider, you need to set a configuration variable:

```properties
oauth2_clientsWithPermissions=keycloak,okta
```

This variable defines a list of providers (separated by commas) for which groups and rights synchronization should be performed. For other providers (e.g. Google, Facebook), synchronization is not performed.

The check is in progress at `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`.

### Provider detection

The Provider ID is obtained in `AbstractOAuth2SuccessHandler.getProviderId()`:

```java
protected String getProviderId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken) {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        return oauth2Token.getAuthorizedClientRegistrationId();
    }
    return null;
}
```

The check to see if rights should be synchronized is done in `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`:

```java
protected boolean shouldSyncPermissions(String providerId) {
    if (providerId == null) return false;
    String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
    if (Tools.isEmpty(configuredProviders)) return false;
    List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
    return providers.contains(providerId);
}
```

### Group extraction

Group extraction is done in `AbstractOAuth2SuccessHandler.extractGroupsFromOAuth2()` and supports various formats:

1. **Basic attributes**: `groups`, `roles`, `group_membership`
2. **Keycloak realm_access**: `realm_access.roles`
3. **Keycloak resource_access**: `resource_access.{client}.roles`
4. **Spring Security authorities**: Automatically extracted roles (with the `ROLE_` prefix removed)

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

- `extractFromAttribute()` - ​​extracts from simple attributes
- `extractFromResourceAccess()` - ​​extracts from Keycloak `resource_access`
- `extractFromRealmAccess()` - ​​extracts from Keycloak `realm_access`
- `extractRolesFromClientResource()` - ​​extracts roles from a specific client
- `extractRolesFromRolesObject()` - ​​helper method for extraction from the roles object

### Group mapping

Mapping takes place in the `applyOAuth2Permissions()` (admin) or `applyOAuth2UserGroups()` (user) method:

1. **User Groups** - Looking for user groups with the same name as the OAuth2 group
2. **Permission Groups** - Looking for permission groups with the same name as the OAuth2 group (admin zone only)
3. **Full Sync** - All old groups will be deleted and new ones will be added

**Admin zone** (`OAuth2AdminSuccessHandler.synchronizeUserGroups()`):

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

### Automatic setup of admin rights (admin zone only)

For configured OAuth2 providers (defined in `oauth2_clientsWithPermissions`), admin rights are set automatically in the **admin zone** based on the configuration variable:

```properties
NTLMAdminGroupName=admin
```

If OAuth2 groups contain a group named from `NTLMAdminGroupName`, the user will be marked as an admin.

The setup is done in `OAuth2AdminSuccessHandler.applyOAuth2Permissions()`:

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

**Important**: Admin rights are checked after group synchronization. If a user does not have admin rights, login to the admin zone is denied with the error `accessDenied`.

**User zone**: `OAuth2UserSuccessHandler` **does not set** admin rights - this is for the admin zone only.

## Security aspects

### Limitations of OAuth2 processes

1. **Configurable synchronization** - Synchronization of groups and admin rights only works for OAuth2 providers configured in `oauth2_clientsWithPermissions`
2. **Email validation** - Each user must have a valid email attribute
3. **Email Uniqueness** - Email must be unique in the system

### Error handling

OAuth2 integration includes two types of error handling:

**1. Authentication errors** (`OAuth2DynamicErrorHandler`):

Handles errors that occur during OAuth2 authentication (e.g. invalid token, server error):

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

**Error mapping to codes:**

| Exception | Error code |
| --------- | ---------- |
| `OAuth2AuthorizationCodeRequestTypeNotSupported` | `oauth2_provider_not_configured` |
| `OAuth2AuthenticationException` with `invalid_token` /`invalid_grant` | `oauth2_invalid_token` |
| `OAuth2AuthenticationException` with `access_denied` | `oauth2_access_denied` |
| `OAuth2AuthenticationException` with `server_error` | `oauth2_server_error` |
| `AuthorizationRequestNotFoundException` | `oauth2_authorization_request_not_found` |
| `ClientAuthorizationException` | `oauth2_client_authorization_failed` |
| Other | `oauth2_authentication_failed` |

**2. Errors in Success Handlers** (`handleError`):

Common method in `AbstractOAuth2SuccessHandler` using `OAuth2LoginHelper`:

```java
// Pomocná metóda pre spracovanie OAuth2 chýb
protected void handleError(HttpServletRequest request, HttpServletResponse response,
                          String errorCode, String redirectUrl) throws IOException {
    HttpSession session = request.getSession();
    session.setAttribute("oauth2_logon_error", errorCode);
    response.sendRedirect(redirectUrl);
}
```

**Admin zone** (`OAuth2AdminSuccessHandler`):

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

**User zone** (`OAuth2UserSuccessHandler`):

```java
// Chyby sa uložia do session a používateľ je presmerovaný na /
handleError(request, response, "oauth2_email_not_found", "/");
handleError(request, response, "oauth2_user_create_failed", "/");
handleError(request, response, "oauth2_exception", "/");
```

### Error handling in AdminLogonController

AdminLogonController reads errors from the session and displays the corresponding error messages:

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

**Error types:**

- `oauth2_email_not_found` - ​​OAuth2 provider did not return email attribute
- `oauth2_user_create_failed` - ​​Failed to create new user in database
- `oauth2_exception` - ​​General error during OAuth2 process
- `accessDenied` - ​​User does not have admin rights after rights synchronization

### Translation keys

To display error messages correctly, the following translation keys must be defined in the `text.properties` files:

```properties
logon.err.noadmin=Zadaný používateľ nie je administrátor systému
logon.err.oauth2_email_not_found=OAuth2 prihlásenie zlyhalo: email sa nepodarilo získať
logon.err.oauth2_user_create_failed=OAuth2 prihlásenie zlyhalo: nepodarilo sa vytvoriť používateľa
logon.err.oauth2_exception=OAuth2 prihlásenie zlyhalo: vyskytla sa neočakávaná chyba
logon.err.oauth2_unknown=OAuth2 prihlásenie zlyhalo: neznáma chyba
```

## OAuth2 Login Helper

`OAuth2LoginHelper` is a helper class with shared functionality for OAuth2 processing. It provides:

### Login type detection

```java
// Zistí, či ide o admin prihlásenie (na základe session atribútu)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLogin(request);

// Zistí typ prihlásenia a odstráni session atribút (jednorazové použitie)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);
```

### Error handling

```java
// Nastaví chybu do session a presmeruje podľa typu prihlásenia
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", isAdminLogin);

// Alebo s explicitnou redirect URL
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", "/custom/redirect/");
```

### Redirect URL constants

```java
OAuth2LoginHelper.getAdminRedirectUrl();  // "/admin/logon/"
OAuth2LoginHelper.getUserRedirectUrl();   // "/"
OAuth2LoginHelper.getErrorRedirectUrl(isAdminLogin);  // Podľa typu
```

## OAuth2 Dynamic Success Handler

### Operating principle

`OAuth2DynamicSuccessHandler` decides which handler to use based on the session attribute `oauth2_is_admin_section` using `OAuth2LoginHelper`:

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

### Setting the session attribute

**Admin zone**: `AdminLogonController.showLogonForm()` sets:

```java
session.setAttribute("oauth2_is_admin_section", true);
```

**User zone**: The logon controller for the customer zone sets `false` or sets nothing (default is user handler).

## Login page

### Displaying OAuth2 buttons

OAuth2 buttons are displayed automatically if the configuration variable `oauth2_clients` is set:

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

If the configuration variable `oauth2_adminLogonAutoRedirect` is set to the name of the provider (e.g. `Keycloak` or `Google`), the login page for the administration will automatically redirect to the given OAuth2 provider. Therefore, the standard WebJET CMS login form will not be displayed either.

### Generating OAuth2 URLs

`AdminLogonController` automatically generates OAuth2 URLs for all configured providers:

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

## Diagnostics and debugging

### Logging

OAuth2 Success Handlers include detailed logging for diagnostics:

**OAuth2DynamicSuccessHandler**:

```java
Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);
```

**AbstractOAuth2SuccessHandler**:

```java
Logger.debug(AbstractOAuth2SuccessHandler.class, "Detected OAuth2 provider ID: " + providerId);
Logger.debug(AbstractOAuth2SuccessHandler.class, "Provider '" + providerId + "' shouldSyncPermissions: " + shouldSync);
Logger.info(AbstractOAuth2SuccessHandler.class, "Final extracted groups: " + groups);
```

**OAuth2AdminSuccessHandler / OAuth2UserSuccessHandler**:

```java
Logger.info(class, "Created new user for email: " + email);
Logger.info(class, "Applying OAuth2 permissions for provider: " + providerId);
Logger.info(class, "Found OAuth2 groups for user " + email + ": " + groups);
Logger.info(class, "Synchronized user " + email + " to " + newUserGroups.size() + " user groups");
```

### Common problems

1. **Missing email** - OAuth2 provider does not return email attribute
   - Check scopes: `openid,profile,email`
   - Error: `oauth2_email_not_found`

2. **Invalid Configuration** - Invalid OAuth2 endpoint URL
   - Check all URIs in the configuration
   - All parameters must be set for custom providers

3. **Groups not syncing** - Provider not configured in `oauth2_clientsWithPermissions` or incorrect group names
   - Check `oauth2_clientsWithPermissions` configuration
   - Verify group names in WebJET vs OAuth2 provider
   - See logs at DEBUG level for details of group extraction

4. **Redirect URI** - Incorrectly set redirect URI at the provider
   - Format: `{baseUrl}/login/oauth2/code/{registrationId}`
   - Example: `https://your-domain.com/login/oauth2/code/keycloak`

5. **Admin zone vs User zone** - Login is directed to the wrong handler
   - Check the setting of the `oauth2_is_admin_section` session attribute
   - Admin logon page must set this attribute

6. **Missing admin rights** - User cannot access the admin area
   - Check `NTLMAdminGroupName` configuration
   - Verify the presence of the admin group in OAuth2 groups
   - Error: `accessDenied`

## Configuration examples

### Keycloak with WebJET

**Creating a client in Keycloak:**

- Client ID: `webjetcms-client`
- Client Protocol: `openid-connect`
- Access Type: `confidential`
- Valid Redirect URIs: `https://your-domain.com/login/oauth2/code/keycloak`
- Standard Flow Enabled: `ON`
- Direct Access Grants Enabled: `OFF` (recommended)

**Configuration in WebJET:**

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

**Groups in Keycloak:**

- Create groups with names identical to WebJET groups
- Assign users to appropriate groups
- Admin group: `webjet-admin` (by NTLMAdminGroupName)
- Map groups to tokens (Client Scopes - dedicated scope - Add mapper - Group Membership)

**Testing:**

- Log in via OAuth2 on the admin page: `https://your-domain.com/admin/logon/`
- Verify group synchronization in the WebJET admin interface
- Check logs for detailed synchronization information

### Google OAuth2

```properties
oauth2_clients=google
oauth2_googleClientId=your-google-client-id.apps.googleusercontent.com
oauth2_googleClientSecret=your-google-client-secret
```

**Note**: For Google OAuth2, groups are **not** synchronized because Google is not in `oauth2_clientsWithPermissions`. You need to use a provider like Keycloak or Okta to synchronize groups.

### OAuth2 for customer zone (User logon)

To implement OAuth2 login in the customer zone:

**Controller configuration for user logon:**

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

**Difference from the admin zone:**

- **Does not** set the `oauth2_is_admin_section` session attribute (or sets it to `false`)
- Uses `afterLogonRedirect` instead of `adminAfterLogonRedirect`
- OAuth2UserSuccessHandler:
  - Synchronizes only user groups
  - Does not set permission groups
  - Does not set admin flag
  - Does not require admin rights

## Implementation overview

OAuth2 integration is implemented using the Spring Security OAuth2 module and includes:

- **Spring Security configuration** - Configuring OAuth2 clients and endpoints
- **OAuth2DynamicSuccessHandler** - Dynamic decision-making between admin and user handler after successful authentication
- **OAuth2DynamicErrorHandler** - Handling errors during OAuth2 authentication with redirection to the correct logon page
- **OAuth2LoginHelper** - Helper class with shared functionality for OAuth2 processing (login type detection, error handling)
- **Two types of Success Handlers**:
  - **OAuth2AdminSuccessHandler** - For admin zone (synchronizes user groups + permission groups + admin flag)
  - **OAuth2UserSuccessHandler** - For user zone (only synchronizes user groups)
- **AbstractOAuth2SuccessHandler** - Common base class with functionality for both handlers
- **Automatic group synchronization** - Mapping groups from OAuth2 provider to WebJET groups (only for configured providers)
- **Admin rights from OAuth2** - Automatic setting of admin rights based on groups (admin zone only)

## Architecture

### Main components

1. [SpringSecurityConf](../../../../src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java)

   - Configuring OAuth2 clients
   - Registering OAuth2DynamicSuccessHandler and OAuth2DynamicErrorHandler
   - Configuring authorized client service

2. [OAuth2LoginHelper](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2LoginHelper.java)

   - Helper class with shared functionality for OAuth2 processing
   - Login type detection (admin/user)
   - Error handling and redirection

3. [OAuth2DynamicSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicSuccessHandler.java)

   - Dynamic decision-making between admin and user processor
   - Uses OAuth2LoginHelper to distinguish login type

4. [OAuth2DynamicErrorHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicErrorHandler.java)

   - Handling errors during OAuth2 authentication
   - Mapping exceptions to error codes
   - Redirect to the correct logon page (admin/user)

5. [AbstractOAuth2SuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/AbstractOAuth2SuccessHandler.java)

   - Abstract base class for OAuth2 Success Handlers
   - Common functionality for extracting groups and attributes from OAuth2
   - Error handling via session

6. [OAuth2AdminSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2AdminSuccessHandler.java)

   - Processing successful OAuth2 authentication for **admin zone**
   - Create or update a user
   - Synchronization of groups and rights (user groups + permission groups)
   - Setting admin rights

7. [OAuth2UserSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2UserSuccessHandler.java)

   - Processing successful OAuth2 authentication for **customer zone**
   - Synchronize only user groups (not permission groups)
   - Does not set admin rights

8. [AdminLogonController](../../../../src/main/java/sk/iway/iwcm/logon/AdminLogonController.java)

   - Display OAuth2 login links on the logon page
   - Generating URLs for OAuth2 providers
   - Handling OAuth2 errors from session
   - Setting the `oauth2_is_admin_section` attribute

9. [Logon Template](../../../../src/main/webapp/admin/skins/webjet8/logon-spring.jsp)
   - Display OAuth2 login buttons

## API reference

### OAuth2LoginHelper

Helper class with shared functionality:

- `isAdminLogin(request)` - ​​Determines if this is an admin login based on the session attribute
- `isAdminLoginAndClear(request)` - ​​Detects login type and removes session attribute
- `getErrorRedirectUrl(isAdminLogin)` - ​​Returns redirect URL for error based on login type
- `getAdminRedirectUrl()` - ​​Returns the redirect URL for the admin zone (`/admin/logon/`)
- `getUserRedirectUrl()` - ​​Returns the redirect URL for the user zone (`/`)
- `handleError(request, response, errorCode, redirectUrl)` - ​​Sets an error in the session and redirects
- `handleError(request, response, errorCode, isAdminLogin)` - ​​Sets an error and redirects by type

### OAuth2DynamicErrorHandler

Handler for OAuth2 authentication errors:

- `onAuthenticationFailure()` - ​​Handles the error, detects the login type and redirects to the correct logon page
- `getErrorCodeFromException()` - ​​Maps exceptions to error codes for the translation table

### OAuth2DynamicSuccessHandler

Dynamic router for OAuth2 authentication:

- `onAuthenticationSuccess()` - ​​Decides between admin and user processor using `OAuth2LoginHelper.isAdminLoginAndClear()`

### AbstractOAuth2SuccessHandler

Abstract base class with common functionality:

- `onAuthenticationSuccess()` - ​​Abstract method implemented in descendants
- `getUsernameAttribute()` - ​​Returns the attribute name for username from the configuration (`oauth2_usernameAttribute`), or the default value `preferred_username`
- `createNewUserFromOAuth2()` - ​​Creating a new user from OAuth2 data
- `updateExistingUserFromOAuth2()` - ​​Updating an existing user
- `getProviderId()` - ​​Finding OAuth2 provider ID from authentication
- `shouldSyncPermissions()` - ​​Checking if the provider has rights synchronization configured
- `extractGroupsFromOAuth2()` - ​​Extract groups from OAuth2 attributes (all formats)
- `extractFromAttribute()` - ​​Extraction from simple attributes
- `extractFromResourceAccess()` - ​​Extraction from Keycloak `resource_access`
- `extractFromRealmAccess()` - ​​Extraction from Keycloak `realm_access`
- `extractRolesFromClientResource()` - ​​Extracting roles from a specific client
- `extractRolesFromRolesObject()` - ​​Helper method for extraction from roles object
- `handleError()` - ​​Helper method for handling OAuth2 errors via session

### OAuth2AdminSuccessHandler (Admin zone)

Handler for admin zone:

- `onAuthenticationSuccess()` - ​​Main entry point after successful authentication
- `applyOAuth2Permissions()` - ​​Applying rights from OAuth2 (user groups + permission groups + admin flag)
- `removeAllGroupAssignments()` - ​​Removal from all groups (user + permission)
- `synchronizeUserGroups()` - ​​Synchronization of user groups and permission groups

### OAuth2UserSuccessHandler (User zone)

Handler for customer zone:

- `onAuthenticationSuccess()` - ​​Main entry point after successful authentication
- `applyOAuth2UserGroups()` - ​​Apply only user groups (not permission groups, not admin flag)
- `removeAllGroupAssignments()` - ​​Removal from all user groups
- `synchronizeUserGroups()` - ​​Synchronization of user groups only

### SpringSecurityConf

Methods for OAuth2 configuration:

- `filterChain()` - ​​Registers `OAuth2DynamicSuccessHandler` as a success handler
- `clientRegistrationRepository()` - ​​Bean for registering OAuth2 clients (returns empty implementation if there are no clients)
- `buildClientRegistration()` - ​​Create a configuration for a specific provider (supports Google, Facebook, GitHub, Okta + custom)
- `authorizedClientService()` - ​​Bean for managing authorized clients

### AdminLogonController

Methods for OAuth2 admin login:

- `showLogonForm()` - ​​Sets the session attribute `oauth2_is_admin_section` and generates an OAuth2 URL
- Handles OAuth2 errors from the session and displays appropriate error messages
