package sk.iway.iwcm.tags;

import java.util.StringTokenizer;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;


/**
 *  Kontrola ci dany user ma pravo na dane menu
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2007/04/11 09:26:38 $
 */

public final class MenuTag extends TagSupport
{
	private static final long serialVersionUID = 1L;

	/**
	 *  The page to which we should forward for the user to log on.
	 */
	private String name = null;
	private String notName = null;

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
		// Is there a valid user logged on?
		HttpSession session = pageContext.getSession();
		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

		//Logger.error(this,"valid="+valid+" name="+name);

		// Forward control based on the results
		//Logger.println(this,"name="+name+" notName="+notName);
		if (user!=null && user.isValid())
		{
			if (name != null)
			{
				if (isEnabledPerms(name, user))
				{
					return(EVAL_BODY_INCLUDE);
				}
			}
			if (notName != null)
			{
				//v not name robime AND
				StringTokenizer st = new StringTokenizer(notName, "|");
				boolean maNejakePovolene = false;
				while (st.hasMoreTokens())
				{
					if (user.isEnabledItem(st.nextToken().trim()))
					{
						maNejakePovolene = true;
						break;
					}
				}

				if (maNejakePovolene==false)
				{
					//robime redirect na welcome
					return(EVAL_BODY_INCLUDE);
				}
			}
		}
		else
		{
			try
			{
				pageContext.forward("/admin/logon.jsp");
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			Logger.debug(this,"MENU: user je null");
		}
		return(SKIP_BODY);
	}

	/**
	 * Overi, ci ma user aspon jedno zo zadanych prav (oddelenych znakom |), ak ano, vrati true
	 * @param perms
	 * @param user
	 * @return
	 */
	public static boolean isEnabledPerms(String perms, Identity user)
	{
		//v name robime OR
		StringTokenizer st = new StringTokenizer(perms, "|");
		while (st.hasMoreTokens())
		{
			String perm = st.nextToken();
			if (Tools.isEmpty(perm)) continue;
			if (perm.startsWith("gid:")) {
				//specialna moznost pouzitia <iwcm:menu pre kontrolu user skupin <iwcm:menu name="gid:34-Editacia profilu klienta v systeme|gid:33-Vytvorenie profilu klienta v systeme">
				try
				{
					//skus ziskat id
					int gid = Tools.getIntValue(perm.substring(4, perm.indexOf("-")).trim(), -1);
					if (gid > 0) {
						if (user.isInUserGroup(gid)) return true;
					};
				}
				catch (Exception e)
				{
				}
			}
			else if ((user.isAdmin() || UsersDB.checkUserPerms(user, "admin|editableGroupsNotEmpty")) && user.isEnabledItem(perm.trim()))
			{
				return true;
			}
		}

		return false;
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
	public int doEndTag() throws JspException
	{
		return(EVAL_PAGE);
	}


	/**
	 *  Release any acquired resources.
	 */
	@Override
	public void release()
	{
		//Logger.println(this,"RELEASE: " + name + ";"+notName);
		super.release();
		this.name = null;
		this.notName = null;

	}

	public String getNotName()
	{
		return notName;
	}
	public void setNotName(String notName)
	{
		this.notName = notName;
	}


}
