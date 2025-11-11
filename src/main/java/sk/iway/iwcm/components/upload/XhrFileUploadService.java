package sk.iway.iwcm.components.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.stripes.MultipartWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling file uploads via XHR.
 * It stores uploaded file chunks temporarily in random folder, and assembles them once all chunks are received.
 * It also provides methods to move, replace, and delete uploaded files by their unique keys.
 */
@Service
public class XhrFileUploadService {

    private final String ALLOWED_EXTENSIONS = "doc docx xls xlsx xml ppt pptx pdf jpeg jpg bmp tiff psd zip rar png mp4";
    private final String BASE_DIR = "WEB-INF/tmp/";
    private final String FINAL_PREFIX = "final_";
    private final String SEPARATOR = "__upload__";

    protected XhrFileUploadResponse processUpload(HttpServletRequest request) {
        Prop prop = Prop.getInstance();
        try {
            String name = request.getParameter("name");
            Logger.debug(XhrFileUploadService.class, "doPost, name from parameter: "+name);

            //dropzone.js kompatibilita
            Part filePart = request.getPart("file");

            if (filePart == null) {
                XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
                xhrFileUploadResponse.setError(prop.getText("components.forum.new.upload_not_allowed_filetype"));
                xhrFileUploadResponse.setFile("");
                xhrFileUploadResponse.setSuccess(false);
                return xhrFileUploadResponse;
            }

            if (filePart.getSubmittedFileName() != null) {
                name = filePart.getSubmittedFileName();
            }

            Logger.debug(XhrFileUploadService.class, "doPost, name from filePart: "+name);

            String extension = FileTools.getFileExtension(name);

            Logger.debug(XhrFileUploadService.class, "doPost, name="+name+", extension="+extension);
            List<String> allowedExtensions = Tools.getStringListValue(Tools.getTokens(Constants.getString("xhrFileUploadAllowedExtensions", ALLOWED_EXTENSIONS), " "));
            if (!allowedExtensions.contains("*") && !allowedExtensions.contains(extension)) {
                Logger.debug(XhrFileUploadService.class, "doPost, extension not allowed: "+extension);
                XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
                xhrFileUploadResponse.setError(prop.getText("components.forum.new.upload_not_allowed_filetype"));
                xhrFileUploadResponse.setFile(name);
                xhrFileUploadResponse.setSuccess(false);
                return xhrFileUploadResponse;
            }

            int chunk = Tools.getIntValue(request.getParameter("chunk"), 0);
            int chunks = Tools.getIntValue(request.getParameter("chunks"), 0);

            //dropzone.js kompatibilita
            if (request.getParameter("dzchunkindex") != null) chunk = Tools.getIntValue(request.getParameter("dzchunkindex"), 0);
            if (request.getParameter("dztotalchunkcount") != null) chunks = Tools.getIntValue(request.getParameter("dztotalchunkcount"), 0);

            Logger.debug(XhrFileUploadService.class, "doPost, chunk="+chunk+", chunks="+chunks);

            HttpSession session = request.getSession();

            String fileKey;
            PartialUploadHolder holder = (PartialUploadHolder) session.getAttribute("partialUploadFile-" + name);
            if (holder == null || chunk == 0) {
                fileKey = RandomStringUtils.secure().next(15, true, true);
                holder = new PartialUploadHolder(chunks, name, fileKey);

                session.setAttribute("partialUploadFile-" + name, holder);
            }
            else {
                fileKey = holder.getKey();
            }

            boolean isLast = false;
            if (holder.getPartPaths().size() + 1 == holder.getChunks() || holder.getChunks() == 0) {
                //je to posledny alebo jediny chunk
                isLast = true;
            }

            String suffix = FileTools.getFileExtension(name);

            File targetDir = new File(Tools.getRealPath(BASE_DIR + fileKey));
            if (!targetDir.exists()) {
                boolean mkdirSuccess = targetDir.mkdirs();
                Logger.debug(XhrFileUploadService.class, "mkdir result =" + mkdirSuccess);
            }

            //zapisem data do docasneho suboru
            File tempUploadFile = File.createTempFile(fileKey, ".part." + chunk, targetDir);
            FileOutputStream tempFos = new FileOutputStream(tempUploadFile);
            InputStream tempIs = filePart.getInputStream();
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = tempIs.read(bytes)) != -1) {
                tempFos.write(bytes, 0, read);
            }

            tempIs.close();
            tempFos.close();

            String filePartName = tempUploadFile.getAbsolutePath();//filePart.get//getName();
            holder.getPartPaths().add(chunk, filePartName);

            XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
            xhrFileUploadResponse.setSuccess(true);
            xhrFileUploadResponse.setChunkUploaded(chunk);
            xhrFileUploadResponse.setSize(new File(filePartName).length());

