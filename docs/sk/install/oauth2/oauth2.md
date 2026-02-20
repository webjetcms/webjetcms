# OAuth2 Autentifikácia v WebJET CMS

WebJET CMS podporuje OAuth2 autentifikáciu pre prihlasovanie používateľov prostredníctvom externých poskytovateľov ako sú Google, Facebook, GitHub, Keycloak a ďalších. Konfigurácia sa číta pri inicializácii WebJET CMS, po zmene hodnôt alebo prvotnom nastavení je potrebné reštartovať aplikačný server.

## Konfigurácia

### Základná konfigurácia

Pre aktiváciu OAuth2 je potrebné nastaviť konfiguračnú premennú:

```properties
oauth2_clients=google,keycloak
```

### Preddefinovaní poskytovatelia

Pre populárnych poskytovateľov stačí nastaviť `clientId` a `clientSecret`:

#### Google

```properties
oauth2_googleClientId=your-google-client-id
oauth2_googleClientSecret=your-google-client-secret
```

#### Facebook

```properties
oauth2_facebookClientId=your-facebook-client-id
oauth2_facebookClientSecret=your-facebook-client-secret
```

#### GitHub

```properties
oauth2_githubClientId=your-github-client-id
oauth2_githubClientSecret=your-github-client-secret
```

#### Okta

```properties
oauth2_oktaClientId=your-okta-client-id
oauth2_oktaClientSecret=your-okta-client-secret
```

### Keycloak

Pre vlastných poskytovateľov (napríklad Keycloak) je potrebné nastaviť všetky `OAuth2` parametre:

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

### Podporované OAuth2 atribúty

WebJET CMS pri OAuth2 autentifikácii extrahuje nasledujúce atribúty z OAuth2/OIDC providera:

#### Základné používateľské atribúty

- `email` - **Povinný** - Email používateľa (musí byť jedinečný)
- `given_name` - Krstné meno používateľa
- `family_name` - Priezvisko používateľa
- `preferred_username` - **Predvolený** atribút pre login (štandardný OIDC atribút, konfigurovateľný cez `oauth2_usernameAttribute`)

#### Atribúty pre skupiny a práva

Pre synchronizáciu skupín (iba pre nakonfigurovaných providerov v `oauth2_clientsWithPermissions`):

- `groups` - Jednoduchý zoznam skupín
- `roles` - Jednoduchý zoznam rolí
- `group_membership` - Alternatívny názov pre skupiny
- `resource_access.<client>.roles` - Keycloak client-specific roles
- `realm_access.roles` - Keycloak realm roles
- Spring Security authorities - Automaticky extrahované z autentifikácie

**Pravidlá pre extrakciu loginu:**

1. **Prednostný atribút**: Hodnota konfiguračnej premennej `oauth2_usernameAttribute` (predvolene `preferred_username`) - ak je poskytnutý OAuth2 providerom, použije sa pre login
2. **Fallback**: Časť emailu pred zavináčom - ak atribút nie je dostupný

**Príklady:**

- Keycloak poskytuje `preferred_username: "john.doe"` → login bude `john.doe`
- Google neposkytuje `preferred_username` → login bude časť pred @ z emailu (napr. `john.doe` z `john.doe@gmail.com`)
- Ak je nastavené `oauth2_usernameAttribute=sub`, použije sa atribút `sub` namiesto `preferred_username`

### Konfigurácia atribútu pre login (username)

Názov OAuth2 atribútu používaného pre login používateľa je konfigurovateľný:

```properties
oauth2_usernameAttribute=preferred_username
```

Ak premenná nie je nastavená, použije sa predvolená hodnota `preferred_username` (štandardný OIDC atribút). Hodnota tejto premennej určuje, z ktorého atribútu OAuth2 tokenu sa načíta login. Ak daný atribút v tokene neexistuje, použije sa ako fallback časť emailu pred zavináčom.

### Redirect URI

Pre všetkých poskytovateľov sa automaticky nastavuje redirect URI:

```text
{baseUrl}/login/oauth2/code/{registrationId}
```

