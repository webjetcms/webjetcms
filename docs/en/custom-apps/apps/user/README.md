# Users of

## Registration form

If you have in the page [password protected zone](../../../redactor/zaheslovana-zona/README.md) with the registration form, you can call up your own code when registering and, for example, edit/add user data or set up groups to which they belong based on additional information (e.g. email address).

To `pageParams` registration add parameter `afterSaveInterceptor` - `!INCLUDE(/components/user/newuser.jsp, ..., afterSaveInterceptor=sk.iway...MojaTrieda`, while this class implements `sk.iway.iwcm.stripes.AfterRegUserSaveInterceptor`. In the method `public boolean intercept(UserDetails user, HttpServletRequest request)` you can modify `user` object. When the method returns `true`, the changes are then also entered into the database. The class name can also be entered via a conf. variable `stripesUserAfterSaveClass` if you are not satisfied with the entry into `pageParams` object.

Class example:

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

After registration WebJET sends a welcome email, if you send it yourself in your implementation you can override the method `shouldSendUserWelcomeEmail` and return false.

## Logging in and out

If you need to execute custom code when a user logs in/logs out of a password-protected zone, you can implement a class `sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor`, which contains methods `public boolean logon(UserDetails user, HttpServletRequest request);` called at login and `public boolean logoff(UserDetails user, HttpServletRequest request);` called when the user logs out. Enter the name of your class in the conf. variable `stripesLogonLogoffInterceptorClass`.
