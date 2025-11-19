# OAuth2 Autentifikácia v WebJET CMS

WebJET CMS podporuje OAuth2 autentifikáciu pre prihlasovanie používateľov prostredníctvom externých poskytovateľov ako sú Google, Facebook, GitHub, Keycloak a ďalších.

## Prehľad implementácie

OAuth2 integrácia je implementovaná pomocou Spring Security OAuth2 modulu a obsahuje:

- **Spring Security konfiguráciu** - Konfigurácia OAuth2 klientov a endpoints
- **OAuth2SuccessHandler** - Spracovanie úspešnej autentifikácie a synchronizácia používateľov
- **Automatická synchronizácia skupín** - Mapovanie skupín z OAuth2 providera na WebJET skupiny
- **Admin práva z OAuth2** - Automatické nastavenie admin práv na základe skupín

## Architektúra

### Hlavné komponenty

1. **SpringSecurityConf** (`src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java`)
   - Konfigurácia OAuth2 klientov
   - Registrácia success handlera
   - Konfigurácia authorized client service

2. **OAuth2SuccessHandler** (`src/main/java/sk/iway/iwcm/system/spring/OAuth2SuccessHandler.java`)
   - Spracovanie úspešnej OAuth2 autentifikácie
   - Vytvorenie alebo aktualizácia používateľa
   - Synchronizácia skupín a práv

3. **AdminLogonController** (`src/main/java/sk/iway/iwcm/logon/AdminLogonController.java`)
   - Zobrazenie OAuth2 prihlasovacích odkazov na logon stránke
   - Generovanie URL pre OAuth2 poskytovateľov

4. **Logon Template** (`src/main/webapp/admin/skins/webjet8/logon-spring.jsp`)
   - Zobrazenie OAuth2 prihlasovacích tlačidiel

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

### Vlastní poskytovatelia (napr. Keycloak)

Pre vlastných poskytovateľov je potrebné nastaviť všetky OAuth2 parametre:

```properties
oauth2_keycloakClientId=webjet-client
oauth2_keycloakClientSecret=your-client-secret
oauth2_keycloakAuthorizationUri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/auth
oauth2_keycloakTokenUri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=https://keycloak.example.com/auth/realms/your-realm
oauth2_keycloakUserNameAttributeName=email
oauth2_keycloakScopes=openid,profile,email
oauth2_keycloakClientName=Keycloak
```

### Redirect URI

Pre všetkých poskytovateľov sa automaticky nastavuje redirect URI:

```text
{baseUrl}/login/oauth2/code/{registrationId}
```

Príklad: `https://your-webjet-domain.com/login/oauth2/code/google`

## Synchronizácia používateľov

### Vytvorenie nového používateľa

Keď sa používateľ prihlási prvýkrát cez OAuth2:

1. **Extrakcia údajov** - Z OAuth2 atribútov sa extrahuje email, meno a priezvisko
2. **Vytvorenie používateľa** - Vytvorí sa nový používateľ v WebJET databáze
3. **Nastavenie loginu** - Login sa nastaví ako časť emailu pred zavináčom
4. **Autorizácia** - Používateľ sa označí ako autorizovaný

```java
UserDetails userDetails = new UserDetails();
userDetails.setEmail(email);
userDetails.setFirstName(givenName);
userDetails.setLastName(familyName);
userDetails.setLogin(email.substring(0, email.indexOf("@")));
userDetails.setAuthorized(true);
```

### Aktualizácia existujúceho používateľa

Pre existujúcich používateľov sa aktualizujú:

- Meno a priezvisko (ak sa zmenili)
- Skupinové priradenia (iba pre Keycloak)

## Synchronizácia skupín

OAuth2SuccessHandler obsahuje špecializovanú logiku pre synchronizáciu skupín z nakonfigurovaných OAuth2 providerov.

### Konfigurácia synchronizácie práv

Aby sa synchronizovali skupiny a práva z OAuth2 providera, je potrebné nastaviť konfiguračnú premennú:

```properties
oauth2_clientsWithPermissions=keycloak,okta
```

