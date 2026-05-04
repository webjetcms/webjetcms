package sk.iway.iwcm.components.news;

import java.util.Properties;


public class VelocityEngine extends org.apache.velocity.app.VelocityEngine
{

	public VelocityEngine()
	{
		super();
		addLogger();
	}

	public VelocityEngine(String propsFilename)
	{
		super(propsFilename);
		addLogger();
	}

	public VelocityEngine(Properties p)
	{
		super(p);
		addLogger();
	}

	private void addLogger() {
		//this.addProperty("runtime.log.logsystem.class", VelocityLogger.class.getCanonicalName());
	}
}