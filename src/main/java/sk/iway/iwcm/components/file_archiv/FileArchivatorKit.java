package sk.iway.iwcm.components.file_archiv;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *	FileArchivatorKit.java
 *
 *
 * Title			webjet7
 * Company		Interway s.r.o. (www.interway.sk)
 * Copyright 	Interway s.r.o. (c) 2001-2015
 * @author		$Author: jeeff $(prau)
 * @version		Revision: 1.3  11.2.2015
 * created		Date: 11.2.2015 14:40:05
 * modified   	Date: 11.2.2015 14:40:05
 * Ticket 		Number: #17263
 */
public class FileArchivatorKit
{
    private Prop prop = Prop.getInstance();
    private List<String> errorsList;

    public FileArchivatorKit(Prop paramProp)
    {
        if(paramProp != null)
        {
            prop = paramProp;
        }
    }

    /** Skontroluje ci je modul File Archiv povoleny pre aktualneho usera
     *
     */
    public static boolean isArchivEnabled(HttpServletRequest request)
    {
        return isArchivEnabled(UsersDB.getCurrentUser(request));
    }

    /** Skontroluje ci je modul File Archiv povoleny pre usera
     *
     * @param userIdentity Identity
     */
    public static boolean isArchivEnabled(Identity userIdentity)
    {
        return userIdentity != null && userIdentity.isEnabledItem("cmp_file_archiv");
    }

	public static String getArchivPath()
	{
	    //robi lokalne problem pri zapnutej konf. premennej "enableStaticFilesExternalDir"
		String defaultArchivPath = "/files/archiv/";
		if(Tools.isEmpty(Constants.getString("fileArchivDefaultDirPath")))
			return defaultArchivPath;

		return Constants.getString("fileArchivDefaultDirPath");
	}

	public static String getInsertLaterPath()
	{
		String defaultInsertLaterPath = "files/archiv_insert_later/";
		if(Tools.isEmpty(Constants.getString("fileArchivInsertLaterDirPath")))
			return defaultInsertLaterPath;

		return Constants.getString("fileArchivInsertLaterDirPath");
	}

	public static String getFullInsertLaterPath()
	{
		return getArchivPath() + getInsertLaterPath();
	}

	/** Vrati unikatne meno suboru
	 *
	 * @param fileNameParam - subor
	 * @param directoryPath - cesta k suboru
	 * @param preferredDate - null / preferovany datum
	 */
	public static String getUniqueFileName(String fileNameParam, String directoryPath, String preferredDate)
	{
		String dirPath = directoryPath;
		if(Tools.isAnyEmpty(fileNameParam, dirPath) == false)
		{
			if(!dirPath.endsWith("/"))
				dirPath += "/";

			String ext = DB.internationalToEnglish(FileTools.getFileExtension(fileNameParam));
			if(Tools.isNotEmpty(ext)) ext = "."+ext;
			String fileName = DocTools.removeChars(FileTools.getFileNameWithoutExtension(fileNameParam), false);
			int version = 0;
			int loopSafe = 10000;
			String str_version;
			String dateStamp = "";
			//funkcia nebola ziadana
			if(Constants.getBoolean(FileArchivatorDB.getConstantsPrefix()+"CreateFileDateStamp"))
			{
				dateStamp = getDateStampAsString();
				if(Tools.isNotEmpty(preferredDate))
					dateStamp = preferredDate;
			}
			String fullFileName = fileName+ext;
			if(FileTools.isFile(dirPath+fullFileName))
			{
				do
				{
					version++;
					str_version = version+"";
					fullFileName = fileName+"_v_"+str_version+dateStamp+ext;
				}while( FileTools.isFile(dirPath+fullFileName) && version < loopSafe);
				return fullFileName;
			}
			return fileName+ext;
		}
		return null;
	}

