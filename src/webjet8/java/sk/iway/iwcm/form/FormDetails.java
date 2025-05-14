package sk.iway.iwcm.form;

import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.tags.support_logic.CustomResponseUtils;
import sk.iway.iwcm.users.UserDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Streda, 2002, jún 26
 *@modified     $Date: 2003/12/16 18:36:26 $
 */
public class FormDetails
{
	private int id;
	private String formName;
	private String data;
	private String files;
	private String createDateString;
	private boolean header;
	private long createDate;
	private String html;
	private Map<String, Integer> colNames;
	private String note;
	private UserDetails userDetails = null;
	private long lastExportDate = 0;
	int docId;
	private Date doubleOptInDate;

	private Date crDate;

	public Date getCrDate()
	{
		return crDate == null ? null : (Date) crDate.clone();
	}

	public void setCrDate(Date crDate)
	{
		this.crDate = crDate == null ? null : (Date) crDate.clone();
	}

	/**
	 *  Gets the tableRow attribute of the FormDetails object
	 *
	 *@return    The tableRow value
	 */
	public String getTableRow()
	{
		String begin = "<td class='sort_td'>";
		String end = "</td>";
		if (header)
		{
			begin = "<td class='sort_thead_td' type='CaseInsensitiveString' nowrap>";
			end = "</td>";
		}

		String[] td = getColumnsSortedAsArray();

		int i = td.length;
		int counter;
		StringBuilder ret = new StringBuilder();
		for (counter=0; counter<i; counter++)
		{
			String text = td[counter];
			if (Tools.isEmpty(text))
			{
				text = "&nbsp;";
			}
			if (text.length() > 100)
			{
				//text = text.substring(0, 90) + "...";
				text = "<div style='width:140px;height:80px;overflow: auto;'>"+text+"</div>";
			}
			ret.append(begin).append(text).append(end).append('\n');
		}

		return (ret).toString();
	}

	private String[] tdCached = null;

