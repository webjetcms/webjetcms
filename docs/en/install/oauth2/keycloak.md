# Keycloak - Installation and Configuration for WebJET CMS

<!-- spellcheck-off -->

This tutorial describes the complete procedure for installing a local Keycloak server via Docker and configuring it for logging into WebJET CMS, including group and rights synchronization.

## 1. Installing Keycloak

### Running via Docker Compose

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml up -d
```

Keycloak will be available at: **http://localhost:18080**

### Logging into Keycloak administration

- **URL**: http://localhost:18080
- **Username**: `admin`
- **Password**: `admin`

### Stopping Keycloak

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml down
```

!>**Note**: Keycloak data is stored in Docker volume `webjetcms-keycloakdata` and will survive container restarts.

### Ready version

For ease of use, a ready-made version is configured via the file [realm-export.json.template](../../../../.devcontainer/keycloak/realm-export.json.template), which contains a ready-made configuration for direct use. It is necessary to add an entry to the `hosts` file:

```shell
127.0.0.1   keycloak.local
```

and set `CODECEPT_DEFAULT_PASSWORD` to the password in the environment variables, which will be used to create the `admin` user but also the users for WebJET CMS `adminuser,testuser,banker`. They are assigned to groups `Admini,Bankári,Newsletter`.

```shell
CODECEPT_DEFAULT_PASSWORD=your_password_here
```

Then, just run the container that will use the JSON template. In WebJET CMS, just set the configuration variables:

```txt
oauth2_clients=keycloak
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=your_password_here set in CODECEPT_DEFAULT_PASSWORD ENV variable
```

the remaining variables are set to values ​​that match the configuration in realm-export.json.template. After restarting WebJET CMS, you will be able to test login using Keycloak. Its administration is available at http://keycloak.local:18080 (don't forget to add an entry to the hosts file). After logging into the Keycloak admin console, don't forget to switch realm to `webjetcms` in the top right.

## 2. Keycloak Configuration

### 2.1 Creating a Realm

