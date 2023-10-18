package sk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import org.aspectj.lang.Signature;
import org.aspectj.lang.JoinPoint.StaticPart;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/**
 *  AspectException.aj - AspectJ kod, ktory zaloguje SQL exception do adminlogu
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 26.1.2009 16:33:58
 *@modified     $Date: 2009/09/10 19:33:08 $
 */
public aspect AspectException
{
	public pointcut scope(): within(sk.iway..*) && !within(sk.iway.iwcm.Constants) && !within(sk.iway.iwcm.update.UpdateAction);

	before(Exception e): handler(Exception+) && args(e) && scope()
	{
		logException(e, thisJoinPointStaticPart, thisEnclosingJoinPointStaticPart);
	}

	protected void logException(Exception e, StaticPart location, StaticPart enclosing)
	{
		if (InitServlet.isWebjetInitialized()==false) return;
		
		if (e instanceof SQLException)
		{
			if (e.toString().indexOf("adminlog_notify")!=-1) return;
			if (e.toString().indexOf("stat_views_")!=-1) return;
			if (e.toString().indexOf("timed out")!=-1) return;
			if (e.toString().indexOf("Network is down")!=-1) return;
			if (e.toString().indexOf("Host is down")!=-1) return;
			if (e.toString().indexOf("Cannot create JDBC driver")!=-1) return;
			if (e.toString().indexOf("Unknown server host name")!=-1) return;
			if (e.toString().indexOf("Unknown host")!=-1) return;
			if (e.toString().indexOf("Cannot get a connection")!=-1) return;
			if (e.toString().indexOf("Data too long for column 'description'")!=-1) return;
			if (e.toString().indexOf("already has more than 'max_user_connections' active connections")!=-1) return;
			
			Signature signature = location.getSignature();
         
         String source = signature.getDeclaringTypeName() + ":" + (enclosing.getSourceLocation().getLine());
         
         Cache c = Cache.getInstance();
         String cacheKey = "aspectException_"+e+"-"+source;
         if (c.getObject(cacheKey)!=null) return;
         
         StringWriter sw = new StringWriter();
      	e.printStackTrace(new PrintWriter(sw));
      	
      	if (sw.toString().indexOf("at sk.iway.iwcm.Adminlog.add(")!=-1) return;
         
			System.out.println("--------------------- ASPECT zaciatok ---------- ");
			System.out.println("(a) " + source + " - " + e.toString()+" t="+e);
			System.out.println("signature="+signature);
			System.out.println("source="+source);
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null)
			{
				System.out.println("domain="+rb.getDomain()+" ip="+rb.getRemoteIP()+" userId="+rb.getUserId()+" url="+rb.getUrl()+" qs="+rb.getQueryString());
			}
			System.out.println("e="+e);
			System.out.println("stackTrace="+sw.toString());
			
			Adminlog.add(Adminlog.TYPE_SQLERROR, "SQL ERROR:\nexception: "+e+"\nsource: "+source+"\nstackTrace:\n"+sw.toString(), -1, -1);
			
			System.out.println("--------------------- ASPECT koniec ---------- ");
			
			int auditExceptionTimeout = Constants.getInt("auditExceptionTimeout");
			if (auditExceptionTimeout>0)
			{
				c.setObject(cacheKey, "true", Constants.getInt("auditExceptionTimeout"));
			}
		}
	}
}
