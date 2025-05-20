package sk.iway.iwcm.gallery;

import static sk.iway.iwcm.Tools.isEmpty;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.GalleryDBTools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.components.gallery.GalleryService;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.filebrowser.FileDirBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFileFilter;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.metadata.MetadataCleaner;
import sk.iway.iwcm.tags.support_logic.ResponseUtils;
import sk.iway.spirit.MediaDB;

/**
 * Praca s FotoGaleriou
 *
 *@Title WebJET
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2002
 *@version 1.0
 *@created Nedeďż˝e, 2003, mďż˝j 18
 *@modified $Date: 2004/03/12 10:16:32 $
 */
public class GalleryDB
{

	public static final List<String> LANGUAGES = Arrays.asList("sk", "cz", "de", "en", "pl", "ru", "cho", "esp", "hu"); //NOSONAR
	public static final String DATE_FROM_SESSION_FILTER = "date_from_filter";
	public static final String DATE_TO_SESSION_FILTER = "date_to_filter";

	private static final Random rand = new Random();

	protected GalleryDB() {
		//utility class
	}

	private static final Mapper<GalleryDimension> dimensionMapper = new Mapper<GalleryDimension>()
	{
		@Override
		public GalleryDimension map(ResultSet rs) throws SQLException
		{
			GalleryDimension gallery = new GalleryDimension();

			gallery.setGalleryId(rs.getInt("dimension_id"));
			gallery.setGalleryPath(rs.getString("image_path"));
			gallery.setGalleryName((rs.getString("gallery_name") != null) ? (rs.getString("gallery_name")) : "");
			gallery.setGalleryPerex((rs.getString("gallery_perex") != null) ? (rs.getString("gallery_perex")) : "");
			gallery.setGalleryDate(rs.getTimestamp("create_date"));
			gallery.setGalleryViews(rs.getInt("views"));
			gallery.setAuthor((rs.getString("author") != null) ? (rs.getString("author")) : "");
			gallery.setWatermark(rs.getString("watermark"));
			gallery.setWatermarkPlacement(rs.getString("watermark_placement"));
			gallery.setWatermarkSaturation(rs.getInt("watermark_saturation"));
			return gallery;
		}
	};

	/**
	 * ziska z jazyka priponu pre nacitanie z databazy
	 *
	 * @param lng
	 * @param request
	 * @return
	 */
	public static String getLngAdd(String lng, HttpServletRequest request)
	{
		String lngAdd = "_sk";
		if (lng == null || (lng.length() != 2 && !"esp".equals(lng) && !"cho".equals(lng)) )
		{
			lng = Constants.getString("defaultLanguage");
		}
		lng = lng.replace('\'', ' ');
		lng = lng.replace(';', ' ');
		// podpora je iba pre tieto jazyky
		if (!LANGUAGES.contains(lng) )
		{
			lng = "sk";
		}
		if (lng.length() == 2 || lng.length() == 3)
		{
			lngAdd = "_" + lng.toLowerCase();
		}
		return (lngAdd);
	}

	public static Collection<IwcmFile> getImagesFromDir(String basePath, boolean alsoSubfolders){
			return getFilesFromDir(basePath, alsoSubfolders, listFiles(basePath));
	}

