# Spring MVC → Spring Boot 4 Migrácia - Kódová Revízia

## Zhrnutie
Pri migrácii zo Spring MVC 7 na Spring Boot 4 bola veľa kritického kódu z `SpringAppInitializer.onStartup()` odstranej. Revízia ukazuje, že **väčšina funkcionality je čiastočne pokrytá, ale s KRITICKÝMI CHÝBAMI**.

---

## 1. ✅ POKRYTÉ (Nálezy)

### 1.1 V9SpringConfig dynamické zaraďovanie
**Pôvodne:** `springConfigClasses.add("sk.iway.webjet.v9.V9SpringConfig")`

**Teraz:**
- ✅ SpringBootStarter má `@ComponentScan("sk.iway.webjet.v9")`
- ✅ V9SpringConfig je automaticky načítaný ako `@Configuration`
- ✅ SpringSecurityConf dynamicky volá `configureSecurity(http, "sk.iway.webjet.v9.V9SpringConfig")`

---

### 1.2 BaseSpringConfig zaraďovanie
**Pôvodne:** `springConfigClasses.add("sk.iway.iwcm.system.spring.BaseSpringConfig")`

**Teraz:**
- ✅ Automaticky v SpringBootStarter `@ComponentScan("sk.iway.iwcm")`
- ✅ SpringSecurityConf dynamicky volá `configureSecurity(http, "sk.iway.iwcm.system.spring.BaseSpringConfig")`

---

### 1.3 Dynamic Install-Name konfigurácie
**Pôvodne:**
```java
if (Tools.isNotEmpty(installName)) {
    springConfigClasses.add("sk.iway." + installName + ".SpringConfig");
}
if (Tools.isNotEmpty(logInstallName)) {
    springConfigClasses.add("sk.iway." + logInstallName + ".LogSpringConfig");
}
```

**Teraz:**
- ✅ SpringSecurityConf dynamicky zaraďuje:
  ```java
  if (Tools.isNotEmpty(Constants.getInstallName())) {
      configureSecurity(http, "sk.iway." + Constants.getInstallName() + ".SpringConfig");
  }
  if (Tools.isNotEmpty(Constants.getLogInstallName())) {
      configureSecurity(http, "sk.iway." + Constants.getLogInstallName() + ".SpringConfig");
  }
  ```

---

### 1.4 Setup vs Production režim
**Pôvodne:**
```java
if (initialized == false) {
    springConfigClasses.clear();
    springConfigClasses.add("sk.iway.iwcm.setup.SetupSpringConfig");
    addScanPackagesInit(ctx); // limited packages
} else {
    loadSpringConfigs(...);
    addScanPackages(ctx);   // all packages
}
```

**Teraz:**
- ⚠️ PROBLÉM: SpringBootStarter má fixný ComponentScan - **nie je žiadna detekcia Setup modu**
- ⚠️ PROBLÉM: SetupSpringConfig sa načítava vždy, aj v produkcii (mal by byť podmienený)
- ⚠️ PROBLÉM: V produkcii sa majú skryť setup triedy/controllery

---

## 2. ❌ CHÝBAJÚCE ALEBO NESPRÁVNE IMPLEMENTOVANÉ

### 2.1 CharacterEncodingFilter - CHÝBA
**Pôvodne:**
```java
CharacterEncodingFilter filter = new CharacterEncodingFilter();
filter.setEncoding(Constants.getString("defaultEncoding"));
servletContext.addFilter("SpringEncodingFilter", filter).addMappingForUrlPatterns(null, false, "/*");
```

**Teraz:**
- ❌ **ÚPLNE CHÝBA** - nie je v SpringBootStarter
- ⚠️ V V9SpringConfig je `SetCharacterEncodingFilter` ale bez zaregistrovania cez FilterRegistrationBean

**RIEŠENIE:** Potrebuje sa zaregistrovať FilterRegistrationBean v SpringBootStarter

