package sk.iway.iwcm;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.util.EntityUtils;
import org.apache.struts.util.ResponseUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sk.iway.Html2Text;
import sk.iway.Password;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.components.basket.BasketDB;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.helpers.RequestHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.users.UsersDB;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Podporne metody
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      Nedeďż˝e, 2003, december 28
 *@modified     $Date: 2004/02/19 17:21:45 $
 */
public class Tools
{
	public static final DecimalFormat decimalFormat;
	private static FastDateFormat simpleDateTimeFormat;
	private static FastDateFormat simpleDateFormat;
	private static FastDateFormat simpleTimeFormat;
	public static final Collator slovakCollator;

	static
	{
		decimalFormat = new DecimalFormat("0.##");
		simpleDateFormat = FastDateFormat.getInstance(Constants.getString("dateFormat"));
		simpleDateTimeFormat = FastDateFormat.getInstance(Constants.getString("dateTimeFormat"));
		simpleTimeFormat = FastDateFormat.getInstance(Constants.getString("timeFormat"));
		slovakCollator = Collator.getInstance(new Locale("sk"));
		slovakCollator.setStrength(Collator.SECONDARY);
	}

	/**
	 * Toto potrebujeme zavolat po inicializacii Constants aby bolo mozne zmenit pattern
	 */
	public static void reinitialize()
	{
		simpleDateFormat = FastDateFormat.getInstance(Constants.getString("dateFormat"));
		simpleDateTimeFormat = FastDateFormat.getInstance(Constants.getString("dateTimeFormat"));
		simpleTimeFormat = FastDateFormat.getInstance(Constants.getString("timeFormat"));
	}

	public static String formatDate(long date)
	{
		if (date == 0) return "";
		return (formatDate(new Date(date)));
	}
	public static String formatDate(Date date)
	{
		if (date == null) return "";
		return (simpleDateFormat.format(date));
	}
	public static String formatDate(Object o, String nullValue)
	{
		if (o == null) return nullValue;
		if(o instanceof Date)
			return simpleDateFormat.format((Date)o);
		if(o instanceof java.sql.Timestamp)
			return simpleDateFormat.format((java.sql.Timestamp)o);
		if(o instanceof java.sql.Date)
			return simpleDateFormat.format((java.sql.Date)o);
		return nullValue;
	}
	public static String formatTime(long time)
	{
		if (time == 0) return "";
		return (formatTime(new Date(time)));
	}
	public static String formatTime(Date date)
	{
		if (date == null) return "";
		return (simpleTimeFormat.format(date));
	}
	public static String formatDateTime(long time)
	{
		return (formatDate(new Date(time)) + " " + formatTime(new Date(time)));
	}
	public static String formatDateTime(Date time)
	{
		if (time == null) return "";
		return formatDateTime(time.getTime());
	}
	public static String formatDateTime(Object o, String nullValue)
	{
		if (o == null) return nullValue;
		if(o instanceof Date)
			return formatDateTime(((Date)o).getTime());
		if(o instanceof java.sql.Timestamp)
			return formatDateTime(((java.sql.Timestamp)o).getTime());
		if(o instanceof java.sql.Date)
			return formatDateTime(((java.sql.Date)o).getTime());
		return nullValue;
	}

	public static String formatDateTimeSeconds(long time)
	{
		return formatDateTimeSeconds(new Date(time));
	}

	public static String formatDateTimeSeconds(Date time)
	{
		if (time == null) return "";
		return simpleDateTimeFormat.format(time);
	}

