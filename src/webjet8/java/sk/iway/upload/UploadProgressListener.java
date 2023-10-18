package sk.iway.upload;

import org.apache.commons.fileupload.ProgressListener;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;

import javax.servlet.http.HttpSession;

/**
 *  UploadProgressListener.java
 *  
 *  Tracks downloading of an uploaded file. Tigthly coupled with
 *	 progress notifier, @see upload_progress.jsp
 *		Those two communicate by sharing the same session, while 
 *	 attributes' values can be considered a 'message'.  
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 2.2.2011 17:04:19
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UploadProgressListener implements ProgressListener
{
	private final HttpSession session;

	public UploadProgressListener(HttpSession session)
	{
		this.session = session;
		session.setAttribute("uploadProgressStart", System.currentTimeMillis());
	}

	@Override
	public void update(long bytesRead, long contentLength, int items)
	{
		if (Constants.getBoolean("uploadSimulateSlowSpeed"))
		{
			try
			{
	   		 Logger.debug(getClass(),"SLOW UPLOAD, len="+bytesRead+" offset="+contentLength);
			  Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Logger.debug(getClass(), "Failed to simulate pause, reason: " + e.getMessage());
				//TODO: do something?
			}
		}
		
		Logger.debug(UploadProgressListener.class, "Read: "+(bytesRead *100 / contentLength));
		session.setAttribute("uploadProgressContentLength", contentLength);
		session.setAttribute("uploadProgressBytesRead", bytesRead);
	}
}