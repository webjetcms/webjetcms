package cn.bluejoe.elfinder.controller.executors;

import static sk.iway.iwcm.system.elfinder.FsService.isFileTypeAllowed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload2.core.FileItem;
import org.json.JSONArray;
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
import sk.iway.iwcm.common.FileBrowserTools;
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
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.system.metadata.MetadataCleaner;
import sk.iway.iwcm.users.UsersDB;

/** Handles elFinder file and chunk uploads, including permission checks and conflict backups. */
@SuppressWarnings("java:S116")
public class UploadCommandExecutor extends AbstractJsonCommandExecutor
{
	private static final Pattern CHUNK_NAME = Pattern.compile("(.*)\\.([0-9]+)_([0-9]+)\\.part");

	/** Returns the final upload path, rather than the temporary chunk filename. */
	private String getChunkFileName(HttpServletRequest request, Matcher chunk) {
		String uploadPath = request.getParameter("upload_path[]");
		return uploadPath != null && uploadPath.startsWith("/") ? uploadPath : chunk.group(1);
	}

	/**
	 * Filters uploads by the filename extensions supported by the current picker mode.
	 * Checks normalized filenames and, when replacing a file, the existing target's type.
	 * Chunk uploads are checked using their final path, including {@code upload_path[]},
	 * rather than the temporary {@code .part} name. Rejected temporary files and retained
	 * chunks are deleted before conflict renames or destination writes can occur.
	 * Permissions and size limits are checked separately by the upload flow.
	 *
	 * @param files parsed uploads keyed by their path or multipart field name
	 * @param target destination folder or existing file; also determines the picker mode
	 * @param request request containing chunk metadata and the final upload path
	 * @param user user whose filename normalization rules apply
	 * @param rejectedTypes receives rejected filenames for the error or warning response
	 * @return a new map containing accepted uploads with their original keys and file items
	 * @throws IOException if target access or deletion of a rejected temporary file fails
	 */
	private Map<String, FileItem<?>> filterUploadTypes(Map<String, FileItem<?>> files, FsItemEx target,
			HttpServletRequest request, Identity user, Set<String> rejectedTypes) throws IOException {
		Map<String, FileItem<?>> allowed = new LinkedHashMap<>();
		int selectedType = target.getFsServiceSelectedType();
		Matcher chunk = CHUNK_NAME.matcher(Tools.getStringValue(request.getParameter("chunk"), ""));
		String chunkFileName = request.getParameter("cid") != null && chunk.matches() ? getChunkFileName(request, chunk) : null;
		boolean targetAllowed = target.isFolder() || isFileTypeAllowed(selectedType, target.getPath());
		for (Entry<String, FileItem<?>> entry : files.entrySet()) {
			String name = chunkFileName == null ? entry.getKey() : chunkFileName;
			String fileName = IwcmFsVolume.removeSpecialChars(fixFileNameDirPath(getFilename(name)), target, user);
			if (targetAllowed && isFileTypeAllowed(selectedType, fileName)) {
				allowed.put(entry.getKey(), entry.getValue());
				continue;
			}
			rejectedTypes.add(name);
			entry.getValue().delete();
			if (chunkFileName != null) {
				String key = String.format("chunk_%s_%s", request.getParameter("cid"), chunkFileName);
				Parts parts = (Parts) request.getServletContext().getAttribute(key);
				if (parts != null) parts.removeFromApplicationContext(request);
			}
		}
		return allowed;
	}

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

