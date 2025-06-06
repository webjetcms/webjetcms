<%@page import="java.util.Map"%><%@page import="sk.iway.iwcm.system.ntlm.NtlmLogonAction"%>
<%@page import="javax.naming.directory.Attributes"%>
<%@page import="javax.naming.directory.SearchResult"%>
<%@page import="javax.naming.directory.InitialDirContext"%>
<%@page import="javax.naming.ldap.LdapContext"%>
<%@page import="javax.naming.directory.DirContext"%>
<%@page import="javax.naming.directory.SearchControls"%>
<%@page import="javax.naming.*"%>
<%@page import="java.util.Hashtable"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.io.StringWriter"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String ldapUrl = Constants.getString("ldapProviderUrl");
if(request.getParameter("ldapUrl") != null) ldapUrl = request.getParameter("ldapUrl");
String ldapLogin = Constants.getString("ldapUsername");
if(request.getParameter("ldapLogin") != null) ldapLogin = request.getParameter("ldapLogin");
String ldapLoginDomainAppend = Constants.getString("ldapDomainAppend");
if(request.getParameter("ldapLoginDomainAppend") != null) ldapLoginDomainAppend = request.getParameter("ldapLoginDomainAppend");
String ldapPassword = Constants.getString("ldapPassword");
if(request.getParameter("ldapPassword") != null) ldapPassword = request.getParameter("ldapPassword");
String ldapOu = "";
if(request.getParameter("ldapOu") != null) ldapOu = request.getParameter("ldapOu");
String ldapFilter = Constants.getString("ldapFilter");
if(request.getParameter("ldapFilter") != null) ldapFilter = request.getParameter("ldapFilter");
String numOfResults = "100";
if(request.getParameter("numOfResults") != null) numOfResults = request.getParameter("numOfResults");
%>
<form action="<%=PathFilter.getOrigPath(request) %>" method="post" name="runForm">
	<table style="margin: 20px 5px 20px 30px;" class="tbMigracia">
		<tr>
			<td>
				<label>LDAP URL</label>
			</td>
			<td>
				<input size="100" type="text" name="ldapUrl" value="<%=ldapUrl%>" placeholder="napr. ldap://ldap.server:389, pripadne aj s OU ldap://ldap.server:389/DC=ad,DC=interway,DC=sk??base"/>
			</td>
		</tr>
		<tr>
			<td>
				<label>LDAP login</label>
			</td>
			<td>
				<input size="100" type="text" name="ldapLogin" value="<%=ldapLogin%>" placeholder="napr. ldaplogin"/>
			</td>
		</tr>
        <tr>
            <td>
                <label>LDAP login domain append</label>
            </td>
            <td>
                <input size="100" type="text" name="ldapLoginDomainAppend" value="<%=ldapLoginDomainAppend%>" placeholder="domena, ktora sa bude automaticky doplnat za meno napr @web.iway.sk"/>
            </td>
        </tr>
		<tr>
			<td>
				<label>LDAP password</label>
			</td>
			<td>
				<input size="100" type="text" name="ldapPassword" value="<%=ldapPassword%>"/>
			</td>
		</tr>
		<tr>
			<td>
				<label>LDAP OU</label>
			</td>
			<td>
				<input size="100" type="text" name="ldapOu" value="<%=ldapOu%>" placeholder="napr. DC=ad,DC=interway,DC=sk"/>
			</td>
		</tr>
        <tr>
            <td>
                <label>LDAP Filter</label>
            </td>
            <td>
                <input size="100" type="text" name="ldapFilter" value="<%=ldapFilter%>"/>
            </td>
        </tr>
		<tr>
			<td>
				<label>Number of results</label>
			</td>
			<td>
				<input type="text" name="numOfResults" value="<%=numOfResults%>"/>
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<input type="submit" name="submit" value="Run" />
			</td>
		</tr>
	</table>