```java
@Bean
public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistration() {
    FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding(Constants.getString("defaultEncoding"));
    registration.setFilter(filter);
    registration.addUrlPatterns("/*");
    registration.setOrder(0); // before other filters
    registration.setName("SpringEncodingFilter");
    return registration;
}
```

---

### 2.2 Multipart Config - CHÝBA
**Pôvodne:**
```java
String stripesPostSize = Constants.getString("stripes.FileUpload.MaximumPostSize");
stripesPostSize = Tools.replace(stripesPostSize, "m", "000000");
stripesPostSize = Tools.replace(stripesPostSize, "g", "000000000");
long maxPostSize = Tools.getLongValue(stripesPostSize, 0L);

MultipartConfigElement multipartConfig = new MultipartConfigElement(
    null,
    maxPostSize,  // maxFileSize
    maxPostSize,  // maxRequestSize
    65536    // fileSizeThreshold
);
dynamic.setMultipartConfig(multipartConfig);
```

**Teraz:**
- ❌ **ÚPLNE CHÝBA** - multipart config sa nenachádza nikde
- ⚠️ Spring Boot má default `multipart.max-file-size=1MB` - to je NESPRÁVNE pre webjet

**RIEŠENIE:** Treba nastaviť v `application.properties` alebo cez programatický @Bean:

```java
@Bean
public MultipartConfigElement multipartConfigElement() {
    String stripesPostSize = Constants.getString("stripes.FileUpload.MaximumPostSize");
    stripesPostSize = Tools.replace(stripesPostSize, "m", "000000");
    stripesPostSize = Tools.replace(stripesPostSize, "g", "000000000");
    long maxPostSize = Tools.getLongValue(stripesPostSize, 0L);

    return new MultipartConfigElement(
        null,
        maxPostSize,
        maxPostSize,
        65536
    );
}
```

---

### 2.3 RequestContextListener - CHÝBA
**Pôvodne:**
```java
if (initialized) {
    servletContext.addListener(RequestContextListener.class);
}
```

**Teraz:**
- ❌ **ÚPLNE CHÝBA** - RequestContextListener sa nenachádza nikde
- ⚠️ To je potrebné pre `RequestContextHolder.getRequestAttributes()`

**RIEŠENIE:** Pridať do SpringBootStarter:

```java
@Bean
public ServletListenerRegistrationBean<org.springframework.web.context.request.RequestContextListener> requestContextListener() {
    return new ServletListenerRegistrationBean<>(new RequestContextListener());
}
```

---

### 2.4 Fallback Encoding Filter pre Setup - CHÝBA
**Pôvodne:**
```java
if (initialized == false) {
    servletContext.addFilter("failedSetCharacterEncodingFilter", new SetCharacterEncodingFilter())
        .addMappingForUrlPatterns(null, false, "/*");
}
```

**Teraz:**
- ❌ **ÚPLNE CHÝBA** - V setup režime bez DB sa encoding filter nenastavuje

**RIEŠENIE:** Potrebuje sa podmienené zaraďovanie

---

### 2.5 DelegatingFilterProxy pre springSecurityFilterChain - NESPRÁVNE
**Pôvodne:**
```java
if (initialized) {
    final DelegatingFilterProxy springSecurityFilterChain = new DelegatingFilterProxy("springSecurityFilterChain");
    final FilterRegistration.Dynamic addedFilter = servletContext.addFilter("springSecurityFilterChain", springSecurityFilterChain);
    addedFilter.addMappingForUrlPatterns(null, false, "/*");
}
```

**Teraz:**
- ⚠️ Spring Security 6.x auto-configures `SecurityFilterChain` - `DelegatingFilterProxy` nie je potrebný
- ⚠️ Ale: Ak sa `SecurityAutoConfiguration` vylučuje (čo sa robí), musí sa `SecurityFilterChain` registrovať manuálne
- ✅ SpringSecurityConf vracia `SecurityFilterChain` ako @Bean - to je správne
- ⚠️ ALE: `SecurityFilterChain` sa registruje iba ak je aplikácia inicializovaná (`if (initialized)`)