	/**
	 * povodne renameTo robilo problemy a to take ze niekedy sa stary subor nedal
	 * zmazat a preto nebol vytvoreny ani novy
	 * riesenie -> najprv vytvorim novy subor a ak sa da potom zmazem ten stary
	 *
	 *@param  oldFilePath  Description of the Parameter
	 *@param  newFilePath  Description of the Parameter
	 *@return              Description of the Return Value
	 */
	public static boolean renameFile(String oldFilePath, String newFilePath)
	{
		if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(oldFilePath)))
		{
			new IwcmFile(oldFilePath).renameTo(new IwcmFile(newFilePath));
			return true;
		}
		boolean renamed = false;

		try
		{
			File renameFile = new File(oldFilePath);
			if (renameFile.exists()==false)
			{
				 Logger.error(Tools.class, "renameFile(): zdrojovy subor "+renameFile.getAbsolutePath()+" neexistuje!");
				 return false;
			}

			renamed = renameFile.renameTo(new File(newFilePath));
			if (renamed == false)
			{
				File newFile = new File(newFilePath);
				File oldFile = new File(oldFilePath);
				boolean fileCreated = newFile.createNewFile();
				if (!fileCreated)
					throw new IllegalStateException(newFilePath+" could not be created");
				FileInputStream inStream = new FileInputStream(oldFile);
				try
				{
					FileOutputStream out = new FileOutputStream(newFile);
					try
					{
						int c;
						byte[] buff = new byte[150000];
						while ((c = inStream.read(buff)) != -1)
						{
							out.write(buff, 0, c);
						}
					}
					finally { out.close(); }
				}
				finally { inStream.close(); }

				renamed = true;

				try
				{
					//skus ho zmazat
					if(oldFile.delete() == false)
						 Logger.error(Tools.class, "renameFile(): zdrojovy subor "+oldFile.getAbsolutePath()+" sa nepodarilo vymazat!");
				}
				catch (Exception ex)
				{

				}
			}
		}
		catch (Exception ex)
		{

		}

		return renamed;
	}

	/**
	 * Otestuje, ci zadany string je prazdny
	 * @param s
	 * @return - true ak s je null, alebo prazdny, inak false
	 */
	public static boolean isEmpty(String s)
	{
		return StringUtils.isBlank(s);
	}

	/**
	 * Otestuje, ci zadany string nie je prazdny
	 * @param s
	 * @return
	 */
	public static boolean isNotEmpty(String s)
	{
		if (s==null) return false;
		return(!isEmpty(s));
	}

	/**
	 * Otestuje, ci zadany StringBuilder je prazdny
	 * @param sb
	 * @return - true ak sb je null, alebo prazdny, inak false
	 */
	public static boolean isEmpty(StringBuilder sb)
	{
		if(sb==null)
			return true;

		return isEmpty(sb.toString());
	}

	/**
	 * Otestuje, ci zadana kolekcia je prazdna
	 *
	 * @param c kolekcia
	 * @return true ak je kolekcia null alebo prazdna, inak false
	 */
	public static boolean isEmpty(Collection<?> c)
	{
		if (null == c)
			return true;
		return c.isEmpty();
	}

	/**
	 * Otestuje, ci zadany StringBuilder nie je prazdny
	 * @param sb
	 * @return
	 */
	public static boolean isNotEmpty(StringBuilder sb)
	{
		return !isEmpty(sb);
	}

	/**
	 * Otestuje, ci su dane retazce rovnake, pricom empty a empty sa pocitaju za rovnake.
	 *
	 * @param a  retazec alebo null
	 * @param b  retazec alebo null
	 * @return   true ak sďż˝ oba empty, alebo oba non-empty a equal
	 */
	public static boolean areSame(String a, String b)
	{
		return isEmpty(a) ? isEmpty(b) : a.equals(b);
	}

	/**
	 *
	 * Otestuje, ci zadany parameter je email
	 * @param email
	 * @return
	 */
	public static boolean isEmail(String email)
	{
		if (Tools.isEmpty(email)) return false;

		int at = email.indexOf('@');
		if (at == -1) return false;
		if (at == 1) return email.matches("^[a-zA-Z0-9]@[a-zA-Z0-9]+[a-zA-Z0-9._-]*\\.[a-zA-Z]{2,16}$");
		if (at == 2) return email.matches("^[a-zA-Z0-9]+[a-zA-Z0-9]+@[a-zA-Z0-9]+[a-zA-Z0-9._-]*\\.[a-zA-Z]{2,16}$");

		return email.matches("^[a-zA-Z0-9]+[a-zA-Z0-9._\\-+=#$%&]*[a-zA-Z0-9]+@[a-zA-Z0-9]+[a-zA-Z0-9._-]*\\.[a-zA-Z]{2,16}$");
	}

	/**
	 * Otestuje, ci zadany string je telefonne cislo
	 */
	public static boolean isPhoneNumber(String phoneNumber) {
		if (Tools.isEmpty(phoneNumber))
			return false;

		try
        {
            String[] regexp = FormDB.getRegExpByType("phone");
            if (regexp != null && regexp.length>2 && Tools.isNotEmpty(regexp[2]))
            {
                if (phoneNumber.matches(regexp[2])) return true;
                else return false;
            }
        }
		catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }

		//regexp nie je definovany vratime true, ze je validny
		return true;
	}

	/**
	 * Vykona preklad URL na ine URL, pouziva sa ak je potrebne loopback data stahovat z ineho portu / nazvu
	 * @param url
	 * @return
	 */
	public static String natUrl(String url)
	{
		String natUrlTranslate = Constants.getString("natUrlTranslate");
		if (Tools.isEmpty(natUrlTranslate)) return url;

		String urlOriginal = url;
		url = Tools.replaceStrings(url, "natUrlTranslate", true);

		Logger.debug(Tools.class, "natUrl: "+urlOriginal+"->"+url);

		return url;
	}

	/**
	 * Nahradi vyrazy v texte podla konf. premennej constantName, vyrazy musia byt na novom riadku vo formate stary|novy, pouziva sa ked potrebujem nahradit viacero vyrazov
	 * @param text
	 * @param constantName
	 * @param replaceWholeLine - ak je true nahradia sa len vyrazy zhodne na novom riadku
	 * @return
	 */
	public static String replaceStrings(String text, String constantName, boolean replaceWholeLine)
	{
		String replaces = Constants.getString(constantName);
		if (Tools.isEmpty(replaces)) return text;

		try
		{
			String[] urls = Tools.getTokens(replaces, "\n");
			if (urls.length==0) return text;
			for (String pair : urls)
			{
				if (Tools.isEmpty(pair)) continue;
				String[] pairArray = pair.split("\\|");
				if (pairArray==null || pairArray.length!=2) continue;

				if (replaceWholeLine)
				{
					if (text.startsWith(pairArray[0]) == true && text.startsWith(pairArray[1]) == false)
					{
						text = Tools.replace(text, pairArray[0], pairArray[1]);
					}
				}
				else
				{
					text = Tools.replace(text, pairArray[0], pairArray[1]);
				}
			}
		}
		catch (Exception ex)
		{
		}

		return text;
	}

	/**
	 * Autorizuje sa pomozou tokenu, stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @param token
	 * @return
	 */
	public static String authWithTokenAndDownloadUrl(String url, String token)
	{
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Bearer " + token);
		return downloadUrl( url,  SetCharacterEncodingFilter.getEncoding(),  null,0, headers);
	}

	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @return
	 */
	public static String downloadUrl(String url)
	{
		return(downloadUrl(url, SetCharacterEncodingFilter.getEncoding(), null));
	}

	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url url
	 * @param characterEncoding encoding
	 * @return
	 */
	public static String downloadUrl(String url, String characterEncoding)
	{
		return(downloadUrl(url, characterEncoding, null));
	}


	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @return
	 */
	public static String downloadUrl(String url, javax.servlet.http.Cookie [] cookies)
	{
		return(downloadUrl(url, SetCharacterEncodingFilter.getEncoding(), cookies));
	}

	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @param defaultEncoding - kodovanie (ak nebude nastavene v response hlavicke)
	 * @param cookies cookies ktore sa maju pouzit pri requeste
	 * @return
	 */
	public static String downloadUrl(String url, String defaultEncoding, javax.servlet.http.Cookie[] cookies)
	{
		return downloadUrl( url,  defaultEncoding,  cookies,0);
	}

	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @param defaultEncoding - kodovanie (ak nebude nastavene v response hlavicke)
	 * @param cookies cookies ktore sa maju pouzit pri requeste
	 * @param timeOutSeconds cas v sekundach po uplynuti ktorych sa spojenie prerusi.
	 * @return
	 */
	public static String downloadUrl(String url, String defaultEncoding, javax.servlet.http.Cookie[] cookies,int timeOutSeconds)
	{
		return downloadUrl( url,  defaultEncoding,  cookies, timeOutSeconds, null);
	}

	/**
	 * Stiahne pozadovane URL a vrati to ako String, ak nastala chyba, vrati null
	 * @param url
	 * @param defaultEncoding - kodovanie (ak nebude nastavene v response hlavicke)
	 * @param cookies cookies ktore sa maju pouzit pri requeste
	 * @param timeOutSeconds cas v sekundach po uplynuti ktorych sa spojenie prerusi.
	 * @param headers dodatocne hlavicky ktore chceme requestu pridat
	 * @return
	 */
	public static String downloadUrl(String url, String defaultEncoding, javax.servlet.http.Cookie[] cookies,int timeOutSeconds, Map<String, String> headers)
	{
		String body = null;
		if (url.startsWith("http://") || url.startsWith("https://"))
		{
			try
			{
				if (timeOutSeconds < 1 && "cloud".equals(Constants.getInstallName())) timeOutSeconds = 120;

				url = Tools.natUrl(url);

				Logger.debug(Tools.class,"DownloadUrl: " + url);

				//body obsahuje URL adresu, ktoru je treba stiahnut
				URLConnection conn = null;
				URL urlObj = new URL(url);

				if (url.startsWith("https://"))
				{
					doNotVerifyCertificates(urlObj.getHost());
				}

				conn = urlObj.openConnection();

				if(timeOutSeconds > 0)
				{
					conn.setConnectTimeout(timeOutSeconds*1000);
					conn.setReadTimeout(timeOutSeconds*1000);
				}
				/*if (conn instanceof HttpURLConnection)
				{
					HttpURLConnection urlConn = (HttpURLConnection)conn;
				}*/
				if(cookies != null)
				{
					for (javax.servlet.http.Cookie cookie : cookies) {
					    conn.addRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue());
					}
				}

				if(headers != null && headers.keySet().size() > 0)
				{
					for(Map.Entry<String, String> entry : headers.entrySet())
					{
						conn.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}

				String ua = Constants.getString("downloadUrlUserAgent");
				if(Tools.isNotEmpty(ua)) conn.addRequestProperty("User-Agent", ua);

				conn.setAllowUserInteraction(false);
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.connect();
				String location = conn.getHeaderField("Location");
				String encoding = conn.getHeaderField("Content-Type");

				if (encoding==null || encoding.indexOf("charset=")==-1)
				{
					encoding = defaultEncoding;
				}
				else
				{
					encoding = encoding.substring(encoding.indexOf("charset=")+8).trim();
				}

				Logger.debug(Tools.class,"---> ENCODING: " + encoding);

				StringBuilder sb = new StringBuilder();
				BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
				InputStreamReader in = new InputStreamReader(is, encoding);
				char[] buffer = new char[8000];
				int n = 0;
				while (true)
				{
					 n = in.read(buffer);
					 if (n < 1) break;
					 sb.append(buffer, 0, n);
				}
				in.close();
				body = sb.toString();

				if (Tools.isNotEmpty(location))
				{
					body = "<html><body>\n" +
							"<a href='"+location+"'>"+location+"</a>\n" +
							"<script language='JavaScript'>\n" +
							"window.location.href='"+location+"';\n" +
							"</script>\n" +
							"</body></html>";
				}

			}
			catch (Exception ex)
			{
			    if (url==null || url.contains("localconf.jsp")==false)
                {
                    Logger.error(Tools.class, "ERROR downloadUrl(" + url + ")");
                    sk.iway.iwcm.Logger.error(ex);
                    body = null;
                }
			}
		}
		return(body);
	}

	/**
	 * Stiahnutie stranky s vyuzitim Jakarta HTTP Client (zvlada POST aj GET)
	 * @param basePath
	 * @param req
	 * @return
	 */
	public static String downloadUrl(String basePath, HttpServletRequest req)
	{
		String data = null;

		Request request;

		basePath = Tools.natUrl(basePath);
		//WebJETProxySelector.setProxyForHttpClient(client, basePath);
		Logger.debug(Tools.class, "downloadUrl: basePath="+basePath);
		String name;
		String value;

		if ("GET".equalsIgnoreCase(req.getMethod()))
		{
			if (basePath.indexOf('?')==-1 && req.getQueryString()!=null && req.getQueryString().length()>1)
			{
				basePath = basePath + "?" + req.getQueryString();
			}
			Logger.println(Tools.class,"JE TO GET: " + basePath);
			request = Request.Get(basePath);
		}
		else
		{
			Logger.println(Tools.class,"JE TO POST: " + basePath);

			request = Request.Post(basePath);
			Form form = Form.form();
		   Enumeration<String> params = req.getParameterNames();
			while (params.hasMoreElements())
			{
				name = params.nextElement();
				value = req.getParameter(name);
				Logger.println(Tools.class,"param: " + name+"="+value);
				form.add(name, value);
			}
			request.bodyForm(form.build());
		}

      //method.setFollowRedirects(true);
      //method.setStrictMode(false);

      Enumeration<String> e = req.getHeaderNames();
		while (e.hasMoreElements())
		{
			name = e.nextElement();
			value = req.getHeader(name);
			if ("host".equalsIgnoreCase(name))
			{
				//value = "www2.ing.cz";
				try
				{
					URL url = new URL(basePath);
					value = url.getHost();
				}
				catch (Exception ex)
				{

				}
			}
			request.addHeader(name, value);
			Logger.println(Tools.class,name+": "+value);
		}

		String contentType = req.getContentType()+"; charset="+req.getCharacterEncoding();
		request.setHeader("Content-Type", contentType);
		Logger.println(Tools.class,"header: Content-Type: " + contentType);

		try {
			Response response = request.execute();
			HttpResponse httpResponse = response.returnResponse();

			// write out the response headers
			Logger.println(Tools.class,"*** Response ***");
			Logger.println(Tools.class,"Status Line: " + httpResponse.getStatusLine().toString());
			Logger.println(Tools.class,"Status Code: " + httpResponse.getStatusLine().getStatusCode());
			Header[] responseHeaders = httpResponse.getAllHeaders();
			Header h;
			for (int i=0; i<responseHeaders.length; i++)
			{
				h = responseHeaders[i];
				//Logger.print(this,responseHeaders[i]);
				if (h.getName()!=null && h.getValue() != null)
				{
					//Logger.debug(Tools.class,"Response headers: name="+h.getName()+" value="+h.getValue());
					//res.setHeader(h.getName(), h.getValue());
				}
			}

			String encoding = null;
			if (httpResponse.getLastHeader("Content-Type")!=null) encoding = httpResponse.getLastHeader("Content-Type").getValue();
			if (encoding==null || encoding.indexOf("charset=")==-1)
			{
				encoding = SetCharacterEncodingFilter.getEncoding();
			}
			else
			{
				encoding = encoding.substring(encoding.indexOf("charset=")+8).trim();
			}

			Logger.debug(Tools.class,"---> ENCODING: " + encoding);

			data = EntityUtils.toString(httpResponse.getEntity(), Charset.forName(encoding));
		}
		catch (Exception ex) {
			Logger.error(Tools.class, ex);
		}

		return(data);
	}

	/**
	 * Rozparsuje retazec, najde tam pozadovany tag a v nom hodnotu atributu, napr v retazci
	 * <topurl value="hodnota">: tag=topurl, param=value, navratova hodnota je hodnota
	 * @param data
	 * @param tag
	 * @param param
	 * @return
	 */
	public static String parseTagValue(String data, String tag, String param)
	{
		if (data == null || tag == null || param == null)
		{
			return(null);
		}
		String ret = null;
		try
		{
			//najdi tag
			int startIndex = data.indexOf("<"+tag);
			if (startIndex != -1)
			{
				//najdi param
				startIndex = data.indexOf(" "+param+"=", startIndex);
				//najdi uvodzovky
				startIndex = data.indexOf("\"", startIndex);
				//najdi koncove uvodzovky
				int endIndex = data.indexOf("\"", startIndex+1);
				if (endIndex > startIndex)
				{
					ret =  data.substring(startIndex+1, endIndex);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(ret);
	}

	public static String join(String[] array)
	{
		return join(array, ",");
	}

	public static String join(String[] array, String glue)
	{
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < array.length; i++)
		{
			result.append(array[i]);
			if (i < array.length - 1) result.append(glue);
		}

		return result.toString();
	}

	public static <T> String join(List<T> array)
	{
		return join(array, ",");
	}

	public static <T> String join(List<T> array, String glue) {
		StringBuilder result = new StringBuilder();
		if (array == null || array.size()<1) return "";
		for(int i = 0; i < array.size(); i++)
		{
			result.append(array.get(i));
			if (i < array.size() - 1) result.append(glue);
		}

		return result.toString();
	}

	/**
	 * Naformatuje velkost suboru ako 1,45 KB
	 * @param lengthLong
	 * @return
	 */
	public static String formatFileSize(long lengthLong)
	{
		String length = "";
		if (lengthLong > (1024 * 1024))
		{
			length = decimalFormat.format((lengthLong / (1024D * 1024D))) + " MB";
		}
		else if (lengthLong > 1024)
		{
			length = decimalFormat.format((lengthLong / 1024D)) + " kB";
		}
		else
		{
			length = decimalFormat.format(lengthLong) + " B";
		}
		return(length);
	}

	/**
	 * Vrati timestamp aktualneho casu
	 * @return
	 */
	public static long getNow()
	{
		return System.currentTimeMillis();
	}

	/**
	 * @deprecated - use Password.generatePassword
	 * @param len
	 * @return
	 */
	@Deprecated
	public static String generatePassword(int len)
	{
		return Password.generatePassword(len);
	}

	/**
	 * skonvertuje String na int, vratane vsetkych kontrol. Ak sa nepodari, vrati defaultValue
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int getIntValue(String value, int defaultValue)
	{
		int ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = Integer.parseInt(value.trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	public static int getIntValue(Integer value)
	{
		return(getIntValue(value, 0));
	}

	/**
	 * Ziska int hodnotu z Integera, alebo vrati defaultValue (ak je null)
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int getIntValue(Integer value, int defaultValue)
	{
		if (value==null)
		{
			return(defaultValue);
		}
		return(value.intValue());
	}

	/**
	 * ziska integer hodnoty z pola stringov a ak je null, tak prazdny zoznam
	 *
	 * @param value
	 * @return
	 */
	public static List<Integer> getIntegerListValue(String[] value) {
		List<Integer> ret = new ArrayList<>();
		if (null != value)
		{
			for (String v : value)
			{
				Integer i = Integer.valueOf(v);
				if (null != i)
				{
					ret.add(i);
				}
			}
		}
		return ret;
	}

	public static float getFloatValue(String value, float defaultValue)
	{
		float ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = Float.parseFloat(value.trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	public static double getDoubleValue(String value, double defaultValue)
	{
		double ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = Double.parseDouble(value.replace(',', '.').trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	public static double getDoubleValue(Object value, double defaultValue)
	{
		double ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = ((Double)value).doubleValue();
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	/**
	 * Ziska BigDecimal hodnotu zo Stringu alebo z defaultValue ak je value null.
	 * Ak <code>value</code> alebo <code>defaultValue</code> nie je platnou reprezentaciou BigDecimal-u vrati BigDecimal s hodnotou 0.
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static BigDecimal getBigDecimalValue(String value, String defaultValue)
	{
		BigDecimal ret = null;
		try
		{
			if (value!=null)
			{
				ret = new BigDecimal(value.replace(',', '.').replace(" ","").trim());
			}
			else
			{
				ret = new BigDecimal(defaultValue);
			}
		}
		catch (Exception ex)
		{
			ret = BigDecimal.valueOf(0);
		}
		return ret;
	}

	/**
	 * Get BigDecimal value from object. If value is null or not valid BigDecimal representation, return defaultValue.
	 * @param value - BigDecimal object or value.toString() will be used to parse
	 * @param defaultValue - default value to return if parsing fails
	 */
	public static BigDecimal getBigDecimalValue(Object value, String defaultValue)
	{
		try
		{
			if (value!=null)
			{
				if (value instanceof BigDecimal) {
					return ((BigDecimal)value);
				}
				return getBigDecimalValue(value.toString(), defaultValue);
			}
		}
		catch (Exception ex)
		{
		}
		return new BigDecimal(defaultValue);
	}

	/**
	 * Ziska BigDecimal hodnotu zo Stringu, ak je null, alebo ak <code>value</code> nie je platnou reprezentaciou BigDecimal-u
	 * vrati BigDecimal s hodnotou 0.
	 * @param value
	 * @return
	 */
	public static BigDecimal getBigDecimalValue(String value)
	{
		return getBigDecimalValue(value, "0");
	}

	public static long getLongValue(String value, long defaultValue)
	{
		long ret = defaultValue;
		try
		{
			if (value!=null)
			{
				ret = Long.parseLong(value.replace(',', '.').trim());
			}
		}
		catch (Exception ex)
		{

		}
		return(ret);
	}

	/**
	 * Vrati hodnotu parametra z requestu, v Tomcate je bug, ak je URL ...?docid=3#kotva, tak to vrati
	 * vratane tej kotvy.
	 * EDIT 13.8.2014 : navratova hodnota je osetrena na XSS
	 * @param request
	 * @param name
	 * @return
	 */
	public static String getParameter(HttpServletRequest request, String name)
	{
		String ret = request.getParameter(name);
		if (ret!=null && ret.indexOf('#')!=-1)
		{
			ret = ret.substring(0, ret.indexOf('#'));
		}
		return org.apache.struts.util.ResponseUtils.filter(ret);
	}

	/**
	 * Ziska string hodnotu z request objektu podla mena, ak neexistuje alebo je prazdne vrati defaultValue
	 * @param request
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getRequestAttribute(HttpServletRequest request, String name, String defaultValue)
	{
		if (request !=null && request.getAttribute(name)!=null && Tools.isNotEmpty((String)request.getAttribute(name)))
		{
			return (String)request.getAttribute(name);
		}
		return defaultValue;
	}

	/**
	 *  Replace stringu na string
	 *
	 *@param  src     zdrojovy string
	 *@param  oldStr  co sa ma nahradit
	 *@param  newStr  za co sa ma nahradit
	 *@return         string src v ktorom je nahradene oldStr za newStr
	 */
	public static String replace(String src, String oldStr, String newStr)
	{
		if (src == null)
		{
			return (null);
		}

		//nemozeme pouzit Tools.isEmpty, pretoze to nam trimne medzery aj crlf a podmienka bude zla
		if (oldStr == null || oldStr.length()==0) return src;

		if (src.indexOf(oldStr) == -1)
		{
			return (src);
		}

		StringBuilder result = new StringBuilder(src.length() + 50);
		int startIndex = 0;
		int endIndex = src.indexOf(oldStr);
		int failsafe = 0;
		while (endIndex != -1 && failsafe++<2000)
		{
			result.append(src.substring(startIndex, endIndex));
			result.append(newStr);
			startIndex = endIndex + oldStr.length();
			endIndex = src.indexOf(oldStr, startIndex);
		}
		result.append(src.substring(startIndex, src.length()));
		return result.toString();
	}

	/**
	 *  Replace stringu na string
	 *
	 *@param  src     zdrojovy string
	 *@param  oldStr  co sa ma nahradit
	 *@param  newStr  za co sa ma nahradit
	 *@return         string src v ktorom je nahradene oldStr za newStr
	 */
	public static StringBuilder replace(StringBuilder src, String oldStr, String newStr)
	{
		if (src == null)
		{
			return (null);
		}
		if (src.indexOf(oldStr) == -1 || newStr == null)
		{
			return (src);
		}
		int replaceIndex = src.indexOf(oldStr);
		int failsafe = 0;
		while (replaceIndex != -1 && failsafe++<300)
		{
			src.replace(replaceIndex, replaceIndex+oldStr.length(), newStr);
			replaceIndex = src.indexOf(oldStr, replaceIndex+newStr.length());
		}

		return src;
	}

	/**
	 *  Funkcia, ktora zisti pocet vyskytov vstupneho substringu v retazci
	 *
	 *	@param  src     	retazec, v ktorom chceme zistit pocet vyskytov
	 *	@param  subString  co sa ma nahradit
	 *
	 *	@return         	pocet vyskytov substring v src, ak je vstupny string null vrati -1, inak stale pocet vyskytov
	 */
	public static int getNumberSubstring(String src, String subString)
	{
		if (src == null)
			return (-1);

		if (src.indexOf(subString) == -1 || src.isEmpty() || subString.isEmpty())
			return (0);

	   int counter = 0;
	   Pattern p = Pattern.compile("\\b" + subString + "\\b"); // co sa ma matchovat, \\b oznacuje hranicu slova, 'medzibankovy' nezapocita, 'iban:' zapocita
	   Matcher m = p.matcher(src); // v com sa to ma matchovat

	   while(m.find())
	   	counter++;

		return (counter);
	}

	/**
	 * Nahradenie retazca oldStr retazcom newStr bez ohladu na velkost pismen
	 * @param src
	 * @param oldStr
	 * @param newStr
	 * @return
	 */
	public static String replaceIgnoreCase(String src, String oldStr, String newStr)
	{
		if (src == null)
		{
			return (null);
		}
		String srcLC = src.toLowerCase();
		oldStr = oldStr.toLowerCase();
		if (srcLC.indexOf(oldStr) == -1)
		{
			return (src);
		}
		StringBuilder result = new StringBuilder(src.length() + 50);
		int startIndex = 0;
		int endIndex = srcLC.indexOf(oldStr);
		while (endIndex != -1)
		{
			result.append(src.substring(startIndex, endIndex));
			result.append(newStr);
			startIndex = endIndex + oldStr.length();
			endIndex = srcLC.indexOf(oldStr, startIndex);
		}
		result.append(src.substring(startIndex, src.length()));
		return result.toString();
	}

	public static String URLEncode(String text) { //NOSONAR
		if (text == null)
			return ("");
		try {
			text = java.net.URLEncoder.encode(text, SetCharacterEncodingFilter.getEncoding());
			text = sk.iway.iwcm.Tools.replace(text, "+", "%20");
		} catch (UnsupportedEncodingException ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return (text);
	}

	public static String URLDecode(String text) { //NOSONAR
		try {
			text = java.net.URLDecoder.decode(text, SetCharacterEncodingFilter.getEncoding());
		} catch (UnsupportedEncodingException|IllegalArgumentException ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return (text);
	}

	/**
	 * Opravuje moznu chybu HTTP response splitting
	 * http://en.wikipedia.org/wiki/HTTP_response_splitting
	 * @param headerParam
	 * @return
	 */
	public static String sanitizeHttpHeaderParam(String headerParam)
	{
		if (headerParam == null) return null;
		headerParam = headerParam.replace('\n', ' ');
		headerParam = headerParam.replace('\r', ' ');
		return headerParam;
	}

	/**
	 * Vrati IP adresu vzdialeneho pocitaca, vie spracovat aj poziadavky za proxy
	 * @param request
	 * @return
	 */
	public static String getRemoteIP(HttpServletRequest request)
	{
		String ip = request.getRemoteAddr();
		//priklad: x-forwarded-for: unknown, 195.168.35.4
		String xForwardedFor = request.getHeader("x-forwarded-for");
		if (Constants.getBoolean("serverBeyoundProxy") && xForwardedFor != null && xForwardedFor.length()>4)
		{
			if ("unknown".equals(xForwardedFor)==false && xForwardedFor.indexOf(".")!=-1)
			{
				String[] values = Tools.getTokens(xForwardedFor, ",", true);
				for (String value : values)
                {
                    //ak tam nie je bodka, je to asi unknown a to ignorujeme
                    if (value.indexOf(".")<1) continue;
                    //bereme prvu hodnotu v poradi
                    ip = value.trim();
                    break;
                }
			}
		}

		return(ip);
	}

	/**
	 * Vrati DNS meno vzdialeneho pocitaca, vie spracovat aj poziadavky za proxy
	 * @param request
	 * @return
	 */
	public static String getRemoteHost(HttpServletRequest request)
	{
		String ip = getRemoteIP(request);

		if (Constants.getBoolean("disableReverseDns")==true) return ip;

		String host = null;

		try
		{
			request.getSession().setAttribute(StatDB.REMOTE_IP, ip);
			host = (String) request.getSession().getAttribute(sk.iway.iwcm.stat.StatDB.REMOTE_HOST);
		}
		catch (Exception e)
		{
			//uz nie je platna session
		}

		if (host == null)
		{
			host = ip;

			//ip z privatnych rozsahov nepouzijem
			if (ip.startsWith("192.168.") || ip.startsWith("10.")) return host;

			//skus zdetekovat ci je to IP adresa, alebo meno
			try
			{
				if (host.indexOf(',')!=-1)
				{
					host = host.substring(0, host.indexOf(',')).trim();
				}
				String domain = host.substring(host.lastIndexOf('.') + 1);
				Integer.parseInt(domain);
				//ok je to cislo, inak by sa to zdrbalo
				host = InetAddress.getByName(ip).getHostName();

				request.getSession().setAttribute(sk.iway.iwcm.stat.StatDB.REMOTE_HOST, host);
			}
			catch (Exception ex)
			{

			}
		}
		return(host);
	}

	/**
	 * Vrati DNS meno vzdialeneho pocitaca na zaklade IP adresy
	 * @param ip
	 * @return
	 */
	public static String getRemoteHost(String ip)
	{
		String host = ip;
		try
		{
			host = InetAddress.getByName(ip).getHostName();
		}
		catch (Exception e)
		{

		}
		return host;
	}

	/**
	 * Vrati true, ak sa jedna o HTTPS pripojenie
	 * @param request
	 * @return
	 */
	public static boolean isSecure(HttpServletRequest request)
	{
		//tu je mozne dorobit ine detekcie, na zaklade nejakej hlavicky (ak SSL nie je riesene Tomcatom)
		String headerName = Constants.getString("requestIsSecureHeaderName");
		if (Constants.getBoolean("serverBeyoundProxy") && Tools.isNotEmpty(headerName))
		{
			String secureHeaderValue = request.getHeader(headerName);
			//pre rezim kde je priamo http alebo https
			if ("https".equalsIgnoreCase(secureHeaderValue)) return true;
		}

		return request.isSecure();
	}

	/**
	 * Ziska hodnotu cookie daneho mena
	 * @param cookies
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getCookieValue(javax.servlet.http.Cookie[] cookies, String name, String defaultValue)
	{
		if (cookies == null) return defaultValue;

		int i;
		int len = cookies.length;
		for (i=0; i<len; i++)
		{
			if (name.equals(cookies[i].getName()))
			{
				return(cookies[i].getValue());
			}
		}
		return(defaultValue);
	}


	/**
	 * Vrati pole typu String, s jednotlivymi polozkami v retazci, ak sa retazec neda rozdelit, vrati prazdne pole
	 * @param groups 	- retazec, ktory sa ma rozparsovat
	 * @param delimiter
	 * @return
	 */
	public static String[] getTokens(String groups, String delimiter)
	{
		return(getTokens(groups, delimiter, true));
	}

	/**
	 * Vrati pole typu String, s jednotlivymi polozkami v retazci, ak sa retazec neda rozdelit, vrati prazdne pole
	 * @param groups
	 * @param delimiter
	 * @param doTrim
	 * @return
	 */
	public static String[] getTokens(String groups, String delimiter, boolean doTrim)
	{
		String[] ret = new String[0];
		StringTokenizer st;
		int i = 0;
		String s;
		try
		{
			if (Tools.isNotEmpty(groups))
			{
				st = new StringTokenizer(groups, delimiter);
				ret = new String[st.countTokens()];
				while (st.hasMoreTokens())
				{
					s = st.nextToken();
					if (doTrim) s = s.trim();
					ret[i++] = s;
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);

	}

	public String[] split(String groups, String delimiter) {
		return split(groups, delimiter, true);
	}

	public String[] split(String groups, String delimiter, boolean doTrim) {
		return getTokens(groups, delimiter, doTrim);
	}

	/**
	 * Vrati pole typu int s jednotlivymi polozkami v retazci
	 * @param groups
	 * @param delimiter
	 * @return
	 */
	public static int[] getTokensInt(String groups, String delimiter)
	{
		if (Tools.isEmpty(groups)) return new int[0];
		String[] retStr = getTokens(groups, delimiter, true);
		int[] retInt = new int[retStr.length];
		for (int i=0; i<retInt.length; i++)
		{
			retInt[i] = Tools.getIntValue(retStr[i], 0);

		}
		return(retInt);

	}

	/**
	 * Vrati pole typu Integer s jednotlivymi polozkami v retazci
	 * @param groups
	 * @param delimiter
	 * @return
	 */
	public static Integer[] getTokensInteger(String groups, String delimiter)
	{
		int[] ints = getTokensInt(groups, delimiter);

		if(ints.length < 1) return new Integer[0];

		return Arrays.stream(ints).boxed().toArray(Integer[]::new);
	}

	/**
	 * Vrati docId stranky, pouzije docDetails v requeste a ked nie je
	 * pouzije docid parameter z requestu
	 * Nutne pouzit ked je nejake remapovanie stranky (cize parameter
	 * je iny ako skutocne zobrazena stranka)
	 * @param request
	 * @return
	 */
	public static int getDocId(HttpServletRequest request)
	{
		int docId = Tools.getIntValue(request.getParameter("docid"), -1);
      DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
      if (docDetails != null)
      {
      	//Logger.println(Tools.class, "JSP REMAP DOC docId="+docDetails.getDocId());
      	docId = docDetails.getDocId();
      }
      return(docId);
	}

	/**
	 * Ziska hodnotu parametra zo zadaneho URL
	 * @param url
	 * @param paramName
	 * @return
	 */
	public static String getParameterFromUrl(String url, String paramName)
	{
		try
		{
			int start = url.indexOf(paramName);
			StringTokenizer st = new StringTokenizer(url.substring(start+paramName.length()+1), " &#");
			if (st.hasMoreTokens())
			{
				return(st.nextToken().trim());
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(null);
	}

	/**
	 * Prida parameter k zadanemu URL
	 * @param url
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public static String addParameterToUrl(String url, String paramName, String paramValue)
	{
		if(paramName == null || paramValue == null) return url;

		paramName = Tools.URLEncode(paramName);

		if (url == null) return("?"+paramName+"="+Tools.URLEncode(paramValue));

		if (url.indexOf("?"+paramName+"=")!=-1 || url.indexOf("&"+paramName+"=")!=-1 || url.indexOf("&amp;"+paramName+"=")!=-1)
		{
			return(url);
		}

		if (url.indexOf('?')==-1)
		{
			return(url + "?" + paramName + "=" + Tools.URLEncode(paramValue));
		}
		return(url + "&amp;" + paramName + "=" + Tools.URLEncode(paramValue));
	}

	public static String addParameterToUrlNoAmp(String url, String paramName, String paramValue)
	{
		if(paramName == null || paramValue == null) return url;

		paramName = Tools.URLEncode(paramName);

		if (url == null) return("?"+paramName+"="+Tools.URLEncode(paramValue));

		if (url.indexOf("?"+paramName+"=")!=-1 || url.indexOf("&"+paramName+"=")!=-1 || url.indexOf("&amp;"+paramName+"=")!=-1)
		{
			return(url);
		}

		if (url.indexOf('?')==-1)
		{
			return(url + "?" + paramName + "=" + Tools.URLEncode(paramValue));
		}
		return(url + "&" + paramName + "=" + Tools.URLEncode(paramValue));
	}

	public static String addParametersToUrl(String url, String paramNameValue)
	{
		if (url == null) return("?"+paramNameValue);

		if (url.indexOf('?')==-1)
		{
			return(url + "?" + paramNameValue);
		}
		return(url + "&amp;" + paramNameValue);
	}

	public static String addParametersToUrlNoAmp(String url, String paramNameValue)
	{
		if (url == null) return("?"+paramNameValue);

		if (url.indexOf('?')==-1)
		{
			return(url + "?" + paramNameValue);
		}
		return(url + "&" + paramNameValue);
	}

	/**
	 * Vrati URL vratane pripadnych parametrov (okrem docid)
	 * @param request
	 * @return
	 */
	public static String getBaseLink(HttpServletRequest request, String[] skipNames)
	{
		String baseLink = PathFilter.getOrigPath(request);
		Enumeration<String> parameters = request.getParameterNames();
		while (parameters.hasMoreElements())
		{
		   String name = parameters.nextElement();
		   if ("docid".equals(name)) continue;

		   boolean skipParam = false;
		   if (skipNames!=null && skipNames.length>0)
		   {
		   	for (int s=0; s<skipNames.length; s++)
		   	{
		   		if (skipNames[s].equals(name))
		   		{
		   			skipParam = true;
		   			break;
		   		}
		   	}
		   }
		   if (skipParam) continue;

		   String[] values = request.getParameterValues(name);
		   for (int i=0; i<values.length; i++)
		   {
			   String value = org.apache.struts.util.ResponseUtils.filter(values[i]);
			   baseLink = addParameterToUrl(baseLink, name, value);
		   }
		}
		return baseLink;
	}

	/**
	 * Oreze z HTML kodu vsetko za start a pred end, start a end sa tam nebude nachadzat
	 * @param data - HTML kod
	 * @param start - zaciatocny text orezu
	 * @param end - koncovy text orezu
	 * @return
	 */
	public static String getCleanBody(String data, String start, String end)
	{
		//odstran vsetko pred <body> a po </body>
		String data_lcase = data.toLowerCase();
		start = start.toLowerCase();
		end = end.toLowerCase();
		int index = data_lcase.indexOf(start);
		if (index != -1)
		{
			if (index > 0)
			{
				int index2 = data_lcase.indexOf(">", index + 1);
				if (index2 > index && index2 < data_lcase.length())
				{
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf(end);
			if (index > 0)
			{
				data = data.substring(0, index);
			}
		}
		return (data);
	}
	/**
	 * vrati String a ak je null tak defaultValue
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static String getStringValue(String value, String defaultValue)
	{
		String ret = defaultValue;
		if(value != null) ret = value;
		return(ret);
	}

	/**
	 * vrati read-only zoznam stringov a ak je null, tak prazdny zoznam
	 *
	 * @param value
	 * @return
	 */
	public static List<String> getStringListValue(String[] value)
	{
		if (value == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(value);
	}

	public static void doNotVerifyCertificates()
	{
		doNotVerifyCertificates(null); //NOSONAR
	}

	/**
	 * Nastavi neverifikaciu certifikatov pre SSL spojenie
	 */
	@SuppressWarnings({"java:S4830", "java:S1186"})
	public static SSLContext doNotVerifyCertificates(String host)
	{
		SSLContext sc = null;
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager()
			{
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[]{};
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
				}
			}};
			// Install the all-trusting trust manager
			sc = SSLContext.getInstance("SSL"); //NOSONAR
			sc.init(null, trustAllCerts, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			/**
			 * pri httpS preskakuje kontrolu na verifikaciu hostname s SSL certifikatom
			 * riesenie chyby javax.net.ssl.SSLHandshakeException: No subject alternative DNS name matching 'moja.domena' found.
			 * vynimku je mozne zadat cez konf. premennu httpsDnsNameMatchingExceptions
			 */
			String httpsDnsNameMatchingExceptions = Constants.getString("httpsDnsNameMatchingExceptions");
			if (Tools.isNotEmpty(httpsDnsNameMatchingExceptions))
			{
				if(Tools.isAnyEmpty(host, httpsDnsNameMatchingExceptions) == false)
				{
					Logger.debug(Tools.class, "HttpsDnsNameMatchingExceptions: Checking: "+host+" vs "+httpsDnsNameMatchingExceptions);
					for(String hostException : Tools.getTokens(httpsDnsNameMatchingExceptions, ",", true))
					{
						if(hostException != null && hostException.length() > 2 && host.endsWith(hostException))
						{
							HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true); //NOSONAR
							break;
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return sc;
	}

	/**
	 * Vrati kod dna podla cisla dna z kalendara (Calendar.DAY_OF_WEEK), vrati
	 * su, mo, tu, we, th, fr, sa
	 * @param dayOfWeek
	 * @return
	 */
	public static String getDayCode(int dayOfWeek)
	{
		if (dayOfWeek==Calendar.SUNDAY) return("su");
		else if (dayOfWeek==Calendar.MONDAY) return("mo");
		else if (dayOfWeek==Calendar.TUESDAY) return("tu");
		else if (dayOfWeek==Calendar.WEDNESDAY) return("we");
		else if (dayOfWeek==Calendar.THURSDAY) return("th");
		else if (dayOfWeek==Calendar.FRIDAY) return("fr");
		else return("sa");
	}

	/**
	 * Vlozi jquery javascript alebo ak uz bol vlozeny, tak prazdny String
	 * @param request
	 * @return String
	 */
	public static String insertJQuery(HttpServletRequest request)
	{
		boolean alreadyInserted = isNotEmpty( (String)(request.getAttribute("jQueryInserted")) );
		request.setAttribute("jQueryInserted", "true");
		if (alreadyInserted || "true".equals(request.getParameter("isDmail")))
			return "";
		else
		{
			StringBuilder htmlCode = new StringBuilder();
			if ("iwcm.interway.sk".equals(request.getServerName()) || "iwcm.iway.sk".equals(request.getServerName())) htmlCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jquery.js\" ></script>");
			else htmlCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jquery.min.js\" ></script>");

			if (request.getAttribute("docDetails")!=null && request.getAttribute("pageGroupDetails")!=null && request.getAttribute("commonPageFunctionsInserted")==null)
			{
				request.setAttribute("commonPageFunctionsInserted", "true");
				request.setAttribute("mailProtection", true);
				htmlCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/page_functions.js.jsp?language=").append(PageLng.getUserLng(request)).append("\" ></script>");
				htmlCode.append("<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"/components/form/check_form.css\" /> ");
			}

			return htmlCode.toString();
		}
	}

	/**
	 * Vlozi jquery javascript alebo ak uz bol vlozeny, tak prazdny String
	 * @param pageContext
	 * @return String
	 */
	public static String insertJQueryUI(PageContext pageContext, String... modules)
	{
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		return insertJQueryUI(request, modules);
	}

	/**
	 * Vlozi jquery javascript alebo ak uz bol vlozeny, tak prazdny String
	 * @param request
	 * @return String
	 */
	public static String insertJQueryUI(HttpServletRequest request, String... modules)
	{
		StringBuilder returnCode = new StringBuilder(insertJQuery(request));
		boolean coreInserted = isNotEmpty( (String)(request.getAttribute("jQueryUI-core-Inserted")) );
		request.setAttribute("jQueryUI-core-Inserted", "true");
		if(!coreInserted)
		{
			if ("iwcm.interway.sk".equals(request.getServerName()))
				returnCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jqui/jquery-ui-core.js\" ></script>");
			else
				returnCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jqui/jquery-ui-core.min.js\" ></script>");
		}

		if(modules == null)
			return returnCode.toString();

		for (String module : modules)
		{
			boolean moduleInserted = isNotEmpty( (String)(request.getAttribute("jQueryUI-"+module+"-Inserted")) );
			if (moduleInserted==false)
			{
				moduleInserted = isNotEmpty( (String)(request.getAttribute("jQueryUI-all-Inserted")) );
			}
			request.setAttribute("jQueryUI-"+module+"-Inserted", "true");
			if(!moduleInserted)
			{
				if ("iwcm.interway.sk".equals(request.getServerName()))
					returnCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jqui/jquery-ui-").append(module).append(".js\" ></script>");
				else
					returnCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jqui/jquery-ui-").append(module).append(".min.js\" ></script>");

				if("datepicker".equals(module))
				{
					String lng = "";
					if (request.getAttribute("PageLng")!=null)
					{
						lng = (String)request.getAttribute("PageLng");
					}
					else
					{
						lng = ResponseUtils.filter(request.getParameter("language"));
						if (lng != null) request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);
						if (lng==null)
						{
							lng = (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
						}
						if (lng == null)
						{
							lng = PageLng.getUserLng(request);
						}
					}
					if(Tools.isEmpty(lng))
						lng = "sk";

					returnCode.append("<script type=\"text/javascript\" src=\"/components/_common/javascript/jqui/i18n/jqui-datepicker-").append(lng).append(".js\" ></script>");
				}
			}
		}

		return returnCode.toString();
	}

	public static String insertAngular2(HttpServletRequest request) {
		return insertAngular2(request, true, true);
	}

	public static String insertAngular2(HttpServletRequest request, boolean minified, boolean insertBaseHref)
	{
		boolean alreadyInserted = Tools.getBooleanValue((String) request.getAttribute("angular2Inserted"), false);
		request.setAttribute("angular2Inserted", "true");

		if (!alreadyInserted)
		{
			StringBuilder htmlCode = new StringBuilder();

			if (insertBaseHref) {
				DocDetails docDetails = new RequestHelper(request).getDocument();
				if (docDetails != null) {
					htmlCode.append("<base href=\"").append(Tools.getBaseHref(request)).append(docDetails.getDocLink()).append("/\">");
				}
			}

			String version = Constants.getString("AngularCDNVersion");

			htmlCode.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/angular.js/").append(version).append("/angular2-polyfills");

			if (minified) {
				htmlCode.append(".min");
			}

			htmlCode.append(".js\"></script>");

			return htmlCode.toString();
		}

		return "";
	}

	public static String getDomainBaseHref(HttpServletRequest request) {

		StringBuilder sb = new StringBuilder();

		sb.append("http");

		if (Tools.isSecure(request)) {
			sb.append("s");
		}

		sb.append("://");
		sb.append(DocDB.getDomain(request));

		return sb.toString();
	}

	/**
	 * Vrati base HTTP adresu servera, cize nieco ako http://www.interway.sk
	 * vie si poradit aj s portami a umiestnenim za reverzne proxy
	 * @param request
	 * @return
	 */
	public static String getBaseHref(HttpServletRequest request)
	{
		String httpServerName = Constants.getString("httpServerName");

		if (request == null)
		{
			if (httpServerName.startsWith("http")==false) httpServerName = "http://"+httpServerName;

			if (Tools.isEmpty(httpServerName)) {
				//skus vydedukovat podla requestBeanu
				RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
				if (rb!=null) return rb.getBaseHref();
				return null;
			}

			StringBuilder baseHref = new StringBuilder(httpServerName);

			if (Constants.getInt("httpServerPort") != 80)
			{
				baseHref.append(':').append(Constants.getInt("httpServerPort"));
			}

			return baseHref.toString();
		}

		StringBuilder baseHref = new StringBuilder();
		if (Tools.isSecure(request)) baseHref.append("https");
		else baseHref.append("http");
		baseHref.append("://");

		if (Tools.isNotEmpty(httpServerName))
		{
			baseHref = new StringBuilder(httpServerName);
		}
		else if (Constants.getBoolean("serverBeyoundProxy") && Tools.isNotEmpty(request.getHeader("x-forwarded-host")))
		{
			baseHref.append(request.getHeader("x-forwarded-host"));
		}
		else
		{
			baseHref.append(Tools.getServerName(request, false));
		}

        if (Constants.getInt("httpServerPort") == -1)
        {
            //ak je httpServerPort nastavene na -1 pridame do URL priamo jeho hodnotu (pouziva sa, ked server bezi na viacerych portoch a nie je maskovany navonok)
            if (request.getServerPort()!=80 && request.getServerPort()!=443)
            {
                baseHref.append(':').append(request.getServerPort());
            }
        }
		else if (Constants.getInt("httpServerPort") != 80)
		{
			baseHref.append(':').append(Constants.getInt("httpServerPort"));
		}

		if (Tools.isEmpty(httpServerName))
		{
			if (Tools.isNotEmpty(request.getContextPath()) && request.getContextPath().equals("/")==false)
			{
				baseHref.append(request.getContextPath());
			}
		}

		return baseHref.toString();
	}

	/**
	 * Vrati base HTTP adresu servera pre loopback HTTP spojenie, cize nieco ako http://localhost:8080/
	 * @param request
	 * @return
	 */
	public static String getBaseHrefLoopback(HttpServletRequest request)
	{
		String baseHref = Constants.getString("baseHrefLoopback");
		if (Tools.isEmpty(baseHref)) baseHref = getBaseHref(request);
		else baseHref = Tools.replace(baseHref, "SERVER_NAME", Tools.getServerName(request));

		return baseHref;
	}

	public static boolean isInteger(String possibleNumber)
	{
		if (possibleNumber == null)
			return false;
		return possibleNumber.trim().matches("^-?[0-9]+$"); //NOSONAR
	}

	/**
	 * Vrati Calendar z rodneho cisla. Tento Calendar
	 * obsahuje vek cloveka s tymto rodnych cislom.
	 *
	 * pocet rokov si potom zistime jednoduchym zavolanim:
	 * 	extractFromAge(rodneCislo).get(Calendar.YEAR)
	 *
	 * Pri priestupnych rokoch ma odchylku +- jeden den.
	 *
	 * @param birthId - rodne cislo
	 * @return {@link Calendar}
	 */
	public static Calendar extractAgeFrom(String birthId)
	{
		if (birthId == null || birthId.length() < 6)
			throw new IllegalArgumentException("Neplatne rodne cislo: "+birthId);
		Calendar now = Calendar.getInstance();
		//calendar pocita mesiace od 0, ale rodne cislo od 1
		now.add(Calendar.MONTH, 1);
		//pretekajuci kalendar...
		now.setLenient(true);
		//zistime a odpocitame roky
		int birthYear = Integer.parseInt(birthId.substring(0,2)) > (now.get(Calendar.YEAR) - 2000) ?
					Integer.parseInt(birthId.substring(0,2)) + 1900 :
					Integer.parseInt(birthId.substring(0,2)) + 2000;
		now.add(Calendar.YEAR, -birthYear );
		//to iste s mesiacmi a dnami
		int birthMonth = Integer.parseInt(birthId.substring(2,4)) > 50 ?
					Integer.parseInt(birthId.substring(2,4)) - 50:
					Integer.parseInt(birthId.substring(2,4));
		now.add(Calendar.MONTH,-birthMonth);
		now.add(Calendar.DAY_OF_MONTH, - Integer.parseInt(birthId.substring(4,6)));
		return now;
	}

	/**
	 * Vracia id prihlaseneho pouzivatela. Identicka metoda je aj v {@link BasketDB},
	 * kde nema co robit
	 *
	 * @param request
	 * @return {@link Integer} userId
	 */
	public static int getUserId(HttpServletRequest request)
	{
		int userId = -1;
		Identity user = UsersDB.getCurrentUser(request);
		if (user != null) userId = user.getUserId();
		return(userId);
	}

	/**
	 * Otestuje, ci pole arrOne obsahuje aspon jeden prvok z pola arrTwo
	 * @param arrOne
	 * @param arrTwo
	 * @return
	 */
	public static boolean containsOneItem(int[] arrOne, int[] arrTwo)
	{
		if (arrOne==null || arrOne.length==0 || arrTwo==null || arrTwo.length==0) return false;

		for (int one : arrOne)
		{
			for (int two : arrTwo)
			{
				if (one == two) return true;
			}
		}
		return false;
	}

	/**
	 * Otestuje, ci pole arrOne obsahuje two
	 * @param arrOne
	 * @param two
	 * @return
	 */
	public static boolean containsOneItem(int[] arrOne, int two)
    {
        if (arrOne==null || arrOne.length==0) return false;

        for (int one : arrOne)
        {
           if (one==two) return true;
        }
        return false;
    }

	/**
	 * Otestuje, ci pole arrOne obsahuje aspon jeden prvok z pola arrTwo
	 * @param arrOne
	 * @param arrTwo
	 * @return
	 */
	public static boolean containsOneItem(String[] arrOne, String[] arrTwo)
	{
		if (arrOne==null || arrOne.length==0 || arrTwo==null || arrTwo.length==0) return false;

		for (String one : arrOne)
		{
			for (String two : arrTwo)
			{
				if (one.equals(two)) return true;
			}
		}
		return false;
	}

    /**
     * Otestuje, ci pole arrOne obsahuje two
     * @param arrOne
     * @param two
     * @return
     */
    public static boolean containsOneItem(String[] arrOne, String two)
    {
        if (arrOne==null || arrOne.length==0 || two==null) return false;

        for (String one : arrOne)
        {
           if (one.equals(two)) return true;
        }
        return false;
    }

	/**
	 * Vrati, ci je rozdiel medzi dvomi double cislami mensi ako zadana odchylka
	 */
	public static boolean doubleEquals(double firstNumber,double secondNumber,double eps)
	{
		return Math.abs( firstNumber - secondNumber ) < eps;
	}

	/**
	 * Vrati, ci sa dve cisla rovnaju v signifikantnych bitoch -
	 * reprezentacia dvoch cisiel v JVM v double formate sa moze lisit
	 * na poslednych 8 bitoch - preto == nemusi fungovat.
	 * Skuste System.out.println((1.0d / 49)*49)
	 */
	public static boolean doubleEquals(double firstNumber,double secondNumber)
	{
		return doubleEquals(firstNumber, secondNumber,1e-7);
	}

	public static boolean isAnyNotEmpty(String... values)
	{
		for (String value : values)
			if (isNotEmpty(value))
				return true;
		return false;
	}


	public static boolean isAnyEmpty(String... values)
	{
		for (String value : values)
			if (isEmpty(value))
				return true;
		return false;
	}

	public static boolean areNotEmpty(String... values)
	{
		return !isAnyEmpty(values);
	}

	/**
	 * Vrati novy zoznam, ktory pozostava z prvkov povodneho zoznamu,
	 * ktore splnaju zadanu podmienku.
	 */
	public static <T> List<T> filter( List<? extends T> originalList,SelectionFilter<? super T> filter)
	{
		List<T> list = new ArrayList<>();
		for (T element : originalList)
			if (filter.fullfilsConditions(element))
				list.add(element);
		return list;
	}

	/**
	 * Vrati serverName z poziadavky aj ked je za proxy.
	 *
	 * @param request
	 * @return
	 */
	public static String getServerName(HttpServletRequest request)
	{
		return getServerName(request, true);
	}

	/**
	 * Vrati server name aj ked je za proxy
	 * @param request
	 * @param useAlias - ak je true robi sa aj serverAlias, false treba len pri tvorbe presmerovani
	 * @return
	 */
	public static String getServerName(HttpServletRequest request, boolean useAlias)
	{
		if (request == null) return null;

		String serverName = request.getServerName();
		if (Constants.getBoolean("serverBeyoundProxy"))
        {
			String forwardHost = Tools.sanitizeHttpHeaderParam(request.getHeader("x-forwarded-host"));
			if (Tools.isNotEmpty(forwardHost))
			{
				if (forwardHost.contains("<")==false && forwardHost.contains(">")==false && forwardHost.contains("\"")==false && forwardHost.contains("'")==false)
				{
					serverName = forwardHost;
				}
			}
        }

		//sometimes serverName is send with :port at the end by some k8s proxy, remove it
		int index = serverName.indexOf(':');
		if (index > 0) serverName = serverName.substring(0, index);

		if (useAlias)
		{
			String domainName = DomainRedirectDB.getDomainFromAlias(serverName);
			if (Tools.isNotEmpty(domainName)) serverName = domainName;
		}

		return (serverName);
	}

	/**
	 * Vracia cely String, ktory pouzivatel uvidi v adresnej liste v prehliadaci,
	 * URL + queryString
	 *
	 * nieco ako:
	 * http://www.google.sk/search?q=something&amp;ie=utf-8&amp;oe=utf-8&amp;aq=t&amp;rls=org.mozilla:sk:official&amp;client=firefox-a
	 */
	public static String getRequestedLink(HttpServletRequest request)
	{
		StringBuilder link = new StringBuilder();
		link.append( Tools.isSecure(request) ? "https" : "http" );
		link.append("://");
		link.append(Tools.getServerName(request));
		String path = PathFilter.getOrigPath(request);
		if (path == null) path = request.getRequestURI();
		link.append(path);
		if (Tools.isNotEmpty(request.getQueryString()))
		{
			link.append('?');
			link.append(request.getQueryString());
		}
		return link.toString();
	}

	public static String getRealPath(String virtualPathParam) {
		String virtualPath = virtualPathParam;

		// JDK 8 + Tomcat 8 nezvladaju url cast v ceste k suboru
		if (Tools.isNotEmpty(virtualPath)) {
			int index = virtualPath.indexOf("?");
			if (index > 0) {
				virtualPath = virtualPath.substring(0, index);
			}

			if (virtualPath.contains(":")) {
				virtualPath = Tools.replace(virtualPath, ":", "-");
			}

			if (virtualPath.contains("*")) {
				virtualPath = Tools.replace(virtualPath, "*", "");
			}

			virtualPath = virtualPath.trim();

			if (virtualPath.contains("%")) {
				virtualPath = Tools.URLDecode(virtualPath);
			}
		}
		if (virtualPath.startsWith("/") == false)
			virtualPath = "/" + virtualPath; // NOSONAR

		// pre cloud WebJET posielame inu cestu k suborom
		if (FilePathTools.isExternalDir(virtualPath)) {
			File file;
			if (virtualPath.startsWith("/shared")
					|| virtualPath.startsWith(Constants.getString("thumbServletCacheDir") + "shared")) {
				// ziskaj cestu pre akoby shared domenu
				file = new File(FilePathTools.getDomainBaseFolder("shared"), virtualPath);
				if (virtualPath.equals("/shared") || virtualPath.equals("/shared/")) {
					try {
						// vytvor hlavnu strukturu pre shared adresar na sietovom disku, domenove sa
						// robia automaticky, toto je komplikovanejsie
						if (file.exists() == false)
							file.mkdirs();
					} catch (Exception ex) {
					}
				}
			} else {
				file = new File(FilePathTools.getDomainBaseFolder(), virtualPath);
			}
			// Logger.debug(Tools.class, "getRealPath, return="+file.getAbsolutePath()+"
			// virtualPath="+virtualPath);

			String path = file.getAbsolutePath();
			if (virtualPath.endsWith("/") && path.endsWith(File.separator) == false)
				path += File.separator;
			return (path);
		}

		// String path = Constants.getServletContext().getRealPath(virtualPath);
		String path = null;
		try {
			// kvoli Tomcat 8.5 a symlinkam to potrebujeme takto
			File f = new File(Constants.getServletContext().getRealPath("/"));
			// to musi ist CanonicalPath aby nam nevracalo
			// /www/tomcat_au2/webapps2/../webapps/iway_2016
			String rootPath = f.getCanonicalPath();
			if (Tools.isEmpty(virtualPath) || "/".equals(virtualPath))
				path = rootPath;
			else {
				// pridavame cestu k root adresaru, to nema zacinat na lomitko
				String addPath = Tools.replace(virtualPath, "/", "" + File.separatorChar).substring(1);
				f = new File(rootPath, addPath);
				// tu musi ist getAbsolutePath aby neprelozilo symlink
				path = f.getAbsolutePath();
			}
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}

		if (path == null) {
			path = Constants.getServletContext().getRealPath("/") + Tools.replace(virtualPath, "/", "" + File.separatorChar);
			path = Tools.replace(path, "" + File.separatorChar + File.separatorChar, "" + File.separatorChar);
		}

		// podpora /admin/generate.jsp kde sa volaju subory z ../support/generators
		// adresara
		if (Tools.isNotEmpty(PathFilter.getCustomPath()) && virtualPath.startsWith("..")
				&& virtualPath.indexOf("/WebContent/") == -1) {
			String myPath = Constants.getServletContext().getRealPath("/") + "/" + virtualPath; // NOSONAR
			if (myPath.indexOf("/WebContent/..") != -1) {
				path = Tools.replace(myPath, "/WebContent/..", "");
			}
		}

		// JDK9 + Tomcat 8 - root path by mala koncit lomitkom (tak to bolo doteraz)
		if ("/".equals(virtualPath) && path.endsWith(File.separator) == false)
			path = path + File.separator;

		if (path.contains(".."))
			path = Tools.replace(path, "/webapps2/../webapps/", "/webapps/");

		path = FileTools.symlinkReplaceToRootPath(path);

		//add last slash if it's on virutal path (to directly combine with file name)
		if (virtualPath.endsWith("/") && path.endsWith(File.separator) == false) path += File.separator;

		return path;
	}

	/**
	 *  capitalize("nieco Nieco nIeCo") => "Nieco Nieco Nieco"
	 */
	public static String capitalize(String source)
	{
		if (isEmpty(source))
			return source;

		Matcher matcher = Pattern.compile("(\\b)(.)(.*?)(\\b)").matcher(source); //NOSONAR

		while (matcher.find())
		{
			String whole = matcher.group(0);
			String spaceBefore = matcher.group(1);
			String firstLetter = matcher.group(2).toUpperCase();
			String remainder = matcher.group(3).toLowerCase();
			String spaceAfter = matcher.group(4);
			source = source.replace(whole, spaceBefore+firstLetter+remainder+spaceAfter);
		}

		return source;
	}

	/**
	 * Funkcia vrati objekt typu Date zo vstupneho retazca dateString na zaklade formatu uvedeneho v parametri formatString.
	 * Napr. Tools.getDateFromString("20091231 235959", "yyyyMMdd HHmmss") vrati objekt Date s nastavenymi hodnotami ako 31.12.2009 23:59:59
	 *
	 * @param dateString		vstupny retazec, z ktoreho chceme ziskat objekt Date, napr. "20091231 235959"
	 * @param formatString	formatovaci retazec, podla ktoreho sa bude formatovat vstupny retazec dateString, napr. "yyyyMMdd HHmmss"
	 *
	 * @return vrati objekt typu Date s nastavenymi hodnotami podla vstupnych parametrov, ak sa vyskytne nejaka parsovacia chyba, vrati null
	 */
	public static Date getDateFromString(String dateString, String formatString)
	{
		Date date = null;
   	try
   	{
   		DateFormat formatter = new SimpleDateFormat(formatString);
   		date = formatter.parse(dateString);
   	}
   	catch (ParseException e)
   	{
   		sk.iway.iwcm.Logger.error(e);
   	}

   	return date;

	}

	/**
	 * Funkcia vrati lokalizovane meno mesiaca z properties, pouzitim pozadovaneho jazyka
	 * @param month - cislo mesiaca v konvencii triedy <code>Calendar</code> (tj. januar = 0, februar = 1 ... december = 11)
	 * @param lng - pozadovany jazyk, ak je <code>null</code>, pouzije sa defaultLanguage z Constants
	 * @return - lokalizovany nazov mesiaca
	 */
	public static String getMonthNameLocalized(int month, String lng)
	{
		String localizedMonth = "";
		Prop prop;
		if(lng == null)
			prop = Prop.getInstance();
		else
			prop = Prop.getInstance(lng);
		switch(month) //NOSONAR
		{
			case 0: localizedMonth = prop.getText("component.calendar.january");break;
			case 1: localizedMonth = prop.getText("component.calendar.february");break;
			case 2: localizedMonth = prop.getText("component.calendar.march");break;
			case 3: localizedMonth = prop.getText("component.calendar.april");break;
			case 4: localizedMonth = prop.getText("component.calendar.may");break;
			case 5: localizedMonth = prop.getText("component.calendar.june");break;
			case 6: localizedMonth = prop.getText("component.calendar.july");break;
			case 7: localizedMonth = prop.getText("component.calendar.august");break;
			case 8: localizedMonth = prop.getText("component.calendar.september");break;
			case 9: localizedMonth = prop.getText("component.calendar.october");break;
			case 10: localizedMonth = prop.getText("component.calendar.november");break;
			case 11: localizedMonth = prop.getText("component.calendar.december");break;
		}
		return localizedMonth;
	}

	/**
	 * Funkcia vrati lokalizovane meno mesiaca z properties, pouzitim predvoleneho jazyka (parameter defaultLanguage z Constants)
	 * @param month - cislo mesiaca v konvencii triedy <code>Calendar</code> (tj. januar = 0, februar = 1 ... december = 11)
	 * @return - lokalizovany nazov mesiaca
	 */
	public static String getMonthNameLocalized(int month)
	{
		return getMonthNameLocalized(month, null);
	}

		// This method returns a buffered image with the contents of an image
	public static BufferedImage toBufferedImage(Image image) {
	    if (image instanceof BufferedImage) {
	        return (BufferedImage)image;
	    }

	    // This code ensures that all the pixels in the image are loaded
	    image = new ImageIcon(image).getImage();

	    // Determine if the image has transparent pixels; for this method's
	    // implementation, see Determining If an Image Has Transparent Pixels
	    //boolean hasAlpha = hasAlpha(image);

	    // Create a buffered image with a format that's compatible with the screen
	    BufferedImage bimage = null;
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    try {
	        // Determine the type of transparency of the new buffered image
	        int transparency = Transparency.OPAQUE;
//	        if (hasAlpha) {
//	            transparency = Transparency.BITMASK;
//	        }

	        // Create the buffered image
	        GraphicsDevice gs = ge.getDefaultScreenDevice();
	        GraphicsConfiguration gc = gs.getDefaultConfiguration();
	        bimage = gc.createCompatibleImage(
	            image.getWidth(null), image.getHeight(null), transparency);
	    } catch (HeadlessException e) {
	        // The system does not have a screen
	    }

	    if (bimage == null) {
	        // Create a buffered image using the default color model
	        int type = BufferedImage.TYPE_INT_RGB;
//	        if (hasAlpha) {
//	            type = BufferedImage.TYPE_INT_ARGB;
//	        }
	        bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
	    }

	    // Copy image to buffered image
	    Graphics g = bimage.createGraphics();

	    // Paint the image onto the buffered image
	    g.drawImage(image, 0, 0, null);
	    g.dispose();

	    return bimage;
	}

	@SuppressWarnings("unchecked")
	public static <T> T firstNonNull(T...possibilities)
	{
		if (possibilities.length == 0)
			return null;
		T lastOne = possibilities[possibilities.length - 1];
		for (T t : possibilities)
			if (t != null)
				return t;

		return lastOne;
	}

	/**
	 * Ziskanie hodnoty ako boolean
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBooleanValue(String value, boolean defaultValue)
	{
		if (value==null)
		{
			return(defaultValue);
		}
		return value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("1");
	}

	/**
	 * deeply clones a Map by cloning all the values.
	 */
	public static Map<String,String> deepCopy(Map<String, String> original) {
	    Map<String, String> copy = new HashMap<>(original.size());
	    for(Map.Entry<String, String> entry : original.entrySet()) {
	        copy.put(entry.getKey(), entry.getValue());
	    }
	    return copy;
	}


	public static boolean addCookie(javax.servlet.http.Cookie cookie, HttpServletResponse response, HttpServletRequest request)
	{

		if (response == null || cookie == null || request == null)
		{
			Logger.debug(Tools.class,"response or cookie or request is null cookie wasn't add");
			return false;
		}

		String cookieName = cookie.getName();
		if ("statBrowserIdNew".equals(cookieName))
		{
			if (canSetCookie("statisticke", request.getCookies())==false)
			{
				Logger.debug(Tools.class,"Nemozem nastavit statBrowserIdNew pretoze nie je povolena skupina statisticke");
				return false;
			}
		}

		if(canSetAnyCookie(request.getCookies()) || cookie.getName().equalsIgnoreCase("JSESSIONID"))
		{
			if (Tools.isSecure(request) && Tools.isNotEmpty(Constants.getString("strictTransportSecurity")))
			{
				cookie.setSecure(true);
			}

			response.addCookie(cookie);
			return true;
		}
		else
		{
			Logger.debug(Tools.class,"adding cookies is denied  " + cookie.getName());
			return false;
		}
	}

	public static boolean canSetCookie(String classification, javax.servlet.http.Cookie[] cookies)
	{
		if (Constants.getBoolean("gdprAllowAllCookies")) return true;

		if(Tools.isEmpty(classification) || "ALWAYS".equals(classification) || "nutne".equals(classification))
		{
			return true;
		}

		boolean gdprInsertAllScriptsBeforeConsent = Constants.getBoolean("gdprInsertAllScriptsBeforeAccept");
		String cookieName = Constants.getString("gdprCookieName");
		String[] enableCookiesClass = Tools.getTokens(Tools.getCookieValue(cookies, cookieName, ""), "_");

		// LPA https://intra.iway.sk/helpdesk/tickets/detail/32155?comment=2267668
		if (gdprInsertAllScriptsBeforeConsent && enableCookiesClass.length == 0) {
			return true;
		}

		for(String enableCookie: enableCookiesClass)
		{
			if(classification.equals(enableCookie))
				return true;
		}

		return false;
	}

	/**
	 * Toto umoznuje uplne vypnut vkladanie cookies, staci nastavit cookie s nazvom cc_cookie_decline (Constants.disableCookiesCookieName) s hodnotu cc_cookie_decline (Constants.disableCookiesCookieValue)
	 * @param actualCookies
	 * @return
	 */
	private static boolean canSetAnyCookie(javax.servlet.http.Cookie[] actualCookies)
	{
		if(actualCookies == null || Tools.isEmpty(Constants.getString("disableCookiesCookieName")) || Tools.isEmpty(Constants.getString("disableCookiesCookieValue")) )
			return true;

		if(actualCookies.length > 0 )
		{
			for (int i = 0; i < actualCookies.length; i++)
			{
				if (actualCookies[i].getName().equals (Constants.getString("disableCookiesCookieName"))
							&& Constants.getString("disableCookiesCookieValue").equals(actualCookies[i].getValue()))
					return false;
			}
		}
		return true;
	}

	/**
	 * Vycisti html od znaciek a pod, vrati cisty text, odstrani viacnasobne whitespaces a newlines
	 * pre viac info : <a href="http://jsoup.org/apidocs/">http://jsoup.org/apidocs/ </a>
	 * @param html
	 * @return
	 */
	public static String html2text(String html)
	{
		return Html2Text.html2text(html);
	}

	/**
	 * Vrati hodnotu parametra, alebo attributu (ak je parameter null). Ak je aj atribut null, vrati null.
	 *
	 * @param name
	 * @param request
	 * @return
	 */
	public static String getParamAttribute(String name, HttpServletRequest request)
	{
		String ret = request.getParameter(name);
		if (ret == null)
		{
			ret = (String) request.getAttribute(name);
		}
		return (ret);
	}

	/**
	 * deep copy of list
	 * @param source
	 * @return
	 */
	public static <T> List<T> copyList(List<T> source)
	{
		List<T> dest = new ArrayList<>();
		for (T item : source)
		{
			dest.add(item);
		}
		return dest;
	}

	/**
	 * performs save conversion from long to int
	 * if long value isn't convertable to int without change of value
	 * exception is thrown
	 * @param l
	 * @return int representation of longsomhow to
	 */
	public static int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}


	/**
	 * Zmaze duplikaty z dlheho stringu, kde su hodnoty oddelene ciarkou napr.(String str = "auto,dom,strom")
	 *
	 * @param txt
	 * @param splitterRegex
	 * @return
	 */
	public static String removeDuplicates(String txt, String splitterRegex)
	{
		List<String> values = new ArrayList<>();
		String[] splitted = txt.split(splitterRegex);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < splitted.length; ++i)
		{
			if (!values.contains(splitted[i]))
			{
				values.add(splitted[i]);
				sb.append(splitterRegex);
				sb.append(splitted[i]);
			}
		}
		return sb.substring(1) + ",";
	}

	/**
	 * V dlhem stringu, kde su hodnoty oddelene ciarkou napr.(String str = "auto,dom,strom")
	 * skontroluje ci cele casti medzi oddelovacmi sa nenachodzaju v comparableText-e
	 *
	 * @param allText
	 * @param splitterRegex
	 * @param comparableText
	 * @return
	 */
	public static boolean containsWholePartInString(String allText, String splitterRegex, String comparableText)
	{
		boolean ret = false;
		String[] splitted = allText.split(splitterRegex);
		for (int i = 0; i < splitted.length; ++i)
		{
			if (comparableText.equals(splitted[i]))
			{
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Z dlheho stringu, kde su hodnoty oddelene ciarkou napr.(String str = "auto,dom,strom")
	 * prida na zaciatok kazdej hodnoty predponu z parametra "addStringStart"
	 *
	 * @param txt
	 * @param splitterRegex
	 * @param addStringStart
	 * @return
	 */
	public static String parseStringAddOnStart(String txt, String splitterRegex, String addStringStart)
	{
		List<String> values = new ArrayList<>();
		String[] splitted = txt.split(splitterRegex);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < splitted.length; ++i)
		{
			if (!values.contains(splitted[i]))
			{
				values.add(addStringStart + splitted[i]);
				sb.append(splitterRegex);
				sb.append(addStringStart + splitted[i]);
			}
		}
		return sb.substring(1) + ",";
	}

	/**
	 *
	 * @param string
	 * @param delimiter
	 * @return
	 */
	public List<String> stringToList(String string, String delimiter)
	{
		return new ArrayList<>(Arrays.asList(string.split(delimiter)));
	}

	/**
	 * prida k ceste parameter time s timestampom (aby sa znovunacitavalo ce es es ko)
	 * @param virtualPath
	 * @return
	 */
	public static String getPathWithTimestamp(String virtualPath)
	{
		String realPath = PathFilter.getRealPath(virtualPath);
		File file = new File(realPath);
		if (file.exists())
		{
			return addParameterToUrl(virtualPath,"time",String.valueOf(file.lastModified()));
		}
		return virtualPath;
	}


	/**
	 * checks if remote IP of requestis listed in allowed IPs constant ( comma separated string )
	 * @param request
	 * @param constantName - meno konfiguracnej premennej (ta je nastavena na znak * alebo zoznam ZACIATKOV povolenych ip adries oddelenych ciarkou)
	 * @return
	 */
	public static boolean checkIpAccess(HttpServletRequest request,String constantName){
		String enabledIPs = Constants.getString(constantName);

		if ("*".equals(enabledIPs)) return true;

		if (Tools.isEmpty(enabledIPs))
		{
			//[#44392 - Vystavenie rest služby pre konkrétny formulár] - uloha #15 =>
			//iba tieto 2 premenne mozu byt prazdne a je to povazovane za OK, ostatne musia mat nastavene * aby sa nestalo, ze kvoli preklepu v nazve premennej sa sluzba otvori navonok
			if ("webEnableIPs".equals(constantName) || "adminEnableIPs".equals(constantName)) return true;
		}

		if (enabledIPs != null && enabledIPs.length() > 1)
		{
			StringTokenizer st = new StringTokenizer(enabledIPs, ",");
			String ip;
			String myIP = Tools.getRemoteIP(request);
			Logger.debug(PathFilter.class,"Checking: " + myIP + " vs " + enabledIPs);
			while (st.hasMoreTokens())
			{
				ip = st.nextToken();
				if (myIP.startsWith(ip.trim()))
				{
					return true;
				}
			}
		}
		return false;

	}


	/**
	 * Vrati request.getUserPrincipal aj pre spring owrapovanie (pouzivane pri integracii na IIS)
	 * @param request
	 * @return
	 */
	public static Principal getUserPrincipal(HttpServletRequest request)
	{
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		//Logger.debug(Tools.class, "getUserPrincipal() attr: "+attr);
		Principal p = attr.getRequest().getUserPrincipal();
		//Logger.debug(Tools.class, "PRINCIPAL attr: " + p);
		if (p != null) return p;

		return request.getUserPrincipal();
	}

	public static String fixURL(String url)
	{
		if (url==null) return("");

		url = DocTools.removeCharsDir(url).toLowerCase();

		url = Tools.replace(url, "//", "/");
		url = Tools.replace(url, "=", "-");
		url = Tools.replace(url, "_-_", "_");
		url = Tools.replace(url, "__", "_");
		url = Tools.replace(url, "___", "_");
		url = Tools.replace(url, "._", "_");
		url = Tools.replace(url, "_.html", ".html");
		url = Tools.replace(url, ".html.html", ".html");
		//ak mame nahradu spojok a bodiek v URL toto treba
		url = Tools.replace(url, "-html.html", ".html");
		//htm je tu kvoli importu web stranok zo zip archivu
		url = Tools.replace(url, ".htm.html", ".htm");
		url = Tools.replace(url, "-htm.html", ".htm");
		return(url);
	}

	/**
	 * Vrati orezany text po najblizsie slovo
	 */
	public static String truncateText(String text, int length){

		if(text.length() > length){
			int index = text.lastIndexOf(" ", length);
			if (index > 0) text = text.substring(0, index);
			else text = text.substring(0, length);
		}
		return text;
	}

	/**
	 * Skonvertuje |* tagy na html tagy
	 * @param text
	 * @return
	 */
	public static String convertToHtmlTags(String text){

		if (text.contains("*||")) text = Tools.replace(text, "*||", "</");
		if (text.contains("*|")) text = Tools.replace(text, "*|", "<");
		if (text.contains("|*")) text = Tools.replace(text, "|*", ">");
		if (text.contains("&amp;#47;")) text = Tools.replace(text, "&amp;#47;", "&#47;");

		return  text;
	}

	public static boolean isNumeric(String str)
	{
		if (Tools.isEmpty(str)) return false;
		try
		{
			Double.parseDouble(str);
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}

	/**
	 * Funkcia podobna JS kodu setTimeout pre asynchronne spustenie ulohy (vid InitServlet volanie localconf.jsp)
	 * @param runnable
	 * @param delay
	 */
	public static void setTimeout(Runnable runnable, int delay){
		new Thread(() -> {
			try {
				Thread.sleep(delay);
				runnable.run();
			}
			catch (Exception e){
				Logger.error(Tools.class, e);
			}
		}).start();
	}

	public static String prepositionsAppendNbsp(String text) {
		String result = text;
		String[] prepositions = Tools.getTokens(Constants.getString("prepositions"), ",");

		for (String preposition : prepositions) {
			preposition = preposition.trim();
			result = Tools.replace(result, " " + preposition + " ", " " + preposition + "&nbsp;");
		}

		return result;
	}

	/**
	 * Odescapuje HTML kod, napr. z &amp;lt; spravi &lt;, je to potrebne napr. pri exporte do Excelu a podobne
	 * @param text
	 * @return
	 */
	public static String unescapeHtmlEntities(String text)
	{
		String returnString = text;
		returnString = Tools.replace(returnString, "&lt;", "<");
		returnString = Tools.replace(returnString, "&gt;", ">");
		returnString = Tools.replace(returnString, "&amp;", "&");
		returnString = Tools.replace(returnString, "&quot;", "\"");

		return returnString;
	}

	public static String escapeHtml(String text) {
		if(text != null) {
//			return StringEscapeUtils.escapeHtml(text);
			return ResponseUtils.filter(text).replace("&amp;", "&");
		} else {
			return null;
		}
	}

    public static String getJsonUnsafe(String json) {
	    return json;
    }

    /**
     * RequestParameterNames
     */

    /**
     *
     * @param request HttpServletRequest
     * @return unescaped request parameter names
     */
    public static Enumeration<String> getRequestParameterNamesUnsafe(HttpServletRequest request) {
        return getRequestParameterNames(request, false);
    }

    /**
     *
     * @param request HttpServletRequest
     * @return escaped request parameter names
     */
    public static Enumeration<String> getRequestParameterNames(HttpServletRequest request) {
        return getRequestParameterNames(request, true);
    }

    /**
     *
     * @param request HttpServletRequest
     * @param sanitize escape request parameter names if it's true
     * @return escaped/unescaped request parameter names
     */
    public static Enumeration<String> getRequestParameterNames(HttpServletRequest request, boolean sanitize) {
        Enumeration<String> e = request.getParameterNames();

        if(sanitize) {
            List<String> names = Collections.list(e);

            for(int i = 0; i < names.size(); i++) {
                names.set(i, escapeHtml(names.get(i)));
            }

            e = Collections.enumeration(names);
        }

        return e;
    }

    /**
     * RequestParameterValues
     */

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @return escaped request parameter values
     */
	public static String[] getRequestParameterValues(HttpServletRequest request, String param) {
		return getRequestParameterValues(request, param, true);
	}

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @param sanitize escape request parameter values if it's true
     * @return escaped/unescaped request parameter values
     */
	public static String[] getRequestParameterValues(HttpServletRequest request, String param, boolean sanitize) {
		if(sanitize) {
			String[] values = request.getParameterValues(param);
			if (values == null) return null;

			for(int i = 0; i < values.length; i++) {
				values[i] = escapeHtml(values[i]);
			}
			return values;
		} else {
			return request.getParameterValues(param);
		}
	}

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @return unescaped request parameter values
     */
	public static String[] getRequestParameterValuesUnsafe(HttpServletRequest request, String param) {
		return getRequestParameterValues(request, param, false);
	}

    /**
     * RequestParameter
     */

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @return escaped request parameter value
     */
	public static String getRequestParameter(HttpServletRequest request, String param) {
		return getRequestParameter(request, param, true);
	}

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @@return unescaped request parameter value
     */
    public static String getRequestParameterUnsafe(HttpServletRequest request, String param) {
        return getRequestParameter(request, param, false);
    }

    /**
     *
     * @param request HttpServletRequest
     * @param param Request parameter name
     * @param sanitize escape request parameter value if it's true
     * @return escaped/unescaped request parameter value
     */
	public static String getRequestParameter(HttpServletRequest request, String param, boolean sanitize) {
		if(sanitize) {
			return escapeHtml(request.getParameter(param));
		} else {
			return request.getParameter(param);
		}
	}

	/**
	 *
	 * @param out JspWriter
	 * @param json JSONArray with news
	 * @param callback name of callback you want to call with json as a parameter
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void printJSON(JspWriter out, JSONArray json, String callback) throws JSONException, IOException {
		if (callback == null) {
			out.println(Tools.getJsonUnsafe(json.toString(4)));
		} else {
			out.println(callback + "(" + Tools.getJsonUnsafe(json.toString(4)) + ");");
		}
	}

    /**
     *
     * @param out JspWriter
     * @param json JSONArray with news
     * @throws JSONException
     * @throws IOException
     */
    public static void printJSON(JspWriter out, JSONArray json) throws JSONException, IOException {
        printJSON(out, json, null);
    }

    public static String getRequestURI(HttpServletRequest request) {
        return Tools.escapeHtml(request.getRequestURI());
    }

    public static String getRequestQueryString(HttpServletRequest request) {
        return Tools.escapeHtml(request.getQueryString());
    }

	public static Calendar nextWorkingDay(Calendar calendar) {
		do {
			calendar.add(Calendar.DAY_OF_YEAR , 1);
		} while (!isWorkingDay(calendar));
		return calendar;
	}

	public static boolean isWorkingDay(Calendar cal) {
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			return false;
		}
		int result = new SimpleQuery().forInt("SELECT holiday_id FROM holidays WHERE hday=? AND hmonth = ? AND (hyear = ? OR hyear is NULL or hyear = '')", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR));
		if (result > 0)
			return false;
		return true;
	}

	public static String trim(String value) {
		if (value == null) return value;

		return value.trim();
	}

	/**
	 * Returns toString value of StringBuilder OR null if StringBuilder is NULL
	 * @param builder
	 * @return
	 */
	public static String toString(StringBuilder builder) {
		if (builder == null) return null;
		return builder.toString();
	}

	/**
	 * @deprecated use DomainRedirectDB.getDomainFromAlias
	 * @param aliasedDomainName
	 * @return
	 */
	@Deprecated
	public static String getDomainFromAlias(String aliasedDomainName) {
		return DomainRedirectDB.getDomainFromAlias(aliasedDomainName);
	}

	/**
	 * Returns Spring ApplicationContext to access spring beans from not spring classes
	 * @return
	 */
	public static ApplicationContext getSpringContext() {
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		ApplicationContext context;
      	if (requestBean == null) context = (ApplicationContext) Constants.getServletContext().getAttribute("springContext");
		else context = requestBean.getSpringContext();

		return context;
	}

	/**
	 * Returns Spring Bean, you can get the SpringBean from not spring classes
	 * @param <T>
	 * @param name
	 * @param clazz
	 * @return
	 */
	public static <T> T getSpringBean(String name, Class<T> clazz) {
		ApplicationContext springContext = getSpringContext();
        if (springContext == null || !springContext.containsBean(name)) {
            return null;
        }

        return springContext.getBean(name, clazz);
    }

	/**
	 * Returns true if b IS NOT NULL and value is TRUE
	 * @param b
	 * @return
	 */
	public static boolean isTrue(Boolean b) {
		return Boolean.TRUE.equals(b);
	}

	/**
	 * Returns true if b IS NOT NULL and is FALSE
	 * @param b
	 * @return
	 */
	public static boolean isFalse(Boolean b) {
		return Boolean.FALSE.equals(b);
	}

	/**
	 * Decode base64 encoded string
	 * @param encoded
	 * @return
	 */
	public static String base64Decode(String encoded) {
		if (Tools.isEmpty(encoded)) return "";
		return new String(Base64.getDecoder().decode(encoded));
	}

	/**
	 * Encode string to base64
	 * @param text
	 * @return
	 */
	public static String base64Encode(String text) {
		if (Tools.isEmpty(text)) return "";
		return Base64.getEncoder().encodeToString(text.getBytes());
	}
}
