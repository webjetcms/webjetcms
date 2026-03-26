package sk.iway.iwcm.stat.heat_map;

import java.io.File;
import java.io.FileFilter;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  HeatMapCleaner.java
 *
 *  Regularly deletes old heat map images in /WEB-INF/tmp/heatmap/ folder
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2010 13:55:05
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class HeatMapCleaner
{
	public static void main(String[] args)
	{
		try{
			cleanOldImages();
		}
		catch (Exception e){sk.iway.iwcm.Logger.error(e);}
	}

	private static void cleanOldImages()
	{
		File folder = new File(Tools.getRealPath("/WEB-INF/tmp/heat_map/"));
		if (!folder.exists())
			return;

		final long now = System.currentTimeMillis();
		final long TOO_OLD = Constants.getInt("statHeatMapImageTimeout")*1000;
		File[] oldImages = folder.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File image)
			{
				return (now - image.lastModified()) > TOO_OLD;
			}
		});

		erase(oldImages);
	}

	private static void erase(File[] oldImages)
	{
		for (File image : oldImages)
		{
			boolean success = image.delete();
			if (success) sk.iway.iwcm.Logger.println(HeatMapCleaner.class, "HeatMapCleaner deletes: "+image.getName());
		}
	}
}
