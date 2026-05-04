package sk.iway.iwcm.sync.export;

/**
 * Exporter pre FLV video.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.6.2012 16:54:59
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FlashExporter extends ComponentExporter
{

	public FlashExporter(String params)
	{
		super(params);
	}

	@Override
	public void export(ContentBuilder callback)
	{
		String file = pageParams.getValue("file", null);
		callback.addLink(file);
	}

}