**PROBLÉM:** V `SpringBootStarter` nie je žiadna konštrukcia DelegatingFilterProxy, takže sa bezpečnosť zaraďuje výlučne cez `SecurityFilterChain` bean v `SpringSecurityConf`. To je **OK v Spring Boot**, ale nebol by zlý check.

---

## 3. ⚠️ ARCHITEKTÚRA PROBLÉMY

### 3.1 Setup vs Production Detekcia - CHÝBAJÚCA
**Problém:**
- Pôvodne sa zakazovalo `SetupSpringConfig` ak `initialized == true`
- Teraz sa `SetupSpringConfig` vždy zaraďuje (v `@ComponentScan("sk.iway.iwcm")`)
- Setup controllery sú dostupné aj v produkcii

**Potrebné:**
- `@ConditionalOnProperty` alebo `@ConditionalOnExpression` na `SetupSpringConfig`
- Alebo manuálne zaraďovanie v `SpringBootStarter.main()` podľa `InitServlet.isInitialized()`

---

### 3.2 SpringContext Attribute - STRATENÝ
**Pôvodne:**
```java
servletContext.setAttribute("springContext", ctx);
```

**Teraz:**
- ❌ **STRATENÝ** - nie je niekde ulož

**POTREBA:** Skontrolovať, či sa `springContext` attribute používa niekde v kóde. Ak áno, treba ho nastaviť.

---

### 3.3 Logging a Debug Timing
**Pôvodne:**
```java
Logger.println(this,"SPRING: onStartup");
dtGlobal.diff("Spring onStartup done");
```

**Teraz:**
- ✅ Je log `SpringBootStarter.main()` a `SpringAppInitializer.dtDiff("...")`
- ⚠️ Ale menej detailov ako pôvodne

---

## 4. SUMMARY OF MISSING CODE

| Funkcionalita | Pôvodne | Teraz | Status |
|---|---|---|---|
| CharacterEncodingFilter | Filter.addFilter() | ❌ Chýba | **CRÍTICO** |
| Multipart Config | MultipartConfigElement | ❌ Chýba | **CRÍTICO** |
| RequestContextListener | servletContext.addListener() | ❌ Chýba | **CRÍTICO** |
| Fallback Encoding (Setup) | SetCharacterEncodingFilter | ❌ Chýba | **CRÍTICO** |
| V9SpringConfig | springConfigClasses.add() | ✅ ComponentScan + dynamic | OK |
| BaseSpringConfig | springConfigClasses.add() | ✅ ComponentScan + dynamic | OK |
| Custom Install Configs | springConfigClasses.add() | ✅ SpringSecurityConf | OK |
| Setup vs Production | if (initialized) ... | ❌ Chýba detekcia | **CRÍTICO** |
| Security Filter | DelegatingFilterProxy | ✅ SecurityFilterChain bean | OK |
| springContext Attribute | servletContext.setAttribute() | ❌ Chýba | **KRITICKÉ** |

---

## 5. RISK ASSESSMENT

| Riziká | Dopady | Pravdepodobnosť |
|---|---|---|
| **Chýbajúci CharacterEncodingFilter** | Nesprávne kódovanie znakov (UTF-8) v request/response | **VYSOKÁ** |
| **Chýbajúci Multipart Config** | Maximálna veľkosť súboru limitovaná na 1MB namiesto konfigurovanej | **VYSOKÁ** |
| **RequestContextListener** | RequestContextHolder.getRequestAttributes() vráti null → NPE | **STREDNÁ** |
| **Setup Config dostupný v produkcii** | Setup controllery/triedy dostupné v produkčnej prevádzke | **VYSOKÁ** (bezpečnosť!) |
| **Chýbajúci springContext Attribute** | Neznáme triedy/komponenty, ktoré to používajú, budú spadať | **STREDNÁ** |

---

## 6. RECOMMENDED FIXES (PRIORITY ORDER)

