# Používatelia

## Registračný formulár

Ak máte v stránke [zaheslovanú zónu](../../../redactor/zaheslovana-zona/README.md) s registračným formulárom môžete pri registrácii vyvolať váš vlastný kód a napr. upraviť/doplniť údaje používateľa, alebo nastaviť skupiny do ktorých patrí na základe dodatočných informácii (napr. email adresy).

Do `pageParams` registrácie pridajte parameter `afterSaveInterceptor` - `!INCLUDE(/components/user/newuser.jsp, ..., afterSaveInterceptor=sk.iway...MojaTrieda`, pričom táto trieda implementuje `sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor`. V metóde `public boolean intercept(UserDetails user, HttpServletRequest request)` môžete modifikovať `user` objekt. Keď metóda vráti `true`, tak sa následne zmeny zapíšu aj do databázy. Meno triedy je možné zadať aj cez konf. premennú `stripesUserAfterSaveClass` ak vám nevyhovuje zadanie do `pageParams` objektu.

Príklad triedy:

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

Po registrácii WebJET posiela uvítací email, ak ho posielate sami vo vašej implementácii môžete prepísať metódu `shouldSendUserWelcomeEmail` a vrátiť hodnotu false.

## Prihlásenie a odhlásenie

Ak potrebujete vykonať vlastný kód pri prihlásení používateľa/odhlásení používateľa do zaheslovanej zóny, môžete implementovať triedu `sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor`, ktorá obsahuje metódy `public boolean logon(UserDetails user, HttpServletRequest request);` volanú pri prihlásení a `public boolean logoff(UserDetails user, HttpServletRequest request);` volanú pri odhlásení používateľa. Meno vašej triedy zadajte do konf. premennej `stripesLogonLogoffInterceptorClass`.