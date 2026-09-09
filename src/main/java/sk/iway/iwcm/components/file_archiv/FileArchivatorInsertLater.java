package sk.iway.iwcm.components.file_archiv;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import sk.iway.Html2Text;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * Publishes file archive uploads that were scheduled for a future date.
 * The job claims each waiting record atomically, moves its staged file into the archive,
 * updates version history, and sends the configured notification.
 */
public class FileArchivatorInsertLater
{
    private static final String AUDIT_FILE_ARCHIVATOR_INSERT_LATER = "FileArchivatorInsertLater";

    /**
     * Runs the scheduled-upload publisher on an administrative cluster node.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args)
    {
        if (ClusterDB.isPublicNode()) {
            //only run this task on admin nodes because of database permissions
            return;
        }

        boolean deleteFullCache = false;
        try
        {
            //prevent to run at the same time on different cluster nodes, because of possible file conflicts, so we add random sleep before start
            long rndSleep = ThreadLocalRandom.current().nextInt( 10000);
            Thread.sleep(rndSleep);

            List<FileArchivatorBean> filesToUpload = FileArchivatorDB.getFilesToUpload();

            for(FileArchivatorBean fab : filesToUpload)
            {
                RequestBean currentRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
                if (currentRequestBean == null) {
                    SetCharacterEncodingFilter.registerDataContext(null);
                    currentRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
                }

                int domainId = fab.getDomainId();
                if (domainId > -1) {
                    GroupDetails group = GroupsDB.getInstance().getGroup(domainId);
                    if (group != null) {
                        currentRequestBean.setDomain(group.getDomainName());
                    }
                }

                //test if file exist's maybe it's on another cluster node
                IwcmFile file = new IwcmFile(Tools.getRealPath(fab.getFilePath()+fab.getFileName()));
                if(!file.exists())
                {
                    //skip this file
                    continue;
                }

                //subor, ktory sme sa v minulosti pokusili nahrat neuspesne
                //state -2 is permanent (fail-closed); an admin must re-trigger the row manually by resetting uploaded to 0
                if(fab.getUploaded()==-2)
                    continue;

                int stav = 0;

                //ulozime subor na nove miesto
                String uniqueFileName = renameFile(fab);

                if(uniqueFileName==null)
                {
                    //A detached row that lost the atomic claim was already handled by another node.
                    if(fab.getUploaded() == 0) continue;
                    stav = 1;
                }
                else
                {
                    //vymazeme prazdne priecinky
                    if( !removeEmptyDirs(fab.getFilePath(), "archiv_insert_later") )
                        stav = 4;
                }

                sendMail(fab, stav);
                deleteFullCache = true;
            }
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " error:" +e.getMessage()+"\n"+ sw, -1, -1);
            sk.iway.iwcm.Logger.error(e);
        }
        finally
        {
            if (deleteFullCache) {
                //clean all cache, so public node will fetch new list of files with updated names etc
                Cache.getInstance().removeObjectStartsWithName(FileArchivatorDB.getCachePrefix(), true);
            } else {
                //clear only local waiting list cache
                Cache.getInstance().removeObjectStartsWithName(FileArchivatorDB.getCachePrefix()+"getWaitingFileList");
            }
        }
        SetCharacterEncodingFilter.unRegisterDataContext();
    }

	/**
	 * Claims and publishes a scheduled archive upload using the configured repository.
	 *
	 * @param scheduledBean waiting archive record whose staged file should be published
	 * @return the published file name, or {@code null} when the record could not be claimed or published
	 */
	public static String renameFile(FileArchivatorBean scheduledBean) {
        FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
        return renameFile(scheduledBean, repository);
    }

    /**
     * Atomically claims a waiting upload and serializes its file-system publication within this JVM.
     *
     * @param scheduledBean waiting archive record
     * @param repository repository used to claim the record
     * @return the published file name, or {@code null} when the claim or publication fails
     */
    static String renameFile(FileArchivatorBean scheduledBean, FileArchiveRepository repository) {
        if(scheduledBean.getId() == null || repository.claimWaitingFile(scheduledBean.getId(), scheduledBean.getDomainId()) != 1) {
            return null;
        }
        //The database claim is also the durable failure state if this process stops mid-publication.
        scheduledBean.setUploaded(-2);
        synchronized (FileArchivatorKit.FILE_OPERATION_LOCK) {
            return renameFileLocked(scheduledBean);
        }
    }

    /**
     * Publishes an already claimed scheduled record and updates its archive version history.
     *
     * @param scheduledBean claimed archive record in the durable processing state
     * @return the published file name, or {@code null} when publication fails
     */
    private static String renameFileLocked(FileArchivatorBean scheduledBean) {
        IwcmFile stagedFile = new IwcmFile(Tools.getRealPath(scheduledBean.getFilePath() + scheduledBean.getFileName()));
        if(stagedFile.exists() == false) {
            Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " staged file no longer exists: " + scheduledBean.getVirtualPath(), -1, -1);
            return null;
        }

