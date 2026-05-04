package sk.updater;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  Logger.java - logger pre vypis vsetkeho mozneho
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 23.10.2005 22:02:57
 *@modified     $Date: 2005/11/07 10:39:22 $
 */
@SuppressWarnings("rawtypes")
public class Logger
{
	public static SimpleDateFormat simpleDateTimeFormat;

	public static boolean showClassName = false;
	public static boolean showInstallName = false;


	static
	{
		simpleDateTimeFormat = new SimpleDateFormat("dd.MM H:mm:ss");
	}

	private static String formatMessage(Class c, String message)
	{
		StringBuilder ret = new StringBuilder("[");
		ret.append(simpleDateTimeFormat.format(new Date(System.currentTimeMillis())));
		if (showInstallName) ret.append(" "+InitServlet.INSTALL_NAME);
		if (showClassName) ret.append(" "+c);
		ret.append("] ");
		ret.append(message);

		return(ret.toString());
	}

	public static void println(Class c, String message)
	{
		print(c, message + "\n");
	}

	public static void print(Class c, String message)
	{
		System.out.print(formatMessage(c, message));
	}

	public static void println(Object o, String message)
	{
		println(o.getClass(), message);
	}

	public static void print(Object o, String message)
	{
		print(o.getClass(), message);
	}

	public static void printlnError(Object o, Exception e)
	{
		printlnError(o.getClass(), e.getMessage());
	}

	public static void printlnError(Object o, String message)
	{
		printlnError(o.getClass(), message);
	}

	public static void printError(Object o, String message)
	{
		printError(o.getClass(), message);
	}

	public static void printlnError(Class c, String message)
	{
		print(c, message + "\n");
	}

	public static void printError(Class c, String message)
	{
		System.err.print(formatMessage(c, message));
	}
}
