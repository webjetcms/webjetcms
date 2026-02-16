package sk.iway.iwcm.tags;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Kontrola ci je prihlaseny pouzivatel (admin)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2010/01/20 11:15:08 $
 */
public final class CheckLogonTag extends TagSupport
{
	private static final long serialVersionUID = 6045363530295540861L;

	/**
    *  The key of the session-scope bean we look for.
    */
   private String name = Constants.USER_KEY;

   /**
    *  The page to which we should forward for the user to log on.
    */
   private String page = "/admin/logon.jsp";
   private boolean admin = false;

   //tu je mozne zadat zoznam pristupovych prav ktore sa maju kontrolovat ako menuWebpages|menuFbrowser|cmp_map
   private String perms = null;

   /**
    *  Return the bean name.
    *
    *@return    The name value
    */
   public String getName()
   {
      return (this.name);
   }


   /**
    *  Set the bean name.
    *
    *@param  name  The new bean name
    */
   public void setName(String name)
   {
      this.name = name;
   }


   /**
    *  Return the forward page.
    *
    *@return    The page value
    */
   public String getPage()
   {
      return (this.page);
   }


   /**
    *  Set the forward page.
    *
    *@param  page  The new forward page
    */
   public void setPage(String page)
   {
      this.page = page;
   }


   // ------------------------------------------------------- Public Methods

   /**
    *  Defer our checking until the end of this tag is encountered.
    *
    *@return                   Description of the Return Value
    *@exception  JspException  if a JSP exception has occurred
    */
   @Override
   public int doStartTag() throws JspException
   {
      return (SKIP_BODY);
   }


   /**
    *  Perform our logged-in user check by looking for the existence of a
    *  session scope bean under the specified name. If this bean is not present,
    *  control is forwarded to the specified logon page.
    *
    *@return                   Description of the Return Value
    *@exception  JspException  if a JSP exception has occurred
    */
   @Override
   @SuppressWarnings("unused")
   public int doEndTag() throws JspException
   {
      // Is there a valid user logged on?
      boolean valid = false;
      HttpSession session = pageContext.getSession();
      Identity user = UsersDB.getCurrentUser(session);
      if ((session != null) && (user != null))
      {
         //Logger.error(this,"Session nie je null");
         if (user.isValid())
         {
            if (admin == true)
            {
               if (user.isAdmin())
               {
                  valid = true;
               }
            }
            else
            {
               valid = true;
            }
         }
      }

      //Logger.error(this,"valid="+valid+" name="+name);

      // Forward control based on the results
      if (valid)
      {
         //user je prihlaseny
         if (Tools.isNotEmpty(perms))
         {
            boolean isAnyEnabled = false;
            String persmArr[] = Tools.getTokens(perms, "|", true);
            for (String perm : persmArr)
            {
               if (MenuTag.isEnabledPerms(perms, user))
               {
                  isAnyEnabled = true;
                  break;
               }
            }

            if (isAnyEnabled == false)
            {
               try
               {
                  //user nema pravo na modul, presmeruj ho na /admin/ (ak sa este da)
                  HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
                  response.sendRedirect("/admin/403.jsp");
               }
               catch (Exception e)
               {

               }
               return SKIP_PAGE;
            }
         }

         return (EVAL_PAGE);
      }
      else
      {
         try
         {
         	String domainController = AuthenticationFilter.getDomainController();
				if (Tools.isNotEmpty(domainController))
				{
					HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
					response.sendRedirect("/ntlm/logon.do?admin=true");
					return (SKIP_PAGE);
				}
				else
				{
					pageContext.forward(page);
				}
         }
         catch (Exception e)
         {
            throw new JspException(e.toString());
         }
         return (SKIP_PAGE);
      }
   }


   /**
    *  Release any acquired resources.
    */
   @Override
   public void release()
   {
      super.release();
      this.name = Constants.USER_KEY;
      this.page = "/logon.jsp";
      this.admin = false;
      this.perms = null;
   }

   /**
    *  Sets the admin attribute of the CheckLogonTag object
    *
    *@param  admin  The new admin value
    */
   public void setAdmin(boolean admin)
   {
      this.admin = admin;
   }

   /**
    *  Gets the admin attribute of the CheckLogonTag object
    *
    *@return    The admin value
    */
   public boolean isAdmin()
   {
      return admin;
   }

   public String getPerms()
   {
      return perms;
   }

   public void setPerms(String perms)
   {
      this.perms = perms;
   }

}
