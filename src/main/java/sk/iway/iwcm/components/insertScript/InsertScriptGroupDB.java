package sk.iway.iwcm.components.insertScript;

import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  InsertScriptGroupDB.java
 *
 *	DAO class for manipulating with InsertScriptGroupBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.09.2016 15:04:15
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InsertScriptGroupDB extends JpaDB<InsertScriptGroupBean>
{
	private static InsertScriptGroupDB instance;
	
	public InsertScriptGroupDB()
	{
		super(InsertScriptGroupBean.class);
	}
	
	public static InsertScriptGroupDB getInstance()
	{
		if(instance == null)
			instance = new InsertScriptGroupDB();
		
		return instance;
	}
		
	public static boolean deleteByInsertScriptBean(InsertScriptBean isb)
	{
		if(Tools.isEmpty(isb.getGroupIds()))
			return true;
		boolean success = true;
		for(InsertScriptGroupBean isgb:isb.getGroupIds())
		{
			if(!isgb.delete())
				success = false;
		}
			
		return success;
	}
	
	public List<InsertScriptGroupBean> findByGroupId(int groupId)
	{
		return JpaTools.findByMatchingProperty(InsertScriptGroupBean.class, "groupId", groupId);
	}
	
	public InsertScriptGroupBean findFirstByGroupId(int groupId)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptGroupBean.class, "groupId", groupId);
	}	
	public List<InsertScriptGroupBean> findByInsertScript(InsertScriptBean insertScript)
	{
		return JpaTools.findByMatchingProperty(InsertScriptGroupBean.class, "insertScriptBeanGr", insertScript);
	}
	
	public InsertScriptGroupBean findFirstByInsertScript(int insertScript)
	{
		return JpaTools.findFirstByMatchingProperty(InsertScriptGroupBean.class, "insertScript", insertScript);
	}
}