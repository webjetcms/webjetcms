package sk.iway.iwcm.filebrowser;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  Bean popisujuci atribut suboru (vratane definicii)
 *
 *@Title        WebJET 4.0
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Streda, 2003, október 15
 *@modified     $Date: 2004/02/20 21:38:21 $
 */
public class FileAtrBean
{
	private int atrId;
	private String atrName;
	private String atrDescription;
	private String atrDefaultValue;
	private int atrType;
	private int valueInt;
	private boolean valueBool;
	private String valueString;
	private int orderPriority;
	private String atrGroup;
	private String trueValue;
	private String falseValue;
	private String link;
	private String fileName;
	private static final DecimalFormat decimalFormat;

	static
	{
		decimalFormat = new DecimalFormat("0.##");
	}

	/**
	 * vygeneruje HTML podobu atributu
	 * @return
	 */
	public String getHtml()
	{
		StringBuilder ret = new StringBuilder();
		String actualValue = "";
		//ak mame docid, znamena to, ze uz to ma v DB nejaku hodnotu
		if (link!=null && link.length() > 1)
		{
			actualValue = getValue();
		}
		StringTokenizer st = new StringTokenizer(atrDefaultValue, ",");
		if (st.countTokens()>1)
		{
			ret.append("<select name='atr_").append(atrId).append("'>").append("<option value=''></option>");
			String tmp, selected;
			while (st.hasMoreTokens())
			{
				tmp = st.nextToken().trim();
				if (atrType==FileAtrDB.TYPE_BOOL)
				{
					if ("true".equalsIgnoreCase(tmp) || "yes".equalsIgnoreCase(tmp))
					{
						tmp = "true";
					}
					else
					{
						tmp = "false";
					}
				}
				if (tmp.compareTo(actualValue)==0)
				{
					selected = " selected";
				}
				else
				{
					selected = "";
				}
				ret.append("<option value='").append(tmp).append('\'').append(selected).append('>').append(tmp).append("</option>");
			}
			ret.append("</select>");
		}
		else
		{
			if (link==null || link.length() < 1)
			{
				actualValue = atrDefaultValue;
			}
			ret.append("<input name='atr_").append(atrId).append("' value='").append(actualValue).append("'>");
		}
		return (ret.toString());
	}

	/**
	 * ziska hodnotu ako string
	 * @return
	 */
	public String getValue()
	{
		if (atrType==FileAtrDB.TYPE_INT)
		{
			return(Integer.toString(valueInt));
		}
		else if (atrType==FileAtrDB.TYPE_BOOL)
		{
			return(Boolean.toString(valueBool));
		}
		return(valueString);
	}

	public String getValueHtml()
	{
		if (atrType==FileAtrDB.TYPE_INT)
		{
			return(Integer.toString(valueInt));
		}
		else if (atrType==FileAtrDB.TYPE_BOOL)
		{
			if (valueBool == true)
			{
				if (trueValue!=null && trueValue.length()>0)
				{
					return(trueValue);
				}
			}
			else
			{
				if (falseValue!=null && falseValue.length()>0)
				{
					return(falseValue);
				}
			}
			return(Boolean.toString(valueBool));
		}
		return(valueString);
	}


	/**
	 *  Gets the atrId attribute of the AtrBean object
	 *
	 *@return    The atrId value
	 */
	public int getAtrId()
	{
		return atrId;
	}

	/**
	 *  Sets the atrId attribute of the AtrBean object
	 *
	 *@param  atrId  The new atrId value
	 */
	public void setAtrId(int atrId)
	{
		this.atrId = atrId;
	}

	/**
	 *  Sets the atrName attribute of the AtrBean object
	 *
	 *@param  atrName  The new atrName value
	 */
	public void setAtrName(String atrName)
	{
		this.atrName = atrName;
	}

	/**
	 *  Gets the atrName attribute of the AtrBean object
	 *
	 *@return    The atrName value
	 */
	public String getAtrName()
	{
		return atrName;
	}

	/**
	 *  Sets the atrDescription attribute of the AtrBean object
	 *
	 *@param  atrDescription  The new atrDescription value
	 */
	public void setAtrDescription(String atrDescription)
	{
		this.atrDescription = atrDescription;
	}

	/**
	 *  Gets the atrDescription attribute of the AtrBean object
	 *
	 *@return    The atrDescription value
	 */
	public String getAtrDescription()
	{
		return atrDescription;
	}

