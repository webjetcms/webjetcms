package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 *  Vylistuje zoznam groups a dokumenty v danej grupe (pre groupslist.jsp)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      $Date: 2004/02/16 16:44:23 $
 *@modified     $Date: 2004/02/16 16:44:23 $
 */
public class SitemapAction
{
	public static final String SESSION_LINK_GROUPID="session_link_groupid";

	public static String doTree(HttpServletRequest request)
	{
		int groupId = Constants.getInt("rootGroupId");
		try
		{
			if (request.getParameter("rootId") != null)
			{
				groupId = Integer.parseInt(request.getParameter("rootId"));
			}
			else
			{
				if (request.getAttribute("groupid") != null)
				{
					groupId = Integer.parseInt((String) request.getAttribute("groupid"));
				}
				else if (request.getParameter("groupid") != null)
				{
					groupId = Integer.parseInt(request.getParameter("groupid"));
				}
			}
		}
		catch (Exception ex)
		{

		}

		String forward="sk";
		String pForward = request.getParameter("forward");
		if (pForward!=null && pForward.endsWith(".jsp"))
		{
			forward = "/templates/"+pForward;
		}

		request.setAttribute("group_id", Integer.toString(groupId));

		GroupsDB groupsDB = GroupsDB.getInstance();

		List<GroupDetails> myGroups = new ArrayList<>();

		//prekopirovanie zoznamu a vykonanie uprav

		int rootGroup = groupsDB.getRoot(groupId);

		if (request.getAttribute("groupid")!=null)
		{
			//je to nastavene z JSP stranky, takze toto je aj ROOT skupina
			rootGroup = Integer.parseInt((String) request.getAttribute("groupid"));
		}
		Logger.println(SitemapAction.class,"Action: rootGroup = " + rootGroup);
		for (GroupDetails groupOrig : groupsDB.getGroupsTree(rootGroup, true, false))
		{
			if (groupOrig == null || groupOrig.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_HIDDEN || groupOrig.isInternal() == true)
			{
				continue;
			}
			GroupDetails parentGroup = groupsDB.getGroup(groupOrig.getParentGroupId());
			if (parentGroup != null && parentGroup.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_NOSUB) continue;

			GroupDetails groupNew = new GroupDetails();
			groupNew.setGroupId(groupOrig.getGroupId());
			groupNew.setParentGroupId(groupOrig.getParentGroupId());
			if (groupNew.getGroupId()==rootGroup)
			{
				groupNew.setParentGroupId(0);
			}
			if (groupNew.getGroupId()==groupId)
			{
				groupNew.setGroupName("<span class=sitemap-selected-dir>"+groupOrig.getGroupName()+"</span>");
			}
			else
			{
				groupNew.setGroupName(groupOrig.getGroupName());
			}
			//groupNew.setGroupName(Tools.replace(groupNew.getGroupName(), "'", "&#39;"));
			groupNew.setGroupName(groupNew.getGroupName().replace('\'', '´'));
			myGroups.add(groupNew);
		}

		//set data for group tree
		request.setAttribute("groups", myGroups);

		request.setAttribute("path", groupsDB.getPath(groupId));
		GroupDetails actualGroup = groupsDB.getGroup(groupId);

		List<DocDetails> docs = new ArrayList<>();

		//select documents within given group
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(request);
			String sql;
			DocDetails doc;


			sql = "SELECT d.* FROM documents d WHERE group_id=? AND available=? AND show_in_menu=? ORDER BY sort_priority, title";
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, groupId);
			ps.setBoolean(2, true);
			ps.setBoolean(3, true);
			rs = ps.executeQuery();

			boolean at_least_one = false;

			while (rs.next())
			{
				doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				//doc.setData(dbUtil.getDbString(rs, "data"));
				doc.setDateCreated(DB.getDbTimestamp(rs, "date_created"));
				doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
				doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));
				doc.setAuthorId(rs.getInt("author_id"));
				doc.setSearchable(rs.getBoolean("searchable"));
				doc.setGroupId(rs.getInt("group_id"));
				doc.setAvailable(rs.getBoolean("available"));
				doc.setPasswordProtected(DB.getDbString(rs, "password_protected"));
				/** @todo NACITAJ PRISTUPOVE PRAVA */

				doc.setCacheable(rs.getBoolean("cacheable"));
				doc.setExternalLink(DB.getDbString(rs, "external_link"));
				doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
				doc.setTempId(rs.getInt("temp_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setNavbar(DB.getDbString(rs, "navbar"));
				doc.setSortPriority(rs.getInt("sort_priority"));

				if (actualGroup.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_NORMAL ||
							(actualGroup.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_ONLYDEFAULT &&
										doc.getDocId() == actualGroup.getDefaultDocId())
				)
				{
					docs.add(doc);
					at_least_one = true;
				}

			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			if (at_least_one == false)
			{
				request.setAttribute("ziadne_dokumenty", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>V tejto skupine nie sú žiadne dokumenty.</b><br>");
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		request.setAttribute("docs", docs);

		return (forward);
	}
}
