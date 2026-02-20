# Keycloak - Inštalácia a konfigurácia pre WebJET CMS

Tento návod popisuje kompletný postup inštalácie lokálneho Keycloak servera cez Docker a jeho konfiguráciu pre prihlasovanie do WebJET CMS vrátane synchronizácie skupín a práv.

<!-- TOC -->
- [1. Inštalácia Keycloak](#1-inštalácia-keycloak)
- [2. Konfigurácia Keycloak](#2-konfigurácia-keycloak)
  - [2.1 Vytvorenie Realmu](#21-vytvorenie-realmu)
  - [2.2 Vytvorenie klienta (Client)](#22-vytvorenie-klienta-client)
  - [2.3 Vytvorenie skupín (Groups)](#23-vytvorenie-skupín-groups)
  - [2.4 Vytvorenie rolí klienta (Client Roles)](#24-vytvorenie-rolí-klienta-client-roles)
  - [2.5 Mapovanie skupín a vlastných atribútov do tokenu](#25-mapovanie-skupín-a-vlastných-atribútov-do-tokenu)
  - [2.6 Vytvorenie testovacieho používateľa](#26-vytvorenie-testovacieho-používateľa)
- [3. Konfigurácia WebJET CMS](#3-konfigurácia-webjet-cms)
  - [3.1 Konfiguračné premenné pre admin zónu](#31-konfiguračné-premenné-pre-admin-zónu)
  - [3.2 Konfigurácia pre zákaznícku/user zónu](#32-konfigurácia-pre-zákaznícku-user-zónu)
  - [3.3 Konfigurácia vlastného username atribútu](#33-konfigurácia-vlastného-username-atribútu)
  - [3.4 Vytvorenie skupín vo WebJET CMS](#34-vytvorenie-skupín-vo-webjet-cms)
- [4. Príklad reálneho tokenu (Orange SK)](#4-príklad-reálneho-tokenu-orange-sk)
- [5. Testovanie](#5-testovanie)
- [6. Časté problémy](#6-časté-problémy)
<!-- /TOC -->

## 1. Inštalácia Keycloak

### Spustenie cez Docker Compose

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml up -d
```

Keycloak bude dostupný na: **http://localhost:18080**

### Prihlásenie do administrácie Keycloak

- **URL**: http://localhost:18080
- **Username**: `admin`
- **Password**: `admin`

### Zastavenie Keycloak

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml down
```

> **Poznámka**: Dáta Keycloak sú uložené v Docker volume `webjetcms-keycloakdata` a prežijú reštart kontajnera.

## 2. Konfigurácia Keycloak

### 2.1 Vytvorenie Realmu

1. Prihláste sa do Keycloak admin konzoly (http://localhost:18080)
2. V ľavom hornom rohu kliknite na dropdown "master" → **Create realm**
3. Vyplňte:
   - **Realm name**: `webjet` (alebo ľubovoľný názov, napr. `orange`)
   - **Enabled**: ON
4. Kliknite **Create**

### 2.2 Vytvorenie klienta (Client)

1. V ľavom menu kliknite **Clients** → **Create client**
2. **General Settings**:
   - **Client type**: `OpenID Connect`
   - **Client ID**: `webjet-client` (tento ID budete používať v konfigurácii WebJET)
   - Kliknite **Next**
3. **Capability config**:
   - **Client authentication**: `ON` (zmení sa na "confidential" typ)
   - **Standard flow**: `ON`
   - **Direct access grants**: `OFF` (odporúčané pre produkciu)
   - Kliknite **Next**
4. **Login settings**:
   - **Valid redirect URIs**: `http://localhost/login/oauth2/code/keycloak`
     - Pre produkciu použite: `https://your-domain.com/login/oauth2/code/keycloak`
     - Pre vývoj môžete nastaviť: `http://localhost/login/oauth2/code/*`
   - **Valid post logout redirect URIs**: `http://localhost/*`
   - **Web origins**: `http://localhost`
5. Kliknite **Save**

#### Získanie Client Secret

1. Prejdite na záložku **Credentials** v práve vytvorenom klientovi
2. Skopírujte hodnotu **Client secret** — budete ju potrebovať pri konfigurácii WebJET

### 2.3 Vytvorenie skupín (Groups)

Skupiny v Keycloak sa mapujú na skupiny používateľov (user groups) a skupiny práv (permission groups) vo WebJET CMS. Názvy skupín **musia byť zhodné** s názvami skupín vo WebJET.

1. V ľavom menu kliknite **Groups** → **Create group**
2. Vytvorte skupiny podľa potreby, napríklad:
   - `webjet-admin` — admin skupina (bude použitá pre nastavenie admin práv)
   - `editors` — skupina pre editorov obsahu
   - `customerAdmin` — skupina pre zákazníckych administrátorov

### 2.4 Vytvorenie rolí (Roles)

Keycloak pozná dva typy rolí:

- **Client roles** — role viazané na konkrétneho klienta (napr. `webjet-client`). Objavujú sa v tokene v sekcii `resource_access.<clientId>.roles`. Vhodné ak máte viacero klientov a potrebujete odlíšiť práva pre každý z nich.
- **Realm roles** — globálne role pre celý realm, zdieľané naprieč všetkými klientmi. Objavujú sa v tokene v sekcii `realm_access.roles`. Vhodné pre univerzálne role platné naprieč aplikáciami.

WebJET CMS automaticky extrahuje a mapuje **oba typy rolí** na skupiny používateľov a skupiny práv.

#### Vytvorenie Client roles

1. Prejdite na **Clients** → vyberte váš klient (`webjet-client`)
2. Záložka **Roles** → **Create role**
3. Vytvorte role podľa potreby, napríklad:
   - **Role name**: `customerAdmin`
   - **Role name**: `editors`
4. Priraďte role používateľom:
   - Prejdite na **Users** → vyberte používateľa
   - Záložka **Role mapping** → **Assign role**
   - Filter: **Filter by clients** → vyberte role z vášho klienta

#### Vytvorenie Realm roles

1. V ľavom menu kliknite **Realm roles** → **Create role**
2. Vytvorte role podľa potreby, napríklad:
   - **Role name**: `spravca-obsahu`
   - **Role name**: `schvalovatel`
3. Priraďte role používateľom:
   - Prejdite na **Users** → vyberte používateľa
   - Záložka **Role mapping** → **Assign role**
   - Filter: **Filter by realm roles** → vyberte požadované role

> **Dôležité**: Oba typy rolí vyžadujú vytvorenie príslušného mappera v client scope (sekcia 2.5), aby sa objavili v ID tokene a UserInfo odpovedi. Bez mappera role síce existujú v Keycloak, ale WebJET CMS ich neuvidí.

### 2.5 Mapovanie skupín, rolí a vlastných atribútov do tokenu

Keycloak štandardne neposiela skupiny, klientské role a vlastné atribúty v ID tokene ani v UserInfo odpovedi. Musíte vytvoriť mappers v rámci client scope vášho klienta, aby boli tieto údaje dostupné pre WebJET CMS.

> **Poznámka**: Keycloak síce obsahuje predvolené mappery pre role (v scope `roles`), ale tie pridávajú role iba do **access tokenu**. WebJET CMS číta údaje z **ID tokenu** a **UserInfo endpointu**, preto je potrebné vytvoriť vlastné mappery s nastavením "Add to ID token" a "Add to userinfo".

#### Mapovanie skupín (groups)

1. Prejdite na **Clients** → vyberte váš klient → záložka **Client scopes**
2. Kliknite na `webjet-client-dedicated` scope (dedicated scope pre vášho klienta)
3. Kliknite **Configure a new mapper** (alebo **Add mapper** → **By configuration**)
4. Vyberte **Group Membership**
5. Vyplňte:
   - **Name**: `groups`
   - **Token Claim Name**: `groups`
   - **Full group path**: `OFF` (ak chcete iba názov skupiny bez cesty)
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
6. Kliknite **Save**

#### Mapovanie klientských rolí (client roles)

Pre prenos klientských rolí (vytvorených v sekcii 2.4) do tokenu:

1. V rovnakom dedicated scope kliknite **Add mapper** → **By configuration**
2. Vyberte **User Client Role**
3. Vyplňte:
   - **Name**: `client roles`
   - **Client ID**: vyberte vášho klienta (napr. `webjet-client`)
   - **Multivalued**: `ON`
   - **Token Claim Name**: `resource_access.webjet-client.roles` (nahraďte `webjet-client` názvom vášho klienta)
   - **Claim JSON Type**: `String`
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
4. Kliknite **Save**

WebJET CMS automaticky extrahuje role z formátu `resource_access.{client}.roles` a porovná ich s existujúcimi skupinami/skupinami práv.

#### Mapovanie realm rolí (realm roles)

Ak používate realm role (vytvorené v sekcii 2.4), musíte vytvoriť mapper aby sa objavili v tokene:

1. V rovnakom dedicated scope kliknite **Add mapper** → **By configuration**
2. Vyberte **User Realm Role**
3. Vyplňte:
   - **Name**: `realm roles`
   - **Multivalued**: `ON`
   - **Token Claim Name**: `realm_access.roles`
   - **Claim JSON Type**: `String`
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
4. Kliknite **Save**

> **Upozornenie**: Hodnota **Token Claim Name** musí byť presne `realm_access.roles`. Akákoľvek iná hodnota spôsobí, že WebJET CMS role nenájde.

#### Mapovanie vlastných atribútov (napr. customerNumber)

Pre mapovanie vlastných používateľských atribútov (ako `customerNumber`, `custId`, atď.):

1. V rovnakom dedicated scope kliknite **Add mapper** → **By configuration**
2. Vyberte **User Attribute**
3. Vyplňte:
   - **Name**: `customerNumber`
   - **User Attribute**: `customerNumber` (názov atribútu v Keycloak profile)
   - **Token Claim Name**: `customerNumber` (názov v tokene)
   - **Claim JSON Type**: `String`
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
4. Kliknite **Save**

Opakujte pre každý vlastný atribút, ktorý chcete mať v tokene.

### 2.6 Definícia vlastných atribútov v User Profile

Aby sa vlastné atribúty (napr. `customerNumber`) zobrazovali pri používateľoch a boli dostupné v tokenoch, musíte ich najprv definovať v User Profile:

1. V ľavom menu kliknite **Realm settings** → záložka **User profile**
2. Kliknite **Create attribute**
3. Vyplňte:
   - **Attribute name**: `customerNumber`
   - **Display name**: `Customer Number` (voliteľné - zobrazí sa pri používateľovi)
   - **Multivalued**: `OFF`
4. V sekcii **Permission** nastavte:
   - **Can user view?**: `ON`
   - **Can user edit?**: `ON` (alebo `OFF` ak nechcete aby si to mohol meniť)
   - **Can admin view?**: `ON`
   - **Can admin edit?**: `ON`
5. Kliknite **Create**

Opakujte pre každý vlastný atribút. Po vytvorení sa atribút objaví v detaile používateľa na záložke **Details**.

### 2.7 Vytvorenie testovacieho používateľa

1. V ľavom menu kliknite **Users** → **Add user**
2. Vyplňte:
   - **Username**: `testuser`
   - **Email**: `testuser@example.com`
   - **Email verified**: `ON`
   - **First name**: `Test`
   - **Last name**: `User`
3. Kliknite **Create**
4. Záložka **Credentials** → **Set password**:
   - **Password**: `test`
   - **Temporary**: `OFF`
5. Záložka **Groups** → **Join group** → pridajte používateľa do vytvorených skupín (napr. `webjet-admin`)
6. Záložka **Role mapping** → **Assign role** → Filter by clients → priraďte client role (napr. `customerAdmin`)
7. Pre vlastné atribúty: záložka **Details** → zrolujte nadol, vyplňte hodnotu atribútu (napr. **Customer Number**: `0134416700`) → kliknite **Save**

## 3. Konfigurácia WebJET CMS

### 3.1 Konfiguračné premenné pre admin zónu

Nastavte nasledovné konfiguračné premenné v WebJET CMS (Nastavenia → Konfigurácia):

```properties
# Zoznam OAuth2 providerov (oddelený čiarkami)
oauth2_clients=keycloak

# Client ID a Secret z Keycloak (záložka Credentials)
oauth2_keycloakClientId=webjet-client
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

**Dôležité**: Premenné `oauth2_keycloakTokenUri`, `oauth2_keycloakUserInfoUri` a `oauth2_keycloakJwkSetUri` musia byť prístupné zo servera, kde beží WebJET CMS (server-to-server komunikácia). Premenná `oauth2_keycloakAuthorizationUri` musí byť prístupná z prehliadača používateľa.

### 3.2 Konfigurácia pre zákaznícku/user zónu

Pre prihlasovanie v zákazníckej zóne platia rovnaké konfiguračné premenné. Rozdiel je v správaní handlerov:

- **Admin zóna** (`OAuth2AdminSuccessHandler`): Synchronizuje user groups + permission groups + nastavuje admin flag + vyžaduje admin práva
- **User zóna** (`OAuth2UserSuccessHandler`): Synchronizuje iba user groups, nenastavuje admin flag, nevyžaduje admin práva

O rozlíšenie sa stará `OAuth2DynamicSuccessHandler` na základe session atribútu `oauth2_is_admin_section`, ktorý nastavuje admin logon stránka.

### 3.3 Konfigurácia vlastného username atribútu

Štandardne sa pre login používateľa používa atribút `preferred_username` z OIDC tokenu. Ak váš Keycloak (alebo iný OAuth2 provider) posiela login v inom atribúte, môžete ho nakonfigurovať:

```properties
oauth2_usernameAttribute=customerNumber
```

**Príklad**: Ak token obsahuje `"customerNumber": "0134416700"`, login používateľa bude nastavený na `0134416700`.

Ak konfiguračná premenná nie je nastavená, použije sa predvolená hodnota `preferred_username`. Ak ani daný atribút v tokene neexistuje, použije sa ako fallback časť emailu pred zavináčom.

### 3.4 Vytvorenie skupín vo WebJET CMS

Aby synchronizácia skupín fungovala, musia skupiny vo WebJET existovať a ich názvy musia zodpovedať názvom skupín/rolí v Keycloak tokene.

1. Prejdite do **Admin** → **Používatelia** → **Skupiny používateľov**
2. Vytvorte skupiny s **rovnakými názvami** ako v Keycloak:
   - `webjet-admin`
   - `editors`
   - `customerAdmin`
3. Pre admin práva: Vytvorte aj **skupiny práv** (Admin → Používatelia → Skupiny práv) s rovnakými názvami

WebJET CMS automaticky:
- Načíta skupiny z tokenu (z `groups`, `roles`, `resource_access.*.roles`, `realm_access.roles`)
- Porovná ich s existujúcimi skupinami vo WebJET podľa názvu
- Odstráni staré priradenia a nastaví nové (úplná synchronizácia)
- V admin zóne nastaví admin flag ak oauth2 skupiny obsahujú skupinu z `NTLMAdminGroupName`

## 4. Príklad reálneho tokenu (Orange SK)

Nasledujúci príklad ukazuje reálnu štruktúru OAuth2 tokenu z produkčného Keycloak servera (Orange SK). WebJET CMS automaticky extrahuje a spracuje relevantné atribúty:

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

### Ako WebJET CMS spracuje tento token

| Atribút v tokene | Použitie vo WebJET | Poznámka |
|---|---|---|
| `email` | Email používateľa, identifikácia | Povinný, musí byť jedinečný |
| `preferred_username` | Login (predvolene) | V tomto prípade UUID — nevhodné ako login |
| `customerNumber` | Login (ak nastavené `oauth2_usernameAttribute=customerNumber`) | Odporúčané pre tento typ tokenu |
| `resource_access.CMS_Orange_OSK.roles` | Skupiny — extrahuje sa `customerAdmin` | Automaticky |
| `given_name` / `family_name` | Meno a priezvisko | Ak sú prítomné v tokene |

### Konfigurácia WebJET pre tento token

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

V tomto prípade:
- Login používateľa bude `0134416700` (z `customerNumber`)
- Používateľ bude priradený do skupiny `customerAdmin` (z `resource_access.CMS_Orange_OSK.roles`)
- Ak je prihlásenie cez admin zónu a `NTLMAdminGroupName=customerAdmin`, používateľ dostane admin práva

## 5. Testovanie

### Overenie Keycloak endpointov

Pred konfiguráciou WebJET overte, že Keycloak endpointy sú dostupné:

```bash
# OpenID Configuration (vypíše všetky endpointy)
curl http://localhost:18080/realms/webjet/.well-known/openid-configuration

# JWK Set URI
curl http://localhost:18080/realms/webjet/protocol/openid-connect/certs
```

### Overenie tokenu

Pre overenie štruktúry tokenu môžete použiť direct access grant (ak je povolený):

```bash
curl -X POST http://localhost:18080/realms/webjet/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=webjet-client" \
  -d "client_secret=YOUR-CLIENT-SECRET" \
  -d "username=testuser" \
  -d "password=test" \
  -d "scope=openid profile email"
```

Výsledný access token dekódujte na [jwt.io](https://jwt.io) a overte prítomnosť atribútov (`groups`, `resource_access`, `customerNumber`, atď.).

### Testovanie prihlásenia do WebJET

1. Spustite WebJET CMS
2. Prejdite na admin logon stránku: `http://localhost/admin/logon/`
3. Kliknite na tlačidlo **Keycloak Login**
4. Prihláste sa testovacím používateľom v Keycloak
5. Po úspešnom prihlásení by ste mali byť presmerovaní do admin rozhrania
6. Overte v admin rozhraní:
   - Správne meno a priezvisko
   - Správny login (podľa `oauth2_usernameAttribute`)
   - Priradenie do skupín
   - Admin práva (ak má používateľ skupinu z `NTLMAdminGroupName`)

## 6. Časté problémy

### Chyba: "oauth2_email_not_found"

- Keycloak nevracia email v tokene
- **Riešenie**: Skontrolujte, že používateľ má nastavený email a scopes obsahujú `email`

### Chyba: "accessDenied"

- Používateľ nemá admin práva po synchronizácii skupín
- **Riešenie**: Overte, že:
  - `NTLMAdminGroupName` obsahuje správny názov admin skupiny
  - Používateľ má túto skupinu/rolu v Keycloak
  - Skupiny sú správne namapované do tokenu (mapper v client scope)

### Skupiny sa nesynchronizujú

- Provider nie je v zozname `oauth2_clientsWithPermissions`
- **Riešenie**: Pridajte ID providera do `oauth2_clientsWithPermissions=keycloak`
- Overte, že skupiny v Keycloak majú rovnaké názvy ako vo WebJET
- Skontrolujte logy na úrovni DEBUG pre detaily extrakcie skupín

### Redirect URI mismatch

- Keycloak hlási chybu s redirect URI
- **Riešenie**: V konfigurácii klienta v Keycloak (Settings → Valid redirect URIs) pridajte:
  - `http://localhost/login/oauth2/code/keycloak` (pre lokálny vývoj)
  - `https://your-domain.com/login/oauth2/code/keycloak` (pre produkciu)

### Login je UUID namiesto čitateľného mena

- `preferred_username` v tokene obsahuje UUID (napr. Keycloak interný identifikátor)
- **Riešenie**: Nastavte `oauth2_usernameAttribute` na vhodný atribút (napr. `customerNumber`, `identityName`)

### Keycloak endpointy nie sú prístupné z WebJET servera

- Token URI, UserInfo URI a JWK Set URI musia byť prístupné server-to-server
- **Riešenie**: Ak WebJET beží v Docker kontajneri, použite `host.docker.internal` namiesto `localhost` v konfigurácii URI endpointov
