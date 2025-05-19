package sk.iway.iwcm.stat.heat_map;



/**
 *  Click.java
 *  
 *  	Represents a click event captured by tracking javascript. 
 *  	Used to recreate a heat map made of those clicks.  
 *  
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 28.05.2010 15:30:16
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class Click
{
	int documentId;
	int dayOfMonth;
	int x;
	int y;
	
	public int getDocumentId()
	{
		return documentId;
	}

	public Click setDocumentId(int documentId)
	{
		this.documentId = documentId;
		return this;
	}

	public int getDayOfMonth()
	{
		return dayOfMonth;
	}

	public Click setDayOfMonth(int dayOfMonth)
	{
		this.dayOfMonth = dayOfMonth;
		return this;
	}

	public int getX()
	{
		return x;
	}

	public Click setX(int x)
	{
		this.x = x;
		return this;
	}

	public int getY()
	{
		return y;
	}

	public Click setY(int y)
	{
		this.y = y;
		return this;
	}


}