</form>
<p>&nbsp;</p>
<%
if(request.getParameter("submit") != null)
{
out.println(" ==== LDAP: start ==== <br/>");out.flush();
//System.out.println(" ==== LDAP: zacinam import ==== ");
DirContext ctx = null;
try
{
	if(Tools.isAnyEmpty(ldapLogin, ldapPassword, ldapUrl))
	{
	  out.println("Login, password, oro URL is empty!<br/>");out.flush();
	  return;
	}

    //IT MUST BY Hashtable NOT Map
	Hashtable<String, String> env = new Hashtable<>();

   boolean ldapUseSslProtocol = (ldapUrl.contains(":636") || ldapUrl.contains("ldaps"));
   if(ldapUseSslProtocol) env.put(Context.SECURITY_PROTOCOL, "ssl");
   env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.SECURITY_AUTHENTICATION, "Simple");
    env.put(Context.STATE_FACTORIES, "PersonStateFactory");
   env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
   env.put(Context.PROVIDER_URL, ldapUrl);
   env.put(Context.SECURITY_PRINCIPAL,  ldapLogin+ldapLoginDomainAppend);
   env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
   env.put(Context.REFERRAL, "follow");
   env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");

   out.println(" LDAP: connecting ... <br/>");out.flush();
	ctx = new InitialDirContext(env);
	out.println(" LDAP: conection success <br/>");out.flush();

  SearchControls searchCtls = new SearchControls();
  searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

  String[] retAttrs = new String[]{"sAMAccountName","userPrincipalName","mail","title","givenName","sn","cn","streetAddress","l",
				  "postalCode","co","company","telephoneNumber", "mobile",
				  "department","description","memberOf","distinguishedName"};

  searchCtls.setReturningAttributes(retAttrs);

  out.println(" LDAP: reading employess at OU: "+ldapOu+"<br/>");out.flush();
  ldapFilter = Tools.replace(ldapFilter, "!USERNAME!", ldapLogin+ldapLoginDomainAppend);
  NamingEnumeration<SearchResult> results = ctx.search(ldapOu, ldapFilter, searchCtls);

  int count = 0;
  while (results != null && results.hasMore())
  {
      SearchResult sr = results.next();
      Attributes attrs = sr.getAttributes();
      StringBuilder userAttr = new StringBuilder();
      userAttr.append("=== "+(count+1)+". ");
      userAttr.append(" (sAMAccountName="+NtlmLogonAction.getAtrValue(attrs, "sAMAccountName", "")+")");
      userAttr.append(" (userPrincipalName="+NtlmLogonAction.getAtrValue(attrs, "userPrincipalName", "")+")");
      userAttr.append(" (mail="+NtlmLogonAction.getAtrValue(attrs, "mail", "")+")");
      userAttr.append(" (title="+NtlmLogonAction.getAtrValue(attrs, "title", "")+")");
      userAttr.append(" (givenName="+NtlmLogonAction.getAtrValue(attrs, "givenName", "")+")");
      userAttr.append(" (sn="+NtlmLogonAction.getAtrValue(attrs, "sn", "")+")");
      userAttr.append(" (cn="+NtlmLogonAction.getAtrValue(attrs, "cn", "")+")");
      userAttr.append(" (streetAddress="+NtlmLogonAction.getAtrValue(attrs, "streetAddress", "")+")");
      userAttr.append(" (l="+NtlmLogonAction.getAtrValue(attrs, "l", "")+")");
      userAttr.append(" (postalCode="+NtlmLogonAction.getAtrValue(attrs, "postalCode", "")+")");
      userAttr.append(" (co="+NtlmLogonAction.getAtrValue(attrs, "co", "")+")");
      userAttr.append(" (company="+NtlmLogonAction.getAtrValue(attrs, "company", "")+")");
      userAttr.append(" (telephoneNumber="+NtlmLogonAction.getAtrValue(attrs, "telephoneNumber", "")+")");
      userAttr.append(" (mobile="+NtlmLogonAction.getAtrValue(attrs, "mobile", "")+")");
      userAttr.append(" (department="+NtlmLogonAction.getAtrValue(attrs, "department", "")+")");
      userAttr.append(" (description="+NtlmLogonAction.getAtrValue(attrs, "description", "")+")");
      userAttr.append(" (distinguishedName="+NtlmLogonAction.getAtrValue(attrs, "distinguishedName", "")+")");
      userAttr.append(" (memberOf="+attrs.get("memberOf")+")");
      out.println(userAttr+" <br/>");out.flush();
	  if(++count == Tools.getIntValue(numOfResults, 100000)) break;
  }

  out.println("<br/>");out.flush();
  ctx.close();
  ctx = null;
}
catch (Exception ex)
{
   sk.iway.iwcm.Logger.error(ex);
   StringWriter sw = new StringWriter();
   ex.printStackTrace(new PrintWriter(sw));
   out.println(sw.toString().replace("\n", "<br/>"));
}
finally
{
  try
  {
    if (ctx != null)
      ctx.close();
  }
  catch (Exception ex2)
  {
  }
}
out.println(" ==== LDAP: end ==== <br/>");out.flush();
}
%>
