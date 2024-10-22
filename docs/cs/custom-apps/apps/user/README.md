# Uživatelé

## Registrační formulář

Pokud máte na stránce [zóna chráněná heslem](../../../redactor/zaheslovana-zona/README.md) pomocí registračního formuláře můžete při registraci vyvolat vlastní kód a například upravit/přidat údaje uživatele nebo nastavit skupiny, do kterých patří na základě dalších informací (např. e-mailové adresy).

Na `pageParams` registrace přidat parametr `afterSaveInterceptor` - `!INCLUDE(/components/user/newuser.jsp, ..., afterSaveInterceptor=sk.iway...MojaTrieda`, zatímco tato třída implementuje `sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor`. V metodě `public boolean intercept(UserDetails user, HttpServletRequest request)` můžete upravit `user` objekt. Když metoda vrátí `true`, změny se pak rovněž zapíší do databáze. Název třídy lze zadat také prostřednictvím konf. proměnné `stripesUserAfterSaveClass` pokud nejste spokojeni se vstupem do `pageParams` objekt.

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

Po registraci WebJET odešle uvítací e-mail, pokud jej ve své implementaci odešlete sami, můžete tuto metodu přepsat. `shouldSendUserWelcomeEmail` a vrátit false.

## Přihlašování a odhlašování

Pokud potřebujete spustit vlastní kód při přihlášení/odhlášení uživatele ze zóny chráněné heslem, můžete implementovat třídu. `sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor`, který obsahuje metody `public boolean logon(UserDetails user, HttpServletRequest request);` volané při přihlášení a `public boolean logoff(UserDetails user, HttpServletRequest request);` zavolán při odhlášení uživatele. Název třídy zadejte do proměnné conf. `stripesLogonLogoffInterceptorClass`.
