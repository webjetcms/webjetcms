package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 *  Vytvori stromovu strukturu adresarov a dokumentov
 *
 *@Title        e-iwcm.sk
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2004/01/13 07:50:25 $
 *@modified     $Date: 2004/01/13 07:50:25 $
 */
public class DocTreeDB extends DB
{
	private List<DocTreeDetails> docs;
	int counter = 0;
	private String navbar;

	DocTreeDetails first;


	/**
	 *  Constructor for the DocTreeDB object
	 *
	 *@param  group_id    Description of the Parameter
	 *@param  groupsDB    Description of the Parameter
	 *@param  serverName  Description of the Parameter
	 */
	public DocTreeDB(int group_id, GroupsDB groupsDB, String serverName)
	{
		docs = new ArrayList<>();
		//ArrayList a_grp = groupsDB.getGroupsTree(group_id, true, false);
		List<GroupDetails> a_grp = new ArrayList<>();
		a_grp.add(groupsDB.findGroup(group_id));
		groupsDB.getGroupsTree(group_id, a_grp, false, true);

		DocTreeDetails det;

		Connection db_conn = null;
		try
		{
			db_conn = DBPool.getConnection(serverName);
			int max_group_id = 0;
			//najskor vytvorim adresare a potom tam nastrkam dokumenty
			boolean is_first = true;
			for (GroupDetails group : a_grp)
			{
				if (group == null || group.getMenuType()==GroupDetails.MENU_TYPE_HIDDEN || group.isInternal() == true)
				{
					continue;
				}
				det = new DocTreeDetails();
				det.setId(group.getGroupId());
				//ak je to prvy nastav mu parenta na 0
				if (is_first)
				{
					det.setParent(0);
					navbar = groupsDB.getNavbar(group.getGroupId());
					first = det;
				}
				else
				{
					if (group.getParentGroupId() == first.getId())
					{
						det.setParent(0);
					}
					else
					{
						det.setParent(group.getParentGroupId());
					}
				}

				det.setName(group.getGroupName());
				if (group.getDefaultDocId() > 0)
				{
					det.setLink("/showdoc.do?docid=" + group.getDefaultDocId());
				}
				else
				{
					det.setLink("#");
					//det.setName("" + det.getName() + "</font>");
				}
				if (!is_first)
				{
					//set whole link
					det.setJsTree("fld" + det.getId() + "=insFld(fld" + det.getParent() + ", gFld(\"" + det.getName() + "\", \"" + det.getLink() + "\"))");
					docs.add(det);
				}
				is_first = false;
			}

			counter = max_group_id + 1000;
			is_first = true;
			for (GroupDetails group : a_grp)
			{
				if (group == null || group.getMenuType()==GroupDetails.MENU_TYPE_HIDDEN || group.isInternal() == true)
				{
					continue;
				}
				/*
				 *  if (is_first)
				 *  {
				 *  load(group.getGroupId(), db_conn, group.getDefaultDocId(), 0);
				 *  }
				 *  else
				 *  {
				 */
				if (group.getMenuType()==GroupDetails.MENU_TYPE_NORMAL)
				{
					load(group.getGroupId(), db_conn, group.getDefaultDocId(), group.getGroupId());
				}
				//}
			}
			if (db_conn != null && !db_conn.isClosed())
				db_conn.close();
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
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  group_id           Description of the Parameter
	 *@param  db_conn            Description of the Parameter
	 *@param  group_default_doc  Description of the Parameter
	 *@param  doc_parent         Description of the Parameter
	 *@exception  Exception      Description of the Exception
	 */
	public void load(int group_id, Connection db_conn, int group_default_doc, int doc_parent) throws Exception
	{
		DocTreeDetails det;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM documents WHERE group_id=? AND available=? AND show_in_menu=? ORDER BY sort_priority, title";
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, group_id);
			ps.setBoolean(2, true);
			ps.setBoolean(3, true);
			rs = ps.executeQuery();

			int doc_id;
			while (rs.next())
			{
				det = new DocTreeDetails();
				det.setId(counter);
				counter++;
				det.setName(getDbString(rs, "title"));
				doc_id = rs.getInt("doc_id");
				det.setLink("/showdoc.do?docid=" + doc_id);
				//det.setParent(doc_parent);
				if (doc_parent == first.getId())
				{
					det.setParent(0);
				}
				else
				{
					det.setParent(doc_parent);
				}

				if (doc_id != group_default_doc)
				{
					det.setJsTree("insDoc(fld" + det.getParent() + ", gLnk(2, \"" + det.getName() + "\", \"" + det.getLink() + "\"))");
					docs.add(det);
				}
			}

			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception sqle)
		{
			Logger.error(this,"GroupsDB spadlo spojenie" + sqle.getMessage());
			sk.iway.iwcm.Logger.error(sqle);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	/**
	 *  vrati zoznam dokumentov
	 *
	 *@return    The docs value
	 */
	public List<DocTreeDetails> getDocs()
	{
		return (docs);
	}

	/**
	 *  nastavi navigacnu listu (default bude na nazov korenoveho objektu)
	 *
	 *@param  newNavbar  The new navbar value
	 */
	public void setNavbar(String newNavbar)
	{
		navbar = newNavbar;
	}

	/**
	 *  vrati navigacnu listu
	 *
	 *@return    The navbar value
	 */
	public String getNavbar()
	{
		return navbar;
	}

	/**
	 *  vrati korenovy objekt
	 *
	 *@return    The first value
	 */
	public DocTreeDetails getFirst()
	{
		return (first);
	}
}
