package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;

/**
 *  Rozsiruje DocDetails o zoznam atributov, pouziva sa pri tabulkovom vypise atributov pre viacero dokumentov. Nechcel
 * som to davat priamo do DocDetails, aby tam zbytocne nevysel ArrayList s atributami, ktory naviac zvycajne bude null.
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Piatok, 2003, oktďż˝ber 24
 *@modified     $Date: 2003/12/01 08:27:43 $
 */
public class AtrDocBean extends DocDetails
{
   private List<AtrBean> atrList;

   public List<AtrBean> getAtrList()
   {
      if (atrList == null)
      {
         atrList = new ArrayList<>();
      }
      return atrList;
   }
   public void addAtr(AtrBean atr)
   {
      getAtrList().add(atr);
   }

   /**
    * Rozparsuje meno parametra, ktory je vo formate atrs_TYP_MENO, kde TYP je
    * sposob vyhodnotenia (SS-substring, EQ-equal, LT-less than,GT-greater than)
    * @param param
    * @return
    */
   private String[] parseParam(String param)
   {
      String[] ret = null;
      StringTokenizer st = new StringTokenizer(param, "_");
      if (st.countTokens()>2)
      {
         ret = new String[2];
         //atrs_
         st.nextToken();
         //typ
         ret[0] = st.nextToken();
         //meno
         ret[1] = st.nextToken();
         while (st.hasMoreTokens())
         {
         	ret[1] = ret[1] + "_" + st.nextToken();
         }
      }
      return(ret);
   }

   /**
    * vrati true, ak je treba tento riadok vymazat (nevyhovuje podmienke)
    * @param param
    * @param paramValue
    * @return
    */
   public boolean mustRemove(String param, String paramValue)
   {
   	String[] paramValues = new String[1];
   	paramValues[0] = paramValue;
   	return mustRemove(param, paramValues);
   }

   public boolean mustRemove(String param, String[] paramValues)
   {
      //rozparsuj param na hodnoty
      String[] apv = parseParam(param);
      int i;
      for (i=0; i<paramValues.length; i++)
      {
	      paramValues[i] = DB.internationalToEnglish(paramValues[i]).toLowerCase();
	      // z nejakeho dovodu sa zle prenasa + v URL, takze nahradime za medzeru
	      paramValues[i] = paramValues[i].replace('+', ' ');
      }

      if (apv != null)
      {
         String type = apv[0].toUpperCase();
         String name = DocTools.removeChars(DB.internationalToEnglish(apv[1])).replace('-', '_');
         String atrValue;
         double iAtrValue;
         double iParamValue;
         for (AtrBean atrBean : atrList)
         {
         	String atrName = DocTools.removeChars(DB.internationalToEnglish(atrBean.getAtrName())).replace('-', '_');
            Logger.debug(this,"porovnavam: "+atrName+"="+DB.internationalToEnglish(atrBean.getValue()).toLowerCase()+" vs "+name + "="+paramValues[0]+" type="+type);

            if (atrName.equalsIgnoreCase(name) || name.equals(Integer.toString(atrBean.getAtrId())))
            {
            	atrValue = DB.internationalToEnglish(atrBean.getValue()).toLowerCase();
            	//z nejakeho dovodu sa zle prenasa + v URL, takze nahradime za medzeru
            	atrValue = atrValue.replace('+', ' ');
               if ("SS".equals(type))
               {
                  //v tabulke sa musi nachadzat substring
               	for (i=0; i<paramValues.length; i++)
                  {
	                  if (atrValue.indexOf(paramValues[i])!=-1)
	                  {
	                     //Logger.println(this,"MUST REMOVE: "+atrValue+" "+paramValue);
	                     return false;
	                  }
                  }
               	return true;
               }
               else if ("EQ".equals(type))
               {
               	for (i=0; i<paramValues.length; i++)
                  {
               		if (Tools.isEmpty(paramValues[i]))
            			{
               			if (paramValues.length==1) return(false);
               			continue;
            			}
	                  //equal - porovnanie
	                  if (atrValue.equalsIgnoreCase(paramValues[i]))
	                  {
	                     //Logger.println(this,"MUST REMOVE: "+atrValue+" "+paramValue);
	                     return(false);
	                  }
                  }
               	return true;
               }
               else if ("LT".equals(type))
               {
                  //menej ako
                  iAtrValue = atrBean.getValueNumber();
                  try
                  {
                     iParamValue = Double.parseDouble(paramValues[0].replace(',', '.'));
                     //zachovavam len to co je mensie rovne ako hodnota
                     //teda vyhadzujem to co je vacsie
                     if (iAtrValue > iParamValue)
                     {
                        //Logger.println(this,"MUST REMOVE: "+atrValue+" "+paramValue);
                        return(true);
                     }
                  }
                  catch (Exception ex)
                  {

                  }
               }
               else if ("GT".equals(type))
               {
                  //viac ako
                  iAtrValue = atrBean.getValueNumber();
                  try
                  {
                     iParamValue = Double.parseDouble(paramValues[0].replace(',', '.'));
                     //zachovavam len to co je vacsie rovne ako hodnota
                     if (iAtrValue < iParamValue)
                     {
                        //Logger.println(this,"MUST REMOVE: "+atrValue+" "+paramValue);
                        return(true);
                     }
                  }
                  catch (Exception ex)
                  {

                  }
               }
               else if ("GTLT".equals(type))
               {
                  //interval (10-20)
                  iAtrValue = atrBean.getValueNumber();
                  try
                  {
                  	String paramValue = paramValues[0].replace(',', '.');
                  	i = paramValue.indexOf('|');
                     if (i==-1) i = paramValue.indexOf(':');

                     iParamValue = Double.parseDouble(paramValue.substring(0, i));
                     double iParamValue2 = Double.parseDouble(paramValue.substring(i+1));
                     //zachovavam len to co je vacsie ako hodnota
                     if (iAtrValue < iParamValue || iAtrValue > iParamValue2)
                     {
                        //Logger.println(this,"MUST REMOVE: "+atrValue+" "+paramValue);
                        return(true);
                     }
                  }
                  catch (Exception ex)
                  {

                  }
               }
            }
         }
      }

      //iteruj po atributoch a najdi atribut s danym menom

      return(false);
   }

}
