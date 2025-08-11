
package cn.bluejoe.elfinder.controller.executors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.editor.UploadFileAction;
import sk.iway.iwcm.editor.UploadFileForm;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.VideoConvert;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.metadata.MetadataCleaner;
import sk.iway.iwcm.users.UsersDB;

@SuppressWarnings("java:S116")
public class UploadCommandExecutor extends AbstractJsonCommandExecutor
{

	/**
	 * Odstranenie adresara z cesty k suboru pri uploade na windows z IE (ie bug)
	 */
	private static String fixFileNameDirPath(String fileName)
	{
		try
		{
			int lomka = fileName.lastIndexOf("/");
			if (lomka > 0) fileName = fileName.substring(lomka+1);
			lomka = fileName.lastIndexOf("\\");
			if (lomka > 0) fileName = fileName.substring(lomka+1);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		fileName = Tools.replace(fileName, "..", ".");

		return fileName;
	}

	private String getFilename(String filepath) {
		int lomka = filepath.lastIndexOf("/");

		if (lomka > -1) {
			return filepath.substring(lomka + 1);
		}

		return filepath;
	}

	private String getDirectory(String filepath)
	{
		int lomka = filepath.lastIndexOf("/");

		if (lomka > 0) {
			return filepath.substring(0, lomka);
		}

		return "";
	}

	private FsItemEx fileItemExFromDirectory(FsItemEx itemEx, String directory) throws IOException
	{
		String[] directoriesArray = Tools.getTokens(directory, "/");
		return new FsItemEx(itemEx, directoriesArray[0]);
	}

	// large file will be splitted into many parts
	class Part
	{
		long _start;
		long _size;
		FileItem _content;

		public Part(long start, long size, FileItem fileItem)
		{
			super();
			this._start = start;
			this._size = size;
			this._content = fileItem;
		}
	}

	// a large file with many parts
	static class Parts
	{
		public static synchronized Parts getOrCreate(
				HttpServletRequest request, String chunkId, String fileName,
				long total, long totalSize)
		{
			//chunkId is not an unique number for files uploaded in one upload form
			String key = String.format("chunk_%s_%s", chunkId, fileName);
			// stores chunks in application context
			Parts parts = (Parts) request.getServletContext().getAttribute(key);

			if (parts == null)
			{
				parts = new Parts(chunkId, fileName, total, totalSize);
				request.getServletContext().setAttribute(key, parts);
			}

			return parts;
		}

		private String _chunkId;
		// number of parts
		private long _numberOfParts;
		private long _totalSize;

		private String _fileName;

		// all chunks
		Map<Long, Part> _parts = new HashMap<>();

		public Parts(String chunkId, String fileName, long numberOfParts,
				long totalSize)
		{
			_chunkId = chunkId;
			_fileName = fileName;
			_numberOfParts = numberOfParts;
			_totalSize = totalSize;
		}

		public synchronized void addPart(long partIndex, Part part)
		{
			_parts.put(partIndex, part);
		}

		public boolean isReady()
		{
			return _parts.size() == _numberOfParts;
		}

		public InputStream openInputStream() throws IOException //NOSONAR
		{
			return new InputStream()
			{
				long partIndex = 0;
				Part part = _parts.get(partIndex);
				InputStream is = part._content.getInputStream();

				@Override
				public int read() throws IOException
				{
					while (true)
					{
						// current part is not read completely
						int c = is.read();
						if (c != -1)
						{
							return c;
						}

						// next part?
						if (partIndex == _numberOfParts - 1)
						{
							is.close();
							return -1;
						}

						part = _parts.get(++partIndex);
						is.close();
						is = part._content.getInputStream();
					}
				}

				@Override
				public int read(byte[] buffer, int start, int bufferSize) throws IOException
				{
					while (true)
					{
						// current part is not read completely
						int c = is.read(buffer, start, bufferSize);
						if (c != -1)
						{
							return c;
						}

						// next part?
						if (partIndex == _numberOfParts - 1)
						{
							is.close();
							return -1;
						}

						part = _parts.get(++partIndex);
						is.close();
						is = part._content.getInputStream();
					}
				}
			};
		}

		public void checkParts() throws IOException
		{
			long totalSize = 0;

			for (long i = 0; i < _numberOfParts; i++)
			{
				Part part = _parts.get(i);
				totalSize += part._size;
			}

			if (totalSize != _totalSize)
				throw new IOException(String.format(
						"invalid file size: excepted %d, but is %d",
						_totalSize, totalSize));
		}

		public void removeFromApplicationContext(HttpServletRequest request)
		{
			String key = String.format("chunk_%s_%s", _chunkId, _fileName);
			request.getServletContext().removeAttribute(key);

			for (long i = 0; i < _numberOfParts; i++)
			{
				try {
					_parts.get(i)._content.delete();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	interface FileWriter
	{
		FsItemEx createAndSave(String fileName, InputStream is, long size)
				throws IOException;
	}

	@SuppressWarnings("java:S1075")
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		@SuppressWarnings("unchecked")
		Map<String,FileItem> filesMap = (Map<String,FileItem>) request.getAttribute("MultipartWrapper.files");
		//List<FileItem> files = new ArrayList<FileItem>(filesMap.values());
		final List<FsItemEx> added = new ArrayList<>();

		String target = request.getParameter("target");
		String[] renames = request.getParameterValues("renames[]");

		final FsItemEx dir = super.findItem(fsService, target);
		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();

		final List<String> notUploaded = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<String> notUploadedSession = (List<String>) request.getAttribute("MultipartWrapper.notUploaded");
		if (notUploadedSession != null) notUploaded.addAll(notUploadedSession) ;

		if (user != null && UsersDB.isFolderWritable(user.getWritableFolders(), dir.getPath()) && filesMap != null)
		{

			if (renames != null && renames.length > 0) {
				String path = dir.getPath();
				for (String rename : renames) {
					File file = new File(Tools.getRealPath(path + "/" + rename));

					if (file.exists()) {
						int i = 1;

						File fileTo = null;
						do {
							String filename = rename.contains(".") ? rename.substring(0, rename.lastIndexOf(".")) + "-" + (i++) + rename.substring(rename.lastIndexOf("."), rename.length()) : rename + "-" + (i++);
							fileTo = new File(Tools.getRealPath(path + "/" + filename));
						}
						while (fileTo.exists());
						file.renameTo(fileTo); //NOSONAR

						FsItemEx fsItem = new FsItemEx(dir, fileTo.getName());
						added.add(fsItem);
					}
				}
			}

			for (Entry<String, FileItem> entry : filesMap.entrySet())
			{
				String filepath = entry.getKey();
				FileItem fi = entry.getValue();
				String directory = getDirectory(filepath);
				String fileName = getFilename(filepath);
				fileName = fixFileNameDirPath(fileName);

				//wj8 - elfinder - kontrola prav pre typy suborov (#20592)
				String uploadType = "file";
				if(dir.getPath().startsWith("/images")) uploadType = "image";

				if (dir.getPath().startsWith("/files") || dir.getPath().startsWith("/images") || dir.getPath().startsWith("/shared"))
				{
					fileName = DB.internationalToEnglish(fileName);
					fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();

					directory = DB.internationalToEnglish(directory);
					directory = DocTools.removeCharsDir(directory, true).toLowerCase();
				}

				String realPathDir = Tools.getRealPath(dir.getPath());
				File file = new File(realPathDir);

				// is not update
				if (!file.exists() || !file.isFile()) {
					realPathDir = Tools.getRealPath(dir.getPath() + directory + "/" + fileName);
					file = new File(realPathDir);
				}

				final File newFile = file;
				//fi.write(newFile);
				final String uploadTypeFinal = uploadType;
				final String directoryFinal = directory;

				FileWriter fw = new FileWriter()
				{
					@Override
					public FsItemEx createAndSave(String fileName, InputStream is, long size)
							throws IOException
					{
						if (fileName.startsWith("/")) fileName = fileName.substring(1);
						fileName = DB.internationalToEnglish(fileName);
						fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
						fileName = Tools.replace(fileName, "/", ""+File.separatorChar);

						boolean isAllowedForUpload = UploadFileTools.isFileAllowed(uploadTypeFinal, fileName, size, user, request);

						if (isAllowedForUpload)
						{
							// fis.getName() returns full path such as 'C:\temp\abc.txt' in
							// IE10
							// while returns 'abc.txt' in Chrome
							// see
							// https://github.com/bluejoe2008/elfinder-2.x-servlet/issues/22
							//java.nio.file.Path p = java.nio.file.Paths.get(fileName);
							FsItemEx newFileEx = new FsItemEx(dir, fileName);

							/*
							 * String fileName = fis.getName(); FsItemEx newFile = new
							 * FsItemEx(dir, fileName);
							 */
							//newFileEx.createFile();
							//newFile.writeStream(is);

							File fileToSave = new File(newFile.getParentFile()+File.separator+fileName);
							IwcmFsDB.writeFiletoDest(is, fileToSave, (int)size, true);

							Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload, path="+dir.getPath()+directoryFinal+"/"+fileName, -1, -1);

							//if (filter.accepts(newFile))
								added.add(newFileEx);

							return newFileEx;
						}
						else
						{
							notUploaded.add(fileName);
							return null;
						}
					}
				};

				// chunked upload
				if (request.getParameter("cid") != null)
				{
					processChunkUpload(request, filesMap, fw);
					if (added.size() > 0)
					{
						FsItemEx newFileEx = added.get(added.size()-1);
						fileName = newFileEx.getName();
					}
				}
				else
				{
					boolean isAllowedForUpload = UploadFileTools.isFileAllowed(uploadType, fileName, fi.getSize(), user, request);

					if (isAllowedForUpload)
					{
						IwcmFsDB.writeFiletoDest(fi.getInputStream(), newFile, (int)fi.getSize(), true);

						Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "elfinder upload, path="+dir.getPath()+directory+"/"+fileName, -1, -1);
					}
					else
					{
						notUploaded.add(fileName);
					}
				}

				IwcmFile newFileIwcm = new IwcmFile(newFile);
				if (newFileIwcm.exists()) {
					fi.delete();
					FsItemEx fsItem = new FsItemEx(dir, fileName);
					if (!dir.isFolder()) {
						fsItem = dir;
					}

					if (Tools.isEmpty(directory)) {
						added.add(fsItem);
					}
					else {
						FsItemEx itemDirectory = fileItemExFromDirectory(dir, directory);
						if (!added.contains(itemDirectory)) {
							added.add(itemDirectory);
						}
					}
				}

				//POZOR: vo WJ9 je toto presunute do UploadService, pozor ale na cast VideoConvert, kde sa pracuje s FsItem kvoli premenovaniu, to tu treba zachovat

				//kvoli chunked uploadu musime zrekonstruovat nanovo
				newFileIwcm = new IwcmFile(Tools.getRealPath(dir.getPath()+directory+"/"+fileName));

				//vycisti metadata
				MetadataCleaner.removeMetadata(newFileIwcm);

				//ak je treba, aplikujem vodotlac na obrazky
				GalleryDB.applyWatermarkOnUpload(newFileIwcm);

				// ak je to povolene, pokusime sa skonvertovat CMYK obrazok na RGB
				ImageTools.convertCmykToRgb(newFileIwcm.getAbsolutePath());

				//ak je to JPG obrazok, skusime ziskat datum vytvorenia fotografie na zaklade EXIF metadat
				Date dateCreated = GalleryDB.getExifDateOriginal(newFileIwcm);

				if (VideoConvert.isVideoFile(fileName))
				{
					if (fileName.endsWith("."+Constants.getString("defaultVideoFormat"))==false)
					{
						//nie je to mp4, treba skonvertovat
						UploadFileForm my_form = new UploadFileForm();
						my_form.setBitRate(Constants.getInt("defaultVideoBitrate"));
						my_form.setVideoWidth(Constants.getInt("defaultVideoWidth"));
						my_form.setVideoHeight(Constants.getInt("defaultVideoHeight"));
						my_form.setKeepOriginalVideo(false);

						//zmaz povodny added mpg subor
						for (FsItemEx item : added)
						{
							if (item.getPath().endsWith(fileName))
							{
								added.remove(item);
								break;
							}
						}

						String fileURL = VideoConvert.convert(my_form, newFileIwcm.getVirtualPath(), request);
						Logger.debug(this.getClass(), "Converted video: "+fileURL);
						if (Tools.isNotEmpty(fileURL) && fileURL.lastIndexOf("/")>1)
						{
							String videoFileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
							added.add(new FsItemEx(dir, videoFileName));
							added.add(new FsItemEx(dir, Tools.replace(videoFileName, "."+Constants.getString("defaultVideoFormat"), ".jpg")));
						}
					}
					else
					{
						//pre mp4 vytvorime len screenshot
						String image = VideoConvert.makeScreenshot(newFileIwcm.getAbsolutePath(), null);
						if (image != null)
                        {
                            String imageFilename = new IwcmFile(image).getName();

                            if (Tools.isEmpty(directory))
                            {
                                added.add(new FsItemEx(dir, imageFilename));
                            }
                        }
					}
				}

				if (FileTools.isImage(newFileIwcm.getName())) {
					if (GalleryDB.isGalleryFolder(dir.getPath()+directory)) {
						//we must replace o_ file because it will be used in resize process instead of new file
						IwcmFile orig = new IwcmFile(Tools.getRealPath(dir.getPath()+directory+"/o_"+fileName));
						if (orig.exists()) {
							FileTools.copyFile(newFileIwcm, orig);
						}

						GalleryDB.resizePicture(newFileIwcm.getAbsolutePath(), dir.getPath()+directory);
						added.add(new FsItemEx(dir, "s_"+fileName));
						added.add(new FsItemEx(dir, "o_"+fileName));
					} else if (Constants.getBoolean("imageAlwaysCreateGalleryBean")) {
						GalleryDB.setImage(dir.getPath()+directory, fileName);
					}

					//zapise datum vytvorenia fotografie (ak vieme ziskat)
					if (dateCreated != null) {
						GalleryDB.setUploadDateImage(dir.getPath()+directory, fileName, dateCreated.getTime());
					}
				}

				//ak existuje adresar files, treba indexovat
				if (FileIndexer.isFileIndexerConfigured())
				{
					List<ResultBean> indexedFiles = new ArrayList<>();
					FileIndexerTools.indexFile(dir.getPath() + directory + "/" + fileName, indexedFiles, request);
				}

				UploadFileAction.reflectionLoader(request, user, dir.getPath() + directory + "/" + fileName);
			}
		}
		else
		{
			json.put("error", prop.getText("components.elfinder.commands.upload.error"));
		}
		json.put("added", files2JsonArray(request, added));


		if (notUploaded != null && notUploaded.size()>0)
		{
			String fileNames = notUploaded.toString();
			json.put("error", prop.getText("admin.dragdropupload.forbidden", fileNames));
		}
	}

	private void processChunkUpload(HttpServletRequest request, Map<String,FileItem> filesMap, FileWriter fw)
				throws NumberFormatException, IOException
		{
			// cid : unique id of chunked uploading file
			String cid = request.getParameter("cid");
			// solr-5.5.2.tgz.48_65.part
			String chunk = request.getParameter("chunk");

			// 100270176,2088962,136813192
			String range = request.getParameter("range");
			String[] tokens = range.split(",");

			Logger.debug(UploadCommandExecutor.class, "chunk="+chunk+" range="+range+" cid="+cid);

			Matcher m = Pattern.compile("(.*)\\.([0-9]+)\\_([0-9]+)\\.part") //NOSONAR
					.matcher(chunk);

			if (m.find())
			{
				String fileName = m.group(1);
				String uploadPath = request.getParameter("upload_path[]");
				if (Tools.isNotEmpty(uploadPath) && uploadPath.startsWith("/"))
				{
					fileName = uploadPath;
				}
				long index = Long.parseLong(m.group(2));
				long total = Long.parseLong(m.group(3));

				Parts parts = Parts.getOrCreate(request, cid, fileName, total + 1,
						Long.parseLong(tokens[2]));

				long start = Long.parseLong(tokens[0]);
				long size = Long.parseLong(tokens[1]);

				Logger.debug(UploadCommandExecutor.class, String.format("uploaded part(%d/%d) of file: %s",
						index, total, fileName));

				parts.addPart(index, new Part(start, size, filesMap.get("upload[]")));
				Logger.debug(UploadCommandExecutor.class, String.format(">>>>%d", parts._parts.size()));
				if (parts.isReady())
				{
					parts.checkParts();

					Logger.debug(UploadCommandExecutor.class, String.format("file is uploadded completely: %s",
							fileName));

					long totalSize = 0;
					for (long i = 0; i < parts._numberOfParts; i++)
					{
						totalSize += parts._parts.get(i)._content.getSize();
					}

					fw.createAndSave(fileName, parts.openInputStream(), totalSize);

					// remove from application context
					parts.removeFromApplicationContext(request);
				}
			}
		}
}
