package sk.iway.iwcm.editor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.upload.UploadedFile;

/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.12 $
 *@created      Piatok, 2002, mďż˝j 17
 *@modified     $Date: 2004/03/12 10:16:36 $
 */
public class UploadFileAction {

	private static final String FILE_NOT_ALLOWED = "/admin/FCKeditor/editor/dialog/editor_upload_iframe.jsp";

	private UploadFileAction() {
		//utility class
	}

	/**
	 * Save image that was drag/drop inside opened web page editor (uploadType=ckeditor).
	 * @param request
	 * @param response
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public static String execute(HttpServletRequest request, HttpServletResponse response, CommonsMultipartFile multipartFile) throws IOException {

		HttpSession session = request.getSession();
		if (session == null) return "logon_admin";

		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		//povolenie pre blog alebo wiki
		if (UsersDB.checkUserPerms(user, Constants.getString("webpagesFunctionsPerms")) == false) return "logon_admin";

		int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);
		int docId = Tools.getIntValue(request.getParameter("docId"), -1);
		String title = request.getParameter("title");
		FileItem file = multipartFile.getFileItem();
		String fileURL = "";
		String realPath = null;
		String fileName = null;
		//Size must be taken from CommonsMultipartFile !! - CommonsMultipartFile.getFileItem().getSize() isn't firht value
		long fileSize = multipartFile.getSize();

		//Check file size for upload, upload type id fix ckeditor, we dont use this method for other upload type's
		if (!isFileAllowed("ckeditor", file, user, request, fileSize)) return FILE_NOT_ALLOWED;

		if (file != null) {
			//Retrieve the file name
			fileName = file.getName().trim();

			//Check file name
			if (!Tools.isEmpty(fileName)) {

				String extension = FileTools.getFileExtension(fileName);
				ImageInfo ii = new ImageInfo(file.getInputStream());
				Logger.debug(UploadFileAction.class, "ckeditor upload, extension=" + extension + " ii=" + ii.getFormatName());

				String extensionII = ii.getFormatName();
				if (extensionII != null && extensionII.length() > 2) {
					//musime spravit podla ImageInfo lebo Word klame ze posiela png ale realne je obrazok jpg
					extensionII = extensionII.toLowerCase();
					extensionII = Tools.replace(extensionII, "jpeg", "jpg");
					extension = extensionII;
				}

				if (fileName.equals("image.png") || fileName.equals("image.jpg")) {
					//jedna sa o pasted grafiku z nejakeho programu
					fileName = "image-" + PkeyGenerator.getNextValue("ckeditor_upload_counter") + "." + extension;
				}

				//adresar odvodime podla posledne editovanej stranky
				Logger.debug(UploadFileAction.class, "UPLOAD docId="+docId+" groupId="+groupId);

				String subDir = UploadFileTools.getPageUploadSubDir(docId, groupId, title, null);
				if (ContextFilter.isRunning(request) && subDir.startsWith(request.getContextPath()))
					subDir = ContextFilter.removeContextPath(request.getContextPath(), subDir);

				IwcmFile f = IwcmFile.fromVirtualPath(Constants.getString("imagesRootDir") + subDir, fileName);
				if(f.exists())
					fileName = PkeyGenerator.getNextValue("ckeditor_upload_counter")+"_"+fileName;

				realPath = Tools.getRealPath(Constants.getString("imagesRootDir") + subDir) + File.separatorChar + fileName;
				fileURL = Constants.getString("imagesRootDir") + subDir + "/" + fileName;

				if (realPath != null && fileSize > 0) {
					IwcmFile f2 = new IwcmFile(realPath);

					//Save main file
					IwcmFsDB.writeFiletoDest(file.getInputStream(), new File(f2.getPath()), (int)fileSize);

					Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "file upload: " + realPath, -1, -1);
				}

				if (GalleryDB.isGalleryFolder(Constants.getString("imagesRootDir") + subDir)) {
					//v realpath mas cestu k obrazku kde jeOriginal
					//ten nechas ako je
					//ale vytvoris jeho novu kopiu
					GalleryDB.resizePicture(realPath, Constants.getString("imagesRootDir") + subDir);
				} else if (Constants.getBoolean("imageAlwaysCreateGalleryBean")) {
					GalleryDB.setImage(Constants.getString("imagesRootDir") + subDir, fileName);
				}

				UploadFileAction.reflectionLoader(request, user, fileURL);
			}

			if (fileURL != null && fileName != null) {
				response.setStatus(HttpServletResponse.SC_OK);
				PrintWriter out = response.getWriter();

				fileURL = request.getContextPath() + fileURL;
				fileURL = Tools.replace(fileURL, "//", "/");

				out.print("{\"fileName\":\""+ResponseUtils.filter(fileName)+"\",\"uploaded\":1,\"url\":\""+ Tools.replace(fileURL, "/", "\\/") +"\",\"error\":{\"number\"" +
						":201,\"message\":\"A file with the same name is already available. The uploaded file was renamed to \\\"847" +
						"(2).jpg\\\".\"}}");
			}
		}

		return null;
	}

	/**
	 * Check is the file is allowed
	 * @param uploadType
	 * @param file
	 * @param user
	 * @param request
	 * @param fileSize
	 * @return
	 */
	public static boolean isFileAllowed(String uploadType, FileItem file, Identity user, HttpServletRequest request, Long fileSize) {

		if (file == null) return false;

		//Prep file name
		String fileName = file.getName().trim();
		fileName = fileName.toLowerCase();

		//
		return UploadFileTools.isFileAllowed(uploadType, fileName, fileSize, user, request);
	}

