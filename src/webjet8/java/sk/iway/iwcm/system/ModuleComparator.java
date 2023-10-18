package sk.iway.iwcm.system;

import java.util.Comparator;

import sk.iway.iwcm.i18n.Prop;

/**
 *  ModuleComparator.java
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 23.1.2007 10:51:32
 *@modified     $Date: 2007/01/23 14:24:43 $
 */
public class ModuleComparator implements Comparator<ModuleInfo>
{
	private Prop prop;
	
	public ModuleComparator(Prop prop)
	{
		this.prop = prop;
	}
	@Override
	public int compare(ModuleInfo m1, ModuleInfo m2)
	{
		String name1 = prop.getText(m1.getNameKey());
		String name2 = prop.getText(m2.getNameKey());
		
		return(name1.compareTo(name2));
	}
}
