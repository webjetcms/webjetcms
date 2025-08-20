package sk.iway.iwcm.components.ai.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import sk.iway.iwcm.io.IwcmInputStream;

@Service
public class AiTempFileStorage {

    public static final String AI_FILE_DIR = "/WEB-INF/ai_files";

    public static Path getFileFolder() throws IOException {
        String filePath = Tools.getRealPath(AI_FILE_DIR);
        Path tempDir = Paths.get(filePath);

        if (Files.notExists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        return tempDir;
    }

    public static String addImage(String base64, String fileName, String fileFormat) throws IOException {
        return addImage(base64, fileName, fileFormat, getFileFolder());
    }

    public static String addImage(String base64, String fileName, String fileFormat, Path fileFolder) throws IOException {
        File tempUploadFile = File.createTempFile(fileName, fileFormat, fileFolder.toFile());

        byte[] imageBytes = Base64.getDecoder().decode(base64);
        try (FileOutputStream out = new FileOutputStream(tempUploadFile)) {
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
        byte buff[] = new byte[8000];
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

    public static void saveTempFile(String fileName) throws IOException {
        Path tempFileFolder = getFileFolder();
        Path tempFilePath = tempFileFolder.resolve(fileName);

        if (Files.notExists(tempFilePath)) {
            throw new IOException("Field with name " + fileName + " do not exists.");
        }


    }
}
