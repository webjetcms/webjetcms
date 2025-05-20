package sk.iway.iwcm.components.file_archiv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.validation.Errors;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.AdminUploadServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public class FileArchiveService extends FileArchivSupportMethodsService {

	private static final String DELETE_WAITING_ERR = "components.file_archiv.waiting_file.delete_err";
	private static final String DELETE_STRUCTURE_ERR = "components.file_archiv.delete_structure_err";
	private static final String FILE_FIELD = "errorField.editorFields.file";
	private static final String EMPTY_FILE_ERR = "components.file_archiv.error.empty_file";
	private static final String FILE_UPLOAD_ERR = "components.file_archiv.upload.file_upload_error";
	private static final String ACTION_NOT_SUPPORTED = "components.file_archiv.action_not_allowed";
	private static final String RECORD_NOT_FOUND = "components.file_archiv.not_found_archiv_record";
	private static final String DB_SAVE_FAILED = "components.file_archiv.upload.after_save_error";
	private static final String LOGGER_USER_ID = "Pouzivatel id: ";

	private static final String UPLOAD_NEW_FILE_VERSION = "uploadNewFileVersion";
	private static final String MOVE_BEHIND = "moveBehind";

	private static final String PERMISSION_DENIED = "admin.operationPermissionDenied";

	public enum UploadType {
		NO_ACTION,
		NEW_VERSION,
		REPLACEMENT,
		HISTORY_VERSION
	}

	public UploadType getUploadType(String key) {
		if(Tools.isEmpty(key)) return UploadType.NO_ACTION;

		key = key.trim().toLowerCase();

		switch(key) {
			case "new_version":
				return UploadType.NEW_VERSION;
			case "replacement":
				return UploadType.REPLACEMENT;
			case "history_version":
				return UploadType.HISTORY_VERSION;
			default:
				return UploadType.NO_ACTION;
		}
	}

	public static final List<LabelValue> getUploadTypeOptions(Prop prop) {
		List<LabelValue> options = new ArrayList<>();

		//default option - need be first
		options.add(new LabelValue(prop.getText("components.file_archiv.upload_like.no_action"), UploadType.NO_ACTION.toString()));
		options.add(new LabelValue(prop.getText("components.file_archiv.upload_like.new_version"), UploadType.NEW_VERSION.toString()));
		options.add(new LabelValue(prop.getText("components.file_archiv.upload_like.replacement"), UploadType.REPLACEMENT.toString()));
		options.add(new LabelValue(prop.getText("components.file_archiv.upload_like.history_version"), UploadType.HISTORY_VERSION.toString()));

		return options;
	}

	public static final List<LabelValue> getStatusIconOptions(Prop prop) {
		List<LabelValue> statusIcons = new ArrayList<>();
		statusIcons.add(new LabelValue(prop.getText("components.file_archiv.statusIcon.all_file"), "referenceId:gte:-1"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-star\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.statusIcon.main_file"), "referenceId:eq:-1"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-star-off\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.statusIcon.not_main_file"), "referenceId:gt:0"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-map-pin\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.statusIcon.show_file"), "showFile:true"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-map-pin-off\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.statusIcon.not_show_file"), "showFile:false"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-texture\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.pattern"), "referenceToMain:notEmpty"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-calendar-time\" style=\"color: #FF4B58;\"></i> " + prop.getText("components.file_archiv.statusIcon.waiting_for_upload"), "uploaded:0"));
		statusIcons.add(new LabelValue("<i class=\"ti ti-calendar-plus\" style=\"color: #000000;\"></i> " + prop.getText("components.file_archiv.statusIcon.waiting_new_version"), "waitingFiles"));
		return statusIcons;
	}

	public FileArchiveService(Identity user, Prop prop, FileArchivatorBean fab, FileArchiveRepository repository) {

		if (prop == null) prop = Prop.getInstance();
		if (repository == null) repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);

		this.prop = prop;
		this.currentUser = user;
		this.domainId = CloudToolsForCore.getDomainId();
		this.repository = repository;

		//for safety
		if(fab.getEditorFields() == null) {
			FileArchivatorEditorFields faef = new FileArchivatorEditorFields();
			if(fab.getId() == null || fab.getId() < 1) {
				faef.fromFileArchivatorBean(fab, ProcessItemAction.CREATE, currentUser, 1, repository);
			} else {
				faef.fromFileArchivatorBean(fab, ProcessItemAction.EDIT, currentUser, 1, repository);
			}
		}

		this.fab = fab;
		this.saveLater = Tools.isTrue(fab.getEditorFields().getSaveLater()) || fab.getUploaded() == 0;
		this.renameFile = Tools.isTrue(fab.getEditorFields().getRenameFile());
		this.isPatternFile = Tools.isNotEmpty( fab.getReferenceToMain() );
		this.saveAfterId = Tools.getIntValue(fab.getEditorFields().getSaveAfterId(), -1);
		this.uploadType = getUploadType(fab.getEditorFields().getUploadType());
		this.referenceId = fab.getId() == null ? -1 : fab.getId().intValue();
		if(this.referenceId < 1) this.referenceId = -1;

		if(Tools.isNotEmpty(fab.getEditorFields().getFile())) {
			String filePath = AdminUploadServlet.getTempFilePath( fab.getEditorFields().getFile() );
			this.fileToUpload = new IwcmFile(filePath);
			this.fileToUploadName = AdminUploadServlet.getOriginalFileName(fab.getEditorFields().getFile());
		}

		this.errorParams = new String[0];
	}

	/**
	 * Instance for old Java/JSP (non Spring) classes.
	 * For Spring use standard FileArchiveService constructor.
	 * @param user
	 * @param fileArchivId
	 * @return
	 */
	public static FileArchiveService getInstance(Identity user, Long fileArchivId) {
		Prop prop = Prop.getInstance();
		FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
		FileArchivatorBean fab = repository.findFirstByIdAndDomainId(fileArchivId, CloudToolsForCore.getDomainId()).orElse(null);
		if(fab == null) return null;
		return new FileArchiveService(user, prop, fab, repository);
	}

	public final List<String> getErrorList() {
		return errorList;
	}

	public final String[] getErrorParams() {
		return errorParams;
	}

	public final List<FileArchivatorBean> getSameFiles() {
		return sameFiles;
	}

	public void checkFileProperties(Errors errors) {
		boolean needCheckFile = false;
		String tempFileKey = fab.getEditorFields().getFile();

		//First check perms
		if(currentUser.isEnabledItem("cmp_fileArchiv_edit_del_rollback") == false) {
			errorList.add(PERMISSION_DENIED);
			return;
		}

		//Bonus permission check for
		if(uploadType == UploadType.HISTORY_VERSION && currentUser.isEnabledItem("cmp_fileArchiv_advanced_settings") == false) {
			errorList.add(PERMISSION_DENIED);
			return;
		}

        if(referenceId < 1) {
			//When CREATING new entity, we cant do special actions like NEW_VERSION, REPLACEMENT or HISTORY_VERSION (they work with allready existing files)
			if(uploadType != UploadType.NO_ACTION) {
				errorList.add(ACTION_NOT_SUPPORTED);
				return;
			}

            //CREATE - file name must be there
			needCheckFile = true;
            if( Tools.isEmpty(tempFileKey) ) {
                errors.rejectValue(FILE_FIELD, "", prop.getText(EMPTY_FILE_ERR));
				return;
			}
        } else {
            //EDIT

			if(uploadType != UploadType.NO_ACTION && Constants.getBoolean("fileArchivCanEdit") == false) {
				//By constant, its PROHIBITED to change FILE (like real file not a record)
				errorList.add("components.file_archiv.edit_not_permitted");
				return;
			}

			//Check if pattern archiv is allowed (REPLACEMENT is allways allowed)
			if(isPatternFile == true && uploadType != UploadType.REPLACEMENT && Constants.getBoolean("fileArchivAllowPatternVersion") == false) {
				errorList.add("components.file_archiv.upload.pattern_version");
				return;
			}

			// MAIN file is awaiting upload -> CAN ONLY replace file (maybe by mistake he uploaded wrong one)
			if(fab.getUploaded() == 0 && (uploadType == UploadType.NEW_VERSION || uploadType == UploadType.HISTORY_VERSION)) {
				errorList.add(ACTION_NOT_SUPPORTED);
				return;
			}

			//Check that file that want's be pattern  - have pattern on himself (prohibits)
			if(isPatternFile && fab.getFileArchiveId() > 0 && repository.countReferencesToMain(fab.getVirtualPath(), domainId) > 0) {
				errorList.add("components.file_archiv.cant_be_pattern");
				return;
			}

			if(uploadType != UploadType.NO_ACTION) {
				//Action that requires upload of file - new file name must be there
				needCheckFile = true;
				if(Tools.isEmpty(tempFileKey)) {
					errors.rejectValue(FILE_FIELD, "", prop.getText(EMPTY_FILE_ERR));
					return;
				}
			}

			if(uploadType == UploadType.HISTORY_VERSION && saveAfterId < 1) {
				//Want add new history version but is missing ID of file to move behind
				errors.rejectValue("errorField.editorFields.saveAfterSelect", "", prop.getText("components.file_archiv.save_after.empty_err"));
				return;
			}

			if(renameFile && Tools.isEmpty(fab.getEditorFields().getNewFileName())) {
				//Want rename file but is missing NEW file name
				errors.rejectValue("errorField.editorFields.newFileName", "", prop.getText(EMPTY_FILE_ERR));
				return;
			}
        }

		if(Tools.isTrue(fab.getEditorFields().getSaveLater())) {
			if(!isUploadDateCorrect(true)) {
				errors.rejectValue("errorField.editorFields.dateUploadLater", "", prop.getText("components.file_archiv.upload.upload_date_wrong"));
				return;
			}

			if(!isCorrectEmails(true)) {
				errors.rejectValue("errorField.editorFields.emails", "", prop.getText("components.file_archiv.upload.emails_wrong"));
				return;
			}
		}

		if(fab.getUploaded() == 0) {
			if(!isUploadDateCorrect(false)) {
				errors.rejectValue("errorField.dateUploadLater", "", prop.getText("components.file_archiv.upload.upload_date_wrong"));
				return;
			}

			if(!isCorrectEmails(false)) {
				errors.rejectValue("errorField.emails", "", prop.getText("components.file_archiv.upload.emails_wrong"));
				return;
			}
		}

        //Check, that given file is valid
        if(needCheckFile) {
            checkFilePropertiesDetails(errors);
        }
	}

	private void checkFilePropertiesDetails(Errors errors) {
		//kontrola prav na zapis do suboru
		if(currentUser.isFolderWritable( normalizePath(getFileDirPath()) ) == false) {
			errorList.add( "components.elfinder.commands.upload.error");
			Logger.debug(this, "User nema pravo na zapis do priecinku:  " + normalizePath(getFileDirPath()) );
			return;
		}

		if(getFileDirPath() == null || FileBrowserTools.hasForbiddenSymbol(getFileDirPath())) {
			errorList.add(FILE_UPLOAD_ERR);
			return;
		}

		if(fileToUpload.exists() == false || fileToUploadName.length() < 5) {
			errors.rejectValue(FILE_FIELD, "", prop.getText("components.file_archiv.upload.file_not_exists_or_short_name"));
			return;
		}

		if(fileToUpload.getLength() > FileArchivatorKit.getMaxFileSize()) {
			errors.rejectValue(FILE_FIELD, "", prop.getText("components.file_archiv.upload.file_is_too_big", fileToUpload.getLength() + "", FileArchivatorKit.getMaxFileSize() + ""));
			return;
		}

		FileArchivatorKit fk = new FileArchivatorKit(prop);
		if(!fk.hasAllowedExtensions(fileToUploadName, referenceId))
		{
			errors.rejectValue(FILE_FIELD, "", fk.getErrorsList().get(0));
			return;
		}

		if(isConcurrentModification(referenceId, referenceId))
		{
			errors.rejectValue(FILE_FIELD, "", prop.getText("components.file_archiv.upload.concurrent_modification"));
			return;
		}

		//ak uz existuje referencia v databaze k suboru ktory este len ideme nahrat, tak je to problem. Niekto zmazal subor rucne.
		//subor nemozeme nahrat pretoze by mohol byt zmazany inym zaznamom v databaze - omylom
		if(FileArchivatorKit.existsPathInDB(getFileDirPath() + fileToUploadName) && !FileTools.isFile(getFileDirPath() + fileToUploadName) ) {
			errors.rejectValue(FILE_FIELD, "", prop.getText("components.file_archiv.upload.db_enrty_exists"));
			return;
		}

		//File is OK - check if it's duplicity
		if(Tools.isFalse(fab.getEditorFields().getUploadRedundantFile())) {
			findSameFiles(fab, false, true);
			if(getSameFiles() != null && getSameFiles().isEmpty() == false) {
				errorList.add("components.file_archiv.redundant_file_error");
				errorParams = new String[] { fileToUploadName, prop.getText("components.file_archiv.upload_redundant_file") };
			}
		}
	}

	public String saveFile() {
		if(fab.getId() == null || fab.getId() < 1) {
			//CREATE action - can only upload file

			//Upload new file - MAIN
			return uploadFile(UploadType.NO_ACTION);
		} else {
			//EDIT action

			if(uploadType == UploadType.REPLACEMENT) {
				//Replace current file
				return replaceFile();
			} else if(uploadType == UploadType.NEW_VERSION) {
				//Upload new version of file
				return uploadNewFileVersion();
			} else if(uploadType == UploadType.HISTORY_VERSION) {
				//Upload new version of file
				return moveBehind(this.saveAfterId);
			} else {
				if(checkPerms() == false) return PERMISSION_DENIED;

				String oldReferenceToMain = repository.getReferenceToMain(fab.getId(), domainId);

				//NO file action needed - just do NORMAL entity save
				if(fab.saveWithDebugLog(getClass(), "saveFile - NO ACTION") == false)
					return DB_SAVE_FAILED;
				else {
					//I pattern file -> update all history versions reference to main file
					if(isPatternFile) updateReferenceToMainFile(referenceId, fab.getReferenceToMain());
					else {
						//Check if older version was pattern and now is not -> in this case update all history versions
						if(Tools.isNotEmpty(oldReferenceToMain)) updateReferenceToMainFile(referenceId, fab.getReferenceToMain());
					}

					return null;
				}
			}
		}
	}

	private synchronized String uploadFile(UploadType uploadType) {
		String uniqueFileName = FileArchivatorKit.getUniqueFileName(fileToUploadName, getFileDirPath(), null);

		//Create file, write content into file AND check if file exists
		String responseTxt = createWriteCheckFile( getFileDirPath() + uniqueFileName );
		if(responseTxt != null) return responseTxt;

		//
		prepareFileArchivatorBean(getFileDirPath(), uniqueFileName, (long)-1, true);
		fab.setGlobalId(FileArchivatorKit.generateNextGlobalId());
		fab.setDateUploadLater( fab.getEditorFields().getDateUploadLater() );
		fab.setEmails( fab.getEditorFields().getEmails() );

		if(fab.saveWithDebugLog(getClass(), "uploadFile") == false) {
			return DB_SAVE_FAILED;
		}

		return null;
	}

	private synchronized String uploadNewFileVersion() {
		if(checkPerms() == false) return PERMISSION_DENIED;

		FileArchivatorBean fabOld = repository.findFirstByIdAndDomainId(Long.valueOf(referenceId), domainId).orElse(null);
		if(fabOld == null) return RECORD_NOT_FOUND;

		String oldVirtualPath = fabOld.getVirtualPath();
		String dateStamp = FileArchivatorKit.getDateStampAsString(fabOld.getDateInsert());
		//if we are updating existing file, we want to keep the original file name
		fileToUploadName = fabOld.getFileName();

		String uniqueFileName = FileArchivatorKit.getUniqueFileName(fileToUploadName, getFileDirPath(), dateStamp);
		String fileUrl = getFileDirPath() + uniqueFileName;
		IwcmFile realFile = new IwcmFile( Tools.getRealPath(fileUrl) );

		try {
			InputStream newFileIS = new IwcmInputStream(fileToUpload);
			if(saveLater) {
				//Just create new file that gonna be uploaded inn future
				IwcmFsDB.writeFiletoDest(newFileIS, new File(realFile.getAbsolutePath()), Tools.safeLongToInt(fileToUpload.getLength()));

			} else {
				//Create new file AND fill it with content of old file
				IwcmFile oldFile = new IwcmFile( fabOld.getRealPath() );
				InputStream oldFileIS = new IwcmInputStream(oldFile);
				IwcmFsDB.writeFiletoDest(oldFileIS, new File(realFile.getAbsolutePath()), Tools.safeLongToInt(oldFile.getLength()));

				//Re-write original file with new content
				FileTools.copyFile(newFileIS, oldFile);
			}
		} catch(IOException ioex) {
			Logger.debug(FileArchiveService.class, "Nastala chyba '" + ioex.getMessage() + "' pri ukladani suboru " + realFile.getPath());
			sk.iway.iwcm.Logger.error(ioex);
			return FILE_UPLOAD_ERR;
		}

		if(saveLater == true) {
			//fab is working with new file
			prepareFileArchivatorBean(getFileDirPath(), uniqueFileName, referenceId, true);
			fab.setDateUploadLater( fab.getEditorFields().getDateUploadLater() );
			fab.setEmails( fab.getEditorFields().getEmails() );
			fab.setOrderId(-1);

			//Its all, just save IT
			fab.saveWithDebugLog(getClass(), UPLOAD_NEW_FILE_VERSION);
			return null;
		} else {
			//fab is working with OLD file where we copied new content
			prepareFileArchivatorBean(getFileDirPath(), fabOld.getFileName(), null, true);

			//pri nahravani neskor existenciu kontrolujeme skor kvoli multidomain
			if(!FileTools.isFile(fab.getVirtualPath())) return FILE_UPLOAD_ERR;
		}

		//First save new entity that represents history version
		Long originalFabId = fabOld.getId();
		fabOld.setId(null);
		fabOld.setFileName(uniqueFileName);
		fabOld.setReferenceId(referenceId);
		fabOld.setDateInsert(new Date());
		// File is now Historic version, turn off indexing of file
		fabOld.setIndexFile(false);
		if(fabOld.saveWithDebugLog(getClass(), UPLOAD_NEW_FILE_VERSION) == false) {
			return DB_SAVE_FAILED;
		}

		//Prepare FAB
		fab.setId(originalFabId);
		fab.setDateUploadLater(null);
		if(fab.saveWithDebugLog(getClass(), UPLOAD_NEW_FILE_VERSION) == false) {
			return DB_SAVE_FAILED;
		}

		if(isPatternFile) updateReferenceToMainFile(referenceId, fab.getReferenceToMain());
		else updateReferenceToMainFile(oldVirtualPath);

		//All good, do increment order id
		FileArchivatorKit.incrementOrderId(fab.getId());

		Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "File upload (file archiv), path=" + fileUrl, -1, -1);
		return null;
	}

	private String replaceFile()
	{
		if(checkPerms() == false) return PERMISSION_DENIED;

		FileArchivatorBean fabOld = repository.findFirstByIdAndDomainId(Long.valueOf(referenceId), domainId).orElse(null);
		if(fabOld == null) return RECORD_NOT_FOUND;

		//vymazanie stareho suboru
		IwcmFile iFile = new IwcmFile( fabOld.getRealPath() );
		if(!iFile.exists() || !iFile.delete()) {
			Logger.debug(FileArchiveService.class, "Subor : " + fabOld.getVirtualPath() + " neexistuje, alebo sa nepodarilo zmazat.");
			return  FILE_UPLOAD_ERR;
		}

		String fileUrl = getFileDirPath() + fabOld.getFileName();

		//Create file, write content into file AND check if file exists
		String responseTxt = createWriteCheckFile(fileUrl);
		if(responseTxt != null) return responseTxt;

		prepareFileArchivatorBean(getFileDirPath(), fabOld.getFileName(), null, false);

		if(fab.saveWithDebugLog(getClass(), "replaceFile") == false) {
			return DB_SAVE_FAILED;
		}

		Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "File upload (file archiv), path=" + fileUrl, -1, -1);
		return null;
	}

	/**
	 * @return 1 if change was successful, 0 if not, -1 if change WAS NOT needed
	 */
	public int checkAndRenameFile() {
		if(renameFile) {
			if(Tools.isEmpty(fab.getEditorFields().getNewFileName())) return 0;

			String newFileName = fab.getEditorFields().getNewFileName();
			String oldVirtualPath = fab.getVirtualPath();

			//if user add new file name WITH extension, we want to remove it
			if(newFileName.indexOf(".") != -1)
				newFileName = newFileName.substring(0, newFileName.lastIndexOf("."));

			newFileName += FileArchivatorKit.getFileExtension(fab.getFileName());
			newFileName = DocTools.removeChars(newFileName, false);
			newFileName = Tools.replace(newFileName, " ", "");

			if(referenceId > -1) {
				//We added new file and want to rename it at same time ... get MAIN entity
				this.fab = repository.findFirstByIdAndDomainId(Long.valueOf(referenceId), domainId).orElse(null);
				if(this.fab == null) return 0;
			}

			if(renameFile(fab.getFilePath(), newFileName , fab)) {
				fab.setFileName(newFileName);
				updateReferenceToMainFile(oldVirtualPath);

				repository.save(this.fab);

				//Change successful
				return 1;
			}

			//Change failed
			return 0;
		}

		//Change was not required
		return -1;
	}

	private boolean renameFile(String newDirPath, String newfileName, FileArchivatorBean oldFileBean)
	{
		newDirPath = normalizePath(newDirPath);
		String oldDirPath = normalizePath(oldFileBean.getFilePath());

		//premenujeme meno poslednej verzie suboru na novo nahraty
		boolean renamed = true;
		//posledny subor si odlozime
		if(!Tools.renameFile(Tools.getRealPath(oldDirPath + oldFileBean.getFileName()), Tools.getRealPath(newDirPath + newfileName)))
			renamed = false;

		Logger.debug(FileArchiveService.class, "Rename from "+(oldDirPath + oldFileBean.getFileName())+" to "+(Tools.getRealPath(newDirPath + newfileName))+" result: "+renamed);

		return renamed;
	}

	/**
	 * vymazanie aktualneho suboru z vlakna a jeho nahradenie predchadzajucim suborom z vlakna
	 *
	 */
	public boolean rollback()
	{
		if(checkPerms() == false) return false;

		boolean result = true;

		if(fab.getReferenceId() > -1) {
			return false;
		}

        if(!currentUser.isFolderWritable( normalizePath(fab.getFilePath()) ))
        {
            Logger.debug(FileArchiveService.class, LOGGER_USER_ID + currentUser.getUserId() + " nema pravo na ROLLBACK suboru: " + fab.getVirtualPath() + " ");
            return false;
        }

		FileArchivatorBean previousFab = repository.findFirstByReferenceIdOrderByOrderIdAsc(fab.getId().intValue());
		if(previousFab != null)
		{
			IwcmFile iFileActual = new IwcmFile( fab.getRealPath() );
			IwcmFile renamed = new IwcmFile( fab.getRealPath() );
			IwcmFile iFilePrevious = new IwcmFile( previousFab.getRealPath() );
			if(iFilePrevious.exists() && iFileActual.exists() && iFileActual.delete() && iFilePrevious.renameTo(renamed))
			{
				Long oldId = previousFab.getId();
				previousFab.setFilePath(fab.getFilePath());
				previousFab.setFileName(fab.getFileName());
				previousFab.setReferenceId(null);
				previousFab.setOrderId(-1);
				previousFab.setId( fab.getId() );

				fab.setId(oldId);

				if(previousFab.saveWithDebugLog(getClass(), "rollback"))
				{
					if(fab.delete())
					{
						//Must be uploaded -1 aka allready uploaded (uploaded 0 are waiting)
						List<FileArchivatorBean> files = FileArchivatorDB.getByReferenceId(referenceId);
						if(files!=null)
						{
							for(FileArchivatorBean file : files) {
								file.setOrderId(file.getOrderId() - 1);
								if(file.saveWithDebugLog(getClass(), "rollback") == false) {
									result = false;
								}
							}
						}
                        Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "EDIT: File Archiv ROLLBACK zmeny:"+FileArchivatorKit.getPojoZmeny(previousFab, fab), fab.getId(), previousFab.getId());
						return result;
					}
					else
						Logger.error(FileArchiveService.class, "Chyba pri ukladani do DB :"+previousFab.toString());
				}
			}
			else
				Logger.error(FileArchiveService.class, "Chyba pri premenovani suboru :"+previousFab.getFilePath()+fab.getFileName()+" na "+fab.getVirtualPath());
		}
		else
			Logger.error(FileArchiveService.class, "Nepodarilo sa vykonat rollback: neexistujuci hlavny subor, alebo ziadny predchadzajuci subor vo vlakne. Id :"+fab.getId().intValue());

		return false;
	}

	public String deleteStructure()
	{
		if(checkPerms() == false) return PERMISSION_DENIED;

		boolean isSuccess = true;

		//najskorej zmazeme referencie
		List<FileArchivatorBean> fabList = repository.findAllByReferenceIdAndDomainId(fab.getId(), domainId);
		if(fabList != null)
		{
			//ak na niektory z mazanych suborov nemame pravo, nezmazeme ani tie na ktore mame pravo.
			for(FileArchivatorBean tmpFab : fabList)
			{
				if(!currentUser.isFolderWritable( normalizePath(tmpFab.getFilePath()) ))
				{
					Logger.debug(FileArchiveService.class, LOGGER_USER_ID + currentUser.getUserId() + " nema pravo na zmazanie suboru (v liste): " + tmpFab.getVirtualPath() + " ");
					return DELETE_STRUCTURE_ERR;
				}
			}

			for(FileArchivatorBean tmpFab : fabList)
			{
				isSuccess = deleteFile(tmpFab, "Sub ", true);
			}
		}

		if(!isSuccess)
			return DELETE_STRUCTURE_ERR;

		//a teraz zmazeme hlavny subor
		FileArchivatorBean fabToDelete = repository.findFirstByIdAndDomainId(fab.getId(), domainId).orElse(null);

		if(fabToDelete != null)
		{
			//zmazeme vzor
			deleteFilePatterns(fabToDelete, true);
			isSuccess = deleteFile(fabToDelete,"Hlavny ", true);
		}

		return isSuccess ? null : DELETE_STRUCTURE_ERR;
	}

	/** Mazanie za podmienky ze na subor Neexistuje referencia
	 *
	 */
	public String deleteWaitingFile() {
		if(!currentUser.isFolderWritable( normalizePath(fab.getFilePath()) )) {
			Logger.debug(FileArchiveService.class, LOGGER_USER_ID + currentUser.getUserId() + " nema pravo na zmazanie suboru: " + fab.getVirtualPath() + " ");
			return DELETE_WAITING_ERR;
		}

		IwcmFile iFile = new IwcmFile( fab.getRealPath());
		if(iFile.exists() && iFile.delete()) {
			deleteFilePatterns(fab, true);
		} else {
			Logger.debug(FileArchiveService.class, "Subor :"+fab.getVirtualPath()+" sa nepodarilo zmazat.");
			return DELETE_WAITING_ERR;
		}

		if(fab.delete()) {
			String fileDescription = "Subor "+fab.getVirtualPath()+" zmazany \n "+fab.toString();
			Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, fileDescription, -1, -1);
			return null;
		}

		return DELETE_WAITING_ERR;
	}

	/** Vrati vsetky subory cakajuce na nahranie v buducnosti.
	 * AK je zadana entita, vrati sa tie, co patria pod nu.
	 * Do CACHE sa vsak ulozia vsetky subory.
	 *
	 * @return List<FileArchivatorBean>
	 */
	@SuppressWarnings("unchecked")
	public static List<FileArchivatorBean> getWaitingFileList(Long mainId, FileArchiveRepository repo)
	{
		List<FileArchivatorBean> allWaitingFiles = null;
		String methodName = "getWaitingFileList-";
		String cacheKey = cachePrefix+methodName+"-domainId-"+CloudToolsForCore.getDomainId();
		Object o = Cache.getInstance().getObject(cacheKey);
		if(o != null) {
			allWaitingFiles = (List<FileArchivatorBean>) o;
		} else {
			//Cache is empty, get data from DB
			Logger.debug(FileArchiveService.class, "Nacitam z databazy: " + cacheKey);

			allWaitingFiles = repo.findAllByUploadedAndDomainId(0, CloudToolsForCore.getDomainId());

			//Save in cache
			Cache.getInstance().setObject(cacheKey, allWaitingFiles, getCacheTime(methodName));
		}

		if(mainId == null || allWaitingFiles == null) {
			return allWaitingFiles;
		} else {
			//Get id of main file
			List<FileArchivatorBean> toReturn = new ArrayList<>();
			for(FileArchivatorBean fab : allWaitingFiles) {
				if(mainId.equals(fab.getReferenceId())) {
					toReturn.add(fab);
				}
			}

			return toReturn;
		}
	}

	public String moveBehind(int moveBelowFileId) {
		if(checkPerms() == false) return PERMISSION_DENIED;

		if(moveBelowFileId < 1) return RECORD_NOT_FOUND;

		//Verify, that moveBelowFileId is valid history version of this file
		FileArchivatorBean fileMoveBelowBean = repository.findFirstByIdAndDomainId(Long.valueOf(moveBelowFileId), domainId).orElse(null);
		if(fileMoveBelowBean == null) return RECORD_NOT_FOUND;

		String dateStamp = FileArchivatorKit.getDateStampAsString(fab.getDateInsert());
		String uniqueFileName = FileArchivatorKit.getUniqueFileName(fileToUploadName, getFileDirPath(), dateStamp);

		//
		String responseTxt = createWriteCheckFile( getFileDirPath() + uniqueFileName );
		if(responseTxt != null) return "components.file_archiv.move_file_failed";

		Long referenceIdKKs = fileMoveBelowBean.getReferenceId().longValue() == -1 ? fileMoveBelowBean.getId() : fileMoveBelowBean.getReferenceId();

		prepareFileArchivatorBean(getFileDirPath(), uniqueFileName, referenceIdKKs, true);
		fab.setOrderId(-1);

		if(fab.saveWithDebugLog(getClass(), MOVE_BEHIND) == false) return "components.file_archiv.record_saved_fail";

		//inkrementujeme order id - only allready uploaded files aka -1
		for(FileArchivatorBean archivBean : repository.findAllByReferenceIdAndUploadedAndDomainId(referenceIdKKs, -1, domainId)) {
			if(archivBean.getOrderId() > fileMoveBelowBean.getOrderId()) {
				archivBean.setOrderId(archivBean.getOrderId() + 1);
				archivBean.saveWithDebugLog(getClass(), MOVE_BEHIND);
			} else if(archivBean.getOrderId() == -1) {
				// prave vlozeny fileLaterInsertBean
				archivBean.setOrderId( fileMoveBelowBean.getOrderId() == -1 ? 2 : fileMoveBelowBean.getOrderId() + 1 );
				archivBean.saveWithDebugLog(getClass(), MOVE_BEHIND);
			}
		}

		//premazeme cache
		FileArchivatorKit.deleteFileArchiveCache();
		return null;
	}

	/**
	 * Create file, write content into file AND check if file exists
	 *
	 * @param fileUrl
	 * @return
	 */
	private String createWriteCheckFile(String fileUrl) {
		IwcmFile realFile = new IwcmFile( Tools.getRealPath(fileUrl) );
		try {
			InputStream in = new IwcmInputStream(fileToUpload);
			IwcmFsDB.writeFiletoDest(in, new File(realFile.getAbsolutePath()), Tools.safeLongToInt(fileToUpload.getLength()));

			//pri nahravani neskor existenciu kontrolujeme skor kvoli multidomain
			if(saveLater == false && FileTools.isFile(fileUrl) == false)
				return FILE_UPLOAD_ERR;

			//Log successful upload of ifle
			Adminlog.add(Adminlog.TYPE_FILE_ARCHIVE, "FileArchiveService - File uploaded, path=" + fileUrl, -1, -1);
		} catch(IOException ioex) {
			Logger.debug(FileArchiveService.class, "Nastala chyba '" + ioex.getMessage() + "' pri ukladani suboru " + realFile.getPath());
			sk.iway.iwcm.Logger.error(ioex);
			return FILE_UPLOAD_ERR;
		}
		return null;
	}

	public static final Long getId(String filePath, String fileName, FileArchiveRepository far) {
		if(Tools.isEmpty(filePath) || Tools.isEmpty(fileName)) return -1L;
		filePath = FileArchivSupportMethodsService.normalizeToOldPath(filePath);
		return far.findIdByFilePathAndFileName(filePath, fileName, CloudToolsForCore.getDomainId()).map(Long::longValue).orElse(-1L);
	}
}