	/**
	 *  Sets the atrDefaultValue attribute of the AtrBean object
	 *
	 *@param  atrDefaultValue  The new atrDefaultValue value
	 */
	public void setAtrDefaultValue(String atrDefaultValue)
	{
		this.atrDefaultValue = atrDefaultValue;
	}

	/**
	 *  Gets the atrDefaultValue attribute of the AtrBean object
	 *
	 *@return    The atrDefaultValue value
	 */
	public String getAtrDefaultValue()
	{
		return atrDefaultValue;
	}

	/**
	 *  Sets the atrType attribute of the AtrBean object
	 *
	 *@param  atrType  The new atrType value
	 */
	public void setAtrType(int atrType)
	{
		this.atrType = atrType;
	}

	/**
	 *  Gets the atrType attribute of the AtrBean object
	 *
	 *@return    The atrType value
	 */
	public int getAtrType()
	{
		return atrType;
	}

	/**
	 *  Sets the docId attribute of the AtrBean object
	 *
	 *@param  docId  The new docId value
	 */

	/**
	 *  Gets the docId attribute of the AtrBean object
	 *
	 *@return    The docId value
	 */

	/**
	 *  Sets the valueString attribute of the AtrBean object
	 *
	 *@param  valueString  The new valueString value
	 */
	public void setValueString(String valueString)
	{
		this.valueString = valueString;
	}

	/**
	 *  Gets the valueString attribute of the AtrBean object
	 *
	 *@return    The valueString value
	 */
	public String getValueString()
	{
		return valueString;
	}

	/**
	 *  Sets the valueInt attribute of the AtrBean object
	 *
	 *@param  valueInt  The new valueInt value
	 */
	public void setValueInt(int valueInt)
	{
		this.valueInt = valueInt;
	}

	/**
	 *  Gets the valueInt attribute of the AtrBean object
	 *
	 *@return    The valueInt value
	 */
	public int getValueInt()
	{
		return valueInt;
	}

	/**
	 *  Sets the valueBool attribute of the AtrBean object
	 *
	 *@param  valueBool  The new valueBool value
	 */
	public void setValueBool(boolean valueBool)
	{
		this.valueBool = valueBool;
	}

	/**
	 *  Gets the valueBool attribute of the AtrBean object
	 *
	 *@return    The valueBool value
	 */
	public boolean isValueBool()
	{
		return valueBool;
	}
	public void setOrderPriority(int orderPriority)
	{
		this.orderPriority = orderPriority;
	}
	public int getOrderPriority()
	{
		return orderPriority;
	}
	public void setAtrGroup(String atrGroup)
	{
		this.atrGroup = atrGroup;
	}
	public String getAtrGroup()
	{
		return atrGroup;
	}
	public String getTrueValue()
	{
		return trueValue;
	}
	public void setTrueValue(String trueValue)
	{
		this.trueValue = trueValue;
	}
	public String getFalseValue()
	{
		return falseValue;
	}
	public void setFalseValue(String falseValue)
	{
		this.falseValue = falseValue;
	}
	public void setLink(String link)
	{
		this.link = link;
	}
	public String getLink()
	{
		return link;
	}
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}
	public String getFileName()
	{
		return fileName;
	}

	public String getIcon()
	{
		String icon = "/components/_common/mime/default.gif";
		try
		{
			String ext = getFileName().substring(getFileName().lastIndexOf('.') + 1).toLowerCase();
			ext = ext.trim().toLowerCase();
			IwcmFile f = new IwcmFile(Tools.getRealPath("/components/_common/mime/" + ext + ".gif"));
			if (f.exists())
			{
				icon = "/components/_common/mime/" + ext + ".gif";
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(icon);
	}

	public String getLength()
	{
		double lengthDouble = 0;
		IwcmFile f = new IwcmFile(Tools.getRealPath(getLink()));
		if (f!=null)
		{
			lengthDouble = f.length();
		}
		String length = "";
		if (lengthDouble > (1024 * 1024))
		{
			length = decimalFormat.format(lengthDouble / (1024 * 1024)) + " MB";
		}
		else if (lengthDouble > 1024)
		{
			length = decimalFormat.format(lengthDouble / 1024) + " kB";
		}
		else
		{
			length = decimalFormat.format(lengthDouble) + " B";
		}
		return(length);
	}

}
