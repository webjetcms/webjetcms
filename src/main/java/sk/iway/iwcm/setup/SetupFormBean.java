package sk.iway.iwcm.setup;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.common.DocTools;

@Getter
@Setter
public class SetupFormBean {

	private String dbDriver = "org.mariadb.jdbc.Driver";
	private String dbDomain = "localhost";
	private String dbPort = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private String dbParameters = "";
	private String dbName = "webjet_web";
	private boolean dbUseSuperuser = false;
	private String dbSuperuserUsername = "";
	private String dbSuperuserPassword = "";

	private String conf_installName = "webjet";
	private String conf_license = "";
	private String conf_smtpServer = "smtp.local";
	private String conf_defaultLanguage = "";
	private String encoding = "utf-8";

	//Param just to inform BE what language is used in FE
	private String pageLngIndicator = "sk";

	public void reset(ActionMapping arg0, HttpServletRequest arg1)
	{
		dbUseSuperuser = false;
	}

	/**
	 * @param conf_installName The conf_installName to set.
	 */
	public void setConf_installName(String conf_installName)
	{
		this.conf_installName = DocTools.removeChars(DB.internationalToEnglish(conf_installName));
	}

	/**
	 * @param dbName The dbName to set.
	 */
	public void setDbName(String dbName)
	{
		this.dbName = dbName.toLowerCase();
	}

	/**
	 * @return Returns the dbUseSuperuser.
	 */
	public boolean isDbUseSuperuser()
	{
		return dbUseSuperuser;
	}
}
