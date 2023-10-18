package sk.iway.iwcm.components.blog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.UserDetails;

/**
 * BlogRightsSetter.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: marosurbanec $
 *@version $Revision: 1.3 $
 *@created Date: 18.4.2011 12:42:55
 *@modified $Date: 2004/08/16 06:26:11 $
 */
class BlogRightsSetter
{
	private UserDetails usr;

	public BlogRightsSetter(UserDetails blogger)
	{
		usr = blogger;
	}

	public void setRights()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, usr.getUserId());
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO user_disabled_items VALUES(?,?)");

			forbidAllModules(ps);
			forbidTreeManipulation(ps);
			enableBlog(db_conn);

			db_conn.close();
			ps = null;
			db_conn = null;
		}catch (Exception ex){
			sk.iway.iwcm.Logger.error(ex);
		}finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2){}
		}
	}

	private void forbidAllModules(PreparedStatement ps) throws SQLException
	{
		for (Object modul : Modules.getInstance().getModules())
		{
			if (modul == null)
				continue;
			ModuleInfo theModule = (ModuleInfo) modul;
			ps.setInt(1, usr.getUserId());
			ps.setString(2, theModule.getItemKey());
			ps.executeUpdate();
			// zakaz TOP most modulu nezakaze automaticky aj jeho sub moduly,
			// treba ich zakazat rucne
			if (theModule.getSubmenus() == null)
				continue;
			for (ModuleInfo subModule : theModule.getSubmenus())
			{
				if (subModule.getItemKey() == null)
					continue;
				ps.setInt(1, usr.getUserId());
				ps.setString(2, subModule.getItemKey());
				ps.executeUpdate();
			}
		}
	}

	private void forbidTreeManipulation(PreparedStatement ps) throws SQLException
	{
		ps.setInt(1, usr.getUserId());
		ps.setString(2, "cmp_blog_admin");
		ps.executeUpdate();
		ps.setInt(1, usr.getUserId());
		ps.setString(2, "editDir");
		ps.executeUpdate();
		ps.close();
	}

	private void enableBlog(Connection db_conn) throws SQLException
	{
		PreparedStatement ps;
		ps = db_conn.prepareStatement("DELETE FROM user_disabled_items WHERE user_id = ? AND item_name = ?");
		ps.setInt(1, usr.getUserId());
		ps.setString(2, "cmp_blog");
		ps.executeUpdate();
		ps.setString(2, "addPage");
		ps.executeUpdate();
		ps.close();
	}
}