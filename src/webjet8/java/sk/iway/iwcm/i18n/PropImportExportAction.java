package sk.iway.iwcm.i18n;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UsersDB;

/**
 *  PropImportExportAction.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 6.2.2012 13:35:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PropImportExportAction extends WebJETActionBean
{

	@Validate(on={"importAct"},required=true)
	private FileBean propFile;
	private String language;
	private String prefix;
	private String filterPrefix;
	private boolean onlyNewKeys;

	public FileBean getPropFile()
	{
		return propFile;
	}

	public void setPropFile(FileBean propFile)
	{
		this.propFile = propFile;
	}


	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);
		setLanguage(context.getRequest().getParameter("lng"));
	}


	@DefaultHandler
	public Resolution defaultAction() {
		return (new ForwardResolution(RESOLUTION_CONTINUE));
	}

	public Resolution importAct()
	{
		if (isAdminLogged()==false) return new ForwardResolution(RESOLUTION_NOT_LOGGED);
		Identity user = UsersDB.getCurrentUser(getRequest());
		if (user==null || user.isDisabledItem("edit_text")) return new ForwardResolution(RESOLUTION_NOT_LOGGED);

		Logger.debug(getClass(), "Importing custom properties for language: " + getLanguage());
		IwayProperties iwprop = new IwayProperties();
		if(propFile != null){
			try {
				if(propFile.getFileName().endsWith(".properties") || propFile.getFileName().endsWith(".txt") || propFile.getFileName().endsWith(".json") || propFile.getContentType().contains("text")) {
					if(propFile.getFileName().endsWith(".json"))
						iwprop = loadJsonFile(propFile);
					else
						iwprop.load(propFile);

					//save to DB
					//find if the is already one, if is, update, else insert
					PropDB.save(getCurrentUser(), iwprop,getLanguage(), prefix, filterPrefix, onlyNewKeys);
					getRequest().setAttribute("saveOk","ok");
				} else {
					Logger.debug(getClass(),"Import failed. File " + propFile.getFileName() + " is not text.");
					getRequest().setAttribute("message", Prop.getInstance().getText("admin.conf_editor.notText"));
				}
			}
			catch (Exception e)
			{
				Logger.debug(getClass(), "Failed to load properties from fileBean. cause: " + e.getMessage());
				sk.iway.iwcm.Logger.error(e);
				getRequest().setAttribute("saveFail","fail");
			}
		}

		Prop.getInstance(true);

		 return new ForwardResolution(RESOLUTION_CONTINUE);
  }

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public String getLanguage()
	{
		return language;
	}

	private IwayProperties loadJsonFile(FileBean propFile)
   {
		IwayProperties iwprop = null;

		try
		{
	   	InputStream is = propFile.getInputStream();
	   	iwprop = new ObjectMapper().readValue(is, IwayProperties.class);
	   	is.close();
		}
		catch(Exception e){sk.iway.iwcm.Logger.error(e);}

		return iwprop;
   }

	public String getPrefix()
	{
		return prefix;
	}

	public void setPrefix(String prefix)
	{
		this.prefix = prefix;
	}

	public boolean isOnlyNewKeys()
	{
		return onlyNewKeys;
	}

	public void setOnlyNewKeys(boolean onlyNewKeys)
	{
		this.onlyNewKeys = onlyNewKeys;
	}

	public String getFilterPrefix()
	{
		return filterPrefix;
	}

	public void setFilterPrefix(String filterPrefix)
	{
		this.filterPrefix = filterPrefix;
	}

}
