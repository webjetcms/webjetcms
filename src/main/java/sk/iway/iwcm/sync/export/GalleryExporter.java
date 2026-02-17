package sk.iway.iwcm.sync.export;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDB;

/**
 * Export udajov pre komponent "gallery".
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2012
 * @author $Author: jeeff vbur $
 * @version $Revision: 1.3 $
 * @created Date: 8.6.2012 21:58:36
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class GalleryExporter extends ComponentExporter
{

	public GalleryExporter(String params)
	{
		super(params);
	}

	@Override
	public void export(ContentBuilder callback)
	{
		String styleName = pageParams.getValue("styleName", null);
		if (Tools.isNotEmpty(styleName))
		{
			String stylePath = "/components/" + Constants.getInstallName() + "/gallery/gallery-" + styleName + ".jsp";
			callback.addLink(stylePath);
		}
		callback.addLink(pageParams.getValue("pictureLink", null));

		HttpServletRequest request = callback.getRequest();
		String directory = pageParams.getValue("dir", null);
		boolean recursive = pageParams.getBooleanValue("alsoSubfolders", false);
		boolean hasCloudShop = "cloud".equals(Constants.getInstallName()) && CloudToolsForCore.hasShop(request);
		if ("cloud".equals(Constants.getInstallName())) recursive = true;

		Logger.debug(GalleryExporter.class, "GalleryDB.getImages(directory="+directory+", recursive="+recursive+", sk, null, , asc, request);");
		List<GalleryBean> images = GalleryDB.getImages(directory, recursive, "sk", null, "", "asc", request);
		Logger.debug(GalleryExporter.class, "images="+images.size());
		List<String> imagePaths = new ArrayList<>(); // cesty k uz exportovanym adresarom
		for (GalleryBean image : images)
		{
			if("cloud".equals(Constants.getInstallName()) && hasCloudShop==false && image.getImagePath().startsWith("/images/gallery/products")) continue;

			// add image
			Map<String, GalleryBean> translations = new HashMap<>();
			for (String language : GalleryDB.LANGUAGES)
			{
				translations.put(language, GalleryDB.getGalleryBean(image.getImagePath(), image.getImageName(), request, language));
			}
			callback.addGalleryImage(translations);

			// add directory
			String imagePath = image.getImagePath();
			if (!imagePaths.contains(imagePath))
			{
				Dimension[] dim = GalleryDB.getDimension(imagePath);
				Logger.debug(GalleryExporter.class, "Adding gallery info, imagePath="+imagePath+", dim[0]="+dim[0]+", dim[1]="+dim[1]);
				callback.addGalleryInfo(GalleryDB.getGalleryInfo(imagePath, -1), dim[0], dim[1]);
				imagePaths.add(imagePath);
			}

			Logger.debug(GalleryExporter.class, "Add file, image="+imagePath+"/"+image.getImageName()+" imagePath="+image.getImagePath());

			// add files
			callback.addLink(imagePath + "/" + image.getImageName());
			callback.addLink(imagePath + "/o_" + image.getImageName());
			callback.addLink(imagePath + "/s_" + image.getImageName());
		}
	}

}
