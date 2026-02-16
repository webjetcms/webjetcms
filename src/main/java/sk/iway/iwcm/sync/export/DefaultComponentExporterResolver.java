package sk.iway.iwcm.sync.export;

import java.lang.reflect.Method;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  DefaultComponentExporterResolver.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.5.2013 10:46:19
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DefaultComponentExporterResolver implements ComponentExporterResolver
{

	static final String COMPONENT_BANNER  = "/components/banner/banner.jsp";
	static final String COMPONENT_GALLERY = "/components/gallery/gallery.jsp";
	static final String COMPONENT_INQUIRY = "/components/inquiry/inquiry.jsp";
	static final String COMPONENT_FLV     = "/components/_common/flvplayer";

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public ComponentExporter forInclude(String include)
	{
		if (include.startsWith(COMPONENT_BANNER))			return new BannerExporter(include);
		if (include.startsWith(COMPONENT_GALLERY))			return new GalleryExporter(include);
		if (include.startsWith(COMPONENT_INQUIRY))			return new InquiryExporter(include);
		if (include.startsWith(COMPONENT_FLV))			return new FlashExporter(include);
		String projectSpecificComponentExporterResolverClass = Constants.getString("projectSpecificComponentExporterResolverClass");
		if (Tools.isNotEmpty(projectSpecificComponentExporterResolverClass))
		{
			try
			{
				Class projectSpecificClass = Class.forName(projectSpecificComponentExporterResolverClass);
				Method method = projectSpecificClass.getMethod("forInclude", new Class[]{String.class});
				return (ComponentExporter) method.invoke(projectSpecificClass.getDeclaredConstructor().newInstance(), include);
			}
			catch (Exception e)
			{
				Logger.debug(ComponentExporter.class, "Failed to find or invoke project specific ComponentExporter resolver: "
							+ projectSpecificComponentExporterResolverClass + ", cause: " + e.getMessage());
			}
		}
		return null;
	}
}