	/**
	 * zamena obsahu suborov dirPath+fileName &lt;-&gt; oldFileBean.getFilePath()+oldFileBean.getFileName()
	 * BHR: musel som prerobit z Tools.renameFile, pretoze sa stalo, ze niekedy nezmazalo zdrojovy subor a teda sa premenovanie nedokoncilo
	 */
	public static boolean renameFile(String dirPath, String fileName, FileArchivatorBean oldFileBean)
	{
		boolean renamed = true;
		IwcmFile oldFile = new IwcmFile(Tools.getRealPath(oldFileBean.getFilePath()+oldFileBean.getFileName()));
		IwcmFile newFile = new IwcmFile(Tools.getRealPath(dirPath+fileName));
		IwcmFile tmpFile = new IwcmFile(Tools.getRealPath(dirPath+"empty_file_for_rename_only"+getFileExtension(fileName)));
		try
		{
			//copy old file to temp file for later usage
			if(FileTools.copyFile(oldFile, tmpFile) == false)
			{
				 Logger.error(FileArchivatorKit.class, "renameFile: nepodarilo sa premenovat "+oldFile.getVirtualPath()+" > "+tmpFile.getVirtualPath());
				 renamed = false;
			}
			//owerwrite old file with new file
			if(FileTools.copyFile(newFile, oldFile) == false)
			{
				 Logger.error(FileArchivatorKit.class, "renameFile: nepodarilo sa premenovat "+newFile.getVirtualPath()+" > "+oldFile.getVirtualPath());
				 renamed = false;
			}
			//copy temp (old) file over new file
			if(FileTools.copyFile(tmpFile, newFile) == false)
			{
				 Logger.error(FileArchivatorKit.class, "renameFile: nepodarilo sa premenovat "+tmpFile.getVirtualPath()+" > "+newFile.getVirtualPath());
				 renamed = false;
			}
		}
		catch(Exception e)
		{
			renamed = false;
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			if(renamed && tmpFile.exists())
				 tmpFile.delete();
		}
		return renamed;
	}

	public static boolean reSetReference(Long oldReferenceId, Long newReferenceId)
	{
		List<FileArchivatorBean> files = FileArchivatorDB.getByReferenceId(oldReferenceId);
		if(files != null)
		{
			for(FileArchivatorBean file:files)
			{
				Logger.debug(FileArchivatorKit.class, "Change reference from "+oldReferenceId+" to "+newReferenceId+" file: "+file.getFilePath()+file.getFileName());
				file.setReferenceId(newReferenceId);
				if(!file.save())
				{
					return false;
				}
			}
		}
		FileArchivatorBean newBean = FileArchivatorDB.getInstance().getById(newReferenceId);
		if (newBean != null)
		{
			newBean.setReferenceId(null);
			//force save to call FullTextIndexer
			newBean.save();
		}
		return true;
	}

	public static String getMD5(IwcmFile iwcmFile)
	{
		String md5Hex = "null";
		if(iwcmFile == null || iwcmFile.exists() == false)
		{
			return md5Hex;
		}

		try
		{
			md5Hex = DigestUtils.md5Hex(new IwcmInputStream(iwcmFile));
		}
		catch(Exception exc)
		{
			Logger.debug(FileArchivatorKit.class, "Zlyhalo generovanie MD5 odtlacku");
			sk.iway.iwcm.Logger.error(exc);
		}
		return md5Hex;
	}

	/** Vrati datum ako String.
	 *
	 */
	public static String getDateStampAsString()
	{
		return getDateStampAsString(null);
	}

	/** Vrati datum a cas ako String.
	 *
	 */
	public static String getDateStampAsString(Date date)
	{
		Calendar now = Calendar.getInstance();
		if(date != null)
			now.setTime(date);
		StringBuilder dateString = new StringBuilder();
		dateString.append(now.get(Calendar.YEAR)).append(".").append(now.get(Calendar.MONTH)+1).append(".").append(now.get(Calendar.DAY_OF_MONTH)).append("_").append(now.get(Calendar.HOUR_OF_DAY)).append(".").append(now.get(Calendar.MINUTE));
		return dateString.toString();
	}

	public static String getFileExtension(String fileName)
	{
		if(fileName == null)
			return "empty";
		if(fileName.lastIndexOf(".") != -1)
			return fileName.toLowerCase().substring(fileName.lastIndexOf("."));
		return "nul";
	}

    public static String getFileExtension(String fileName, boolean allowNull)
    {
        if(fileName == null)
        {
            if(allowNull)
                return null;
            return "empty";
        }
        if(fileName.lastIndexOf(".") != -1)
            return fileName.toLowerCase().substring(fileName.lastIndexOf("."));

        if(allowNull)
            return null;

        return "nul";
    }