Príklad: `https://your-webjet-domain.com/login/oauth2/code/google`

## Synchronizácia používateľov

### Vytvorenie nového používateľa

Keď sa používateľ prihlási prvýkrát cez OAuth2:

1. **Extrakcia údajov** - Z OAuth2 atribútov sa extrahuje email, meno, priezvisko a login (username)
2. **Vytvorenie používateľa** - Vytvorí sa nový používateľ v WebJET databáze
3. **Nastavenie loginu** - Login sa nastaví prednostne z atribútu určeného konfiguračnou premennou `oauth2_usernameAttribute` (predvolene `preferred_username`), ak nie je dostupný použije sa časť emailu pred zavináčom
4. **Autorizácia** - Používateľ sa označí ako autorizovaný

Vytvorenie používateľa prebieha v `AbstractOAuth2SuccessHandler.createNewUserFromOAuth2()`:

```java
UserDetails userDetails = new UserDetails();
userDetails.setEmail(email);
userDetails.setFirstName(givenName);
userDetails.setLastName(familyName);

// Nastav login - prednostne z username atribútu, inak použij email pred zavináčom
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

**Podporované atribúty pre login:**

- Konfigurovateľné cez `oauth2_usernameAttribute` (predvolene `preferred_username` - štandardný OIDC atribút)
- Fallback: časť emailu pred zavináčom

### Aktualizácia existujúceho používateľa

Pre existujúcich používateľov sa aktualizujú:

- Meno a priezvisko (ak sa zmenili v OAuth2 provideri)
- Login (ak OAuth2 provider poskytuje atribút definovaný v `oauth2_usernameAttribute` / predvolene `preferred_username` a ten sa zmenil)
- Skupinové priradenia (iba pre nakonfigurovaných providerov v `oauth2_clientsWithPermissions`)

Aktualizácia prebieha v `AbstractOAuth2SuccessHandler.updateExistingUserFromOAuth2()`.

## Synchronizácia skupín

OAuth2 Success Handlers obsahujú špecializovanú logiku pre synchronizáciu skupín z nakonfigurovaných OAuth2 providerov.

### Rozdiely medzi Admin a User zónou

**Admin zóna** (`OAuth2AdminSuccessHandler`):

- Synchronizuje **user groups** (skupiny používateľov)
- Synchronizuje **permission groups** (skupiny práv)
- Nastavuje **admin flag** na základe `NTLMAdminGroupName`
- Vyžaduje admin práva pre úspešné prihlásenie

**User zóna** (`OAuth2UserSuccessHandler`):

- Synchronizuje iba **user groups** (skupiny používateľov)
- **Nesynchronizuje** permission groups
- **Nenastavuje** admin flag
- Nevyžaduje admin práva

### Konfigurácia synchronizácie práv

Aby sa synchronizovali skupiny a práva z OAuth2 providera, je potrebné nastaviť konfiguračnú premennú:

```properties
oauth2_clientsWithPermissions=keycloak,okta
```

Táto premenná definuje zoznam providerov (oddelený čiarkami), pre ktorých sa má vykonávať synchronizácia skupín a práv. Pre ostatných providerov (napr. Google, Facebook) sa synchronizácia nevykonáva.

Kontrola prebieha v `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`.

### Detekcia providera

Provider ID sa získava v `AbstractOAuth2SuccessHandler.getProviderId()`:

```java
protected String getProviderId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken) {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        return oauth2Token.getAuthorizedClientRegistrationId();
    }
    return null;
}
```

Kontrola či sa majú synchronizovať práva prebieha v `AbstractOAuth2SuccessHandler.shouldSyncPermissions()`:

```java
protected boolean shouldSyncPermissions(String providerId) {
    if (providerId == null) return false;
    String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
    if (Tools.isEmpty(configuredProviders)) return false;
    List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
    return providers.contains(providerId);
}
```

### Extrakcia skupín

Extrakcia skupín prebieha v `AbstractOAuth2SuccessHandler.extractGroupsFromOAuth2()` a podporuje rôzne formáty:

1. **Základné atribúty**: `groups`, `roles`, `group_membership`
2. **Keycloak realm_access**: `realm_access.roles`
3. **Keycloak resource_access**: `resource_access.{client}.roles`
4. **Spring Security authorities**: Automaticky extrahované role (s odstránením prefixu `ROLE_`)

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

Pomocné metódy pre extrakciu:

- `extractFromAttribute()` - extrahuje z jednoduchých atribútov
- `extractFromResourceAccess()` - extrahuje z Keycloak `resource_access`
- `extractFromRealmAccess()` - extrahuje z Keycloak `realm_access`
- `extractRolesFromClientResource()` - extrahuje role z konkrétneho klienta
- `extractRolesFromRolesObject()` - pomocná metóda pre extrakciu z roles objektu

### Mapovanie skupín

Mapovanie prebieha v metóde `applyOAuth2Permissions()` (admin) alebo `applyOAuth2UserGroups()` (user):

1. **User Groups** - Hľadajú sa skupiny používateľov s názvom zhodným s OAuth2 skupinou
2. **Permission Groups** - Hľadajú sa skupiny práv s názvom zhodným s OAuth2 skupinou (iba admin zóna)
3. **Úplná synchronizácia** - Odstránia sa všetky staré skupiny a pridajú sa nové

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

### Automatické nastavenie admin práv (iba admin zóna)

Pre nakonfigurovaných OAuth2 providerov (definovaných v `oauth2_clientsWithPermissions`) sa admin práva nastavujú automaticky v **admin zóne** na základe konfiguračnej premennej:

```properties
NTLMAdminGroupName=admin
```

Ak OAuth2 skupiny obsahujú skupinu s názvom z `NTLMAdminGroupName`, používateľ sa označí ako admin.

Nastavenie prebieha v `OAuth2AdminSuccessHandler.applyOAuth2Permissions()`:

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

**Dôležité**: Admin práva sa kontrolujú po synchronizácii skupín. Ak používateľ nemá admin práva, prihlásenie do admin zóny je zamietnuté s chybou `accessDenied`.

**User zóna**: `OAuth2UserSuccessHandler` **nenastavuje** admin práva - toto je určené iba pre admin zónu.

## Bezpečnostné aspekty

### Obmedzenia OAuth2 procesov

1. **Konfigurovateľná synchronizácia** - Synchronizácia skupín a admin práv funguje iba pre OAuth2 providerov nakonfigurovaných v `oauth2_clientsWithPermissions`
2. **Email validácia** - Každý používateľ musí mať validný email atribút
3. **Jedinečnosť emailu** - Email musí byť jedinečný v systéme

### Error handling

OAuth2 Success Handlers obsahujú rozsiahle error handling s ukladaním chýb do session a presmerovaním na logon stránku.

Spoločná metóda v `AbstractOAuth2SuccessHandler`:

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

### Spracovanie chýb v AdminLogonController

AdminLogonController číta chyby zo session a zobrazuje príslušné chybové hlášky:

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

**Typy chýb:**

- `oauth2_email_not_found` - OAuth2 provider nevrátil email atribút
- `oauth2_user_create_failed` - Zlyhalo vytvorenie nového používateľa v databáze
- `oauth2_exception` - Všeobecná chyba počas OAuth2 procesu
- `accessDenied` - Používateľ nemá admin práva po synchronizácii práv

### Prekladové kľúče

Pre správne zobrazenie chybových hlášok je potrebné mať definované nasledujúce prekladové kľúče v `text.properties` súboroch:

```properties
logon.err.noadmin=Zadaný používateľ nie je administrátor systému
logon.err.oauth2_email_not_found=OAuth2 prihlásenie zlyhalo: email sa nepodarilo získať
logon.err.oauth2_user_create_failed=OAuth2 prihlásenie zlyhalo: nepodarilo sa vytvoriť používateľa
logon.err.oauth2_exception=OAuth2 prihlásenie zlyhalo: vyskytla sa neočakávaná chyba
logon.err.oauth2_unknown=OAuth2 prihlásenie zlyhalo: neznáma chyba
```

## OAuth2 Dynamic Success Handler

### Princíp fungovania

`OAuth2DynamicSuccessHandler` rozhoduje, ktorý handler použiť na základe session atribútu `oauth2_is_admin_section`:

```java
HttpSession session = request.getSession(false);
boolean isAdminLogin = false;

