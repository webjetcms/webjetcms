# Keycloak - Instalace a konfigurace pro WebJET CMS

<!-- spellcheck-off -->

Tento návod popisuje kompletní postup instalace lokálního Keycloak serveru přes Docker a jeho konfiguraci pro přihlašování do WebJET CMS včetně synchronizace skupin a práv.

## 1. Instalace Keycloak

### Spuštění přes Docker Compose

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml up -d
```

Keycloak bude dostupný na: **http://localhost:18080**

### Přihlášení do administrace Keycloak

- **URL**: http://localhost:18080
- **Username**: `admin`
- **Password**: `admin`

### Zastavení Keycloak

```bash
cd .devcontainer/db/
docker compose -f docker-compose-keycloak.yml down
```

!>**Poznámka**: Data Keycloak jsou uložena v Docker volume `webjetcms-keycloakdata` a přežijí restart kontejneru.

### Připravená verze

Pro snadné použití je připravena verze konfigurovaná přes soubor [realm-export.json.template](../../../../.devcontainer/keycloak/realm-export.json.template), který obsahuje připravenou konfiguraci pro přímé použití. Je třeba do `hosts` souboru přidat záznam:

```shell
127.0.0.1   keycloak.local
```

a do proměnných prostředí nastavit `CODECEPT_DEFAULT_PASSWORD` na heslo, které se použije pro vytvoření `admin` uživatele ale i uživatele pro WebJET CMS `adminuser,testuser,banker`. Jsou přiřazen do skupin `Admini,Bankári,Newsletter`.

```shell
CODECEPT_DEFAULT_PASSWORD=your_password_here
```

Následně stačí spustit kontejner, který použije JSON šablonu. Ve WebJET CMS stačí nastavit konfigurační proměnné:

```txt
oauth2_clients=keycloak
oauth2_keycloakClientId=webjetcms-client
oauth2_keycloakClientSecret=your_password_here set in CODECEPT_DEFAULT_PASSWORD ENV variable
```

zbývající proměnné jsou nastaveny na hodnoty, které odpovídají konfiguraci v realm-export.json.template. Po restartu WebJET CMS budete moci testovat přihlášení pomocí Keycloak. Jeho administrace je dostupná na adrese http://keycloak.local:18080 (nezapomeňte přidat záznam do hosts souboru). Po přihlášení se do admin konzole Keycloak nezapomeňte přepnout vpravo nahoře realm na `webjetcms`.

## 2. Konfigurace Keycloak

### 2.1 Vytvoření Realmu

1. Přihlaste se do Keycloak admin konzole (http://localhost:18080)

2. V levém horním rohu klikněte na dropdown "master" - **Create realm**

3. Vyplňte:
    - **Realm name**: `webjetcms` (nebo libovolný název)
    - **Enabled**: ON

4. Klikněte **Create**

### 2.2 Vytvoření klienta (Client)

1. V levém menu klikněte **Clients** - **Create client**

2. **General Settings**:
    - **Client type**: `OpenID Connect`
    - **Client ID**: `webjetcms-client` (tento ID budete používat v konfiguraci WebJET)
    - Klikněte **Next**

3. **Capability config**:
    - **Client authentication**: `ON` (změní se na "confidential" typ)
    - **Standard flow**: `ON`
    - **Direct access grants**: `OFF` (doporučeno pro produkci)
    - Klikněte **Next**

4. **Login settings**:
    - **Valid redirect URIs**: `http://localhost/login/oauth2/code/keycloak`
      - Pro produkci použijte: `https://your-domain.com/login/oauth2/code/keycloak`
      - Pro vývoj můžete nastavit: `http://localhost/login/oauth2/code/*`
    - **Valid post logout redirect URIs**: `http://localhost/*`
    - **Web origins**: `http://localhost`

5. Klikněte **Save**

#### Získání Client Secret

1. Přejděte na záložku **Credentials** v právě vytvořeném klientovi