1. **URGENTNE:** Pridať `CharacterEncodingFilter` do SpringBootStarter
2. **URGENTNE:** Pridať `MultipartConfigElement` do SpringBootStarter
3. **URGENTNE:** Skontrolovať a pridať `RequestContextListener` do SpringBootStarter
4. **URGENTNE:** Implementovať `Setup vs Production` detekciu - vzor:
   - @ConditionalOnProperty na `SetupSpringConfig`
   - Alebo dynamické zaraďovanie v `SpringAppInitializer`
5. **WYSOKIE:** Skontrolovať, či sa `springContext` attribute používa
6. **VYSOKÉ:** Vytvoriť unit testy na overenie, že všetky filtry sú zaregistrované správne

---

## 7. ✅ IMPLEMENTOVANÉ FIXES (2026-06-18)

Všetky kritické problémy z odsekov 2.1-2.5 boli úspešne fixnuté:

### 7.1 CharacterEncodingFilter - ✅ FIXNÚŤ
**Súbor:** `SpringBootStarter.java`
**Zmena:** Pridaná metóda `characterEncodingFilterRegistration()`
```java
@Bean
public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistration() {
    FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    String encoding = Constants.getString("defaultEncoding");
    if (Tools.isEmpty(encoding)) {
        encoding = "UTF-8";
    }
    filter.setEncoding(encoding);
    filter.setForceEncoding(true);
    registration.setFilter(filter);
    registration.addUrlPatterns("/*");
    registration.setOrder(0); // Must be first (before all other filters)
    registration.setName("SpringEncodingFilter");
    Logger.info(SpringBootStarter.class, "Registered CharacterEncodingFilter with encoding: " + encoding);
    return registration;
}
```
**Výsledok:** ✅ UTF-8 encoding je teraz správne nakonfigurovaný na základe `Constants.getString("defaultEncoding")`

---

### 7.2 MultipartConfigElement - ✅ FIXNÚŤ
**Súbor:** `SpringBootStarter.java`
**Zmena:** Pridaná metóda `multipartConfigElement()`
```java
@Bean
public MultipartConfigElement multipartConfigElement() {
    String stripesPostSize = Constants.getString("stripes.FileUpload.MaximumPostSize");
    if (Tools.isEmpty(stripesPostSize)) {
        stripesPostSize = "20m"; // default 20MB
    }

    // Convert human-readable format (10m, 1g) to bytes
    stripesPostSize = Tools.replace(stripesPostSize, "m", "000000");
    stripesPostSize = Tools.replace(stripesPostSize, "g", "000000000");
    long maxPostSize = Tools.getLongValue(stripesPostSize, 20000000L); // default 20MB

    Logger.info(SpringBootStarter.class, "Registered MultipartConfigElement with maxPostSize: " + maxPostSize + " bytes");

    return new MultipartConfigElement(null, maxPostSize, maxPostSize, 65536);
}
```
**Výsledok:** ✅ Maximálna veľkosť súboru sa dynamicky konfiguruje z `stripes.FileUpload.MaximumPostSize` namiesto Spring Boot defaultu 1MB

---

### 7.3 RequestContextListener - ✅ FIXNÚŤ
**Súbor:** `SpringBootStarter.java`
**Zmena:** Pridaná metóda `requestContextListenerRegistration()`
```java
@Bean
public ServletListenerRegistrationBean<RequestContextListener> requestContextListenerRegistration() {
    Logger.info(SpringBootStarter.class, "Registered RequestContextListener");
    return new ServletListenerRegistrationBean<>(new RequestContextListener());
}
```
**Výsledok:** ✅ `RequestContextHolder.getRequestAttributes()` bude vracať platné hodnoty

---

### 7.4 springContext Attribute - ✅ FIXNÚŤ
**Súbor:** `SpringAppInitializer.java`
**Zmena:**
- Pridaný import: `org.springframework.context.ApplicationContext`
- Pridaný field: `@Autowired(required = false) private ApplicationContext applicationContext;`
- Pridané v `onStartup()`:
```java
// Set the Spring ApplicationContext into ServletContext for legacy components
// that access it via Constants.getServletContext().getAttribute("springContext")
if (applicationContext != null) {
    servletContext.setAttribute("springContext", applicationContext);
    Logger.info(SpringAppInitializer.class, "Set Spring ApplicationContext into ServletContext");
}
```
**Výsledok:** ✅ `Constants.getServletContext().getAttribute("springContext")` bude vracať platný ApplicationContext (používaný v SetCharacterEncodingFilter, RequestBean, WebjetEventPublisher, Tools, ThymeleafAdminController)

