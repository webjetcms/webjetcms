package sk.iway.iwcm.stat;

import static sk.iway.iwcm.Tools.isEmpty;

import java.util.Map;

import sk.iway.iwcm.utils.MapUtils;

/**
 *  OsVersionTranslator.java
 *  
 *  Translates cryptic OS names and version into
 *  human readable format
 *  
 *  All OS X isntances are mapped to "Macintosh"
 *  All Linux / Solaris instances are mapped to "Unix"
 *  Windows versions are translated into their respective names
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.5.2011 16:54:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class OsVersionTranslator
{
	private final Product os;
	private static final Map<String, String> windowsVersions = MapUtils.asMap(
			"10.0","10",
				"6.3", "8",
				"6.2", "8",
				"6.1", "7",
				"6.0", "Vista",
				"5.2", "XP x64 Edition",
				"5.1", "XP",
				"5.01","2000",
				"5.0", "2000",
				"4.90", "Millenium"
	);
	
	private static final Map<String, String> macVersions = MapUtils.asMap(

				"10.15","Catalina",
				"10.14","Mojave",
				"10.13","High Sierra",
				"10.12","Sierra",
				"10.11","El Capitan",
				"10.10","Yosemite",
				"10.9","Mavericks",
				"10.8","Mountain Lion",
				"10.7", "Lion",
				"10.6", "Snow Leopard",
				"10.5", "Leopard",
				"10.4", "Tiger"
			);

	public OsVersionTranslator(Product os)
	{
		this.os = os;
	}

	public String getVersion()
	{
		String name = getOsName();
		if (name.contains("unknown"))
			return "";
		if (name.equals("macOS"))
			return translateMacVersion(os.version);
		if (name.equals("Windows"))
			return translateMicrosoftVersion(os.version);
		if (name.equals("Unix"))
			return os.name;
		return "";	
	}
	
	private String translateMacVersion(String version)
	{
		if (isEmpty(version))
			return "";
		//replace all non numbers by dots 
		version = version.trim().replaceAll("[^0-9]", ".");
		String[] versionParts = version.split("[.]+");
		if (versionParts.length < 2)
			return "";
		
		String dottedVersion = versionParts[0]+'.'+versionParts[1];
		if (macVersions.containsKey(dottedVersion))
			return macVersions.get(dottedVersion);
		return dottedVersion.trim();
	}

	public String translateMicrosoftVersion(String version){
		if (isEmpty(version))
			return "";
		if (windowsVersions.containsKey(version.trim()))
			return windowsVersions.get(version.trim());
		return version.trim();
	}

	public String getOsName()
	{
		String lowCaseName = os.name.toLowerCase();
		if (lowCaseName.contains("unknown"))
			return "unknown";
		if (lowCaseName.contains("mac"))
			return "macOS";
		if (lowCaseName.contains("windows nt") || lowCaseName.contains("win"))
			return "Windows";
		return "Unix";
	}
}