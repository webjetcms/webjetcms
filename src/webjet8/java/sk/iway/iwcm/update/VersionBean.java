package sk.iway.iwcm.update;

import sk.iway.iwcm.Tools;

/**
 *  VersionBean.java - informacie o verzii produktu,
 * kopia sk.iway.updater.VersionBean aby sa nemusel pribalovat updater package
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2004/05/31 15:09:01 $
 *@modified     $Date: 2004/05/31 15:09:01 $
 */
public class VersionBean
{
	private String product;
	private String version;
	private String note;
	/**
	 * @return Returns the note.
	 */
	public String getNote()
	{
		return note;
	}
	public String getNoteHTML()
	{
		return(Tools.replace(note, "\n", "<br>\n"));
	}
	/**
	 * @param note The note to set.
	 */
	public void setNote(String note)
	{
		this.note = note;
	}
	/**
	 * @return Returns the version.
	 */
	public String getVersion()
	{
		return version;
	}
	/**
	 * @param version The version to set.
	 */
	public void setVersion(String version)
	{
		this.version = version;
	}
	/**
	 * @return Returns the product.
	 */
	public String getProduct()
	{
		return product;
	}
	/**
	 * @param product The product to set.
	 */
	public void setProduct(String product)
	{
		this.product = product;
	}
}
