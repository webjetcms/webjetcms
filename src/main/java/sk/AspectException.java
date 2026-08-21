package sk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/**
 * Logs caught SQL exceptions to the admin log.
 */
@Aspect
public class AspectException
{
	@Pointcut("within(sk.iway..*) && !within(sk.iway.iwcm.Constants)")
	public void scope()
	{
	}

	@Before(value = "handler(java.lang.Exception+) && args(exception) && scope()", argNames = "exception")
	public void beforeExceptionHandler(JoinPoint.StaticPart location, JoinPoint.EnclosingStaticPart enclosing, Exception exception)
	{
		logException(exception, location, enclosing);
	}

	protected void logException(Exception exception, JoinPoint.StaticPart location, JoinPoint.StaticPart enclosing)
	{
		if (InitServlet.isWebjetInitialized()==false) return;

		if (exception instanceof SQLException)
		{
			if (exception.toString().contains("adminlog_notify")) return;
			if (exception.toString().contains("stat_views_")) return;
			if (exception.toString().contains("timed out")) return;
			if (exception.toString().contains("Network is down")) return;
			if (exception.toString().contains("Host is down")) return;
			if (exception.toString().contains("Cannot create JDBC driver")) return;
			if (exception.toString().contains("Unknown server host name")) return;
			if (exception.toString().contains("Unknown host")) return;
			if (exception.toString().contains("Cannot get a connection")) return;
			if (exception.toString().contains("Data too long for column 'description'")) return;
			if (exception.toString().contains("already has more than 'max_user_connections' active connections")) return;

			Signature signature = location.getSignature();
			String source = signature.getDeclaringTypeName() + ":" + enclosing.getSourceLocation().getLine();

			Cache cache = Cache.getInstance();
			String cacheKey = "aspectException_"+exception+"-"+source;
			if (cache.getObject(cacheKey)!=null) return;

			StringWriter stackTrace = new StringWriter();
			exception.printStackTrace(new PrintWriter(stackTrace));

			if (stackTrace.toString().contains("at sk.iway.iwcm.Adminlog.add(")) return;

			System.out.println("--------------------- ASPECT start ---------- ");
			System.out.println("(a) " + source + " - " + exception + " t=" + exception);
			System.out.println("signature="+signature);
			System.out.println("source="+source);
			RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (requestBean != null)
			{
				System.out.println("domain="+requestBean.getDomain()+" ip="+requestBean.getRemoteIP()+" userId="+requestBean.getUserId()+" url="+requestBean.getUrl()+" qs="+requestBean.getQueryString());
			}
			System.out.println("exception="+exception);
			System.out.println("stackTrace="+stackTrace);

			Adminlog.add(Adminlog.TYPE_SQLERROR, "SQL ERROR:\nexception: "+exception+"\nsource: "+source+"\nstackTrace:\n"+stackTrace, -1, -1);

			System.out.println("--------------------- ASPECT end ---------- ");

			int auditExceptionTimeout = Constants.getInt("auditExceptionTimeout");
			if (auditExceptionTimeout>0)
			{
				cache.setObject(cacheKey, "true", auditExceptionTimeout);
			}
		}
	}
}
