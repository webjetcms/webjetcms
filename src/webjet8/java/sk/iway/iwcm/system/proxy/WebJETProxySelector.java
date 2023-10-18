package sk.iway.iwcm.system.proxy;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.UsernamePasswordCredentials;
//import org.apache.commons.httpclient.auth.AuthScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  ProxySelector.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.9.2012 14:54:56
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class WebJETProxySelector extends ProxySelector
{
	private ProxySelector defsel = null;
	private String proxyHost = null;
	private int proxyPort = 0;
	private String proxyHostsException = null;

	private String proxyHostHttps = null;
	private int proxyPortHttps = 0;

	//private String proxyUsername = null;
	//private String proxyPassword = null;

	private WebJETProxySelector(ProxySelector def, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyHostsException, String proxyHostHttps, int proxyPortHttps)
	{
		defsel = def;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyHostsException = proxyHostsException;
		if (Tools.isNotEmpty(proxyHostHttps) && proxyHostHttps.length()>3) this.proxyHostHttps = proxyHostHttps;
		if (proxyPortHttps>1) this.proxyPortHttps = proxyPortHttps;
	}

	/**
	 * Inicializacia spojenia cez proxy
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 */
	public static void initProxy(String host, int port, final String username, final String password, final String hostsException, final String httpsHost, final int httpsPort)
	{
		Logger.println(InitServlet.class,"Setting http proxy: " + host+":"+port+", https proxy: " + httpsHost + ":" + httpsPort + " user="+username+" exceptions="+hostsException);

		//System.setProperty("http.proxyType", "4");
      System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", Integer.toString(port));
		Logger.debug(WebJETProxySelector.class, "Setting proxy: http.proxyHost="+host+" proxyPort="+port);

		/* uz netrena, pridavame to v InitServlete podla http nastaveni
      System.setProperty("https.proxyHost", host);
      System.setProperty("https.proxyPort", Integer.toString(port));
      */

		if (Tools.isNotEmpty(httpsHost) && httpsHost.length()>3)
		{
			System.setProperty("https.proxyHost", httpsHost);
			Logger.debug(WebJETProxySelector.class, "Setting httpS proxy, host="+httpsHost);
		}
		if (httpsPort>1)
		{
			System.setProperty("https.proxyPort", Integer.toString(httpsPort));
			Logger.debug(WebJETProxySelector.class, "Setting httpS proxy, port="+httpsPort);
		}

		//System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

		if (Tools.isNotEmpty(hostsException)) System.setProperty("http.nonProxyHosts", Tools.replace(hostsException, ",", "|"));

		String usernameNoDomain = username;
		/* zda sa, ze toto fakt netreba, ani vo VUB nebolo potrebne takto delit, takze zatial zakomentovane
		if (username.contains("\\"))
		{
			int i = username.indexOf("\\");
			String domain = username.substring(0, i);
			usernameNoDomain = username.substring(i+1);

			System.setProperty("http.auth.ntlm.domain", domain);
		}
		*/

		WebJETProxySelector selector = new WebJETProxySelector(ProxySelector.getDefault(), host, port, usernameNoDomain, password, hostsException, httpsHost, httpsPort);
		ProxySelector.setDefault(selector);

		System.setProperty("http.proxySet", "true");

		/*
			TODO: set authenticator
		*/
	}

	/**
	 * Toto je zvlastne, kvoli TB sme to museli presunut do samostatnej metody a inicializovat az na konci InitServletu, pretoze inak sa httpS proxy neautentifikovala
	 */
	public static void setAuthenticator()
	{
		final String username = ""+Constants.getString("proxyUser");
		final String password = ""+Constants.getString("proxyPassword");

		if (username.length()>1)
		{
			Authenticator.setDefault(new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					Logger.debug(WebJETProxySelector.class, "getPasswordAuthentication, requestorType:"+getRequestorType());
					if (getRequestorType() == RequestorType.PROXY)
					{
						String prot = getRequestingProtocol().toLowerCase();
						String host = System.getProperty(prot + ".proxyHost", "");
						if (Tools.isEmpty(host)) host = Constants.getString("proxyHost");
						Logger.debug(WebJETProxySelector.class, "Testing auth for prot:"+prot+" host:"+host+" requestingHost="+getRequestingHost());
						//zakomentovane, pretoze nam to vracalo IP adresu if (getRequestingHost().equalsIgnoreCase(host))
						Logger.debug(WebJETProxySelector.class, "Returning authentication, u=" + username);
						return new PasswordAuthentication(username, password.toCharArray());
					}
					return null;
				}
			});
		}
	}

	/**
	 * Nastavi proxy pre Apache Commons HttpClient v3
	 * @param client
	 */
	/*public static void setProxyForHttpClient(HttpClient client, String url)
	{
		try
		{
			String proxyHost = Constants.getString("proxyHost");
			if (Tools.isEmpty(proxyHost)) return;

			int proxyPort = Constants.getInt("proxyPort");
			String proxyHostsException = Constants.getString("proxyHostsException");
			String proxyUser = Constants.getString("proxyUser");
			String proxyPassword = Constants.getString("proxyPassword");

			Logger.debug(WebJETProxySelector.class, "Setting proxy for http client, url="+url);
			if (Tools.isNotEmpty(proxyHostsException))
			{
				//ziskanie samotnej domeny z http://server.domena/nieco
				if (url.indexOf("://")!=-1) url = url.substring(url.indexOf("://")+3);
				if (url.indexOf("/")!=-1) url = url.substring(0, url.indexOf("/"));
				//odstranenie portu
				if (url.indexOf(":")!=-1) url = url.substring(0, url.indexOf(":"));

				Logger.debug(WebJETProxySelector.class, "Testing "+(","+proxyHostsException+",")+" vs "+(","+url+","));

				if ( (","+proxyHostsException+",").indexOf(","+url+",")!=-1 )
				{
					Logger.debug(WebJETProxySelector.class, "Returning noproxy");
					return;
				}
			}

			Logger.debug(WebJETProxySelector.class, "Returning proxy="+proxyHost+":"+proxyPort+" user="+proxyUser+" pass="+proxyPassword);
			if (Tools.isNotEmpty(proxyUser))
			{
				client.getState().setProxyCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));
			}
			else
			{
				client.getHostConfiguration().setProxy(proxyHost, proxyPort);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}*/

	@Override
	public java.util.List<Proxy> select(URI uri)
	{
		if (uri == null)
		{
			throw new IllegalArgumentException("URI can't be null.");
		}
		Logger.debug(WebJETProxySelector.class, "select: uri="+uri);
		String protocol = uri.getScheme();
		if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
		{
			List<Proxy> list = new ArrayList<>();

			if (Tools.isNotEmpty(proxyHostsException))
			{
				if ( (","+proxyHostsException+",").indexOf(","+uri.getHost()+",")!=-1 )
				{
					Logger.debug(WebJETProxySelector.class, "Returning noproxy");
					list.add(Proxy.NO_PROXY);
					return list;
				}
			}

			Proxy p;
			if ("https".equalsIgnoreCase(protocol) && Tools.isNotEmpty(proxyHostHttps) && proxyPortHttps>1)
			{
				p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostHttps, proxyPortHttps));
				Logger.debug(WebJETProxySelector.class, "Returning proxy https="+proxyHostHttps+":"+proxyPortHttps);
			}
			else
			{
				p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				Logger.debug(WebJETProxySelector.class, "Returning proxy="+proxyHost+":"+proxyPort);
			}
			list.add(p);

			return list;
		}
		if (defsel != null && uri.getPort()!=443 && uri.getPort()!=80)
		{
			Logger.debug(WebJETProxySelector.class, "Returning default proxy");
			return defsel.select(uri);
		}
		else
		{
			Logger.debug(WebJETProxySelector.class, "Returning NO proxy");
			List<Proxy> l = new ArrayList<>();
			l.add(Proxy.NO_PROXY);
			return l;
		}
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
	{
		Logger.debug(WebJETProxySelector.class, "Proxy connection fail:"+ioe);
		if (uri == null || sa == null || ioe == null)
		{
			throw new IllegalArgumentException("Arguments can't be null.");
		}
		if (defsel != null)
				defsel.connectFailed(uri, sa, ioe);
	}
}
