package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.file_archiv.FileArchiveService.UploadType;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

/**
 * Provides shared state, validation, path handling, and file operations for file archive services.
 * Subclasses use these helpers to keep archive records and their physical files synchronized.
 */
public abstract class FileArchivSupportMethodsService {

	protected static String cachePrefix = "fileArchiv-";
	private static final String CONSTANTS_PREFIX = "fileArchiv";
	private static final String CACHE_TIME_KEY = "CacheTime";
	public static final String SEPARATOR = "/";

	protected HttpServletRequest request;
    protected Prop prop;
	protected Identity currentUser;
	protected int domainId;
	protected FileArchiveRepository repository;

	protected FileArchivatorBean fab;
	protected long referenceId;

	protected boolean saveLater = false;
	protected boolean renameFile = false;
	protected int saveAfterId = -1;

	protected boolean isPatternFile = false;

	protected UploadType uploadType;

	protected String fileToUploadName;
	protected IwcmFile fileToUpload;

	protected List<String> errorList = new ArrayList<>();
	protected String[] errorParams;
	protected List<FileArchivatorBean> sameFiles = new ArrayList<>();

	/**
	 * Redirects pattern references from an old main-file path to the current archive file.
	 *
	 * @param oldReferenceToMain previous virtual path of the main file
	 */
    protected void updateReferenceToMainFile(String oldReferenceToMain)
	{
		repository.updateReferenceToMain(oldReferenceToMain, fab.getVirtualPath(), domainId);
	}

	/**
	 * Updates the pattern reference stored by every history record in a file thread.
	 *
	 * @param referenceId identifier of the main file whose history is updated
	 * @param referenceToMain new main-file virtual path
	 */
	protected void updateReferenceToMainFile(Long referenceId, String referenceToMain)
	{
		repository.updateReferenceToMain(referenceId, referenceToMain, domainId);
	}
    /**
     * Checks whether the configured delayed-upload date is in the future.
     *
     * @param fromEditorFields {@code true} to read the pending editor value; {@code false} to read the entity value
     * @return {@code true} when the selected upload date is present and still in the future
     */
    protected boolean isUploadDateCorrect(boolean fromEditorFields) {
		Date uploadLAterDate = null;

		if(fromEditorFields == true)
			uploadLAterDate = fab.getEditorFields().getDateUploadLater();
		else
			uploadLAterDate = fab.getDateUploadLater();

		if(uploadLAterDate == null) return false;
		return uploadLAterDate.after(new java.util.Date());
	}

	/**
	 * Validates the comma-separated notification addresses configured for a delayed upload.
	 *
	 * @param fromEditorFields {@code true} to read the pending editor value; {@code false} to read the entity value
	 * @return {@code true} when at least one address is present and every address is valid
	 */
	protected boolean isCorrectEmails(boolean fromEditorFields) {
		String emailsStr = null;

		if(fromEditorFields == true)
			emailsStr = fab.getEditorFields().getEmails();
		else
			emailsStr = fab.getEmails();

		if(Tools.isEmpty(emailsStr)) return false;

		String[] emailsArray = Tools.getTokens(emailsStr,",");
		for (String s : emailsArray) {
			if (!Tools.isEmail(s.trim()))
				return false;
		}
		return true;
	}

	/**
	 * Detects whether the main file changed since the current edit operation began.
	 *
	 * @param oldId identifier of the file originally loaded for editing
	 * @param referenceId identifier used to reload the current archive record
	 * @return {@code true} when the reloaded record is no longer the main file
	 */
	protected boolean isConcurrentModification(Long oldId, Long referenceId) {
		if(oldId > 0) {
			FileArchivatorBean fileBean = repository.findFirstByIdAndDomainId(referenceId, domainId).orElse(null);
			if(fileBean != null && fileBean.getReferenceId() != -1) {
				Logger.debug(FileArchiveService.class, "Pozor !!! Moze nastat ConcurrentModification pri id: " + oldId + " subor: " + fileBean.getFileName());
				return true;
			}
		}
		return false;
	}

	/**
	 * Selects the configured archive destination, optionally deriving it from the file category.
	 *
	 * @return preferred virtual destination path
	 */
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

	/**
	 * Resolves and validates the final destination directory without creating it.
	 *
	 * @return normalized directory path, or {@code null} when it is outside the archive root
	 */
	protected String resolveFileDestinationDirPath() {
		return validateFileDirPath(getPreferredDirPath());
	}

	/**
	 * Resolves and validates the physical storage directory without creating it.
	 * Scheduled uploads use the insert-later staging prefix.
	 *
	 * @return normalized storage directory path, or {@code null} when it is outside the archive root
	 */
	protected String resolveFileDirPath() {
		String dirPath = resolveFileDestinationDirPath();
		if(dirPath == null || saveLater == false) return dirPath;
		return validateFileDirPath(FileArchivatorKit.getFullInsertLaterPath() + dirPath);
	}