            if (isLast) {
                session.removeAttribute("partialUploadFile-" + name);
                // mam posledny, spojim ich do jedneho

                FileOutputStream fos;
                FileInputStream fis;
                byte[] fileBytes;
                File tempfile = null;
                try {
                    //createTempFile() pridava do kazdeho nazvu random cisla, preto pouzijeme SEPARATOR na lahke odmazanie z finalneho nazvu
                    String finalFileName = FINAL_PREFIX + FileTools.getFileNameWithoutExtension(name) + SEPARATOR;
                    tempfile = File.createTempFile(finalFileName, "." + suffix, targetDir);
                    fos = new FileOutputStream(tempfile, true);
                    for (String chunkPath : holder.getPartPaths()) {
                        File inputFile = new File(chunkPath);
                        fis = new FileInputStream(inputFile);
                        fileBytes = new byte[(int) inputFile.length()];
                        fis.read(fileBytes, 0, (int) inputFile.length());
                        fos.write(fileBytes);
                        fos.flush();
                        fileBytes = null;
                        fis.close();
                        fis = null;
                        inputFile.delete(); //NOSONAR
                    }
                    fos.close();
                    fos = null;

                    WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);
                    fileWebjetEvent.publishEvent();

                    if (request.getAttribute("xhrError") != null) {
                        Logger.debug(XhrFileUploadService.class, "doPost, xhrError, name="+name);
                        xhrFileUploadResponse = new XhrFileUploadResponse();
                        xhrFileUploadResponse.setFile(name);
                        xhrFileUploadResponse.setSuccess(false);
                        xhrFileUploadResponse.setError((String) request.getAttribute("xhrError"));
                        return xhrFileUploadResponse;
                    }

                    Logger.debug(XhrFileUploadService.class, "doPost, success, name="+name+", key="+fileKey);

                    xhrFileUploadResponse.putName(name);
                    xhrFileUploadResponse.putKey(fileKey);

                } catch (IOException ioe) {
                    sk.iway.iwcm.Logger.error(ioe);
                }
            } else {
                MultipartWrapper.slowdownUpload();
            }

            return xhrFileUploadResponse;
        }
        catch (Exception ex) {
            Logger.warn(XhrFileUploadService.class, ex.getMessage());
            sk.iway.iwcm.Logger.error(ex);

            XhrFileUploadResponse xhrFileUploadResponse = new XhrFileUploadResponse();
            xhrFileUploadResponse.setError(prop.getText("components.docman.error.db"));
            xhrFileUploadResponse.setSuccess(false);
            return xhrFileUploadResponse;
        }
    }

    public void setResponse(HttpServletResponse response, XhrFileUploadResponse xhrFileUploadResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(mapper.writeValueAsString(xhrFileUploadResponse));
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    @SuppressWarnings("java:S1130")
    public String moveFile(String fileKey, String dir) throws IOException
    {
        IwcmFile dirFile = new IwcmFile(dir);
        String dirVirtualPath = dirFile.getVirtualPath();

        IwcmFile file = getTempFinalFile(fileKey);

        if (file == null || !file.exists()) return null;

        String originalFilename = file.getName();

        if (originalFilename.startsWith(FINAL_PREFIX)) {
            String baseName = originalFilename.substring(FINAL_PREFIX.length()).split(SEPARATOR)[0];
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex != -1) {
                originalFilename = baseName + originalFilename.substring(dotIndex);
            } else {
                originalFilename = baseName;
            }
        }

        if (dirVirtualPath.startsWith("/images") || dirVirtualPath.startsWith("/files"))
        {
            originalFilename = DB.internationalToEnglish(originalFilename);
            originalFilename = DocTools.removeCharsDir(originalFilename, true).toLowerCase();
        }

        String filename = originalFilename;

        IwcmFile dest = new IwcmFile(dir, originalFilename);
        int counter = 1;
        while (dest.exists())
        {
            filename = originalFilename + "-" + counter;
            if (originalFilename.contains("."))	filename = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "-" + counter + originalFilename.substring(originalFilename.lastIndexOf("."));
            dest = new IwcmFile(dir, filename);
            counter++;
        }

        Logger.debug(XhrFileUploadService.class, "moveFile, Moving file "+file.getAbsolutePath()+", to "+dest.getAbsolutePath());
        FileTools.moveFile(file, dest);
        Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);
        return filename;
    }

    @SuppressWarnings("java:S1130")
    public String moveAndReplaceFile(String fileKey, String dir, String fileNameParam) throws IOException
    {
        String fileName = fileNameParam;

        IwcmFile dirFile = new IwcmFile(dir);
        String dirVirtualPath = dirFile.getVirtualPath();

        if (dirVirtualPath.startsWith("/images") || dirVirtualPath.startsWith("/files"))
        {
            fileName = DB.internationalToEnglish(fileName);
            fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
        }

        IwcmFile file = getTempFinalFile(fileKey);
        if (file == null || !file.exists()) return null;
        IwcmFile dest = new IwcmFile(dir + fileName);

        if (dest.exists()) {
            dest.delete();
        }

        Logger.debug(XhrFileUploadService.class, "moveAndReplaceFile, Moving file "+file.getAbsolutePath()+", to "+dest.getAbsolutePath());
        FileTools.moveFile(file, dest);
        Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);

        return fileName;
    }

    public boolean delete(String hash) {
        IwcmFile file = getTempFinalFile(hash);
        return file != null && file.delete();
    }

    public String getTempFileName(String fileKey)
    {
        IwcmFile file = getTempFinalFile(fileKey);
        return file != null ? file.getName() : null;
    }

    public String getTempFilePath(String fileKey)
    {
        IwcmFile file = getTempFinalFile(fileKey);
        return file != null ? file.getAbsolutePath() : null;
    }

    private IwcmFile getTempFinalFile(String fileKey) {
        IwcmFile baseDir = new IwcmFile(Tools.getRealPath(BASE_DIR + fileKey));
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return null;
        }
        IwcmFile[] files = baseDir.listFiles(file -> file.getName().startsWith(FINAL_PREFIX));
        if (files == null) return null;
        Optional<IwcmFile> finalFile = Arrays.stream(files).findFirst();
        return finalFile.orElse(null);
    }
}
