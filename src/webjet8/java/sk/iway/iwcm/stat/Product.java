package sk.iway.iwcm.stat;

/**
 *  Product.java
 *
 *		Represents a product found in User-Agent
 *		string
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 13.5.2011 11:28:31
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Product
{
	public String name;
	public String version;
	public String comment;
	
	public Product(){}
	
	public Product(String name)
	{
		this.name = name;
	}
	public Product(String name, String version)
	{
		this(name);
		this.version = version;
	}
}