2. Zkopírujte hodnotu **Client secret** - budete ji potřebovat při konfiguraci WebJET

### 2.3 Vytvoření skupin (Groups)

Skupiny v Keycloak se mapují na skupiny uživatelů (user groups) a skupiny práv (permission groups) ve WebJET CMS. Názvy skupin **musí být shodné** s názvy skupin ve WebJET.

1. V levém menu klikněte **Groups** - **Create group**

2. Vytvořte skupiny podle potřeby, například:
    - `webjet-admin` - admin skupina (bude použita pro nastavení admin práv)
    - `editors` - skupina pro editory obsahu
    - `customerAdmin` - skupina pro zákaznické administrátory

### 2.4 Vytvoření rolí (Roles)

Keycloak zná dva typy rolí:
- **Client roles** - role vázané na konkrétního klienta (např. `webjetcms-client`). Objevují se v tokenu v sekci `resource_access.<clientId>.roles`. Vhodné pokud máte více klientů a potřebujete odlišit práva pro každý z nich.
- **Realm roles** - globální role pro celý realm, sdílené napříč všemi klienty. Objevují se v tokenu v sekci `realm_access.roles`. Vhodné pro univerzální role platné napříč aplikacemi.

WebJET CMS automaticky extrahuje a mapuje **oba typy rolí** na skupiny uživatelů a skupiny práv.

#### Vytvoření Client roles

1. Přejděte na **Clients** - vyberte váš klient (`webjetcms-client`)

2. Záložka **Roles** - **Create role**

3. Vytvořte role podle potřeby, například:
    - **Role name**: `customerAdmin`
    - **Role name**: `editors`

4. Přiřaďte role uživatelům:
    - Přejděte na **Users** - vyberte uživatele
    - Záložka **Role mapping** - **Assign role**
    - Filtr: **Filtr by clients** - vyberte role z vašeho klienta

#### Vytvoření Realm roles

1. V levém menu klikněte **Realm roles** - **Create role**

2. Vytvořte role podle potřeby, například:
    - **Role name**: `spravca-obsahu`
    - **Role name**: `schvalovatel`

3. Přiřaďte role uživatelům:
    - Přejděte na **Users** - vyberte uživatele
    - Záložka **Role mapping** - **Assign role**
    - Filtr: **Filtr by realm roles** - vyberte požadované role

> **Důležité**: Oba typy rolí vyžadují vytvoření příslušného mapperu v client scope (sekce 2.5), aby se objevily v ID tokenu a UserInfo odpovědi. Bez mappera role sice existují v Keycloak, ale WebJET CMS je neuvidí.

### 2.5 Mapování skupin, rolí a vlastních atributů do tokenu

Keycloak standardně neposílá skupiny, klientské role a vlastní atributy v ID tokenu ani v UserInfo odpovědi. Musíte vytvořit mappers v rámci client scope vašeho klienta, aby byly tyto údaje dostupné pro WebJET CMS.

> **Poznámka**: Keycloak sice obsahuje výchozí mappery pro role (ve scope `roles`), ale ty přidávají role pouze do **access tokenu**. WebJET CMS čte údaje z **ID tokenu** a **UserInfo endpointu**, proto je třeba vytvořit vlastní mappery s nastavením "Add to ID token" a "Add to userinfo".

#### Mapování skupin (groups)

1. Přejděte na **Clients** - vyberte váš klient - záložka **Client scopes**

2. Klepněte na `webjetcms-client-dedicated` scope (dedicated scope pro vašeho klienta)

3. Klikněte **Configure a new mapper** (nebo **Add mapper** - **By configuration**)

4. Vyberte **Group Membership**

5. Vyplňte:
    - **Name**: `groups`
    - **Token Claim Name**: `groups`
    - **Full group path**: `OFF` (pokud chcete pouze název skupiny bez cesty)
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

6. Klikněte **Save**

#### Mapování klientských rolí (client roles)

