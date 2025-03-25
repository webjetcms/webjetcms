# Uživatelé

## Registrační formulář

Máte-li ve stránce [zaheslovanou zónu](../../../redactor/zaheslovana-zona/README.md) s registračním formulářem můžete při registraci vyvolat váš vlastní kód. upravit/doplnit údaje uživatele, nebo nastavit skupiny do kterých patří na základě dodatečných informací (např. email adresy).

Do `pageParams` registrace přidejte parametr `afterSaveInterceptor` - `!INCLUDE(/components/user/newuser.jsp, ..., afterSaveInterceptor=sk.iway...MojaTrieda`, přičemž tato třída implementuje `sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor`. V metodě `public boolean intercept(UserDetails user, HttpServletRequest request)` můžete modifikovat `user` objekt. Když metoda vrátí `true`, tak se následně změny zapíší i do databáze. Jméno třídy lze zadat i přes konf. proměnnou `stripesUserAfterSaveClass` pokud vám nevyhovuje zadání do `pageParams` objektu.

Příklad třídy:

```java
package sk.iway.installname.user;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor;
import sk.iway.iwcm.users.UserDetails;

public class RegUserInterceptor implements AfterRegUserSaveInterceptor {

    @Override
    public boolean intercept(UserDetails user, HttpServletRequest request) {
        //your code
        if ("true".equals(request.getParameter("myparam"))) user.setFieldA("changed text");
        return true;
    }

}
```

Po registraci WebJET posílá uvítací email, pokud ho posíláte sami ve vaší implementaci můžete přepsat metodu `shouldSendUserWelcomeEmail` a vrátit hodnotu false.

## Přihlášení a odhlášení

Pokud potřebujete provést vlastní kód při přihlášení uživatele/odhlášení uživatele do zaheslované zóny, můžete implementovat třídu `sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor`, která obsahuje metody `public boolean logon(UserDetails user, HttpServletRequest request);` volanou při přihlášení a `public boolean logoff(UserDetails user, HttpServletRequest request);` volanou při odhlášení uživatele. Zadejte název vaší třídy do konf. proměnné `stripesLogonLogoffInterceptorClass`.
