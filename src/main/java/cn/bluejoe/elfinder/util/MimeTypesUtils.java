package cn.bluejoe.elfinder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

//JEEFF import org.springframework.core.io.ClassPathResource;

public abstract class MimeTypesUtils
{
	private static Map<String, String> _map;

	public static final String UNKNOWN_MIME_TYPE = "application/oct-stream";

	static
	{
		_map = new HashMap<String, String>();
		try
		{
			load();
		}
		catch (Throwable e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static String getMimeType(String ext)
	{
		return _map.get(ext.toLowerCase());
	}

	public static boolean isUnknownType(String mime)
	{
		return mime == null || UNKNOWN_MIME_TYPE.equals(mime);
	}

	private static void load() throws IOException
	{
		//JEEFF InputStream is = new ClassPathResource("/mime.types").getInputStream();
		IwcmFile f = new IwcmFile(Tools.getRealPath("/WEB-INF/mime.types"));
		InputStream is = new IwcmInputStream(f);
		BufferedReader fr = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = fr.readLine()) != null)
		{
			line = line.trim();
			if (line.startsWith("#") || line.isEmpty())
			{
				continue;
			}

			String[] tokens = line.split("\\s+");
			if (tokens.length < 2)
				continue;

			for (int i = 1; i < tokens.length; i++)
			{
				putMimeType(tokens[i], tokens[0]);
			}
		}
		fr.close();
		is.close();
	}

	public static void putMimeType(String ext, String type)
	{
		if (ext == null || type == null)
			return;

		_map.put(ext.toLowerCase(), type);
	}
}