if (session != null) {
    Boolean isAdminSection = (Boolean) session.getAttribute("oauth2_is_admin_section");
    isAdminLogin = (isAdminSection != null && isAdminSection);
    // Po použití odstráň atribút
    session.removeAttribute("oauth2_is_admin_section");
}

if (isAdminLogin) {
    adminHandler.onAuthenticationSuccess(request, response, authentication);
} else {
    userHandler.onAuthenticationSuccess(request, response, authentication);
}
```

### Nastavenie session atribútu

**Admin zóna**: `AdminLogonController.showLogonForm()` nastavuje:

```java
session.setAttribute("oauth2_is_admin_section", true);
```

**User zóna**: Logon controller pre zákaznícku zónu nastavuje `false` alebo nenastavuje nič (default je user handler).

## Logon stránka

### Zobrazenie OAuth2 tlačidiel

OAuth2 tlačidlá sa zobrazujú automaticky ak je nastavená konfiguračná premenná `oauth2_clients`:

```jsp
<c:if test="${isOAuth2Enabled}">
    <c:forEach var="url" items="${urls}">
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

### Generovanie OAuth2 URL

`AdminLogonController` automaticky generuje OAuth2 URL pre všetkých nakonfigurovaných poskytovateľov:

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
    model.addAttribute("urls", oauth2AuthenticationUrls);
}
```

## Diagnostika a ladenie

### Logovanie

OAuth2 Success Handlers obsahujú podrobné logovanie pre diagnostiku:

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

1. **Chýbajúci email** - OAuth2 provider nevracia email atribút
   - Skontrolujte scopes: `openid,profile,email`
   - Chyba: `oauth2_email_not_found`

2. **Neplatná konfigurácia** - Chybné OAuth2 endpoint URL
   - Skontrolujte všetky URI v konfigurácii
   - Pre custom providerov musia byť nastavené všetky parametre

3. **Skupiny sa nesynchronizujú** - Provider nie je nakonfigurovaný v `oauth2_clientsWithPermissions` alebo chybné názvy skupín
   - Skontrolujte `oauth2_clientsWithPermissions` konfiguráciu
   - Overťe názvy skupín v WebJET vs OAuth2 provider
   - Pozrite logy na úrovni DEBUG pre detaily extrakcie skupín

4. **Redirect URI** - Nesprávne nastavený redirect URI u poskytovateľa
   - Formát: `{baseUrl}/login/oauth2/code/{registrationId}`
   - Príklad: `https://your-domain.com/login/oauth2/code/keycloak`

