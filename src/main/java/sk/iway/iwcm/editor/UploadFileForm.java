package sk.iway.iwcm.editor;

/**
 *  upload suboru (obrazok, subor) na server (formular)
 *  Extenduje EditorForm aby bolo mozne nastavovat atributy pre indexovane subory
 *
 *@Title        iwcm
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Piatok, 2002, máj 17
 *@modified     $Date: 2003/01/28 11:30:12 $
 */
public class UploadFileForm
{
   private int videoWidth=320;
   private int videoHeight=240;
   private int bitRate=64;
   private boolean keepOriginalVideo = false;

	public int getVideoWidth()
	{
		return videoWidth;
	}

	public void setVideoWidth(int videoWidth)
	{
		this.videoWidth = videoWidth;
	}

	public int getVideoHeight()
	{
		return videoHeight;
	}

	public void setVideoHeight(int videoHeight)
	{
		this.videoHeight = videoHeight;
	}

	public int getBitRate()
	{
		return bitRate;
	}

	public void setBitRate(int bitRate)
	{
		this.bitRate = bitRate;
	}

	public boolean isKeepOriginalVideo()
	{
		return keepOriginalVideo;
	}

	public void setKeepOriginalVideo(boolean keepOriginalVideo)
	{
		this.keepOriginalVideo = keepOriginalVideo;
	}
}
