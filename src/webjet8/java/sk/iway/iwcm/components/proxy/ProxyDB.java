package sk.iway.iwcm.components.proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 *  ProxyDB.java - praca s databazou zaznamov proxy, singleton
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.11.2008 14:58:13
 *@modified     $Date: 2009/06/02 06:23:51 $
 */
public class ProxyDB
{
	private static ProxyDB instance;
	private static final Object classLock = ProxyDB.class;

	private ProxyBean[] data;
	private List<ProxyBean> dataList;

	public static ProxyDB getInstance()
	{
		return getInstance(false);
	}

	/**
	 * Ziskanie instancie
	 * @param forceRefresh - ak je nastavene na true, znova sa aktualizuju data z databazy
	 * @return
	 */
	public static ProxyDB getInstance(boolean forceRefresh)
	{
		synchronized (classLock)
		{
			if (instance == null || forceRefresh)
			{
				instance = new ProxyDB();
			}
			return instance;
		}
	}

	/**
	 * Vrati proxy bean pre zadane URL, alebo null
	 * @param url
	 * @return
	 */
	public ProxyBean getProxy(String url)
	{
		for (ProxyBean proxy : data) {
			String[] urls;
			int i = proxy.getLocalUrl().trim().indexOf("\n");
			if (i > 0) {
				urls = Tools.getTokens(proxy.getLocalUrl(), "\n", true);
			} else {
				urls = new String[1];
				urls[0] = proxy.getLocalUrl();
			}
			for (String proxyUrl : urls) {
				if (proxyUrl.startsWith("^") && proxyUrl.endsWith("$")) {
					if (url.equals(proxyUrl.substring(1, proxyUrl.length()-1))) return proxy;
				} else if (proxyUrl.endsWith("$")) {
					if (url.endsWith(proxyUrl.substring(0, proxyUrl.length()-1))) return proxy;
				} else if (url.startsWith(proxyUrl)) {
					return proxy;
				}
			}
		}
		return null;
	}

	/**
	 * Return matched localUrl by url from web prowser
	 * @param proxy
	 * @param url
	 * @return
	 */
	public static String getLocalUrl(ProxyBean proxy, String url) {
		String[] urls;
		int i = proxy.getLocalUrl().trim().indexOf("\n");
		if (i > 0) {
			urls = Tools.getTokens(proxy.getLocalUrl(), "\n", true);
		} else {
			urls = new String[1];
			urls[0] = proxy.getLocalUrl();
		}
		for (String proxyUrl : urls) {
			if (proxyUrl.startsWith("^") && proxyUrl.endsWith("$")) {
				if (url.equals(proxyUrl.substring(1, proxyUrl.length()-1))) return proxyUrl.substring(1, proxyUrl.length()-1);
			} else if (proxyUrl.endsWith("$")) {
				if (url.endsWith(proxyUrl.substring(0, proxyUrl.length()-1))) return proxyUrl.substring(0, proxyUrl.length()-1);
			} else if (url.startsWith(proxyUrl)) {
				return proxyUrl;
			}
		}
		//failsafe
		return proxy.getLocalUrl();
	}

	/**
	 * Vrati proxy so zadanym proxyId
	 * @param proxyId
	 * @return
	 */
	public ProxyBean getProxy(int proxyId)
	{
		for (ProxyBean proxy : data)
		{
			if (proxy.getProxyId()==proxyId)
			{
				return proxy;
			}
		}
		return null;
	}

	/**
	 * Private konstruktor
	 */
	private ProxyDB()
	{
		Logger.debug(ProxyDB.class, "ProxyDB.constructor");
		reloadData();

		ClusterDB.addRefresh(ProxyDB.class);
	}

	private static void fillProxyBean(ProxyBean proxy, ResultSet rs) throws SQLException
	{
		proxy.setProxyId(rs.getInt("proxy_id"));
		proxy.setName(DB.getDbString(rs, "name"));
		proxy.setLocalUrl(DB.getDbString(rs, "local_url"));
		proxy.setRemoteServer(DB.getDbString(rs, "remote_server"));
		proxy.setRemotePort(rs.getInt("remote_port"));
		proxy.setRemoteUrl(DB.getDbString(rs, "remote_url"));
		proxy.setCropStart(DB.getDbString(rs, "crop_start"));
		proxy.setCropEnd(DB.getDbString(rs, "crop_end"));
		proxy.setEncoding(DB.getDbString(rs, "encoding"));
		proxy.setProxyMethod(DB.getDbString(rs, "proxy_method"));
		proxy.setIncludeExt(DB.getDbString(rs, "include_ext"));

		proxy.setAuthMethod(DB.getDbString(rs, "auth_method"));
		proxy.setAuthUsername(DB.getDbString(rs, "auth_username"));
		proxy.setAuthPassword(DB.getDbString(rs, "auth_password"));
		proxy.setAuthHost(DB.getDbString(rs, "auth_host"));
		proxy.setAuthDomain(DB.getDbString(rs, "auth_domain"));

		proxy.setAllowedMethods(DB.getDbString(rs, "allowed_methods"));

		proxy.setKeepCropStart(rs.getBoolean("keep_crop_start"));
		proxy.setKeepCropEnd(rs.getBoolean("keep_crop_end"));
	}