Pro přenos klientských rolí (vytvořených v sekci 2.4) do tokenu:

1. Ve stejném dedicated scope klikněte **Add mapper** - **By configuration**

2. Vyberte **User Client Role**

3. Vyplňte:
    - **Name**: `client roles`
    - **Client ID**: vyberte vašeho klienta (např. `webjetcms-client`)
    - **Multivalued**: `ON`
    - **Token Claim Name**: `resource_access.webjetcms-client.roles` (nahraďte `webjetcms-client` názvem vašeho klienta)
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Klikněte **Save**

WebJET CMS automaticky extrahuje role z formátu `resource_access.{client}.roles` a porovná je se stávajícími skupinami/skupinami práv.

#### Mapování realm rolí (realm roles)

Pokud používáte realm role (vytvořené v sekci 2.4), musíte vytvořit mapper aby se objevily v tokenu:

1. Ve stejném dedicated scope klikněte **Add mapper** - **By configuration**

2. Vyberte **User Realm Role**

3. Vyplňte:
    - **Name**: `realm roles`
    - **Multivalued**: `ON`
    - **Token Claim Name**: `realm_access.roles`
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Klikněte **Save**

> **Upozornění**: Hodnota **Token Claim Name** musí být přesně `realm_access.roles`. Jakákoli jiná hodnota způsobí, že WebJET CMS role nenajde.

#### Mapování vlastních atributů (např. customerNumber)

Pro mapování vlastních uživatelských atributů (jako `customerNumber`, `custId`, atd.):

1. Ve stejném dedicated scope klikněte **Add mapper** - **By configuration**

2. Vyberte **User Attribute**

3. Vyplňte:
    - **Name**: `customerNumber`
    - **User Attribute**: `customerNumber` (název atributu v Keycloak profilu)
    - **Token Claim Name**: `customerNumber` (název v tokenu)
    - **Claim JSON Type**: `String`
    - **Add to ID token**: `ON`
    - **Add to access token**: `ON`
    - **Add to userinfo**: `ON`

4. Klikněte **Save**

Opakujte pro každý vlastní atribut, který chcete mít v tokenu.

### 2.6 Definice vlastních atributů v User Profile

Aby se vlastní atributy (např. `customerNumber`) zobrazovaly u uživatelů a byly dostupné v tokenech, musíte je nejprve definovat v User Profile:

1. V levém menu klikněte **Realm settings** - záložka **User profile**

2. Klikněte **Create attribute**

3. Vyplňte:
    - **Attribute name**: `customerNumber`
    - **Display name**: `Customer Number` (volitelné - zobrazí se u uživatele)
    - **Multivalued**: `OFF`

4. V sekci **Permission** nastavte:
    - **Can user view?**: `ON`
    - **Can user edit?**: `ON` (nebo `OFF` pokud nechcete abys to mohl měnit)
    - **Can admin view?**: `ON`
    - **Can admin edit?**: `ON`

5. Klikněte **Create**

Opakujte pro každý vlastní atribut. Po vytvoření se atribut objeví v detailu uživatele na záložce **Details**.

### 2.7 Vytvoření testovacího uživatele

1. V levém menu klikněte **Users** - **Add user**

2. Vyplňte:
    - **Username**: `testuser`
    - **Email**: `testuser@example.com`
    - **Email verified**: `ON`
    - **První název**: `Test`
    - **Last name**: `User`

3. Klikněte **Create**

4. Záložka **Credentials** - **Set password**:
    - **Password**: `test`
    - **Temporary**: `OFF`

5. Záložka **Groups** - **Join group** - přidejte uživatele do vytvořených skupin (např. `webjet-admin`)

6. Záložka **Role mapping** - **Assign role** - Filter by clients - přiřaďte client role (např. `customerAdmin`)

7. Pro vlastní atributy: záložka **Details** - srolujte dolů, vyplňte hodnotu atributu (např. **Customer Number**: `0134416700`) - klikněte **Save**

