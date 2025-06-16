package sk.iway.iwcm.tags;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.support.CustomTagUtils;
import sk.iway.iwcm.tags.support.ResponseUtils;

/**
 * SelectTag.java - tag pre render selectu zo Struts s pridanim poslednej
 * moznosti na pridanie hodnoty
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2005
 * @author $Author: jeeff $
 * @version $Revision: 1.3 $
 * @created Date: 16.8.2005 11:34:08
 * @modified $Date: 2010/01/20 11:15:08 $
 */
public class SelectTag extends sk.iway.iwcm.tags.support.SelectTag
{
	private static final long serialVersionUID = 2751499992225522333L;
	public static final String SELECT_KEY = "org.apache.struts.taglib.html.SELECT";

	//tu je mozne zadat SQL prikaz pre setnutie ako ArrayList do pageContextu
	// (pouzite pre options tag)
	private String sqlQuery = null;
	//ak je nastavene na nejaku nie null hodnotu zrendruje sa ako posledna polozka option pre moznost zadania novej hodnoty
	String enableNewText = null;
	String enableNewTextKey = null;
	public static final String NEW_OPTION_VALUE = "htmlSelectTagNewValue";

	public int doStartTag() throws JspException
	{
		if (Tools.isNotEmpty(enableNewTextKey))
		{
			Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), (HttpServletRequest)pageContext.getRequest());
			enableNewText = prop.getText(enableNewTextKey);
		}

		ArrayList<LabelValueDetails> options = new ArrayList<LabelValueDetails>();
		if (sqlQuery != null)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sqlQuery);
				rs = ps.executeQuery();

				int queryType = 0;
				if (sqlQuery.indexOf("label")!=-1 && sqlQuery.indexOf("value")!=-1)
				{
					queryType = 1;
				}
				String label;
				String value;
				while (rs.next())
				{
					if (queryType == 1)
					{
						label = ResponseUtils.filter(DB.getDbString(rs, "label"));
						value = ResponseUtils.filter(DB.getDbString(rs, "value"));
					}
					else
					{
						label = ResponseUtils.filter(rs.getString(1));
						value = label;
					}
					options.add(new LabelValueDetails(label, value));
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;

				if (Tools.isNotEmpty(enableNewText) && options.size()==0)
				{
					//aby sme sa mohli prepnut na Nova skupina
					options.add(new LabelValueDetails("", ""));
				}

				pageContext.setAttribute("selectSqlQueryOptions", options);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			finally
			{
				try
				{
					if (db_conn != null)
						db_conn.close();
					if (rs != null)
						rs.close();
					if (ps != null)
						ps.close();
				}
				catch (Exception ex2)
				{
				}
			}
		}

		if (Tools.isNotEmpty(enableNewText))
		{
			HttpServletRequest request= (HttpServletRequest) pageContext.getRequest();
			//	zapis linku na javascript
			if (request.getAttribute("sk.iway.iwcm.tags.SelectTag.isJsIncluded")==null)
			{
				CustomTagUtils.getInstance().write(pageContext, "<script type='text/javascript' language='JavaScript' src='"+request.getContextPath()+"/components/_common/html_tags_support.jsp'></script>");
				request.setAttribute("sk.iway.iwcm.tags.SelectTag.isJsIncluded", "true");
			}
			String origOnchange = getOnchange();
			setOnchange(
				Tools.isEmpty(origOnchange)
				? "htmlSelectTagAddOption(this)"
				: ("htmlSelectTagAddOption(this);" + origOnchange) // najprv pridame novu moznost, potom dame "onchange"
			);
			setOnclick("if(1==this.length){onchange();}");
		}

		CustomTagUtils.getInstance().write(pageContext, renderSelectStartElement());
		// Store this tag itself as a page attribute
		pageContext.setAttribute(SELECT_KEY, this);
		this.calculateMatchValues();
		return (EVAL_BODY_BUFFERED);
	}

	public int doEndTag() throws JspException
	{
		// Remove the page scope attributes we created
		pageContext.removeAttribute(SELECT_KEY);
		// Render a tag representing the end of our current form
		StringBuilder results = new StringBuilder();
		if (saveBody != null)
		{
			results.append(saveBody);
		}
		if (Tools.isNotEmpty(enableNewText))
		{
			results.append("<option value='"+NEW_OPTION_VALUE+"'>"+enableNewText+"</option>");
		}
		results.append("</select>");
		CustomTagUtils.getInstance().write(pageContext, results.toString());
		return (EVAL_PAGE);
	}

	/**
	 * Calculate the match values we will actually be using.
	 *
	 * @throws JspException
	 */
	private void calculateMatchValues() throws JspException
	{
		if (this.value != null)
		{
			this.match = new String[1];
			this.match[0] = this.value;
		}
		else
		{
			Object bean = CustomTagUtils.getInstance().lookup(pageContext, name, null);
			if (bean == null)
			{
				//aby sa top dalo pouzit aj v cistom forme
				String requestProperty[] = pageContext.getRequest().getParameterValues(property);
				if (requestProperty != null && requestProperty.length>0)
				{
					this.match = requestProperty;
					return;
				}

				this.match = new String[0];
				return;
				/*
				JspException e = new JspException(messages.getMessage("getter.bean", name));
				RequestUtils.saveException(pageContext, e);
				throw e;
				*/
			}
			try
			{
				this.match = BeanUtils.getArrayProperty(bean, property);
				if (this.match == null)
				{
					this.match = new String[0];
				}
			}
			catch (IllegalAccessException e)
			{
				CustomTagUtils.getInstance().saveException(pageContext, e);
				throw new JspException( CustomTagUtils.getInstance().getMessage("getter.access", property, name) );
			}
			catch (InvocationTargetException e)
			{
				Throwable t = e.getTargetException();
				CustomTagUtils.getInstance().saveException(pageContext, t);
				throw new JspException( CustomTagUtils.getInstance().getMessage("getter.result", property, t.toString()) );
			}
			catch (NoSuchMethodException e)
			{
				//RequestUtils.saveException(pageContext, e);
				//throw new JspException(messages.getMessage("getter.method", property, name));

				//mame pouzite vo forme, kde taka properties nie je, skusme vydolovat z requestu
				if (Tools.isNotEmpty(pageContext.getRequest().getParameter(name)))
				{
					this.match = pageContext.getRequest().getParameterValues(name);
				}
			}
		}
	}

	/**
	 * @return Returns the sqlQuery.
	 */
	public String getSqlQuery()
	{
		return sqlQuery;
	}

	/**
	 * @param sqlQuery
	 *           The sqlQuery to set.
	 */
	public void setSqlQuery(String sqlQuery)
	{
		this.sqlQuery = sqlQuery;
	}
	/**
	 * @return Returns the enableNewText.
	 */
	public String getEnableNewText()
	{
		return enableNewText;
	}
	/**
	 * @param enableNewText The enableNewText to set.
	 */
	public void setEnableNewText(String enableNewText)
	{
		this.enableNewText = enableNewText;
	}

	public String getEnableNewTextKey()
	{
		return enableNewTextKey;
	}

	public void setEnableNewTextKey(String enableNewTextKey)
	{
		this.enableNewTextKey = enableNewTextKey;
	}
}