package sk.iway.iwcm.components.insertScript;

import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  InsertScriptDocDB.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.10.2016 13:43:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InsertScriptDocDB  extends JpaDB<InsertScriptDocBean> 
{
	private static InsertScriptDocDB instance;
	
	public InsertScriptDocDB()
	{
		super(InsertScriptDocBean.class);
	}
	
	public static InsertScriptDocDB getInstance()
	{
		if(instance == null)
			instance = new InsertScriptDocDB();
		
		return instance;
	}
	
	public List<InsertScriptDocBean> findByInsertScript(InsertScriptBean insertScript)
	{
		return JpaTools.findByMatchingProperty(InsertScriptDocBean.class, "insertScriptBeanDoc", insertScript);
	}
	
	public static boolean deleteByInsertScriptBean(InsertScriptBean isb)
	{
		if(Tools.isEmpty(isb.getDocIds()))
			return true;
		boolean success = true;
		for(InsertScriptDocBean isgb:isb.getDocIds())
		{
			if(!isgb.delete())
				success = false;
		}
			
		return success;
	}
}
