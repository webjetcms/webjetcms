package sk.iway.iwcm.gallery;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
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

		try {
			List<String> errors = new ArrayList<>();

			if(Tools.isEmpty(img)) {
				errors.add("Img can not be empty");
			}

			if(Tools.isEmpty(virtualPath)) {
				errors.add("VirtualPath can not be empty");
			}

			if(width == 0) {
				errors.add("Width can not be zero");
			}

			if(height == 0) {
				errors.add("Height can not be zero");
			}

			if (errors.size() > 0) {
				result.put("errors", new JSONArray(errors));
				result.put("result", false);

				return new StreamingResolution("application/json", result.toString());
			}


			String filename = img.substring(img.lastIndexOf('/') + 1);
			String extension = filename.substring(filename.lastIndexOf('.') + 1);

			String file = virtualPath + "/" + filename;
			String realPathFile = Tools.getRealPath(file);

			String smallFileUrl = file.substring(0, file.lastIndexOf('_')) + "_" + width + "_" + height + "." + extension;
			String realPathFileSmall = Tools.getRealPath(smallFileUrl);

			FileTools.downloadFile(img, file);

			//save pixabay image URL for later use
			if(this.img.contains(PIXABAY)) {
				GalleryService.savePixabayImageUrl(realPathFileSmall.substring(realPathFileSmall.lastIndexOf('/') + 1), this.img);
			}

			GalleryDB.resizePicture(realPathFile, realPathFileSmall, width, height);

			new IwcmFile(realPathFile).delete();

			//ak je treba, aplikujem vodotlac na obrazky
			IwcmFile newFileIwcm = new IwcmFile(realPathFileSmall);
			GalleryDB.applyWatermarkOnUpload(newFileIwcm);

			if (GalleryDB.isGalleryFolder(virtualPath))
			{
				GalleryDB.resizePicture(newFileIwcm.getAbsolutePath(), virtualPath);
			}
			else if (Constants.getBoolean("imageAlwaysCreateGalleryBean"))
			{
				GalleryDB.setImage(virtualPath, filename);
			}

			//ak existuje adresar files, treba indexovat
			if (FileIndexer.isFileIndexerConfigured())
			{
				List<ResultBean> indexedFiles = new ArrayList<>();
				FileIndexerTools.indexFile(smallFileUrl, indexedFiles, getRequest());
			}

			result.put("result", true);
			if (GalleryDB.isGalleryFolder(virtualPath)) result.put("virtualPath", GalleryDB.getImagePathSmall(smallFileUrl));
			else result.put("virtualPath", smallFileUrl);
			result.put("cwd", virtualPath);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

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