        //get old bean
        FileArchivatorBean oldFileBean = FileArchivatorDB.getInstance().getById(scheduledBean.getReferenceId());
        String fileUrl = newPath(scheduledBean.getFilePath());
        if(Tools.isEmpty(fileUrl)) {
            Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " invalid staging path for file: " + scheduledBean.getVirtualPath(), -1, -1);
            return null;
        }

        try {
            IwcmFile targetDirectory = new IwcmFile(Tools.getRealPath(fileUrl));
            if(targetDirectory.exists() == false) {
                targetDirectory.mkdirs();
                if(targetDirectory.exists() == false) {
                    Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " create directory failed for: " + targetDirectory.getAbsolutePath(), -1, -1);
                    return null;
                }
            }

            if (oldFileBean != null) {
                String oldFilePath = FileArchivSupportMethodsService.normalizePath(oldFileBean.getFilePath());
                if(fileUrl.equals(oldFilePath) == false) {
                    Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER +
                        " destination change is not supported for a scheduled new version: " + scheduledBean.getVirtualPath(), -1, -1);
                    return null;
                }

                String uniqueFileName = FileArchivatorKit.getUniqueFileName(scheduledBean.getFileName(), fileUrl, FileArchivatorKit.getDateStampAsString(scheduledBean.getDateInsert()));
                IwcmFile realFile = new IwcmFile(Tools.getRealPath(fileUrl + uniqueFileName));
                //There is OLD copy content of OLD file into new file
                IwcmFile oldFile = new IwcmFile(Tools.getRealPath(oldFileBean.getFilePath() + oldFileBean.getFileName()));
                if(oldFile.renameTo(realFile) == false) {
                    //ERR
                    Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " renameTo failed for file: oldFile: " +oldFile.getAbsolutePath() + ", realFile: "+realFile.getAbsolutePath(), -1, -1);
                    return null;
                }

                //
                FileTools.moveFile(scheduledBean.getFilePath() + scheduledBean.getFileName(), fileUrl + scheduledBean.getFileName());

                //NOW edit and save DB records
                Long originalId = oldFileBean.getId();
                scheduledBean.setFileName(oldFileBean.getFileName());

                oldFileBean.setId(scheduledBean.getId());
                oldFileBean.setFileName(uniqueFileName);
                oldFileBean.setReferenceId(originalId);

                scheduledBean.setId(originalId);
                scheduledBean.setFilePath(fileUrl);
                scheduledBean.setUploaded(-1);
                scheduledBean.setReferenceId(null);

                if(oldFileBean.save() == true && scheduledBean.save() == true) {
                    //All good, do increment order id
		            FileArchivatorKit.incrementOrderId(originalId);

                    //Good
                    return scheduledBean.getFileName();
                }
            } else {
                String uniqueFileName = FileArchivatorKit.getUniqueFileName(scheduledBean.getFileName(), fileUrl, FileArchivatorKit.getDateStampAsString(scheduledBean.getDateInsert()));
                //ITS main file, there is no old file
                if(FileTools.moveFile(scheduledBean.getFilePath() + scheduledBean.getFileName(), fileUrl + uniqueFileName) == false) {
                    //ERR
                    Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " moveFile failed for file: origUrl: " +scheduledBean.getVirtualPath() + ", destUrl: "+fileUrl + uniqueFileName, -1, -1);
                    return null;
                }

                scheduledBean.setFileName(uniqueFileName);
                scheduledBean.setFilePath(fileUrl);
                scheduledBean.setUploaded(-1);
                scheduledBean.setReferenceId(null);

                if(scheduledBean.save() == false) {
                    Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " save failed for file: " +scheduledBean.getVirtualPath(), -1, -1);
                    Logger.error(FileArchivatorInsertLater.class, "save failed");
                    return null;
                }

                return uniqueFileName;
            }
        } catch (Exception e) {
            Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " renameFile error: \n" +e.getMessage(), -1, -1);
            Logger.error(e);
            return null;
        }

        Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " renameFile failed for file: " +scheduledBean.getVirtualPath(), -1, -1);
        return null;
	}

    /**
     * Resolves the domain associated with an archive record.
     *
     * @param fab archive record containing the domain identifier
     * @return configured domain name, falling back to the current domain
     */
    private static String getDomainByFab(FileArchivatorBean fab)
    {
        String domainName = CloudToolsForCore.getDomainName();
        if(Constants.getBoolean("multiDomainEnabled"))
        {
            GroupDetails root = GroupsDB.getInstance().getGroup(fab.getDomainId());
            if (root != null) {
                return root.getDomainName();
            }
        }
        return domainName;
    }

    /**
     * Converts a validated insert-later staging path to its final archive destination.
     *
     * @param oldPath virtual staging path
     * @return normalized archive path, or {@code null} when the staging path is invalid
     */
    private static String newPath(String oldPath)
    {
        String normalizedOldPath = FileArchivSupportMethodsService.normalizePath(oldPath);
        String normalizedInsertLaterPath = FileArchivSupportMethodsService.normalizePath(FileArchivatorKit.getFullInsertLaterPath());
        if(Tools.isAnyEmpty(normalizedOldPath, normalizedInsertLaterPath) || normalizedOldPath.startsWith(normalizedInsertLaterPath) == false) {
            return null;
        }

        String destinationPath = normalizedOldPath.substring(normalizedInsertLaterPath.length());
        if(Tools.isEmpty(destinationPath)) return null;
        destinationPath = FileArchivSupportMethodsService.normalizePath(destinationPath);

        String archivePath = FileArchivSupportMethodsService.normalizePath(FileArchivatorKit.getArchivPath());
        if(Tools.isEmpty(archivePath) || destinationPath.startsWith(archivePath) == false || FileBrowserTools.hasForbiddenSymbol(destinationPath)) {
            return null;
        }
        return destinationPath;
    }

    /**
     * Sends the scheduled-upload result notification and records failed publications in the audit log.
     *
     * @param fileArchivatorBean processed archive record
     * @param stav result code, where zero represents success
     */
    private static void sendMail(FileArchivatorBean fileArchivatorBean, int stav)
    {
        Prop prop = Prop.getInstance();

        StringBuilder text = new StringBuilder();
        text.append("<html><head>");
        text.append("<style>");
        text.append("body{");
        text.append("font-family: Arial;");
        text.append("font-size: 11pt;");
        text.append('}');
        text.append("</style></head><body>");
        text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nasledujici_soubor_bol_uspesne_nacitany")).append(":<br/><br/>");
        if(stav!=0)
            text = new StringBuilder(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.pri_nahravani_suboru_nastala_nasledujuca_chyba")).append(": ");
        if(stav==1)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.soubor_sa_nepodarilo_ulozit_na_disk")).append("<br/><br/>");
        if(stav==2)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.zaznam_o_soubore_sa_nepodarilo_ulozit_do_databazy")).append("<br/><br/>");
        if(stav==3)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nepodarilo_sa_vymazat_docasny_soubor_alebo_zaznam_o_nom_z_databazy")).append("<br/><br/>");
        if(stav==4)
            text.append(prop.getText("components.file_archiv.FileArchivatorInsertLater.java.nepodarilo_se_vymazat_prazdne_adresare")).append("<br/><br/>");

        FileArchivatorBean fab = fileArchivatorBean;
        String emails = fab.getEmails();
        //Ak tento subor nie je novy, a je aktualizaciou starsieho suboru, potrebujeme vypisat v jeho vlastnosti.
        if(fab.getReferenceId() != -1)
        {
            FileArchivatorBean fabByReference = FileArchivatorDB.getInstance().getById(fab.getReferenceId());
            // referencia na neho sameho s novym ID ale v archive
            if(fabByReference != null)
            {
                fab = fabByReference;
                //referencia na hlavny subor
                fabByReference = FileArchivatorDB.getInstance().getById(fab.getReferenceId());
                if(fabByReference != null)
                    fab = fabByReference;
            }
        }

        String subject = prop.getText("components.file_archiv.FileArchivatorInsertLater.java.soubor_bol_uspesne_nahrany") +" "+ fab.getVirtualFileName();
        if(stav!=0)
            subject = prop.getText("components.file_archiv.FileArchivatorInsertLater.java.pozor_nastala_chyba_pri_ukladani_souboru_") +" "+ fab.getVirtualFileName();

        String dir = "";
        String finalPath = newPath(fab.getFilePath());
        if(Tools.isEmpty(finalPath)) finalPath = FileArchivSupportMethodsService.normalizePath(fab.getFilePath());
        if(Tools.isNotEmpty(finalPath)) dir = finalPath;

        if(Tools.isNotEmpty(fab.getVirtualFileName()))
            text.append(prop.getText("components.file_archiv.virtualFileName")).append(": ")
                     .append(fab.getVirtualFileName()).append("<br/>");
        if(Tools.isNotEmpty(dir))
                     text.append(prop.getText("components.file_archiv.directory")).append(": ").append(dir).append("<br/>");
        if(Tools.isNotEmpty(fab.getFileName()))
            text.append(prop.getText("components.gallery.fileName")).append(": ")
                     .append(fab.getFileName()).append("<br/>");
        if(fab.getValidFrom()!=null)
            text.append(prop.getText("inquiry.valid_since")).append(": ")
                     .append(Tools.formatDateTimeSeconds(fab.getValidFrom())).append("<br/>");
        if(fab.getValidTo()!=null)
            text.append(prop.getText("inquiry.valid_till")).append(": ")
                     .append(Tools.formatDateTimeSeconds(fab.getValidTo())).append("<br/>");
        if(Tools.isNotEmpty(fab.getProduct()))
            text.append(prop.getText("components.file_archiv.product")).append(": ")
                     .append(fab.getProduct()).append("<br/>");
        if(Tools.isNotEmpty(fab.getCategory()))
            text.append(prop.getText("components.bazar.category")).append(": ")
                     .append(fab.getCategory()).append("<br/>");
        if(Tools.isNotEmpty(fab.getProductCode()))
            text.append(prop.getText("components.file_archiv.kod_produktu")).append(": ")
                     .append(fab.getProductCode()).append("<br/>");
        if( Tools.isTrue(fab.getShowFile()) )
            text.append(prop.getText("editor.show")).append(":").append(prop.getText("qa.publishOnWeb.yes")).append("<br/>");
		else
            text.append(prop.getText("editor.show")).append(":").append(prop.getText("qa.publishOnWeb.no")).append("<br/>");


        text.append(prop.getText("components.banner.priority")).append(": ")
                 .append(fab.getPriority()).append("<br/>");
        if(Tools.isNotEmpty(fab.getReferenceToMain()))
            text.append(prop.getText("components.file_archiv.pattern")).append(": ").append(fab.getReferenceToMain()).append("<br/>");
        text.append(prop.getText("components.file_archiv.reference")).append(": ").append(fab.getReferenceId()).append("<br/>");

        String baseHref = "http://"+getDomainByFab(fab);

        text.append("<br/>").append(prop.getText("components.file_archiv.link_on_file")).append(": <a href=\"").append(baseHref).append("/")
                 .append(dir).append(fab.getFileName()).append("\">").append(baseHref).append("/").append(dir).append(fab.getFileName())
                 .append("</a>");

        text.append("</body></html>");

        String fromName = prop.getText("components.file_archiv.title");
        String fromEmail = "no-reply@"+getDomainByFab(fab).replace("www.", "");

        if(Tools.isNotEmpty(Constants.getString("fileArchivFromMail")) && Tools.isEmail(Constants.getString("fileArchivFromMail")))
            fromEmail = Constants.getString("fileArchivFromMail");

        if(Tools.isEmpty(emails))
            emails = Constants.getString("fileArchivSupportEmails");

        if(stav != 0) {
            Adminlog.add(Adminlog.TYPE_CRON, AUDIT_FILE_ARCHIVATOR_INSERT_LATER + " saved error, stav=" +stav+" : \n"+ Html2Text.html2text(text.toString()), -1, -1);
        }

        String[] emailsArray = Tools.getTokens(emails, ",", true);
        for (String recipient : emailsArray)
            SendMail.send(fromName, fromEmail, recipient,  subject, text.toString());
    }

    /**
     * Removes the empty directory chain from a starting directory up to the configured boundary.
     *
     * @param startDir first directory considered for removal
     * @param stopDir boundary directory that must not be traversed past
     * @return {@code true} when no deletion is needed or the empty directory tree is removed
     */
    public static boolean removeEmptyDirs(String startDir, String stopDir)
    {
        boolean result = true;

        if( Tools.isEmpty(startDir) || Tools.isEmpty(stopDir) )
            return result;

        IwcmFile startDirFile = new IwcmFile(Tools.getRealPath(startDir));
        IwcmFile stopDirFile = new IwcmFile(Tools.getRealPath(stopDir));

        if(!startDirFile.isDirectory())
            return result;

        int numberOfFiles = startDirFile.listFiles().length;

        if(numberOfFiles!=0)
            return result;

        IwcmFile helpFile = startDirFile;
        IwcmFile deleteFile = startDirFile;

        //kym je adresar prazdny, alebo iba s jednym suborom - inym prazdnym Dirom
        //a sucasne jeho nazov nie je stopDir
        while( numberOfFiles<=1 && !helpFile.getName().equals(stopDirFile.getName()) )
        {
            deleteFile = helpFile;
            helpFile = helpFile.getParentFile();
            numberOfFiles = helpFile.listFiles().length;
        }

        //rekurzivne vymaze vsetky subory nadol, vratane zadaneho adresara
        result = FileTools.deleteDirTree(deleteFile);
        Logger.debug(FileArchivatorInsertLater.class, "Vymazanie prazdnych adresarov, zaciatok(vratane): "+deleteFile.getAbsolutePath());

        return result;
    }
}