	public static boolean isFileAllowed(UploadedFile formFile, Identity user, String uploadType, HttpServletRequest request)
	{
		String fileName = formFile.getFileName().trim();
		return UploadFileTools.isFileAllowed(uploadType, fileName, formFile.getFileSize(), user, request);
	}

	public static boolean isFileAllowed(String uploadType, String fileName, long fileSize, Identity user, HttpServletRequest request)
	{
		return UploadFileTools.isFileAllowed(uploadType,fileName,fileSize,user,request);
	}

	public static void reflectionLoader(HttpServletRequest request, Identity user, String fileUrl) {
		String uploadFileActionReflectionLoader = Constants.getString("uploadFileActionReflectionLoader");

		if (Tools.isNotEmpty(uploadFileActionReflectionLoader)) {
			String[] reflectionLoaderClasses = Tools.getTokens(uploadFileActionReflectionLoader, "|");

			for (String reflectionLoaderClass : reflectionLoaderClasses) {
				String className = reflectionLoaderClass.substring(0, reflectionLoaderClass.lastIndexOf("."));
				String methodName = reflectionLoaderClass.substring(reflectionLoaderClass.lastIndexOf(".") + 1);

				try {
					Class<?> clazz = Class.forName(className);
				    Method method = clazz.getMethod(methodName, HttpServletRequest.class, user.getClass(), String.class);
				    method.invoke(null, request, user, fileUrl);
				} catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
					Logger.debug(UploadFileAction.class, "ReflectionLoader - " + className + "." + methodName + " exception");
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}
	}

	/**
	 * @deprecated use UploadFileTools.getPageUploadSubDir
	 */
	@Deprecated
	public static String getPageUploadSubDir(int docId, int groupId, String prefix){
		return UploadFileTools.getPageUploadSubDir(docId,groupId,null,prefix);
	}

	/**
	 * @deprecated - use UploadFileTools.getUploadMaxSize
	 */
	@Deprecated
	public static int getUploadMaxSize(Identity user, String type){
		return UploadFileTools.getUploadMaxSize(user,type);
	}

	/**
	 * @deprecated - use UploadFileTools.getUploadFileTypes
	 */
	@Deprecated
	public static String getUploadFileTypes(Identity user, String type){
		return UploadFileTools.getUploadFileTypes(user,type);
	}

	/**
	 * @deprecated - use UploadFileTools.getPageUploadSubDir
	 */
	@Deprecated
	public static String getPageUploadSubDir(int docId, int groupId){
		return UploadFileTools.getPageUploadSubDir(docId,groupId);
	}

}
