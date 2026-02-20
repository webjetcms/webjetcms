package sk.iway.iwcm.system.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;

public class MetadataCleaner {

	public static boolean removeMetadata(String virtualPath)
	{
		IwcmFile file = new IwcmFile(Constants.getServletContext().getRealPath(virtualPath));
		return removeMetadata(file);
	}

	public static boolean removeMetadata(IwcmFile file)
	{
		String metadataRemoverCommand = Constants.getString("metadataRemoverCommand");
		String metadataRemoverParams = Constants.getString("metadataRemoverParams");
		String metadataRemoverExtensions = Constants.getString("metadataRemoverExtensions");

		boolean result = false;


		try
		{
			if (Tools.isNotEmpty(metadataRemoverCommand))
			{
				File f = new File(metadataRemoverCommand);
				if (f.exists() && f.canRead())
				{
					//overi, ci sa pripona suboru nachadza medzi filtrovanymi priponami.
					String fileExtension = file.getName().substring(file.getName().lastIndexOf(".")+1, file.getName().length()).toLowerCase();
					if (Arrays.asList(Tools.getTokens(metadataRemoverExtensions.toLowerCase(), ",")).contains(fileExtension))
					{
						waitForGfs();

						Logger.println(MetadataCleaner.class, "executing metadata remover: " + metadataRemoverCommand);
						Runtime rt = Runtime.getRuntime();
						String[] params = Tools.getTokens(metadataRemoverParams, " ");
						String[] args = new String[params.length+1];
						int index=0;
						args[index++] = metadataRemoverCommand;
						//naseka argumenty do pola
						for (int i=0; i<params.length; i++)
						{
							if (params[i].indexOf("{filePath}")!=-1)
							{
								//ak je DBFS, vytvori lokalnu kopiu na disku, na konci ju ulozi opat do DB.
								if (IwcmFsDB.useDBStorage(file.getVirtualPath()))
								{
									IwcmFsDB.writeFileToDisk(new File(file.getPath()),new File(IwcmFsDB.getTempFilePath(file.getPath())));
									args[index++] = IwcmFsDB.getTempFilePath(file.getPath());
								}
								else
								{
									args[index++] = file.getPath();
								}
							}
							else
							{
								args[index++] = params[i];
							}

						}

						String cmdString = "";
						if (args != null)
						{
							for (int i = 0; i < args.length; i++)
							{
								if (Tools.isNotEmpty(args[i]))
									cmdString += " " + args[i];
							}
						}
						Logger.println(MetadataCleaner.class, "CMD:\n" + cmdString);
						Process proc = rt.exec(args);
						InputStream stderr = proc.getErrorStream();
						BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
						String line = null;
						while ((line = br.readLine()) != null)
						{
							Logger.println(MetadataCleaner.class, line);
						}
						br.close();
						int exitValue = proc.waitFor();

						//ak ide o subor z DBFS, ulozi ho naspat do DB
						if (IwcmFsDB.useDBStorage(file.getVirtualPath()))
						{
							IwcmFsDB.writeFileToDB(new File(IwcmFsDB.getTempFilePath(file.getVirtualPath())), new File(file.getVirtualPath()));
							new File(IwcmFsDB.getTempFilePath(file.getVirtualPath())).delete();
						}

						if (exitValue==0)
							result = true;
						Logger.println(MetadataCleaner.class, "ExitValue: " + exitValue);
					}
					else
						result = true;
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return result;
	}

	public static void waitForGfs()
	{
		int metadataWaitTime = Constants.getInt("metadataWaitTime");
		if (metadataWaitTime > 0)
		{
			try
			{
				//kvoli GFS a tomu, ze menime existujuci subor je potrebne pockat aby nevznikol konflikt
				Thread.sleep(metadataWaitTime);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}
}
