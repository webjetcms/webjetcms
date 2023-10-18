package sk.iway.iwcm.doc;

import java.util.StringTokenizer;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
;

/**
 *  Vlastnosti perex skupiny
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2007/09/07 13:39:29 $
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class PerexGroupBean
{
   private int perexGroupId;
   private String perexGroupName;
   private String[] relatedPages;

   /**
    * V akych skupinach je mozne perex skupinu pouzit
    */
   private String availableGroups;


	/**
	 * @return Returns the perexGroupName.
	 */
	public String getPerexGroupName()
	{
		return perexGroupName;
	}

	public String getPerexGroupNameId()
	{
		if (Constants.getBoolean("perexGroupIncludeId")==false) return getPerexGroupName();

		return perexGroupName+" (id:"+perexGroupId+")";
	}

	/**
	 * @param perexGroupName The perexGroupName to set.
	 */
	public void setPerexGroupName(String perexGroupName)
	{
		this.perexGroupName = perexGroupName;
	}

	/**
	 * @return Returns the relatedPages.
	 */
	public String[] getRelatedPages()
	{
		return relatedPages;
	}

	/**
	 * @param relatedPages The relatedPages to set.
	 */
	public void setRelatedPages(String pages)
	{
		int size;
		StringTokenizer st;
		int index = 0;
		String[] relatedPages = new String[0];

		try
		{

			if (Tools.isNotEmpty(pages))
			{
				st = new StringTokenizer(pages, ",");
				size = st.countTokens();
				if (size > 0)
				{
					//Logger.println(this,"-------\nRelatedPages: "+pages);
					relatedPages = new String[size];
					while (st.hasMoreTokens())
					{
						if ( index < size)
						{
							relatedPages[index] = st.nextToken().trim();
							//Logger.println(this,relatedPages[index]);
							index++;
						}
					}
				}
			}

			//relatedPages = RelatedPagesDB.getTokens(pages, ",");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		this.relatedPages = relatedPages;
	}

	/**
	 * @return Returns the perexGroupId.
	 */
	public int getPerexGroupId()
	{
		return perexGroupId;
	}
	/**
	 * @param perexGroupId The perexGroupId to set.
	 */
	public void setPerexGroupId(int perexGroupId)
	{
		this.perexGroupId = perexGroupId;
	}

	public String getAvailableGroups()
	{
		return availableGroups;
	}

	public int[] getAvailableGroupsInt()
	{
		return Tools.getTokensInt(availableGroups, ",");
	}

	public void setAvailableGroups(String availableGroups)
	{
		this.availableGroups = availableGroups;
	}
}
