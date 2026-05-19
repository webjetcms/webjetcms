# OAuth2 Autentifikace v WebJET CMS

WebJET CMS podporuje OAuth2 autentifikaci pro přihlašování uživatelů prostřednictvím externích poskytovatelů jako jsou Google, Facebook, GitHub, Keycloak a dalších. Konfigurace se čte při inicializaci WebJET CMS, po změně hodnot nebo prvotním nastavení je třeba restartovat aplikační server.

Párování uživatelů se provádí na základě emailu, který musí být jedinečný. Při prvním přihlášení přes OAuth2 se vytvoří nový uživatel v databázi WebJET s nastaveným přihlašovacím jméno, jménem, ​​příjmením a autorizací. Skupiny uživatelů a skupiny práv se synchronizují na základě shody jména. Administrátoři se nastavují automaticky na základě členství ve skupině definované v konfigurační proměnné `NTLMAdminGroupName`.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/q8xs3qDq-G4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Konfigurace

### Základní konfigurace

Pro aktivaci OAuth2 je třeba nastavit konfigurační proměnnou:

```properties
oauth2_clients=google,keycloak
```

Pokud potřebujete zobrazit různé poskytovatele v administraci nebo v zákaznické zóně můžete nastavit `oauth2_clientsIncludeAdmin` a `oauth2_clientsIncludeUser`:

```properties
oauth2_clientsIncludeAdmin=keycloak
oauth2_clientsIncludeUser=google,facebook
```

v takovém případě se pro administraci zobrazí pouze `keycloak` a pro zákaznickou zónu `google` a `facebook`. Zadaní poskytovatelé musí být nakonfigurováni v `oauth2_clients` aby se zobrazili v přihlašovací stránce. Ve výchozím nastavení je nastavena hodnota `*`, což znamená, že se zobrazí každý nakonfigurovaný poskytovatel.

### Předdefinovaní poskytovatelé

Pro populární poskytovatele stačí nastavit `clientId` a `clientSecret`. Pomocí hodnoty `oauth2_{provider}DefaultGroups` můžete definovat skupiny uživatelů, které se přidělí nově vytvořenému uživateli při prvním přihlášení přes daného poskytovatele. Hodnota je seznam ID skupin oddělen čárkou (např. `1,3`). Tyto skupiny se neaplikují pro již existující uživatele, čili po prvním přihlášení můžete uživatelům nastavit práva jak potřebujete přímo v administraci WebJET CMS a tato práva zůstanou zachována pro další přihlášení. Pokud používáte více poskytovatelů, můžete definovat různé skupiny pro každého poskytovatele abyste uměli rozlišit uživatele pro poskytovatele.

#### Google

```properties
oauth2_googleClientId=your-google-client-id
oauth2_googleClientSecret=your-google-client-secret
oauth2_googleDefaultGroups=1,2
```

