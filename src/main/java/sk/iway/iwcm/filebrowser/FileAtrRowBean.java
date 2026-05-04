package sk.iway.iwcm.filebrowser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sk.iway.iwcm.DB;

/**
 *  Riadok tabulky atributov
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Nedeľe, 2003, december 28
 *@modified     $Date: 2004/02/25 22:09:58 $
 */
public class FileAtrRowBean
{

	private List<FileAtrBean> atrList;
	private String fileName;
	private String link;

	/**
	 *  Gets the atrList attribute of the FileAtrRowBean object
	 *
	 *@return    The atrList value
	 */
	public List<FileAtrBean> getAtrList()
	{
		if (atrList == null)
		{
			atrList = new ArrayList<>();
		}
		return atrList;
	}

	public String getAtrValue(int atrId)
	{
		for (FileAtrBean atr : getAtrList())
		{
			if (atr.getAtrId() == atrId)
			{
				return(atr.getValue());
			}
		}
		return(null);
	}

	/**
	 *  Adds a feature to the Atr attribute of the FileAtrRowBean object
	 *
	 *@param  atr  The feature to be added to the Atr attribute
	 */
	public void addAtr(FileAtrBean atr)
	{
		getAtrList().add(atr);
	}

	/**
	 *  Rozparsuje meno parametra, ktory je vo formate atrs_TYP_MENO, kde TYP je
	 *  sposob vyhodnotenia (SS-substring, EQ-equal, LT-less than,GT-greater
	 *  than)
	 *
	 *@param  param
	 *@return
	 */
	private String[] parseParam(String param)
	{
		String[] ret = null;
		StringTokenizer st = new StringTokenizer(param, "_");
		if (st.countTokens() > 2)
		{
			ret = new String[2];
			//atrs_
			st.nextToken();
			//typ
			ret[0] = st.nextToken();
			//meno
			ret[1] = st.nextToken();
		}
		return (ret);
	}

	/**
	 *  vrati true, ak je treba tento riadok vymazat (nevyhovuje podmienke)
	 *
	 *@param  param
	 *@param  paramValue
	 *@return
	 */
	public boolean mustRemove(String param, String paramValue)
	{
		//rozparsuj param na hodnoty
		String[] apv = parseParam(param);
		paramValue = DB.internationalToEnglish(paramValue).toLowerCase();
		if (apv != null)
		{
			String type = apv[0].toUpperCase();
			String name = DB.internationalToEnglish(apv[1]);
			String atrValue;
			int iAtrValue;
			int iParamValue;

			for (FileAtrBean atrBean : atrList)
			{
				if (DB.internationalToEnglish(atrBean.getAtrName()).equalsIgnoreCase(name))
				{
					if ("SS".equals(type))
					{
						//v tabulke sa musi nachadzat substring
						atrValue = DB.internationalToEnglish(atrBean.getValue()).toLowerCase();
						if (atrValue.indexOf(paramValue) == -1)
						{
							//Logger.println(this,"MUST REMOVE: " + atrValue + " " + paramValue);
							return (true);
						}
					}
					else if ("EQ".equals(type))
					{
						//v tabulke sa musi nachadzat substring
						atrValue = DB.internationalToEnglish(atrBean.getValue()).toLowerCase();
						if (atrValue.equalsIgnoreCase(paramValue) == false)
						{
							//Logger.println(this,"MUST REMOVE: " + atrValue + " " + paramValue);
							return (true);
						}
					}
					else if ("LT".equals(type))
					{
						//v tabulke sa musi nachadzat substring
						atrValue = DB.internationalToEnglish(atrBean.getValue()).toLowerCase();
						iAtrValue = atrBean.getValueInt();
						try
						{
							iParamValue = Integer.parseInt(paramValue);
							//zachovavam len to co je mensie rovne ako hodnota
							//teda vyhadzujem to co je vacsie
							if (iAtrValue > iParamValue)
							{
								//Logger.println(this,"MUST REMOVE: " + atrValue + " " + paramValue);
								return (true);
							}
						}
						catch (Exception ex)
						{

						}
					}
					else if ("GT".equals(type))
					{
						//v tabulke sa musi nachadzat substring
						atrValue = DB.internationalToEnglish(atrBean.getValue()).toLowerCase();
						iAtrValue = atrBean.getValueInt();
						try
						{
							iParamValue = Integer.parseInt(paramValue);
							//zachovavam len to co je vacsie ako hodnota
							if (iAtrValue < iParamValue)
							{
								//Logger.println(this,"MUST REMOVE: " + atrValue + " " + paramValue);
								return (true);
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

		return (false);
	}
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}
	public String getFileName()
	{
		return fileName;
	}
	public void setLink(String link)
	{
		this.link = link;
	}
	public String getLink()
	{
		return link;
	}
}
