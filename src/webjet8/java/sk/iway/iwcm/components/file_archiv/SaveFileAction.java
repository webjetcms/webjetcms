package sk.iway.iwcm.components.file_archiv;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UsersDB;

/**
 *	SaveFileAction.java
 *
 * Title		webjet7
 * Company		Interway s.r.o. (www.interway.sk)
 * Copyright 	Interway s.r.o. (c) 2001-2015
 * @author		$Author: jeeff $(prau)
 * @version		Revision: 1.3  10.2.2015
 * created		Date: 10.2.2015 15:03:09
 * modified   	Date: 10.2.2015 15:03:09
 * Ticket 		Number: #17263
 */
public class SaveFileAction extends WebJETActionBean
{
	private int oldId;
	private FileBean file = null;
	private boolean uploadLater;
	private boolean replace;

	private static Prop prop = Prop.getInstance();

	private FileArchivatorBean fab = null;
	private String dateUploadLater;
	private String timeUploadLater;

	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);
		Identity user = UsersDB.getCurrentUser(context.getRequest());
		if (user == null || !user.isAdmin())
		{
			Logger.error(FileArchivatorAction.class, "User not loged, or user is not admin");
			return;
		}

		Calendar nextDay = Calendar.getInstance();
		nextDay.add(Calendar.DATE, 1);
		dateUploadLater = Tools.formatDate(nextDay.getTime());

		Calendar nextHour = Calendar.getInstance();
		nextHour.add(Calendar.HOUR, 1);
		timeUploadLater = Tools.formatTime(nextHour.getTime());

		oldId = Tools.getIntValue(Tools.getParameter(getRequest(), "oldId"), -1);
		if(oldId > 0)
		{
			fab = FileArchivatorDB.getInstance().getById(oldId);
			if(fab != null && fab.getId() > 0)
				fab.setNote(Tools.replace(Tools.replace(Tools.replace(Tools.replace(fab.getNote(),"&lt;","<"),"&gt;",">"),"&quot;","\""),"&nbsp;"," "));
		}
		else
		{
			//ak mam parameter oldId, ale zaznam neexistuje
			if(getRequest().getParameter("oldId") != null)
				return;
			fab = new FileArchivatorBean();
			fab.setVirtualFileName("");
			fab.setFilePath(FileArchivatorKit.getArchivPath());
			fab.setEmails(user.getEmail());
			fab.setShowFile(true);
		}
	}

	@HandlesEvent("save")
	public Resolution saveFile()
	{
		if(!isAdminLogged())
		{
			return (new ForwardResolution("/components/maybeError.jsp"));
		}

		String fileName = null;
		if(file != null)
		{
			fileName = file.getFileName();
		}

		Logger.debug(SaveFileAction.class, "saveFile() IP: "+Tools.getRemoteIP(getRequest())+" file: "+fileName+", oldId:"+oldId);

		if(checkFileProperties(file))
		{
			if(replace)
			{
				//nahradenie suboru
				if(replaceFile(file))
					getRequest().setAttribute("divSuccess", "true");
			}
			else
			{
				//klasicke nahranie/archivacia suboru
				if(uploadFile(file))
					getRequest().setAttribute("divSuccess", "true");
			}
		}

		return (new ForwardResolution("/components/maybeError.jsp"));
	}

	/** Nahra subor na server. Synchronized preto, lebo metoda vytvara verzie suborov a meni odkazy v databaze na hlavny (najaktualnejsi) subor.
	 *
	 */
	protected synchronized boolean uploadFile(FileBean fileBean)
	{
		if(getFileDirPath() == null || FileBrowserTools.hasForbiddenSymbol(getFileDirPath()))
		{
			return false;
		}

        Identity user = getCurrentUser();

		//kontrola prav na zapis do suboru
		if(FileArchivatorKit.isArchivEnabled(user) == false || isAdminLogged() == false || user.isFolderWritable("/"+getFileDirPath())==false)
		{
			Logger.debug(this, "User nema pravo na zapis do priecinku:  "+"/"+getFileDirPath());
			setError("components.elfinder.commands.upload.error",getRequest());
			return false;
		}

		String dateStamp = null;

		FileArchivatorBean fabOld = FileArchivatorDB.getInstance().getById(oldId);
		if(fabOld != null)
			dateStamp = FileArchivatorKit.getDateStampAsString(fabOld.getDateInsert());

		String uniqueFileName = FileArchivatorKit.getUniqueFileName(fileBean.getFileName(), getFileDirPath(), dateStamp);
		String fileUrl = "/"+getFileDirPath()+uniqueFileName;
		String realPath = Tools.getRealPath(fileUrl);
		IwcmFile f = new IwcmFile(realPath);
		try
		{
			IwcmFsDB.writeFiletoDest(fileBean.getInputStream(),new File(f.getAbsolutePath()),Tools.safeLongToInt(fileBean.getSize()));
			boolean result = setFilePropertiesAfterUploadReplace(getFileDirPath(), uniqueFileName);
			if(result)
				 Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (file archiv), path="+fileUrl, -1, -1);

			return result;
		}
		catch(IOException ioex)
		{
			Logger.debug(SaveFileAction.class, "Nastala chyba '"+ioex.getMessage()+"' pri ukladani suboru "+f.getPath());
			sk.iway.iwcm.Logger.error(ioex);
		}
		setError("components.file_archiv.upload.file_upload_error",getRequest());
		return false;
	}

	private int getUserId()
	{
		Identity user = UsersDB.getCurrentUser(getRequest());
		int userId = -1;
		if(user != null)
			userId = user.getUserId();

		return userId;
	}

	// ziska potrebne parametre z requestu a zavola metodu, ktora ich nasetuje. Ak nastane chyba do request-atributu nastavi hlasku.
	private boolean setFileAttributesAfterSave(String dirPath, String fileName)
	{
		return setFilePropertiesAfterUploadReplace(dirPath, fileName);
	}

	protected boolean setFilePropertiesAfterUploadReplace(String dirPath, String fileName)
	{
		if(replace)
		{
			if(!FileArchivatorKit.setFilePropertiesAfterUploadReplace(dirPath, fileName, oldId, getFileArchivatorBean(dirPath, fileName), getRequest()))
			{
				Logger.debug(SaveFileAction.class, "AfterUploadError (replace) !!! Property: "+fileName+","+oldId+","+getUserId()+","+fab.getVirtualFileName()+","+fab.getValidFrom()+","+fab.getValidTo());
				setError("components.file_archiv.upload.after_save_error",getRequest());
				return false;
			}
			return true;
		}

		if(!FileArchivatorKit.setFilePropertiesAfterUpload(getFileArchivatorBean(dirPath, fileName), oldId, uploadLater, getRequest()))
		{
			Logger.debug(SaveFileAction.class, "AfterUploadError !!! Property: "+fileName+","+oldId+","+getUserId()+","+fab.getVirtualFileName()+","+fab.getValidFrom()+","+fab.getValidTo());
			setError("components.file_archiv.upload.after_save_error",getRequest());
			return false;
		}
		return true;
	}

	private FileArchivatorBean getFileArchivatorBean(String dirPath, String fileName)
	{
		//uz sa musim tvarit ako novy subor
		IwcmFile newfile = new IwcmFile(Tools.getRealPath(dirPath+fileName));
		fab.setId(0);
		fab.setFilePath(dirPath);
		fab.setFileName(fileName);
		fab.setUserId(getUserId());
		fab.setDateUploadLater(getUploadLaterDate());
		fab.setUploaded(uploadLater ? 0 : -1);
		fab.setMd5(FileArchivatorKit.getMD5(newfile));
		fab.setFileSize(file != null ? newfile.length() : 0);
		fab.setDomainId(CloudToolsForCore.getDomainId());

		return fab;
	}

	private String getPreferredDirPath()
    {
        if(Constants.getBoolean("fileArchivUseCategoryAsLink") && Tools.isNotEmpty(fab.getCategory()))
        {
            String dirName = sk.iway.iwcm.DB.internationalToEnglish(fab.getCategory()).toLowerCase();
            dirName = DocTools.removeChars(dirName,true);
            return FileArchivatorKit.getArchivPath()+dirName;
        }
        return fab.getFilePath();
    }

	// vrati cestu  k suboru alebo null
	protected String getFileDirPath()
	{
		String dirPath = getPreferredDirPath();

		if(uploadLater)
		{
			dirPath = FileArchivatorKit.getFullInsertLaterPath() + dirPath;
		}

		if(Tools.isEmpty(dirPath) || (!dirPath.startsWith(FileArchivatorKit.getArchivPath()) && !dirPath.startsWith("/"+FileArchivatorKit.getArchivPath())))
		{
			Logger.debug(SaveFileAction.class, "Not allowed path. Allowed path is: "+FileArchivatorKit.getArchivPath());
			setError("components.file_archiv.upload.file_has_not_allowed_path",FileArchivatorKit.getArchivPath(),getRequest());
			return null;
		}

		if(!dirPath.endsWith("/"))
		{
			dirPath += "/";
		}

		IwcmFile fileDir = new IwcmFile(Tools.getRealPath(dirPath));
		if (!fileDir.exists())
		{
			fileDir.mkdirs();
		}
		return dirPath;
	}

	/** Skontorluje velkost suboru, zachytava NPE
	 *
	 */
	private boolean isFileSizeOk(FileBean fileBean)
	{
		if(fileBean == null)
		{
			setError("components.file_archiv.upload.file_is_null",getRequest());
			return false;
		}

		return fileBean.getSize() <= FileArchivatorKit.getMaxFileSize();
	}

	/**Skontroluje vlastnosti suboru (dlzku nazvu, velkost, povolene pripony)
	 *
	 */
	private boolean checkFileProperties(FileBean fileToCheck)
	{
		if(fileToCheck == null || fileToCheck.getFileName().length() < 5)
		{
			String fileName = "null";
			if(fileToCheck != null)
			{
				fileName = fileToCheck.getFileName();
			}
			Logger.debug(SaveFileAction.class, "checkFileProperties() fileBean je null alebo ma kratky nazov. Subor: "+fileName);
			setError("components.file_archiv.upload.file_not_exists_or_short_name", fileName, getRequest());
			return false;
		}
        FileArchivatorKit fk = new FileArchivatorKit(prop);
		if(!fk.hasAllowedExtensions(fileToCheck.getFileName(), oldId))
		{
			setErrorText(fk.getErrorsList().get(0));
			return false;
		}

		if(!isFileSizeOk(fileToCheck))
		{
			Logger.debug(SaveFileAction.class, "Velkost suboru je prekrocena. Aktualna velkost: "+fileToCheck.getSize()+" maximalna velkost: "+FileArchivatorKit.getMaxFileSize());
			setError("components.file_archiv.upload.file_is_too_big", fileToCheck.getSize()+"", FileArchivatorKit.getMaxFileSize()+"", getRequest());
			return false;
		}

		if(FileArchivatorKit.isConcurrentModification(oldId))
		{
			setError("components.file_archiv.upload.concurrent_modification", getRequest());
			return false;
		}

		//ak uz existuje referencia v databaze k suboru ktory este len ideme nahrat, tak je to problem. Niekto zmazal subor rucne.
		//subor nemozeme nahrat pretoze by mohol byt zmazany inym zaznamom v databaze - omylom
		if(FileArchivatorKit.existsPathInDB(getFileDirPath()+fileToCheck.getFileName()) && !FileTools.isFile(getFileDirPath()+fileToCheck.getFileName()) )
		{
			setError("components.file_archiv.upload.db_enrty_exists", getRequest());
			return false;
		}

		if(replace && uploadLater)
		{
			setError("components.file_archiv.upload.replace_uploadLater_checked", getRequest());
			return false;
		}

		if(replace)
		{
			if(oldId < 1)
			{
				setError("components.file_archiv.upload.wrong_oldId", String.valueOf(fab.getId()), getRequest());
				return false;
			}
		}

		if(uploadLater)
		{
			if(!isUploadDateCorrect())
			{
				setError("components.file_archiv.upload.upload_date_wrong", getRequest());
				return false;
			}

			if(!isCorrectEmails())
			{
				setError("components.file_archiv.upload.emails_wrong", getRequest());
				return false;
			}
		}

		//vzory nemozu mat archiv
		FileArchivatorBean oldFabBean = FileArchivatorDB.getInstance().getById(oldId);
		if((Tools.isNotEmpty(fab.getReferenceToMain()) && (oldId > 0 && (oldFabBean != null && isReplace() == false)))&& !Constants.getBoolean("fileArchivAllowPatternVersion"))
		{
			setError("components.file_archiv.upload.pattern_version", getRequest());
			return false;
		}

		return true;
	}

	//skontroluje, ci je zadany cas pre buduce nahranie korektny
	private boolean isUploadDateCorrect()
	{
		String uploadDate = getDateUploadLater();
		String uploadTime = getTimeUploadLater();

		if(Tools.isEmpty(uploadDate) || Tools.isEmpty(uploadTime))
			return false;

		Date uploadDateTime = new Date(DB.getTimestamp(uploadDate + " " + uploadTime));
		Date currentDateTime = new Date();
		return !currentDateTime.after(uploadDateTime);
	}

	protected void setError(String key,javax.servlet.http.HttpServletRequest req)
	{
		setErrorText(prop.getText(key));
	}

	private void setError(String key, String param1, javax.servlet.http.HttpServletRequest req)
	{
		setErrorText(prop.getText(key, param1));
	}

	private void setError(String key, String param1, String param2,javax.servlet.http.HttpServletRequest req)
	{
		setErrorText(prop.getText(key, param1, param2));
	}

	private boolean isCorrectEmails()
	{
		String[] emailsArray = Tools.getTokens(fab.getEmails(),",");
		for (String s : emailsArray)
		{
			if (!Tools.isEmail(s.trim()))
				return false;
		}
		return true;
	}

	private boolean replaceFile(FileBean fileBean)
	{
		if(getFileDirPath() == null)
		{
			return false;
		}

		FileArchivatorBean fabOld = FileArchivatorDB.getInstance().getById(oldId);

		//vymazanie stareho suboru
		String oldFilePath = Tools.getRealPath(fabOld.getFilePath()+fabOld.getFileName());
		IwcmFile iFile = new IwcmFile(oldFilePath);
		if(!iFile.exists() || !iFile.delete())
		{
			Logger.debug(SaveFileAction.class, "Subor :"+fabOld.getFilePath()+fabOld.getFileName()+" neexistuje, alebo sa nepodarilo zmazat.");
			return false;
		}

		String fileUrl = "/"+getFileDirPath()+fabOld.getFileName();
		String newFilePath = Tools.getRealPath(fileUrl);
		IwcmFile f = new IwcmFile(newFilePath);
		try
		{
			IwcmFsDB.writeFiletoDest(fileBean.getInputStream(),new File(f.getAbsolutePath()),Tools.safeLongToInt(fileBean.getSize()));
			boolean result = setFileAttributesAfterSave(getFileDirPath(),fabOld.getFileName());
			if(result)
				 Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (file archiv), path="+fileUrl, -1, -1);

			return result;
		}
		catch(IOException ioex)
		{
			Logger.debug(SaveFileAction.class, "Nastala chyba '"+ioex.getMessage()+"' pri ukladani suboru "+f.getPath());
			sk.iway.iwcm.Logger.error(ioex);
		}
		setError("components.file_archiv.upload.file_upload_error",getRequest());
		return false;
	}

	public FileBean getFile()
	{
		return file;
	}

	public void setFile(FileBean file)
	{
		this.file = file;
	}

	public boolean isUploadLater() {
		return uploadLater;
	}

	public void setUploadLater(boolean uploadLater) {
		this.uploadLater = uploadLater;
	}

	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	public FileArchivatorBean getFab()
	{
		return fab;
	}

	public void setFab(FileArchivatorBean fab)
	{
		this.fab = fab;
	}

	private Date getUploadLaterDate()
	{
		Date result = null;
		if(uploadLater && Tools.isNotEmpty(dateUploadLater) && Tools.isNotEmpty(timeUploadLater))
			result = new Date(DB.getTimestamp(dateUploadLater + " " + timeUploadLater));
		return result;
	}

	public String getDateUploadLater()
	{
		return dateUploadLater;
	}

	public void setDateUploadLater(String dateUploadLater)
	{
		this.dateUploadLater = dateUploadLater;
	}

	public String getTimeUploadLater()
	{
		return timeUploadLater;
	}

	public void setTimeUploadLater(String timeUploadLater)
	{
		this.timeUploadLater = timeUploadLater;
	}

	public int getOldId()
	{
		return oldId;
	}

	public void setOldId(int oldId)
	{
		this.oldId = oldId;
	}
}