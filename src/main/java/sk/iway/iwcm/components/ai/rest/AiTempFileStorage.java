package sk.iway.iwcm.components.ai.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

@Service
public class AiTempFileStorage {

    private AiTempFileStorage() {
        // Intentionally left empty to prevent instantiation
    }

    public static final String AI_FILE_DIR = "/WEB-INF/tmp/ai_files";

    public static Path getFileFolder() throws IOException {
        String filePath = Tools.getRealPath(AI_FILE_DIR);
        Path tempDir = Paths.get(filePath);

        if (Files.notExists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        if(Files.isDirectory(tempDir) == false) throw new IOException("Tempfile folder is not a folder");

        return tempDir;
    }

    public static String addImage(String base64, String fileName, String fileFormat) throws IOException {
        return addImage(base64, fileName, fileFormat, getFileFolder());
    }

    public static String addImage(String base64, String fileName, String fileFormat, Path fileFolder) throws IOException {
        IwcmFile tempUploadFile = new IwcmFile( File.createTempFile(fileName, fileFormat, fileFolder.toFile()) );

        byte[] imageBytes = Base64.getDecoder().decode(base64);
        try (IwcmOutputStream out = new IwcmOutputStream(tempUploadFile)) {
            out.write(imageBytes);
        }

        return tempUploadFile.getName();
    }

    public static void downloadFile(String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Path tempFileFolder = getFileFolder();
        Path tempFilePath = tempFileFolder.resolve(fileName);

        if (Files.notExists(tempFilePath)) {
            throw new IOException("Field with name " + fileName + " do not exists.");
        }

        String filePath = tempFilePath.toAbsolutePath().toString();

        ServletOutputStream out = response.getOutputStream();
        //citaj subor a posielaj na vystup
        byte[] buff = new byte[8000];
        IwcmInputStream fis = new IwcmInputStream(filePath);
        int len;

        String mimeType = "application/octet-stream";
        try {
	    	mimeType = Constants.getServletContext().getMimeType(filePath.toLowerCase());
        } catch (Exception ex) {
	    	sk.iway.iwcm.Logger.error(ex);
        }

        if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";

        response.setContentType(mimeType);

        while ((len = fis.read(buff)) != -1) out.write(buff, 0, len);

        fis.close();
        out.flush();
        out.close();
    }

    public static String saveTempFile(String tempFileName, String fielName, String imageLocation) throws IOException, IllegalStateException {
        if(Tools.isEmpty(tempFileName)) throw new IOException("File not found");
        if(Tools.isEmpty(fielName)) throw new IllegalStateException("New file name cant be empty");
        if(Tools.isEmpty(imageLocation)) throw new IllegalStateException("New file location cant be empty");

        Path tempFileFolder = getFileFolder();
        Path tempFilePath = tempFileFolder.resolve(tempFileName);

        if (Files.notExists(tempFilePath))
            throw new IOException("File with name " + tempFileName + " do not exists.");

        if(Files.isDirectory(tempFilePath))
            throw new IOException("File with name " + tempFileName + " is a FOLDER.");

        IwcmFile tempFile = new IwcmFile( tempFilePath.toFile() );
        InputStream tempFileIS = new IwcmInputStream(tempFile);

        String path = imageLocation;
        if(imageLocation.endsWith("/") == false) path += "/";

        String newFileUrl = path + fielName + "." + tempFileName.substring(tempFileName.lastIndexOf('.') + 1);
        IwcmFile newRealFile = new IwcmFile( Tools.getRealPath(newFileUrl) );

        IwcmFsDB.writeFiletoDest(tempFileIS, new File(newRealFile.getAbsolutePath()), Tools.safeLongToInt(tempFile.getLength()));

        String prefix = tempFileName.substring(0, tempFileName.lastIndexOf('_') + 1);

        //After success remove all temp files with same prefix
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempFileFolder)) {
            for (Path child : stream) {
                if(child.getFileName().toString().startsWith(prefix)) {
                    Files.delete(child);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newFileUrl;
    }
}
