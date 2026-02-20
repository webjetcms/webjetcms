package sk.iway.iwcm.sync.export;

import sk.iway.iwcm.PageParams;

/**
 * Exporter pre komponent.
 * Pre kazdy komponent, ktory podporuje export a import, treba vytvorit prislusnu triedu a zaregistrovat ju v metode "forInclude".
 * Pri inicializacii dostane exporter parametre "pageParams" zo stranky,
 * potrebne subory nahlasi v metode "export" do callback objektu.
 * 
 * pre komponenty specificke pre projekt treba vyrobit triedu ktora bude implementovat {@link ComponentExporterResolver}
 * a meno tejto triedy nastavit do konfiguracnej premennej projectSpecificComponentExporterResolverClass
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.6.2012 9:28:30
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public abstract class ComponentExporter
{
	protected PageParams pageParams;

	public ComponentExporter(String params)
	{
		pageParams = new PageParams(params);
	}
	
	public ComponentExporter()
	{
		pageParams = new PageParams();
	}

	/**
	 * Nahlasi data potrebne na spravne vykonanie komponentu.
	 * 
	 * @param callback
	 */
	public abstract void export(ContentBuilder callback);
	
}