1. Log in to the Keycloak admin console (http://localhost:18080)
2. In the top left corner, click on the "master" dropdown - **Create realm**
3. Fill in:
   - **Realm name**: `webjetcms` (or any name)
   - **Enabled**: ON
4. Click **Create**

### 2.2 Creating a client (Client)

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

#### Obtaining a Client Secret

1. Go to the **Credentials** tab in the newly created client
2. Copy the **Client secret** value - you will need it when configuring WebJET

### 2.3 Creating Groups

Groups in Keycloak map to user groups and permission groups in WebJET CMS. Group names **must match** the group names in WebJET.

1. In the left menu, click **Groups** - **Create group**
2. Create groups as needed, for example:
   - `webjet-admin` - ​​admin group (will be used to set admin rights)
   - `editors` - ​​group for content editors
   - `customerAdmin` - ​​group for customer administrators

### 2.4 Creating Roles

Keycloak recognizes two types of roles:

- **Client roles** - roles tied to a specific client (e.g. `webjetcms-client`). They appear in the token in the `resource_access.<clientId>.roles` section. Suitable if you have multiple clients and need to differentiate the rights for each of them.
- **Realm roles** - global roles for the entire realm, shared across all clients. They appear in the token in the `realm_access.roles` section. Suitable for universal roles valid across applications.

WebJET CMS automatically extracts and maps **both types of roles** to user groups and rights groups.

#### Creating Client roles

1. Go to **Clients** - select your client (`webjetcms-client`)
2. Tab **Roles** - **Create role**
3. Create roles as needed, for example:
   - **Role name**: `customerAdmin`
   - **Role name**: `editors`
4. Assign roles to users:
   - Go to **Users** - select a user
   - **Role mapping** tab - **Assign role**
   - Filter: **Filter by clients** - select roles from your client

#### Creating Realm roles

1. In the left menu, click **Realm roles** - **Create role**
2. Create roles as needed, for example:
   - **Role name**: `spravca-obsahu`
   - **Role name**: `schvalovatel`
3. Assign roles to users:
   - Go to **Users** - select a user
   - **Role mapping** tab - **Assign role**
   - Filter: **Filter by realm roles** - select the desired roles

> **Important**: Both types of roles require the creation of an appropriate mapper in the client scope (section 2.5) in order for them to appear in the ID token and UserInfo response. Without a mapper, the roles exist in Keycloak, but WebJET CMS will not see them.

### 2.5 Mapping groups, roles, and custom attributes to a token

Keycloak does not send groups, client roles, and custom attributes in the ID token or UserInfo response by default. You must create mappers within your client scope to make this data available to WebJET CMS.

> **Note**: Keycloak includes default mappers for roles (in scope `roles`), but they only add roles to the **access token**. WebJET CMS reads data from the **ID token** and the **UserInfo endpoint**, so you need to create your own mappers with the settings "Add to ID token" and "Add to userinfo".

#### Group mapping (groups)

1. Go to **Clients** - select your client - **Client scopes** tab
2. Click on `webjetcms-client-dedicated` scope (dedicated scope for your client)
3. Click **Configure a new mapper** (or **Add mapper** - **By configuration**)
4. Select **Group Membership**
5. Fill in:
   - **Name**: `groups`
   - **Token Claim Name**: `groups`
   - **Full group path**: `OFF` (if you want just the group name without the path)
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
6. Click **Save**

#### Mapping client roles

To transfer client roles (created in section 2.4) to a token:

1. In the same dedicated scope, click **Add mapper** - **By configuration**
2. Select **User Client Role**
3. Fill in:
   - **Name**: `client roles`
   - **Client ID**: select your client (e.g. `webjetcms-client`)
   - **Multivalued**: `ON`
   - **Token Claim Name**: `resource_access.webjetcms-client.roles` (replace `webjetcms-client` with your client name)
   - **Claim JSON Type**: `String`
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
4. Click **Save**

WebJET CMS automatically extracts roles from the `resource_access.{client}.roles` format and compares them to existing groups/rights groups.

#### Mapping realm roles

If you are using realm roles (created in section 2.4), you need to create a mapper to make them appear in the token:

1. In the same dedicated scope, click **Add mapper** - **By configuration**
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

> **Warning**: The **Token Claim Name** value must be exactly `realm_access.roles`. Any other value will cause WebJET CMS to not find the role.

#### Mapping custom attributes (e.g. customerNumber)

To map custom user attributes (like `customerNumber`, `custId`, etc.):

1. In the same dedicated scope, click **Add mapper** - **By configuration**
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

### 2.6 Defining custom attributes in the User Profile

For custom attributes (e.g. `customerNumber`) to be displayed for users and available in tokens, you must first define them in the User Profile:

1. In the left menu, click **Realm settings** - **User profile** tab
2. Click **Create attribute**
3. Fill in:
   - **Attribute name**: `customerNumber`
   - **Display name**: `Customer Number` (optional - will be displayed to the user)
   - **Multivalued**: `OFF`
4. In the **Permission** section, set:
   - **Can user view?**: `ON`
   - **Can user edit?**: `ON` (or `OFF` if you don't want to be able to change it)
   - **Can admin view?**: `ON`
   - **Can admin edit?**: `ON`
5. Click **Create**

Repeat for each custom attribute. Once created, the attribute will appear in the user's details on the **Details** tab.

### 2.7 Creating a test user

1. In the left menu, click **Users** - **Add user**
2. Fill in:
   - **Username**: `testuser`
   - **Email**: `testuser@example.com`
   - **Email verified**: `ON`
   - **First name**: `Test`
   - **Last name**: `User`
3. Click **Create**
4. **Credentials** - **Set password** tab:
   - **Password**: `test`
   - **Temporary**: `OFF`
5. **Groups** tab - **Join group** - add the user to the created groups (e.g. `webjet-admin`)
6. **Role mapping** tab - **Assign role** - Filter by clients - assign client role (e.g. `customerAdmin`)
7. For custom attributes: **Details** tab - scroll down, fill in the attribute value (e.g. **Customer Number**: `0134416700`) - click **Save**

## 3. WebJET CMS Configuration

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

**Important**: The variables `oauth2_keycloakTokenUri`, `oauth2_keycloakUserInfoUri` and `oauth2_keycloakJwkSetUri` must be accessible from the server where WebJET CMS is running (server-to-server communication). The variable `oauth2_keycloakAuthorizationUri` must be accessible from the user's browser.

### 3.2 Configuration for customer/user zone

The same configuration variables apply for logging into the customer zone. The difference is in the behavior of the handlers:

- **Admin zone** (`OAuth2AdminSuccessHandler`): Synchronizes user groups + permission groups + sets admin flag + requires admin rights
- **User zone** (`OAuth2UserSuccessHandler`): Synchronizes only user groups, does not set admin flag, does not require admin rights

The resolution is handled by `OAuth2DynamicSuccessHandler` based on the session attribute `oauth2_is_admin_section`, which is set by the admin logon page.

### 3.3 Configuring your own username attribute

By default, the `preferred_username` attribute from the OIDC token is used for the user login. If your Keycloak (or other OAuth2 provider) sends the login in a different attribute, you can configure it:

```properties
oauth2_usernameAttribute=customerNumber
```

**Example**: If the token contains `"customerNumber": "0134416700"`, the user's login will be set to `0134416700`.

If the configuration variable is not set, the default value `preferred_username` is used. If the given attribute does not exist in the token either, the part of the email before the at sign is used as a fallback.

### 3.4 Creating groups in WebJET CMS

For group synchronization to work, groups must exist in WebJET and their names must match the names of groups/roles in the Keycloak token.

1. Go to **Admin** - **Users** - **User Groups**
2. Create groups with **the same names** as in Keycloak:
   - `webjet-admin`
   - `editors`
   - `customerAdmin`
3. For admin rights: Also create **rights groups** (Admin - Users - Rights Groups) with the same names

WebJET CMS automatically:

- Retrieves groups from token (from `groups`, `roles`, `resource_access.*.roles`, `realm_access.roles`)
- Compare them with existing groups in WebJET by name
- Removes old assignments and sets new ones (full synchronization)
- In the admin zone, set the admin flag if oauth2 groups contain a group from `NTLMAdminGroupName`

## 4. Example of a real token (Orange SK)

The following example shows the real structure of an OAuth2 token from a production Keycloak server (Orange SK). WebJET CMS automatically extracts and processes the relevant attributes:

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

### How WebJET CMS processes this token

| Attribute in token | Use in WebJET | Note |
| --- | --- | --- |
| `email` | User email, identification | Required, must be unique |
| `preferred_username` | Login (default) | In this case UUID - unsuitable as a login |
| `customerNumber` | Login (if `oauth2_usernameAttribute=customerNumber` is set) | Recommended for this token type |
| `resource_access.CMS_Orange_OSK.roles` | Groups - `customerAdmin` is extracted | Automatically |
| `given_name` / `family_name` | Name and surname | If present in the token |

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

- User login will be `0134416700` (from `customerNumber`)
- The user will be assigned to group `customerAdmin` (from `resource_access.CMS_Orange_OSK.roles`)
- If the login is via the admin zone and `NTLMAdminGroupName=customerAdmin`, the user will receive admin rights

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

To verify the token structure, you can use direct access grant (if enabled):

```bash
curl -X POST http://localhost:18080/realms/webjet/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=webjetcms-client" \
  -d "client_secret=YOUR-CLIENT-SECRET" \
  -d "username=testuser" \
  -d "password=test" \
  -d "scope=openid profile email"
```

Decode the resulting access token at [jwt.io](https://jwt.io) and verify the presence of the attributes (`groups`, `resource_access`, `customerNumber`, etc.).

### Testing WebJET login

1. Launch WebJET CMS
2. Go to the admin logon page: `http://localhost/admin/logon/`
3. Click the **Keycloak Login** button
4. Log in as a test user in Keycloak
5. After successful login, you should be redirected to the admin interface
6. Verify in the admin interface:
   - Proper name and surname
   - Correct login (according to `oauth2_usernameAttribute`)
   - Group assignment
   - Admin rights (if the user has a group from `NTLMAdminGroupName`)

## 6. Common problems

### Error: "oauth2_email_not_found"

- Keycloak does not return email in token
- **Solution**: Check that the user has an email set and the scopes contain `email`

### Error: "accessDenied"

- User does not have admin rights after group synchronization
- **Solution**: Verify that:
  - `NTLMAdminGroupName` contains the correct admin group name
  - The user has this group/role in Keycloak
  - Groups are correctly mapped to the token (mapper in client scope)

### Groups are not syncing

- Provider is not in the list `oauth2_clientsWithPermissions`
- **Solution**: Add the provider ID to `oauth2_clientsWithPermissions=keycloak`
- Verify that the groups in Keycloak have the same names as in WebJET
- Check logs at DEBUG level for group extraction details

### Redirect URI mismatch

- Keycloak reports an error with redirect URI
- **Solution**: In the client configuration in Keycloak (Settings - Valid redirect URIs), add:
  - `http://localhost/login/oauth2/code/keycloak` (for local development)
  - `https://your-domain.com/login/oauth2/code/keycloak` (for production)

### Login is a UUID instead of a human-readable name

- `preferred_username` in the token contains a UUID (e.g. Keycloak internal identifier)
- **Solution**: Set `oauth2_usernameAttribute` to an appropriate attribute (e.g. `customerNumber`, `identityName`)

### Keycloak endpoints are not accessible from the WebJET server

- Token URI, UserInfo URI and JWK Set URI must be accessible server-to-server
- **Solution**: If WebJET is running in a Docker container, use `host.docker.internal` instead of `localhost` in the endpoint URI configuration
