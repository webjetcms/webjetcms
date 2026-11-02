package sk.iway.iwcm.admin.upload;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.system.stripes.MultipartWrapper;
import sk.iway.iwcm.users.UsersDB;

/**
 * Receives administrative uploads in chunks, validates their destination, and assembles them.
 * Completed uploads are either written directly to an approved folder or retained temporarily
 * for a subsequent conflict-resolution request.
 */
@WebServlet("/admin/upload/chunk")
@MultipartConfig
public class AdminUploadServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Map<String,PathHolder> temporary = new ConcurrentHashMap<>();

	/**
	 * Validates and stores one upload chunk, assembling and processing the file after the final chunk.
	 * File archive requests also preserve their validated bulk metadata across all chunks.
	 *
	 * @param request multipart upload request
	 * @param response response receiving the JSON upload status
	 * @throws ServletException when the servlet container cannot process the multipart request
	 * @throws IOException when request data cannot be read or the response cannot be written
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
        String name = request.getParameter("name");
        String destinationFolder = AdminUploadValidator.normalizeDestinationFolder(request.getParameter("destinationFolder"));
        boolean writeDirectlyToDestination = "true".equals(request.getParameter("writeDirectlyToDestination"));
        String overwriteMode = request.getParameter("overwriteMode");
        boolean isBase64 = "base64".equals(request.getParameter("encoding"));
        String uploadType = Tools.getStringValue(request.getParameter("uploadType"), "");
        boolean saveIntoArchive = "fileArchive".equals(uploadType);
        FileArchiveBulkUploadOptions bulkUploadOptions = null;

        JSONObject output = new JSONObject();

		if (name == null) {
			//dropzone.js kompatibilita
			Part filePart = request.getPart("file");
			name = filePart.getSubmittedFileName();
        }
        String originalName = name;

        if (destinationFolder!=null && (saveIntoArchive || destinationFolder.startsWith("/files") || destinationFolder.startsWith("/images") || destinationFolder.startsWith("/shared"))) {
            name = AdminUploadValidator.normalizeFileName(name);
        }

		Logger.debug(AdminUploadServlet.class, "doPost, name="+name);

        Identity user = UsersDB.getCurrentUser(request);

        long fileSize = Tools.getLongValue(request.getHeader("Content-Length"), Long.MAX_VALUE);
        if (request.getParameter("dztotalfilesize")!=null) fileSize = Tools.getLongValue(request.getParameter("dztotalfilesize"), fileSize);

        String extension = FileTools.getFileExtension(name);

        int chunk = Tools.getIntValue(request.getParameter("chunk"), 0);
        int chunks = Tools.getIntValue(request.getParameter("chunks"), 0);

        //dropzone.js compatibility
        if (request.getParameter("dzchunkindex")!=null) chunk = Tools.getIntValue(request.getParameter("dzchunkindex"), 0);
        if (request.getParameter("dztotalchunkcount")!=null) chunks = Tools.getIntValue(request.getParameter("dztotalchunkcount"), 0);

        HttpSession session = request.getSession();
        String partialUploadSessionKey = "partialUploadFile-"+name;
        PartialUploadHolder holder = (PartialUploadHolder)session.getAttribute(partialUploadSessionKey);

        //bulkUploadOptions stays non-null whenever errorKey remains null on the archive upload path below
        String errorKey;
        if (saveIntoArchive) {
            errorKey = AdminUploadValidator.validateUserAndFile(
                name, uploadType, fileSize, user, request
            );
            if (errorKey == null) {
                String referer = request.getHeader("referer");
                errorKey = FileArchiveUploadService.validateArchiveUploadPermission(user, destinationFolder, referer);
                if (errorKey == null) {
                    destinationFolder = FileArchiveUploadService.normalizeArchiveFolder(destinationFolder);
                    if (chunk > 0 && holder != null && holder.getFileArchiveBulkUploadOptions() != null) {
                        bulkUploadOptions = holder.getFileArchiveBulkUploadOptions();
                    } else {
                        bulkUploadOptions = FileArchiveBulkUploadOptions.fromRequest(request);
                    }
                    errorKey = bulkUploadOptions.getErrorKey();
                }
            }
        } else {
            errorKey = AdminUploadValidator.validateChunk(
                destinationFolder, name, uploadType, fileSize,
                writeDirectlyToDestination, user, request
            );
        }

		if (errorKey != null) {
			cleanupPartialUpload(session, partialUploadSessionKey, holder);
			try {
				Prop prop = Prop.getInstance();
				output.put("error", prop.getText(errorKey));
				output.put("file", name);
				output.put("success", false);
			}
			catch (JSONException e) {
                Logger.error(AdminUploadServlet.class, e);
			}
		}
        else {
            Logger.debug(AdminUploadServlet.class, "doPost, chunk="+chunk+" chunks="+chunks);

            Part filePart = request.getPart("file");

            if (holder==null || chunk == 0)
            {
                cleanupPartialUpload(session, partialUploadSessionKey, holder);
                holder = new PartialUploadHolder(chunks, name);
                holder.setFileArchiveBulkUploadOptions(bulkUploadOptions);
                session.setAttribute(partialUploadSessionKey, holder);
            }
            boolean isLast = false;
            if (holder.getPartPaths().size()+1 == holder.getChunks() || holder.getChunks()==0)
            {
                //je to posledny alebo jediny chunk
                isLast = true;
            }

            //zapisem data do docasneho suboru
            File tempFolder = new File(Tools.getRealPath("/WEB-INF/tmp/"));
            if (tempFolder.exists()==false) tempFolder.mkdirs();
            File tempUploadFile = File.createTempFile(name, "."+extension, tempFolder);
            Logger.debug(AdminUploadServlet.class, "Storing uploaded file, tempFile=" + tempUploadFile.getAbsolutePath());

            FileOutputStream tempFos = new FileOutputStream(tempUploadFile);
            InputStream tempIs = filePart.getInputStream();
            int read=0;
            byte[] bytes = new byte[1024];
            while ((read = tempIs.read(bytes)) != -1)
            {
                tempFos.write(bytes, 0, read);
            }
            tempIs.close();
            tempFos.close();

            String filePartName = tempUploadFile.getAbsolutePath();//filePart.get//getName();
            holder.getPartPaths().add(chunk, filePartName);
            if (!isLast) {
                session.setAttribute(partialUploadSessionKey, holder); //this update holder also in redis
            }
            try {
                output.put("chunk-uploaded", chunk);
                output.put("size", new File(filePartName).length());
                output.put("success", true);
            } catch (JSONException e) {
                //
            }

            if (isLast)
            {
                session.removeAttribute(partialUploadSessionKey);
                // mam posledny, spojim ich do jedneho

                IwcmOutputStream fos = null;
                String random = RandomStringUtils.secure().next(15, true, true);
                boolean destinationFileExists = false;
                IwcmFile imageDestination = null;
                IwcmFile imageStaging = null;
                int imageResult = 0;
                try
                {
                    IwcmFile outputFile = null;
                    String destinationVirtualPath = "";

                    if (saveIntoArchive == false && writeDirectlyToDestination && Tools.isNotEmpty(destinationFolder) && FileBrowserTools.hasForbiddenSymbol(destinationFolder)==false)
                    {
                        IwcmFile dirFile = new IwcmFile(Tools.getRealPath(destinationFolder));
                        if (dirFile.exists()==false) dirFile.mkdirs();

                        //over, ci subor v destinacii existuje
                        destinationVirtualPath = destinationFolder + name;
                        outputFile = new IwcmFile(Tools.getRealPath(destinationVirtualPath));
                        if (outputFile.exists()) {

                            if ("keepboth".equals(overwriteMode)) {
                                //ziskaj meno noveho suboru
                                name = UploadService.getKeppBothFileName(destinationFolder, name);

                                if (name != null) {
                                    destinationVirtualPath = destinationFolder + name;
                                    outputFile = new IwcmFile(Tools.getRealPath(destinationVirtualPath));
                                } else {
                                    destinationFileExists = true;
                                }

                            }
                            else {
                                destinationFileExists = true;
                            }
                        }

                        //ak subor neexistuje mozeme ho rovno zapisat
                        if (destinationFileExists==false || "overwrite".equals(overwriteMode)) {
                            if (isBase64) {
                                // Publish the editor's image only after validation and any required encoding have succeeded.
                                imageDestination = outputFile;
                                imageStaging = new IwcmFile(File.createTempFile(random, "." + extension, tempFolder));
                                outputFile = imageStaging;
                            }
                            fos = new IwcmOutputStream(outputFile);
                            destinationFileExists = false;
                        } else {
                            destinationVirtualPath = "";
                        }
                    }

                    if (fos == null) {
                        outputFile = new IwcmFile(File.createTempFile(random, "."+extension, tempFolder));
                        fos = new IwcmOutputStream(outputFile);
                    }
                    try (IwcmOutputStream assembled = fos) {
                        for (String chunkPath : holder.getPartPaths()) {
                            File inputFile = new File(chunkPath);
                            try (FileInputStream input = new FileInputStream(inputFile)) {
                                input.transferTo(assembled);
                            }
                            inputFile.delete();
                        }
                    }

                    if (isBase64) {
                        imageResult = decodeBase64File(outputFile);
                        if (imageResult != 0) throw new IOException("Image decoding failed, result=" + imageResult);
                    }
                    if (imageDestination != null) {
                        if (!FileTools.copyFile(outputFile, imageDestination)) {
                            throw new IOException("Could not save processed image to " + imageDestination.getVirtualPath());
                        }
                        outputFile = imageDestination;
                    }

                    //output = "{\"name\":\""+name+"\",\"key\":\""+random+"\"}";
                    try
                    {
                        if (Tools.isNotEmpty(destinationVirtualPath))
                        {
                            UploadService uploadService = new UploadService(destinationVirtualPath, request);
                            uploadService.process();
                            if (uploadService.getErrorKey() != null) {
                                output.put("success", false);
                                output.put("error", Prop.getInstance(request).getText(uploadService.getErrorKey()));
                            }
                            Logger.debug(AdminUploadServlet.class, "upload processed, removed="+uploadService.getRemovedUrls()+" added="+uploadService.getAddedUrls());

                            Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + destinationVirtualPath, -1, -1);
                        }

                        if ("skip".equals(overwriteMode) && destinationFileExists)  {
                            if (outputFile != null) outputFile.delete();
                            destinationFileExists = false;
                        }

                        output.put("name", name);
                        output.put("destinationFolder", destinationFolder);
                        output.put("uploadType", uploadType);
                        output.put("virtualPath", destinationVirtualPath);
                        output.put("key", random);
                        output.put("exists", destinationFileExists);
                    }
                    catch (JSONException e) {
                        Logger.error(AdminUploadServlet.class, e);
                    }
                    if (outputFile != null) {
                        temporary.put(random, new PathHolder(name, outputFile.getAbsolutePath(), Tools.getNow()));

                        if (saveIntoArchive && writeDirectlyToDestination) {
                            Prop prop = Prop.getInstance(request);
                            FileArchiveUploadService.saveNewArchiveFile(user, prop, destinationFolder, name,
                                originalName, random, bulkUploadOptions, output);
                        }
                    }
                }
                catch (IOException ioe)
                {
                    Logger.error(AdminUploadServlet.class, ioe);
                    output.put("success", false);
                    output.put("error", Prop.getInstance(request).getText(imageResult != 0
                        ? ImageTools.getErrorKey(imageResult) : "multiple_files_upload.upload_error"));
                }
                finally {
                    if (imageStaging != null) imageStaging.delete();
                }
            }
            else
            {
                MultipartWrapper.slowdownUpload();
            }
        }

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(output.toString());
	}

    /**
     * Removes a partial upload from the session and deletes every temporary chunk already stored.
     *
     * @param session session containing the partial upload
     * @param sessionKey session attribute key for the upload
     * @param holder partial upload state, or {@code null} when no upload is in progress
     */
    private static void cleanupPartialUpload(HttpSession session, String sessionKey, PartialUploadHolder holder) {
        if (holder == null) return;

        session.removeAttribute(sessionKey);
        for (String partPath : holder.getPartPaths()) {
            File partFile = new File(partPath);
            if (partFile.exists() && partFile.delete() == false) {
                Logger.debug(AdminUploadServlet.class, "Failed to delete partial upload file: " + partPath);
            }
        }
    }

    /**
     * Moves a temporary upload to its final destination, replacing an existing file if necessary.
     *
     * @param fileKey unique key of the temporary upload
     * @param destinationFolder virtual path of the destination folder, for example {@code /images/gallery/}
     * @param fileNameParam requested destination file name
     * @return the final file name, or {@code null} when the temporary file was not moved
     * @throws IOException when the file cannot be moved
     */
    public static String moveAndReplaceFile(String fileKey, String destinationFolder, String fileNameParam) throws IOException
    {
        String fileName = fileNameParam;
        if (Tools.isNotEmpty(fileKey) && temporary.containsKey(fileKey))
        {
            IwcmFile dirFile = new IwcmFile(Tools.getRealPath(destinationFolder));
            if (dirFile.exists()==false) dirFile.mkdirs();

            if (destinationFolder.startsWith("/images") || destinationFolder.startsWith("/files") || destinationFolder.startsWith("/shared"))
            {
                fileName = DB.internationalToEnglish(fileName);
                fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
            }

            IwcmFile file = new IwcmFile(temporary.get(fileKey).getTempPath());
            if (!file.exists()) return null;
            IwcmFile dest = new IwcmFile(dirFile.getAbsolutePath(), fileName);

            if (dest.exists()) {
                dest.delete();
            }

            Logger.debug(AdminUploadServlet.class, "Moving file "+file.getAbsolutePath()+" to "+dest.getAbsolutePath());
            FileTools.moveFile(file, dest);
            Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload (xhr), path=" + dest.getVirtualPath(), -1, 1);

            return fileName;
        }
        return null;
    }

    /**
     * Deletes a temporary upload that is no longer needed.
     *
     * @param fileKey unique key of the temporary upload
     * @return {@code true} when the temporary file existed and was deleted
     */
    public static boolean deleteTempFile(String fileKey) {
        PathHolder pathHolder = temporary.remove(fileKey);
        if (pathHolder != null) {
            IwcmFile file = new IwcmFile(pathHolder.getTempPath());
            if (file.exists()) return file.delete();
        }

        return false;
    }

    /**
     * Decodes Base64 upload content and restores the requested image format when necessary.
     * Valid PNG input saved as PNG retains its original bytes without re-encoding.
     *
     * @param file temporary file containing Base64-encoded data
     * @return 0 on success, otherwise an ImageTools result code
     */
    private int decodeBase64File(IwcmFile file) {
        try {
            byte[] decoded;
            try (InputStream input = new IwcmInputStream(file)) {
                decoded = Base64.getDecoder().decode(input.readAllBytes());
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(decoded));
            if (image == null) return 4;

            if ("png".equals(FileTools.getFileExtension(file.getName()))
                && new ImageInfo(new ByteArrayInputStream(decoded)).getFormat() == ImageInfo.FORMAT_PNG) {
                // Keep the validated PNG's original compression and metadata; the caller publishes the staged file.
                try (IwcmOutputStream output = new IwcmOutputStream(file)) {
                    output.write(decoded);
                }
                return 0;
            }

            if (!ImageTools.supportsTransparency(file.getName())) {
                BufferedImage opaque = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = opaque.createGraphics();
                try {
                    graphics.drawImage(image, 0, 0, Color.WHITE, null);
                } finally {
                    graphics.dispose();
                }
                image = opaque;
            }
            return ImageTools.writeImage(image, file.getName(), 90, file);
        } catch (IllegalArgumentException ex) {
            Logger.error(AdminUploadServlet.class, ex);
            return 4;
        } catch (IOException ex) {
            Logger.error(AdminUploadServlet.class, ex);
            return 2;
        } catch (Exception ex) {
            Logger.error(AdminUploadServlet.class, ex);
            return 3;
        }
    }

    /**
	 * Returns the physical path of a temporary upload for validation by downstream consumers.
	 *
	 * @param fileKey unique key of the temporary upload
	 * @return temporary file path, or {@code null} when the key is unknown
	 */
	public static String getTempFilePath(String fileKey)
	{
		if (Tools.isNotEmpty(fileKey) && temporary.containsKey(fileKey))
		{
			return temporary.get(fileKey).getTempPath();
		}
		return null;
	}

    /**
     * Returns the original name of a temporary upload.
     *
     * @param fileKey unique key of the temporary upload
     * @return original file name, or {@code null} when the key is unknown
     */
    public static String getOriginalFileName(String fileKey) {
        if (Tools.isNotEmpty(fileKey) && temporary.containsKey(fileKey))
		{
			return temporary.get(fileKey).getFileName();
		}
		return null;
    }
}