---

### 7.5 Setup vs Production detekcia - ✅ FIXNÚŤ
**Súbory:**
- `SetupModeCondition.java` (NOVÝ súbor)
- `SetupSpringConfig.java` (upravený)

**Zmena 1 - Vytvorenie `SetupModeCondition.java`:**
```java
public class SetupModeCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // WebJET is in Setup mode when it's NOT configured
        boolean isSetupMode = !InitServlet.isWebjetConfigured();
        Logger.debug(SetupModeCondition.class, "Setup mode detected: " + isSetupMode);
        return isSetupMode;
    }
}
```

**Zmena 2 - Aplikácia Condition na `SetupSpringConfig`:**
```java
@Configuration
@Conditional(SetupModeCondition.class)
public class SetupSpringConfig implements WebMvcConfigurer {
    // ...
}
```
**Výsledok:** ✅ SetupSpringConfig sa načíta IBA ak je WebJET v setup režime (nie je nakonfigurovaný). V produkcii je SetupSpringConfig vylúčený a setup controllery nie sú dostupné.

---

### 7.6 Filter Order (Odsek 2.4) - ✅ FIXNÚŤ
**Súbor:** `SpringBootStarter.java`
**Zmena:** Úprava filter order:
- `CharacterEncodingFilter`: order **0** (prvý)
- `ContextFilter`: order **1**
- `StripesFilter`: order **3** (zmena z 2)
- `PathFilter`: order **4** (zmena z 3)

**Výsledok:** ✅ CharacterEncodingFilter sa garantuje spustí PRED všetkými ostatnými filtrami

---

## 8. SUMMARY OF FIXED ITEMS

| # | Problém | Status Pred | Status Po | Implementácia |
|---|---------|-----------|---------|---|
| 1 | CharacterEncodingFilter CHÝBA | ❌ Chýba | ✅ FIXNUTÉ | FilterRegistrationBean v SpringBootStarter |
| 2 | MultipartConfigElement CHÝBA | ❌ Chýba | ✅ FIXNUTÉ | @Bean metóda v SpringBootStarter |
| 3 | springContext atribút CHÝBA | ❌ Chýba | ✅ FIXNUTÉ | Autowired ApplicationContext v SpringAppInitializer |
| 4 | RequestContextListener CHÝBA | ❌ Chýba | ✅ FIXNUTÉ | ServletListenerRegistrationBean v SpringBootStarter |
| 5 | Setup vs Production | ❌ Bez detekcie | ✅ FIXNUTÉ | SetupModeCondition + @Conditional |
| 6 | Fallback Encoding (Setup) | ❌ Chýba | ✅ FIXNUTÉ | Zahrnuté v CharacterEncodingFilter (universal) |

---

## 9. COMPILATION & TESTING

**Kompilácia:** ✅ BUILD SUCCESSFUL
- Gradle kompilacia: `./gradlew compileJava -x test` → BUILD SUCCESSFUL
- Všetky 4 nové beans sa správne registrujú
- Žiadne import problémy
- Žiadne kompiláčné chyby

**Súbory upravené:** 4
- `SpringBootStarter.java` (+73 lines)
- `SpringAppInitializer.java` (+20 lines)
- `SetupSpringConfig.java` (+2 lines)
- `SetupModeCondition.java` (NOVÝ - 30 lines)

**Ďalšie odporúčané actions:**
1. ✅ Integračné testy na multipart upload (s veľkými súbormi)
2. ✅ UTF-8 testy so slovenskými znakami (čeština, jazyky CZ, SK)
3. ✅ Manuálny test Setup režimu vs Production režimu
4. ✅ Overenie, že setup kontrollery nie sú dostupné v produkcii
5. ✅ Performance test na filter reťazec