5. **Admin zóna vs User zóna** - Prihlásenie smeruje na nesprávny handler
   - Skontrolujte nastavenie `oauth2_is_admin_section` session atribútu
   - Admin logon stránka musí nastaviť tento atribút

6. **Chýbajúce admin práva** - Používateľ nemôže pristúpiť do admin zóny
   - Skontrolujte `NTLMAdminGroupName` konfiguráciu
   - Overťe prítomnosť admin skupiny v OAuth2 groups
   - Chyba: `accessDenied`

## Príklady konfigurácie

### Keycloak s WebJET

**Vytvorenie klienta v Keycloak:**

- Client ID: `webjetcms-client`
- Client Protocol: `openid-connect`
- Access Type: `confidential`
- Valid Redirect URIs: `https://your-domain.com/login/oauth2/code/keycloak`
- Standard Flow Enabled: `ON`
- Direct Access Grants Enabled: `OFF` (odporúčané)

**Konfigurácia v WebJET:**

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

- Vytvorte skupiny s názvami zhodnými s WebJET skupinami
- Priraďte používateľov do príslušných skupín
- Admin skupina: `webjet-admin` (podľa NTLMAdminGroupName)
- Mapujte skupiny na token (Client Scopes → dedicated scope → Add mapper → Group Membership)

