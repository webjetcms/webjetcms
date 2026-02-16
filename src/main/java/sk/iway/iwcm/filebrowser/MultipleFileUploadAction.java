package sk.iway.iwcm.filebrowser;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Encoding;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.editor.UploadFileAction;
import sk.iway.iwcm.editor.UploadFileForm;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.VideoConvert;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.metadata.MetadataCleaner;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.upload.DiskMultiPartRequestHandler;
import sk.iway.upload.UploadedFile;

/**
 *  MultipleFileUploadAction2.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff blade $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.4.2010 21:36:17
 *@modified     $Date: 2004/08/16 06:26:11 $
 */


@WebServlet(name = "MultipleFileUploadAction",
		urlPatterns = {"/admin/multiplefileupload.do"}

)
public class MultipleFileUploadAction extends HttpServlet
{

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		execute(request,response); //NOSONAR
	}

	public void execute(
			HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		Encoding.setResponseEnc(request, response, "text/html");

		DiskMultiPartRequestHandler multipartHandler = new DiskMultiPartRequestHandler();
		try
		{
			request = multipartHandler.handleRequest(request);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		Logger.debugAllHeaders(request);
		Logger.debugAllParameters(request);

		PrintWriter out = response.getWriter();
		boolean returnJson = Tools.getBooleanValue(Tools.getParameter(request, "returnJson"), false);
		if (returnJson) {
			response.setContentType("application/json; charset=UTF-8");
		}

		/*
		 * fbrowser - upload suborov v zalozke subory
		 * gallery - upload suborov v galerii
		 */
		String typeOfUpload = "fbrowser";
		if(request.getParameter("type") != null)
		{
			typeOfUpload = request.getParameter("type");
		}

        UserDetails user = UsersDB.getCurrentUser(request);

		boolean ipCorrect = false;

		if (user == null)
		{
            //session a prihlaseny uzivatel sa musi ziskavat cez session holder, kvoli Flash session bugu
            SessionHolder sh = SessionHolder.getInstance();
            String sessionId = request.getParameter("sessionId");
            Logger.debug(MultipleFileUploadAction.class, "sessionId=" + sessionId + " sh=" + sh);
            if (sh == null || Tools.isEmpty(sessionId)) {
				out.println(getMessage(request, false, "Security Error"));
                return ;
            }
            SessionDetails sd = sh.get(sessionId);
            if (sd == null) {
				out.println(getMessage(request, false, "Security Error"));
                return ;
            }
            user = UsersDB.getUser(sd.getLoggedUserId());

            ipCorrect = Tools.getRemoteIP(request).equals(sd.getRemoteAddr());
            //kvoli TB ktorej sa meni IP adresa pocas prihlasenia kvoli dvojitej proxy
            if (ipCorrect == false && Constants.getBoolean("sessionStealingCheck")==false) ipCorrect = true;
        }
        else {
            ipCorrect = true;
        }

		String[] allowUploadGroupIds = Tools.isNotEmpty(Constants.getString("galleryAllowUploadGroupIds")) ? Tools.getTokens(Constants.getString("galleryAllowUploadGroupIds"), ",") : null;
		String dir = Tools.getStringValue(Tools.getParameter(request, "dir"), "");
		if (user == null || user.isAdmin() == false || ipCorrect == false)
		{
			if (isAllowedUploadForUserGroup(user, dir)) {

			}
			else if ("true".equals(request.getAttribute("allowUpload")))
			{

			}
			else if (user != null && "gallery".equals(typeOfUpload) && Tools.containsOneItem(Tools.getTokens(user.getUserGroupsIds(), ","),allowUploadGroupIds))
			{

			}
			else
			{
				Logger.debug(MultipleFileUploadAction.class, "user is not allowed to upload - remoteIp="+Tools.getRemoteIP(request));
				out.println(getMessage(request, false, "Security Error"));
				return ;
			}
		}

		//nastavim userId do RequestBean-u
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (user != null) rb.setUserId(user.getUserId());

		//sprav upload
		UploadedFile formFile;

		try
		{
			//ziskam si adresar z parametru
			if(Tools.isEmpty(dir))
			{
				Logger.debug(MultipleFileUploadAction.class, "dir is empty");
				out.println(getMessage(request, false, "Upload error"));
				return ;
			}

			if (ContextFilter.isRunning(request) && dir.startsWith(request.getContextPath()))
			{
				dir = ContextFilter.removeContextPath(request.getContextPath(), dir);
			}

			if (Tools.isNotEmpty(dir) && GalleryDB.isGalleryFolder(dir)) typeOfUpload = "gallery";

			Logger.debug(MultipleFileUploadAction.class, "Type of upload: "+typeOfUpload);

			String fileNameFallback = null;


			Map<String, UploadedFile> files = multipartHandler.getFileElements();
			Set<Map.Entry<String, UploadedFile>> set = files.entrySet();
			for (Map.Entry<String,UploadedFile> me : set)
			{
				formFile = files.get(me.getKey());
				cleanMetadata(formFile);
				Logger.debug(MultipleFileUploadAction.class, "formFile="+formFile);
				if (formFile != null && formFile.getFileSize() > 0)
				{
					Logger.debug(MultipleFileUploadAction.class, "fileSize="+formFile.getFileSize());

					if("fbrowser".equals(typeOfUpload))
					{
						//kontroluje limity pre velkost a typy suborov
						if(UploadFileAction.isFileAllowed(formFile, new Identity(user), "file", request) == false)
						{
							if(request.getAttribute("permissionDenied") != null)
							{
								if("fileSize".equals(request.getAttribute("permissionDenied")))
								{
									Logger.debug(MultipleFileUploadAction.class, "ERROR: File exceeds");
									out.println(getMessage(request, false, "File exceeds"));
								}
								else if("fileType".equals(request.getAttribute("permissionDenied")))
								{
									Logger.debug(MultipleFileUploadAction.class, "ERROR: Invalid filetype");
									out.println(getMessage(request, false, "Invalid filetype"));
								}
							}
						}
						else
						{
							//retrieve the file name
							String fileName = formFile.getFileName().trim();
							//Logger.println(this,"we have a file name="+fileName+" content type="+file.getContentType());
							Logger.debug(MultipleFileUploadAction.class, "file name="+fileName);

							if (fileName != null && fileName.length() > 1)
							{
								if (dir.startsWith("/files") || dir.startsWith("/images"))
								{
									fileName = DB.internationalToEnglish(fileName);
									fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
								}

								String realPath = null;

								realPath = Tools.getRealPath(dir) + File.separatorChar + fileName;

								Logger.debug(MultipleFileUploadAction.class, "realPath="+realPath);

								if (realPath != null)
								{
									IwcmFile f = new IwcmFile(realPath);

									IwcmFsDB.writeFiletoDest(formFile.getInputStream(), new File(f.getPath()), formFile.getFileSize());

									Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "Multiple file upload, file="+dir+"/"+fileName, -1, -1);

									//ak je treba, aplikujem vodotlac na obrazky
									GalleryDB.applyWatermarkOnUpload(f);

									//zmaz temp subor
									formFile.destroy();
									//ak existuje adresar files, treba indexovat
									if (FileIndexer.isFileIndexerConfigured())
									{
										List<ResultBean> indexedFiles = new ArrayList<>();
										//musim dat do session prihlaseneho uzivatela, kvoli flash session bug
										boolean userInSessionExists = request.getSession().getAttribute(Constants.USER_KEY) != null;
										if (userInSessionExists==false) LogonTools.setUserToSession(request.getSession(), new Identity(user));
										FileIndexerTools.indexFile(dir + "/" + fileName, indexedFiles, request);
										if (userInSessionExists==false) request.getSession().removeAttribute(Constants.USER_KEY);
									}
									//MetadataCleaner.removeMetadata(f);
								}

								out.println(getMessageForFile(request, true, dir, fileName));
								if (fileNameFallback==null) fileNameFallback = fileName;
							}
						}
					}
					if("gallery".equals(typeOfUpload))
					{
						//kontroluje limity pre velkost a typy suborov
						if(UploadFileAction.isFileAllowed(formFile, new Identity(user), "image", request) == false)
						{
							if(request.getAttribute("permissionDenied") != null)
							{
								if("fileSize".equals(request.getAttribute("permissionDenied")))
								{
									Logger.debug(MultipleFileUploadAction.class, "ERROR (gallery): File exceeds");
									out.println(getMessage(request, false, "File exceeds"));
								}
								else if("fileType".equals(request.getAttribute("permissionDenied")))
								{
									Logger.debug(MultipleFileUploadAction.class, "ERROR (gallery): Invalid filetype");
									out.println(getMessage(request, false, "Invalid filetype"));
								}
							}
						}
						else
						{

							//retrieve the file name
							String fileName = formFile.getFileName().trim();
							//Logger.println(this,"we have a file name="+fileName+" content type="+file.getContentType());

							Logger.debug(MultipleFileUploadAction.class, "file name (gallery)="+fileName);

							if (fileName != null && fileName.length() > 1)
							{
								fileName = DB.internationalToEnglish(fileName);
								fileName = DocTools.removeChars(fileName);

								BufferedInputStream buffReader = new BufferedInputStream(formFile.getInputStream());
								String realPath = null;

								Prop prop = Prop.getInstance(Constants.getServletContext(), request);
								Dimension[] dims = GalleryDB.getDimension(dir);

								String fNameLC = fileName.toLowerCase();
								//ak sa jedna o obrazok
								if (fNameLC.endsWith(".jpg") || fNameLC.endsWith(".jpeg") || fNameLC.endsWith(".gif") || fNameLC.endsWith(".png") || VideoConvert.isVideoFile(fNameLC))
								{
									fileName = fNameLC.endsWith(".jpeg")?fileName.substring(0, fileName.lastIndexOf("."))+".jpg":fileName;
									realPath = Tools.getRealPath(dir+"/"+fileName); //NOSONAR

									Logger.debug(MultipleFileUploadAction.class, "realPath="+realPath);

									if (realPath != null)
									{
										IwcmFile f = new IwcmFile(realPath);

										//odstranime metadata este pred dalsim spracovanim suborov v galerii
										//MetadataCleaner.removeMetadata(f);

										IwcmFsDB.writeFiletoDest(formFile.getInputStream(), new File(f.getPath()),formFile.getFileSize());

										Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "Multiple file upload, file="+dir+"/"+fileName, -1, -1);

										//ak je treba, aplikujem vodotlac na obrazky
										GalleryDB.applyWatermarkOnUpload(f);

										//zmaz temp subor
										formFile.destroy();

										if (VideoConvert.isVideoFile(fNameLC))
										{
											UploadFileForm my_form = new UploadFileForm();
											my_form.setBitRate(768);
											my_form.setVideoWidth(Constants.getInt("defaultVideoWidth"));
											my_form.setVideoHeight(Constants.getInt("defaultVideoHeight"));
											my_form.setKeepOriginalVideo(false);

											String fileURL = dir+"/"+fileName;
											fileURL = VideoConvert.convert(my_form, fileURL, request);
										}
										else
										{
											// ak je to povolene, pokusime sa skonvertovat CMYK obrazok na RGB
											ImageTools.convertCmykToRgb(f.getPath());

											//ak je to JPG obrazok, skusime ziskat datum vytvorenia fotografie na zaklade EXIF metadat
											Date dateCreated = GalleryDB.getExifDateOriginal(f);

											//ak existuje zmaz subor o_nazov, lebo by sa to znova updatlo zo stareho
											IwcmFile origFile = new IwcmFile(Tools.getRealPath(dir+"/o_"+fileName));
											if (origFile.exists())
											{
												origFile.delete();
											}
											//resize
											GalleryDB.resizePictureImpl(dims, realPath, out, prop, GalleryDB.getResizeMode(dir));

											if(Tools.isNotEmpty(request.getParameter("item")) && user != null)
												GalleryDB.updateImageItem(-1, request.getParameter("item"), user.getFullName(), dir, fileName, PageLng.getUserLng(request));

											//zapise datum vytvorenia fotografie (ak vieme ziskat)
											if(dateCreated != null)
												GalleryDB.setUploadDateImage(dir, fileName, dateCreated.getTime());
										}
									}
								}
								buffReader.close();
								out.println(getMessageForFile(request, false, dir, fileName));
								if (fileNameFallback==null) fileNameFallback = fileName;
							}
						}
					}
				}
			}

			if (fileNameFallback!=null && "1".equals(request.getParameter("fallback")))
			{
				String type="image";
				if (dir.startsWith("/files")) type="file";

				out.print("<script type='text/javascript'>window.location.href=\"/admin/dir.do?type="+type+"&selectLink="+Tools.URLEncode(fileNameFallback)+"\";</script>");
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			out.println(getMessage(request, false, "Upload error"));
		}

		out.close();
	}

	public static void cleanMetadata(UploadedFile uploadedFile)
	{
//		String fileName = formFile.get
		String metadataRemoverCommand = Constants.getString("metadataRemoverCommand");
		if (Tools.isNotEmpty(metadataRemoverCommand) && uploadedFile != null)
		{
			try
			{
				File tempFile = File.createTempFile("upload", uploadedFile.getFileName());
				IwcmFile tempIwcm = new IwcmFile(tempFile);
				IwcmFsDB.writeFiletoDest(uploadedFile.getInputStream(), tempFile, 0);
				MetadataCleaner.removeMetadata(tempIwcm);
				uploadedFile.setLocalFile(tempIwcm);
			} catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

	}

	private String getMessageForFile(HttpServletRequest request, boolean result, String dir, String fileName) {
		boolean returnJson = Tools.getBooleanValue(Tools.getParameter(request, "returnJson"), false);
		if (!returnJson) {
			return "OK";
		}

		Map<String, Object> map = new HashMap<>();
		map.put("result", result);
		map.put("dir", dir);
		map.put("fileName", fileName);

		return new JSONObject(map).toString();
	}

	private String getMessage(HttpServletRequest request, boolean result, String message) {
		boolean returnJson = Tools.getBooleanValue(Tools.getParameter(request, "returnJson"), false);
		if (!returnJson) {
			return message;
		}

		Map<String, Object> map = new HashMap<>();
		map.put("result", result);
		map.put("message", message);

		return new JSONObject(map).toString();
	}

	private boolean isAllowedUploadForUserGroup(UserDetails user, String dir) {
		List<String> allowed = Tools.getStringListValue(Tools.getTokens(Constants.getString("allowUploadToDirUserGroupId", ""), ","));
		// format url:userGroupId napr. /files/project:25,/files/project2:26
		if (allowed == null || allowed.isEmpty()) {
			return false;
		}

		List<Integer> userGroupIds = UserGroupsDB.getInstance().getUserGroupIdsList(user.getUserGroupsIds(), UserGroupDetails.TYPE_PERMS);
		for (Integer userGroupId : userGroupIds) {
			if (allowed.stream().anyMatch(a ->{
				String[] arr = Tools.getTokens(a, ":");

				if (arr.length == 2) {
					String path = arr[0];
					boolean pathEndsWithStar = path.endsWith("*");

					if (pathEndsWithStar) {
						path = Tools.replace(path, "*", "");
					}

					if (!path.endsWith("/")) {
						path += "/"; //NOSONAR
					}

					boolean dirOk = pathEndsWithStar ? dir.startsWith(path) : dir.equalsIgnoreCase(path);
					int pathUserGroupId = Tools.getIntValue(arr[1], 0);

					return dirOk && pathUserGroupId == userGroupId;
				}

				return false;
			})) {
				Logger.debug(MultipleFileUploadAction.class, String.format("Allowed upload for dir: %s and userGroupId: %d", dir, userGroupId));
				return true;
			}
		}

		return false;
	}
}