	/**
	 * Vrati hodnoty pre bunky tabulky vypisu presne v poradi ako je hlavicka
	 * @return
	 */
	public String[] getColumnsSortedAsArray()
	{
		if (tdCached != null) return tdCached.clone();

		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		String field;
		int i;
		Integer iInteger;
		int counter = 0;

		i = st.countTokens();
		if (colNames!=null && colNames.size()>1)
		{
			i = colNames.size();
		}

		String[] td = new String[i];

		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			field = null;
			try
			{
				i = text.indexOf('~');
				if (i>0)
				{
					field = text.substring(0, i);
					if (i<text.length())
					{
						text = text.substring(i+1);
					}
				}
				else if (i==0)
				{
					if (text.length() == 1)
					{
						text = "";
					}
					else
					{
						text = text.substring(1);
					}
				}
			}
			catch (Exception ex)
			{

			}

			//if (text.compareTo("\t")==0) continue;
			if (text.trim().length() < 1)
			{
				text = "&nbsp;";
			}
			if (header)
			{
				text = text.replace('_', ' ');
			}
			text = text.trim();

			i = counter;
			if (colNames!=null && field!=null && field.length()>0)
			{
				iInteger = colNames.get(field);
				if (iInteger!=null)
				{
					i = iInteger.intValue();
				}
			}
			if (i<td.length)
			{
				td[i] = CryptoFactory.decrypt(text);
			}

			counter++;
		}
		tdCached = td;
		return td;
	}

	/**
	 * vrati hodnotu v stlpci daneho mena
	 * @param columnName
	 * @return
	 */
	public String getColumn(String columnName)
	{
		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		String field;
		int i;
		//int counter = 0;

		i = st.countTokens();
		if (colNames!=null && colNames.size()>1)
		{
			i = colNames.size();
		}

		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			field = null;
			try
			{
				i = text.indexOf('~');
				if (i>0)
				{
					field = text.substring(0, i);
					if (i<text.length())
					{
						text = text.substring(i+1);
					}
				}
				else if (i==0)
				{
					if (text.length() == 1)
					{
						text = "";
					}
					else
					{
						text = text.substring(1);
					}
				}
			}
			catch (Exception ex)
			{

			}

			//if (text.compareTo("\t")==0) continue;
			if (text.trim().length() < 1)
			{
				text = "&nbsp;";
			}
			if (header)
			{
				text = text.replace('_', ' ');
			}
			text = text.trim();

			if (columnName.equalsIgnoreCase(field))
			{
				return(text);
			}

			//counter++;
		}

		return ("");
	}


	/**
	 * vrati zoznam stlpcov
	 * @return
	 */
	public List<LabelValueDetails> getColNamesArray()
	{
		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		String field;
		int i;
		Integer iInteger;
		int counter = 0;

		i = st.countTokens();
		if (colNames!=null && colNames.size()>1)
		{
			i = colNames.size();
		}

		String[] td = new String[i];

		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			field = null;
			try
			{
				i = text.indexOf('~');
				if (i>0)
				{
					field = text.substring(0, i);
					if (i<text.length())
					{
						text = text.substring(i+1);
					}
				}
				else if (i==0)
				{
					if (text.length() == 1)
					{
						text = "";
					}
					else
					{
						text = text.substring(1);
					}
				}
			}
			catch (Exception ex)
			{

			}

			//if (text.compareTo("\t")==0) continue;
			if (text.trim().length() < 1)
			{
				text = "&nbsp;";
			}
			if (header)
			{
				text = text.replace('_', ' ');
			}
			text = text.trim();

			i = counter;
			if (colNames!=null && field!=null && field.length()>0)
			{
				iInteger = colNames.get(field);
				if (iInteger!=null)
				{
					i = iInteger.intValue();
				}
			}
			if (i<td.length)
			{
				td[i] = text;
			}

			counter++;
		}

		i = td.length;
		List<LabelValueDetails> ret = new ArrayList<>();
		LabelValueDetails lvd;
		String selected;
		for (counter=0; counter<i; counter++)
		{
			text = td[counter];
			if (text==null || text.length()<1)
			{
				text = "";
			}
			selected="";
			if ("email".equals(text))
			{
				selected="selected";
			}
			lvd = new LabelValueDetails(text, selected);
			ret.add(lvd);
		}

		return (ret);
	}

	/**
	 * Vrati hashtabulku s nazvami a hodnotami poli
	 * @return
	 */
	public Map<String,String> getNameValueTable()
	{
		Map<String,String> nameValueTable = new Hashtable<>();

		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		String field;
		int i;

		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			field = null;
			try
			{
				i = text.indexOf('~');
				if (i>0)
				{
					field = text.substring(0, i);
					if (i<text.length())
					{
						text = text.substring(i+1);
					}
				}
				else if (i==0)
				{
					if (text.length() == 1)
					{
						text = "";
					}
					else
					{
						text = text.substring(1);
					}
				}

				nameValueTable.put(field, text);
			}
			catch (Exception ex)
			{

			}


		}

		return (nameValueTable);
	}


	/**
	 *  Gets the createDateHtml attribute of the FormDetails object
	 *
	 *@return    The createDateHtml value
	 */
	public String getCreateDateHtml()
	{
		return(createDateString);
	}

	/**
	 *  Gets the createDateXLS attribute of the FormDetails object
	 *
	 *@return    The createDateXLS value
	 */
	public String getCreateDateXLS()
	{
		if (header)
		{
			return ("Dátum\t");
		}
		else
		{
			return (createDateString + "\t");
		}
	}

	/**
	 *  Gets the rowCSV attribute of the FormDetails object
	 *
	 *@return    The rowCSV value
	 */
	public String getRowCSV()
	{
		StringBuilder ret = new StringBuilder();
		String begin = "";
		String end = "\t";
		if (header)
		{
			begin = "";
			end = "\t";
		}
		StringTokenizer st = new StringTokenizer(data, "|");
		String text;
		//bereme aj separatory, takze kazdy druhy je \t
		while (st.hasMoreTokens())
		{
			text = st.nextToken();
			if (text.startsWith("~"))
			{
				if (text.length() == 1)
				{
					text = "";
				}
				else
				{
					text = text.substring(1);
				}
			}
			if (header)
			{
				text = text.replace('_', ' ');
			}
			text = text.trim();
			ret.append(begin).append(text).append(end);
		}
		return (ret.append('\n')).toString();
	}

	/**
	 *  Gets the tableAttachements attribute of the FormDetails object
	 *
	 *@return    The tableAttachements value
	 */
	public String getAttachements()
	{
		return(getAttachements(""));
	}

	public String getAttachements(String baseHref)
	{
		if (files==null || files.length()<1) return("&nbsp;");
		String ret = null;
		StringTokenizer st = new StringTokenizer(files, ",");
		String name;
		String link;
		int index;
		StringBuilder retBuffer = null;
		while (st.hasMoreTokens())
		{
			link = st.nextToken();
			index = link.indexOf('_');
			if (index > 0)
			{
				try
				{
					name = link.substring(index + 1);
				}
				catch (Exception ex)
				{
					name = link;
				}
			}
			else
			{
				name = link;
			}
			if (retBuffer == null)
			{
				retBuffer = new StringBuilder();
				retBuffer.append("<a class='sort_link' target='_blank' href='").append(baseHref).append("/apps/forms/admin/attachment/?name=").append(link).append("'>").append(name).append("</a>");
			}
			else
			{
				retBuffer.append(", <a class='sort_link' target='_blank' href='").append(baseHref)
				.append("/apps/forms/admin/attachment/?name=").append(link).append("'>").append(name).append("</a>");
			}

		}
		if (retBuffer!=null) ret = retBuffer.toString();
		if (ret == null)
		{
			ret = "&nbsp;";
		}
		return (ret);
	}

	/**
	 *  Gets the id attribute of the FormDetails object
	 *
	 *@return    The id value
	 */
	public int getId()
	{
		return id;
	}

	/**
	 *  Sets the id attribute of the FormDetails object
	 *
	 *@param  id  The new id value
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 *  Sets the formName attribute of the FormDetails object
	 *
	 *@param  formName  The new formName value
	 */
	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	/**
	 *  Gets the formName attribute of the FormDetails object
	 *
	 *@return    The formName value
	 */
	public String getFormName()
	{
		return formName;
	}

	/**
	 *  Sets the data attribute of the FormDetails object
	 *
	 *@param  data  The new data value
	 */
	public void setData(String data)
	{
		if (data.contains("<") || data.contains(">"))
		{
			//aktualny kod escapuje znaky uz na urovni IwcmRequest objektu, na stare data ktore mozu obsahovat XSS ale radsej pouzijeme toto
			this.data = CustomResponseUtils.filter(data);
		}
		else
		{
			this.data = data;
		}
	}

	/**
	 *  Gets the data attribute of the FormDetails object
	 *
	 *@return    The data value
	 */
	public String getData()
	{
		return data;
	}

	/**
	 *  Sets the files attribute of the FormDetails object
	 *
	 *@param  files  The new files value
	 */
	public void setFiles(String files)
	{
		this.files = files;
	}

	/**
	 *  Gets the files attribute of the FormDetails object
	 *
	 *@return    The files value
	 */
	public String getFiles()
	{
		return files;
	}

	/**
	 *  Sets the createDateString attribute of the FormDetails object
	 *
	 *@param  createDateString  The new createDateString value
	 */
	public void setCreateDateString(String createDateString)
	{
		this.createDateString = createDateString;
	}

	/**
	 *  Gets the createDateString attribute of the FormDetails object
	 *
	 *@return    The createDateString value
	 */
	public String getCreateDateString()
	{
		return createDateString;
	}

	/**
	 *  Sets the header attribute of the FormDetails object
	 *
	 *@param  header  The new header value
	 */
	public void setHeader(boolean header)
	{
		this.header = header;
	}

	/**
	 *  Gets the header attribute of the FormDetails object
	 *
	 *@return    The header value
	 */
	public boolean isHeader()
	{
		return header;
	}

	/**
	 *  Sets the createDate attribute of the FormDetails object
	 *
	 *@param  createDate  The new createDate value
	 */
	public void setCreateDate(long createDate)
	{
		this.createDate = createDate;
	}

	/**
	 *  Gets the createDate attribute of the FormDetails object
	 *
	 *@return    The createDate value
	 */
	public long getCreateDate()
	{
		return createDate;
	}

	/**
	 *  Sets the html attribute of the FormDetails object
	 *
	 *@param  html  The new html value
	 */
	public void setHtml(String html)
	{
		this.html = html;
	}

	/**
	 *  Gets the html attribute of the FormDetails object
	 *
	 *@return    The html value
	 */
	public String getHtml()
	{
		return html;
	}
	public void setColNames(Map<String, Integer> colNames)
	{
		this.colNames = colNames;
	}
	public Map<String, Integer> getColNames()
	{
		//do NOT clone() this, callers may want to modify the content!
		return colNames;
	}
	public String getNote()
	{
		return note;
	}
	public void setNote(String note)
	{
		this.note = note;
	}
	public sk.iway.iwcm.users.UserDetails getUserDetails()
	{
		if (userDetails == null)
		{
			userDetails = new UserDetails();
		}
		return userDetails;
	}
	public void setUserDetails(sk.iway.iwcm.users.UserDetails userDetails)
	{
		this.userDetails = userDetails;
	}

	public long getLastExportDate()
	{
		return lastExportDate;
	}

	public String getLastExportDateString()
	{
		if (lastExportDate > 1000)
		{
			return(Tools.formatDateTime(lastExportDate));
		}
		return "&nbsp;";
	}

	public void setLastExportDate(long lastExportDate)
	{
		this.lastExportDate = lastExportDate;
	}

	public int getDocId()
	{
		return docId;
	}

	public void setDocId(int docId)
	{
		this.docId = docId;
	}

	public Date getDoubleOptInDate() {
		return doubleOptInDate;
	}

	public void setDoubleOptInDate(Date doubleOptInDate) {
		this.doubleOptInDate = doubleOptInDate;
	}
}