	/**
	 * Normalizes a directory and verifies that it remains within the configured archive root.
	 *
	 * @param dirPath virtual directory path to validate
	 * @return normalized directory path, or {@code null} when validation fails
	 */
	private String validateFileDirPath(String dirPath) {
		dirPath = normalizePath(dirPath);
		String fileArchivPath = normalizePath( FileArchivatorKit.getArchivPath() );

		if(Tools.isEmpty(dirPath) || dirPath.startsWith(fileArchivPath) == false) {
			Logger.debug(FileArchivSupportMethodsService.class, "Not allowed path. Allowed path is: " + FileArchivatorKit.getArchivPath());
			errorList.add("components.file_archiv.upload.file_has_not_allowed_path");
			errorParams = new String[] {fileArchivPath};
			return null;
		}

		return dirPath;
	}

	/**
	 * Resolves the physical storage path and creates its directory when it does not exist.
	 *
	 * @return normalized storage directory path, or {@code null} when validation fails
	 */
	protected String getFileDirPath() {
		String dirPath = resolveFileDirPath();
		if(dirPath == null) return null;

		IwcmFile fileDir = new IwcmFile(Tools.getRealPath(dirPath));
		if (!fileDir.exists()) fileDir.mkdirs();

		//Just in case check and remove double //
		return dirPath;
	}

	/**
	 * Populates archive persistence fields from the file placed in its storage directory.
	 *
	 * @param dirPath normalized storage directory
	 * @param fileName stored file name
	 * @param referenceId identifier of the main file, or {@code -1} for a new main file
	 * @param isNew {@code true} to reset the entity identifier before persistence
	 */
	protected void prepareFileArchivatorBean(String dirPath, String fileName, Long referenceId, boolean isNew)
	{
		//uz sa musim tvarit ako novy subor
		IwcmFile newfile = new IwcmFile(Tools.getRealPath(dirPath + fileName));
		if(isNew == true) fab.setFileArchiveId(0);
		fab.setFilePath(dirPath);
		fab.setFileName(fileName);
		fab.setReferenceId(referenceId);
		fab.setUserId(currentUser.getUserId());
		fab.setUploaded(this.saveLater ? 0 : -1);
		fab.setMd5(FileArchivatorKit.getMD5(newfile));
		fab.setFileSize(newfile.length());
		fab.setDomainId(domainId);
	}

	/**
	 * Finds uploaded main files whose content hash matches the candidate file.
	 *
	 * @param newFab candidate archive record
	 * @param removePattern {@code true} to exclude pattern files from the result
	 * @param isBeforeSave {@code true} to calculate the candidate hash from the temporary upload
	 */
	protected void findSameFiles(FileArchivatorBean newFab, boolean removePattern, boolean isBeforeSave)
	{
		//The MD5 hash isne generated YET
		if(isBeforeSave) newFab.setMd5( FileArchivatorKit.getMD5(fileToUpload) );

		List<FileArchivatorBean> fabHashList = FileArchivatorDB.getByHash(newFab);
		if(fabHashList != null)
		{
			for(FileArchivatorBean fab2: fabHashList) {
				//odstranime archivy a vzory
				if(fab2.getReferenceId() == -1 && (!removePattern || Tools.isEmpty(fab2.getReferenceToMain()))) {
					Logger.debug(FileArchiveService.class, "Same hash " + newFab.getMd5() + " in file: " + fab2.getVirtualPath());
					sameFiles.add(fab2);
				}
			}
		}
	}

	/**
	 * Deletes an archive file and its database record after verifying user permissions.
	 *
	 * @param fabToDelete archive record to remove
	 * @param prefixText description prefix used in diagnostic messages
	 * @param ignoreMissingFile {@code true} to delete the record even when the physical file is absent
	 * @return {@code true} when permission and physical-file checks allow the record deletion attempt
	 */
	protected boolean deleteFile(FileArchivatorBean fabToDelete, String prefixText, boolean ignoreMissingFile)
	{
		if(checkPerms() == false) return false;

		if(fabToDelete == null) {
			Logger.debug(FileArchiveService.class, prefixText+" FAB je null ");
			return false;
		}

		if(!currentUser.isFolderWritable( normalizePath(fabToDelete.getFilePath()) )) {
			Logger.debug(FileArchiveService.class, "Pouzivatel "+((currentUser != null)?"id: "+currentUser.getUserId():"null")+" nema pravo na zmazanie ("+prefixText+") suboru: "+fabToDelete.getVirtualPath()+" ");
			return false;
		}

		boolean isSuccess = true;
		IwcmFile iFile = new IwcmFile( fabToDelete.getRealPath() );

		if(iFile.exists() == true) {
			if(iFile.delete() == false) {
				Logger.debug(FileArchiveService.class, prefixText + " subor :" + fabToDelete.getVirtualPath() + " sa nepodarilo zmazat.");
				return false;
			}
		} else {
			Logger.debug(FileArchiveService.class, prefixText + " subor :" + fabToDelete.getVirtualPath() + " sa nenašiel.");

			if(ignoreMissingFile == true) {
				//File is missing but we IGNORE it -> for example during DELETE action, so we can delete it from DB even if it is not on disk
				//Do nothing
			} else {
				//Problem
				return false;
			}
		}

		// delete file index
		new SimpleQuery().execute("DELETE FROM documents WHERE external_link=?", "'/" + fabToDelete.getVirtualPath() + "'");

		if(fabToDelete.delete()) {
			String fileDescription = "Subor "+fabToDelete.getVirtualPath()+" zmazany \n "+fabToDelete.toString();
			Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, fileDescription, currentUser.getUserId(), -1);
		}

