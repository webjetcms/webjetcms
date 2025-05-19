package sk.iway.iwcm.doc;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.SelectTag;

/**
 *  Bean popisujuci atribut stranky (vratane definicii)
 *
 *@Title        WebJET 4.0
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Streda, 2003, október 15
 *@modified     $Date: 2003/10/24 15:33:30 $
 */
@SuppressWarnings("java:S1659")
public class AtrBean implements Serializable
{
   private int atrId;
   private String atrName;
   private String atrDescription;
   private String atrDefaultValue;
   private int atrType;
   private int docId;
   private double valueNumber;
   private boolean valueBool;
   private String valueString;
   private int orderPriority;
   private String atrGroup;
   private String trueValue;
   private String falseValue;

   /**
    * vygeneruje HTML podobu atributu
    * @return
    */
   public String getHtml()
   {
   	return getHtml(false, null, null, null);
   }

   /**
    * vygeneruje HTML podobu atributu
    * @return
    */
   public String getHtml(boolean onWebPage) {
      	return getHtml(onWebPage, null, null, null);
   }

   /**
    * vygeneruje HTML podobu atributu
    * @return
    */
   public String getHtml(boolean onWebPage, String name, String id, String classes)
   {
   	if (atrDefaultValue == null) atrDefaultValue = "";
   	StringBuilder ret = null;
      String actualValue = "";
      //ak mame docid, znamena to, ze uz to ma v DB nejaku hodnotu
      if (docId > 0)
      {
         actualValue = getValue();
      }
      String delimeter = ",";
      if(atrDefaultValue.indexOf("|") != -1 )
      {
      	delimeter = "|";	//ak najdem '|', parsujem podla '|'
      }
      StringTokenizer st = new StringTokenizer(atrDefaultValue, delimeter);
      if (st.countTokens()>1)
      {
         if(onWebPage)
         {
            Prop prop = Prop.getInstance();
         	ret = new StringBuilder("<option value=''>"+prop.getText("components.atr.doesntMatter")+"</option>");
         }
         else
         {
            if(Tools.isEmpty(name))
         	   ret = new StringBuilder("<select name='atr_"+atrId+"' ");
            else
         	   ret = new StringBuilder("<select name='"+name+"' ");

            if(Tools.isNotEmpty(id)) ret.append("id='"+id+"' ");

            if(Tools.isNotEmpty(classes)) ret.append("class='"+classes+"' ");

            ret.append(">");

         	ret.append("<option value=''></option>");
         }
         String tmp, selected;
         while (st.hasMoreTokens())
         {
            tmp = st.nextToken();
            if (atrType==AtrDB.TYPE_BOOL)
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
            Logger.debug(AtrBean.class, "Select atr");
            if (tmp.compareTo(actualValue)==0)
            {
               selected = " selected";
            }
            else
            {
               selected = "";
            }
            ret.append("<option value='").append(ResponseUtils.filter(tmp)).append('\'').append(selected).append('>').append(tmp).append("</option>");
         }
         if(!onWebPage)
         {
				ret.append("</select>");
         }
      }
      else
      {
      	if (atrDefaultValue.startsWith("multiline"))
         {
      		int cols=40;
      		int rows=4;
      		st = new StringTokenizer(atrDefaultValue, "-");
      		if (st.countTokens() == 3)
      		{
      			st.nextToken();
      			//pocet stlpcov
      			cols = Tools.getIntValue(st.nextToken(), cols);
      			rows = Tools.getIntValue(st.nextToken(), rows);
      		}
      		if (docId < 1)
	         {
	            actualValue = "";
	         }
	         ret = new StringBuilder("<textarea name='atr_"+atrId+"' rows='"+rows+"' cols='"+cols+"'>"+actualValue+"</textarea>");
         }
      	else if (atrDefaultValue.equals("autoSelect"))
         {
      		if(onWebPage)
            {
            	ret = new StringBuilder("<option value=''>nezáleží</option>");
            }
            else
            {
            	ret = new StringBuilder("<select name='atr_"+atrId+"' onChange='htmlSelectTagAddOption(this)'>");
            	ret.append("<option value=''></option>");
            }

            //ziskaj zoznam hodnot tohto atributu z DB
            Connection db_conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try
				{
					db_conn = DBPool.getConnection();
					//nemozeme pouzit DISTINCT, pretoze MySQL nie je case sensitive
					ps = db_conn.prepareStatement("SELECT value_string FROM doc_atr WHERE atr_id=? ORDER BY value_string");
					ps.setInt(1, getAtrId());
					rs = ps.executeQuery();
					String value, selected;
					Map<String, String> values = new Hashtable<>();
					while (rs.next())
					{
						value = DB.getDbString(rs, "value_string");
						if (values.get(value)!=null) continue;
						values.put(value, value);
						if (value.compareTo(actualValue)==0)
		            {
		               selected = " selected";
		            }
		            else
		            {
		               selected = "";
		            }
						ret.append("<option value='").append(ResponseUtils.filter(value)).append('\'').append(selected).append('>').append(value).append("</option>");
					}
					rs.close();
					ps.close();
					db_conn.close();
					rs = null;
					ps = null;
					db_conn = null;

					if(!onWebPage)
	            {
						ret.append("<option value='"+SelectTag.NEW_OPTION_VALUE+"'>+++</option>");
	            }
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				finally
				{
					try
					{
						if (rs != null)
							rs.close();
						if (ps != null)
							ps.close();
						if (db_conn != null)
							db_conn.close();
					}
					catch (Exception ex2)
					{
						sk.iway.iwcm.Logger.error(ex2);
					}
				}

            //ret += "<option value='"+tmp+"'"+selected+">"+tmp+"</option>";
				if(!onWebPage)
            {
					ret.append("</select>");
            }
         }
      	else
      	{
	         if (docId < 1)
	         {
	            actualValue = atrDefaultValue;
	         }
	         if (getAtrType()==AtrDB.TYPE_BOOL)
	         {
	         	ret = new StringBuilder("<input type='radio' name='atr_"+atrId+"' value='false'");
	         	if (isValueBool()==false) ret.append( " checked='checked'");
	         	ret.append( "> "+getFalseValue());
	         	ret.append( "<br>");
	         	ret.append( "<input type='radio' name='atr_"+atrId+"' value='true'");
	         	if (isValueBool()==true) ret.append( " checked='checked'");
	         	ret.append( "> "+getTrueValue());

	         }
	         else
	         {
		         ret = new StringBuilder("<input name='atr_"+atrId+"'");
		         if (getAtrType()==AtrDB.TYPE_INT) ret.append(" onKeyUp='inputCheckNumberKey(this);' onBlur='inputCheckNumber(this);'");
		         else if (getAtrType()==AtrDB.TYPE_DOUBLE) ret.append(" onKeyUp='inputCheckNumberKeyFloat(this);' onBlur='inputCheckNumberKeyFloat(this);'");
		         else ret.append(" length='128' ");
		         ret.append( " value='"+ResponseUtils.filter(actualValue)+"'>");
	         }
      	}
      }
      return (ret.toString());
   }

   /**
    * ziska hodnotu ako string
    * @return
    */
   public String getValue()
   {
      if (atrType==AtrDB.TYPE_INT)
      {
         return(Long.toString(Math.round(valueNumber)));
      }
      else if (atrType==AtrDB.TYPE_DOUBLE)
      {
         return(Double.toString(valueNumber));
      }
      else if (atrType==AtrDB.TYPE_BOOL)
      {
         return(Boolean.toString(valueBool));
      }
      return(valueString);
   }

   public String getValueHtml()
   {
      if (atrType==AtrDB.TYPE_INT)
      {
         return(Long.toString(Math.round(valueNumber)));
      }
      else if (atrType==AtrDB.TYPE_DOUBLE)
      {
         return(Tools.replace(Double.toString(valueNumber), ".", ","));
      }
      else if (atrType==AtrDB.TYPE_BOOL)
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



   public String getAtrName(String lang)
   {
      return parse(atrName,lang);
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

   public String getAtrDescription(String lang)
   {
      return parse(atrDescription,lang);
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
	   if (atrDefaultValue == null) {
		   return "";
	   }

	   return atrDefaultValue;
   }

   /**
    *  Gets the atrDefaultValue attribute of the AtrBean object
    *
    *@return    The atrDefaultValue value
    */
   public String getAtrDefaultValue(String lang)
   {
      return parse(atrDefaultValue,lang);
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
   public void setDocId(int docId)
   {
      this.docId = docId;
   }

   /**
    *  Gets the docId attribute of the AtrBean object
    *
    *@return    The docId value
    */
   public int getDocId()
   {
      return docId;
   }

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
    *@param  valueNumber  The new valueNumber value
    */
   public void setValueNumber(double valueNumber)
   {
      this.valueNumber = valueNumber;
   }

   /**
    *  Gets the valueInt attribute of the AtrBean object
    *
    *@return    The valueInt value
    */
   public double getValueNumber()
   {
      return valueNumber;
   }

   public void setValueInt(int valueInt)
   {
   	valueNumber = valueInt;
   }

   public int getValueInt()
   {
   	return((int)Math.round(valueNumber));
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

   public String getAtrGroup(String lang)
   {
   	return parse(atrGroup, lang);
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

   /**
    * vygeneruje HTML podobu atributu
    * @param lang - jazyk web stranky
    * @return
    */
   public String getHtml(String lang)
   {
   	StringBuilder ret;
      String actualValue = "";
      //ak mame docid, znamena to, ze uz to ma v DB nejaku hodnotu
      if (docId > 0)
      {
         actualValue = getValue();
      }
      String delimeter = ",";

      if(getAtrDefaultValue() != null && getAtrDefaultValue().indexOf("|") != -1)
      {
      	delimeter = "|";	//ak najdem '|', parsujem podla '|'
      }
      StringTokenizer st = new StringTokenizer(getAtrDefaultValue(), delimeter);
      if (st.countTokens()>1)
      {
         ret = new StringBuilder("<select name='atr_"+atrId+"'>");
         ret.append("<option value=''></option>");
         String tmp, selected;
         while (st.hasMoreTokens())
         {
            tmp = st.nextToken();
            tmp=parse(tmp, lang);
            if (atrType==AtrDB.TYPE_BOOL)
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
            ret.append("<option value='").append(ResponseUtils.filter(tmp)).append('\'').append(selected).append('>').append(tmp).append("</option>");
         }
         ret.append("</select>");
      }
      else
      {
      	if (getAtrDefaultValue(lang)!=null && getAtrDefaultValue(lang).startsWith("multiline"))
         {
      		int cols=40;
      		int rows=4;
      		st = new StringTokenizer(getAtrDefaultValue(lang), "-");
      		if (st.countTokens() == 3)
      		{
      			st.nextToken();
      			//pocet stlpcov
      			cols = Tools.getIntValue(st.nextToken(), cols);
      			rows = Tools.getIntValue(st.nextToken(), rows);
      		}
      		if (docId < 1)
	         {
	            actualValue = "";
	         }
	         ret = new StringBuilder("<textarea name='atr_").append(atrId).append("' rows='").append(rows).append("' cols='").append(cols).append("'>").append(actualValue).append("</textarea>");
         }
      	else if (getAtrDefaultValue(lang)!=null && getAtrDefaultValue(lang).equals("autoSelect"))
         {
         	ret = new StringBuilder("<select name='atr_").append(atrId);
         	ret.append("' onChange='htmlSelectTagAddOption(this)'>").append("<option value=''></option>");

            //ziskaj zoznam hodnot tohto atributu z DB
            Connection db_conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try
				{
					db_conn = DBPool.getConnection();
					//nemozeme pouzit DISTINCT, pretoze MySQL nie je case sensitive
					ps = db_conn.prepareStatement("SELECT value_string FROM doc_atr WHERE atr_id=? ORDER BY value_string");
					ps.setInt(1, getAtrId());
					rs = ps.executeQuery();
					String value, selected;
					Map<String, String> values = new Hashtable<>();
					while (rs.next())
					{
						value = DB.getDbString(rs, "value_string");
						if (values.get(value)!=null) continue;
						values.put(value, value);
						if (value.compareTo(actualValue)==0)
		            {
		               selected = " selected";
		            }
		            else
		            {
		               selected = "";
		            }
						ret.append("<option value='").append(ResponseUtils.filter(value)).append('\'').append(selected).append('>').append(value).append("</option>");
					}
					rs.close();
					ps.close();
					db_conn.close();
					rs = null;
					ps = null;
					db_conn = null;

					ret.append("<option value='").append(SelectTag.NEW_OPTION_VALUE).append("'>+++</option>");
	         }
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				finally
				{
					try
					{
						if (rs != null)
							rs.close();
						if (ps != null)
							ps.close();
						if (db_conn != null)
							db_conn.close();
					}
					catch (Exception ex2)
					{
						sk.iway.iwcm.Logger.error(ex2);
					}
				}

            //ret += "<option value='"+tmp+"'"+selected+">"+tmp+"</option>";
				ret.append("</select>");
         }
      	else
      	{
	         if (docId < 1)
	         {
	            actualValue = getAtrDefaultValue(lang);
	         }
	         if (getAtrType()==AtrDB.TYPE_BOOL)
	         {
	         	ret = new StringBuilder("<input type='radio' name='atr_").append(atrId).append("' value='false'");
	         	if (isValueBool()==false) ret.append(" checked='checked'");
	         	ret.append("> ").append(getFalseValue());
	         	ret.append("<br>");
	         	ret.append("<input type='radio' name='atr_").append(atrId+"' value='true'");
	         	if (isValueBool()==true) ret.append(" checked='checked'");
	         	ret.append("> ").append(getTrueValue());

	         }
	         else
	         {
		         ret = new StringBuilder("<input name='atr_").append(atrId+"'");
		         if (getAtrType()==AtrDB.TYPE_INT) ret.append(" onKeyUp='inputCheckNumberKey(this);' onBlur='inputCheckNumber(this);'");
		         else if (getAtrType()==AtrDB.TYPE_DOUBLE) ret.append(" onKeyUp='inputCheckNumberKeyFloat(this);' onBlur='inputCheckNumberKeyFloat(this);'");
		         ret.append(" value='").append(ResponseUtils.filter(actualValue)).append("'>");
	         }
      	}
      }
      return (ret.toString());
   }

   public static  String parse(String str, String lang)
	{
		if (str == null)
			return null;
		String[] tokens = str.split("\\|");
		if (tokens != null && tokens.length > 1)
		{
			for (String s : tokens)
			{
				if (s.indexOf(':') != -1)
				{
					if (s.substring(0, s.indexOf(':')).equalsIgnoreCase(lang))
					{
						return s.substring(s.indexOf(':') + 1, s.length());
					}
				}
			}

		}
		else
		{
			if (tokens!=null && tokens.length==1){
				if (tokens[0].indexOf(':') != -1){
					return tokens[0].substring(tokens[0].indexOf(':') + 1, tokens[0].length());
				}
			}
			return str;
		}
		return str;
	}
}
