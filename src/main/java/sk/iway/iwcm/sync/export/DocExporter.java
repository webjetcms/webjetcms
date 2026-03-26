package sk.iway.iwcm.sync.export;

import java.util.List;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 * Exporter udajov z webovej stranky.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.6.2012 20:42:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DocExporter
{
	/**
	 * Exportuje vsetky linky a komponenty z danej stranky a jej sablon.
	 * 
	 * @param doc
	 * @param callback
	 */
	public static void export(DocDetails doc, ContentBuilder callback)
	{
		callback.setDoc(doc);
		
		String externalLink = doc.getExternalLink();
		if (Tools.isNotEmpty(externalLink))
		{
			callback.addLink(externalLink);
		}
		else
		{
			callback.addHtml(doc.getData());
			TemplateDetails template = TemplatesDB.getInstance().getTemplate(doc.getTempId());
			callback.addHtml(template.getFooterDocData());
			callback.addHtml(template.getHeaderDocData());
			callback.addHtml(template.getMenuDocData());
			callback.addHtml(template.getRightMenuDocData());
			callback.addHtml(template.getObjectADocData());
			callback.addHtml(template.getObjectBDocData());
			callback.addHtml(template.getObjectCDocData());
			callback.addHtml(template.getObjectDDocData());
		}
		
		//perex obrazok
		if (Tools.isNotEmpty(doc.getPerexImage()))
		{
			Logger.debug(DocExporter.class, "Adding PEREX IMAGE:"+doc.getPerexImage());
			callback.addLink(doc.getPerexImage());
		}
		
		//pridaj media
		List<Media> media = MediaDB.getMedia(null, "documents", doc.getDocId(), null, 0, false);
		for (Media m : media)
		{
			if (m.getMediaLink()!=null && (m.getMediaLink().startsWith("/files") || m.getMediaLink().startsWith("/images")))
			{
				Logger.debug(DocExporter.class, "Adding media link: "+m.getMediaLink());
				callback.addLink(m.getMediaLink());
				if (Tools.isNotEmpty(m.getMediaThumbLink()))
				{
					Logger.debug(DocExporter.class, "Adding media thumb link: "+m.getMediaThumbLink());
					callback.addLink(m.getMediaThumbLink());
				}
			}
		}

		callback.setDoc(null);
	}


}
