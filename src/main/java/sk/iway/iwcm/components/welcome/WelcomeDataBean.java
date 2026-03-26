package sk.iway.iwcm.components.welcome;
/**
 * WelcomeDataBean.java
 * 
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2014
 * @author $Author: jeeff mkolejak $
 * @version $Revision: 1.3 $
 * @created Date: 16.7.2014 10:37:57
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class WelcomeDataBean
{
	private int fillFormsNumber;
	private int documentForumNumber;
	private int statViewsNumber;
	private int statErrorNumber;

	public int getFillFormsNumber()
	{
		return fillFormsNumber;
	}

	public void setFillFormsNumber(int fillFormsNumber)
	{
		this.fillFormsNumber = fillFormsNumber;
	}

	public int getDocumentForumNumber()
	{
		return documentForumNumber;
	}

	public void setDocumentForumNumber(int documentForumNumber)
	{
		this.documentForumNumber = documentForumNumber;
	}

	public int getStatViewsNumber()
	{
		return statViewsNumber;
	}

	public void setStatViewsNumber(int statViewsNumber)
	{
		this.statViewsNumber = statViewsNumber;
	}

	public int getStatErrorNumber()
	{
		return statErrorNumber;
	}

	public void setStatErrorNumber(int statErrorNumber)
	{
		this.statErrorNumber = statErrorNumber;
	}
}
