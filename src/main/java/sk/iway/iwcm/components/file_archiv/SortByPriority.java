package sk.iway.iwcm.components.file_archiv;

import java.util.Comparator;

/**
 *	SortByKey.java
 *
 * Title			webjet4
 * Company		Interway s.r.o. (www.interway.sk)
 * Copyright 	Interway s.r.o. (c) 2001-2013
 * @author		$Author: prau $(prau)
 * @version		Revision: 1.3  1.12.2013
 * created		Date: 1.12.2013 11:15:05
 * modified   	Date: 1.12.2013 11:15:05
 */
public class SortByPriority implements Comparator<FileArchivatorBean>
{
	private boolean asc = false;
	public SortByPriority(boolean asc)
	{
		this.asc = asc;
	}
	
	public SortByPriority()
	{
		
	}
	
	@Override
	public int compare(FileArchivatorBean fab1, FileArchivatorBean fab2) 
	{
		if(asc)
			return fab1.getPriority() - fab2.getPriority();
		else
			return fab2.getPriority() - fab1.getPriority();
	}
	
}
