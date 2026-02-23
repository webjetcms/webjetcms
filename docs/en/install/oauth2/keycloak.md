# Keycloak - Installation and configuration for WebJET CMS

<!-- spellcheck-off -->

This tutorial describes the complete procedure for installing a local Keycloak server via Docker and configuring it for logging into the WebJET CMS, including synchronizing groups and permissions.

## 1. Installing Keycloak

### Running via Docker Compose

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml up -d
```

Keycloak will be available on: **http://localhost:18080**

### Login to Keycloak administration

- **URL**: http://localhost:18080
- **Username**: `admin`
- **Password**: `admin`

### Stopping Keycloak

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml down
```

!>**Note**: Keycloak data is stored in a Docker volume `webjetcms-keycloakdata` and survive the container reboot.

### Ready version

For ease of use, a version configured via a file is prepared [realm-export.json.template](../../../../.devcontainer/keycloak/realm-export.json.template), which contains a ready-to-use configuration for direct use. It is necessary to `hosts` file to add an entry:

```shell
127.0.0.1   keycloak.local
```

and set the environment variables to `CODECEPT_DEFAULT_PASSWORD` to the password that will be used to create `admin` users but also users for WebJET CMS `adminuser,testuser,banker`. They are assigned to groups `Admini,Bankári,Newsletter`.

```shell
CODECEPT_DEFAULT_PASSWORD=your_password_here
```

Then you just need to start the container, which will use the JSON template. In WebJET CMS you just need to set the configuration variables:

```txt
oauth2_clients=keycloak
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=your_password_here set in CODECEPT_DEFAULT_PASSWORD ENV variable
```

the remaining variables are set to values that correspond to the configuration in realm-export.json.template. After restarting the WebJET CMS, you will be able to test the login using Keycloak. Its administration is available at http://keycloak.local:18080 (don't forget to add an entry to the hosts file). After logging in to the Keycloak admin console, don't forget to switch the top right realm to `webjetcms`.

## 2. Keycloak configuration

### 2.1 Creating Realm