	public static void incrementOrderId(Long referenceId)
	{
		List<FileArchivatorBean> files = FileArchivatorDB.getByReferenceId(referenceId);
		int oldOrder = -1;
		if(files != null)
		{
			for(FileArchivatorBean file:files)
			{
				oldOrder = file.getOrderId();
				if(file.getOrderId() == -1)
					file.setOrderId(2);
				else
					file.setOrderId(file.getOrderId()+1);
				Logger.debug(FileArchivatorKit.class, "Increment orderId from "+oldOrder+" to "+file.getOrderId()+" file: "+file.getFilePath()+file.getFileName());
				file.save();
			}
		}
		//musim tu vymazat cache
		FileArchivatorKit.deleteFileArchiveCache();
		Logger.debug(FileArchivatorKit.class, "Increment orderId for referenceId = "+referenceId);
	}

	public static String[] getDomainNames()
	{
		return Tools.getTokens(Constants.getString("fileArchivDomainsName"), ",");
	}

	/** Zisti ci na subor ktory chceme nahrat uz v databaze neexistovala URL a subor bol zmazany bez zmazania zaznamu o subore
	 *
	 */
	public static boolean existsPathInDB(String path)
	{
		if(path != null && path.lastIndexOf("/")+1 < path.length())
		{
			String filePath = path.substring(0,path.lastIndexOf("/")+1);
			String fileName = path.substring(path.lastIndexOf("/")+1);
			return FileArchivatorDB.getByPath(filePath, fileName) != null;
		}
		return false;
	}

	/**Skontroluje konzisteciu suborov, ak niektory chyba, vrati naplneny string. Pozor ! Vypoctovo narocne, prechadza vsetky zaznamy v DB a fyzicky kontroluje ci subory existuju
	 *
	 */
	public static String checkFileConsistency()
	{
		StringBuilder result = new StringBuilder();
		for(FileArchivatorBean fab: FileArchivatorDB.getInstance().getAll())
		{
			if(!FileTools.isFile(fab.getFilePath()+fab.getFileName()))
				result.append("Error file ").append(fab.getFilePath()).append(fab.getFileName()).append(" is missing ! Id: ")
						 .append(fab.getId()).append("<br>");
		}
		return result.toString();
	}

