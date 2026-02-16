package sk.iway.iwcm.gallery;

import static sk.iway.iwcm.Tools.isEmpty;

import sk.iway.iwcm.Constants;

/**
 *  WatermarkSetup.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.8.2010 13:44:41
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class WatermarkSetup
{
	private String watermark;
	private String watermarkPlacement;
	private int watermarkSaturation;
	
	
	public String getWatermark()
	{
		return watermark;
	}
	public void setWatermark(String watermark)
	{
		this.watermark = watermark;
	}
	public String getWatermarkPlacement()
	{
		if (isEmpty(watermarkPlacement))
			return Constants.getString("galleryWatermarkGravity");
		return watermarkPlacement;
	}
	public void setWatermarkPlacement(String watermarkPlacement)
	{
		this.watermarkPlacement = watermarkPlacement;
	}
	public int getWatermarkSaturation()
	{
		if (watermarkSaturation == 0)
			return Constants.getInt("galleryWatermarkSaturation");
		return watermarkSaturation;
	}
	public void setWatermarkSaturation(int watermarkSaturation)
	{
		this.watermarkSaturation = watermarkSaturation;
	}
}