	/**
	 * Nacita data z databazy proxy zaznamov
	 */
	private synchronized void reloadData()
	{
		boolean isDatabaseOK = false;

		List<ProxyBean> dataListNew = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM proxy");
			rs = ps.executeQuery();

			isDatabaseOK = true;

			while (rs.next())
			{
				ProxyBean proxy = new ProxyBean();
				fillProxyBean(proxy, rs);
				dataListNew.add(proxy);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		if (isDatabaseOK==false) throw new NullPointerException();

		//skonvertuj ArrayList na pole
		ProxyBean[] dataNew = dataListNew.toArray(new ProxyBean[0]);
		Logger.debug(ProxyDB.class, "data loaded, size: " + dataNew.length);

		//nastav
		this.data = dataNew;
		this.dataList = dataListNew;
	}

	/**
	 * Ulozi proxy zaznam do databazy a aktualizuje zoznam proxy
	 * @param proxy
	 * @return
	 */
	public boolean saveProxy(ProxyBean proxy)
	{
		boolean saveOK = false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ProxyBean original = null;
		try
		{
			String sql = "INSERT INTO proxy (name, local_url, remote_server, remote_url, remote_port, crop_start, crop_end, encoding, proxy_method, include_ext, auth_method, auth_username, auth_password, auth_host, auth_domain, allowed_methods, keep_crop_start, keep_crop_end) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			if (proxy.getProxyId()>0)
			{
				sql = "UPDATE proxy SET name=?, local_url=?, remote_server=?, remote_url=?, remote_port=?, crop_start=?, crop_end=?, encoding=?, proxy_method=?, include_ext=?, auth_method=?, auth_username=?, auth_password=?, auth_host=?, auth_domain=?, allowed_methods=?, keep_crop_start=?, keep_crop_end=? WHERE proxy_id=?";
				original = getProxy(proxy.getProxyId());
			}
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			int psCounter = 1;
			ps.setString(psCounter++, proxy.getName());
			ps.setString(psCounter++, proxy.getLocalUrl());
			ps.setString(psCounter++, proxy.getRemoteServer());
			ps.setString(psCounter++, proxy.getRemoteUrl());
			ps.setInt(psCounter++, proxy.getRemotePort());
			ps.setString(psCounter++, proxy.getCropStart());
			ps.setString(psCounter++, proxy.getCropEnd());
			ps.setString(psCounter++, proxy.getEncoding());
			ps.setString(psCounter++, proxy.getProxyMethod());
			ps.setString(psCounter++, proxy.getIncludeExt());

			ps.setString(psCounter++, proxy.getAuthMethod());
			ps.setString(psCounter++, proxy.getAuthUsername());
			ps.setString(psCounter++, proxy.getAuthPassword());
			ps.setString(psCounter++, proxy.getAuthHost());
			ps.setString(psCounter++, proxy.getAuthDomain());

			ps.setString(psCounter++, proxy.getAllowedMethods());

			ps.setBoolean(psCounter++, proxy.isKeepCropStart());
			ps.setBoolean(psCounter++, proxy.isKeepCropEnd());

			if (proxy.getProxyId()>0) ps.setInt(psCounter++, proxy.getProxyId());

			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			saveOK = true;

			if (original == null)
			{
				Adminlog.add(Adminlog.TYPE_PROXY_CREATE, "Vytvorene proxy "+proxy.getName(), -1, -1);
			}
			else
			{
				BeanDiffPrinter diff = new BeanDiffPrinter(new BeanDiff().setOriginal(original).setNew(proxy));
				Adminlog.add(Adminlog.TYPE_PROXY_UPDATE, "Zmenene proxy "+proxy.getName()+diff, proxy.getProxyId(), -1);
			}

			reloadData();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return saveOK;
	}

	/**
	 * Vymaze zaznam z databazy
	 * @param proxyId
	 * @return
	 */
	public boolean deleteProxy(int proxyId)
	{
		boolean delOK = false;
		ProxyBean original = getProxy(proxyId);

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			Adminlog.add(Adminlog.TYPE_PROXY_DELETE, "Zmazane proxy "+original.getName(), proxyId, -1);
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM proxy WHERE proxy_id=?");
			ps.setInt(1, proxyId);
			ps.execute();
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			delOK = true;

			reloadData();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		return delOK;
	}

	public ProxyBean[] getData()
	{
		return data;
	}

	public List<ProxyBean> getDataList()
	{
		return dataList;
	}

	/**
	 * Vrati rozne nazvy vzdialenych serverov z tabulky proxy kvoli formularu na filtrovanie
	 *
	 * @return
	 */
	public static List<String> getDistinctRemoteServers()
	{
		List<String> retList = new ArrayList<>();

		try
		{
			retList = new SimpleQuery().forListString("SELECT DISTINCT remote_server FROM proxy");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return retList;
	}

	/**
	 * Vrati rozne nazvy vzdialenych serverov z dodaneho listu proxy kvoli formularu na filtrovanie
	 * @param beans List proxybeanov
	 * @return
	 */
	public static List<String> getDistinctRemoteServers(List<ProxyBean> beans)
	{
		List<String> retList = new ArrayList<>();
		for(ProxyBean p : beans){
			if(!retList.contains(p.getRemoteServer().trim().toLowerCase()))
				retList.add(p.getRemoteServer().trim().toLowerCase());
		}
   	return retList;
	}

	/**
	 * Vrati tie proxy, ktore zodpovedaju filtracnym podmienkam
	 *
	 * @param name				nazov proxy, pouziva sa LIKE
	 * @param remoteServer	vzdialeny server, porovnava sa presny nazov, kedze sa vybera cez selectBox
	 *
	 * @return
	 */
	public static List<ProxyBean> getProxyBeans(String name, String remoteServer)
   {
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM proxy WHERE proxy_id > 0 ");

		if (Tools.isNotEmpty(name))
		{
			sql.append(" AND name LIKE ?");
			params.add("%" + name + "%");

		}

		if (Tools.isNotEmpty(remoteServer))
		{
			sql.append(" AND remote_server = ?");
			params.add(remoteServer);
		}

		List<ProxyBean> proxys = new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(new Mapper<ProxyBean>()
		{
			@Override
			public ProxyBean map(ResultSet rs) throws SQLException
			{
				ProxyBean proxy = new ProxyBean();

				fillProxyBean(proxy, rs);

				return proxy;
			}
		});

		return proxys;
	}

	public static String getCleanBodyIncludeStartEnd(String data, String start, String end)
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
				int index2 = data_lcase.lastIndexOf("<", index+1);
				if (index2 != -1)
				{
					data = data.substring(index2);
				}
			}

			index = data.toLowerCase().indexOf(end);
			if (index > 0)
			{
				data = data.substring(0, index + end.length());
			}
		}
		return (data);
	}