		return isSuccess;
	}

	/**
	 * Deletes all pattern files that refer to the supplied main file.
	 *
	 * @param file main archive record
	 * @param ignoreMissingFile {@code true} to remove records whose physical files are already absent
	 * @return {@code true} when every matching pattern is removed
	 */
	protected boolean deleteFilePatterns(FileArchivatorBean file, boolean ignoreMissingFile) {
		if(file == null) return false;

		List<FileArchivatorBean> fabList = repository.findAllByReferenceToMainAndDomainId(file.getVirtualPath(), domainId);
		if(fabList == null) return true;

		boolean isSuccess = true;
		for(FileArchivatorBean patternToDelete: fabList) {
			if(deleteFile(patternToDelete, "Vzor ", ignoreMissingFile)) {
				Logger.debug(FileArchiveService.class, "Vzor subor: "+patternToDelete.getVirtualPath()+" zmazany.");
			} else {
				isSuccess = false;
				Logger.debug(FileArchiveService.class, "Vzor subor: "+patternToDelete.getVirtualPath()+" sa nepodarilo zmazat.");
			}
		}

		return isSuccess;
	}

	/**
	 * Resolves the cache lifetime for a specific archive operation with a component-wide fallback.
	 *
	 * @param methodName operation name used in the method-specific configuration key
	 * @return cache lifetime in minutes
	 */
	protected static int getCacheTime(String methodName)
	{
		int timeMinutes = Tools.getIntValue(Constants.getInt(CONSTANTS_PREFIX+CACHE_TIME_KEY+"-"+methodName),-1) ;
		if(timeMinutes >= 0 )
			return timeMinutes;

		timeMinutes = Tools.getIntValue(Constants.getInt(CONSTANTS_PREFIX+CACHE_TIME_KEY),-1) ;
		if(timeMinutes >= 0 )
			return timeMinutes;

		return 120;
	}

	/**
	 * Normalizes an archive directory path to include leading and trailing separators.
	 *
	 * @param path path to normalize
	 * @return normalized path, or the original empty value
	 */
	public static String normalizePath(String path) {
		if(Tools.isEmpty(path)) return path;

		if(path.startsWith(SEPARATOR) == false) path = SEPARATOR + path;
		if(path.endsWith(SEPARATOR) == false) path += SEPARATOR;

		//Just in case remove double //
		return path.replace("//", SEPARATOR);
	}

	/**
	 * Converts an archive directory to the legacy form without a leading separator.
	 *
	 * @param path path to normalize
	 * @return legacy path form, or an empty string for empty input
	 */
	public static String normalizeToOldPath(String path) {
		if(Tools.isEmpty(path)) return "";

		if(path.startsWith(SEPARATOR) == true) path = path.substring(1);
		if(path.endsWith(SEPARATOR) == false) path = path + SEPARATOR;

		//Just in case remove double //
		return path.replace("//", SEPARATOR);
	}

	/**
	 * Checks the permissions and feature flag required by the current archive operation.
	 *
	 * @return {@code true} when the current user may perform the selected operation
	 */
	protected final boolean checkPerms() {
		// For everything we needs this perm
		if(currentUser.isEnabledItem("cmp_file_archiv") == false) return false;

		// For edit / delete / rollback
		if(currentUser.isEnabledItem("cmp_fileArchiv_edit_del_rollback") == false) return false;

		// For history upload
		if(this.uploadType == UploadType.HISTORY_VERSION && currentUser.isEnabledItem("cmp_fileArchiv_advanced_settings") == false) return false;

		// If we inserting new file we need this constant to be true
		if(this.uploadType != UploadType.NO_ACTION && Constants.getBoolean("fileArchivCanEdit") == false) return false;

		// ELSE all good
		return true;
	}
}
