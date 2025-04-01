package sk.iway.iwcm.calendar;

/**
 *  <p>
 *
 *  Title: IWCM sample web site</p> <p>
 *
 *  Description:</p> <p>
 *
 *  Copyright: Copyright (c) 2001</p> <p>
 *
 *  Company: Interway s.r.o. (www.interway.sk)</p>
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       not attributable
 *@version      1.0
 *@created      Utorok, 2003, júl 22
 *@modified     $Date: 2003/07/22 17:55:18 $
 */

public class EventTypeDetails
{
   private int typeId = -1;
   private String name;
   private int schvalovatelId = -1;

   /**
    *  Gets the {3} attribute of the EventTypeDetails object
    *
    *@return    The {3} value
    */
   public int getTypeId()
   {
    return typeId;
   }

   /**
    *  Sets the {3} attribute of the EventTypeDetails object
    *
    *@param  typeId
    */
   public void setTypeId(int typeId)
   {
    this.typeId = typeId;
   }

   /**
    *  Gets the {3} attribute of the EventTypeDetails object
    *
    *@return    The {3} value
    */
   public String getName()
   {
      return name;
   }

   /**
    *  Sets the {3} attribute of the EventTypeDetails object
    *
    *@param  name  The new {3} value
    */
   public void setName(String name)
   {
      this.name = name;
   }

	public int getSchvalovatelId()
	{
		return schvalovatelId;
	}

	public void setSchvalovatelId(int schvalovatelId)
	{
		this.schvalovatelId = schvalovatelId;
	}
}