	public static String getCleanBodyIncludeStartNoEnd(String data, String start, String end)
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
				int index2 = data_lcase.lastIndexOf('<', index);
				if (index2 !=-1)
				{
					data = data.substring(index2);
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

	public static String removeLineContains(String html, String searchString, boolean ignoreCase)
	{
		if (Tools.isEmpty(html) || Tools.isEmpty(searchString)) return(html);

		if (ignoreCase) searchString = searchString.toLowerCase();

		StringBuilder out = new StringBuilder(html.length());
		StringTokenizer st = new StringTokenizer(html, "\n");
		String line;
		String lineLC;
		while (st.hasMoreTokens())
		{
			line = st.nextToken();
			if (ignoreCase) lineLC = line.toLowerCase();
			else lineLC = line;

			if (lineLC.indexOf(searchString)!=-1) continue;

			out.append(line + "\n");
		}

		return(out.toString());
	}

	/**
	 * Odstrani z HTML kodu vsetky vyskyty medzi start a end (napr. nejaky TD element), napr.:
	 * html = removeElements(html, "<TD Class=\"ms-vb-icon\">", "</TD>");
	 * @param htmlCode
	 * @param startHtml
	 * @param endHtml
	 * @return
	 */
	public static String removeElements(String htmlCode, String startHtml, String endHtml)
	{
		StringBuilder sb = new StringBuilder();
		int failsafe = 0;
		int lastPos = 0;
		int start = htmlCode.indexOf(startHtml);
		while (start != -1 && failsafe++ < 500 && lastPos != -1)
		{
			sb.append(htmlCode.substring(lastPos, start));

			//posun sa na dalsie miesto
			lastPos = htmlCode.indexOf(endHtml, start);
			if (lastPos != -1) lastPos += endHtml.length();
			start = htmlCode.indexOf(startHtml, start+1);

			if (start == -1)
			{
				//koniec HTML kodu
				sb.append(htmlCode.substring(lastPos));
			}
		}

		return sb.toString();
	}
}