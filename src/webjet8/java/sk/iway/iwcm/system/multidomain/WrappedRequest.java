package sk.iway.iwcm.system.multidomain;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 *  WrappedRequest.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 24.10.2008 13:54:05
 *@modified     $Date: 2008/10/28 09:48:59 $
 */
public class WrappedRequest extends HttpServletRequestWrapper
{
	private Map<String, String[]> params = new Hashtable<>();
   private String uri;
   private String queryString = null;
   private StringBuffer url;

   public WrappedRequest(HttpServletRequest request, String newUri) {
       super(request);
       int w = newUri.indexOf('?');
       if(w==(-1))
           this.uri = newUri;
       else {
           this.uri = newUri.substring(0, w);
           this.queryString = newUri.substring(w+1);
           int j = this.queryString.indexOf('#');
           if(j!=(-1))
               this.queryString = this.queryString.substring(0, j);
       }
       this.url = new StringBuffer();
       url.append(request.getScheme()).append("://").append(request.getLocalAddr());
       int port = request.getLocalPort();
       if(port!=80)
           url.append(":" + port);
       url.append(request.getContextPath()).append(newUri);
       params.putAll(request.getParameterMap());
       if(queryString!=null) {
           String[] nameValues = queryString.split("&");
           for(int i=0; i<nameValues.length; i++) {
               int n = nameValues[i].indexOf('=');
               if(n>0) {
                    String[] value = new String[1];
                    value[0] = nameValues[i].substring(n+1);
                   params.put(nameValues[i].substring(0, n), value);
               }
           }
       }
   }
   @Override
   public String getParameter(String name)
   {
       Object o = params.get(name);
       if (o == null) return null;
       if (o instanceof String)
       {
           return (String)o;
       }
       else if (o instanceof String[])
       {
           String[] arr = (String[])o;
           if (arr.length>0) return arr[0];
       }
       return String.valueOf(o);
   }

   @Override
   public Map<String, String[]> getParameterMap() {
   	 //toto tu kvoli performance nechcem return params == null ? null : (Map) params.clone();
       return params;
   }
   @Override
   public Enumeration<String> getParameterNames() {
       return ((Hashtable<String, String[]>)params).keys();
   }
   @Override
   public String getQueryString() {
       return queryString;
   }
   @Override
   public String getServletPath() {
       return uri;
   }
   @Override
   public String getRequestURI() {
       return getContextPath() + uri;
   }
   @Override
   public StringBuffer getRequestURL() {
       return url;
   }
}
