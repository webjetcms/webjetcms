package sk.iway.iwcm;

/**
 *  Pd4mlOptions.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: Jun 19, 2009 12:35:48 PM
 *@modified     $Date: 2009/06/19 11:37:32 $
 */
public class Pd4mlOptions
{
	private boolean fitPageVertically;
	private int htmlWidth;
	public boolean isFitPageVertically()
	{
		return fitPageVertically;
	}

	public void setFitPageVertically(boolean fitPageVertically)
	{
		this.fitPageVertically = fitPageVertically;
	}

	/**
	 * @param htmlWidth The htmlWidth to set.
	 */
	public void setHtmlWidth(int htmlWidth)
	{
		this.htmlWidth = htmlWidth;
	}

	/**
	 * @return Returns the htmlWidth.
	 */
	public int getHtmlWidth()
	{
		return htmlWidth;
	}
}