Táto premenná definuje zoznam providerov (oddelený čiarkami), pre ktorých sa má vykonávať synchronizácia skupín a práv. Pre ostatných providerov (napr. Google, Facebook) sa synchronizácia nevykonáva.

### Detekcia providera

```java
private boolean shouldSyncPermissions(String providerId) {
    String configuredProviders = Constants.getString("oauth2_clientsWithPermissions");
    if (Tools.isEmpty(configuredProviders)) {
        return false;
    }
    List<String> providers = List.of(Tools.getTokens(configuredProviders, ","));
    return providers.contains(providerId);
}
```

### Extrakcia skupín

Handler extrahuje skupiny z rôznych OAuth2 atribútov:

1. **Základné atribúty**: `groups`, `roles`, `group_membership`
2. **Keycloak realm_access**: `realm_access.roles`
3. **Keycloak resource_access**: `resource_access.{client}.roles`
4. **Spring Security authorities**: Automaticky extrahované role

```java
// Príklad štruktúry Keycloak tokenov
{
  "realm_access": {
    "roles": ["admin", "user"]
  },
  "resource_access": {
    "webjet-client": {
      "roles": ["webjet-admin", "content-editor"]
    }
  }
}
```

### Mapovanie skupín

1. **User Groups** - Hľadajú sa skupiny používateľov s názvom zhodným s OAuth2 skupinou
2. **Permission Groups** - Hľadajú sa skupiny práv s názvom zhodným s OAuth2 skupinou
3. **Úplná synchronizácia** - Odstránia sa všetky staré skupiny a pridajú sa nové

```java
// Odstránenie zo všetkých existujúcich skupín
removeAllGroupAssignments(userDetails);

// Pridanie do nových skupín
for (UserGroupDetails group : newUserGroups) {
    userDetails.addToGroup(group.getUserGroupId());
}

// Pridanie do permission groups
for (PermissionGroupBean group : newPermissionGroups) {
    UsersDB.addUserToPermissionGroup(userDetails.getUserId(), group.getUserPermGroupId());
}
```

## Admin práva

### Automatické nastavenie admin práv

Pre nakonfigurovaných OAuth2 providerov (definovaných v `oauth2_clientsWithPermissions`) sa admin práva nastavujú automaticky na základe konfiguračnej premennej:

```properties
NTLMAdminGroupName=admin
```

Ak OAuth2 skupiny obsahujú skupinu s názvom z `NTLMAdminGroupName`, používateľ sa označí ako admin.

```java
String adminGroupName = Constants.getString("NTLMAdminGroupName");
boolean isAdmin = oauth2Groups.contains(adminGroupName);
if (userDetails.isAdmin() != isAdmin) {
    userDetails.setAdmin(isAdmin);
    UsersDB.saveUser(userDetails);
}
```

## Bezpečnostné aspekty

### Obmedzenia OAuth2 procesov

1. **Konfigurovateľná synchronizácia** - Synchronizácia skupín a admin práv funguje iba pre OAuth2 providerov nakonfigurovaných v `oauth2_clientsWithPermissions`
2. **Email validácia** - Každý používateľ musí mať validný email atribút
3. **Jedinečnosť emailu** - Email musí byť jedinečný v systéme

### Error handling

OAuth2SuccessHandler obsahuje rozsiahle error handling s ukladaním chýb do session a presmerovaním na logon stránku:

