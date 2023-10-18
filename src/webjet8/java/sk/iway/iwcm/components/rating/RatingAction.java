package sk.iway.iwcm.components.rating;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.StatDB;

/**
 *  RatingAction.java - ulozenie ratingu do DB
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: bhric $
 */
public class RatingAction extends Action
{
	@Override
	public ActionForward execute(ActionMapping mapping,
				ActionForm form,
				HttpServletRequest request,
				HttpServletResponse response)
				 throws IOException, ServletException
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			HttpSession session = request.getSession();
			RatingBean rBean = (RatingBean)form;
			int fromDocId = 4;
			String errors = "";
			String action = "";
			boolean saveOK = false;
			int rateAgainCycleInHours = session.getAttribute("rateAgainCycleInHours"+rBean.getDocId()) != null ? Tools.getIntValue((Integer)session.getAttribute("rateAgainCycleInHours"+rBean.getDocId()), 0) : 0;
			if (request.getParameter("fromDocId") != null)
			{
				fromDocId = Integer.parseInt(request.getParameter("fromDocId"));
			}

			if (request.getParameter("action") != null)
			{
				action = request.getParameter("action");
			}

			Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
			if (user != null)
			{
				// rBean ked sa uz vyssie pouziva
				//if (rBean != null)
				//{
					rBean.setUserId(user.getUserId());

					if ("save".equals(action) && RatingDB.getRatingByUserByDoc(user.getUserId(), rBean.getDocId(), rateAgainCycleInHours) == null)
					{

						saveOK = RatingDB.saveRating(rBean);

						//zvysim userovi rating rank
						db_conn = DBPool.getConnection();
						ps = db_conn.prepareStatement("UPDATE users SET rating_rank=rating_rank+1 WHERE user_id=?");
						ps.setInt(1, user.getUserId());
						ps.execute();
						ps.close();
						db_conn.close();
						ps = null;
						db_conn = null;
					}//update je len v pripade ze sa hlasuje len raz za stranku
					else if ("update".equals(action) && rateAgainCycleInHours < 1)
					{
						RatingBean rb = RatingDB.getRatingByUserByDoc(user.getUserId(), rBean.getDocId(), rateAgainCycleInHours);
						rBean.setRatingId(rb.getRatingId());
						saveOK = RatingDB.saveRating(rBean);
					}
			//	}
			}
			else
			{
				int userId = (int)getBrowserId(request, null) + 10000;
				RatingBean userRatingBean = RatingDB.getRatingByUserByDoc(userId, rBean.getDocId(), rateAgainCycleInHours);
				if ("save".equals(action) || ("update".equals(action) && rateAgainCycleInHours < 1))
				{
					rBean.setUserId(userId);
					if (userRatingBean != null) rBean.setRatingId(userRatingBean.getRatingId());
					saveOK = RatingDB.saveRating(rBean);
				}
			}

			String forward = request.getParameter("forward");

			if (saveOK)
			{
				if (Tools.isNotEmpty(forward)) response.sendRedirect(addParameterToUrl(forward, "showRate", "true"));
				else response.sendRedirect("/showdoc.do?docid="+fromDocId+"&showRate=true");
			}
			else
			{
				if (Tools.isNotEmpty(forward)) response.sendRedirect(addParameterToUrl(forward, "error", errors));
				else response.sendRedirect("/showdoc.do?docid="+fromDocId+"&error="+errors);
			}
		}
		catch(Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return(null);
	}

	/**
	 * Metoda vrati aktualne browserId, prip. priradi nove na zaklade typu pouzivatela (stroj, registrovany, neregistrovany).
	 *
	 * @param request
	 * @param response
	 *
	 * @return
	 */
	public static long getBrowserId(HttpServletRequest request, HttpServletResponse response)
	{
		return (StatDB.getBrowserId(request, response));
	}

	public static String addParameterToUrl(String url, String paramName, String paramValue)
	{
		if (url == null) return("?"+paramName+"="+paramValue);

		if (url.indexOf("?"+paramName+"=")!=-1 || url.indexOf("&"+paramName+"=")!=-1 || url.indexOf("&amp;"+paramName+"=")!=-1)
		{
			return(url);
		}

		if (url.indexOf('?')==-1)
		{
			return(url + "?" + paramName + "=" + paramValue);
		}
		return(url + "&amp;" + paramName + "=" + Tools.URLEncode(paramValue));
	}
}