## 3. Konfigurace WebJET CMS

### 3.1 Konfigurační proměnné pro admin zónu

Nastavte následující konfigurační proměnné v WebJET CMS (Nastavení - Konfigurace):

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

**Důležité**: proměnné `oauth2_keycloakTokenUri`, `oauth2_keycloakUserInfoUri` a `oauth2_keycloakJwkSetUri` musí být přístupné ze serveru, kde běží WebJET CMS (server-to-server komunikace). Proměnná `oauth2_keycloakAuthorizationUri` musí být přístupná z prohlížeče uživatele.

### 3.2 Konfigurace pro zákaznickou/user zónu

Pro přihlašování v zákaznické zóně platí stejné konfigurační proměnné. Rozdíl je v chování handlerů:
- **Admin zóna** (`OAuth2AdminSuccessHandler`): Synchronizuje user groups + permission groups + nastavuje admin flag + vyžaduje admin práva
- **User zóna** (`OAuth2UserSuccessHandler`): Synchronizuje pouze user groups, nenastavuje admin flag, nevyžaduje admin práva

O rozlišení se stará `OAuth2DynamicSuccessHandler` na základě session atributu `oauth2_is_admin_section`, který nastavuje admin logon stránka.

### 3.3 Konfigurace vlastního username atributu

Standardně se pro login uživatele používá atribut `preferred_username` z OIDC tokenu. Pokud váš Keycloak (nebo jiný OAuth2 provider) posílá login v jiném atributu, můžete jej nakonfigurovat:

```properties
oauth2_usernameAttribute=customerNumber
```

**Příklad**: Pokud token obsahuje `"customerNumber": "0134416700"`, login uživatele bude nastaven na `0134416700`.

Pokud není konfigurační proměnná nastavena, použije se výchozí hodnota `preferred_username`. Pokud ani daný atribut v tokenu neexistuje, použije se jako fallback část emailu před zavináčem.

### 3.4 Vytvoření skupin ve WebJET CMS

Aby synchronizace skupin fungovala, musí skupiny ve WebJET existovat a jejich názvy musí odpovídat názvem skupin/rolí v Keycloak tokenu.

1. Přejděte do **Admin** - **Uživatelé** - **Skupiny uživatelů**

2. Vytvořte skupiny s **stejnými názvy** jako v Keycloak:
    - `webjet-admin`
    - `editors`
    - `customerAdmin`

3. Pro admin práva: Vytvořte také **skupiny práv** (Admin - Uživatelé - Skupiny práv) se stejnými názvy

WebJET CMS automaticky:
- Načte skupiny z tokenu (z `groups`, `roles`, `resource_access.*.roles`, `realm_access.roles`)
- Porovná je se stávajícími skupinami ve WebJET podle názvu
- Odstraní stará přiřazení a nastaví nová (úplná synchronizace)
- V admin zóně nastaví admin flag pokud oauth2 skupiny obsahují skupinu z `NTLMAdminGroupName`

## 4. Příklad reálného tokenu (Orange CS)

Následující příklad ukazuje reálnou strukturu OAuth2 tokenu z produkčního Keycloak serveru (Orange SK). WebJET CMS automaticky extrahuje a zpracuje relevantní atributy:

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

### Jak WebJET CMS zpracuje tento token

| Atribut v tokenu | Použití ve WebJET | Poznámka |
| -------------------------------------- | -------------------------------------------------------------- | ----------------------------------------- |
| `email`                                | Email uživatele, identifikace | Povinný, musí být jedinečný |
| `preferred_username`                   | Login (výchozí) | V tomto případě UUID - nevhodné jako login |
| `customerNumber`                       | Login (pokud nastaveno `oauth2_usernameAttribute=customerNumber`) | Doporučeno pro tento typ tokenu |

 | `resource_access.CMS_Orange_OSK.roles` | Skupiny - extrahuje se `customerAdmin`                         | Automaticky |
 | `given_name` / `family_name`           | Jméno a příjmení | Jsou-li přítomny v tokenu |