	/**
	 * Pripravi zoznam suborov v adresari, aby sa to dalo opakovane pouzit
	 * @param basePath
	 * @return
	 */
	private static IwcmFile[] listFiles(String basePath)
	{
		if (basePath.endsWith("/"))
		{
			basePath = basePath.substring(0, basePath.length() - 1);
		}

		String realPath = Tools.getRealPath(basePath);
		if (realPath != null)
		{
			IwcmFile file = new IwcmFile(realPath);
			try
			{
				if (isDirectoryFast(file))
				{
					IwcmFile[] files = file.listFiles(new IwcmFileFilter()
					{
						@Override
						public boolean accept(IwcmFile file)
						{
							return (isImageFast(file) || isDirectoryFast(file));
						}
					});
					return files;
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		return new IwcmFile[0];
	}

	/**
	 * Rekurzivne prehlada adresare a naplni Map s File objektami
	 *
	 *@param mainDir - adresar, ktory chceme rekurzivne prehladat
	 *@param hTable - Map, do ktorej budu pridane vsetky subory obrazkov z podadresarov
	 *@return
	 */
	private static List<IwcmFile> getFilesFromDir(String basePath, boolean alsoSubfolders, IwcmFile[] files)
	{
		if (basePath.endsWith("/"))
		{
			basePath = basePath.substring(0, basePath.length() - 1);
		}

		boolean noMiniatures = false;
		String resizeMode = getResizeMode(basePath, true);
		if(resizeMode!=null && resizeMode.equals("N"))
			noMiniatures = true;

		List<IwcmFile> fileList = new ArrayList<>();
		HashSet<String> origFileNames = new HashSet<>();
		int size = files.length;

		//ak sa nemaju nachadzat zmenseniny, nepotrebujeme kontrolovat origName
		if(!noMiniatures)
		{
			for (int i = 0; i < size; i++)
			{
				IwcmFile file = files[i];
				if (isDirectoryFast(file)==false && file.getName().startsWith("o_"))
				{
					origFileNames.add(file.getName());
				}
			}
		}

		for (int i = 0; i < size; i++)
		{
			IwcmFile file = files[i];
			if (alsoSubfolders && isDirectoryFast(file))
			{
				String subDir = basePath + "/" + file.getName();
				fileList.addAll(getFilesFromDir(subDir, alsoSubfolders, listFiles(subDir)));
			}
			else
			{
				//ak vyberame o_image.jpg namiesto image.jpg
				if(noMiniatures)
				{
					if (isImageFast(file))
					{
						fileList.add(file);
					}
				}
				else if (file.getName().startsWith("s_")==false && file.getName().startsWith("o_")==false && file.getName().startsWith("_tmp_o_")==false)
				{
					//pridame iba obrazky ktore maju o_ verziu
					if (isImageFast(file) && (origFileNames.contains("o_"+file.getName()) || Constants.getBoolean("imageAlwaysCreateGalleryBean") ))
					{
						fileList.add(file);
					}
				}
			}
		}

		return fileList;
	}

	/**
	 * Vrati HashTabluku so zaznamami z databazy pre zadany adresar a jeho podadresare
	 * @param dir
	 * @return
	 */
	public static Map<String, GalleryBean> getGalleryBeanTable(String dir, String lng, boolean alsoSubfolders)
	{
		if (dir.length()>2 && dir.endsWith("/"))
		{
			//odstran koncove lomitko
			dir = dir.substring(0, dir.length()-1);
		}

		Map<String, GalleryBean> gBeanTable = new Hashtable<>();
		GalleryBean gBean;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			if (alsoSubfolders)
			{
				ps = db_conn.prepareStatement("SELECT * FROM gallery WHERE image_path LIKE ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, dir + "%");
			}
			else
			{
				ps = db_conn.prepareStatement("SELECT * FROM gallery WHERE image_path=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, dir);
			}
			rs = ps.executeQuery();
			while (rs.next())
			{
				gBean = new GalleryBean();
				gBean.setImageId(rs.getInt("image_id"));
				gBean.setImageName(DB.getDbString(rs, "image_name"));
				gBean.setImagePath(DB.getDbString(rs, "image_path"));
				gBean.setLongDescription(DB.getDbString(rs, "l_description" + lng));
				gBean.setShortDescription(DB.getDbString(rs, "s_description" + lng));
				gBean.setAuthor(rs.getString("author"));
				gBean.setSendCount(rs.getInt("send_count"));
				gBean.setAllowedDomains(rs.getString("allowed_domains"));
				gBean.setPerexGroup(convertPerexGroupString(rs.getString("perex_group")));
				gBean.setUploadDate(DB.getDate(rs, "upload_datetime"));
				gBean.setSortPriority(rs.getInt("sort_priority"));
				gBeanTable.put(gBean.getImageUrl(), gBean);
			}

			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return gBeanTable;
	}

	/**
	 * Skonvertuje list suborov na list GalleryBeanov, ak pre zadanu cestu nenajde GalleryBean z databazy spravi prazdny
	 * @param fileList
	 * @param gBeanTable
	 * @return
	 */
	private static List<GalleryBean> convertFilesToGbean(List<IwcmFile> fileList, Map<String, GalleryBean> gBeanTable)
	{
		List<GalleryBean> gBeanList = new ArrayList<>();

		for (IwcmFile file : fileList)
		{
			String virtualPath = file.getVirtualPath();
			String fileName = file.getName();
			if(fileName.startsWith("o_") && fileName.length()>2)
			{
				int index = virtualPath.lastIndexOf(fileName);
				virtualPath = new StringBuilder(virtualPath).replace(index, index+fileName.length()+1,fileName.substring(2)).toString();
			}
			GalleryBean gb = gBeanTable.get(virtualPath);
			if (gb == null)
			{
				gb = new GalleryBean();
				gb.setImageId(-1);
				gb.setAuthor("");
				gb.setImageName(file.getName());
				gb.setImagePath(file.getVirtualParent());
				gb.setLongDescription("");
				gb.setShortDescription("");
				if (Constants.getBoolean("galleryUseFastLoading")==false) gb.setUploadDate(new Date(file.lastModified()));
			}
			else if (file.getName().startsWith("o_"))
			{
				gb.setImageName(file.getName());
			}
			gBeanList.add(gb);
		}

		return gBeanList;
	}

	private static List<GalleryBean> filterByPerexGroups(List<GalleryBean> gBeanList, String perexGroupIds)
	{
		if (Tools.isEmpty(perexGroupIds)) return gBeanList;

		List<GalleryBean> filtered = new ArrayList<>();

		String[] pgids = convertPerexGroupString(perexGroupIds);
		for (GalleryBean gb : gBeanList)
		{
			if (gb.getPerexGroup()==null || gb.getPerexGroup().length==0) continue;

			if (Tools.containsOneItem(pgids, gb.getPerexGroup()))
			{
				filtered.add(gb);
			}
		}

		return filtered;
	}

	/**
	 * @deprecated pouzite getImages
	 * @param dir
	 * @param servletContext
	 * @param request
	 * @param lng
	 * @return
	 */
	@Deprecated
	public static List<GalleryBean> getImagesFromDir(String dir, ServletContext servletContext, HttpServletRequest request, String lng)
	{
		return getImages(dir, false, lng, null, "title", "asc", request);
	}


	/**
	 * Nacita obrazky zo zadaneho adresara a spravi filtraciu a sortovanie na zaklade zadanych parametrov
	 * @param dir
	 * @param alsoSubfolders
	 * @param lng
	 * @param perexGroupIds
	 * @param orderBy
	 * @param orderDirection
	 * @param request
	 * @return
	 */
	public static List<GalleryBean> getImages(String dir, boolean alsoSubfolders, String lng, String perexGroupIds, String orderBy, String orderDirection, HttpServletRequest request)
	{
		return getImages(dir, alsoSubfolders, lng, perexGroupIds, orderBy, orderDirection, null, request);
	}

	/**
	 * Nacita obrazky zo zadaneho adresara a spravi filtraciu a sortovanie na zaklade zadanych parametrov
	 * @param dir
	 * @param alsoSubfolders
	 * @param lng
	 * @param perexGroupIds
	 * @param orderBy
	 * @param orderDirection
	 * @param saveDirsRequestName - meno request premennej do ktorej vlozi zoznam pod adresarov (pouziva sa pre admin cast)
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<GalleryBean> getImages(String dir, boolean alsoSubfolders, String lng, String perexGroupIds, String orderBy, String orderDirection, String saveDirsRequestName, HttpServletRequest request)
	{
		int galleryCacheResultMinutes = Constants.getInt("galleryCacheResultMinutes");
		Cache c = Cache.getInstance();
		List<GalleryBean> galleryList;
		String CACHE_KEY = null;
		String CACHE_KEY_DIRLIST = null;

		if (galleryCacheResultMinutes > 0 && alsoSubfolders==false && Tools.isEmpty(perexGroupIds))
		{
			//cachovanie zoznamu fotiek na zaklade lastmodified adresara
			IwcmFile galleryDir = new IwcmFile(Tools.getRealPath(dir));
			CACHE_KEY =         "GalleryDB.getImages-"    +dir+"-"+lng+"-"+orderBy+"-"+orderDirection+"-"+galleryDir.lastModified();
			CACHE_KEY_DIRLIST = "GalleryDB.getImagesDirs-"+dir+"-"+lng+"-"+orderBy+"-"+orderDirection+"-"+galleryDir.lastModified();

			galleryList = (List<GalleryBean>) c.getObject(CACHE_KEY);
			if (galleryList != null)
			{
				if (Tools.isNotEmpty(saveDirsRequestName))
				{
					List<FileDirBean> dirList = (List<FileDirBean>)c.getObject(CACHE_KEY_DIRLIST);
					if (dirList != null)
					{
						request.setAttribute(saveDirsRequestName, dirList);
					}
					else
					{
						//nemozeme vratit cachovany zoznam, pretoze nemame dirList
						galleryList = null;
					}
				}

				if (galleryList != null)
				{
					Logger.debug(GalleryDB.class, "Vraciam cachovany vystup galerie, key="+CACHE_KEY+" velkost="+galleryList.size());
					return galleryList;
				}
			}
		}

		galleryList = getImagesImpl(dir, alsoSubfolders, lng, perexGroupIds, orderBy, orderDirection, saveDirsRequestName, request);

		if (galleryCacheResultMinutes > 0 && CACHE_KEY != null)
		{
			//uloz vystup do cache
			c.setObjectSeconds(CACHE_KEY, galleryList, galleryCacheResultMinutes*60, true);

			if (Tools.isNotEmpty(saveDirsRequestName))
			{
				List<FileDirBean> dirList = (List<FileDirBean>)request.getAttribute(saveDirsRequestName);
				if (dirList != null)
				{
					c.setObjectSeconds(CACHE_KEY_DIRLIST, dirList, galleryCacheResultMinutes*60, true);
				}
			}
		}

		return galleryList;
	}

	/**
	 * Nacita obrazky zo zadaneho adresara a spravi filtraciu a sortovanie na zaklade zadanych parametrov
	 * @param dir
	 * @param alsoSubfolders
	 * @param lng
	 * @param perexGroupIds
	 * @param orderBy
	 * @param orderDirection
	 * @param saveDirsRequestName - meno request premennej do ktorej vlozi zoznam pod adresarov (pouziva sa pre admin cast)
	 * @param request
	 * @return
	 */
	private static List<GalleryBean> getImagesImpl(String dir, boolean alsoSubfolders, String lng, String perexGroupIds, String orderBy, String orderDirection, String saveDirsRequestName, HttpServletRequest request)
	{
		DebugTimer dt = new DebugTimer("GalleryDB.getImages, dir="+dir);

		HttpSession session = request.getSession();

		String lngAdd = getLngAdd(lng, request);

		//ziskaj beany
		Map<String, GalleryBean> gBeanTable = getGalleryBeanTable(dir, lngAdd, alsoSubfolders);

		dt.diff("After gBeanTable");

		//ziskaj subory

		IwcmFile[] filesInDir = listFiles(dir);
		dt.diff("After fileList");

		if (Tools.isNotEmpty(saveDirsRequestName))
		{
			request.setAttribute(saveDirsRequestName, GalleryDB.getDirs(dir, filesInDir));
			dt.diff("After get dirs");
		}

		List<IwcmFile> fileList = getFilesFromDir(dir, alsoSubfolders, filesInDir);
		dt.diff("After getFilesFromDir");

		//skonvertuj na pole gbeanov
		List<GalleryBean> gBeanList = convertFilesToGbean(fileList, gBeanTable);

		dt.diff("After convert");

		//skontroluj perex skupiny
		gBeanList = filterByPerexGroups(gBeanList, perexGroupIds);

		dt.diff("After filter perex group");

		//filtrovanie na zaklade datumu
		String filterDateFrom = session != null && session.getAttribute(DATE_FROM_SESSION_FILTER) != null ? (String)session.getAttribute(DATE_FROM_SESSION_FILTER) : null;
		String filterDateTo = session != null && session.getAttribute(DATE_TO_SESSION_FILTER) != null ? (String)session.getAttribute(DATE_TO_SESSION_FILTER) : null;

		if(Tools.isNotEmpty(filterDateFrom) || Tools.isNotEmpty(filterDateTo))
		{
			Calendar dateFrom = null;
			if(Tools.isNotEmpty(filterDateFrom))
			{
				dateFrom = Calendar.getInstance();
				dateFrom.setTimeInMillis(DB.getTimestamp(filterDateFrom, "00:00:00"));
			}
			Calendar dateTo = null;
			if(Tools.isNotEmpty(filterDateTo))
			{
				dateTo = Calendar.getInstance();
				dateTo.setTimeInMillis(DB.getTimestamp(filterDateTo, "23:59:59"));
			}
			List<GalleryBean> filtered = new ArrayList<>();

			if(gBeanList != null && gBeanList.size() > 0)
			{
				for (GalleryBean gb : gBeanList)
				{
					if(gb.getUploadDate() != null)
					{
						if(dateFrom != null && dateTo == null)
						{
							if(gb.getUploadDate().getTime() >= dateFrom.getTimeInMillis())
								filtered.add(gb);
						}
						else if(dateTo != null && dateFrom == null)
						{
							if(gb.getUploadDate().getTime() <= dateTo.getTimeInMillis())
								filtered.add(gb);
						}
						else if (dateFrom != null && dateTo != null)
						{
							if(gb.getUploadDate().getTime() >= dateFrom.getTimeInMillis() && gb.getUploadDate().getTime() <= dateTo.getTimeInMillis())
								filtered.add(gb);
						}
					}
				}
			}
			gBeanList = filtered;
		}
		//END filtrovanie na zaklade datumu

		dt.diff("After filter datum");

		//usortuj
		sort(gBeanList, orderBy, orderDirection);

		dt.diff("After sort");

		List<GalleryBean> returnList = new ArrayList<>();
		try
		{
			int page = 1;
			int pSize = -1;
			if (request.getAttribute("disablegPage") == null)
			{
				if (request.getParameter("gpage") != null) page = Tools.getIntValue(request.getParameter("gpage"), -1);
				else if (request.getAttribute("gpage") != null) page = Tools.getIntValue((String) request.getAttribute("gpage"), -1);
			}
			request.removeAttribute("disablegPage");
			if (session!=null) session.setAttribute("galleryActualPage", Integer.toString(page));
			if (request.getAttribute("disablegPage") == null && request.getParameter("gpsize") != null) pSize = Tools.getIntValue(request.getParameter("gpsize"), -1);
			else if (request.getAttribute("gpsize") != null) pSize = Tools.getIntValue((String) request.getAttribute("gpsize"), -1);

			int start = 0;
			int end = gBeanList.size();
			request.setAttribute("gTotalImages", Integer.toString(end));

			if (pSize > 0)
			{
				// vytvor strankovanie
				List<LabelValueDetails> pages = new ArrayList<>();
				try
				{
					StringBuilder params = new StringBuilder("gpsize=" + pSize);
					Enumeration<String> paramsEnum = request.getParameterNames();
					while (paramsEnum.hasMoreElements())
					{
						String param = paramsEnum.nextElement();
					   // tieto ignorujem, nastavim neskor
						if (param.equals("gpage") || param.equals("gpsize")) continue;
						if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML && param.equals("docid")) continue;

						params.append("&amp;").append(ResponseUtils.filter(param)).append('=').append(ResponseUtils.filter(request.getParameter(param)));
					}
					int counter = 1;
					while (start < end)
					{
						LabelValueDetails lvd = new LabelValueDetails(Integer.toString(counter), "");
						if (counter != page)	lvd.setValue(params.toString() + "&amp;gpage=" + counter);
						pages.add(lvd);
						counter++;
						start += pSize;
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				if (pages.size() > 1)
				{
					request.setAttribute("gPages", pages);
				}

				start = (page - 1) * pSize;
				end = start + pSize;
			}
			if (start < 0) start = 0;
			if (end > gBeanList.size() || Boolean.TRUE.equals(request.getAttribute("returnAllPages")))
			{
				end = gBeanList.size();
			}

			//cache nastaveni adresara galerie (pre FSDB nezistujem rozmery obrazka presne, ale pouzijem nastavenie galerie)
			Map<String, Dimension[]> dimensionDirTable = new Hashtable<>();
			for (int i = start; i < end; i++)
			{
				GalleryBean gBean = gBeanList.get(i);

				if (gBeanList.size()>1)
				{
					try
					{
						if (i > 0) gBean.setPrevImage(gBeanList.get(i-1).getImageUrl());
						else gBean.setPrevImage(gBeanList.get(gBeanList.size()-1).getImageUrl());

						if (i < (gBeanList.size()-1))	gBean.setNextImage(gBeanList.get(i+1).getImageUrl());
						else gBean.setNextImage(gBeanList.get(0).getImageUrl());
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}

				try
				{
					// testni ci existuje originalImage
					String originalImage = Tools.getRealPath(gBean.getImagePath() + File.separatorChar + "o_" + gBean.getImageName());
					IwcmFile file = new IwcmFile(originalImage);
					if (isImageFast(file))
					{
						gBean.setOriginalImage(file.getVirtualPath());
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				// ziskaj velky obrazok a ziskaj jeho rozmer a velkost
				try
				{
					IwcmFile bigImageFile = new IwcmFile(Tools.getRealPath(gBean.getImageUrl()));
					if (Constants.getBoolean("galleryUseFastLoading") || bigImageFile.exists())
					{
						if (Constants.getBoolean("galleryUseFastLoading")==false) gBean.setBigLength(FileTools.getFormatFileSize(bigImageFile.length(), false));
						if (Constants.getBoolean("galleryUseFastLoading"))
						{
							Dimension[] dims = dimensionDirTable.get(gBean.getImagePath());
							if (dims == null)
							{
								Logger.debug(GalleryDB.class, "getting dims for: "+gBean.getImagePath());
								dims = getDimension(gBean.getImagePath());
								dimensionDirTable.put(gBean.getImagePath(), dims);
							}
							if (dims != null && dims.length>1)
							{
								gBean.setBigDimension(dims[1].width+"x"+dims[1].height);
							}
						}
						else
						{
							int[] dims = GalleryDBTools.getImageSize(bigImageFile);
							gBean.setBigDimension(dims[0]+"x"+dims[1]);
						}
					}
					// timer.diff("mam: " + fName);
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				returnList.add(gBean);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		dt.diff("Returning");

		return returnList;
	}

	private static void sort(List<GalleryBean> list, String orderBy, String orderDirection)
	{
		if("asc".equals(orderDirection))
		{

			if(orderBy.equalsIgnoreCase("priority"))
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2) {
				        return (gb1.getSortPriority() - gb2.getSortPriority()) == 0?gb1.getImageName().compareTo(gb2.getImageName()):(gb1.getSortPriority() - gb2.getSortPriority());
				    }
				});
			else
			if(orderBy.equalsIgnoreCase("date"))
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2) {
				        int dateCmp = gb1.getUploadDate().compareTo(gb2.getUploadDate());
				        if (dateCmp != 0)
				            return dateCmp;
				        return gb1.getImageName().compareTo(gb2.getImageName());
				    }
				});
			else
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2) {
				       int stringCmp = gb1.getImageName().compareTo(gb2.getImageName());
				       if (stringCmp != 0)
				            return stringCmp;
				       return gb1.getUploadDate().compareTo(gb2.getUploadDate());
				    }
				});
		}
		else
		{
			if(orderBy.equalsIgnoreCase("priority"))
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2) {
				   	 return (gb2.getSortPriority() - gb1.getSortPriority()) == 0?gb2.getImageName().compareTo(gb1.getImageName()):(gb2.getSortPriority() - gb1.getSortPriority());
				    }
				});
			else
			if(orderBy.equalsIgnoreCase("date"))
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2)
				    {
				        int dateCmp = gb2.getUploadDate().compareTo(gb1.getUploadDate());
				        Logger.debug(GalleryDB.class, "Comparing: "+gb1.getImageName()+" ("+Tools.formatDate(gb1.getUploadDate())+") vs "+gb2.getImageName()+" ("+Tools.formatDate(gb2.getUploadDate())+") ret="+dateCmp);
				        if (dateCmp != 0)
				            return dateCmp;
				        return gb2.getImageName().compareTo(gb1.getImageName());
				    }
				});
			else
				Collections.sort(list, new Comparator<GalleryBean>() {
				    @Override
					public int compare(GalleryBean gb1, GalleryBean gb2) {
				       int stringCmp = gb2.getImageName().compareTo(gb1.getImageName());
				       if (stringCmp != 0)
				            return stringCmp;
				       return gb2.getUploadDate().compareTo(gb1.getUploadDate());
				    }
				});
		}
	}

	/**
	 * Vrati nahodny obrazok z galerie, pricom si ho pamata zadany pocet minut
	 *
	 *@param dir
	 *@param rememberMinutes
	 *           - pocet minut, pocas ktorych si zapamata obrazok a negeneruje ho
	 *           znova
	 *@param servletContext
	 *@param request
	 *@return
	 */
	public static GalleryBean getRandomImageFromDir(String dir, int rememberMinutes, ServletContext servletContext,
				HttpServletRequest request)
	{
		String scImage = "GALLERY_RANDOM_IMAGE";
		String scImageTime = "GALLERY_RANDOM_IMAGE_TIME";
		GalleryBean randomImage = null;
		long now = (new java.util.Date()).getTime();
		if (rememberMinutes > 0)
		{
			// najskor zisti, ci uz obrazok nemame a ci nie je platny
			randomImage = (GalleryBean) servletContext.getAttribute(scImage);
			Long imageTime = (Long) servletContext.getAttribute(scImageTime);
			now = now - (rememberMinutes * 60L * 1000);
			if (randomImage != null && imageTime != null && imageTime.longValue() > now)
			{
				return (randomImage);
			}
		}
		// obrazok neexistuje, alebo je stary, ziskaj zoznam a nahodne vyber
		request.setAttribute("disablegPage", "true");
		List<GalleryBean> images = getImages(dir, false, null, null, "title", "asc", request);
		request.removeAttribute("disablegPage");
		int maxSize = images.size();
		if (maxSize < 1)
		{
			return (new GalleryBean());
		}
		// posledne vygenerovany obrazok
		String lastImageName = null;
		if (randomImage != null)
		{
			lastImageName = randomImage.getImageName();
		}
		// vygeneruj nahodne cislo
		int index;
		int counter = 0;
		GalleryBean newImage = null;
		while (counter < 10)
		{
			try
			{
				index = rand.nextInt(maxSize);
				newImage = images.get(index);
				if (lastImageName == null || newImage.getImageName().compareTo(lastImageName) != 0)
				{
					break;
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			counter++;
		}
		// ok, mali by sme mat obrazok
		if (newImage != null)
		{
			// uloz to do servlet contextu
			servletContext.setAttribute(scImage, newImage);
			now = (new java.util.Date()).getTime();
			servletContext.setAttribute(scImageTime, Long.valueOf(now));
			return (newImage);
		}
		return (new GalleryBean());
	}

	/**
	 * Vrati celkovy pocet obrazkov v zadanom adresari, berie do uvahy len
	 *  small verzie obrazkov (pocita len obrazky s predponou "s_")
	 *
	 *@param dir
	 *           - adresar s obrazkami
	 *@return
	 */
	public static int getTotalImagesInDir(String dir)
	{
		int ret = 0;
		try
		{
			String realPath = Tools.getRealPath(dir);
			IwcmFile file = new IwcmFile(realPath);
			IwcmFile[] fileList = file.listFiles(new IwcmFileFilter()
			{
				@Override
				public boolean accept(IwcmFile pathname)
				{
					return (isImageFast(pathname))
								&& (pathname.getName().startsWith("o_")==false && pathname.getName().startsWith("s_")==false);
				}
			});
			ret = fileList.length;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (ret);
	}

	/**
	 * Gets the imageByID attribute of the GalleryDB class
	 *
	 *@param iIDString
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@return The imageByID value
	 */
	public static GalleryBean getImageByID(String iIDString, HttpServletRequest request, String lng)
	{
		return getImageByID(Integer.parseInt(iIDString), request, lng);
	}

	/**
	 * Gets the imageByID attribute of the GalleryDB class
	 *
	 *@param iID
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@return The imageByID value
	 */
	public static GalleryBean getImageByID(int iID, HttpServletRequest request, String lng)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		GalleryBean gBean = null;
		String sql = "SELECT * FROM gallery WHERE image_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);
		String lngAdd = getLngAdd(lng, request);
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, iID);
			rs = ps.executeQuery();
			while (rs.next())
			{
				gBean = new GalleryBean();
				gBean.setImageId(rs.getInt("image_id"));
				gBean.setImageName(DB.getDbString(rs, "image_name"));
				gBean.setImagePath(DB.getDbString(rs, "image_path"));
				gBean.setLongDescription(DB.getDbString(rs, "l_description" + lngAdd));
				gBean.setShortDescription(DB.getDbString(rs, "s_description" + lngAdd));
				gBean.setPerexGroup(convertPerexGroupString(DB.getDbString(rs, "perex_group")));
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return gBean;
	}

	/**
	 * Ulozi informacie o obrazku do DB, vrati id obrazku
	 * @param request
	 */
	public static int setImage(HttpServletRequest request)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;

		int iID = -1;

		try
		{
			String dir = request.getParameter("dir");
			String name = request.getParameter("name");

			GalleryBean gBean = GalleryDB.getGalleryBean(dir, name, request, "sk");

			if (gBean != null) iID = gBean.getImageId();

			Logger.debug(GalleryDB.class, "setImage iID="+iID+" dir="+dir+" name="+name);

			String sql = "INSERT INTO gallery (image_path,image_name,s_description_sk,l_description_sk,s_description_en,l_description_en,s_description_cz,l_description_cz,s_description_de,l_description_de,"+
				"s_description_pl,l_description_pl,s_description_hu,l_description_hu,s_description_ru,l_description_ru,s_description_esp,l_description_esp,s_description_cho,l_description_cho,author,allowed_domains, perex_group, upload_datetime,sort_priority, domain_id)"+
				" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			if (iID > 0)
			{
				Logger.debug(GalleryDB.class, "UPDATE");
				sql = "UPDATE gallery SET image_path=?,image_name=?,s_description_sk=?,l_description_sk=?,s_description_en=?,l_description_en=?,s_description_cz=?,l_description_cz=?,s_description_de=?,l_description_de=?,"
					+"s_description_pl=?,l_description_pl=?,s_description_hu=?,l_description_hu=?,s_description_ru=?,l_description_ru=?,s_description_esp=?,l_description_esp=?,s_description_cho=?,l_description_cho=?,author=?,allowed_domains=?,perex_group=?, upload_datetime = ?, sort_priority=?, domain_id=? WHERE image_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);
			}

			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement(sql);
			int parameterIndex = 1;
			ps.setString(parameterIndex++, dir);
			ps.setString(parameterIndex++, name);
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_sk")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_sk")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_en")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_en")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_cz")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_cz")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_de")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_de")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_pl")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_pl")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_hu")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_hu")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_ru")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_ru")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_esp")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_esp")));
			ps.setString(parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("s_description_cho")));
			DB.setClob(ps, parameterIndex++, EditorDB.escapeInvalidCharacters(request.getParameter("l_description_cho")));
			ps.setString(parameterIndex++, request.getParameter("author"));
			ps.setString(parameterIndex++, request.getParameter("allowed_domains"));
			ps.setString(parameterIndex++, convertPerexGroupString(request.getParameterValues("perexGroup")));

			// formatovanie datumov do stringu kvoli rozdeleniu casu a datumu na dve polozky
	      String requestDate = Tools.getStringValue(request.getParameter("uploadDate"), "01.01.2000");
	      String requestTime = Tools.getStringValue(request.getParameter("uploadTime"), "00:00:00");

			ps.setTimestamp(parameterIndex++, new Timestamp(DB.getTimestamp(requestDate, requestTime)));

			int priority = 0;
			if(Tools.getIntValue(request.getParameter("sort_priority"), 0) == 0)
				priority = getNewPriority(dir);
			else
				priority = Tools.getIntValue(request.getParameter("sort_priority"),Integer.MAX_VALUE);
			ps.setInt(parameterIndex++,priority);

			ps.setInt(parameterIndex++, CloudToolsForCore.getDomainId());

			if (iID > 0)
				ps.setInt(parameterIndex++, iID);

			ps.execute();
			ps.close();
			if (iID <= 0)
			{
				//update pkey generator
				PkeyGenerator.getNextValue("gallery");

				// ziskaj ID z databazy
				ps = db_conn.prepareStatement("SELECT image_id FROM gallery WHERE image_path = ? AND image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, dir);
				ps.setString(2, name);
				rs = ps.executeQuery();
				if (rs.next())
				{
					iID = rs.getInt("image_id");
				}
				rs.close();
				ps.close();
			}

			//kvoli bugu s oblastou zaujmu nam vznikali duplicity, zmaz
			ps = db_conn.prepareStatement("DELETE FROM gallery WHERE image_path = ? AND image_name = ? AND image_id != ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1, dir);
			ps.setString(2, name);
			ps.setInt(3, iID);
			ps.execute();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return iID;
	}


	/**
	 * Metoda vypocita prioritu zobrazenia obrazku pre dany adresar.
	 * @param dir
	 * @return
	 */
	public static int getNewPriority(String dir)
	{
		return getTotalImagesInDir(dir) * 10 +10;
	}

	/**
	 * Metoda vypocita pre obrazok tak ze najde
	 * najvyssiu hodnotu pola sortPriority  v danom adresari
	 * a pripocita 10
	 * @param dir
	 * @return
	 */
	public static int getUpdatePriority(String dir)
	{
		return getMaxPriority(dir) +10;
	}

	/**
	 * Vrati najvyssiu hodnotu priority pre dany directory
	 * @param dir
	 * @return
	 */
	private static int getMaxPriority(String dir)
	{
		int result = DB.queryForInt("Select max(sort_priority) from gallery where image_path = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir);
		return result;
	}

	/**
	 * Uvodne ulozenie obrazku do tabulky gallery, vytvori sa novy zaznam pre obrazok s aktualnym datumom uploadu. <br />
	 * Ak uz taky obrazok existuje, ale nema nastaveny datum uploadu, tak sa mu nastavi datum poslednej modifikacie suboru (Spatne to zistit nevieme). <br />
	 *
	 *
	 * @param 	dir	Nazov virtualneho adresara, v ktorom je obrazok ulozeny napr. /images/gallery
	 * @param 	name	Nazov obrazka s priponou napr. obrazok.jpg
	 *
	 * @return	Vrati identifikator obrazka, ktory bol prave ulozeny do tabulky. Ak uz taky obrazok existuje, vrati jeho id. Ak nastal nejaky problem s DB, vrati -1.
	 */
	public static int setImage(String dir, String name)
	{
		IwcmFile imageFile = new IwcmFile(dir + File.separator + name);

		SimpleQuery query = new SimpleQuery();
		int imageId = 0;

		try
		{
			imageId = query.forInt("SELECT image_id FROM gallery WHERE image_path = ? AND image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir, name);
			if (GalleryDB.getUploadDateImage(dir, name) == null)
				GalleryDB.setUploadDateImage(dir, name, imageFile.lastModified());

		}
		catch (Exception e)
		{
			if (imageId > 0)
				return imageId;
		}
		if (imageId > 0)
			return imageId;


		int priority = getNewPriority(dir);
		String imageSource = GalleryService.getPixabayImageUrl(name, true);
		query.execute("INSERT INTO gallery (image_path, image_name, image_source, upload_datetime, sort_priority, domain_id) VALUES(?, ?, ?, ?, ?, ?)", dir, name, imageSource, new Timestamp(Tools.getNow()),priority, CloudToolsForCore.getDomainId());

		//update pkey generator
		PkeyGenerator.getNextValue("gallery");

		try
		{
			return query.forInt("SELECT image_id FROM gallery WHERE image_path = ? AND image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir, name);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return -1;
		}
	}

	/**
	 * Ulozi do tabulky gallery_dimension novy zaznam pre galeriu.
	 *
	 * @param 	dir	Nazov virtualneho adresara, v ktorom bude galeria ulozena napr. /images/gallery/new
	 * @return	Vrati identifikator galerie, ktora bola prave ulozena do tabulky. Ak uz taka galeria existuje, vrati jeho id. Ak nastal nejaky problem s DB, vrati -1.
	 */
	public static int setGallery(String dir)
	{
		SimpleQuery query = new SimpleQuery();
		int galleryId = 0;

		try
		{
			galleryId = query.forInt("SELECT dimension_id FROM gallery_dimension WHERE image_path= ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir);
		}
		catch (Exception e)
		{
			//
		}
		if (galleryId > 0)
			return galleryId;

		Dimension[] dims = getDimension(dir);

		query.execute("INSERT INTO gallery_dimension (image_path, image_width, image_height, normal_width, normal_height, resize_mode, create_date, domain_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?)",
					dir,dims[0].width, dims[0].height, dims[1].width, dims[1].height, GalleryDB.getResizeMode(dir, true), new Timestamp(Tools.getNow()), CloudToolsForCore.getDomainId());
		try
		{
			return query.forInt("SELECT dimension_id FROM gallery_dimension WHERE image_path= ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return -1;
		}
	}

	/**
	 * Vrati identifikator obrazka z tabulky gallery podla jeho virtualnej cesty a mena
	 *
	 * @param 	dir	Nazov virtualneho adresara, v ktorom je obrazok ulozeny napr. /images/gallery
	 * @param 	name	Nazov obrazka s priponou napr. obrazok.jpg
	 *
	 * @return	Vrati identifikator obrazka podla jeho cesty a mena. Ak nastal nejaky problem pri spojeni s DB, prip. taky obrazok neexistuje, vrati -1
	 */
	public static int getImageId(String dir, String name)
	{
		try
		{
			return new SimpleQuery().forInt("SELECT image_id FROM gallery WHERE image_path= ? AND image_name= ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir, name);
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	/**
	 * Description of the Method
	 *
	 *@param dir
	 *           Description of the Parameter
	 *@param name
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@param imageId
	 *           Description of the Parameter
	 *@param servletContext
	 *           Description of the Parameter
	 */
	public static void removeImage(String imageId, String dir, String name, ServletContext servletContext,
				HttpServletRequest request)
	{
		int iID = Tools.getIntValue(imageId, -1);
		removeImage(iID, dir, name, servletContext, request);
	}

	/**
	 * Description of the Method
	 *
	 *@param imageId
	 *           Description of the Parameter
	 *@param dir
	 *           Description of the Parameter
	 *@param name
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@param servletContext
	 *           Description of the Parameter
	 */
	public static void removeImage(int imageId, String dir, String name, ServletContext servletContext, HttpServletRequest request)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		IwcmFile file = null;
		String sql = "DELETE FROM gallery WHERE image_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);

		if (dir.startsWith("/")==false) dir = "/"+dir;
		if (name.startsWith("/")) name = name.substring(1);

		if (imageId < 1) imageId = getImageId(dir, name);

		String path = Tools.getRealPath(dir);
		try
		{
			if (imageId > 0)
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, imageId);
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
			}
			file = new IwcmFile(path + File.separator + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
			file = new IwcmFile(path + File.separator + "s_" + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
			file = new IwcmFile(path + File.separator + "o_" + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	/**
	 * ziska info o obrazku z databazy
	 *
	 * @param src
	 * @param request
	 * @return
	 */
	public static GalleryBean getGalleryBean(String src, HttpServletRequest request)
	{
		return (getGalleryBean(src, request, null));
	}

	/**
	 * Ziska info o obrazku z databazy podla jazykovej mutacie
	 *
	 * @param src
	 * @param request
	 * @param lng
	 * @return
	 */
	public static GalleryBean getGalleryBean(String src, HttpServletRequest request, String lng)
	{
		String dir = "/";
		String name = src;
		int i = src.lastIndexOf('/');
		if (i > 0)
		{
			dir = src.substring(0, i);
			name = src.substring(i + 1, src.length()).trim();
		}
		return (getGalleryBean(dir, name, request, lng));
	}

	/**
	 * Ziska info o obrazku z databazy
	 *
	 *@param dir
	 *           Description of the Parameter
	 *@param name
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@return The l_S_Description value
	 */
	public static GalleryBean getGalleryBean(String dir, String name, HttpServletRequest request)
	{
		return (getGalleryBean(dir, name, request, null));
	}

	/**
	 * Ziska info o obrazku z databazy pre danu jazykovu mutaciu
	 *
	 * @param dir
	 * @param name
	 * @param request
	 * @param lng
	 * @return
	 */
	public static GalleryBean getGalleryBean(String dir, String name, HttpServletRequest request, String lng)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		//order je tu kvoli bugu z duplicitou obrazkov
		String sql = "SELECT * FROM gallery WHERE image_path=? AND image_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY image_id ASC";
		boolean cyclicViewing = true;
		GalleryBean gBean = null;
		IwcmFile file;
		try
		{
			if ("false".equals(request.getParameter("cyclicViewing")))
			{
				cyclicViewing = false;
			}
			if (!dir.equals("") && !name.equals(""))
			{
				if (dir.startsWith("/")==false) dir = "/"+dir;

				gBean = new GalleryBean();
				if (name.startsWith("s_"))
				{
					name = name.substring(2, name.length());
				}

				request.setAttribute("imageURL", dir + "/" + name);

				if (name.startsWith("o_"))
				{
					//toto mame kvoli najdeniu dat, ktore su pre normal image, ak ale clovek chcel o_ obrazok je nastavenie imageURL pred tymto blokom naschval
					name = name.substring(2, name.length());
				}

				// testni ci existuje originalImage
				if (Constants.getBoolean("galleryUseFastLoading"))
				{
					//kvoli pluske prepnute na lazy testovanie o_ obrazku
					gBean.setOriginalImage(null);
				}
				else
				{
					try
					{
						String originalImage = dir + "/o_" + name;
						file = new IwcmFile(Tools.getRealPath(originalImage));
						if (file.exists() && isImageFast(file))
						{
							gBean.setOriginalImage(originalImage);
						}
					}
					catch (Exception ex)
					{
					}
				}

				gBean.setImagePath(dir);
				gBean.setImageName(name);

				String lngAdd = getLngAdd(lng, request);
				db_conn = DBPool.getConnection(DBPool.getDBName(request));
				ps = db_conn.prepareStatement(sql);
				ps.setString(1, dir);
				ps.setString(2, name);
				rs = ps.executeQuery();
				if (rs.next())
				{
					gBean.setImageId(rs.getInt("image_id"));
					gBean.setImageName(DB.getDbString(rs, "image_name"));
					gBean.setImagePath(DB.getDbString(rs, "image_path"));
					gBean.setLongDescription(DB.getDbString(rs, "l_description" + lngAdd));
					gBean.setShortDescription(DB.getDbString(rs, "s_description" + lngAdd));
					gBean.setAuthor(DB.getDbString(rs, "author"));
					gBean.setSendCount(rs.getInt("send_count"));
					gBean.setAllowedDomains(DB.getDbString(rs, "allowed_domains"));
					gBean.setPerexGroup(convertPerexGroupString(DB.getDbString(rs, "perex_group")));
					gBean.setSelectedX(rs.getInt("selected_x"));
					gBean.setSelectedY(rs.getInt("selected_y"));
					gBean.setSelectedWidth(rs.getInt("selected_width"));
					gBean.setSelectedHeight(rs.getInt("selected_height"));
					gBean.setUploadDate(rs.getTimestamp("upload_datetime"));
					gBean.setSortPriority(rs.getInt("sort_priority"));
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
			}
			if (gBean != null) gBean.setCyclicViewing(cyclicViewing);
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (gBean);
	}

	public static void fillPrevNextLink(GalleryBean gBean, boolean cyclicViewing)
	{
		try
		{
			String dir = gBean.getImagePath();
			String name = gBean.getImageName();
			if (Tools.isEmpty(dir) && Tools.isNotEmpty(gBean.getOriginalImage()))
			{
				dir = gBean.getOriginalImage().substring(0, gBean.getOriginalImage().lastIndexOf("/"));
				name = gBean.getOriginalImage().substring(gBean.getOriginalImage().lastIndexOf("/")+1);
			}

			if (Tools.isEmpty(dir)) return;

			// ziskaj linku na predchadzajuci a nasledujuci obrazok
			String realPath = Tools.getRealPath(dir);
			IwcmFile file = new IwcmFile(realPath);
			IwcmFile[] fileList = file.listFiles(new IwcmFileFilter()
			{
				@Override
				public boolean accept(IwcmFile pathname)
				{
					return (isImageFast(pathname))
								&& pathname.getName().startsWith("s_");
				}
			});
			try
			{
				Arrays.sort(fileList, new Comparator<IwcmFile>()
				{
					@Override
					public int compare(IwcmFile f1, IwcmFile f2)
					{
						return (f1.getName().compareTo(f2.getName()));
					}
				});
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			if (!name.startsWith("s_"))
			{
				name = "s_" + name;
			}
			int i = 0;
			int end = fileList.length;
			for (i = 0; i < end; i++)
			{
				file = fileList[i];
				if (file.getName().equals(name))
				{
					if (i > 0)
					{
						file = fileList[i - 1];
						// meno suboru je s_nieco.jpg
						gBean.setPrevImage(dir + "/" + file.getName().substring(2));
					}
					else if (i == 0)
					{
						if (cyclicViewing && end > 1)
						{
							file = fileList[end - 1];
							// meno suboru je s_nieco.jpg
							gBean.setPrevImage(dir + "/" + file.getName().substring(2));
						}
						else
						{
							gBean.setPrevImage(null);
						}
					}
					if (i < (end - 1))
					{
						file = fileList[i + 1];
						gBean.setNextImage(dir + "/" + file.getName().substring(2));
					}
					else
					{
						if (cyclicViewing && end > 1)
						{
							file = fileList[0];
							gBean.setNextImage(dir + "/" + file.getName().substring(2));
						}
						else
						{
							gBean.setNextImage(null);
						}
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Vrati zoznam adresarov v zadanom adresari
	 *
	 *@param dir
	 *           Description of the Parameter
	 *@return The dirs value
	 */
	public static List<FileDirBean> getDirs(String dir)
	{
		return getDirs(dir, listFiles(dir));
	}
	/**
	 * Vrati zoznam adresarov v zadanom adresari
	 * @param dir
	 * @param fileList
	 * @return
	 */
	public static List<FileDirBean> getDirs(String dir, IwcmFile[] fileList)
	{
		DebugTimer dt = new DebugTimer("GalleryDB.getDirs");

		if (dir == null || dir.length() == 0)
		{
			if (Constants.getString("imagesRootDir").length() > 1)
			{
				dir = Constants.getString("imagesRootDir") + "/" + Constants.getString("galleryDirName");
			}
			else
			{
				dir = "/";
			}
		}
		List<FileDirBean> aList = new ArrayList<>();
		FileDirBean fBean = null;
		try
		{
				fBean = new FileDirBean();
				int index = dir.lastIndexOf('/');
				fBean.setName("..");
				fBean.setIcon("/components/_common/mime/folderback.gif");
				if (index > 0)
				{
					fBean.setPath(dir.substring(0, index));
				}
				else
				{
					fBean.setPath("/");
				}
				if (dir.length() > 1)
				{
					aList.add(fBean);
				}

				try
				{
					// usortuj to podla abecedy
					Arrays.sort(fileList, new Comparator<IwcmFile>()
					{
						@Override
						public int compare(IwcmFile f1, IwcmFile f2)
						{
							return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
						}
					});
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				dt.diff("after compare");

				int imgCount;
				String name;
				for (int i = 0; i < fileList.length; i++)
				{
					if (isDirectoryFast(fileList[i]))
					{
						fBean = new FileDirBean();
						// ziskaj pocet obrazkov v danom adresari
						if (Constants.getBoolean("galleryUseFastLoading")) imgCount = 0;
						else imgCount = getTotalImagesInDir(dir + "/" + fileList[i].getName());

						if (imgCount > 0)
						{
							fBean.setImagesInDir(Integer.toString(imgCount));
						}

						fBean.setName(fileList[i].getName());
						fBean.setIcon("/components/_common/mime/folder.gif");
						fBean.setPath(dir + "/" + fileList[i].getName());

						name = fBean.getPath() + "/";

						if (name.startsWith("/WEB-INF/") || name.startsWith("/admin/") || name.startsWith("/templates/")
									|| name.startsWith("/components/") || name.startsWith("/css/") || name.startsWith("/jscripts/")
									|| name.startsWith("/CVS/") || name.startsWith("/META-INF/") || name.indexOf(".svn") != -1)
						{
							// Logger.println(GalleryDB.class,"som web inf");
							continue;
						}
						aList.add(fBean);
					}
				}

				dt.diff("after total images");
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return aList;
	}

	/**
	 * Description of the Method
	 *
	 *@param request
	 *           Description of the Parameter
	 *@param servletContext
	 *           Description of the Parameter
	 */
	public static void changeDimension(ServletContext servletContext, HttpServletRequest request, PrintWriter out, Prop prop)
	{
		Dimension dim = new Dimension();
		Dimension normalDim = new Dimension();
		String dir = request.getParameter("dir");
		try
		{
			try
			{
				dim.width = Integer.parseInt(request.getParameter("imageWidth"));
			}
			catch (Exception ex)
			{
				dim.width = 160;
			}
			try
			{
				dim.height = Integer.parseInt(request.getParameter("imageHeight"));
			}
			catch (Exception ex)
			{
				dim.height = 120;
			}
			try
			{
				normalDim.width = Integer.parseInt(request.getParameter("imageWidthNormal"));
			}
			catch (Exception ex)
			{
				normalDim.width = 0;
			}
			try
			{
				normalDim.height = Integer.parseInt(request.getParameter("imageHeightNormal"));
			}
			catch (Exception ex)
			{
				normalDim.height = 0;
			}
			changeDimension(servletContext, dir, dim, normalDim, request, out, prop);
		}
		catch (NumberFormatException nfe)
		{
			sk.iway.iwcm.Logger.error(nfe);
		}
	}

	/**
	 * Description of the Method
	 *
	 *@param request
	 *           Description of the Parameter
	 *@param servletContext
	 *           Description of the Parameter
	 */
	public static void changeDimension(ServletContext servletContext, String dir, HttpServletRequest request, PrintWriter out,
				Prop prop)
	{
		Dimension dim = new Dimension();
		Dimension normalDim = new Dimension();
		try
		{
			try
			{
				dim.width = Integer.parseInt(request.getParameter("imageWidth"));
			}
			catch (Exception ex)
			{
				dim.width = 160;
			}
			try
			{
				dim.height = Integer.parseInt(request.getParameter("imageHeight"));
			}
			catch (Exception ex)
			{
				dim.height = 120;
			}
			try
			{
				normalDim.width = Integer.parseInt(request.getParameter("imageWidthNormal"));
			}
			catch (Exception ex)
			{
				normalDim.width = 0;
			}
			try
			{
				normalDim.height = Integer.parseInt(request.getParameter("imageHeightNormal"));
			}
			catch (Exception ex)
			{
				normalDim.height = 0;
			}
			changeDimension(servletContext, dir, dim, normalDim, request, out, prop);
		}
		catch (NumberFormatException nfe)
		{
			sk.iway.iwcm.Logger.error(nfe);
		}
	}

	public static void changeDimension(ServletContext servletContext, String dir, Dimension dim, Dimension dimNormal,
				HttpServletRequest request)
	{
		changeDimension(servletContext, dir, dim, dimNormal, request, null, null);
	}

	public static void println(PrintWriter out, String message)
	{
		out.println(message + "<br>");
		out.flush();
	}

	public static void printlnError(PrintWriter out, String message)
	{
		out.println("<font color='red'>" + message + "</font><br>");
		out.flush();
	}

	/**
	 * Description of the Method
	 *
	 *@param dir
	 *           Description of the Parameter
	 *@param dim
	 *           Description of the Parameter
	 *@param request
	 *           Description of the Parameter
	 *@param servletContext
	 *           Description of the Parameter
	 *@param dimNormal
	 *           Description of the Parameter
	 */
	public static void changeDimension(ServletContext servletContext, String dir, Dimension dim, Dimension dimNormal,
				HttpServletRequest request, PrintWriter out, Prop prop)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;

		//oprava 9.8.2012, povodny stav:
		//Date createDate = new Date(DB.getTimestamp(request.getParameter("createdDate"), request.getParameter("time")));
		Date createDate;

		String resizeMode = "";
		String galleryName = "";
		String galleryPerex = "";
		String author = "";
		if (request != null)
		{
			resizeMode = request.getParameter("resizeMode");
			galleryName = request.getParameter("galleryName");
			galleryPerex = request.getParameter("galleryPerex");
			author = request.getParameter("author");
			//novy stav
			createDate = new Date(DB.getTimestamp(request.getParameter("createdDate"), request.getParameter("time")));
		}
		else
			//novy stav
			createDate = Calendar.getInstance().getTime();
		try
		{
			if (!dir.equals(""))
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("DELETE FROM gallery_dimension WHERE image_path=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, normalizeDir(dir));
				ps.execute();
				ps.close();
				ps = db_conn
					.prepareStatement("INSERT INTO gallery_dimension (image_width, image_height, image_path, normal_width, normal_height, resize_mode," +
							"gallery_name, gallery_perex, create_date, author, watermark, watermark_saturation, watermark_placement, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				ps.setInt(1, dim.width);
				ps.setInt(2, dim.height);
				ps.setString(3, normalizeDir(dir));
				ps.setInt(4, dimNormal.width);
				ps.setInt(5, dimNormal.height);
				ps.setString(6, resizeMode);
				ps.setString(7, galleryName);
				ps.setString(8, galleryPerex);
				ps.setTimestamp(9, new Timestamp(createDate.getTime()));
				ps.setString(10, author);
				if (request != null)
				{
					ps.setString(11, StringUtils.abbreviate(request.getParameter("watermark"), 255));
					ps.setInt(12, Tools.getIntValue(request.getParameter("watermarkSaturation"), 0));
					ps.setString(13, request.getParameter("watermarkPlacement"));
				}
				else
				{
					ps.setString(11, "");
					ps.setInt(12, 0);
					ps.setString(13, "");
				}
				ps.setInt(14, CloudToolsForCore.getDomainId());
				ps.execute();
				ps.close();
				db_conn.close();
				db_conn = null;
				ps = null;
			}

			if(request == null || request.getAttribute("notGenerateAgain") == null) // ak sa nieco zmenilo v dimenziach, pregeneruj gallery opat
			{
				boolean recursive = true;
				if (request != null) recursive = "true".equals(request.getParameter("recursive"));
				if(recursive) updateDirectoryDim(dir, dim, dimNormal, resizeMode, recursive);

				resizePicturesInDirectory(dir, out, prop, resizeMode, recursive);
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	/**
	 * Upravi fyzicku velkost obrazkov v zadanom adresari
	 * @param dir - URL adresa adresara s obrazkami
	 * @param recursive - ak je true, aplikuje sa aj na podadresare
	 * @param prop - Prop objekt s prekladmi pre vypis informacie
	 * @param out - vystupny objekt pre vypis sprav, moze byt NULL
	 */
	public static void resizePicturesInDirectory(String dir, boolean recursive, Prop prop, PrintWriter out) {
		String resizeMode = getResizeMode(dir, true);
		resizePicturesInDirectory(dir, out, prop, resizeMode, recursive);
	}

	/**
	 * Upravi velkost obrazkov v zadanom adresari podla zadaneho rezimu
	 * @param dir
	 * @param out
	 * @param prop
	 * @param resizeMode
	 * @param recursive
	 */
	private static void resizePicturesInDirectory(String dir, PrintWriter out, Prop prop, String resizeMode, boolean recursive)
	{
		if (null != out)
		{
			out.println(prop.getText("components.gallery.resizing_images_in", dir));
		}
		Dimension[] dims = getDimension(dir);
		dir = Tools.replace(dir, "/", File.separator);
		IwcmFile directory = IwcmFile.fromVirtualPath("/" + dir);
		if (directory.exists() && directory.isDirectory())
		{
			IwcmFile[] fileList = directory.listFiles();
			for (IwcmFile file : fileList)
			{
				if (recursive && file.isDirectory())
					resizePicturesInDirectory(file.getVirtualPath(), out, prop, resizeMode, recursive);
				if (file.isFile() && !file.getName().startsWith("s_") && !file.getName().startsWith("o_"))
				{
					resizePictureImpl(dims, file.getAbsolutePath(), out, prop, resizeMode);
				}
			}
		}
	}

	/**
	 * Skopiruje subor oldFilePath na newFilePath
	 *
	 *@param oldFilePath
	 *           Description of the Parameter
	 *@param newFilePath
	 *           Description of the Parameter
	 *@return Description of the Return Value
	 */
	public static boolean copyFile(String oldFilePath, String newFilePath)
	{
		Logger.debug(GalleryDB.class, "copyFile " + oldFilePath + "->" + newFilePath);
		if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(oldFilePath)))
		{
			IwcmFsDB.copyTo(new IwcmFile(oldFilePath), new IwcmFile(newFilePath));
			return true;
		}
		try
		{
			File newFile = new File(newFilePath);
			File oldFile = new File(oldFilePath);
			if (newFile.exists()==false)
			{
				if(newFile.createNewFile() == false) return false;
				//if(newFile.delete() == false) return false;
			}
			FileInputStream inStream = new FileInputStream(oldFile);
			FileOutputStream out = new FileOutputStream(newFile);
			int c;
			byte[] buff = new byte[150000];
			while ((c = inStream.read(buff)) != -1)
			{
				out.write(buff, 0, c);
			}
			out.close();
			inStream.close();
			return (true);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (false);
	}

	public static Dimension[] getDimension(String dir)
	{
		return (getDimension(dir, true));
	}

	public static Dimension[] getDimension(String dir, boolean recursive)
	{
		return (getDimension(dir, recursive, true));
	}

	/**
	 * Ziskanie velkosti foldra
	 * @param dir
	 * @param recursive
	 * @param returnDefault - ak sa nic nenajde a doiteruje sa k root foldru vratia sa default hodnoty
	 * @return
	 */
	public static Dimension[] getDimension(String dir, boolean recursive, boolean returnDefault)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		Dimension dim = null;
		Dimension dimNormal = null;
		dir = dir.replace('\\', '/');
		try
		{
			if (!dir.equals(""))
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM gallery_dimension WHERE image_path=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, normalizeDir(dir));
				rs = ps.executeQuery();
				if (rs.next())
				{
					dim = new Dimension(rs.getInt("image_width"), rs.getInt("image_height"));
					dimNormal = new Dimension(rs.getInt("normal_width"), rs.getInt("normal_height"));
				}
				rs.close();
				ps.close();
				db_conn.close();
				db_conn = null;
				ps = null;
				rs = null;
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		if (recursive && (dim == null || dimNormal == null))
		{
			int index = dir.lastIndexOf('/');
			if (index > 1)
			{
				String parent = dir.substring(0, index);
				Logger.debug(GalleryDB.class, "Testujem gallery size parent: " + parent);
				return (getDimension(parent, recursive, returnDefault));
			}
			else if (returnDefault)
			{
				dim = new Dimension(160, 120);
				dimNormal = new Dimension(750, 560);
			}
		}
		Dimension[] dims = new Dimension[2];
		dims[0] = dim;
		dims[1] = dimNormal;
		return (dims);
	}

	public static String getResizeMode(String dir, boolean recursive)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		dir = dir.replace('\\', '/');
		String resizeMode = null;
		try
		{
			if (!dir.equals(""))
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM gallery_dimension WHERE image_path=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, normalizeDir(dir));
				rs = ps.executeQuery();
				if (rs.next())
				{
					resizeMode = rs.getString("resize_mode");
				}
				rs.close();
				ps.close();
				db_conn.close();
				db_conn = null;
				ps = null;
				rs = null;
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		if (recursive && (Tools.isEmpty(resizeMode)))
		{
			int index = dir.lastIndexOf('/');
			if (index > 1)
			{
				String parent = dir.substring(0, index);
				Logger.debug(GalleryDB.class, "Testujem gallery size parent: " + parent);
				return (getResizeMode(parent, recursive));
			}
			else
			{
				resizeMode = null;
			}
		}
		return (resizeMode);
	}

	public static String getResizeMode(String dir)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		String ret = null;
		dir = dir.replace('\\', '/');
		try
		{
			if (!dir.equals(""))
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT resize_mode FROM gallery_dimension WHERE image_path=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setString(1, normalizeDir(dir));
				rs = ps.executeQuery();
				if (rs.next())
				{
					ret = rs.getString("resize_mode");
				}
				rs.close();
				ps.close();
				db_conn.close();

				rs = null;
				ps = null;
				db_conn = null;
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return ret;
	}

	/**
	 * Zo zadaneho obrazku na serveri pripravi nahladovy a normalny obrazok
	 *
	 *@param realPath cesta k obrazku na serveri
	 *@param dir cesta k adresaru s nastavenim velkosti obrazkov galerie
	 */
	public static void resizePicture(String realPath, String dir)
	{
		Dimension[] dims = getDimension(dir);
		String resizeMode = getResizeMode(dir, true);
		resizePictureImpl(dims, realPath, null, null, resizeMode);
	}

	/**
	 * @param dims
	 * @param realPath
	 * @param out
	 * @param prop
	 * @return
	 */
	public static boolean resizePictureImpl(Dimension[] dims, String realPath, PrintWriter out, Prop prop)
	{
		IwcmFile actualDir = new IwcmFile(realPath);
		return resizePictureImpl(dims, realPath, out, prop, GalleryDB.getResizeMode(actualDir.getVirtualParent()));
	}

	/**
	 * mody: S - shrink to fit C - crop to fit A - presne rozmery W - presna
	 * sirka H - presna vyska iny - shrink to fit fyzicka zmena velkosti obrazka
	 *
	 * @param dims
	 * @param realPath
	 * @param out
	 */
	public static boolean resizePictureImpl(Dimension[] dims, String realPath, PrintWriter out, Prop prop, String resizeMode)
	{
		boolean ret = true;
		File img = new File(realPath);

		//uprava pre fotky zacinajuce na o_ alebo s_
		String imageName = img.getName();
		while(imageName.startsWith("o_") || imageName.startsWith("s_"))
		{
			imageName = imageName.substring(2);
		}
		if(!imageName.equals(img.getName()))
		{
			File renamedFile = new File(img.getParent()+File.separator+imageName);
			if(img.renameTo(renamedFile))
			{
				img = renamedFile;
				realPath = img.getParent()+File.separator+imageName;
			}
		}
		//koniec upravy

		String imgNameLC = img.getName().toLowerCase();

		if (!imgNameLC.endsWith(".jpg") && !imgNameLC.endsWith(".gif") && !imgNameLC.endsWith(".png")
					&& !imgNameLC.endsWith(".jpeg"))
		{
			return (false);
		}
		String realPathSmall = Tools.replace(realPath, img.getName(), "s_" + img.getName());
		String realPathOrig = Tools.replace(realPath, img.getName(), "o_" + img.getName());
		String resizeFrom = realPathOrig;
		Dimension dim = dims[0];
		Dimension dimNormal = dims[1];

		IwcmFile normal = new IwcmFile(realPath);

		//ak sa negeneruju miniatury, nevypisem hlasky o ich "generovani"
		boolean miniatures = true;
		if(resizeMode!=null && resizeMode.equals("N"))
			miniatures = false;


		if (dimNormal.width != 0 || dimNormal.height != 0)
		{
			// zisti ci mame original obrazok
			IwcmFile original = new IwcmFile(realPathOrig);
			if ((original.exists() == false || original.length()<1) && miniatures)
			{
				// skopiruj ho
				copyFile(realPath, realPathOrig);
			}
			if (out != null && prop != null && miniatures)
			{
				println(out, prop.getText("gallery.resizing.normal") + ": " + normal.getName());
			}
			// resizni
			int status = resizePicture(realPathOrig, realPath, dimNormal.getWidth(), dimNormal.getHeight(), resizeMode);
			if (status > 0)
			{
				ret = false;
				if (out != null && prop != null)
				{
					println(out, normal.getName() + ": <span class='error'>" + prop.getText("gallery.resizing.error_" + status)
								+ "</span>");
				}
			}
		}
		else
		{
			// zisti ci mame original obrazok
			IwcmFile original = new IwcmFile(realPathOrig);
			if (original.exists() == true && original.length()>0)
			{
				// skopiruj original na normal
				copyFile(realPathOrig, realPath);
				resizeFrom = realPath;
			}
			else
			{
				// vytvorim ho ako kopia normal do original
				copyFile(realPath, realPathOrig);
			}
			if (out != null && prop != null && miniatures)
			{
				println(out, prop.getText("gallery.resizing.normal") + ": " + normal.getName());
			}
		}
		if (out != null && prop != null && miniatures)
		{
			println(out, prop.getText("gallery.resizing.preview") + ": s_" + normal.getName());
		}
		int status = resizePicture(resizeFrom, realPathSmall, dim.getWidth(), dim.getHeight(), resizeMode);
		if (status > 0)
		{
			ret = false;
			if (out != null && prop != null)
			{
				println(out, normal.getName() + ": <span class='error'>" + prop.getText("gallery.resizing.error_" + status)
							+ "</span>");
			}
		}

		if(resizeMode!=null && resizeMode.equals("N"))
		{
			IwcmFile small = new IwcmFile(realPathSmall);
			if(small.exists())
				ret = small.delete() && ret;
			IwcmFile original = new IwcmFile(realPathOrig);
			if(original.exists())
				ret = original.delete() && ret;
		}

		return (ret);
	}

	/**
	 * fyzicka zmena velkosti obrazka
	 *
	 * @param dims
	 * @param realPathOrig
	 * @param out
	 * @param prop
	 */
	public static boolean resizePictureEdit(Dimension[] dims, String realPathOrig, PrintWriter out, Prop prop)
	{
		boolean ret = true;
		IwcmFile img = new IwcmFile(realPathOrig);

		String realPathSmall = Tools.replace(realPathOrig, img.getName(), "s_" + img.getName().substring(2));
		// odstran o_
		String realPath = Tools.replace(realPathOrig, img.getName(), img.getName().substring(2));
		String resizeFrom = realPath;
		Dimension dim = dims[0];
		Dimension dimNormal = dims[1];
		if (dimNormal.width != 0 && dimNormal.height != 0)
		{
			// zisti ci mame original obrazok
			IwcmFile original = new IwcmFile(realPathOrig);
			if (original.exists() == false)
			{
				// skopiruj ho
				copyFile(realPathOrig, realPath);
			}
			if (out != null && prop != null)
			{
				// println(out, prop.getText("gallery.resizing.normal") + ": " +
				// normal.getName());
			}
			// resizni
			int status = resizePicture(realPathOrig, realPath, dimNormal.getWidth(), dimNormal.getHeight());
			if (status > 0)
			{
				ret = false;
				if (out != null && prop != null)
				{
					// println(out, normal.getName() + ": <span class='error'>" +
					// prop.getText("gallery.resizing.error_" + status)+"</span>");
				}
			}
			resizeFrom = realPathOrig;
		}
		else
		{
			resizeFrom = realPathOrig;
			// skopiruj original na normal
			IwcmFile original = new IwcmFile(realPathOrig);
			if (original.exists() == true)
			{
				// skopiruj ho
				copyFile(realPathOrig, realPath);
			}
		}
		if (out != null && prop != null)
		{
			// println(out, prop.getText("gallery.resizing.preview") + ": s_" +
			// normal.getName());
		}
		int status = resizePicture(resizeFrom, realPathSmall, dim.getWidth(), dim.getHeight());
		if (status > 0)
		{
			ret = false;
			if (out != null && prop != null)
			{
				// println(out, normal.getName() + ": <span class='error'>" +
				// prop.getText("gallery.resizing.error_" + status)+"</span>");
			}
		}

		String resizeMode = getResizeMode(img.getVirtualParent(), true);
		if(resizeMode!=null && resizeMode.equals("N"))
		{
			IwcmFile small = new IwcmFile(realPathSmall);
			IwcmFile normal = new IwcmFile(realPath);
			if(small.exists())
				ret = small.delete() && ret;
			if(normal.exists())
				ret = normal.delete() && ret;
		}

		return (ret);
	}

	/**
	 * Resizne obrazok, vrati: 0-vsetko je OK 1-obrazok je uz teraz mensi
	 * 2-nepodarilo sa ulozit obrazok 3-nastala neznama chyba 4-obrazok je v
	 * nepodporovanom formate
	 *
	 *@param realPath
	 *           Description of the Parameter
	 *@param realPathSmall
	 *           Description of the Parameter
	 *@param width
	 *           Description of the Parameter
	 *@param height
	 *           Description of the Parameter
	 */
	public static int resizePicture(String realPath, String realPathSmall, double width, double height)
	{
		int status = resizePicture(realPath, realPathSmall, width, height, "");
		return status;
	}

	/**
	 * Prida vodotlac
	 *
	 * @param realPath Absolutna cesta k obrazku, nad ktorym sa bude prevadzat vodotlac
	 */
	private static void performWatermarking(String realPath)
	{
		//o_ stands for original image
		boolean shouldPerformWatermarking = (!realPath.contains("/o_") && existImageMagickCompositeCommand()) && Constants.getBoolean("galleryEnableWatermarking");
		if (shouldPerformWatermarking)
		{
			String galleryPath = new IwcmFile(realPath).getVirtualParent();
			GalleryDimension galleryContainingImage = getGalleryInfo(galleryPath, -1);
			if (galleryContainingImage!=null) {
				GalleryDimension watermark = findWaterMarkFor(galleryContainingImage);
				if (watermark.getWatermark() != null)
					addWatermarkSignature(realPath, watermark);
			}
		}
	}

	private static boolean existImageMagickCompositeCommand()
	{
		String filename = System.getProperty("os.name").toLowerCase().contains("win") ? "magick.exe" : "composite";
		return new IwcmFile(Constants.getString("imageMagickDir"), filename).exists();
	}

	/**
	 * Najde vodotlac aj s nastaveniami sytosti farieb a polohy. Hlada
	 * v zadanom adresari a vsetkych nadadresaroch az kym dosiahne /images/gallery
	 */
	public static GalleryDimension findWaterMarkFor(GalleryDimension leaf)
	{
		Logger.debug(GalleryDB.class, String.format("Watermark lookup for %s", leaf.getGalleryPath()));
		List<GalleryDimension> dirs = getDirectoriesToGalleryRoot(leaf);

		for (GalleryDimension dir : dirs)
		{
			if (dir.getWatermark() == null || isEmpty(dir.getGalleryPath()))
				continue;
			if (dir.getWatermark().exists())
			{
				Logger.debug(GalleryDB.class, String.format("Watermark found: %s", dir.getWatermark().getVirtualPath()));
				return dir;
			}
		}

		Logger.debug(GalleryDB.class, "No watermark found");
		//default if nothing is found
		GalleryDimension nullWatermark = new GalleryDimension();
		nullWatermark.setWatermarkPlacement(Constants.getString("galleryWatermarkGravity"));
		nullWatermark.setWatermarkSaturation(Constants.getInt("galleryWatermarkSaturation"));
		nullWatermark.setGalleryPath("/images/gallery");
		return nullWatermark;
	}

	private static List<GalleryDimension> getDirectoriesToGalleryRoot(GalleryDimension dir)
	{
		String pathToGallery = "/images/gallery"; //NOSONAR
		List<GalleryDimension> upDirs = new ArrayList<>();
		GalleryDimension currentDir = dir;
		while(currentDir != null && currentDir.getGalleryPath().startsWith(pathToGallery))
		{
			upDirs.add(currentDir);
			String nextGalleryPath = IwcmFile.fromVirtualPath(currentDir.getGalleryPath()).getVirtualParent();
			currentDir = getGalleryInfo(nextGalleryPath, -1);
		}
		return upDirs;
	}


	/**
	 * Samotny kod, ktory sposobi pridanie vodotlace do obrazku. Spolieha
	 * sa na kniznicu imageMagick a jej program "composite" s parametrom "-watermark"
	 * Cely prikaz vyzera podobne ako:
	 *  /usr/bin/composite -watermark 70x70 /watermark.png -gravity SouthWest /dest.jpg /dest.jpg
	 */
	private static void addWatermarkSignature(String realPath, GalleryDimension watermark)
	{
		List<String> args = new ArrayList<>();

		String filename = System.getProperty("os.name").toLowerCase().contains("win") ? "magick.exe" : "composite";
		String command = new IwcmFile(Constants.getString("imageMagickDir"), filename).getAbsolutePath();

		String watermarkPath = watermark.getWatermark().getAbsolutePath();
		IwcmFile imageFile = new IwcmFile(realPath);
		Logger.debug(GalleryDB.class, "imageFile="+imageFile.getName());
		if (imageFile.getName().startsWith("s_"))
		{
			//skus najst s_watermark subor
			watermarkPath = Tools.getRealPath(getImagePathSmall(watermark.getWatermark().getVirtualPath()));
			Logger.debug(GalleryDB.class, "testing S_watermark: "+watermarkPath);
		}

		args.add(command);
		if (filename.contains("magick.exe"))
			args.add("composite");
		args.add("-dissolve");
		args.add(watermark.getWatermarkSaturation()+"%");

		if (watermarkPath.endsWith(".svg"))
		{
			//je to SVG obrazok, musime zistit rozmer orig obrazku a prepocitat velkost watermarku
			ImageInfo ii = new ImageInfo(imageFile);
			int originalHeight = ii.getHeight();
			int svgHeight = (int)Math.round(originalHeight * (Constants.getInt("galleryWatermarkSvgSizePercent")/100d));
			if (svgHeight < Constants.getInt("galleryWatermarkSvgMinHeight")) svgHeight = Constants.getInt("galleryWatermarkSvgMinHeight");

			//pockaj na GFS
			MetadataCleaner.waitForGfs();

			args.add("-geometry");
			args.add("x"+svgHeight+"+0");
			args.add("-background");
			args.add("none");
		}


		args.add(watermarkPath);
		args.add("-gravity");
		args.add(watermark.getWatermarkPlacement());
		args.add(realPath); //zdrojovy subor
		args.add(realPath); //cielovy subor
		Logger.debug(GalleryDB.class, "Watermark executing:\n" + StringUtils.join(args.iterator(), ' '));

		try
		{
			Process pr = Runtime.getRuntime().exec(args.toArray(new String[0]));
			pr.waitFor();
		}
		catch (Exception e){sk.iway.iwcm.Logger.error(e);}

		//pridaj vodotlac aj do originalneho suboru, nielen do s_
		if (imageFile.getName().startsWith("s_"))
		{
			String originalPath = imageFile.getParent()+File.separatorChar+imageFile.getName().replace("s_", "");
			addWatermarkSignature(originalPath, watermark);
		}
	}

	/**
	 * Resizne obrazok, vrati: 0-vsetko je OK 1-obrazok je uz teraz mensi
	 * 2-nepodarilo sa ulozit obrazok 3-nastala neznama chyba 4-obrazok je v
	 * nepodporovanom formate
	 *
	 *@param realPath
	 *@param realPathSmall
	 *@param width
	 *@param height
	 *@param resizeMode
	 */
	public static int resizePicture(String realPath, String realPathSmall, double width, double height, String resizeMode)
	{
		if(resizeMode!=null && resizeMode.equals("N"))
		{
			IwcmFile fOrigImg = new IwcmFile(realPath);
			String imageName = fOrigImg.getName();
			if (imageName.startsWith("s_") || imageName.startsWith("o_"))
				imageName = imageName.substring(2);
			GalleryDB.setImage(fOrigImg.getVirtualParent(), imageName);
			return (0);
		}

		width = limitMaxSize(width);
		height = limitMaxSize(height);

		Logger.debug(GalleryDB.class, "resizePicture(" + realPath + "," + realPathSmall + "," + width + "," + height+", "+resizeMode);
		IwcmInputStream input=null;
		try
		{
			IwcmFile fSmallImg = new IwcmFile(realPathSmall);
			IwcmFile fOrigImg = new IwcmFile(realPath);

			if (Tools.isEmpty(resizeMode))
			{
				//skus vydedkukovat
				String dir = fOrigImg.getVirtualParent();
				resizeMode = getResizeMode(dir);
				Logger.debug(GalleryDB.class, "Forcing resize mode to:"+resizeMode+" for dir:"+dir);
			}

			String imageName = fOrigImg.getName();
			if (imageName.startsWith("s_") || imageName.startsWith("o_"))
				imageName = imageName.substring(2);

			if (realPathSmall.indexOf(Tools.replace(Constants.getString("thumbServletCacheDir"), "/", String.valueOf(File.separatorChar) ))==-1)
			{
				//toto vykoname len ak sa nejedna o thumb obrazok
				//ulozi obrazok do tabulky gallery, ak je uz ulozeny a nema nastaveny upload_datetime, tak ho nastavi

				//Check if it's pixabay
				String pixabayName = GalleryDB.getImagePathNormal( fSmallImg.getName() );
				if( Tools.isNotEmpty( GalleryService.getPixabayImageUrl(pixabayName, false) ) )
					imageName = pixabayName;

				GalleryDB.setImage(fOrigImg.getVirtualParent(), imageName);
			}

			String realPathLC = realPath.toLowerCase();
			if (fOrigImg.isFile() && FileTools.isImage(realPathLC))
			{
				double scaleFactor = 0.3;
				// opener = new Opener();
				// imp = opener.openImage(realPath);
				ImageInfo originalImage = new ImageInfo();
				input=new IwcmInputStream(realPath);
				originalImage.setInput(input);
				originalImage.check();
				if (originalImage.getWidth() < 1 || originalImage.getHeight() < 1)
					return (-4);
				// BufferedImage originalImage = ImageIO.read(fOrigImg);
				IwcmFile fDestDir = (new IwcmFile(realPathSmall)).getParentFile();
				if (fDestDir.exists() == false)
					fDestDir.mkdirs();
				int w = (int) width;
				int h = (int) height;
				// u crop suradnice laveho horneho rohu
				int x = 0;
				int y = 0;
				int pom_w = 0;
				int pom_h = 0;
				double pomer = (double) originalImage.getWidth() / (double) originalImage.getHeight();
				boolean resizeAndCrop = false;
				if ("A".equals(resizeMode))
				{// presny rozmer
				}
				else if ("C".equals(resizeMode))
				{
					// crop
					if (originalImage.getWidth() > w && originalImage.getHeight() > h)
					{
						// zmensit a orezat
						pom_w = w;
						pom_h = h;
						resizeAndCrop = true;
						h = (int) Math.round(w * 1 / pomer);
						if (h < pom_h)
						{
							h = pom_h;
							w = (int) Math.round(h * pomer);
							// orezeme so sirky
							y = 0;
							x = (w - pom_w) / 2;
						}
						else
						{
							// orezeme s vysky
							x = 0;
							y = (h - pom_h) / 2;
						}
					}
					else if (w > originalImage.getWidth() && h <= originalImage.getHeight())
					{
						// orezat z vysky
						x = 0;
						w = originalImage.getWidth();
						y = (originalImage.getHeight() - h) / 2;
					}
					else if (w <= originalImage.getWidth() && h > originalImage.getHeight())
					{
						// orezat so sirky
						y = 0;
						h = originalImage.getHeight();
						x = (originalImage.getWidth() - w) / 2;
					}
					else
					{

						copyFile(realPath, realPathSmall);
						//convertCmykToRgb(realPathSmall);
						stripExif(realPathSmall);

						return (1);
					}
					/*
					 * } else { w = originalImage.getWidth(); }
					 * if(originalImage.getHeight() > h) { y =
					 * (originalImage.getHeight() - h)/2; } else { h =
					 * originalImage.getHeight(); }
					 */
				}
				else if ("W".equals(resizeMode))
				{// presna sirka
					h = (int) Math.round(w * 1 / pomer);
				}
				else if ("H".equals(resizeMode))
				{// presna vyska
					w = (int) Math.round(h * pomer);
				}
				else
				{
					// ak je obrazok uz teraz mensi, nerob nic
					if (originalImage.getWidth() < w && originalImage.getHeight() < h)
					{

						// skopiruj original na novy (ak bola nejaka operacia v
						// ImageTools)
						copyFile(realPath, realPathSmall);
						// skonvertuj do RGB ak treba a je nastavene
						//convertCmykToRgb(realPathSmall);

						stripExif(realPathSmall);
						return (1);
					}
					if (width < 0 && height < 0)
					{
						w = (int) -width;
						h = (int) -height;
					}
					else if (width > 1 && height > 1)
					{
						scaleFactor = (width * 1.0) / (originalImage.getWidth() * 1.0);
						int newHeight = (int) (originalImage.getHeight() * scaleFactor);
						if (newHeight > height)
						{
							scaleFactor = (height * 1.0) / (originalImage.getHeight() * 1.0);
							w = (int) (originalImage.getWidth() * scaleFactor);
							h = (int) height;
						}
						else
						{
							w = (int) width;
							h = (int) (originalImage.getHeight() * scaleFactor);
						}
					}
					else if (width > 1)
					{
						scaleFactor = (width * 1.0) / (originalImage.getWidth() * 1.0);
						// Logger.println(GalleryDB.class,"scaleFactor: " +
						// scaleFactor);
						w = (int) width;
						h = (int) (originalImage.getHeight() * scaleFactor);
					}
					else if (height > 1)
					{
						scaleFactor = (height * 1.0) / (originalImage.getHeight() * 1.0);
						w = (int) (originalImage.getWidth() * scaleFactor);
						h = (int) height;
					}
				}

				w = GalleryDBTools.limitMaxSize(w);
				h = GalleryDBTools.limitMaxSize(h);

				if (fSmallImg.exists())
				{
					//netreba, convert subor prepise fSmallImg.delete();
				}
				String imageMagickDir = getImageMagicDir();
				// mame ho aj dostupny
				boolean convertExists = false;
				String runtimeFile = getRuntimeFile();

				if (Tools.isNotEmpty(imageMagickDir))
				{
					File f = new File(imageMagickDir + File.separatorChar + runtimeFile);
					if (f.exists() && f.canRead())
					{
						convertExists = true;
					}
				}
				if (Tools.isNotEmpty(imageMagickDir)
							&& (originalImage.getWidth() > 500 || originalImage.getHeight() > 500 || Constants
										.getBoolean("galleryAlwaysUseImageMagick")) && convertExists)
				{
					Logger.debug(GalleryDB.class, "executing image magick: " + imageMagickDir + File.separatorChar + runtimeFile);
					Runtime rt = Runtime.getRuntime();
					File resizePomFile = null;
					List<String> args = null;
					boolean galleryStripExif = Constants.getBoolean("galleryStripExif");

					String pripona = realPathSmall.substring(realPathSmall.lastIndexOf('.'));

					if ("C".equals(resizeMode))
					{
						// crop image
						if (resizeAndCrop)
						{
							// zmensit a orezat
							// longCmd = imageMagickDir + File.separatorChar +
							// runtimeFile + " '" + realPath + "' -resize " + w + "x" +
							// h + "! '" + realPathSmall+"_pomresize'";
							String pomresize = Tools.replace(realPathSmall, pripona, ".pomresize" + pripona);
							args = new ArrayList<>();

							if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPath)))
							{
								IwcmFsDB.writeFileToDisk(new File(realPath),new File(IwcmFsDB.getTempFilePath(realPath)));
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(IwcmFsDB.getTempFilePath(realPath));
								args.add("-resize");
								args.add(w + "x" + h + "!");
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(IwcmFsDB.getTempFilePath(pomresize));
							}
							else
							{
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(realPath);
								args.add("-resize");
								args.add(w + "x" + h + "!");
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(pomresize);
							}

							StringBuilder params = new StringBuilder();
							if (args != null)
							{
								for (int i = 0; i < args.size(); i++)
								{
									params.append(' ').append(args.get(i));
								}
							}
							Logger.debug(GalleryDB.class, "LONGCMD:\n" + params);
							Process proc = rt.exec(args.toArray(new String[0]));
							InputStream stderr = proc.getErrorStream();
							BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
							String line = null;
							while ((line = br.readLine()) != null)
							{
								Logger.debug(GalleryDB.class, line);
							}
							br.close();
							int exitValue = proc.waitFor();
							Logger.debug(GalleryDB.class, "ExitValue: " + exitValue);
							// longCmd = imageMagickDir + File.separatorChar +
							// runtimeFile +" '" + realPathSmall+"_pomresize" + "' '" +
							// realPath + "' -crop " + w + "x" + h + "+"+ x + "+" + y +
							// " -crop " + w + "x" + h + "-"+ x + "-" + y +" '"+
							// realPathSmall+"'";

							args = new ArrayList<>();
							if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPath)))
							{
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(IwcmFsDB.getTempFilePath(pomresize));
								args.add("-crop");
								args.add((int) width + "x" + (int) height + "+" + x + "+" + y);
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(IwcmFsDB.getTempFilePath(realPathSmall));
							}
							else
							{
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(pomresize);
								// args[2] = realPath;
								args.add("-crop");
								args.add((int) width + "x" + (int) height + "+" + x + "+" + y);
								// args[5] = "-crop";
								// args[6] = w + "x" + h + "-"+ x + "-" + y;
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(realPathSmall);
							}
							resizePomFile = new File(pomresize);
						}
						else
						{
							// iba orezat
							// longCmd = imageMagickDir + File.separatorChar +
							// runtimeFile + " '" + realPath + "' -crop " +
							// originalImage.getWidth() + "x" +
							// originalImage.getHeight() + "+"+ x + "+" + y + " -crop "
							// + originalImage.getWidth() + "x" +
							// originalImage.getHeight() + "-"+ x + "-" + y +" '"+
							// realPathSmall+"'";
							args = new ArrayList<>();
							if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPath)))
							{
								IwcmFsDB.writeFileToDisk(new File(realPath),new File(IwcmFsDB.getTempFilePath(realPath)));
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(IwcmFsDB.getTempFilePath(realPath));
								args.add("-crop");
								args.add((int) width + "x" + (int) height + "+" + x + "+" + y);
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(IwcmFsDB.getTempFilePath(realPathSmall));
							}
							else
							{
								args.add(imageMagickDir + File.separatorChar + runtimeFile);
								args.add(realPath);
								args.add("-crop");
								args.add((int) width + "x" + (int) height + "+" + x + "+" + y);
								// args[5] = "-crop";
								// args[6] = originalImage.getWidth() + "x" +
								// originalImage.getHeight() + "-"+ x + "-" + y;
								if (galleryStripExif) args.add("-strip");
								if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
								args.add(realPathSmall);
							}
						}
					}
					else
					{
						// longCmd = imageMagickDir + File.separatorChar + runtimeFile
						// + " '" + realPath + "' -resize " + w + "x" + h + "! '" +
						// realPathSmall+"'";
						args = new ArrayList<>();
						if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPath)))
						{
							IwcmFsDB.writeFileToDisk(new File(realPath),new File(IwcmFsDB.getTempFilePath(realPath)));
							args.add(imageMagickDir + File.separatorChar + runtimeFile);
							args.add(IwcmFsDB.getTempFilePath(realPath));
							args.add("-resize");
							args.add(w + "x" + h + "!");
							if (galleryStripExif) args.add("-strip");
							if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
							if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPathSmall))) args.add(IwcmFsDB.getTempFilePath(realPathSmall));
							else args.add(realPathSmall);
						}
						else
						{
							args.add(imageMagickDir + File.separatorChar + runtimeFile);
							args.add(realPath);
							args.add("-resize");
							args.add(w + "x" + h + "!");
							if (galleryStripExif) args.add("-strip");
							if ("gif".equalsIgnoreCase(pripona)) args.add("+repage");
							args.add(realPathSmall);
						}
					}
					String params = "";
					if (args != null)
					{
						StringBuilder buf = new StringBuilder();
						for (int i = 0; i < args.size(); i++)
						{
							buf.append(' ').append(args.get(i));
						}
						params = buf.toString();
					}
					Logger.debug(GalleryDB.class, "LONGCMD:\n" + params);
					Process proc = rt.exec(args.toArray(new String[0]));
					Logger.debug(GalleryDB.class, "executed");
					InputStream stderr = proc.getErrorStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
					String line = null;
					while ((line = br.readLine()) != null)
					{
						Logger.debug(GalleryDB.class, line);
					}
					br.close();
					int exitValue = proc.waitFor();
					Logger.debug(GalleryDB.class, "ExitValue: " + exitValue);

					if (resizePomFile != null)
						if(resizePomFile.delete() == false) return 3;

					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(realPathSmall)))
					{
						IwcmFsDB.writeFileToDB(new File(IwcmFsDB.getTempFilePath(realPathSmall)), new File(realPathSmall));
						if(new File(IwcmFsDB.getTempFilePath(realPathSmall)).delete() == false) return 3;
						if(new File(IwcmFsDB.getTempFilePath(realPath)).delete() == false) return 3;
					}

					if (exitValue == 0)
					{
						performWatermarking(realPathSmall);
						return (0);
					}
					else
						return (3);
				}
				else
				{
					IwcmInputStream iwStream =  new IwcmInputStream(fOrigImg.getPath(), false);
					BufferedImage originalBufferedImage = ImageIO.read(iwStream);
					iwStream.close();
					// Logger.println(GalleryDB.class,"w="+w+" h="+h);
					int scaleType = Image.SCALE_AREA_AVERAGING;
					if (originalImage.getWidth() > 1000)
					{
						Logger.debug(GalleryDB.class, "smooth resize");
						scaleType = Image.SCALE_SMOOTH;
					}
					// toto je pre ThumbServlet
					if (realPathSmall.indexOf(Constants.getString("thumbServletCacheDir")) != -1)
						scaleType = Image.SCALE_FAST;
					if (originalImage.getWidth() > 2000)
					{
						// Logger.debug(GalleryDB.class,"smooth fast");
						// scaleType = Image.SCALE_FAST;
					}
					DebugTimer timer = new DebugTimer("ImageResize");
					timer.diff("starting: " + scaleType);
					Image smallImage = null;
					if ("C".equals(resizeMode))
					{// crop image
						if (resizeAndCrop)
						{// zmensit a orezat
							smallImage = originalBufferedImage.getScaledInstance(w, h, scaleType);
							BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
							Graphics2D g = bi.createGraphics();
							g.drawImage(smallImage, 0, 0, null);
							smallImage = bi.getSubimage(x, y, pom_w, pom_h);
							w = pom_w;
							h = pom_h;
						}
						else
						{// iba orezat
							smallImage = originalBufferedImage.getSubimage(x, y, w, h);
						}
					}
					else
					{
						smallImage = originalBufferedImage.getScaledInstance(w, h, scaleType);
					}
					timer.diff("scaled");
					BufferedImage bufSmall = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
					timer.diff("buf created");
					// bufSmall.getGraphics().drawImage(smallImage, 0, 0, null);
					bufSmall.createGraphics().drawImage(smallImage, 0, 0, null);
					timer.diff("image drawed");
					bufSmall.getGraphics().dispose();
					timer.diff("disposed");
					try
					{
						ImageWriteParam iwparam = null;
						// ImageIO.write(bufSmall, format,iwparam, fSmallImg);
						// Jimi.putImage("image/" + type, image, realPathSmall);
						ImageWriter writer = null;

						String format = "jpg";
                        if (realPathLC.endsWith(".png"))
                        {
                            format = "png";
                        }
                        else
                        {
                            iwparam = new JPEGImageWriteParam(null);
                            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            iwparam.setCompressionQuality(0.85F);
                        }

						Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
						if (iter.hasNext())
						{
							writer = iter.next();
						}
						if (writer != null) {
							// Prepare output file
							IwcmOutputStream out = new IwcmOutputStream(fSmallImg.getPath());
							ImageOutputStream ios = ImageIO.createImageOutputStream(out);
							writer.setOutput(ios);
							timer.diff("write start");
							// Write the image
							writer.write(null, new IIOImage(bufSmall, null, null), iwparam);
							timer.diff("writed to file");
							// Cleanup
							ios.flush();
							writer.dispose();
							ios.close();
							out.close();
						}
						return (0);
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
						return (2);
					}
				}
			}
		}
		catch (javax.imageio.IIOException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return (4);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			if (input!=null)
			{
				try
				{
				input.close();
				}
				catch (Exception e) {
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}
		return (3);
	}

	/**
	 * @return - image magick runtime file by current Operating System
	 */
	public static String getRuntimeFile()
	{
		String result = "convert";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
		{
			result  = "convert.exe";
		}
		return result ;
	}

	public static String getImageMagicDir()
	{
		return Constants.getString("imageMagickDir");
	}

	/**
	 * Vrati linku na obrazok so zadanym prefixom
	 *
	 * @param prefix
	 * @param fullPath
	 * @return
	 */
	public static String getImagePathPrefix(String prefix, String fullPath)
	{
		return GalleryToolsForCore.getImagePathPrefix(prefix,fullPath);
	}

	/**
	 * Vrati malu verziu obrazku z galerie (ak existuje)
	 *
	 * @param fullPath
	 * @return
	 */
	public static String getImagePathSmall(String fullPath)
	{
		return GalleryToolsForCore.getImagePathSmall(fullPath);
	}

	/**
	 * Vrati velku verziu obrazku z galerie (ak existuje)
	 *
	 * @param fullPath
	 * @return
	 */
	public static String getImagePathNormal(String fullPath)
	{
		return GalleryToolsForCore.getImagePathNormal(fullPath);
	}

	public static String getImagePathOriginal(String fullPath)
	{
		return GalleryToolsForCore.getImagePathOriginal(fullPath);
	}

	/**
	 * Normalizuje cestu k adresaru (v DB v tabulke gallery_dimension), odstrani
	 * koncove lomitko
	 *
	 * @param url
	 * @return
	 */
	private static String normalizeDir(String url)
	{
		if (url.endsWith("/"))
			return (url.substring(0, url.length() - 1));
		return (url);
	}

	/**
	 * Vytvori adresar galerie so zadanymi rozmermi (nastavi ich do databazy)
	 *
	 * @param url
	 * @param width
	 * @param height
	 * @param normalWidth
	 * @param normalHeight
	 * @return
	 */
	public static boolean createGalleryFolder(String url, int width, int height, int normalWidth, int normalHeight)
	{
		IwcmFile f = new IwcmFile(Tools.getRealPath(url));
		f.mkdirs();
		Adminlog.add(Adminlog.TYPE_GALLERY, "Create gallery folder: url= " + url + " w=" + width + " h=" + height + " nw=" + normalWidth + " nh=" + normalHeight, -1, -1);
		Dimension dim = new Dimension(width, height);
		Dimension normalDim = new Dimension(normalWidth, normalHeight);
		changeDimension(Constants.getServletContext(), url, dim, normalDim, null, null, null);
		return (false);
	}

	/**
	 * Otestuje, ci zadany adresar je galeria
	 *
	 * @param galleryFolderUrl
	 * @return
	 */
	public static boolean isGalleryFolder(String galleryFolderUrl)
	{
		galleryFolderUrl = galleryFolderUrl.replace('\\', '/');
		if (galleryFolderUrl.indexOf("/" + Constants.getString("galleryDirName")) != -1)
			return (true);
		// skus zistit, ci mame dany adresar v databaze
		Dimension[] dim = getDimension(galleryFolderUrl, true, false);
		if (dim[0] != null && dim[1] != null)
			return (true);
		return (false);
	}
	/**
	 * Increments send_count for image identified by imgPath
	 * @param imgPath
	 */
	public static void incSendCount(HttpServletRequest request, String imgPath)
	{


		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;

		ResultSet rs  = null;
		int imageId = -1;
		try
		{

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("select image_id from gallery where image_path = ? and image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1,imgPath.substring(0,imgPath.lastIndexOf('/')));
			ps.setString(2, imgPath.substring(imgPath.lastIndexOf('/')+1));
			rs=ps.executeQuery();

			if (rs.next())
			{
				imageId=rs.getInt("image_id");
			}
			rs.close();
			rs = null;
			ps.close();
			if (imageId>0) //ak existuje robime update
			{
				ps = db_conn.prepareStatement("update gallery set send_count = send_count + 1 where image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
				ps.setInt(1, imageId);
				ps.executeUpdate();
			}
			else //inak insert noveho zaznamu
			{
				ps = db_conn.prepareStatement("insert into gallery  (image_path,image_name,send_count, domain_id) values (?,?,1,?)");

				ps.setString(1, imgPath.substring(0,imgPath.lastIndexOf('/')));
				ps.setString(2, imgPath.substring(imgPath.lastIndexOf('/')+1));
				ps.setInt(3, CloudToolsForCore.getDomainId());
				ps.executeUpdate();

				//update pkey generator
				PkeyGenerator.getNextValue("gallery");
			}

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	/**
	 * Test, ci je mozne pohladnicu poslat na zadany email
	 * @param email - email adresa prijemcu
	 * @param gBean - gallery bean
	 * @return
	 */
	public static boolean canSendToEmail(String email, GalleryBean gBean)
	{
		if (Tools.isEmail(email)==false) return false;
		if (gBean == null) return false;
		if (Tools.isEmpty(gBean.getAllowedDomains())) return true;

		email = email.toLowerCase();
		StringTokenizer st = new StringTokenizer(gBean.getAllowedDomains(), ",+\n");
		while (st.hasMoreTokens())
		{
			String domain = st.nextToken().toLowerCase();
			if (email.endsWith(domain)) return true;
		}

		return false;
	}

	/**
	 * Vrati atribut perex_group obrazka s identifikatorom imageId v String tvare, cize nieco ako ,45,12,13, kde cisla su identifikatory perex skupin
	 *
	 * @param imageId	identifikator obrazka, pre ktory chceme zistit jeho perex skupiny. Musi byt vacsi ako 0.
	 * @return
	 */
	public static String getImagePerexGroupString(int imageId)
	{
		return (new SimpleQuery().forString("SELECT perex_group FROM gallery WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), imageId));
	}

	/**
	 * Nastavi atribut perex_group podla perexGroupString obrazku s identifikatorom imageId
	 * @param perexGroupString		retazec v tvare ,perexGroupId,perexGroupId,perexGroupId... nieco ako ,15,16,7,
	 * @param imageId					identifikator obrazku, ktoremu chceme nastavit pozadovany perexGroupString
	 */
	public static void setImagePerexGroupString(String perexGroupString, int imageId)
	{
		if (imageId < 1)
			return;
		try
		{
			new SimpleQuery().execute("UPDATE gallery SET perex_group = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), perexGroupString, imageId);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Prida existujucemu obrazku s identifikatorom taggedImageInt identifikator perex skupiny tagPerexGroupId.
	 * Obsahuje kontrolu, ci uz obrazok perexGroupu nahodou neobsahuje. Ak ano, tak ju nepridava.
	 *
	 * @param 	taggedImageInt	Identifikator obrazka, ktoremu sa doplni perex skupina
	 * @param 	tagPerexGroupId	Identifikator perex skupiny, ktora sa doplni k uz existujucim perex skupina obrazka
	 *
	 * @return	Vrati true ak sa operacia podarila, inak vrati false
	 */
	public static boolean addPerexGroup(int taggedImageInt, int tagPerexGroupId)
	{
		try
		{
   		String existPerexGroupString = GalleryDB.getImagePerexGroupString(taggedImageInt);
   		if (Tools.isEmpty(existPerexGroupString))
   			existPerexGroupString = ",";
   		if (existPerexGroupString.indexOf("," + tagPerexGroupId + ",") == -1)	 //kontrola, ci uz danu perexGroupu nahodou neobsahuje. Ak ano, tak nepridavam
   			GalleryDB.setImagePerexGroupString((existPerexGroupString + tagPerexGroupId + ","), taggedImageInt);
   		return true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
	}

	/**
	 * Odoberie existujucemu obrazku s identifikatorom taggedImageInt identifikator perex skupiny tagPerexGroupId.
	 * Ak obrazok danu perex skupinu neobsahuje, nevykona sa nic a vrati sa true.
	 *
	 * @param 	taggedImageInt	Identifikator obrazka, ktoremu sa odoberie perex skupina
	 * @param 	tagPerexGroupId	Identifikator perex skupiny, ktora sa odoberie z uz existujucich perex skupina obrazka
	 *
	 * @return	Vrati true ak sa operacia podarila, inak vrati false
	 */
	public static boolean removePerexGroup(int taggedImageInt, int tagPerexGroupId)
	{
		try
		{
   		String existPerexGroupString = GalleryDB.getImagePerexGroupString(taggedImageInt);
   		if (Tools.isEmpty(existPerexGroupString))
   			return true;

   		if (existPerexGroupString.indexOf("," + tagPerexGroupId + ",") != -1)	 //odstrani skupinu, len ak ju obsahuje
   			GalleryDB.setImagePerexGroupString((existPerexGroupString.replace(tagPerexGroupId + ",", "")), taggedImageInt);
   		if (GalleryDB.getImagePerexGroupString(taggedImageInt).length() < 3)// ak bola nastavena iba jedna skupina, zmaz vsetky zvysne ciarky
   			GalleryDB.setImagePerexGroupString("", taggedImageInt);
   		return true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
	}

	/**
	 * Funkcia vrati nazov perex skupiny podla jeho identifikatora
	 *
	 * @param perexGroupId	identifikator skupiny, ktorej nazov chceme zistit
	 * @return
	 */
	public static String getPerexGroupName(int perexGroupId)
	{
		return new SimpleQuery().forString("SELECT perex_group_name FROM perex_groups WHERE perex_group_id = ?", perexGroupId);
	}

	/**
	 * Funkcia, ktora z retazca identifikatorov perex skupin (v tvare napr. ,12,15,78,2,) vrati retazec ich zodpovedajucich nazvov (interway, auto, kvety)
	 *
	 * @param perexGroupString retazec pozostavajuci z identifikatorov jednotlivych perex skupin oddelenych ciarkami - napr. ,12,15,78,2,
	 * @return
	 */
	public static String getPerexGroupNameString(String perexGroupString)
	{
		StringBuilder retValue = new StringBuilder();
		String[] perexGroups = convertPerexGroupString(perexGroupString);
		if (perexGroups != null && perexGroups.length > 0)
      	for (String perexGroup : perexGroups)
      		retValue.append(GalleryDB.getPerexGroupName(Tools.getIntValue(perexGroup, -1))).append(", ");

		return retValue.substring(0, (retValue.length()-2));		//odreze poslednu medzeru a ciarku na konci
	}

	public static String convertPerexGroupString(String[] perexGroup)
	{
		StringBuilder ret = new StringBuilder();
		try
		{
			if (perexGroup!=null)
			{
				int size = perexGroup.length;
				int i;
				for (i=0; i<size; i++)
				{
					ret.append(",").append(perexGroup[i]);
				}
				ret.append(",");
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		//Logger.println(GalleryBean.class, "getPerexGroupString = " + ret);

		return(ret.toString());
	}

	public static String[] convertPerexGroupString(String perexGroupString)
	{
		String[] perexGroup = null;

		if (perexGroupString == null)
			perexGroupString = "";

		StringTokenizer st = new StringTokenizer(perexGroupString, ",+_");

		try
		{
			perexGroup = new String[st.countTokens()];
			int i=0;
			while (st.hasMoreTokens())
			{
				int pGroupId = Tools.getIntValue(st.nextToken().trim(), -1);
				perexGroup[i++] = Integer.toString(pGroupId);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return perexGroup;
	}

	public static void updateSelectedInfo(int imageId, int selectedX, int selectedY, int selectedWidht, int selectedHeight)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE gallery SET selected_x=?, selected_y=?, selected_width=?, selected_height=? WHERE image_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			int index = 1;
			ps.setInt( index++,selectedX);
			ps.setInt( index++,selectedY);
			ps.setInt( index++,selectedWidht);
			ps.setInt(index++,selectedHeight);
			ps.setInt(index++,imageId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}

	/**
	 * Sprav crop z obrazka "from" podla zadanych parametrov a zapise ho do  "to"
	 * POZOR: Ignoruje DB storage
	 * @param from
	 * @param width
	 * @param height
	 * @param left
	 * @param top
	 * @param to
	 * @return
	 */
	public static int crop(IwcmFile from, int width,int height,int left,int top,IwcmFile to)
	{
		if (!to.getParentFile().exists())
		{
			to.getParentFile().mkdirs();
		}

		String[] args = new String[5];
		args[1]=from.getAbsolutePath();
		args[2]="-crop";
		args[3]=width+"x"+height+"+"+left+"+"+top;
		args[4]=to.getAbsolutePath();
		return executeImageMagickCommand(args);
	}


	/**
	 * Spusti imageMagic s parametrami v args, args[0] sa nastavi automaticky podla cesty k ImageMagic
	 * @param args
	 */
	public static int executeImageMagickCommand(String[] args)
	{
		args[0] = getImageMagicDir()+File.separatorChar+getRuntimeFile();
		Runtime rt = Runtime.getRuntime();
		Process process = null;
		try
		{
			//odstraneny if nie je potrebny
			StringBuilder params = new StringBuilder();
//			if (args != null)
//			{
				for (int i = 0; i < args.length; i++)
				{
					params.append(' ').append(args[i]);
				}
//			}
			Logger.debug(GalleryDB.class, "LONGCMD:\n" + params);

			process= rt.exec(args);

			InputStream stderr = process.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
			String line = null;
			while ((line = br.readLine()) != null)
			{
				Logger.debug(GalleryDB.class, line);
			}
			br.close();

			int ret = process.waitFor();
			//convert vrati 1 namiesto 0 lebo pri -debug hlasi, ze destination subor neexistuje/nepozna .new priponu
			if (ret==1) ret = 0;

			return ret;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return -1;
	}

	/**
	 * Matoda, ktora zmeni urcitu vlastnost obrazka, ak obrazok neexistuje, vytvori novy s pozadovanymi hodnotami
	 *
	 * @param imageId	identifikator obrazka, ktoreho vlastnosti chceme zmenit
	 * @param item		vlastnost obrazka, ktoru chceme zmenit prepisanim starej hodnoty na novu value
	 * @param value	hodnota, ktoru chceme zapisat do vlastnosti item obrazka
	 */
	public static boolean updateImageItem(int imageId, String item, String value, String imagePath, String imageName, String lng)
	{
		//System.out.println("IMAGEID NA ZMENU::: " + imageId +" - " + imagePath + " - " + imageName);
		//System.out.println("DEBUG: JAZYK NA ZMENU::: " + imageId +" - " + lng);

		if(item == null || value == null)
			return false;

		if (imageId < 0)
			imageId = GalleryDB.setImage(imagePath, imageName);

		if (imageId < 0)
			return false;

		try
		{
			if ("short".equals(item))
				new SimpleQuery().execute("UPDATE gallery SET s_description_" + lng + " = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), DB.prepareString(EditorDB.escapeInvalidCharacters(value), 255), imageId);
			else if ("long".equals(item))
				new SimpleQuery().execute("UPDATE gallery SET l_description_" + lng + " = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), EditorDB.escapeInvalidCharacters(value), imageId);
			else if ("author".equals(item))
				new SimpleQuery().execute("UPDATE gallery SET author = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), DB.prepareString(value, 255), imageId);
			else if ("priority".equals(item))
				new SimpleQuery().execute("UPDATE gallery SET sort_priority = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), Integer.parseInt(value), imageId);
			else if ("upload".equals(item))
				new SimpleQuery().execute("UPDATE gallery SET upload_datetime = ? WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), new Timestamp(Long.parseLong(value)), imageId);

			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return false;
	}

	/**
	 * Matoda, ktora zmeni urcitu vlastnost galerie
	 *
	 * @param path	identifikator galerie - cesta k jej priecinku, ktorej vlastnosti chceme zmenit
	 * @param item		vlastnost galerie, ktoru chceme zmenit prepisanim starej hodnoty na novu value
	 * @param value	hodnota, ktoru chceme zapisat do vlastnosti item galerie
	 */
	public static boolean updateGalleryItem(String path, String item, String value)
	{
		//System.out.println("IMAGEID NA ZMENU::: " + imageId +" - " + imagePath + " - " + imageName);
		//System.out.println("DEBUG: JAZYK NA ZMENU::: " + imageId +" - " + lng);

		if(item == null || value == null)
			return false;

		if (Tools.isEmpty(path))
			return false;

		int galleryId = GalleryDB.setGallery(path);

		if (galleryId > 0)
		{
   		try
   		{
   			if ("short".equals(item))
   				new SimpleQuery().execute("UPDATE gallery_dimension SET gallery_name = ? WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), DB.prepareString(value, 255), galleryId);
   			else if ("long".equals(item))
   				new SimpleQuery().execute("UPDATE gallery_dimension SET gallery_perex = ? WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), value, galleryId);
   			else if ("author".equals(item))
   				new SimpleQuery().execute("UPDATE gallery_dimension SET author = ? WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), DB.prepareString(value, 255), galleryId);

   		}
   		catch (Exception e)
   		{
   			sk.iway.iwcm.Logger.error(e);
   			return false;
   		}

   		return true;
		}
		return false;
	}

	/**
	 * Vrati hodnotu obrazka v galerii, ktora uz existuju v tabulke na zaklade jazyka
	 *
	 * @param imageId identifikator obrazka
	 * @param item		polozka, vlastnost, ktoru chceme zistit
	 * @param lng		jazyk
	 *
	 * @return
	 */
	public static String getDescriptionLng(int imageId, String item, String lng)
	{
		String retValue = "";
		try
		{
   		if ("short".equals(item))
   			retValue = new SimpleQuery().forString("SELECT s_description_" + lng + " FROM gallery WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), imageId);
   		if ("long".equals(item))
   			retValue = new SimpleQuery().forString("SELECT l_description_" + lng + " FROM gallery WHERE image_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), imageId);
   	}
   	catch (Exception e)
   	{
   	}
   	if (retValue == null)
   		retValue = "";

   	return retValue;
	}


	/**
	 * malo by sa pouzivat getGalleryInfo(String galleryPath, int dimensionId)
	 *
	 *@deprecated
	 */
	@Deprecated
	public static GalleryDimension getGalleryInfo(String galleryPath)
	{
		return getGalleryInfo(galleryPath, -1);
	}

	/**
	 * upravil bhric, aby sa dali ziskat info aj pomocou ID galerie
	 *
	 * Metoda vrati informacie o galerie presnejsie jej nazov, id, perex, datum vytvorenia, pocet videni a autorovi
	 *
	 * @param galleryPath	cesta k priecinku fotogalerie, podla ktorej sa identifikuje
	 * @param dimensionId	ID galerie, zhodne s dimension_id - pokial je mensie ako 1, tak vyhlada podla galleryPath
	 * @return vrati bean GalleryDimension naplneny informaciami
	 */
	public static GalleryDimension getGalleryInfo(String galleryPath, int dimensionId)
	{
		GalleryDimension emptyDimensionIfNothingIsFound = new GalleryDimension();
		emptyDimensionIfNothingIsFound.setGalleryPath(galleryPath);
		String sql = "";
		if(dimensionId > 0)
			sql = "SELECT * FROM gallery_dimension WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true);
		else if(Tools.isNotEmpty(galleryPath))
			sql = "SELECT * FROM gallery_dimension WHERE image_path = ?"+CloudToolsForCore.getDomainIdSqlWhere(true);
		else
			return null;

		ComplexQuery query = new ComplexQuery().setSql(sql);
		if (dimensionId > 0)
			query.setParams(dimensionId);
		else
			query.setParams(galleryPath);

		List<GalleryDimension> dimensions = query.list(dimensionMapper);
		return dimensions.size() > 0 ? dimensions.get(0) : emptyDimensionIfNothingIsFound;
	}

	/**
	 * Metoda vrati informacie o count poslednych galeriach
	 *
	 * @param count pocet fotogalerii, ktore sa ma vratit
	 * @return vrati List GalleryDimension naplneny informaciami
	 */
	public static List<GalleryDimension> getLastGalleriesInfo(int count)
	{
		String sql = null;

		switch (Constants.DB_TYPE)
		{
			case Constants.DB_MYSQL : sql = "SELECT * FROM gallery_dimension WHERE create_date IS NOT NULL"+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY create_date DESC LIMIT "+count;
				break;
			case Constants.DB_PGSQL : sql = "SELECT * FROM gallery_dimension WHERE create_date IS NOT NULL"+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY create_date DESC LIMIT "+count;
				break;
			case Constants.DB_MSSQL : sql = "SELECT TOP "+count+" * FROM gallery_dimension WHERE create_date IS NOT NULL"+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY create_date DESC";
				break;
			case Constants.DB_ORACLE : sql = "SELECT GD.*, ROWNUM FROM gallery_dimension GD WHERE create_date IS NOT NULL"+CloudToolsForCore.getDomainIdSqlWhere(true)+" AND ROWNUM < "+count+" ORDER BY create_date DESC";
				break;
			default: throw new IllegalStateException("Unknown database type");
		}

		return new ComplexQuery().setSql(sql).list(dimensionMapper);
	}

	/**
	 * Funkcia vrati GalleryDimensionBeany, ktore obsahuju aspon jednu fotku otagovanu danou perex skupinou a tie budu usporiadane podla datumu vytvorenia
	 *
	 * @param perexGroupId	identifikator skupiny, ktorej aspon jedna fotka sa ma v galerii vyskytovat
	 * @param count			pocet galerii, ktore sa maju vratit
	 *
	 * @return
	 */
	public static List<GalleryDimension> getGalleriesPathByPerexGroup(int perexGroupId, int count)
	{
		List<GalleryDimension> retList = new ArrayList<>();
		String perexGroupSql = "%," + perexGroupId + ",%";

		try
		{
			@SuppressWarnings("unchecked")
			List<String> galleriesPaths = new SimpleQuery().forList("SELECT DISTINCT image_path FROM gallery WHERE gallery.perex_group LIKE ?"+CloudToolsForCore.getDomainIdSqlWhere(true), perexGroupSql);
			for (String galleryPath : galleriesPaths)
			{
				GalleryDimension gd = getGalleryInfo(galleryPath, -1);
				retList.add(gd);
			}

		}
		catch (Exception e)
		{
		}

		try
		{
			Collections.sort(retList, new Comparator<GalleryDimension>()
			{
				@Override
				public int compare(GalleryDimension gd1, GalleryDimension gd2)
				{
					return (gd2.getGalleryDate().compareTo(gd1.getGalleryDate()));
				}
			});
		}
		catch (Exception ex)
		{
			//sk.iway.iwcm.Logger.error(ex);
		}

		if (retList.isEmpty())
			return retList;

		if (count > retList.size())
			return retList;

		return retList.subList(0, count);
	}

	/**
	 * Funkcia vrati GalleryBean prveho obrazka, ktory patri do zadanej fotogalerie podla jej identifikatora
	 *
	 * @param galleryPath	cesta galerie, z ktorej sa ma vybrat prvy obrazok na zaklade abecedneho poradia
	 *
	 * @return
	 */
	public static GalleryBean getFirstImageFromGalleryByPerexGroupFromDB(String galleryPath)
	{
		GalleryBean firstImage = new GalleryBean();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String limitSQLAfter = "";
		String limitSqlBefore = "";
		String whereCondition = "";

		if(Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL)
			limitSQLAfter = " LIMIT 1";

		else if(Constants.DB_TYPE == Constants.DB_MSSQL)
			limitSqlBefore = " TOP " + 1;

		else if(Constants.DB_TYPE == Constants.DB_ORACLE)
			whereCondition = " AND LINENUM BETWEEN 0 AND 1";

		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement("SELECT"+limitSqlBefore+" image_id, image_path, image_name FROM gallery WHERE image_path = ?"+whereCondition+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY image_name ASC " + limitSQLAfter);
			int psCounter = 1;
			ps.setString(psCounter++, galleryPath);

			rs = ps.executeQuery();

			while (rs.next())
			{
				firstImage.setImageId(rs.getInt("image_id"));
				firstImage.setImagePath(rs.getString("image_path"));
				firstImage.setImageName(rs.getString("image_name"));
			}

			rs.close();
			ps.close();
			db_conn.close();

			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return firstImage;
	}

	/**
	 * Funkcia vrati GalleryBean prveho obrazka, ktory patri do zadanej fotogalerie na zaklade jej adresara
	 *
	 * @param galleryPath	cesta galerie, z ktorej sa ma vybrat prvy obrazok na zaklade abecedneho poradia
	 *
	 * @return
	 */
	public static GalleryBean getFirstImageFromGalleryByPerexGroupFromDir(String galleryPath)
	{
		GalleryBean firstImage = new GalleryBean();

		String realPath = sk.iway.iwcm.Tools.getRealPath(galleryPath);
		IwcmFile[] fileList;
		IwcmFile file = null;

		file = new IwcmFile(realPath);
		fileList = file.listFiles(new IwcmFileFilter()
		{
			@Override
			public boolean accept(IwcmFile pathname)
			{
				return (isImageFast(pathname))
							&& pathname.getName().startsWith("s_");
			}
		});

   	if (fileList == null)
   		fileList = new IwcmFile[0];
   	try
   	{
   		Arrays.sort(fileList, new Comparator<IwcmFile>()
   		{
   			@Override
				public int compare(IwcmFile f1, IwcmFile f2)
   			{
   				return (f1.getName().compareTo(f2.getName()));
   			}
   		});
   	}
   	catch (Exception ex)
   	{
   		sk.iway.iwcm.Logger.error(ex);
   	}

   	if (fileList.length > 0)
   	{
   		firstImage.setImagePath(fileList[0].getVirtualParent());
   		firstImage.setImageName(fileList[0].getName().substring(2));
   	}
   	else
   		return null;

   	return firstImage;
   	}

   /**
	 * zvysi pocet videni o jeden
	 * @param dimId - zhodne s dimension_id
	 */
	public static void addGalleryViews(int dimId)
	{
		int views = 0;
		GalleryDimension info = getGalleryInfo(null, dimId);
		if(info == null || info.getGalleryId() < 1)
			return;
		try
		{
			views = DB.queryForInt("SELECT views FROM gallery_dimension WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), Integer.valueOf(dimId));
			++views;
		}
		catch (IllegalStateException e)
		{
			views = 1;
		}
		DB.execute("UPDATE gallery_dimension SET views = ? WHERE dimension_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), Integer.valueOf(views), Integer.valueOf(dimId));
	}

	public static void stripExif(String filePath)
	{
		if (Constants.getBoolean("galleryStripExif") == false) return;

		String imageMagickDir = getImageMagicDir();
		boolean convertExists = false;
		String runtimeFile = getRuntimeFile();

		try
		{
			if (Tools.isNotEmpty(imageMagickDir))
			{
				File f = new File(imageMagickDir + File.separatorChar + runtimeFile);
				if (f.exists() && f.canRead())
				{
					convertExists = true;
				}
			}
			if (Tools.isNotEmpty(imageMagickDir) && convertExists)
			{
				Logger.debug(GalleryDB.class, "executing image magick: " + imageMagickDir + File.separatorChar + runtimeFile);
				Runtime rt = Runtime.getRuntime();

				String[] args = new String[4];

				if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filePath)))
				{
					IwcmFsDB.writeFileToDisk(new File(filePath),new File(IwcmFsDB.getTempFilePath(filePath)));
					args[0] = imageMagickDir + File.separatorChar + runtimeFile;
					args[1] = IwcmFsDB.getTempFilePath(filePath);
					args[2] = "-strip";
					args[3] = IwcmFsDB.getTempFilePath(filePath);
				}
				else
				{
					args[0] = imageMagickDir + File.separatorChar + runtimeFile;
					args[1] = filePath;
					args[2] = "-strip";
					args[3] = filePath;
				}

				StringBuilder params = new StringBuilder();
				for (int i = 0; i < args.length; i++)
				{
					params.append(' ').append(args[i]);
				}
				Logger.debug(GalleryDB.class, "LONGCMD:\n" + params);
				Process proc = rt.exec(args);
				InputStream stderr = proc.getErrorStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
				String line = null;
				while ((line = br.readLine()) != null)
				{
					Logger.debug(GalleryDB.class, line);
				}
				br.close();
				int exitValue = proc.waitFor();
				Logger.debug(GalleryDB.class, "ExitValue: " + exitValue);
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}
	/**
	 * Vrati statistiku odoslani obrazkov
	 *
	 * @param imagePath	cesta obrazka, ktory sa ma vyhladat
	 * @param alsoEmpty  ci ma vratit aj nulove pocty
	 * @return zoznam objektov GalleryBean
	 */
	public static List<GalleryBean> getAllSendCount(String imagePath,boolean alsoEmpty)
	{
		List<Object> params = new ArrayList<>();

   	StringBuilder sql = new StringBuilder();
		sql.append("SELECT image_name, send_count, image_path, image_id, image_path FROM gallery WHERE image_id > 0"+CloudToolsForCore.getDomainIdSqlWhere(true));

   	if (Tools.isNotEmpty(imagePath))
   	{
   		sql.append(" AND image_path LIKE ?");
   		params.add("%" + imagePath + "%");
   	}
   	if(!alsoEmpty){
   		sql.append(" AND send_count > 0 ");
   	}
   	sql.append(" ORDER BY send_count DESC");

      List<GalleryBean> imageBeans = new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(new Mapper<GalleryBean>()
      {
         @Override
			public GalleryBean map(ResultSet rs) throws SQLException
         {
         	GalleryBean imageBean = new GalleryBean();

         	imageBean.setImagePath(rs.getString("image_path"));
         	imageBean.setImageName(rs.getString("image_name"));
				imageBean.setImageId(rs.getInt("image_id"));
				imageBean.setSendCount(rs.getInt("send_count"));

         	return imageBean;
         }
      });

		return imageBeans;
	}

	/**
	 * Vrati statistiku odoslani obrazkov, aj tie ktore maju pocet odoslani 0
	 * @param imagePath	cesta obrazka, ktory sa ma vyhladat
	 * @return zoznam objektov GalleryBean
	 */
	public static List<GalleryBean> getAllSendCount(String imagePath)
	{
		return getAllSendCount(imagePath,true);
	}

	/**
	 * Vrati datum a cas uploadu obrazku z fotogalerie na server
	 *
	 * @param dir	adresar, v ktorom sa obrazok nachadza
	 * @param name	nazov obrazka aj s priponou
	 *
	 * @return datum a cas nahratia obrazku na server, null vrati v pripade, ze sa nepodari spojenie s DB alebo obrazok nema nastaveny upload_datetime
	 */
	public static Date getUploadDateImage(String dir, String name)
	{
		try
		{
			return new SimpleQuery().forDate("SELECT upload_datetime FROM gallery WHERE image_path = ? AND image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), dir, name);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return null;
		}
	}

	/**
	 * Nastavi obrazku, ktory nema ulozeny datum uploadu, datum poslednej modifikacie suboru
	 *
	 * @param dir			adresar obrazka
	 * @param name			meno obrazka
	 * @param uploadDate	datum, ktory sa ma nastavit ako datum uploadu
	 *
	 * @return true ak sa operacia podarila, inak false
	 */
	public static boolean setUploadDateImage(String dir, String name, long uploadDate)
	{
		try
		{
			new SimpleQuery().execute("UPDATE gallery SET upload_datetime = ? WHERE image_path = ? AND image_name = ?"+CloudToolsForCore.getDomainIdSqlWhere(true), new Timestamp(uploadDate), dir, name);
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}

	/**
	 * Konvertuje obrazok na JPG
	 * @param f
	 */
	public static void convertToJPG(IwcmFile f)
	{
		String[] args = new String[]{"imagemagick", "from", "to"};
		String filePath = f.getAbsolutePath();
		String extension = FileTools.getFileExtension(filePath);
		try
		{
			if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filePath)))
			{
				IwcmFsDB.writeFileToDisk(new File(filePath), new File(IwcmFsDB.getTempFilePath(filePath)));
				args[1] = IwcmFsDB.getTempFilePath(filePath);
				args[2] = IwcmFsDB.getTempFilePath(filePath.replace("." + extension, ".jpg"));
			}
			else
			{
				args[1] = filePath;
				args[2] = filePath.replace("." + extension, ".jpg");
			}
			executeImageMagickCommand(args);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * pokus o ziskanie datumu vytvorenia JPG fotky z EXIF metadat
	 * @param file
	 * @return - v pripade ze nenejade, vrati aktualny datum/cas
	 */
	public static Date getExifDateOriginal(IwcmFile file)
	{
		if (Constants.getBoolean("galleryEnableExifDate")==false) return null;

		try
		{
			if(file != null && (file.getName().toLowerCase().endsWith("jpg") || file.getName().toLowerCase().endsWith("jpeg")))
			{
				Metadata metadata = null;
				if(file != null)
				{
					try
					{
						IwcmInputStream is = new IwcmInputStream(file);
						metadata = ImageMetadataReader.readMetadata(is);
						is.close();
						//metadata = ImageMetadataReader.readMetadata(bis, waitForBytes);
					}
					catch (Exception ex2)
					{
						sk.iway.iwcm.Logger.error(ex2);
						metadata = null;
					}
				}

				if(metadata != null)
				{
					for(Directory dir : metadata.getDirectories())
					{
						//adredar ktory drzi EXIF metedata
						if("Exif SubIFD".equals(dir.getName()))
						{
							for(Tag tag : dir.getTags())
							{
								if("Date/Time Original".equals(tag.getTagName()))
								{
									//datum je v tvare 2002:07:13 15:58:28, takze to musime rozparsovat a dat do spravneho tvaru. teda 13.07.2002 15:58:28
									String[] dateTime = Tools.getTokens(tag.getDescription(), " ");
									if(dateTime != null && dateTime.length == 2)
									{
										String[] datum = Tools.getTokens(dateTime[0], ":");
										if(datum != null && datum.length == 3 && Tools.getIntValue(datum[2],-1) != -1 && Tools.getIntValue(datum[1],-1) != -1 && Tools.getIntValue(datum[0],-1) != -1)
										{
											Date date = new Date(DB.getTimestamp(datum[2]+datum[1]+datum[0], dateTime[1]));
											return date;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return null;
	}

	public static boolean existsImageMagickConvertCommand()
	{
		String imageMagickDir = getImageMagicDir();
		boolean convertExists = false;
		String runtimeFile = getRuntimeFile();

		if (Tools.isNotEmpty(imageMagickDir))
		{
			File f = new File(imageMagickDir + File.separatorChar + runtimeFile);
			if (f.exists() && f.canRead())
			{
				convertExists = true;
			}
		}
		return convertExists;
	}

	/**
	 * Vrati hodnotu pre parameter quality z ImageMagick convert prikazu, hodnota sa preberie bud z parametra q alebo z konstanty galleryImageQuality
	 * Vrati -1 ak sa ma pouzit automaticky mod (parameter quality pre convert sa nenastavi)
	 * @param request
	 * @param imageWidth
	 * @return
	 */
	public static int getImageQuality(HttpServletRequest request, int imageWidth)
	{
		int imageQuality = -1;

		if (request != null)
		{
			int qParam = Tools.getIntValue(request.getParameter("q"), -1);
			if (qParam > 10 && qParam <= 100) imageQuality = qParam;
		}

		if (imageQuality == -1)
		{
			String galleryImageQuality = Constants.getString("galleryImageQuality");
			if (Tools.isNotEmpty(galleryImageQuality))
			{
				StringTokenizer st = new StringTokenizer(galleryImageQuality, ";");
				while (st.hasMoreTokens())
				{
					String token = st.nextToken();
					try
					{
						int index = token.indexOf(':');
						if (index != -1)
						{
							int w = Tools.getIntValue(token.substring(0, index), -1);
							int q = Tools.getIntValue(token.substring(index+1), -1);
							if (w > 0 && q > 0 && w <= imageWidth)
							{
								imageQuality = q;
							}
						}
					} catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}
			}
		}

		return imageQuality;
	}

	public static boolean isVideo(GalleryBean gBean)
	{
		String fileName=gBean.getImageName();
		int index = fileName.lastIndexOf(".");
		String videoName = gBean.getImagePath()+"/"+fileName.substring(0,index)+".mp4";
		//File f = new File(gBean.getImagePath()+"/"+videoName);
		IwcmFile f = new IwcmFile(Tools.getRealPath(videoName));
		if (f.exists())
		{
			return true;
		}
		return false;
	}

	/**
	 * Premenuje obrazok v galerii vratane upravy databazy (tabulka gallery a media) a pridania presmerovania
	 * @param originalUrl
	 * @param newUrl
	 * @return
	 */
	public static boolean renameFile(String originalUrl, String newUrl)
	{
		Logger.debug(GalleryDB.class, "renameFile orig="+originalUrl+" new="+newUrl);

		//skontroluj priponu
		String origExtension = FileTools.getFileExtension(originalUrl).toLowerCase();
		String newExtension = FileTools.getFileExtension(newUrl).toLowerCase();

		if (origExtension.equalsIgnoreCase(newExtension)==false)
		{
			newUrl = newUrl + "." + origExtension;
			Logger.debug(GalleryDB.class, "Adding extension: "+origExtension+" newUrl="+newUrl);
		}

		boolean moved = false;
		try
		{
			moved = FileTools.moveFile(originalUrl, newUrl);
			if (moved)
			{
				Adminlog.add(Adminlog.TYPE_GALLERY, "Rename file from="+originalUrl+" to="+newUrl, -1, -1);

				//presun subor iba ak existuje
				if (GalleryDB.getImagePathOriginal(originalUrl).startsWith("o_")) FileTools.moveFile(GalleryDB.getImagePathOriginal(originalUrl), GalleryDB.getImagePathPrefix("o_", newUrl));
				if (GalleryDB.getImagePathSmall(originalUrl).startsWith("s_")) FileTools.moveFile(GalleryDB.getImagePathSmall(originalUrl), GalleryDB.getImagePathPrefix("s_", newUrl));

				//otestuj aj video subory
				String videoFileName = FileTools.getFilePathWithoutExtension(originalUrl)+".mp4";
				IwcmFile f = new IwcmFile(Tools.getRealPath(videoFileName));
				if (f.exists())
				{
					FileTools.moveFile(videoFileName, FileTools.getFilePathWithoutExtension(newUrl)+".mp4");
				}
				videoFileName = FileTools.getFilePathWithoutExtension(originalUrl)+".flv";
				f = new IwcmFile(Tools.getRealPath(videoFileName));
				if (f.exists())
				{
					FileTools.moveFile(videoFileName, FileTools.getFilePathWithoutExtension(newUrl)+".flv");
				}

				//presun data v databaze
				updateGalleryBeanLinkInDatabase(originalUrl, newUrl);

				if ( isGalleryFolder(newUrl) )
				{
					String BASE_DIR = newUrl.substring(0, newUrl.lastIndexOf("/")+1);
					Prop prop = Prop.getInstance();
					Dimension[] dims = GalleryDB.getDimension(BASE_DIR);
					GalleryDB.resizePictureImpl(dims, Tools.getRealPath(newUrl), null, prop, GalleryDB.getResizeMode(BASE_DIR));
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return moved;
	}

	private static void updateGalleryBeanLinkInDatabase(String originalUrl, String newUrl)
	{
		String originalDir = originalUrl.substring(0, originalUrl.lastIndexOf("/"));
		String originalFile = originalUrl.substring(originalUrl.lastIndexOf("/")+1);

		String newDir = newUrl.substring(0, newUrl.lastIndexOf("/"));
		String newFile = newUrl.substring(newUrl.lastIndexOf("/")+1);

		(new SimpleQuery()).execute("DELETE FROM gallery WHERE image_path=? AND image_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true), newDir, newFile);
		(new SimpleQuery()).execute("UPDATE gallery SET image_path=?, image_name=? WHERE image_path=? AND image_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true), newDir, newFile, originalDir, originalFile);
		(new SimpleQuery()).execute("UPDATE documents SET perex_image=? WHERE perex_image=?"+(InitServlet.isTypeCloud() ? (" "+StatDB.getRootGroupWhere("group_id", CloudToolsForCore.getDomainId())) : ""), newDir+"/"+newFile, originalDir+"/"+originalFile);
		(new SimpleQuery()).execute("UPDATE documents SET perex_image=? WHERE perex_image=?"+(InitServlet.isTypeCloud() ? (" "+StatDB.getRootGroupWhere("group_id", CloudToolsForCore.getDomainId())) : ""), GalleryDB.getImagePathPrefix("o_", newDir+"/"+newFile), GalleryDB.getImagePathPrefix("o_", originalDir+"/"+originalFile));
		(new SimpleQuery()).execute("UPDATE documents SET perex_image=? WHERE perex_image=?"+(InitServlet.isTypeCloud() ? (" "+StatDB.getRootGroupWhere("group_id", CloudToolsForCore.getDomainId())) : ""), GalleryDB.getImagePathPrefix("s_", newDir+"/"+newFile), GalleryDB.getImagePathPrefix("s_", originalDir+"/"+originalFile));

		MediaDB.updateLink(originalUrl, newUrl);
		MediaDB.updateThumb(originalUrl, newUrl);
		EditorDB.replaceUrl(originalUrl, newUrl, null);

		MediaDB.updateLink(GalleryDB.getImagePathPrefix("o_", originalUrl), GalleryDB.getImagePathPrefix("o_", newUrl));
		MediaDB.updateThumb(GalleryDB.getImagePathPrefix("o_", originalUrl), GalleryDB.getImagePathPrefix("o_", newUrl));
		EditorDB.replaceUrl(GalleryDB.getImagePathPrefix("o_", originalUrl), GalleryDB.getImagePathPrefix("o_", newUrl), null);

		MediaDB.updateLink(GalleryDB.getImagePathPrefix("s_", originalUrl), GalleryDB.getImagePathPrefix("s_", newUrl));
		MediaDB.updateThumb(GalleryDB.getImagePathPrefix("s_", originalUrl), GalleryDB.getImagePathPrefix("s_", newUrl));
		EditorDB.replaceUrl(GalleryDB.getImagePathPrefix("s_", originalUrl), GalleryDB.getImagePathPrefix("s_", newUrl), null);
	}

	/**
	 * Pomocna metoda na test suboru, ci sa jedna o obrazok, testuje na zaklade pripony
	 * ak je zapnute galleryUseFastLoading netestuje, ci sa realne jedna o subor alebo adresar, testuje len priponu
	 * pouziva sa pri clustri a pripojenom pomalom NAS
	 * @param file
	 * @return
	 */
	private static boolean isImageFast(IwcmFile file)
	{
		//ak je vypnute fast otestuj, ci sa nejedna o adresar, ak ano, nemoze to byt obrazok
		if (Constants.getBoolean("galleryUseFastLoading")==false && file.isFile()==false) return false;

		String suffix = FileTools.getFileExtension(file.getName()).toLowerCase();
		if ("png".equals(suffix) || "jpg".equals(suffix) || "jpeg".equals(suffix) || "gif".equals(suffix))
		{
			return true;
		}
		return false;
	}

	/**
	 * Pomocna metoda na test suboru, ci sa jedna o adresar
	 * ak je zapnute galleryUseFastLoading netestuje, ci sa realne jedna o subor alebo adresar, testuje len priponu
	 * pouziva sa pri clustri a pripojenom pomalom NAS
	 * @param file
	 * @return
	 */
	private static boolean isDirectoryFast(IwcmFile file)
	{
		//ak je vypnute fast vratime rovno ci je to adresar
		if (Constants.getBoolean("galleryUseFastLoading")==false) return file.isDirectory();

		String suffix = FileTools.getFileExtension(file.getName()).toLowerCase();
		if ("png".equals(suffix) || "jpg".equals(suffix) || "jpeg".equals(suffix) || "gif".equals(suffix) || "bmp".equals(suffix) || "zip".equals(suffix))
		{
			return false;
		}
		return file.isDirectory();
	}

	private static double limitMaxSize(double size)
	{
		if (size > 5000) return 5000;
		return size;
	}

	/**
	 * Metoda zmaze celu galeriu vratane podadresarov a vsetkych medii v ramci aktualnej domeny
	 * @param path cesta ku galerii (napr /images/gallery/nejakaGaleria)
	 * @return true ak sa podari zmazat vsetky media a vratane podadresarov, inak false
	 * @throws IllegalArgumentException if path is empty or null
	 */
	public static boolean deleteGallery(String path)
	{
		boolean success = false;
		if(Tools.isEmpty(path)) throw new IllegalArgumentException("path can't be empty");
		String galleryRootFolder = getGalleryRootFolder();
		if(path.equals(galleryRootFolder))
		{
			Logger.debug(GalleryDB.class,"Root gallery folder, skipping...");
			return success;
		}
		if(isGalleryFolder(path))
		{
			Logger.debug(GalleryDB.class, "Deleting gallery " + path + " ...");
			Map<String, GalleryBean> beans = getGalleryBeanTable(path, "_sk", true);
			Logger.debug(GalleryDB.class, "Deleting images...");
			for(GalleryBean b : beans.values())
			{
				removeImage(b);
			}
			Logger.debug(GalleryDB.class, "Deleted " + beans.size() + " images");
			new SimpleQuery().execute("DELETE from gallery_dimension WHERE image_path LIKE ? "+CloudToolsForCore.getDomainIdSqlWhere(true), path+"%");
			try{
				Logger.debug(GalleryDB.class, "Deleting directory: " + Tools.getRealPath(path));
				FileUtils.deleteDirectory(new File(Tools.getRealPath(path)));
				success = true;
				Logger.debug(GalleryDB.class, "Gallery "+path+" deleted");
			}
			catch(IOException e){
				Logger.debug(GalleryDB.class, "Failed to delete gallery " + path + " reason: "+e.getMessage());
			}
		}
		else
		{
			Logger.debug(GalleryDB.class, path + " is not a gallery folder, skipping...");
		}
		return success;
	}

	/**
	 * Vrati relative path to root adresar galerie
	 * @return
	 */
	public static String getGalleryRootFolder()
	{
		return Constants.getString("imagesRootDir") + "/" + Constants.getString("galleryDirName");
	}

	/**
	 * Vymaze gallery bean z DB aj fyzicky subory vo vsetkych velkostiach
	 * @param b
	 */
	private static void removeImage(GalleryBean b)
	{
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		IwcmFile file = null;
		String sql = "DELETE FROM gallery WHERE image_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true);
		String dir = b.getImagePath();
		String name = b.getImageName();
		int imageId = b.getImageId();
		if (dir.startsWith("/")==false) dir = "/"+dir;
		if (name.startsWith("/")) name = name.substring(1);

		if (imageId < 1) imageId = getImageId(dir, name);

		String path = Tools.getRealPath(dir);
		try
		{
			if (imageId > 0)
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, imageId);
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;
			}
			file = new IwcmFile(path + File.separator + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
			file = new IwcmFile(path + File.separator + "s_" + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
			file = new IwcmFile(path + File.separator + "o_" + name);
			if (file.exists())
			{
				boolean ok = file.delete();
				Logger.debug(GalleryDB.class, "Deleting file="+file.getAbsolutePath()+" ok="+ok);
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
	}

	/**
	 * V databaze nastavi rozmer a rezim zmeny velkosti podadresarom podla nastaveneho adresara
	 * @param dir
	 */
	public static void updateDirectoryDimToSubfolders(String dir) {
		Dimension[] dims = getDimension(dir, true);
		String resizeMode = getResizeMode(dir, true);
		updateDirectoryDim(dir, dims[0], dims[1], resizeMode, true);
	}

	private static void updateDirectoryDim(String dir, Dimension dim, Dimension dimNormal, String resizeMode, boolean recursive)
	{
		IwcmFile directory = IwcmFile.fromVirtualPath("/" + dir);
		if (directory.exists() && directory.isDirectory())
		{
			IwcmFile[] fileList = directory.listFiles();
			for (IwcmFile file : fileList)
			{
				if (file.isDirectory())
				{
					java.sql.Connection db_conn = null;
					java.sql.PreparedStatement ps = null;

					try
					{
						if (!dir.equals(""))
						{
							db_conn = DBPool.getConnection();
							ps = db_conn
								.prepareStatement("UPDATE gallery_dimension SET image_width=?, image_height=?, normal_width=?, normal_height=?, resize_mode=? WHERE image_path=?");
							ps.setInt(1, dim.width);
							ps.setInt(2, dim.height);
							ps.setInt(3, dimNormal.width);
							ps.setInt(4, dimNormal.height);
							ps.setString(5, resizeMode);
							ps.setString(6, normalizeDir(file.getVirtualPath()));
							ps.execute();
							ps.close();
							db_conn.close();
							db_conn = null;
							ps = null;
						}
					}
					catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
					finally{
						try{
							if (ps != null) ps.close();
							if (db_conn != null) db_conn.close();
						}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
					}

					if(recursive)
						updateDirectoryDim(normalizeDir(file.getVirtualPath()), dim, dimNormal, resizeMode, recursive);
				}
			}
		}
	}

	/**
	 * aplikuje na "obrazok" vodotlac podla domeny
	 * @param obrazok
	 */
	public static void applyWatermarkOnUpload(IwcmFile obrazok)
	{
		if(obrazok == null || obrazok.exists() == false) return;
		String suffix = FileTools.getFileExtension(obrazok.getName().toLowerCase());
		boolean shouldPerformWatermarking = existImageMagickCompositeCommand() && Constants.getBoolean("galleryWatermarkApplyOnUpload")
														&& ("png".equals(suffix) || "jpg".equals(suffix) || "jpeg".equals(suffix));

		String[] exceptions = Tools.getTokens(Constants.getString("galleryWatermarkApplyOnUploadExceptions"), ",");
		if (exceptions != null && exceptions.length>0)
		{
			for (String exception : exceptions)
			{
				if (obrazok.getAbsolutePath().contains(exception))	shouldPerformWatermarking = false;
			}
		}

		if (shouldPerformWatermarking)
		{
			Logger.debug(GalleryDB.class, "(applyWatermarkOnUpload) absolutePath="+obrazok.getAbsolutePath());
			GalleryDimension watermark = new GalleryDimension();
			watermark.setWatermarkPlacement(Constants.getString("galleryWatermarkGravity"));
			watermark.setWatermarkSaturation(Constants.getInt("galleryWatermarkSaturation"));

			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			String galleryWatermarkApplyOnUploadDir = Constants.getStringExecuteMacro("galleryWatermarkApplyOnUploadDir");
			if(galleryWatermarkApplyOnUploadDir.endsWith("/") == false) galleryWatermarkApplyOnUploadDir += "/";
			//skusim skontrolovat obrazok pre vodotlac podla domeny
			IwcmFile wmFile = new IwcmFile(Tools.getRealPath(galleryWatermarkApplyOnUploadDir+rb.getDomain()+".svg"));
			//ak nemam domenovy, pouzijem default
			if(wmFile.exists() == false) wmFile = new IwcmFile(Tools.getRealPath(galleryWatermarkApplyOnUploadDir+"default.svg"));
			if(wmFile.exists() == false)
			{
				Logger.debug(GalleryDB.class, "(applyWatermarkOnUpload) nemam watermark obrazok {domena="+rb.getDomain()+"}: "+wmFile.getVirtualPath());
				return;
			}
			watermark.setWatermark(wmFile.getVirtualPath());
			addWatermarkSignature(obrazok.getAbsolutePath(), watermark);
		}
	}

	/**
	 * @deprecated - use GalleryDBTools.getImageSize
	 */
	@Deprecated
	public static int[] getImageSize(IwcmFile imageFile){
		return GalleryDBTools.getImageSize(imageFile);
    }

	/**
	 * @deprecated - use GalleryDBTools.cropAndResize
	 */
    @Deprecated
	public static int cropAndResize(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality, int ip)
	{
		return GalleryDBTools.cropAndResize(from,cwidth,cheight,cleft,ctop,finalWidth,finalHeight,fillColor,exactFinalSize,to,imageQuality,ip);
	}
}