	/** Ziska security hash zo stringu, pri chybe vrati prazdny string
	 *
	 */
	public static String getSecurityHash(String input)
	{
		String ret = "";
		try{
			String source = input + input.length();
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(source.getBytes());
			byte[] byteData = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte byteDatum : byteData) {
				sb.append(Integer.toString((byteDatum & 0xff) + 0x100, 16).substring(1));
			}
			ret = sb.toString();
		}
		catch (Exception e)
		{
			Logger.debug(FileArchivatorKit.class, "Problem generating security hash. Cause: " + e.getMessage());
		}
		return ret;
	}

	/** Ak uz fyzicky existuje subor s rovnakym hash-om, vratime list beanov, inak null
	 *
	 */
	public static List<FileArchivatorBean> existSameFiles(FileArchivatorBean newFab, boolean removePattern)
	{
		List<FileArchivatorBean> returnList = new ArrayList<>();
		List<FileArchivatorBean> fabHashList = FileArchivatorDB.getByHash(newFab);

		// fabHashList.size() > 1 preto, lebo 1 je newFab
		if(fabHashList != null && fabHashList.size() > 1)
		{

			for(FileArchivatorBean fab: fabHashList )
			{
				//odstranime prave nahraty subor, archivy a vzory
				if(fab.getId() != newFab.getId() && fab.getReferenceId() == -1 && (!removePattern || Tools.isEmpty(fab.getReferenceToMain())))
				{
					returnList.add(fab);
				}
			}
			if(returnList.size() > 0)
				return returnList;
		}
		return null;
	}

	/** Zmaze celu cache archivu suborov na vsetkych nodoch ak je systemova premenna "fileArchiv-delete-cache-public-node"
	 *
	 *
	 */
	public static void deleteFileArchiveCache()
	{
		//refresh aktualneho nodu node
		Cache.getInstance().removeObjectStartsWithName(FileArchivatorDB.getCachePrefix());

		//refresh nodov clustra
		if(ClusterDB.isServerRunningInClusterMode() &&
			Constants.getBoolean(FileArchivatorDB.getCachePrefix()+"delete-cache-public-node"))
		{
			 ClusterDB.addRefresh("sk.iway.iwcm.Cache-" + FileArchivatorDB.getCachePrefix());
		}
	}

	public static int generateNextGlobalId()
	{
		return PkeyGenerator.getNextValue("file_archiv_global_id");//nn_file_archives_global_id
	}

    public static Collection<String> createCollection(String paramName, HttpServletRequest req)
    {
        String[] propertyArray = Tools.getTokens(Tools.getParameter(req,paramName), "+");
        if(propertyArray != null && propertyArray.length > 0)
            return Arrays.asList(propertyArray);
        return null;
    }

    public static String getVal(HttpServletRequest request, String parameterName)
    {
        if(request.getParameter(parameterName) == null)
            return "";
        return Tools.getParameter(request,parameterName);
    }

    /** bean ulozeny v ResultArchivBean.fab je potrebne ulozit.
     *
     * @param fab FileArchivatorBean (Musi mat vyplneny filePath - cestu k subroru bez lomitka na zaciatku (napr files/archiv/89/) a fileName - nazov suboru s priponou (priloha_1.pdf) a userId - id usera ktory subor nahrava)
     * @param oldId - id suboru, ktory aktualizujeme
     * @return ResultArchivBean
     */
    //prepareAndValidate
    public ResultArchivBean prepareAndValidate(FileArchivatorBean fab, Long oldId, UserDetails user)
    {
        return prepareAndValidate(fab, oldId, user, false) ;
    }

    public ResultArchivBean prepareAndValidate(FileArchivatorBean fab, Long oldId, UserDetails user, boolean isEdit)
    {
        ResultArchivBean resultBean = new ResultArchivBean();
        if(errorsList == null)
            errorsList = new ArrayList<>();
        else
            errorsList.clear();

        if(user == null)
        {
            return addErrorMessageAndReturnFalse("error.userNotLogged");
        }

        if(!isEdit && FileArchivatorKit.existsPathInDB(fab.getFilePath()+fab.getFileName()))
        {
            return addErrorMessageAndReturnFalse("components.file_archiv.file_exists");
        }

        if(Tools.isEmpty(fab.getVirtualFileName()))
        {
            return addErrorMessageAndReturnFalse("components.file_archiv.file_virtual_cannot_be_empty");
        }

        IwcmFile file = new IwcmFile(Tools.getRealPath(fab.getFilePath()+fab.getFileName()));

        fab.setUserId(user.getUserId());
        fab.setMd5(FileArchivatorKit.getMD5(file));
        fab.setFileSize(file.length());
        fab.setDomainId(CloudToolsForCore.getDomainId());
        resultBean.setFab(fab);
        // Logger.debug(SaveFileAction.class, "saveFile() IP: "+Tools.getRemoteIP(getRequest())+" file: "+fileName+","+param_oldId_name+":"+getOldId());

        boolean isDestinationFolderWritable = true;
        //kontrola prav na zapis do suboru
        if(!user.isFolderWritable(fab.getFilePath()))
        {
            Logger.debug(FileArchivatorKit.class, "User nema pravo na zapis do priecinku:  "+fab.getFilePath());
            setError("components.elfinder.commands.upload.error");
            isDestinationFolderWritable = false;
        }

        if (isDestinationFolderWritable && checkFileProperties(file.getName(), file.getLength(), fab.getFilePath()+fab.getFileName(), oldId))
        {
            if(setFilePropertiesAfterUpload(fab, oldId))
            {
                resultBean.setSuccess(true);
                return resultBean;
            }
        }

        resultBean.setErrors(errorsList);
        resultBean.setSuccess(false);
        return resultBean;
    }

    private ResultArchivBean addErrorMessageAndReturnFalse(String propText)
    {
        setError(propText);
        ResultArchivBean rab = new ResultArchivBean();
        rab.setErrors(errorsList);
        rab.setSuccess(false);
        return rab;
    }

    protected void setError(String key)
    {
        addErrorText(prop.getText(key));
    }

    private void setError(String key, String param1)
    {
        addErrorText(prop.getText(key, param1));
    }

    private void setError(String key, String param1, String param2)
    {
        addErrorText(prop.getText(key, param1, param2));
    }

    private void addErrorText(String text)
    {
        if(errorsList == null)
            errorsList = new ArrayList<>();
        errorsList.add(text);
    }

	 public boolean checkFileProperties(String fileName, long length, String pathName, Long oldId)
	 {
		  // errorsList = new ArrayList<String>();
		  if(fileName == null || fileName.length() < 5)
		  {
				//Logger.debug(SaveFileAction.class, "checkFileProperties() fileName je null alebo ma kratky nazov. Subor: "+fileName);
				setError("components.file_archiv.upload.file_not_exists_or_short_name", fileName);
				return false;
		  }

		  if(!hasAllowedExtensions(fileName, oldId))
		  {
				return false;
		  }

		  if(length >= getMaxFileSize())
		  {
				//Logger.debug(SaveFileAction.class, "Velkost suboru je prekrocena. Aktualna velkost: "+length+" maximalna velkost: "+getMaxFileSize());
				setError("components.file_archiv.upload.file_is_too_big", length+"", getMaxFileSize()+"");
				return false;
		  }


		  //ak uz existuje referencia v databaze k suboru ktory este len ideme nahrat, tak je to problem. Niekto zmazal subor rucne.
		  //subor nemozeme nahrat pretoze by mohol byt zmazany inym zaznamom v databaze - omylom
		  if(FileArchivatorKit.existsPathInDB(pathName) && !FileTools.isFile(pathName) )
		  {
				setError("components.file_archiv.upload.db_enrty_exists");
				return false;
		  }

		  return true;
	 }

    /** Sluzi na validaciu {@link FileArchivatorBean} pred ulozenim. Skontroluje velkost suboru, priponu atd.
     * @return Ak vrati true, mozeme {@link FileArchivatorBean} ulozit.
     */
    public boolean hasAllowedExtensions(String fileName, Long oldId)
    {
        if(Tools.isEmpty(fileName))
        {
            setError("components.file_archiv.upload.file_is_null");
            return false;
        }

        //nahravam novu verziu dokumentu, musia ma rovnaku priponu
        if(oldId != null && oldId.longValue() > 0)
        {
	        FileArchivatorBean fab = FileArchivatorDB.getInstance().getById(oldId);
	        if(fab != null)
	        {
		        if(getFileExtension(fab.getFileName()).equalsIgnoreCase(getFileExtension(fileName)))
		        	return true;
		        else
		            setError("components.file_archiv.upload.file_shoud_have_end_with",getFileExtension(fab.getFileName()));
	        }
	        else
	        {
		        setError("components.file_archiv.upload.wrong_oldId", String.valueOf(oldId));
	        }
        }
        else
        {
	        if(Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivAllowExt"), ","), getFileExtension(fileName)))
	        	return true;
	        else
	        {
		        //Logger.debug(SaveFileAction.class, "File "+fileName+" has not allowed extension. Allowed extensions : "+Constants.getString("fileArchivAllowExt"));
		        setError("components.file_archiv.upload.file_not_allowed_extensions", Constants.getString("fileArchivAllowExt") , getFileExtension(fileName));
	        }
        }

        return false;
    }

    public static long getMaxFileSize()
    {
        return Tools.getIntValue(Constants.getString("fileArchivMaxUploadFileSize"), 60000000);
    }

    public boolean setFilePropertiesAfterUpload(FileArchivatorBean newFab, Long oldId)
    {
        if(!FileTools.isFile(newFab.getFilePath()+newFab.getFileName()))
        {
            addErrorText(prop.getText("components.import_web_pages.import_error"));
            return false;
        }

        newFab.setDomainId(CloudToolsForCore.getDomainId());

        //ak uz existuje nejaka starsia verzia naseho suboru
        boolean result = true;
        boolean newVersion = false;
        if(oldId > 0)
        {
            newVersion = true;
            FileArchivatorBean oldFab = FileArchivatorDB.getInstance().getById(oldId);
            if(oldFab == null)
            {
                Logger.debug(FileArchivatorKit.class, "setFilePropertiesAfterUpload() fileBean == null");
                addErrorText(prop.getText("components.file_archiv.older_file_not_exists"));
                return false;
            }

            if(!newFab.getFilePath().equals(oldFab.getFilePath()))
            {
                Logger.debug(FileArchivatorKit.class, "zdrojovy a cielovy priecinok, nie su rovnake");
                addErrorText(prop.getText("components.file_archiv.source_and_destination_file_different"));
                return false;
            }

            //premenujeme (vymenime) subory stary->novy a novy->stary
            if(FileArchivatorKit.renameFile(newFab.getFilePath(), newFab.getFileName(), oldFab))
            {
                //premenujeme v DB
                String tempNameUrl = oldFab.getFileName();
                oldFab.setFileName(newFab.getFileName());
                newFab.setFileName(tempNameUrl);
                //premenujeme aj cestu
                tempNameUrl = oldFab.getFilePath();
                oldFab.setFilePath(newFab.getFilePath());
                newFab.setFilePath(tempNameUrl);
                //ak ideme na newFab nastavovat referenciu, musi byt ulozeny aby mal vytvorene ID.
                if(newFab.getId() <= 0)
                    newFab.save();
                //zmenime referenciu v hlavnom subore
                oldFab.setReferenceId(newFab.getId());

                newFab.setCategory(oldFab.getCategory());
                newFab.setVirtualFileName(oldFab.getVirtualFileName());
                newFab.setFieldA(oldFab.getFieldA());
                newFab.setFieldB(oldFab.getFieldB());

                //globalne id je rovnake pre vsetky subory vo vlakne.
                newFab.setGlobalId(oldFab.getGlobalId());
                if(!oldFab.save() || !newFab.save())
                {
                    result = false;
                }
                else
                {
                    addSaveLogToAdminlog(newFab, newVersion);
                }



                //zmenime referenciu u suborov, ktore na neho odkazuju (a po novom budu odkazovat na novy subor)
                if(!FileArchivatorKit.reSetReference(oldId, newFab.getId()))
                    result = false;

                //posunieme order_id o jednu uroven
                FileArchivatorKit.incrementOrderId(newFab.getId());
                return result;
            }
            else
            {
                addErrorText(prop.getText("components.file_archiv.rename_error"));
                return false;
            }
        }
        else
        {
            //novemu suboru vo vlakne nastavime nove globalne ID
            newFab.setGlobalId(FileArchivatorKit.generateNextGlobalId());
            addSaveLogToAdminlog(newFab, newVersion);
            newFab.save();
        }

        return true;
    }

    private static void addSaveLogToAdminlog(FileArchivatorBean fab, boolean newVersion)
    {
        addSaveLogToAdminlog(fab, newVersion, false);
    }

    private static void addSaveLogToAdminlog(FileArchivatorBean fab, boolean newVersion, boolean replace)
    {
        String strNewVersion = "";
        if(newVersion)
            strNewVersion = "(Aktualizacia existujuceho suboru) ";
        if(replace)
            strNewVersion = "(Nahradenie povodneho suboru) ";

        Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "EDIT: File Archiv "+strNewVersion+"ulozenie:\n"+fab.toString(true), fab.getFileArchiveId(), -1);
    }

    public static String getPojoZmeny(Object newObj,Object originalObj)
    {
        if(newObj == null || originalObj == null)
            return "Bez zmeny";
        BeanDiff diff = new BeanDiff().setNew(newObj).setOriginal(originalObj);
        return new BeanDiffPrinter(diff).toString();
    }

	public List<String> getErrorsList()
	{
		return errorsList;
	}

	/***************************** BACKWARD COMPATIBILITY ******************************/

	/**
	 * Deprecated, use:
	 * FileArchiveService fas = new FileArchiveService(getRequest(), getProp(), entity, repository);
	 * result = fas.deleteStructure();
	 * if(result != null) throwError(result);
	 *
	 * @param fabId
	 * @param user
	 * @return
	 */
	@Deprecated
	public static boolean deleteStructure(int fabId, UserDetails user)
	{
		FileArchiveService fas = FileArchiveService.getInstance(new Identity(user), (long)fabId);
		String result = fas.deleteStructure();
		if (result != null) return false;
		return true;
	}

}
