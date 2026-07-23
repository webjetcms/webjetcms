package sk.iway.iwcm.gallery;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.components.gallery.GalleryService;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.stripes.BindPageParams;
import sk.iway.iwcm.system.stripes.PageParamOnly;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.utils.Pair;

@BindPageParams
public class GalleryActionBean extends WebJETActionBean
{
	@PageParamOnly
	String perexGroup;

	@PageParamOnly
	String dir = "/images/gallery";

	@PageParamOnly
	boolean recursive;

	@PageParamOnly
	String orderBy = "title";

	@PageParamOnly
	String orderDirection = "asc";

	@PageParamOnly
	int itemsOnPage;

	@PageParamOnly
	String style = "photoSwipe";

	private static final String PIXABAY = "pixabay.com";

	List<GalleryBean> photoList;
	int itemsCount;

	private String img;
	private String fileName;
	private int width;
	private int height;
	private String virtualPath;

	@PageParamOnly
	private boolean shortDescription;

	@PageParamOnly
	private boolean longDescription;

	@PageParamOnly
	private boolean author;

	@PageParamOnly
	private int imagesInRow = -1;

	@PageParamOnly
	private boolean thumbsShortDescription;

	@DefaultHandler
	public Resolution defaultEvent()
	{
		String lng = PageLng.getUserLng(getRequest());
		photoList = GalleryDB.getImages(dir, recursive, lng, perexGroup, orderBy, orderDirection, getRequest());

		int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);
		int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), -1);

		String uploadSubdir = UploadFileTools.getPageUploadSubDir(docId, groupId, null, "/images/gallery");
		IwcmFile uploadDirFile = new IwcmFile(Tools.getRealPath(uploadSubdir));
		if ("/images/gallery".equals(dir))
		{
			if (uploadDirFile.exists()==false)
			{
				boolean created = uploadDirFile.mkdirs();
				if (created) dir = uploadSubdir;
			}
			else
			{
				dir = uploadSubdir;
			}
		}


		return new ForwardResolution(RESOLUTION_CONTINUE);
	}

	public String getPerexGroup()
	{
		return perexGroup;
	}

	public void setPerexGroup(String perexGroup)
	{
		this.perexGroup = perexGroup;
	}

	public String getDir()
	{
		return dir;
	}

	public void setDir(String dir)
	{
		this.dir = dir;
	}

	public boolean isRecursive()
	{
		return recursive;
	}

	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

	public String getOrderBy()
	{
		return orderBy;
	}

	public void setOrderBy(String orderBy)
	{
		this.orderBy = orderBy;
	}

	public String getOrderDirection()
	{
		return orderDirection;
	}

	public void setOrderDirection(String orderDirection)
	{
		this.orderDirection = orderDirection;
	}

	public List<GalleryBean> getPhotoList()
	{
		return photoList;
	}

	public void setPhotoList(List<GalleryBean> photoList)
	{
		this.photoList = photoList;
	}

	public boolean getPagination()
	{
		return itemsOnPage > 0;
	}

	public int getItemsCount()
	{
		if (photoList != null) {
			return photoList.size();
		}
		return itemsCount;
	}

	public void setItemsCount(int itemsCount)
	{
		this.itemsCount = itemsCount;
	}

	public int getItemsOnPage()
	{
		return itemsOnPage;
	}

	public void setItemsOnPage(int itemsOnPage)
	{
		this.itemsOnPage = itemsOnPage;
	}

	public String getStyle()
	{
		return style;
	}

	public void setStyle(String style)
	{
		this.style = style;
	}

	public List<Pair<String, String>> getStyles()
	{
		Prop prop = Prop.getInstance(getRequest());
		List<Pair<String, String>> result = new LinkedList<>();

		result.add(new Pair<>("prettyPhoto", prop.getText("components.gallery.visual_style.prettyPhoto")));
		result.add(new Pair<>("photoSwipe", prop.getText("components.gallery.visual_style.photoSwipe")));

		//preskumaj adresar ci tam nieco nie je
		IwcmFile[] files = new IwcmFile(Tools.getRealPath("/components/" + Constants.getInstallName() + "/gallery/")).listFiles();
		for (IwcmFile f : files)
		{
			if (f.getName().startsWith("gallery-")==false) continue;
			if (f.getName().contains("-prettyPhoto.jsp") || f.getName().contains("-photoSwipe.jsp")) continue;

			try
			{
				String name = f.getName().substring("gallery-".length(), f.getName().length()-4);
				addPair(name, result, prop);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		//over ci je tam ten co je zadany
		if (Tools.isNotEmpty(getStyle()))
		{
			boolean found = false;
			for (Pair<String, String> pair : result)
			{
				if (pair.first.equals(getStyle())) found = true;
			}
			if (found == false)
			{
				addPair(getStyle(), result, prop);
			}
		}

		return result;
	}

	private void addPair(String name, List<Pair<String, String>> result, Prop prop)
	{
		String desc = prop.getText("components.gallery.visual_style."+name);
		if (desc.startsWith("components.gallery")) desc = name;

		result.add(new Pair<>(name, desc));
	}

	public boolean getAlsoTags()
	{
		return Tools.isNotEmpty(perexGroup);
	}

	/**
	 * Save image from pixabay URL into WebJET
	 * @return
	 */
	public Resolution saveImage()
	{
		JSONObject result = new JSONObject();
		IwcmFile temporaryFile = null;

		try {
			List<String> errors = new ArrayList<>();
			Prop prop = Prop.getInstance(getRequest());

			if(Tools.isEmpty(img)) {
				errors.add("Img can not be empty");
			}

			if (FileBrowserTools.hasForbiddenSymbol(fileName)) {
				errors.add(prop.getText("components.elfinder.commands.rename.error.banned_character"));
			}

			String sanitizedFileName = DocTools.removeChars(fileName, true);
			if(Tools.isEmpty(sanitizedFileName)) {
				errors.add(prop.getText("editor.upload_iframe.enterFileName"));
			}

			if(Tools.isEmpty(virtualPath)) {
				errors.add("VirtualPath can not be empty");
			}

			String extension = getImageExtension(img);
			boolean isVideo = FileTools.isVideoFile("video." + extension);
			if(Tools.isEmpty(extension) || (FileTools.isImage("image." + extension) == false && isVideo == false)) {
				errors.add(prop.getText("components.forum.new.upload_not_allowed_filetype"));
			}

			if(isVideo == false) {
				if(width == 0) {
					errors.add("Width can not be zero");
				}

				if(height == 0) {
					errors.add("Height can not be zero");
				}
			}

			if (errors.size() > 0) {
				return getSaveImageErrorResponse(result, errors);
			}

			String targetFileName = sanitizedFileName + "." + extension;
			String smallFileUrl = virtualPath + "/" + targetFileName;
			String realPathFileSmall = Tools.getRealPath(smallFileUrl);

			if (FileTools.exists(smallFileUrl)) {
				errors.add(prop.getText("multiple_files_upload.file_exist"));
				return getSaveImageErrorResponse(result, errors);
			}

			String file = virtualPath + "/.pixabay-" + UUID.randomUUID() + "." + extension;
			String realPathFile = Tools.getRealPath(file);
			temporaryFile = new IwcmFile(realPathFile);

			if (FileTools.downloadFile(img, file, null, 0, 120) == false) {
				errors.add(prop.getText("gallery.resizing.error_2"));
				return getSaveImageErrorResponse(result, errors);
			}

			//save pixabay image URL for later use
			if(this.img.contains(PIXABAY)) {
				GalleryService.savePixabayImageUrl(realPathFileSmall.substring(realPathFileSmall.lastIndexOf('/') + 1), this.img);
			}

			if ("svg".equals(extension)) {
				sanitizeSvgFile(temporaryFile);
				FileTools.copyFile(temporaryFile, new IwcmFile(realPathFileSmall));
			}
			else if (isVideo) {
				FileTools.copyFile(temporaryFile, new IwcmFile(realPathFileSmall));
			}
			else {
				GalleryDB.resizePicture(realPathFile, realPathFileSmall, width, height);
			}

			IwcmFile newFileIwcm = new IwcmFile(realPathFileSmall);
			if (newFileIwcm.exists() == false) {
				errors.add(prop.getText("gallery.resizing.error_2"));
				return getSaveImageErrorResponse(result, errors);
			}

			if (isVideo == false) {
				//ak je treba, aplikujem vodotlac na obrazky
				GalleryDB.applyWatermarkOnUpload(newFileIwcm);

				if (GalleryDB.isGalleryFolder(virtualPath))
				{
					GalleryDB.resizePicture(newFileIwcm.getAbsolutePath(), virtualPath);
				}
				else if (Constants.getBoolean("imageAlwaysCreateGalleryBean"))
				{
					GalleryDB.setImage(virtualPath, targetFileName);
				}
			}

			//ak existuje adresar files, treba indexovat
			if (FileIndexer.isFileIndexerConfigured())
			{
				List<ResultBean> indexedFiles = new ArrayList<>();
				FileIndexerTools.indexFile(smallFileUrl, indexedFiles, getRequest());
			}

			result.put("result", true);
			if (isVideo == false && GalleryDB.isGalleryFolder(virtualPath)) result.put("virtualPath", GalleryDB.getImagePathSmall(smallFileUrl));
			else result.put("virtualPath", smallFileUrl);
			result.put("cwd", virtualPath);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			result.put("result", false);
		}
		finally
		{
			if (temporaryFile != null) temporaryFile.delete();
		}

		return new StreamingResolution("application/json", result.toString());
	}

	private String getImageExtension(String imageUrl)
	{
		if (Tools.isEmpty(imageUrl)) return "";

		try {
			String extension = FileTools.getFileExtension(imageUrl);
			if ("jpeg".equals(extension)) return "jpg";
			return extension;
		}
		catch (Exception e) {
			return "";
		}
	}

	/**
	 * Sanitize SVG file by removing script elements and on* event attributes
	 * to prevent XSS attacks when the SVG is served to browsers.
	 */
	private void sanitizeSvgFile(IwcmFile svgFile)
	{
		try {
			String content = FileTools.readFileContent(svgFile.getAbsolutePath());
			Document doc = Jsoup.parse(content, "", Parser.xmlParser());

			// Remove script elements
			doc.select("script").remove();

			// Remove on* event handler attributes from all elements
			for (Element el : doc.select("*")) {
				el.attributes().asList().stream()
					.filter(attr -> attr.getKey().toLowerCase().startsWith("on"))
					.forEach(attr -> el.removeAttr(attr.getKey()));
			}

			// Remove href attributes with javascript: protocol
			for (Element el : doc.select("[href], [xlink:href]")) {
				String href = el.hasAttr("href") ? el.attr("href") : el.attr("xlink:href");
				if (href.replaceAll("\\s", "").toLowerCase().startsWith("javascript:")) {
					el.removeAttr("href");
					el.removeAttr("xlink:href");
				}
			}

			FileTools.saveFileContent(svgFile.getAbsolutePath(), doc.html(), StandardCharsets.UTF_8.name());
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private Resolution getSaveImageErrorResponse(JSONObject result, List<String> errors)
	{
		result.put("errors", new JSONArray(errors));
		result.put("result", false);
		return new StreamingResolution("application/json", result.toString());
	}

	public String getimg()
	{
		return img;
	}

	public void setImg(String img)
	{
		this.img = img;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public String getVirtualPath()
	{
		return virtualPath;
	}

	public void setVirtualPath(String virtualPath)
	{
		this.virtualPath = virtualPath;
	}

	public boolean isShortDescription()
	{
		return shortDescription;
	}

	public void setShortDescription(boolean shortDescription)
	{
		this.shortDescription = shortDescription;
	}

	public boolean isLongDescription()
	{
		return longDescription;
	}

	public void setLongDescription(boolean longDescription)
	{
		this.longDescription = longDescription;
	}

	public boolean isAuthor()
	{
		return author;
	}

	public void setAuthor(boolean author)
	{
		this.author = author;
	}

	public int getImagesInRow()
	{
		return imagesInRow;
	}

	public void setImagesInRow(int imagesInRow)
	{
		this.imagesInRow = imagesInRow;
	}

	public boolean isThumbsShortDescription()
	{
		return thumbsShortDescription;
	}

	public void setThumbsShortDescription(boolean thumbsShortDescription)
	{
		this.thumbsShortDescription = thumbsShortDescription;
	}
}
