package sk.iway.iwcm.sync.inport;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.GalleryDimension;
import sk.iway.iwcm.sync.export.Content;

/**
 *  GalleryImporter.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.6.2012 17:38:43
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class GalleryImporter
{

	public static List<ContentGalleryBean.Info> getGalleryInfos(Content content)
	{
		List<ContentGalleryBean.Info> infoBeans = new ArrayList<ContentGalleryBean.Info>();
		if (null == content) return infoBeans;

		for (Numbered<Content.GalleryInfo> remoteInfo : Numbered.list(content.getGalleryInfos()))
		{
			Content.GalleryInfo localInfo = getLocalGalleryInfo(remoteInfo.item);
			infoBeans.add(new ContentGalleryBean.Info(remoteInfo.number, remoteInfo.item, localInfo));
		}
		return infoBeans;
	}

	public static List<ContentGalleryBean.Image> getGalleryImages(Content content, HttpServletRequest request)
	{
		List<ContentGalleryBean.Image> imageBeans = new ArrayList<ContentGalleryBean.Image>();
		if (null == content) return imageBeans;

		for (Numbered<Map<String, GalleryBean>> remoteImageTranslations : Numbered.list(content.getGalleryImages()))
		{
			GalleryBean remoteImage = remoteImageTranslations.item.values().iterator().next();
			GalleryBean localImage = getLocalGalleryImage(remoteImage, request);
			imageBeans.add(new ContentGalleryBean.Image(remoteImageTranslations.number, remoteImage, localImage));
		}
		return imageBeans;
	}

	public static void importGalleries(HttpServletRequest request, Content content)
	{
		if (null == content) return;
		boolean hasCloudShop = "cloud".equals(Constants.getInstallName()) && CloudToolsForCore.hasShop(request);

		for (Numbered<Content.GalleryInfo> remoteInfo : Numbered.list(content.getGalleryInfos()))
		{
			if (null != request.getParameter("galleryInfo_" + remoteInfo.number) || request.getAttribute("syncAll")!=null)
			{
				if("cloud".equals(Constants.getInstallName()) && hasCloudShop==false && remoteInfo.item.getInfo().getGalleryPath().equals("/images/gallery/products")) continue;
				createLocalGalleryInfo(remoteInfo.item, request);
			}
		}

		for (Numbered<Map<String, GalleryBean>> remoteImageTranslations : Numbered.list(content.getGalleryImages()))
		{
			if (null != request.getParameter("galleryImage_" + remoteImageTranslations.number) || request.getAttribute("syncAll")!=null)
			{
				GalleryBean remoteImage = remoteImageTranslations.item.values().iterator().next();
				if("cloud".equals(Constants.getInstallName()) && hasCloudShop==false && remoteImage.getImagePath().startsWith("/images/gallery/products")) continue;
				createLocalGalleryImage(remoteImage);
			}
		}
	}

	private static Content.GalleryInfo getLocalGalleryInfo(Content.GalleryInfo remoteInfo)
	{
		String path = remoteInfo.getInfo().getGalleryPath();
		GalleryDimension info = GalleryDB.getGalleryInfo(path, -1);
		if (info.getGalleryId() < 0) return null;

		Dimension[] dim = GalleryDB.getDimension(path);
		Content.GalleryInfo localInfo = new Content.GalleryInfo();
		localInfo.setInfo(info);
		localInfo.setDim(dim[0]);
		localInfo.setDimNormal(dim[1]);
		return localInfo;
	}

	private static GalleryBean getLocalGalleryImage(GalleryBean remoteImage, HttpServletRequest request)
	{
		int id = GalleryDB.getImageId(remoteImage.getImagePath(), remoteImage.getImageName());
		if (id < 0) return null;

		return GalleryDB.getImageByID(id, request, Constants.getString("defaultLanguage"));
	}

	private static boolean createLocalGalleryInfo(Content.GalleryInfo remoteInfo, HttpServletRequest request)
	{
		GalleryDB.changeDimension(null, remoteInfo.getInfo().getGalleryPath(), remoteInfo.getDim(), remoteInfo.getDimNormal(), request);
		return true;
	}

	private static boolean createLocalGalleryImage(GalleryBean remoteImage)
	{
		String language = Constants.getString("defaultLanguage");
		if (Tools.isEmpty(language)) language = "sk";
		String path = remoteImage.getImagePath();
		String name = remoteImage.getImageName();
		int id = GalleryDB.setImage(path, name); //GalleryDB.getImageId(path, name);
		GalleryDB.updateImageItem(id, "short", remoteImage.getShortDescription(), path, name, language);
		GalleryDB.updateImageItem(id, "long", remoteImage.getLongDescription(), path, name, language);
		GalleryDB.updateImageItem(id, "author", remoteImage.getAuthor(), path, name, language);
		GalleryDB.updateImageItem(id, "priority", "" + remoteImage.getSortPriority(), path, name, language);
		if (null != remoteImage.getUploadDateNull())
		{
			GalleryDB.updateImageItem(id, "upload", "" + remoteImage.getUploadDateNull(), path, name, language);
		}
		return false;
	}

}
