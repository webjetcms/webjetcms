package sk.iway.iwcm.system.stripes;

import net.sourceforge.stripes.util.CryptoUtil;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *  CSRF.java - trieda pre zakladnu ochranu pred CSRF utokmi
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2014
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 5.3.2014 16:54:56
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CSRF
{
	private static final String SESSION_KEY = "webjet-csrf-tokens-list";

	private static final String PARAMETER_NAME = "__token";


	/**
	 * Zapise input pole do formularu, vola sa priamo vo FormTag
	 * @param session
	 * @param out
	 */
	public static void writeCsrfTokenInputFiled(HttpSession session, JspWriter out)
	{
		try
		{
			out.write(getCsrfTokenInputFiled(session));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati CSRF input field ktory sa vlozi do formularu
	 * @param session
	 * @return
	 */
	public static String getCsrfTokenInputFiled(HttpSession session)
	{
		return getCsrfTokenInputFiled(session, true);
	}

	/**
	 * Vrati CSRF input field ktory sa vlozi do formularu
	 * @param session
	 * @param saveToSession - ak je true, aj sa ulozi na verifikaciu, false sa pouziva pre obfuscovanu verziu pre boty vo WriteTagu
	 * @return
	 */
	public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)
	{
		StringBuilder out = new StringBuilder();

		String token = getCsrfToken(session, saveToSession);

		out.append("<input type=\"hidden\" name=\"");
		out.append(PARAMETER_NAME);
		out.append("\" value=\"");
		out.append(token);
		out.append("\" />");

		return out.toString();
	}

	/**
	 * Vrati CSRF token pre vlozenie do formularu
	 * @param session
	 * @param saveToSession
	 * @return
	 */
	public static String getCsrfToken(HttpSession session, boolean saveToSession)
	{
		String token = CryptoUtil.encrypt(session.getId()+"-"+String.valueOf(Tools.getNow()));
		if (saveToSession) setTokenToSession(session, token);

		return token;
	}


	/**
	 * Verifikuje a nasledne zmaze aby sa znova nedal pouzit token v session
	 * @param request
	 * @return
	 */
	public static boolean verifyTokenAndDeleteIt(HttpServletRequest request)
	{
		if ("true".equals(request.getSession().getAttribute("WriteTag.disableSpamProtectionJavascript"))) {
			return true;
		}

		String[] parameterValues = request.getParameterValues(PARAMETER_NAME);
		if (parameterValues != null) {
			for (String parameterValue : parameterValues) {
				if (verifyToken(request.getSession(), parameterValue, true)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Verifikuje a nasledne zmaze aby sa znova nedal pouzit token v session
	 * @param session
	 * @param tokenValue - CSRF token
	 * @return
	 */
	public static boolean verifyTokenAndDeleteIt(HttpSession session, String tokenValue)
	{
		return verifyToken(session, tokenValue, true);
	}

	/**
	 * Verifikuje token v session, pouzit "iba" pri Ajax-ovych volaniach,
	 * Token sa nemaze po pouziti.
	 *
	 * @param request
	 * @return
	 */
	public static boolean verifyTokenAjax(HttpServletRequest request)
	{
		String tokenValue = request.getParameter(PARAMETER_NAME);
		return verifyTokenAjax(request.getSession(), tokenValue);
	}

	/**
	 * Verifikuje token v session, pouzit "iba" pri Ajax-ovych volaniach,
	 * Token sa nemaze po pouziti.
	 * @param session
	 * @param tokenValue - hodnota tokenu
	 * @return
	 */
	public static boolean verifyTokenAjax(HttpSession session, String tokenValue)
	{
		return verifyToken(session, tokenValue, false);
	}

	private static boolean verifyToken(HttpSession session, String tokenValue, boolean deleteToken)
	{
		if (Tools.isEmpty(tokenValue))
		{
			RequestBean.addError("CSRF token is empty");
			return false;
		}

		@SuppressWarnings("unchecked")
		List<String> tokenList = (List<String>)Tools.sessionGetAttribute(session, SESSION_KEY);
		if (tokenList == null)
		{
			RequestBean.addError("CSRF list in session is empty");
			return false;
		}

		boolean tokenFound = false;
		try
		{
			//tu nam hrozi zmena pola, pri standardnej praci pouzivatela by ale problem byt nemal
			int position = 0;
			for (position = 0; position<tokenList.size(); position++)
			{
				if (tokenValue.equals(tokenList.get(position)))
				{
					tokenFound = true;
					if(deleteToken)
						tokenList.remove(position);
					break;
				}
			}

			if (tokenFound==false)
			{
				RequestBean.addError("CSRF token not found, token="+tokenValue+" tokenList.size="+tokenList.size());
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			RequestBean.addError("CSRF token error "+e.getMessage());
		}

		return tokenFound;
	}

	private static void setTokenToSession(HttpSession session, String token)
	{
		@SuppressWarnings("unchecked")
		List<String> tokenList = (List<String>)Tools.sessionGetAttribute(session, SESSION_KEY);
		if (tokenList == null)
		{
			tokenList = new ArrayList<String>();
			Tools.sessionSetAttribute(session, SESSION_KEY, tokenList);
		}

		tokenList.add(token);

		try
		{
			if (tokenList.size()> Constants.getInt("csrfMaxTokensInSession"))
			{
				tokenList.remove(0);
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati meno parametra aj s CSRF tokenom pre vlozenie do url ako parameter
	 * @param session
	 * @param saveToSession
	 * @return
	 */
	public static String getCSRFTokenQuery(HttpSession session, boolean saveToSession){
		return PARAMETER_NAME + "=" + getCsrfToken(session,saveToSession);
	}

	/**
	 * Returns CSRF token parameter name
	 * @return
	 */
	public static String getParameterName() {
		return PARAMETER_NAME;
	}
}