	private FsItemEx getDestination(FsItemEx parent, String directory, String fileName) throws IOException
	{
		String relativePath = Path.of(directory, fileName).normalize().toString().replace(File.separatorChar, '/');
		while (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
		return new FsItemEx(parent, relativePath);
	}

	private boolean isWritable(FsItemEx item) throws IOException
	{
		String path = item.getPath();
		// An empty volume-relative path denotes the root; null denotes an invalid path.
		return path != null && item.isWritable(item);
	}

	// large file will be splitted into many parts
	class Part
	{
		long _start;
		long _size;
		FileItem<?> _content;

		public Part(long start, long size, FileItem<?> fileItem)
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

			for (Part part : _parts.values())
			{
				try {
					part._content.delete();
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

	/**
	 * Processes parsed uploads and writes elFinder results, errors and warnings to the response object.
	 * Ordinary and chunked updates retain the selected file's path, regardless of the uploaded filename.
	 *
	 * <p>Requested conflict backups are copied before any replacement. If a backup is
	 * rejected or fails, the upload batch and retained chunks are discarded; originals
	 * and any completed backups remain available.</p>
	 *
	 * @param fsService filesystem service used to resolve the requested target
	 * @param request request containing parsed file items, upload paths and optional chunk or backup parameters
	 * @param servletContext context supplied by the dispatcher; unused by this executor
	 * @param json response object receiving added items and any upload errors or warnings
	 * @throws Exception if target resolution, upload processing or response construction fails
	 */
	@SuppressWarnings("java:S1075")
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		@SuppressWarnings("unchecked")
		Map<String,FileItem<?>> filesMap = (Map<String,FileItem<?>>) request.getAttribute("MultipartWrapper.files");
		//List<FileItem> files = new ArrayList<FileItem>(filesMap.values());
		final List<FsItemEx> added = new ArrayList<>();

		String target = request.getParameter("target");
		String[] renames = request.getParameterValues("renames[]");

		final FsItemEx dir = super.findItem(fsService, target);
		Prop prop = Prop.getInstance(request);
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();

		final List<String> notUploaded = new ArrayList<>();
		final Set<String> rejectedTypes = new LinkedHashSet<>();
		@SuppressWarnings("unchecked")
		List<String> notUploadedSession = (List<String>) request.getAttribute("MultipartWrapper.notUploaded");
		if (notUploadedSession != null) notUploaded.addAll(notUploadedSession) ;

		if (user != null && isWritable(dir) && UsersDB.isFolderWritable(user.getWritableFolders(), dir.getPath()) && filesMap != null)
		{
			filesMap = filterUploadTypes(filesMap, dir, request, user, rejectedTypes);

			boolean backupFailed = false;
			if (renames != null && (rejectedTypes.isEmpty() || filesMap.isEmpty() == false)) {
				for (String rename : renames) {
					if (FileBrowserTools.hasForbiddenSymbol(rename)) {
						notUploaded.add(rename);
						backupFailed = true;
						continue;
					}
					if (isFileTypeAllowed(dir.getFsServiceSelectedType(), rename) == false) {
						rejectedTypes.add(rename);
						backupFailed = true;
						continue;
					}

					try {
						if (backupExistingFile(dir, rename, added)) continue;
					} catch (IOException | UncheckedIOException ex) {
						Logger.error(ex);
					}
					notUploaded.add(rename);
					backupFailed = true;
				}
			}
			if (backupFailed) {
				discardUploads(request, filesMap);
				filesMap = Map.of();
			}

			for (Entry<String, FileItem<?>> entry : filesMap.entrySet())
			{
				String filepath = entry.getKey();
				FileItem<?> fi = entry.getValue();
				String directory = getDirectory(filepath);
				String fileName = getFilename(filepath);
				fileName = fixFileNameDirPath(fileName);

				//wj8 - elfinder - kontrola prav pre typy suborov (#20592)
				String uploadType = "file";
				if(dir.getPath().startsWith("/images")) uploadType = "image";

				//
				fileName = IwcmFsVolume.removeSpecialChars(fileName, dir, user);
				directory = IwcmFsVolume.removeSpecialChars(directory, dir, user);

				String realPathDir = Tools.getRealPath(dir.getPath());
				File file = new File(realPathDir);
				final boolean updatingFile = new IwcmFile(file).isFile();
				FsItemEx destinationItem = dir;

				// is not update
				if (!updatingFile) {
					realPathDir = Tools.getRealPath(dir.getPath() + directory + "/" + fileName);
					file = new File(realPathDir);
					destinationItem = getDestination(dir, directory, fileName);
				}

				if (isWritable(destinationItem)==false) {
					notUploaded.add(filepath);
					continue;
				}
				final FsItemEx uploadDestinationParent = destinationItem.getParent();
				final FsItemEx uploadDirectory = updatingFile ? dir.getParent() : dir;

				final File newFile = file;
				//fi.write(newFile);
				final String uploadTypeFinal = uploadType;

				FileWriter fw = new FileWriter()
				{
					/**
					 * Saves assembled chunks after checking write permissions and upload restrictions.
					 *
					 * @param fileName filename reconstructed from the chunk request
					 * @param is stream containing the assembled upload
					 * @param size total upload size in bytes
					 * @return saved filesystem item, or null if permission or upload checks reject it
					 * @throws IOException if target resolution or writing fails
					 */
					@Override
					public FsItemEx createAndSave(String fileName, InputStream is, long size)
							throws IOException
					{
						if (fileName.startsWith("/")) fileName = fileName.substring(1);
						fileName = DB.internationalToEnglish(fileName);
						fileName = DocTools.removeCharsDir(fileName, true).toLowerCase();
						fileName = Tools.replace(fileName, "/", ""+File.separatorChar);

						FsItemEx newFileEx = getDestination(uploadDestinationParent, "", fileName);
						if (isWritable(newFileEx)==false) {
							notUploaded.add(fileName);
							return null;
						}
						if (updatingFile) {
							fileName = newFile.getName();
							newFileEx = dir;
						}

						boolean isAllowedForUpload = UploadFileTools.isFileAllowed(uploadTypeFinal, fileName, size, user, request);

						if (isAllowedForUpload)
						{
							// fis.getName() returns full path such as 'C:\temp\abc.txt' in
							// IE10
							// while returns 'abc.txt' in Chrome
							// see
							// https://github.com/bluejoe2008/elfinder-2.x-servlet/issues/22
							//java.nio.file.Path p = java.nio.file.Paths.get(fileName);
							/*
							 * String fileName = fis.getName(); FsItemEx newFile = new
							 * FsItemEx(dir, fileName);
							 */
							//newFileEx.createFile();
							//newFile.writeStream(is);

							File fileToSave = new File(newFile.getParentFile()+File.separator+fileName);
							IwcmFsDB.writeFiletoDest(is, fileToSave, (int)size, true);

							Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "File upload, path="+new IwcmFile(fileToSave).getVirtualPath(), -1, -1);

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
				boolean fileUploaded = false;
				if (request.getParameter("cid") != null)
				{
					String chunkedFileName = processChunkUpload(request, filesMap, fw);
					if (Tools.isNotEmpty(chunkedFileName))
					{
						fileUploaded = true;
						directory = getDirectory(chunkedFileName);
						fileName = getFilename(chunkedFileName);
					}
				}
				else
				{
					boolean isAllowedForUpload = UploadFileTools.isFileAllowed(uploadType, fileName, fi.getSize(), user, request);

					if (isAllowedForUpload)
					{
						IwcmFsDB.writeFiletoDest(fi.getInputStream(), newFile, (int)fi.getSize(), true);

						Adminlog.add(Adminlog.TYPE_FILE_UPLOAD, "elfinder upload, path="+new IwcmFile(newFile).getVirtualPath(), -1, -1);
						fileUploaded = true;
					}
					else
					{
						notUploaded.add(fileName);
					}
				}

				if (fileUploaded) {
					if (updatingFile) {
						directory = "";
						fileName = newFile.getName();
					}

					IwcmFile newFileIwcm = new IwcmFile(newFile);
					if (newFileIwcm.exists()) {
						fi.delete();
						FsItemEx fsItem = updatingFile ? dir : new FsItemEx(dir, fileName);

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
					newFileIwcm = updatingFile ? new IwcmFile(newFile) : new IwcmFile(Tools.getRealPath(dir.getPath()+directory+"/"+fileName));

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

							String fileURL = VideoConvert.convert(my_form, newFileIwcm.getVirtualPath(), request);
							Logger.debug(this.getClass(), "Converted video: "+fileURL);
							if (Tools.isNotEmpty(fileURL) && fileURL.lastIndexOf("/")>1)
							{
								String originalPath = newFileIwcm.getVirtualPath();
								for (var iterator = added.iterator(); iterator.hasNext();) {
									if (originalPath.equals(iterator.next().getPath())) iterator.remove();
								}
								String videoFileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
								added.add(new FsItemEx(uploadDirectory, directory, videoFileName));
								added.add(new FsItemEx(uploadDirectory, directory, Tools.replace(videoFileName, "."+Constants.getString("defaultVideoFormat"), ".jpg")));
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
									added.add(new FsItemEx(uploadDirectory, imageFilename));
								}
							}
						}
					}

					List<String> galleryAdded = new ArrayList<>();
					int result = UploadFileTools.handleGallery(newFileIwcm, dateCreated, galleryAdded);
					if (result != 0) {
						json.put("warning", Prop.getInstance(request).getText(ImageTools.getErrorKey(result)));
					}
					for (String s : galleryAdded) {
						FsItemEx fsItem = new FsItemEx(uploadDirectory, directory, s);
						added.add(fsItem);
					}

					//ak existuje adresar files, treba indexovat
					if (FileIndexer.isFileIndexerConfigured())
					{
						List<ResultBean> indexedFiles = new ArrayList<>();
						FileIndexerTools.indexFile(newFileIwcm.getVirtualPath(), indexedFiles, request);
					}

					UploadFileAction.reflectionLoader(request, user, newFileIwcm.getVirtualPath());
				}
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
		if (rejectedTypes.isEmpty() == false) {
			JSONArray errors = new JSONArray();
			rejectedTypes.forEach(name -> errors.put("errUploadFile").put(name).put("errUploadMime"));
			if (json.has("error")) errors.put("$1").put(json.remove("error"));
			if (json.has("warning")) errors.put("$1").put(json.remove("warning"));
			json.put(added.isEmpty() ? "error" : "warning", errors);
		}
	}

	/**
	 * Copies an existing upload target to the first free numbered name without removing the original.
	 *
	 * @param directory folder containing the target and its backup, resolved through CMS storage
	 * @param name relative target name already checked for forbidden symbols and allowed file type
	 * @param added receives the backup item only after a successful copy
	 * @return {@code true} if the source is absent or backed up; {@code false} if write access or copying fails
	 * @throws IOException if filesystem item resolution or permission checks fail
	 * @throws UncheckedIOException if S3 metadata cannot be read
	 */
	private boolean backupExistingFile(FsItemEx directory, String name, List<FsItemEx> added) throws IOException {
		IwcmFile source = new IwcmFile(Tools.getRealPath(directory.getPath() + "/" + name));
		if (source.exists() == false) return true;
		if (isWritable(getDestination(directory, "", name)) == false) return false;

		int number = 1;
		int dot = name.lastIndexOf('.');
		IwcmFile destination;
		String backupName;
		do {
			backupName = dot < 0 ? name + "-" + number : name.substring(0, dot) + "-" + number + name.substring(dot);
			number++;
			destination = new IwcmFile(Tools.getRealPath(directory.getPath() + "/" + backupName));
		} while (destination.exists());

		FsItemEx backupItem = getDestination(directory, "", backupName);
		if (isWritable(backupItem) == false || FileTools.copyTree(source, destination) == false) return false;
		added.add(backupItem);
		return true;
	}

	/**
	 * Deletes rejected upload items and removes their retained chunk set after a backup failure.
	 * Individual deletion failures do not stop cleanup of the remaining items.
	 *
	 * @param request request identifying the chunk set in the servlet context, if present
	 * @param files accepted multipart items whose upload has been cancelled
	 */
	private void discardUploads(HttpServletRequest request, Map<String, FileItem<?>> files) {
		for (FileItem<?> upload : files.values()) {
			try { upload.delete(); } catch (IOException ex) { Logger.error(ex); }
		}
		Matcher chunk = CHUNK_NAME.matcher(Tools.getStringValue(request.getParameter("chunk"), ""));
		if (request.getParameter("cid") != null && chunk.matches()) {
			String key = String.format("chunk_%s_%s", request.getParameter("cid"), getChunkFileName(request, chunk));
			Parts parts = (Parts) request.getServletContext().getAttribute(key);
			if (parts != null) parts.removeFromApplicationContext(request);
		}
	}

	private String processChunkUpload(HttpServletRequest request, Map<String,FileItem<?>> filesMap, FileWriter fw)
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

			Matcher m = CHUNK_NAME.matcher(chunk);

			if (m.matches())
			{
				String fileName = getChunkFileName(request, m);
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

					FsItemEx uploadedFile = fw.createAndSave(fileName, parts.openInputStream(), totalSize);

					// remove from application context
					parts.removeFromApplicationContext(request);
					if (uploadedFile != null) return fileName;
				}
			}
			return null;
		}
}
