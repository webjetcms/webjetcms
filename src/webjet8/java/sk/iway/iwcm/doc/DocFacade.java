package sk.iway.iwcm.doc;

import static sk.iway.iwcm.Tools.isEmpty;
import static sk.iway.iwcm.Tools.isInteger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;

/**
 *  DocFacade.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.1.2010 18:28:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DocFacade
{

	List<GroupDetails> groups;
	boolean recursive;
	Identity user;
	StringBuilder sql = new StringBuilder();
	int pageSize;
	int currentPage;
	int allDocumentsCount = 0;

	public List<DocDetails> news (Map<String, Object> options, HttpServletRequest request)
	{
		user = (Identity)request.getSession().getAttribute(Constants.USER_KEY);
		recursive = Boolean.TRUE.equals(options.get("recursive"));
		pageSize = Integer.parseInt(options.get("per_page").toString());
		currentPage = 0;

		if (isInteger(request.getParameter("page")))
			currentPage = Math.max(Integer.parseInt(request.getParameter("page")) - 1, 0);
		sql.append("SELECT * FROM documents WHERE 1 AND group_id IN(");

		String group_ids = options.get("group_ids").toString();
		getGroupsFor(group_ids);

		for (GroupDetails group : groups)
		{

			if (group.isInternal() || !canUserAccess(group))
				continue;
			sql.append(group.getGroupId()).append(',');
		}

		sql = new StringBuilder(StringUtils.chomp(sql.toString(), ",")).append(')');
		if (options.get("where_sql") != null)
			sql.append(' ').append(options.get("where_sql").toString()).append(' ');

		sql.append(" ORDER BY ").append(options.get("order_by"));

		List<DocDetails> documents;
		Logger.debug(getClass(), "Getting documents: "+sql);
		documents = retrieveDocumentsMatchingSql();

		request.setAttribute("page_count", allDocumentsCount % pageSize == 0 ? allDocumentsCount / pageSize : allDocumentsCount / pageSize + 1);
		return new ArrayList<DocDetails>(documents.subList(0, documents.size() < pageSize ? documents.size() : pageSize));
	}

	private boolean canUserAccess(GroupDetails group)
	{
		if (isEmpty(group.getPasswordProtected()))
			return true;

		if (user == null)
			return false;

		List<String> userGroups = Arrays.asList(user.getUserGroupsIds().split(","));
		List<String> groupsWithAccess = Arrays.asList(group.getPasswordProtected().split(","));

		return CollectionUtils.containsAny(userGroups, groupsWithAccess);
	}

	private void getGroupsFor(String groupIds)
	{
		groups = new ArrayList<GroupDetails>();

		groupIds = groupIds.replace("+", ",");

		if (recursive)
			groupIds = GroupsDB.getRecursiveGroupsSqlIn(groupIds);

		for (String id : groupIds.split(","))
		{
			int groupId = Integer.parseInt(id);
			groups.add(GroupsDB.getInstance().findGroup(groupId));
		}
	}

	private List<DocDetails> retrieveDocumentsMatchingSql()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<DocDetails> documents = new ArrayList<DocDetails>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql.toString());
			rs = ps.executeQuery();
			Range pagingFilter = new IntRange(currentPage*pageSize + 1, (currentPage+1)*pageSize);
			while (rs.next())
			{
				if (pagingFilter.containsInteger(++allDocumentsCount))
					documents.add(DocDB.getDocDetails(rs, true, true));
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
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
		return documents;
	}
}
