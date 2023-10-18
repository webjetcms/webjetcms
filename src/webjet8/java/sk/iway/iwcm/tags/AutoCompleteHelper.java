package sk.iway.iwcm.tags;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sk.iway.iwcm.SelectionFilter;
import sk.iway.iwcm.Tools;

/**
 *  AutoCompleteHelper.java
 *
 *		Methods usually requested by auto completers
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.7.2010 11:21:39
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class AutoCompleteHelper
{
	
	public static List<String> filterByContains(List<String> source, final String filterTerm)
	{
		return Tools.filter(source, new SelectionFilter<String>(){
			@Override
			public boolean fullfilsConditions(String candidate)
			{
				return candidate.toLowerCase().contains(filterTerm);
			}
		});
	}
	
	public static void sortByLeadingFirst(List<String> source, final String term)
	{
		Collections.sort(source, new Comparator<String>(){
			@Override
			public int compare(String key1, String key2){
				if (key1 == null || key2 == null) return 0;
				key1 = key1.toLowerCase();
				key2 = key2.toLowerCase();
				if (key1.startsWith(term) && key2.startsWith(term)) return key1.compareTo(key2);
				if (key1.startsWith(term)) return -1;
				if (key2.startsWith(term)) return 1;
				return key1.compareTo(key2);
			}
		});
	}
}