Požadované hodnoty získáte přes [Google API Console](https://console.developers.google.com/) v sekci Credentials. Při registraci OAuth2 klienta u Google je nutné nastavit redirect URI ve formátu `https://your-domain.com/login/oauth2/code/google`.

#### Facebook

```properties
oauth2_facebookClientId=your-facebook-client-id
oauth2_facebookClientSecret=your-facebook-client-secret
oauth2_facebookDefaultGroups=1,3
```

Požadované hodnoty získáte přes [Facebook for Developers](https://developers.facebook.com/docs/facebook-login) v sekci My Apps. Vytvořte aplikaci, nastavte akce pro přihlášení. Při registraci OAuth2 klienta u Facebooku je nutné nastavit redirect URI ve formátu `https://your-domain.com/login/oauth2/code/facebook`. V sekci `App settings` naleznete `App ID` a `App Secret`, které použijete pro konfiguraci WebJET CMS.

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

### Jiný OAuth2 poskytovatel

Pro vlastní poskytovatele (například `Keycloak`) je třeba nastavit všechny `OAuth2` parametry. Jméno konfigurační proměnné se tvoří dynamicky z názvu poskytovatele (například `keycloak`):

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

Pokud byste použili poskytovatele s názvem `myprovider`, museli byste nastavit proměnné s prefixem `oauth2_myprovider...`. Umíte mít takto v systému nastaveno i více OAuth2 poskytovatelů současně (například `keycloak-admins`, `keycloak-users`), každý s vlastní konfigurací.

### Podporované OAuth2 atributy

WebJET CMS při OAuth2 autentifikaci extrahuje následující atributy z OAuth2/OIDC providera:

#### Základní uživatelské atributy

- `email` - ​​**Povinný** - Email uživatele (musí být jedinečný)
- `given_name` - ​​Křestní uživatelské jméno
- `family_name` - ​​Příjmení uživatele
- `preferred_username` - ​​**Výchozí** atribut pro login (standardní OIDC atribut, konfigurovatelný přes `oauth2_usernameAttribute`)
- `picture` - ​​URL adresa profilového obrázku (je-li poskytován)

#### Atributy pro skupiny a práva

Pro synchronizaci skupin (pouze pro nakonfigurované poskytovatele v `oauth2_clientsWithPermissions`):

- `groups` - ​​Jednoduchý seznam skupin
- `roles` - ​​Jednoduchý seznam rolí
- `group_membership` - ​​Alternativní název pro skupiny
- `resource_access.<client>.roles` - ​​Keycloak client-specific roles
- `realm_access.roles` - ​​Keycloak realm roles
- Spring Security authorities - Automaticky extrahováno z autentizace

**Pravidla pro extrakci přihlašovacího jména:**

1. **Přednostní atribut**: Hodnota konfigurační proměnné `oauth2_usernameAttribute` (výchozí `preferred_username`) - pokud je poskytnut OAuth2 serverem, použije se pro login
2. **Fallback**: Část emailu před zavináčem - pokud atribut není dostupný

**Příklady:**

- Keycloak poskytuje `preferred_username: "john.doe"` - ​​login bude `john.doe`
- Google neposkytuje `preferred_username` - ​​login bude část před @ z emailu (např. `john.doe` z `john.doe@gmail.com`)
- Je-li nastaveno `oauth2_usernameAttribute=sub`, použije se atribut `sub` místo `preferred_username`

Pokud při vytvoření nového uživatele existuje konflikt přihlašovacího jména (např. `john.doe` již existuje), použije se emailová adresa jako přihlašovací jméno, aby se předešlo chybě a umožnilo přihlášení. Pokud i takový účet existuje, vytvoří se nový účet `oauth2.google.XXXXXX` kde `XXXXXX` je náhodný alfanumerický řetězec.

### Konfigurace atributu pro login (username)

Název OAuth2 atributu používaného pro login uživatele je konfigurovatelný:

```properties
oauth2_usernameAttribute=preferred_username
```

Pokud proměnná není nastavena, použije se výchozí hodnota `preferred_username` (standardní OIDC atribut). Hodnota této proměnné určuje, ze kterého atributu OAuth2 tokenu se načte login. Pokud daný atribut v tokenu neexistuje, použije se jako fallback část emailu před zavináčem.

### Redirect URI

Pro všechny poskytovatele se automaticky nastavuje redirect URI:

```text
{baseUrl}/login/oauth2/code/{registrationId}
```

Příklad: `https://your-domain.com/login/oauth2/code/google`

## Synchronizace uživatelů

### Vytvoření nového uživatele

Když se uživatel přihlásí poprvé přes OAuth2:

1. **Extrakce údajů** - Z OAuth2 atributů se extrahuje email, jméno, příjmení a přihlašovací jméno (username)
2. **Vytvoření uživatele** - Vytvoří se nový uživatel v WebJET databázi
3. **Nastavení přihlašovacího jména** - Přihlašovací jméno se nastaví přednostně z atributu určeného konfigurační proměnnou `oauth2_usernameAttribute` (výchozí `preferred_username`), pokud není dostupný použije se část emailu před zavináčem
4. **Autorizace** - Uživatel se označí jako autorizovaný

Vytvoření uživatele probíhá v `AbstractOAuth2SuccessHandler.createNewUserFromOAuth2()`:

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

**Podporované atributy pro login:**

- Konfigurovatelné přes `oauth2_usernameAttribute` (výchozí `preferred_username` - standardní OIDC atribut)
- Fallback: část emailu před zavináčem

### Aktualizace stávajícího uživatele

Pro stávající uživatele se aktualizují:

- Jméno a příjmení (pokud se změnily v OAuth2 provideri)
- Login (pokud OAuth2 provider poskytuje atribut definovaný v `oauth2_usernameAttribute` / výchozí `preferred_username` a ten se změnil)
- Skupinová přiřazení (pouze pro nakonfigurované poskytovatele v `oauth2_clientsWithPermissions`)

Aktualizace probíhá v `AbstractOAuth2SuccessHandler.updateExistingUserFromOAuth2()`.

## Synchronizace skupin

OAuth2 Success Handlers obsahují specializovanou logiku pro synchronizaci skupin z nakonfigurovaných OAuth2 poskytovatelů.

### Rozdíly mezi Admin a User zónou

**Admin zóna** (`OAuth2AdminSuccessHandler`):

- Synchronizuje **user groups** (skupiny uživatelů)
- Synchronizuje **permission groups** (skupiny práv)
- Nastavuje **admin flag** na základě `NTLMAdminGroupName`
- Vyžaduje admin práva pro úspěšné přihlášení

**User zóna** (`OAuth2UserSuccessHandler`):

- Synchronizuje pouze **user groups** (skupiny uživatelů)
- **Nesynchronizuje** permission groups
- **Nenastavuje** admin flag
- Nevyžaduje admin práva

### Konfigurace synchronizace práv

Aby se synchronizovaly skupiny a práva z OAuth2 providera, je třeba nastavit konfigurační proměnnou:

```properties
oauth2_clientsWithPermissions=keycloak,okta
```

Tato proměnná definuje seznam poskytovatelů (oddělený čárkami), pro které se má provádět synchronizace skupin a práv. Pro ostatní poskytovatele (např. Google, Facebook) se synchronizace neprovádí.

Kontrola probíhá v `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`.

### Detekce poskytovatele

Provider ID se získává v `AbstractOAuth2SuccessHandler.getProviderId()`:

```java
protected String getProviderId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken) {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        return oauth2Token.getAuthorizedClientRegistrationId();
    }
    return null;
}
```

Kontrola zda se mají synchronizovat práva probíhá v `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`:

```java
protected boolean shouldSyncPermissions(String providerId) {
    if (providerId == null) return false;
    String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
    if (Tools.isEmpty(configuredProviders)) return false;
    List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
    return providers.contains(providerId);
}
```

### Extrakce skupin

Extrakce skupin probíhá v `AbstractOAuth2SuccessHandler.extractGroupsFromOAuth2()` a podporuje různé formáty:

1. **Základní atributy**: `groups`, `roles`, `group_membership`
2. **Keycloak realm_access**: `realm_access.roles`
3. **Keycloak resource_access**: `resource_access.{client}.roles`
4. **Spring Security authorities**: Automaticky extrahované role (s odstraněním prefixu `ROLE_`)

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

Pomocné metody pro extrakci:

- `extractFromAttribute()` - ​​extrahuje z jednoduchých atributů
- `extractFromResourceAccess()` - ​​extrahuje z Keycloak `resource_access`
- `extractFromRealmAccess()` - ​​extrahuje z Keycloak `realm_access`
- `extractRolesFromClientResource()` - ​​extrahuje role z konkrétního klienta
- `extractRolesFromRolesObject()` - ​​pomocná metoda pro extrakci z roles objektu

### Mapování skupin

Mapování probíhá v metodě `applyOAuth2Permissions()` (admin) nebo `applyOAuth2UserGroups()` (user):

1. **User Groups** - Hledají se skupiny uživatelů s názvem shodným s OAuth2 skupinou
2. **Permission Groups** - Hledají se skupiny práv s názvem shodným s OAuth2 skupinou (pouze admin zóna)
3. **Úplná synchronizace** - Odstraní se všechny staré skupiny a přidají se nové

**Admin zóna** (`OAuth2AdminSuccessHandler.synchronizeUserGroups()`):

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

**User zóna** (`OAuth2UserSuccessHandler.synchronizeUserGroups()`):

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

## Admin práva

### Automatické nastavení admin práv (pouze admin zóna)

Pro nakonfigurované OAuth2 poskytovatele (definované v `oauth2_clientsWithPermissions`) se admin práva nastavují automaticky v **admin zóně** na základě konfigurační proměnné:

```properties
NTLMAdminGroupName=admin
```

Pokud OAuth2 skupiny obsahují skupinu s názvem z `NTLMAdminGroupName`, uživatel se označí jako admin.

Nastavení probíhá v `OAuth2AdminSuccessHandler.applyOAuth2Permissions()`:

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

**Důležité**: Admin práva se kontrolují po synchronizaci skupin. Pokud uživatel nemá admin práva, přihlášení do admin zóny je zamítnuto s chybou `accessDenied`.

**User zóna**: `OAuth2UserSuccessHandler` **nenastavuje** admin práva - toto je určeno pouze pro admin zónu.

## Bezpečnostní aspekty

### Omezení OAuth2 procesů

1. **Konfigurovatelná synchronizace** - Synchronizace skupin a admin práv funguje pouze pro OAuth2 poskytovatele nakonfigurované v `oauth2_clientsWithPermissions`
2. **Email validace** - Každý uživatel musí mít platný email atribut
3. **Jedinečnost emailu** - Email musí být jedinečný v systému

### Zpracování chyb

OAuth2 integrace obsahuje dva typy zpracování chyb:

**1. Chyby při autentizaci** (`OAuth2DynamicErrorHandler`):

Zpracovává chyby, které nastanou během OAuth2 autentifikace (např. neplatný token, server error):

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

**Mapování chyb na kódy:**

| Výjimka | Kód chyby |
| --------- | ---------- |
| `OAuth2AuthorizationCodeRequestTypeNotSupported` | `oauth2_provider_not_configured` |
| `OAuth2AuthenticationException` s `invalid_token` /`invalid_grant` | `oauth2_invalid_token` |
| `OAuth2AuthenticationException` s `access_denied` | `oauth2_access_denied` |
| `OAuth2AuthenticationException` s `server_error` | `oauth2_server_error` |
| `AuthorizationRequestNotFoundException` | `oauth2_authorization_request_not_found` |
| `ClientAuthorizationException` | `oauth2_client_authorization_failed` |
| Ostatní | `oauth2_authentication_failed` |

**2. Chyby v Success Handlers** (`handleError`):

Společná metoda v `AbstractOAuth2SuccessHandler` využívající `OAuth2LoginHelper`:

```java
// Pomocná metóda pre spracovanie OAuth2 chýb
protected void handleError(HttpServletRequest request, HttpServletResponse response,
                          String errorCode, String redirectUrl) throws IOException {
    HttpSession session = request.getSession();
    session.setAttribute("oauth2_logon_error", errorCode);
    response.sendRedirect(redirectUrl);
}
```

**Admin zóna** (`OAuth2AdminSuccessHandler`):

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

**User zóna** (`OAuth2UserSuccessHandler`):

```java
// Chyby sa uložia do session a používateľ je presmerovaný na /
handleError(request, response, "oauth2_email_not_found", "/");
handleError(request, response, "oauth2_user_create_failed", "/");
handleError(request, response, "oauth2_exception", "/");
```

### Zpracování chyb v AdminLogonController

AdminLogonController čte chyby ze session a zobrazuje příslušné chybové hlášky:

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

**Typy chyb:**

- `oauth2_email_not_found` - ​​OAuth2 provider nevrátil email atribut
- `oauth2_user_create_failed` - ​​Selhalo vytvoření nového uživatele v databázi
- `oauth2_exception` - ​​Všeobecná chyba během OAuth2 procesu
- `accessDenied` - ​​Uživatel nemá admin práva po synchronizaci práv

### Překladové klíče

Pro správné zobrazení chybových hlášek je třeba mít definovány následující překladové klíče v `text.properties` souborech:

```properties
logon.err.noadmin=Zadaný používateľ nie je administrátor systému
logon.err.oauth2_email_not_found=OAuth2 prihlásenie zlyhalo: email sa nepodarilo získať
logon.err.oauth2_user_create_failed=OAuth2 prihlásenie zlyhalo: nepodarilo sa vytvoriť používateľa
logon.err.oauth2_exception=OAuth2 prihlásenie zlyhalo: vyskytla sa neočakávaná chyba
logon.err.oauth2_unknown=OAuth2 prihlásenie zlyhalo: neznáma chyba
```

## OAuth2 Login Helper

`OAuth2LoginHelper` je pomocná třída se sdílenou funkcionalitou pro OAuth2 zpracování. Poskytuje:

### Detekce typu přihlášení

```java
// Zistí, či ide o admin prihlásenie (na základe session atribútu)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLogin(request);

// Zistí typ prihlásenia a odstráni session atribút (jednorazové použitie)
boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);
```

### Zpracování chyb

```java
// Nastaví chybu do session a presmeruje podľa typu prihlásenia
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", isAdminLogin);

// Alebo s explicitnou redirect URL
OAuth2LoginHelper.handleError(request, response, "oauth2_error_code", "/custom/redirect/");
```

### Redirect URL konstanty

```java
OAuth2LoginHelper.getAdminRedirectUrl();  // "/admin/logon/"
OAuth2LoginHelper.getUserRedirectUrl();   // "/"
OAuth2LoginHelper.getErrorRedirectUrl(isAdminLogin);  // Podľa typu
```

## OAuth2 Dynamic Success Handler

### Princip fungování

`OAuth2DynamicSuccessHandler` rozhoduje, který handler použít na základě session atributu `oauth2_is_admin_section` pomocí `OAuth2LoginHelper`:

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

### Nastavení session atributu

**Admin zóna**: `AdminLogonController.showLogonForm()` nastavuje:

```java
session.setAttribute("oauth2_is_admin_section", true);
```

**User zóna**: Logon controller pro zákaznickou zónu nastavuje `false` nebo nenastavuje nic (default je user handler).

## Přihlašovací stránka

### Zobrazení OAuth2 tlačítek

OAuth2 tlačítka se zobrazují automaticky pokud je nastavena konfigurační proměnná `oauth2_clients`:

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

Pokud je nastavena konfigurační proměnná `oauth2_adminLogonAutoRedirect` na název poskytovatele (např. `Keycloak` nebo `Google`), přihlašovací stránka do administrace automaticky přesměruje na daného OAuth2 poskytovatele. Nezobrazí se tedy ani standardní přihlašovací formulář WebJET CMS.

### Generování OAuth2 URL

`AdminLogonController` automaticky generuje OAuth2 URL pro všechny nakonfigurované poskytovatele:

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

## Diagnostika a ladění

### Logování

OAuth2 Success Handlers obsahují podrobné logování pro diagnostiku:

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

### Časté problémy

1. **Chybějící email** - OAuth2 provider nevrací email atribut
   - Zkontrolujte scopes: `openid,profile,email`
   - Chyba: `oauth2_email_not_found`

2. **Neplatná konfigurace** - Chybné OAuth2 endpoint URL
   - Zkontrolujte všechny URI v konfiguraci
   - Pro vlastní poskytovatele musí být nastaveny všechny parametry

3. **Skupiny se nesynchronizují** - Provider není nakonfigurován v `oauth2_clientsWithPermissions` nebo chybné názvy skupin
   - Zkontrolujte `oauth2_clientsWithPermissions` konfiguraci
   - Ověřte názvy skupin v WebJET vs OAuth2 provider
   - Podívejte logy na úrovni DEBUG pro detaily extrakce skupin

4. **Redirect URI** - Nesprávně nastavený redirect URI u poskytovatele
   - Formát: `{baseUrl}/login/oauth2/code/{registrationId}`
   - Příklad: `https://your-domain.com/login/oauth2/code/keycloak`

5. **Admin zóna vs User zóna** - Přihlášení směřuje na nesprávný handler
   - Zkontrolujte nastavení `oauth2_is_admin_section` session atributu
   - Admin logon stránka musí nastavit tento atribut

6. **Chybějící admin práva** - Uživatel nemůže přistoupit do admin zóny
   - Zkontrolujte `NTLMAdminGroupName` konfiguraci
   - Ověřte přítomnost admin skupiny v OAuth2 groups
   - Chyba: `accessDenied`

## Příklady konfigurace

### Keycloak s WebJET

**Vytvoření klienta v Keycloak:**

- Client ID: `webjetcms-client`
- Client Protocol: `openid-connect`
- Access Type: `confidential`
- Valid Redirect URIs: `https://your-domain.com/login/oauth2/code/keycloak`
- Standard Flow Enabled: `ON`
- Direct Access Grants Enabled: `OFF` (doporučeno)

**Konfigurace v WebJET:**

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

**Skupiny v Keycloak:**

- Vytvořte skupiny s názvy shodnými s WebJET skupinami
- Přiřaďte uživatele do příslušných skupin
- Admin skupina: `webjet-admin` (podle NTLMAdminGroupName)
- Mapujte skupiny na token (Client Scopes - dedicated scope - Add mapper - Group Membership)

**Testování:**

- Přihlaste se přes OAuth2 na admin stránce: `https://your-domain.com/admin/logon/`
- Ověřte synchronizaci skupin v admin rozhraní WebJET
- Zkontrolujte logy pro detailní informace o synchronizaci

### Google OAuth2

```properties
oauth2_clients=google
oauth2_googleClientId=your-google-client-id.apps.googleusercontent.com
oauth2_googleClientSecret=your-google-client-secret
```

**Poznámka**: Pro Google OAuth2 se **nesynchronizují** skupiny, protože Google není v `oauth2_clientsWithPermissions`. Pro synchronizaci skupin je třeba použít poskytovatele jako Keycloak nebo Okta.

### OAuth2 pro zákaznickou zónu (User logon)

Pro implementaci OAuth2 přihlášení v zákaznické zóně:

**Konfigurace controlleru pro user logon:**

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

**Rozdíl oproti admin zóně:**

- **Nenastavuje** `oauth2_is_admin_section` session atribut (nebo nastavuje na `false`)
- Používá `afterLogonRedirect` místo `adminAfterLogonRedirect`
- OAuth2UserSuccessHandler:
  - Synchronizuje pouze user groups
  - Nenastavuje permission groups
  - Nenastavuje admin flag
  - Nevyžaduje admin práva

## Přehled implementace

OAuth2 integrace je implementována pomocí Spring Security OAuth2 modulu a obsahuje:

- **Spring Security konfiguraci** - Konfigurace OAuth2 klientů a endpoints
- **OAuth2DynamicSuccessHandler** - Dynamické rozhodování mezi admin a user zpracovatelem po úspěšné autentifikaci
- **OAuth2DynamicErrorHandler** - Zpracování chyb při OAuth2 autentifikaci s přesměrováním na správnou logon stránku
- **OAuth2LoginHelper** - Pomocná třída se sdílenou funkcionalitou pro OAuth2 zpracování (detekce typu přihlášení, zpracování chyb)
- **Dva typy Success Handlers**:
  - **OAuth2AdminSuccessHandler** - Pro admin zónu (synchronizuje user groups + permission groups + admin flag)
  - **OAuth2UserSuccessHandler** - Pro user zónu (synchronizuje pouze user groups)
- **AbstractOAuth2SuccessHandler** - Společná base třída s funkcionalitou pro oba zpracovatele
- **Automatická synchronizace skupin** - Mapování skupin z OAuth2 providera na WebJET skupiny (pouze pro nakonfigurované poskytovatele)
- **Admin práva z OAuth2** - Automatické nastavení admin práv na základě skupin (pouze admin zóna)

## Architektura

### Hlavní komponenty

1. [SpringSecurityConf](../../../../src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java)

   - Konfigurace OAuth2 klientů
   - Registrace OAuth2DynamicSuccessHandler a OAuth2DynamicErrorHandler
   - Konfigurace authorized client service

2. [OAuth2LoginHelper](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2LoginHelper.java)

   - Pomocná třída se sdílenou funkcionalitou pro OAuth2 zpracování
   - Detekce typu přihlášení (admin/user)
   - Zpracování chyb a přesměrování

3. [OAuth2DynamicSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicSuccessHandler.java)

   - Dynamické rozhodování mezi admin a user zpracovatelem
   - Používá OAuth2LoginHelper pro rozlišení typu přihlášení

4. [OAuth2DynamicErrorHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2DynamicErrorHandler.java)

   - Zpracování chyb při OAuth2 autentifikaci
   - Mapování výjimek na chybové kódy
   - Přesměrování na správnou logon stránku (admin/user)

5. [AbstractOAuth2SuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/AbstractOAuth2SuccessHandler.java)

   - Abstraktní base třída pro OAuth2 Success Handlers
   - Společná funkcionalita pro extrakci skupin a atributů z OAuth2
   - Zpracování chyb přes session

6. [OAuth2AdminSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2AdminSuccessHandler.java)

   - Zpracování úspěšné OAuth2 autentifikace pro **admin zónu**
   - Vytvoření nebo aktualizace uživatele
   - Synchronizace skupin a práv (user groups + permission groups)
   - Nastavení admin práv

7. [OAuth2UserSuccessHandler](../../../../src/main/java/sk/iway/iwcm/system/spring/oauth2/OAuth2UserSuccessHandler.java)

   - Zpracování úspěšné OAuth2 autentifikace pro **zákaznickou zónu**
   - Synchronizace pouze user groups (ne permission groups)
   - Nenastavuje admin práva

8. [AdminLogonController](../../../../src/main/java/sk/iway/iwcm/logon/AdminLogonController.java)

   - Zobrazení OAuth2 přihlašovacích odkazů na logon stránce
   - Generování URL pro OAuth2 poskytovatelů
   - Zpracování OAuth2 chyb ze session
   - Nastavení `oauth2_is_admin_section` atributu

9. [Logon Template](../../../../src/main/webapp/admin/skins/webjet8/logon-spring.jsp)
   - Zobrazení OAuth2 přihlašovacích tlačítek

## API reference

### OAuth2LoginHelper

Pomocná třída se sdílenou funkcionalitou:

- `isAdminLogin(request)` - ​​Zjistí, zda se jedná o admin přihlášení na základě session atributu
- `isAdminLoginAndClear(request)` - ​​Zjistí typ přihlášení a odstraní session atribut
- `getErrorRedirectUrl(isAdminLogin)` - ​​Vrátí redirect URL pro chybu podle typu přihlášení
- `getAdminRedirectUrl()` - ​​Vrátí redirect URL pro admin zónu (`/admin/logon/`)
- `getUserRedirectUrl()` - ​​Vrátí redirect URL pro user zónu (`/`)
- `handleError(request, response, errorCode, redirectUrl)` - ​​Nastaví chybu do session a přesměruje
- `handleError(request, response, errorCode, isAdminLogin)` - ​​Nastaví chybu a přesměruje podle typu

### OAuth2DynamicErrorHandler

Handler pro chyby při OAuth2 autentifikaci:

- `onAuthenticationFailure()` - ​​Zpracuje chybu, zjistí typ přihlášení a přesměruje na správnou logon stránku
- `getErrorCodeFromException()` - ​​Mapuje výjimky na chybové kódy pro překladovou tabulku

### OAuth2DynamicSuccessHandler

Dynamický router pro OAuth2 autentifikaci:

- `onAuthenticationSuccess()` - ​​Rozhoduje mezi admin a user zpracovatelem pomocí `OAuth2LoginHelper.isAdminLoginAndClear()`

### AbstractOAuth2SuccessHandler

Abstraktní base třída se společnou funkcionalitou:

- `onAuthenticationSuccess()` - ​​Abstraktní metoda implementovaná v potomcích
- `getUsernameAttribute()` - ​​Vrátí název atributu pro username z konfigurace (`oauth2_usernameAttribute`), nebo výchozí hodnotu `preferred_username`
- `createNewUserFromOAuth2()` - ​​Vytvoření nového uživatele z OAuth2 dat
- `updateExistingUserFromOAuth2()` - ​​Aktualizace stávajícího uživatele
- `getProviderId()` - ​​Zjištění ID OAuth2 providera z autentizace
- `shouldSyncPermissions()` - ​​Kontrola, zda má provider nakonfigurovanou synchronizaci práv
- `extractGroupsFromOAuth2()` - ​​Extrakce skupin z OAuth2 atributů (všechny formáty)
- `extractFromAttribute()` - ​​Extrakce z jednoduchých atributů
- `extractFromResourceAccess()` - ​​Extrakce z Keycloak `resource_access`
- `extractFromRealmAccess()` - ​​Extrakce z Keycloak `realm_access`
- `extractRolesFromClientResource()` - ​​Extrakce rolí z konkrétního klienta
- `extractRolesFromRolesObject()` - ​​Pomocná metoda pro extrakci z roles objektu
- `handleError()` - ​​Pomocná metoda pro zpracování OAuth2 chyb přes session

### OAuth2AdminSuccessHandler (Admin zóna)

Handler pro admin zónu:

- `onAuthenticationSuccess()` - ​​Hlavní entry point po úspěšné autentifikaci
- `applyOAuth2Permissions()` - ​​Aplikování práv z OAuth2 (user groups + permission groups + admin flag)
- `removeAllGroupAssignments()` - ​​Odstranění ze všech skupin (user + permission)
- `synchronizeUserGroups()` - ​​Synchronizace user groups a permission groups

### OAuth2UserSuccessHandler (User zóna)

Handler pro zákaznickou zónu:

- `onAuthenticationSuccess()` - ​​Hlavní entry point po úspěšné autentifikaci
- `applyOAuth2UserGroups()` - ​​Aplikování pouze user groups (ne permission groups, ne admin flag)
- `removeAllGroupAssignments()` - ​​Odstranění ze všech user groups
- `synchronizeUserGroups()` - ​​Synchronizace pouze user groups

### SpringSecurityConf

Metody pro OAuth2 konfiguraci:

- `filterChain()` - ​​Registruje `OAuth2DynamicSuccessHandler` jako success handler
- `clientRegistrationRepository()` - ​​Bean pro registraci OAuth2 klientů (vrací prázdnou implementaci pokud nejsou klienti)
- `buildClientRegistration()` - ​​Vytvoření konfigurace pro konkrétního providera (podporuje Google, Facebook, GitHub, Okta + custom)
- `authorizedClientService()` - ​​Bean pro správu autorizovaných klientů

### AdminLogonController

Metody pro OAuth2 admin přihlášení:

- `showLogonForm()` - ​​Nastavuje session atribut `oauth2_is_admin_section` a generuje OAuth2 URL
- Zpracovává OAuth2 chyby ze session a zobrazuje příslušné chybové hlášky
