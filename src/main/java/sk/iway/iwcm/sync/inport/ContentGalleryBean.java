package sk.iway.iwcm.sync.inport;

import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.sync.export.Content;

/**
 *  ContentGalleryImageBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 27.6.2012 15:41:56
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public abstract class ContentGalleryBean
{

	public static class Info extends SimpleContentBean<Content.GalleryInfo>
	{

		public Info(int number, Content.GalleryInfo remoteInfo, Content.GalleryInfo localInfo)
		{
			super(number, remoteInfo, localInfo);
		}

		public String getPath()
		{
			return remoteItem.getInfo().getGalleryPath();
		}

	}

	public static class Image extends SimpleContentBean<GalleryBean>
	{

		public Image(int number, GalleryBean remoteImage, GalleryBean localImage)
		{
			super(number, remoteImage, localImage);
		}

		public String getPath()
		{
			return remoteItem.getImagePath();
		}

		public String getName()
		{
			return remoteItem.getImageName();
		}

	}

}
