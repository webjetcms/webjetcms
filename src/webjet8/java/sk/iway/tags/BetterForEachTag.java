package sk.iway.tags;

import org.apache.taglibs.standard.tag.rt.core.ForEachTag;

/**
 * 
 *  BetterForEachTag.java
 *  
 *  Rozsiruje funkcnost standardneho JSTL forEach tagu, pridava atribut 'type'
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author 		mbocko
 *@version      7
 *@created      1.3.2015 19:56:44
 */
public class BetterForEachTag extends ForEachTag
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected String type;

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}



}