### Konfigurace WebJET pro tento token

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

V tomto případě:
- Login uživatele bude `0134416700` (z `customerNumber`)
- Uživatel bude přiřazen do skupiny `customerAdmin` (z `resource_access.CMS_Orange_OSK.roles`)
- Je-li přihlášení přes admin zónu a `NTLMAdminGroupName=customerAdmin`, uživatel obdrží admin práva

## 5. Testování

### Ověření Keycloak endpointů

Před konfigurací WebJET ověřte, že Keycloak endpointy jsou dostupné:

```bash
# OpenID Configuration (vypíše všetky endpointy)
curl http://localhost:18080/realms/webjet/.well-known/openid-configuration

# JWK Set URI
curl http://localhost:18080/realms/webjet/protocol/openid-connect/certs
```

### Ověření tokenu

Pro ověření struktury tokenu můžete použít direct access grant (je-li povolen):

```bash
curl -X POST http://localhost:18080/realms/webjet/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=webjetcms-client" \
  -d "client_secret=YOUR-CLIENT-SECRET" \
  -d "username=testuser" \
  -d "password=test" \
  -d "scope=openid profile email"
```

Výsledný access token dekódujte na [jwt.io](https://jwt.io) a ověřte přítomnost atributů (`groups`, `resource_access`, `customerNumber`, atd.).

### Testování přihlášení do WebJET

1. Spusťte WebJET CMS

2. Přejděte na admin logon stránku: `http://localhost/admin/logon/`

3. Klepněte na tlačítko **Keycloak Login**

4. Přihlaste se testovacím uživatelům v Keycloak

5. Po úspěšném přihlášení byste měli být přesměrováni do admin rozhraní

6. Ověřte v admin rozhraní:
    - Správné jméno a příjmení
    - Správný login (podle `oauth2_usernameAttribute`)
    - Přiřazení do skupin
    - Admin práva (pokud má uživatel skupinu z `NTLMAdminGroupName`)

## 6. Časté problémy

### Chyba: "oauth2\_email\_not\_found"

- Keycloak nevrací email v tokenu
- **Řešení**: Zkontrolujte, že uživatel má nastavený email a scopes obsahují `email`

### Chyba: "accessDenied"

- Uživatel nemá admin práva po synchronizaci skupin
- **Řešení**: Ověřte, že:
  - `NTLMAdminGroupName` obsahuje správný název admin skupiny
  - Uživatel má tuto skupinu/roli v Keycloak
  - Skupiny jsou správně namapovány do tokenu (mapper v client scope)

### Skupiny se nesynchronizují

- Provider není v seznamu `oauth2_clientsWithPermissions`
- **Řešení**: Přidejte ID providera do `oauth2_clientsWithPermissions=keycloak`
- Ověřte, že skupiny v Keycloak mají stejné názvy jako ve WebJET
- Zkontrolujte logy na úrovni DEBUG pro detaily extrakce skupin

### Redirect URI mismatch

- Keycloak hlásí chybu s redirect URI
- **Řešení**: V konfiguraci klienta v Keycloak (Settings - Valid redirect URIs) přidejte:
  - `http://localhost/login/oauth2/code/keycloak` (pro lokální vývoj)
  - `https://your-domain.com/login/oauth2/code/keycloak` (pro produkci)

### Login je UUID namísto čitelného jména

- `preferred_username` v tokenu obsahuje UUID (např. Keycloak interní identifikátor)
- **Řešení**: Nastavte `oauth2_usernameAttribute` na vhodný atribut (např. `customerNumber`, `identityName`)

### Keycloak endpointy nejsou přístupné z WebJET serveru

- Token URI, UserInfo URI a JWK Set URI musí být přístupné server-to-server
- **Řešení**: Pokud WebJET běží v Docker kontejneru, použijte `host.docker.internal` místo `localhost` v konfiguraci URI endpointů
