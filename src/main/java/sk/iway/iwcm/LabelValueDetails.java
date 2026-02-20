package sk.iway.iwcm;

import java.io.Serializable;


/**
 *  Label Value pre nejaky select (genericka trieda, ked treba mat objekt
 *  meno=hodnota)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2003/03/07 10:43:37 $
 *@modified     $Date: 2003/03/07 10:43:37 $
 */

public class LabelValueDetails implements Comparable<LabelValueDetails>, Serializable
{
	private static final long serialVersionUID = 7126068510030215152L;

	/**
    *  Constructor for the LabelValueDetails object
    *
    */
   public LabelValueDetails() { }

   /**
    *  Constructor for the LabelValueDetails object
    *
    *@param  label  Description of the Parameter
    *@param  value  Description of the Parameter
    */
   public LabelValueDetails(String label, String value)
   {
      this.label = label;
      this.value = value;
   }

   @Override
	public int compareTo(LabelValueDetails that)
	{
   	return this.label.compareTo(that.getLabel());
   }
   
   @Override
	public boolean equals(Object o)
   {
   	if ((o == null) || !(o instanceof LabelValueDetails)) return false;
   	return 0 == compareTo((LabelValueDetails)o);
   }

	@Override
	public int hashCode()
	{
		return (null == label) ? 0 : label.hashCode();
	}

	private String label;
   private String value;
   private String value2;
   private int int1;
   private int int2;

   /**
    *  Gets the label attribute of the LabelValueDetails object
    *
    *@return    The label value
    */
   public String getLabel()
   {
      return label;
   }

   /**
    *  Sets the label attribute of the LabelValueDetails object
    *
    *@param  newLabel  The new label value
    */
   public void setLabel(String newLabel)
   {
      label = newLabel;
   }

   /**
    *  Sets the value attribute of the LabelValueDetails object
    *
    *@param  newValue  The new value value
    */
   public void setValue(String newValue)
   {
      value = newValue;
   }

   /**
    *  Gets the value attribute of the LabelValueDetails object
    *
    *@return    The value value
    */
   public String getValue()
   {
      return value;
   }

   /**
    *  Koli sortovaniu
    *
    *@return    Description of the Return Value
    */
   public String toString()
   {
      return (label);
   }
	/**
	 * @return Returns the value2.
	 */
	public String getValue2()
	{
		return value2;
	}
	/**
	 * @param value2 The value2 to set.
	 */
	public void setValue2(String value2)
	{
		this.value2 = value2;
	}

	public int getInt1()
	{
		return int1;
	}

	public void setInt1(int int1)
	{
		this.int1 = int1;
	}

	public int getInt2()
	{
		return int2;
	}

	public void setInt2(int int2)
	{
		this.int2 = int2;
	}
}
