package sk.iway.iwcm.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.Password;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivSupportMethodsService;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorDB;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.findexer.Excel;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.PDF;
import sk.iway.iwcm.findexer.PoiExtractor;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.findexer.Rtf;
import sk.iway.iwcm.findexer.Word;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class FileIndexerTools {

    private FileIndexerTools() {
        //Utility class
    }

    /**
     * Vymazanie suboru z indexu
     * @param url
     * @return
     */
    public static boolean deleteIndexedFile(String url)
    {
        try
        {
            //vymaz Stranku s tymto suborom
            StringTokenizer st = new StringTokenizer(getUrlForGroupsTokenize(url), "/");
            int parentDirId = 0;
            String dirName;
            GroupsDB groupsDB = GroupsDB.getInstance();
            GroupDetails group;
            while (st.hasMoreTokens())
            {
                dirName = st.nextToken();
                if (st.hasMoreTokens())
                {
                    //je to skutocne adresar
                    group = findGroup(groupsDB.getGroupsAll(), parentDirId, dirName);
                    if (group == null)
                    {
                        //adresar neexistuje, takze to nie je indexovane
                        return(true);
                    }
                    parentDirId = group.getGroupId();
                }
                else
                {
                    int docId = getFileDocId(dirName, parentDirId);
                    if (docId < 1) docId = DocDB.getInstance().getDocIdFromURLImpl(url+".html", null);
                    if (docId > 0)
                    {
                        DocDB.deleteDoc(docId, null);
                        return(true);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

        return(false);
    }

    /**
     * najde adresar
     * @param groups - array list adresarov
     * @param parentGroupId - id rodicovskeho adresara
     * @param name - meno adresara
     * @return
     */
    public static GroupDetails findGroup(List<GroupDetails> groups, int parentGroupId, String name)
    {
        String domainName = null;
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null) domainName = rb.getDomain();
        GroupDetails bestMatch = null;
        GroupDetails anyGroup = null;
        for (GroupDetails group : groups)
        {
            if (group.getParentGroupId() == parentGroupId && group.getGroupName().equals(name))
            {
                anyGroup = group;

                //ak este nemame pouzi verziu bez domenoveho mena
                if (bestMatch == null && Tools.isEmpty(group.getDomainName())) bestMatch = group;
                //ak mame aj domenove meno, pouzi verziu s domenovym menom
                if (domainName != null && domainName.equals(group.getDomainName())) {
                    bestMatch = group;
                    break;
                }
            }
        }

        if (bestMatch == null) bestMatch = anyGroup;

        return (bestMatch);
    }

    /**
     * Najde docId pre zadany subor v danom adresari
     * @param fileName - nazov suboru (title stranky)
     * @param dirId - id adresara, v ktorom sa to nachadza
     * @return
     */
    public static int getFileDocId(String fileName, int dirId)
    {
        int docId = -1;
        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("SELECT doc_id FROM documents WHERE (title=? OR virtual_path LIKE ?) AND group_id=?");
            ps.setString(1, fileName);
            ps.setString(2, "%/" + fileName + ".html"); //Special case for file archive, where title do not contain file name but custom name
            ps.setInt(3, dirId);
            rs = ps.executeQuery();
            if (rs.next())
            {
                docId = rs.getInt("doc_id");
            }
            rs.close();
            ps.close();
            db_conn.close();
            rs = null;
            ps = null;
            db_conn = null;
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            }
            catch (Exception ex2)
            {
            }
        }

        return(docId);
    }

    /**
     * Ziska URL pre adresarovu strukturu, ktoru chceme vytvorit
     * @param url
     * @return
     */
    public static String getUrlForGroupsTokenize(String url)
    {
        //vytvor Stranku s tymto suborom
        String urlForTokenize = url;
        if (Constants.getBoolean("enableStaticFilesExternalDir"))
        {
            int domainId = CloudToolsForCore.getDomainId();
            GroupDetails domainRootGroup = GroupsDB.getInstance().getGroup(domainId);
            if (domainRootGroup!=null)
            {
                ///files adresar vytvarame v domenovom foldri
                urlForTokenize = "/" + domainRootGroup.getGroupName() + url;
            }
        }
        return urlForTokenize;
    }

    /**
     * Fulltext Index File
     * @param url - URL of the file
     * @param user - current user
     */
    public static void indexFile(String url, UserDetails user)
    {
        indexFile(url, user, null, null);
    }

    /**
     * Fulltext Index File
     * @param url - URL of the file
     * @param indexedFiles - optional - list of indexed files, after indexing it will be added with indexed file
     * @param request - HttpServletRequest - can provide additional meta data for indexing from request.getParameter(field_a... field_l, perexGroup, passwordProtected)
     * @return
     */
    public static boolean indexFile(String url, List<ResultBean> indexedFiles, HttpServletRequest request)
	{
		return FileIndexerTools.indexFile(url, UsersDB.getCurrentUser(request), indexedFiles, request);
	}

    /**
     * Fulltext Index File
     * @param fileArchivatorBean
     * @param url
     * @param user
     * @deprecated use {@link #indexFile(String, UserDetails)} instead
     */
    @Deprecated
    public static void indexFile(FileArchivatorBean fileArchivatorBean,String url, UserDetails user)
    {
        indexFile(url, user, null, null);
    }

    /**
     * Fulltext Index File
     * @param url - URL of the file
     * @param user - current user
     * @param indexedFiles - optional - list of indexed files, after indexing it will be added with indexed file
     * @param request - optional - HttpServletRequest - can provide additional meta data for indexing from request.getParameter(field_a... field_l, perexGroup, passwordProtected)
     * @return
     */
    private static boolean indexFile(String url, UserDetails user, @Nullable List<ResultBean> indexedFiles, @Nullable HttpServletRequest request)
    {
        url = Tools.replace(url, "//", "/");
        Logger.debug(FileIndexerTools.class,"indexFile(url="+url+" user="+user+")");

        if (shouldIndexFile(url) == false) {
            Logger.debug(FileIndexerTools.class, "File is in non indexable dir: " + url);
            return false;
        }

        boolean saveOk = false;
        try
        {
            String realPath = Tools.getRealPath(url);
            Logger.debug(FileIndexerTools.class,"realPath: "+realPath);
            String data = null;

            if (url.indexOf('.')==-1)
            {
                Logger.error(FileIndexerTools.class,"url musi obsahovat bodku: "+url);
                return false;
            }

            String ext = url.substring(url.lastIndexOf('.')).toLowerCase();

            try
            {
                data = getData(url, realPath, data, ext);
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
            //Logger.debug(FileIndexerTools.class,"data: "+(data != null ? data.length() : "-"));

            if (Tools.isEmpty(data) && Constants.getBoolean("fileIndexerIndexAllFiles") && url.startsWith("/files/"))
			{
				data = "<p><a href='"+url+"'>"+url+"</p>";
			}
            if (data != null && data.trim().length() > 4)
            {
                //mame text, mozeme zaindexovat

				data = FileIndexerTools.cleanText(url, data);

				//Logger.debug(FileIndexer.class, data);

				//Logger.println(FileIndexer.class,data);
				Logger.println(FileIndexer.class,"done, size="+data.length());
				ResultBean result = new ResultBean();
				result.setFile(url);
				result.setData(data);

				if (indexedFiles!=null) {
                    indexedFiles.add(result);
                }

                StringTokenizer st = new StringTokenizer(getUrlForGroupsTokenize(url), "/");
                int parentDirId = 0;
                int parentTempId = 1;
                String dirName;
                GroupsDB groupsDB = GroupsDB.getInstance();
                GroupDetails group = null;
                int level = 0;
                int sortPriority = Constants.getInt("fileIndexerSortPriority");
                while (st.hasMoreTokens())
                {
                    level++;
                    dirName = st.nextToken();

                    if ((Constants.getBoolean("multiDomainEnabled")==false && level > 1) ||
                            (Constants.getBoolean("multiDomainEnabled")==true  && level > 2))
                    {
                        //automaticke navysovanie priority
                        sortPriority = sortPriority * 10;
                    }

                    if (st.hasMoreTokens())
                    {
                        Logger.debug(FileIndexerTools.class,"dirName: "+dirName);
                        //je to skutocne adresar
                        boolean changed = false;
                        group = findGroup(groupsDB.getGroupsAll(), parentDirId, dirName);
                        if (group == null)
                        {
                            //vytvor adresar
                            group = new GroupDetails();
                            changed = true;
                        }
                        if (changed || group.getGroupName().equals(dirName)==false ||
                                group.getSortPriority()!=sortPriority)
                        {
                            if (level==1 && Constants.getBoolean("enableStaticFilesExternalDir"))
                            {
                                //je to root, s nim nerobime nic
                            }
                            else
                            {
                                group.setGroupName(dirName);
                                group.setNavbar(dirName);
                                group.setParentGroupId(parentDirId);
                                group.setSortPriority(sortPriority);
                                group.setTempId(parentTempId);
                                group.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);

                                if (level==2 && Constants.getBoolean("multiDomainEnabled") && Constants.getBoolean("enableStaticFilesExternalDir")==false)
                                {
                                    //aby sa nam multidomain subfolder nezobrazoval v ceste
                                    group.setNavbar("&nbsp;");
                                    group.setUrlDirName(dirName);
                                }

                                groupsDB.setGroup(group);
                                GroupsDB.getInstance(true);
                            }
                        }

                        parentDirId = group.getGroupId();
                        parentTempId = group.getTempId();
                    }
                    else if (group != null)
                    {
                        String title = dirName;
                        FileArchivatorBean fab = FileArchivatorDB.getByUrl(url);
                        String domain = null;
                        //pre FA mam domeny, preco to nevyuzit pri hladani docID
                        if(fab != null && fab.getId() > 0)
                        {
                            title = fab.getVirtualFileName();
                            domain = GroupsDB.getInstance().getDomain(fab.getDomainId());
                        }

                        IwcmFile f = new IwcmFile(realPath);

                        //uz sme na konci, dirName je uz fileName
                        Logger.println(FileIndexer.class,"Vytvaram stranku: " + dirName);
                        DocDB docDB = DocDB.getInstance();

                        int docId = docDB.getDocIdFromURLImpl(url+".html", domain);
                        if (docId < 1) docId = getFileDocId(dirName, group.getGroupId());

                        group = groupsDB.getGroup(parentDirId);

                        DocDetails doc = docDB.getDoc(docId);
                        if (doc == null)
                        {
                            doc = new DocDetails();
                        }

                        //suborom nastavujem rovnaku prioritu ako je priorita adresara
                        doc.setGroupId(parentDirId);
                        doc.setAuthorId(user.getUserId());
                        doc.setSortPriority(group.getSortPriority());
                        doc.setTitle(title);
                        doc.setData(data);
                        doc.setExternalLink(url);
                        doc.setVirtualPath(url+".html");
                        doc.setNavbar(title +  " ("+Tools.formatFileSize(f.length())+")");
                        // Publish start as last modified date of the file
                        doc.setPublishStartString( Tools.formatDateTimeSeconds( f.lastModified() ) );

                        boolean searchable = fab == null || Tools.isTrue(fab.getShowFile());
                        doc.setSearchable(searchable);
                        doc.setAvailable(searchable);
                        doc.setCacheable(false);
                        if (group!=null) doc.setTempId(group.getTempId());

                        if (request != null) {
                            //ak mame v requeste nejake editor hodnoty, nastavme ich, tie z request.getAttribute sa pouzivaju v UnzipAction
                            if (request.getParameter("passwordProtected")!=null) doc.setPasswordProtected(Tools.join(request.getParameterValues("passwordProtected"), ","));
                            if (request.getAttribute("passwordProtected")!=null) doc.setPasswordProtected((String)request.getAttribute("passwordProtected"));

                            String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
                            for (String pismeno : fields)
                            {
                                String value = request.getParameter("field"+pismeno);
                                if (value==null) value = (String)request.getAttribute("field"+pismeno);

                                if (value != null) BeanUtils.setProperty(doc, "field"+pismeno, value);
                            }
                            if (request.getParameter("perexGroup")!=null) doc.setPerexGroup(request.getParameterValues("perexGroup"));
                            if(Tools.isNotEmpty(Constants.getString("beforeFileIndexerEditorSaveMathod")))
                            {
                                try{
                                    String saveMethod = Constants.getString("beforeFileIndexerEditorSaveMathod");
                                    String clazzName = saveMethod.substring( 0, saveMethod.lastIndexOf('.'));
                                    String methodName = saveMethod.substring( clazzName.length() +1 );
                                    Class<?> clazz = Class.forName(clazzName);
                                    Method method = clazz.getMethod(methodName, Class.forName("sk.iway.iwcm.editor.EditorForm"),Class.forName("javax.servlet.http.HttpServletRequest"));
                                    method.invoke(null, doc, request);
                                }
                                catch(Exception e){
                                    Logger.debug(FileIndexer.class, "Exception while trying to invoke "+Constants.getString("beforeFileIndexerEditorSaveMathod")+" , cause: "+e.getMessage());
                                }
                            }
                        }

                        saveOk = DocDB.saveDoc(doc);
                        docDB.updateInternalCaches(doc.getDocId());

                        result.setDocId(doc.getDocId());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        return saveOk;
    }

    /**
     * @param url
     * @param data
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String cleanText(String url, String data)
            throws UnsupportedEncodingException {

        if (data == null)
        {
            return data;
        }

        byte[] buff = data.getBytes(Constants.FILE_ENCODING);
        int size = buff.length;
        int i;
        boolean replaced = false;
        for (i=0; i<size; i++)
        {
            if (buff[i]>0 && buff[i]<32 && buff[i]!=10 && buff[i]!=13)
            {
                replaced = true;
                buff[i] = ' ';
            }
        }
        if (replaced)
        {
            Logger.println(FileIndexer.class,"FileIndexer: replacing bad data in " + url);
            data = new String(buff, Constants.FILE_ENCODING);
        }

        //uprav rozdelenia slov na konci riadkov (hlavne z PDF)
        data = Tools.replace(data, "\r", "");
        //dlha pomlcka za kratku - orpava kodovania na windows-1250
        data = Tools.replace(data, "–", "-");
        data = Tools.replace(data, "- \n", "");
        data = Tools.replace(data, "-\n", "");

        data = Tools.replace(data, "\n", " <br>\n");
        data = Tools.replace(data, "FORMTEXT", "");
        data = Tools.replace(data, "FORMCHECKBOX", "");
        return data;
    }

    /**
     * @param url
     * @param realPath
     * @param data
     * @param ext
     * @return
     */
    public static String getData(String url, String realPath, String data, String ext)
    {
        int maxFileSize = Constants.getInt("fileIndexerMaxFileSize");
        //BHR: typy suborov pre ktore nechceme indexovat obsah, napr, zip, rar a pod., sluzi napr ak ho chceme mat indexovany len kvoli statistike zobrazeni
        String fileIndexerNoDataFileExtension = Constants.getString("fileIndexerNoDataFileExtension");

        IwcmFile file = new IwcmFile(realPath);

        if (maxFileSize < 1 || file.length() < maxFileSize )
        {
            if(fileIndexerNoDataFileExtension != null && Arrays.stream(Tools.getTokens(fileIndexerNoDataFileExtension,",")).anyMatch(ext::equals))
            {
                data = "<p>"+ FileTools.getFileNameWithoutExtension(url)+"</p>";
            }
            else
            {
                if (".doc".equals(ext) || ".docx".equals(ext) || ".dot".equals(ext) ||
                ".xls".equals(ext) || ".xlsx".equals(ext) || ".xlt".equals(ext) ||
                ".ppt".equals(ext) || ".pptx".equals(ext) || ".pot".equals(ext) || ".pps".equals(ext) || ".ppsx".equals(ext))
                {
                    data = PoiExtractor.getText(realPath);
                }
                else if (".doc".equals(ext))
                {
                    data = Word.getText(realPath);
                }
                else if (".xls".equals(ext))
                {
                    data = Excel.getText(realPath);
                }
                else if (".pdf".equals(ext))
                {
                    data = PDF.getText(realPath);
                }
                else if (".rtf".equals(ext))
                {
                    data = Rtf.getText(realPath);
                }
                else if (".txt".equals(ext) || ".csv".equals(ext) || ".xml".equals(ext))
                {
                    data = FileTools.readFileContent(url);
                }
                else if(".zip".equals(ext))
                {
                    data = getArchiveData(url, realPath, ext);
                }
            }
        }
        else
        {
            data = "<p>"+ FileTools.getFileNameWithoutExtension(url)+"</p>";
        }

        if (Tools.isEmpty(data) && Constants.getBoolean("fileIndexerIndexAllFiles"))
        {
            if(url.startsWith("/files/"))
                data = "<p><a href='"+url+"'>"+url+"</p>";
            else if(url.startsWith("/WEB-INF/"))
                data = "<p>"+ FileTools.getFileNameWithoutExtension(url)+"</p>";
        }

        try {
            data = cleanText(url, data);
        } catch (UnsupportedEncodingException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return data;
    }

    private static String getArchiveData(String zipUrl, String realPath, String ext)
    {
        String data = "";

        String tempUrl = "/WEB-INF/tmp/fileindexer/"+ Password.generatePassword(5)+(new Date().getTime())+"/";
        String realTempUrl = Tools.getRealPath(tempUrl);
        try
        {
            if(".zip".equals(ext))
            {
                IwcmInputStream fis = new IwcmInputStream(realPath);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                ZipEntry entry;

                while((entry = zis.getNextEntry()) != null)
                {
                    Logger.debug(FileIndexer.class, "Extracting: "+entry);

                    String url = tempUrl;

                    url += entry.getName();

                    final String fullEntryPath = Tools.getRealPath(url);

                    if(entry.getName().endsWith("/")) {
                        new IwcmFile(fullEntryPath).mkdirs();
                        continue;
                    }
                    else
                    {
                        IwcmFile f = new IwcmFile(fullEntryPath);
                        IwcmFile parent = f.getParentFile();
                        parent.mkdirs();
                    }

                    IwcmFsDB.writeFiletoDest(zis, new File(fullEntryPath), (int)entry.getSize(), false);
                }
                zis.close();
            }
            /*else if(".rar".equals(ext))
            {
                File rarFile=new File(realTempUrl);
                if(rarFile.mkdirs() == false)
                    throw new SecurityException("Unable to create dirs: "+realTempUrl);
                rarFile=new File(realPath);
                final ReadOnlyAccessFile readOnlyAccessFile = new ReadOnlyAccessFile(rarFile);

                final Archive archive = new Archive(readOnlyAccessFile);
                final List<FileHeader> fileHeaders = archive.getFileHeaders();
                for (FileHeader fileHeader : fileHeaders)
                {
                    final String fileNameString = fileHeader.getFileNameString();
                    //write the files to the disk
                    Logger.println(FileIndexer.class,"Unrarring: " + fileNameString);

                    IwcmFile outFile = new IwcmFile(realTempUrl, fileNameString);

                    final IwcmFile parentFolder = outFile.getParentFile();

                    parentFolder.mkdirs();
                    IwcmOutputStream outStream=new IwcmOutputStream(outFile.getAbsolutePath());
                    archive.extractFile(fileHeader,outStream);
                    outStream.close();
                }
                readOnlyAccessFile.close();
            }*/
            IwcmFile tempDir = new IwcmFile(realTempUrl);
            data = getTempDirData(tempDir);
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }

        return data;
    }

    private static String getTempDirData(IwcmFile dir)
    {
        StringBuilder data=new StringBuilder();
        if(dir.isDirectory())
        {
            for(IwcmFile file : dir.listFiles())
            {
                if(file.isDirectory())
                {
                    data.append(getTempDirData(file));
                }
                else
                {
                    String url = file.getVirtualPath();

                    if (url.indexOf('.') > -1)
                    {
                        String ext = url.substring(url.lastIndexOf('.')).toLowerCase();
                        data.append(getData(url, file.getAbsolutePath(), data.toString(), ext)).append('\n');
                    }
                }
                file.delete();
            }
        }
        dir.delete();
        return data.toString();
    }

    /**
     * Check database table dirprop for dir_url, return index_fulltext, search to parents dir
     * @param fileUrl
     * @return
     */
    private static boolean shouldIndexFile(String fileUrl) {

        if (!fileUrl.startsWith("/files/") || fileUrl.endsWith("/upload") || fileUrl.startsWith("/files/protected/"))
		{
			return false;
		}

        if (fileUrl.contains(Constants.getString("fileArchivInsertLaterDirPath")))
        {
            return false;
        }

        //check file archivator
        String faArchivPathPrefix = FileArchivSupportMethodsService.normalizePath( FileArchivatorKit.getArchivPath() );
		if(fileUrl.startsWith(faArchivPathPrefix)) {
            FileArchivatorBean validateFile = FileArchivatorDB.getByUrl(fileUrl);
            if (validateFile==null) return false;
            return Tools.isTrue(validateFile.getIndexFile());
        }

        String testDir = fileUrl;

        int i = testDir.lastIndexOf("/");
        if (i>0) testDir = testDir.substring(0, i);
        else return false;

        Cache c = Cache.getInstance();
        String key = "FileIndexerTools.shouldIndexDir";
        @SuppressWarnings("unchecked")
        Map<String, Boolean> cachedMap = (Map<String, Boolean>)c.getObject(key);
        if (cachedMap == null) {
            cachedMap = new HashMap<>();
            c.setObjectSeconds(key, cachedMap, 300);
        }

        int failsafe = 0;
        while (testDir.length() > 0 && failsafe++<50) {

            Logger.debug(FileIndexerTools.class, "Checking dirprop for dir_url: " + testDir);

            Boolean isIndexed = (new SimpleQuery()).forBooleanWithNull("SELECT index_fulltext FROM dirprop WHERE dir_url=?", testDir);
            if (isIndexed != null) {
                cachedMap.put(testDir, isIndexed);
                return isIndexed.booleanValue();
            }

            i = testDir.lastIndexOf("/");
            if (i>0) testDir = testDir.substring(0, i);
            else testDir = "";
        }

        return false;
    }
}