**Testovanie:**

- Prihláste sa cez OAuth2 na admin stránke: `https://your-domain.com/admin/logon/`
- Overte synchronizáciu skupín v admin rozhraní WebJET
- Skontrolujte logy pre detailné informácie o synchronizácii

### Google OAuth2

```properties
oauth2_clients=google
oauth2_googleClientId=your-google-client-id.apps.googleusercontent.com
oauth2_googleClientSecret=your-google-client-secret
```

**Poznámka**: Pre Google OAuth2 sa defaultne **nesynchronizujú** skupiny, pretože Google nie je v `oauth2_clientsWithPermissions`. Pre synchronizáciu skupín je potrebné použiť provider ako Keycloak alebo Okta.

### OAuth2 pre zákaznícku zónu (User logon)

Pre implementáciu OAuth2 prihlásenia v zákazníckej zóne:

**Konfigurácia controllera pre user logon:**

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
        model.addAttribute("urls", oauth2AuthenticationUrls);
    }
    return USER_LOGON_FORM;
}
```

**Rozdiel oproti admin zóne:**

- **Nenastavuje** `oauth2_is_admin_section` session atribút (alebo nastavuje na `false`)
- Používa `afterLogonRedirect` namiesto `adminAfterLogonRedirect`
- OAuth2UserSuccessHandler:
  - Synchronizuje iba user groups
  - Nenastavuje permission groups
  - Nenastavuje admin flag
  - Nevyžaduje admin práva

## Prehľad implementácie

OAuth2 integrácia je implementovaná pomocou Spring Security OAuth2 modulu a obsahuje:

- **Spring Security konfiguráciu** - Konfigurácia OAuth2 klientov a endpoints
- **OAuth2DynamicSuccessHandler** - Dynamické rozhodovanie medzi admin a user handlerom
- **Dva typy Success Handlers**:
  - **OAuth2AdminSuccessHandler** - Pre admin zónu (synchronizuje user groups + permission groups + admin flag)
  - **OAuth2UserSuccessHandler** - Pre user zónu (synchronizuje iba user groups)
- **AbstractOAuth2SuccessHandler** - Spoločná base trieda s funkcionalitou pre oba handlery
- **Automatická synchronizácia skupín** - Mapovanie skupín z OAuth2 providera na WebJET skupiny (iba pre nakonfigurovaných providerov)
- **Admin práva z OAuth2** - Automatické nastavenie admin práv na základe skupín (iba admin zóna)

## Architektúra

### Hlavné komponenty

1. **SpringSecurityConf** (`src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java`)
   - Konfigurácia OAuth2 klientov
   - Registrácia OAuth2DynamicSuccessHandler
   - Konfigurácia authorized client service

2. **OAuth2DynamicSuccessHandler** (`src/main/java/sk/iway/iwcm/system/spring/OAuth2DynamicSuccessHandler.java`)
   - Dynamické rozhodovanie medzi admin a user handlerom
   - Používa session atribút `oauth2_is_admin_section` pre rozlíšenie

3. **AbstractOAuth2SuccessHandler** (`src/main/java/sk/iway/iwcm/system/spring/AbstractOAuth2SuccessHandler.java`)
   - Abstraktná base trieda pre OAuth2 Success Handlers
   - Spoločná funkcionalita pre extrakciu skupín a atribútov z OAuth2
   - Spracovanie chýb cez session

4. **OAuth2AdminSuccessHandler** (`src/main/java/sk/iway/iwcm/system/spring/OAuth2AdminSuccessHandler.java`)
   - Spracovanie úspešnej OAuth2 autentifikácie pre **admin zónu**
   - Vytvorenie alebo aktualizácia používateľa
   - Synchronizácia skupín a práv (user groups + permission groups)
   - Nastavenie admin práv

5. **OAuth2UserSuccessHandler** (`src/main/java/sk/iway/iwcm/system/spring/OAuth2UserSuccessHandler.java`)
   - Spracovanie úspešnej OAuth2 autentifikácie pre **zákaznícku zónu**
   - Synchronizácia iba user groups (nie permission groups)
   - Nenastavuje admin práva

6. **AdminLogonController** (`src/main/java/sk/iway/iwcm/logon/AdminLogonController.java`)
   - Zobrazenie OAuth2 prihlasovacích odkazov na logon stránke
   - Generovanie URL pre OAuth2 poskytovateľov
   - Spracovanie OAuth2 chýb zo session
   - Nastavenie `oauth2_is_admin_section` atribútu

7. **Logon Template** (`src/main/webapp/admin/skins/webjet8/logon-spring.jsp`)
   - Zobrazenie OAuth2 prihlasovacích tlačidiel

## API referencia

### OAuth2DynamicSuccessHandler

Dynamický router pre OAuth2 autentifikáciu:

- `onAuthenticationSuccess()` - Rozhoduje medzi admin a user handlerom na základe session atribútu `oauth2_is_admin_section`

### AbstractOAuth2SuccessHandler

Abstraktná base trieda so spoločnou funkcionalitou:

- `onAuthenticationSuccess()` - Abstraktná metóda implementovaná v potomkoch
- `getUsernameAttribute()` - Vráti názov atribútu pre username z konfigurácie (`oauth2_usernameAttribute`), alebo predvolenú hodnotu `preferred_username`
- `createNewUserFromOAuth2()` - Vytvorenie nového používateľa z OAuth2 údajov
- `updateExistingUserFromOAuth2()` - Aktualizácia existujúceho používateľa
- `getProviderId()` - Zistenie ID OAuth2 providera z autentifikácie
- `shouldSyncPermissions()` - Kontrola, či má provider nakonfigurovanú synchronizáciu práv
- `extractGroupsFromOAuth2()` - Extrakcia skupín z OAuth2 atribútov (všetky formáty)
- `extractFromAttribute()` - Extrakcia z jednoduchých atribútov
- `extractFromResourceAccess()` - Extrakcia z Keycloak `resource_access`
- `extractFromRealmAccess()` - Extrakcia z Keycloak `realm_access`
- `extractRolesFromClientResource()` - Extrakcia rolí z konkrétneho klienta
- `extractRolesFromRolesObject()` - Pomocná metóda pre extrakciu z roles objektu
- `handleError()` - Pomocná metóda pre spracovanie OAuth2 chýb cez session

### OAuth2AdminSuccessHandler (Admin zóna)

Handler pre admin zónu:

- `onAuthenticationSuccess()` - Hlavný entry point po úspešnej autentifikácii
- `applyOAuth2Permissions()` - Aplikovanie práv z OAuth2 (user groups + permission groups + admin flag)
- `removeAllGroupAssignments()` - Odstránenie zo všetkých skupín (user + permission)
- `synchronizeUserGroups()` - Synchronizácia user groups a permission groups

### OAuth2UserSuccessHandler (User zóna)

Handler pre zákaznícku zónu:

- `onAuthenticationSuccess()` - Hlavný entry point po úspešnej autentifikácii
- `applyOAuth2UserGroups()` - Aplikovanie iba user groups (nie permission groups, nie admin flag)
- `removeAllGroupAssignments()` - Odstránenie zo všetkých user groups
- `synchronizeUserGroups()` - Synchronizácia iba user groups

### SpringSecurityConf

Metódy pre OAuth2 konfiguráciu:

- `filterChain()` - Registruje `OAuth2DynamicSuccessHandler` ako success handler
- `clientRegistrationRepository()` - Bean pre registráciu OAuth2 klientov (vracia prázdnu implementáciu ak nie sú klienti)
- `buildClientRegistration()` - Vytvorenie konfigurácie pre konkrétneho providera (podporuje Google, Facebook, GitHub, Okta + custom)
- `authorizedClientService()` - Bean pre správu autorizovaných klientov

### AdminLogonController

Metódy pre OAuth2 admin prihlásenie:

- `showLogonForm()` - Nastavuje session atribút `oauth2_is_admin_section` a generuje OAuth2 URL
- Spracováva OAuth2 chyby zo session a zobrazuje príslušné chybové hlášky

