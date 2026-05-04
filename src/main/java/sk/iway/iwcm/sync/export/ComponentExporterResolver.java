package sk.iway.iwcm.sync.export;

/**
 *  ComponentExporterResolver.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.5.2013 10:40:05
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface ComponentExporterResolver
{
	/**
	 * Vrati exporter pre prislusny komponent; null ak taky neexistuje.
	 * 
	 * @param include  retazec vnutri textu "!INCLUDE(" a ")!"
	 * @return
	 */
	public ComponentExporter forInclude(String include);
}
