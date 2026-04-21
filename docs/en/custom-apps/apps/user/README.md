# Users

## Registration form

If you have a [password-protected zone](../../../redactor/zaheslovana-zona/README.md) with a registration form on your page, you can call up your own code during registration and, for example, edit/add user data, or set the groups to which they belong based on additional information (e.g. email address).

Add the `afterSaveInterceptor` - `!INCLUDE(/components/user/newuser.jsp, ..., afterSaveInterceptor=sk.iway...MojaTrieda` parameter to the `pageParams` registration, and this class implements `sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor`. In the `public boolean intercept(UserDetails user, HttpServletRequest request)` method, you can modify the `user` object. When the method returns `true`, the changes will be written to the database. The class name can also be specified via the conf. variable `stripesUserAfterSaveClass` if you are not comfortable entering it into the `pageParams` object.

Class example:

```java
package sk.iway.installname.user;

import jakarta.servlet.http.HttpServletRequest;

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

After registration, WebJET sends a welcome email. If you send it yourself in your implementation, you can override the `shouldSendUserWelcomeEmail` method and return the value false.

## Login and logout

If you need to execute custom code when a user logs in/logs out of a password-protected zone, you can implement a class `sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor` that contains methods `public boolean logon(UserDetails user, HttpServletRequest request);` called when a user logs in and `public boolean logoff(UserDetails user, HttpServletRequest request);` called when a user logs out. Enter the name of your class in the config variable `stripesLogonLogoffInterceptorClass`.