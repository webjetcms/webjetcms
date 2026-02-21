package sk.iway.iwcm;

import sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Map;

@WebServlet(name = "LogOff",
        urlPatterns = {"/logoff.do","/admin/logoff.do"}
)
public class LogoffServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.debug(LogoffServlet.class,"LogoffServlet  CALLED - GET");
        execute(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.debug(LogoffServlet.class,"LogoffServlet  CALLED - POST");
        execute(request,response);
    }

    public void execute(HttpServletRequest request,
                                 HttpServletResponse response)
            throws IOException, ServletException
    {

        HttpSession session = request.getSession();
        Identity user = UsersDB.getCurrentUser(session);
        if (user != null && user.isAdmin())
        {
            sk.iway.iwcm.stat.StatDB.addAdmin(request);
        }

        boolean csrfTokenIsValid = true;
        if (Constants.getBoolean("logoffRequireCsrfToken"))
        {
            String token = request.getParameter(CSRF.getParameterName());
            if (Tools.isEmpty(token)) token = request.getHeader("x-csrf-token");

            if (Tools.isEmpty(token) || CSRF.verifyTokenAndDeleteIt(session, token)==false || request.getMethod().equalsIgnoreCase("POST")==false)
            {
                Logger.error(getClass(), "CSRF token mismatch, logoff failed");
                csrfTokenIsValid = false;
            }
        }

        this.callLogonLogoffInterceptor(user, request);

        /**
         * install-custom actions before logoff
         * method takes one argument -  request
         * return value is not dealt with
         * format is fully.qualified.class.name.methodName
         */
        String beforeLogoffAction = Constants.getString("beforeLogoffAction");
        if(Tools.isNotEmpty(beforeLogoffAction))
        {
            try{
                int i = beforeLogoffAction.lastIndexOf('.');
                String beforeLogoffClass = beforeLogoffAction.substring(0, i);
                String beforeLogoffMethod = beforeLogoffAction.substring(i+1);
                Logger.debug(getClass(), "Before logoff action:" + beforeLogoffAction);
                Class<?> c = Class.forName(beforeLogoffClass);
                Method m;
                Class<?>[] parameterTypes = new Class<?>[] {HttpServletRequest.class};
                Object[] arguments = new Object[] {request};
                m = c.getDeclaredMethod(beforeLogoffMethod, parameterTypes);
                m.setAccessible(true); //NOSONAR
                m.invoke(null, arguments);
            }
            catch(Exception e){
                //sk.iway.iwcm.Logger.error(e);
                Logger.debug(getClass(), "Failed to perform before logoff action:" + beforeLogoffAction + " cause: " +e.getMessage());
            }
        }

        if (csrfTokenIsValid) {
            session.removeAttribute(Constants.USER_KEY);
            session.removeAttribute("loggeduser");
            session.invalidate();
            String admin = "";
            if (user != null && user.isAdmin()) admin = " (ADMIN)";
            if (user != null) Adminlog.add(Adminlog.TYPE_USER_LOGOFF, "LogoffAction - user"+admin+" successfully logged off: name = " + user.getLogin(), -1, -1);

            removePermanentLogon(request, response);

            //zmaz z cache user objekty
            if (user != null)
            {
                Cache cache = Cache.getInstance();
                cache.removeUserAllUserObjects(user);
            }
        }

        String logoffRedirect = Constants.getString("logoffRedirectUrl");

        // Forward control to the specified success URI
        if (request.getParameter("forwardDocId")!=null)
        {
            response.sendRedirect("/showdoc.do?docid="+ Tools.getIntValue(request.getParameter("forwardDocId"), 4));
            return;
        }
        else if (request.getParameter("forward")!=null)
        {
            String forward = Tools.replace(Tools.sanitizeHttpHeaderParam(request.getParameter("forward")), "//", "/");
            //security: allow only forwards to local addresses, if requested to external address use forwardDocId with redirect set in webpage
            if (forward.startsWith("/") && forward.contains("//")==false) {
                logoffRedirect = forward;
            }
        }
        //request.getRequestDispatcher("success").forward(request,response);
        //response.sendRedirect("/");

        if (Constants.getBoolean("oauth2_adminLogonAutoRedirect")) logoffRedirect = Tools.addParameterToUrl(logoffRedirect, "logoff", "true");
        response.sendRedirect(logoffRedirect);
    }

    private void callLogonLogoffInterceptor(UserDetails user, HttpServletRequest request)
    {
        String interceptorClassName = Constants.getString("stripesLogonLogoffInterceptorClass");

        if (Tools.isEmpty(interceptorClassName)) {
            interceptorClassName = Tools.getStringValue((String) request.getAttribute("afterSaveInterceptor"), "");
        }

        if (Tools.isNotEmpty(interceptorClassName)) {
            try
            {
                @SuppressWarnings("unchecked")
                Class<? extends AfterLogonLogoffInterceptor> interceptorClass = (Class<? extends AfterLogonLogoffInterceptor>) Class.forName(interceptorClassName);
                AfterLogonLogoffInterceptor interceptor = interceptorClass.getDeclaredConstructor().newInstance();

                interceptor.logoff(user, request);
            }
            catch (Exception e)
            {
                sk.iway.iwcm.Logger.error(e);
            }
        }
    }

    /**
     * @param request
     * @throws UnsupportedEncodingException
     * @author mhalas
     */
    private void removePermanentLogon(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException
    {
        try
        {
            String cookieName = "wjpltrack";

            Cookie[] cookies = request.getCookies();
            Cookie plcookie = null;
            if(cookies != null && cookies.length > 0)
            {
                for(Cookie c : cookies)
                {
                    if(c.getName().equals(cookieName)){
                        plcookie = c; break;
                    }
                }
                if(plcookie != null)
                {
                    String[] keyua = plcookie.getValue().split("-");
                    @SuppressWarnings("unchecked")
                    Map<String, String> plmap = (Map<String, String>)Constants.getServletContext().getAttribute(cookieName);
                    if (plmap != null) plmap.remove(keyua[0]);

                    //delete cookie from client
                    Cookie cookie = new Cookie(cookieName, URLEncoder.encode("deleted","UTF-8"));
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }

    }

}