```java
// Pomocná metóda pre spracovanie OAuth2 chýb
private void handleOAuth2Error(HttpServletRequest request, HttpServletResponse response, String errorCode) throws IOException {
    HttpSession session = request.getSession();
    session.setAttribute("oauth2_logon_error", errorCode);
    response.sendRedirect("/admin/logon/");
}

// Chyby sa uložia do session a používateľ je presmerovaný na logon stránku
handleOAuth2Error(request, response, "oauth2_email_not_found");
handleOAuth2Error(request, response, "oauth2_user_create_failed");
handleOAuth2Error(request, response, "oauth2_exception");
handleOAuth2Error(request, response, "accessDenied");
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

AdminLogonController automaticky generuje OAuth2 URL pre všetkých nakonfigurovaných poskytovateľov:

```java
Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
clientRegistrations.forEach(registration ->
    oauth2AuthenticationUrls.put(registration.getClientName(),
        authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
model.addAttribute("urls", oauth2AuthenticationUrls);
```

## Diagnostika a ladenie

### Logovanie

OAuth2SuccessHandler obsahuje podrobné logovanie pre diagnostiku:

```java
Logger.info(OAuth2SuccessHandler.class, "Found OAuth2 groups for user " + email + ": " + groups);
Logger.debug(OAuth2SuccessHandler.class, "Checking if provider is Keycloak - hasKeycloakAttributes: " + hasKeycloakAttributes);
Logger.info(OAuth2SuccessHandler.class, "Created new user for email: " + email);
```

### Časté problémy

1. **Chýbajúci email** - OAuth2 provider nevracia email atribút
2. **Neplatná konfigurácia** - Chybné OAuth2 endpoint URL
3. **Skupiny sa nesynchronizujú** - Provider nie je nakonfigurovaný v `oauth2_clientsWithPermissions` alebo chybné názvy skupín
4. **Redirect URI** - Nesprávne nastavený redirect URI u poskytovateľa

## Príklady konfigurácie

### Keycloak s WebJET

1. **Vytvorenie klienta v Keycloak**:
   - Client ID: `webjet-client`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `https://your-domain.com/login/oauth2/code/keycloak`

1. **Konfigurácia v WebJET**:

```properties
oauth2_clients=keycloak
oauth2_clientsWithPermissions=keycloak
oauth2_keycloakClientId=webjet-client
oauth2_keycloakClientSecret=generated-secret-from-keycloak
oauth2_keycloakAuthorizationUri=https://keycloak.example.com/auth/realms/webjet/protocol/openid-connect/auth
oauth2_keycloakTokenUri=https://keycloak.example.com/auth/realms/webjet/protocol/openid-connect/token
oauth2_keycloakUserInfoUri=https://keycloak.example.com/auth/realms/webjet/protocol/openid-connect/userinfo
oauth2_keycloakJwkSetUri=https://keycloak.example.com/auth/realms/webjet/protocol/openid-connect/certs
oauth2_keycloakIssuerUri=https://keycloak.example.com/auth/realms/webjet
oauth2_keycloakUserNameAttributeName=email
oauth2_keycloakScopes=openid,profile,email
oauth2_keycloakClientName=Keycloak Login
NTLMAdminGroupName=webjet-admin
```

1. **Skupiny v Keycloak**:

   - Vytvorte skupiny s názvami zhodnými s WebJET skupinami
   - Priraďte používateľov do príslušných skupín
   - Admin skupina: `webjet-admin` (podľa NTLMAdminGroupName)

### Google OAuth2

```properties
oauth2_clients=google
oauth2_googleClientId=your-google-client-id.apps.googleusercontent.com
oauth2_googleClientSecret=your-google-client-secret
```

### API referencia

### OAuth2SuccessHandler

Hlavné metódy:

- `onAuthenticationSuccess()` - Hlavný entry point po úspešnej autentifikácii
- `createNewUserFromOAuth2()` - Vytvorenie nového používateľa
- `updateExistingUserFromOAuth2()` - Aktualizácia existujúceho používateľa
- `applyOAuth2Permissions()` - Aplikovanie práv z OAuth2 (iba pre nakonfigurovaných providerov)
- `extractGroupsFromOAuth2()` - Extrakcia skupín z OAuth2 atribútov
- `getProviderId()` - Zistenie ID OAuth2 providera
- `shouldSyncPermissions()` - Kontrola, či má provider nakonfigurovanú synchronizáciu práv
- `handleOAuth2Error()` - Pomocná metóda pre spracovanie OAuth2 chýb cez session

### SpringSecurityConf

Metódy pre OAuth2:

- `clientRegistrationRepository()` - Bean pre registráciu OAuth2 klientov
- `buildClientRegistration()` - Vytvorenie konfigurácie pre konkrétneho providera
- `authorizedClientService()` - Bean pre správu autorizovaných klientov