1. Log in to the Keycloak admin console (http://localhost:18080)

2. In the upper left corner click on the dropdown "master" - **Create realm**

3. Fill in:
    - **Realm name**: `webjetcms` (or any name)
    - **Enabled**: ON

4. Click **Create**

### 2.2 Creating a Client

1. In the left menu, click **Clients** - **Create client**

2. **General Settings**:
    - **Client type**: `OpenID Connect`
    - **Client ID**: `webjetcms-client` (you will use this ID in the WebJET configuration)
    - Click **Next**

3. **Capability config**:
    - **Client authentication**: `ON` (changes to "confidential" type)
    - **Standard flow**: `ON`
    - **Direct access grants**: `OFF` (recommended for production)
    - Click **Next**

4. **Login settings**:
    - **Valid redirect URIs**: `http://localhost/login/oauth2/code/keycloak`
      - For production use: `https://your-domain.com/login/oauth2/code/keycloak`
      - For development you can set: `http://localhost/login/oauth2/code/*`
    - **Valid post logout redirect URIs**: `http://localhost/*`
    - **Web origins**: `http://localhost`

5. Click **Save**

#### Getting Client Secret

1. Go to the tab **Credentials** in the currently created client

2. Copy the value **Client secret** - you will need it when configuring WebJET

### 2.3 Creating Groups

Groups in Keycloak map to user groups and permission groups in WebJET CMS. Group names **must be identical** with group names in WebJET.

1. In the left menu, click **Groups** - **Create group**

2. Create groups as needed, for example:
    - `webjet-admin` - admin group (will be used to set admin rights)
    - `editors` - group for content editors
    - `customerAdmin` - group for customer administrators

### 2.4 Creating Roles

Keycloak knows two types of roles:
- **Client roles** - client-specific roles (e.g. `webjetcms-client`). They appear in the token in the `resource_access.<clientId>.roles`. Suitable if you have several clients and need to differentiate the rights for each of them.
- **Realm roles** - global roles for the entire realm, shared across all clients. They appear in the token in the `realm_access.roles`. Suitable for universal rolls valid across applications.

WebJET CMS automatically extracts and maps **both types of roles** into user groups and rights groups.

#### Creating Client roles

1. Go to **Clients** - select your client (`webjetcms-client`)

2. Bookmark **Roles** - **Create role**

3. Create roles as needed, for example:
    - **Role name**: `customerAdmin`
    - **Role name**: `editors`

4. Assign roles to users:
    - Go to **Users** - select user
    - Bookmark **Role mapping** - **Assign role**
    - Filter: **Filter by clients** - select roles from your client

#### Creating Realm roles

1. In the left menu, click **Realm roles** - **Create role**

2. Create roles as needed, for example:
    - **Role name**: `spravca-obsahu`
    - **Role name**: `schvalovatel`

3. Assign roles to users:
    - Go to **Users** - select user
    - Bookmark **Role mapping** - **Assign role**
    - Filter: **Filter by realm roles** - select the required roles

> **Important** A: Both types of roles require the creation of an appropriate mapper in the client scope (Section 2.5) to appear in the token ID and UserInfo response. Without a mapper, the roles do exist in Keycloak, but WebJET CMS will not see them.

### 2.5 Mapping groups, roles and custom attributes to a token

By default, Keycloak does not send groups, client roles, and custom attributes in the ID token or in the UserInfo response. You must create mappers within your client's client scope to make this data available to the WebJET CMS.

> **Note**: Keycloak does contain default mappers for roles (in scope `roles`), but these only add roles to the **access token**. WebJET CMS reads data from **Token ID** a **Endpoint UserInfo**, so you need to create custom mappers with the "Add to ID token" and "Add to userinfo" settings.

#### Mapping groups

1. Go to **Clients** - select your client - tab **Client scopes**

2. Click on `webjetcms-client-dedicated` scope (dedicated scope for your client)

3. Click **Configure a new mapper** (or **Add mapper** - **By configuration**)

4. Select **Group Membership**

5. Fill in:
    - **Name**: `groups`
    - **Token Claim Name**: `groups`
    - **Full group path**: `OFF` (if you just want the group name without the path)
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

6. Click **Save**

#### Mapping client roles

To transfer client roles (created in Section 2.4) to the token:

1. In the same dedicated scope click **Add mapper** - **By configuration**

2. Select **User Client Role**

3. Fill in:
    - **Name**: `client roles`
    - **Client ID**: select your client (e.g. `webjetcms-client`)
    - **Multivalued**: `ON`
    - **Token Claim Name**: `resource_access.webjetcms-client.roles` (replace `webjetcms-client` your client's name)
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Click **Save**

WebJET CMS automatically extracts roles from the format `resource_access.{client}.roles` and compares them with existing groups/groups of rights.

#### Mapping realm roles

If you use realm roles (created in section 2.4), you must create a mapper to make them appear in the token:

1. In the same dedicated scope click **Add mapper** - **By configuration**

2. Select **User Realm Role**

3. Fill in:
    - **Name**: `realm roles`
    - **Multivalued**: `ON`
    - **Token Claim Name**: `realm_access.roles`
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Click **Save**

> **Notice**: Value **Token Claim Name** must be exactly `realm_access.roles`. Any other value will cause the WebJET CMS to not find the role.

#### Mapping of custom attributes (e.g. customerNumber)

To map custom user attributes (such as `customerNumber`, `custId`, etc.):

1. In the same dedicated scope click **Add mapper** - **By configuration**

2. Select **User Attribute**

3. Fill in:
    - **Name**: `customerNumber`
    - **User Attribute**: `customerNumber` (attribute name in Keycloak profile)
    - **Token Claim Name**: `customerNumber` (name in token)
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Click **Save**

Repeat for each custom attribute you want to have in the token.

### 2.6 Defining custom attributes in User Profile

To make custom attributes (e.g. `customerNumber`) are displayed to users and available in tokens, you must first define them in the User Profile:

1. In the left menu, click **Realm settings** - bookmark **User profile**

2. Click **Create attribute**

3. Fill in:
    - **Attribute name**: `customerNumber`
    - **Display name**: `Customer Number` (optional - displayed by user)
    - **Multivalued**: `OFF`

4. In the section **Permission** Set:
    - **Can user view?**: `ON`
    - **Can user edit?**: `ON` (or `OFF` if you don't want to be able to change it)
    - **Can admin view?**: `ON`
    - **Can admin edit?**: `ON`

5. Click **Create**

Repeat for each custom attribute. Once created, the attribute will appear in the user details on the **Details**.

### 2.7 Creating a test user

1. In the left menu, click **Users** - **Add user**

2. Fill in:
    - **Username**: `testuser`
    - **Email**: `testuser@example.com`
    - **Email verified**: `ON`
    - **First name**: `Test`
    - **Last name**: `User`

3. Click **Create**

4. Bookmark **Credentials** - **Set password**:
    - **Password**: `test`
    - **Temporary**: `OFF`

5. Bookmark **Groups** - **Join group** - add the user to the created groups (e.g. `webjet-admin`)

6. Bookmark **Role mapping** - **Assign role** - Filter by clients - assign client roles (e.g. `customerAdmin`)

7. For custom attributes: tab **Details** - scroll down, fill in the attribute value (e.g. **Customer Number**: `0134416700`) - click **Save**

## 3. Configuration of WebJET CMS

### 3.1 Configuration variables for the admin zone

Set the following configuration variables in WebJET CMS (Settings - Configuration):

```properties
# Zoznam OAuth2 providerov (oddelený čiarkami)
oauth2_clients=keycloak

# Client ID a Secret z Keycloak (záložka Credentials)
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=VLOŽTE-CLIENT-SECRET-Z-KEYCLOAK

# Keycloak endpointy (nahraďte realm názvom vášho realmu)
oauth2_keycloakAuthorizationUri=http://localhost:18080/realms/webjet/protocol/openid-connect/auth
oauth2_keycloakTokenUri=http://localhost:18080/realms/webjet/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=http://localhost:18080/realms/webjet/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=http://localhost:18080/realms/webjet/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=http://localhost:18080/realms/webjet

# Atribút pre identifikáciu používateľa v OIDC tokene
oauth2_keycloakUserNameAttributeName=email

# Požadované scopes
oauth2_keycloakScopes=openid,profile,email

# Názov tlačidla na logon stránke
oauth2_keycloakClientName=Keycloak Login

# Provideri pre synchronizáciu skupín a práv
oauth2_clientsWithPermissions=keycloak

# Názov skupiny pre admin práva (musí existovať aj v Keycloak aj vo WebJET)
NTLMAdminGroupName=webjet-admin
```

**Important**: Variables `oauth2_keycloakTokenUri`, `oauth2_keycloakUserInfoUri` a `oauth2_keycloakJwkSetUri` must be accessible from the server where the WebJET CMS is running (server-to-server communication). Variable `oauth2_keycloakAuthorizationUri` must be accessible from the user's browser.

### 3.2 Configuration for customer/user zone

The same configuration variables apply for logging in to the customer zone. The difference is in the behavior of the handlers:
- **Admin Zone** (`OAuth2AdminSuccessHandler`): synchronizes user groups + permission groups + sets admin flag + requires admin rights
- **User zone** (`OAuth2UserSuccessHandler`): synchronizes only user groups, does not set admin flag, does not require admin rights

Resolution is taken care of `OAuth2DynamicSuccessHandler` based on the session attribute `oauth2_is_admin_section` that sets the admin logon page.

### 3.3 Configuring a custom username attribute

By default, the attribute is used for the user login `preferred_username` from the OIDC token. If your Keycloak (or other OAuth2 provider) sends the login in a different attribute, you can configure it:

```properties
oauth2_usernameAttribute=customerNumber
```

**Example**: If the token contains `"customerNumber": "0134416700"`, the user login will be set to `0134416700`.

If the configuration variable is not set, the default value is used `preferred_username`. If the attribute does not exist in the token, the part of the email before the callback is used as a fallback.

### 3.4 Creating groups in WebJET CMS

For group synchronization to work, the groups must exist in WebJET and their names must match the group/role names in the Keycloak token.

1. Go to **Admin** - **Users** - **User groups**

2. Create groups with **by the same names** as in Keycloak:
    - `webjet-admin`
    - `editors`
    - `customerAdmin`

3. For admin rights: Create and **groups of rights** (Admin - Users - Rights Groups) with the same names

WebJET CMS automatically:
- Retrieves groups from the token (from `groups`, `roles`, `resource_access.*.roles`, `realm_access.roles`)
- Matches them against existing groups in WebJET by name
- Removes old assignments and sets new ones (full synchronization)
- Sets the admin flag in the admin zone if the oauth2 groups contain a group from `NTLMAdminGroupName`

## 4. Example of a real token (Orange SK)

The following example shows a real OAuth2 token structure from a production Keycloak server (Orange SK). WebJET CMS automatically extracts and processes the relevant attributes:

```json
{
    "exp": 1769439584,
    "iat": 1769439284,
    "auth_time": 1769439284,
    "jti": "e684c242-b216-4ff7-8c67-b66abe80a829",
    "iss": "https://login.oct.orange.sk/auth/realms/orange",
    "sub": "91b45b5d-a09b-4eca-a8df-ca375088dcac",
    "typ": "Bearer",
    "azp": "CMS_Orange_OSK",
    "sid": "f51c29f3-6545-410b-946e-904388add257",
    "resource_access": {
        "CMS_Orange_OSK": {
            "roles": [
                "customerAdmin"
            ]
        }
    },
    "scope": "openid email profile orange",
    "identityName": "421907575887",
    "email_verified": false,
    "preferred_username": "d7e3b085-0df5-49f2-a98c-0401403c7d41",
    "orangeUserSN": "0907575887",
    "customerNumber": "0134416700",
    "orangeUser": "3543568",
    "identityId": 2955399,
    "billId": "1347513",
    "custId": 1347513,
    "orangeUserCN": "0134416700",
    "partyId": "C008ED5577FAC9E4FBB33E99586FF52BFB168831",
    "identity_type": "B2C",
    "email": "sebenova.lena@gmail.com"
}
```

### How WebJET CMS handles this token

| Attribute in token | Usage in WebJET | Note |
| -------------------------------------- | -------------------------------------------------------------- | ----------------------------------------- |
| `email`                                | User Email Identification | Mandatory, must be unique |
| `preferred_username`                   | Login (default) | In this case UUID - not suitable as login |
| `customerNumber`                       | Login (if set `oauth2_usernameAttribute=customerNumber`) | Recommended for this token type |
| `resource_access.CMS_Orange_OSK.roles` | Groups - extracted `customerAdmin`                         | Automatic |
| `given_name` / `family_name`           | First and last name | If present in the token |

### WebJET configuration for this token

```properties
oauth2_clients=keycloak

oauth2_keycloakClientId=CMS_Orange_OSK
oauth2_keycloakClientSecret=YOUR-SECRET

oauth2_keycloakAuthorizationUri=https://login.oct.orange.sk/auth/realms/orange/protocol/openid-connect/auth
oauth2_keycloakTokenUri=https://login.oct.orange.sk/auth/realms/orange/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=https://login.oct.orange.sk/auth/realms/orange/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=https://login.oct.orange.sk/auth/realms/orange/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=https://login.oct.orange.sk/auth/realms/orange

oauth2_keycloakUserNameAttributeName=email
oauth2_keycloakScopes=openid,email,profile,orange
oauth2_keycloakClientName=Orange Login

oauth2_clientsWithPermissions=keycloak

# Použiť customerNumber ako login namiesto preferred_username
oauth2_usernameAttribute=customerNumber

# Admin skupina - musí existovať vo WebJET s rovnakým názvom
NTLMAdminGroupName=customerAdmin
```

In this case:
- The user login will be `0134416700` (z `customerNumber`)
- The user will be assigned to a group `customerAdmin` (z `resource_access.CMS_Orange_OSK.roles`)
- If you are logging in through the admin area and `NTLMAdminGroupName=customerAdmin`, the user gets admin rights

## 5. Testing

### Verifying Keycloak endpoints

Before configuring WebJET, verify that Keycloak endpoints are available:

```bash
# OpenID Configuration (vypíše všetky endpointy)
curl http://localhost:18080/realms/webjet/.well-known/openid-configuration

# JWK Set URI
curl http://localhost:18080/realms/webjet/protocol/openid-connect/certs
```

### Token verification

You can use direct access grant (if enabled) to verify the token structure:

```bash
curl -X POST http://localhost:18080/realms/webjet/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=webjetcms-client" \
  -d "client_secret=YOUR-CLIENT-SECRET" \
  -d "username=testuser" \
  -d "password=test" \
  -d "scope=openid profile email"
```

Decode the resulting access token to [jwt.io](https://jwt.io) and verify the presence of the attributes (`groups`, `resource_access`, `customerNumber`, etc.).

### WebJET login testing

1. Start WebJET CMS

2. Go to the admin logon page: `http://localhost/admin/logon/`

3. Click **Keycloak Login**

4. Log in as a test user in Keycloak

5. After successful login you should be redirected to the admin interface

6. Verify in the admin interface:
    - Correct first and last name
    - Correct login (according to `oauth2_usernameAttribute`)
    - Assignment to groups
    - Admin rights (if the user has a group of `NTLMAdminGroupName`)

## 6. Common problems

### Error: "oauth2\_email\_not\_found"

- Keycloak does not return email in token
- **Solution**: Check that the user has set up email and scopes contain `email`

### Error: "accessDenied"

- User does not have admin rights after synchronizing groups
- **Solution**: Verify that:
  - `NTLMAdminGroupName` contains the correct admin group name
  - The user has this group/role in Keycloak
  - Groups are correctly mapped to the token (mapper in client scope)

### Groups do not synchronise

- Provider not listed `oauth2_clientsWithPermissions`
- **Solution**: Add provider ID to `oauth2_clientsWithPermissions=keycloak`
- Make sure the groups in Keycloak have the same names as in WebJET
- Check logs at DEBUG level for details of group extraction

### Redirect URI mismatch

- Keycloak reports error with redirect URI
- **Solution**: In the client configuration in Keycloak (Settings - Valid redirect URIs) add:
  - `http://localhost/login/oauth2/code/keycloak` (for local developments)
  - `https://your-domain.com/login/oauth2/code/keycloak` (for production)

### Login is a UUID instead of a readable name

- `preferred_username` the token contains a UUID (e.g. Keycloak internal identifier)
- **Solution**: Set `oauth2_usernameAttribute` to the appropriate attribute (e.g. `customerNumber`, `identityName`)

### Keycloak endpoints are not accessible from the WebJET server

- Token URI, UserInfo URI and JWK Set URI must be accessible server-to-server
- **Solution**: If WebJET is running in a Docker container, use `host.docker.internal` instead of `localhost` in the endpoint URI configuration
