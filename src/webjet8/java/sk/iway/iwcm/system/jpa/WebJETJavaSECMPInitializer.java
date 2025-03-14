package sk.iway.iwcm.system.jpa;

import static sk.iway.iwcm.Tools.isEmpty;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.eclipse.persistence.exceptions.PersistenceUnitLoadingException;
import org.eclipse.persistence.internal.jpa.EntityManagerSetupImpl;
import org.eclipse.persistence.internal.jpa.deployment.JavaSECMPInitializer;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.eclipse.persistence.internal.jpa.deployment.SEPersistenceUnitInfo;
import org.eclipse.persistence.jpa.Archive;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.JarPackaging;

/**
 *  WebJETJavaSECMPInitializer.java
 *  Subclassed in order to override reading metadata from persistence.xml and use datasources specified in poolman.xml
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 22.1.2010 11:18:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("rawtypes")
public class WebJETJavaSECMPInitializer extends JavaSECMPInitializer
{
	protected static WebJETJavaSECMPInitializer initializer;

   // Used as a lock in getJavaSECMPInitializer.
   private static final Object initializationLock = new Object();

   /**
    * Get the singleton entityContainer.
    */
   public static WebJETJavaSECMPInitializer getJavaSECMPInitializer() {
   	return getJavaSECMPInitializer(Thread.currentThread().getContextClassLoader(), null, false);
   }

   public static WebJETJavaSECMPInitializer getJavaSECMPInitializer(ClassLoader loader) {
   	return getJavaSECMPInitializer(loader, null, false);
   }

   public static WebJETJavaSECMPInitializer getJavaSECMPInitializerFromAgent()
   {
      return getJavaSECMPInitializer(Thread.currentThread().getContextClassLoader(), null, true);
   }

   public static WebJETJavaSECMPInitializer getJavaSECMPInitializerFromMain(Map m)
   {
      return getJavaSECMPInitializer(Thread.currentThread().getContextClassLoader(), m, false);
   }

   protected WebJETJavaSECMPInitializer()
   {
      super();
   }

   protected WebJETJavaSECMPInitializer(ClassLoader loader)
   {
      super(loader);
   }

   public static WebJETJavaSECMPInitializer getJavaSECMPInitializer(ClassLoader classLoader, Map m, boolean fromAgent) {
      Logger.debug(WebJETJavaSECMPInitializer.class, "getJavaSECMPInitializer start");

      if(!isInitialized) {
          //if(globalInstrumentation != null) {
              synchronized(initializationLock) {
                  if(!isInitialized) {

                     Logger.debug(WebJETJavaSECMPInitializer.class, "getJavaSECMPInitializer, initializing");

                      initializeTopLinkLoggingFile();
                      if(fromAgent) {
                          AbstractSessionLog.getLog().log(SessionLog.FINER, SessionLog.WEAVER, "cmp_init_initialize_from_agent", (Object[])null);
                      }
                      //usesAgent = true;
                      initializer = new WebJETJavaSECMPInitializer(classLoader);
                      initializer.initialize(m != null ? m : new HashMap(0));
                      // all the transformers have been added to instrumentation, don't need it any more.
                      globalInstrumentation = null;
                  }
              }
          //}
          isInitialized = true;
      }
      if(initializer != null && initializer.getInitializationClassLoader() == classLoader) {
          return initializer;
      } else {
          // when agent is not used initializer does not need to be initialized.
          return new WebJETJavaSECMPInitializer(classLoader);
      }
  }

   /**
    * This method initializes the container.  Essentially, it will try to load the
    * class that contains the list of entities and reflectively call the method that
    * contains that list.  It will then initialize the container with that list.
    */
   @SuppressWarnings("unchecked")
   @Override
   public void initialize(Map m) {
      Logger.debug(WebJETJavaSECMPInitializer.class, "JPA: initialize");

       //boolean keepInitialMaps = keepAllPredeployedPersistenceUnits();
       //if(keepInitialMaps) {
           initialPuInfos = new HashMap();
       //}
       // always create initialEmSetupImpls - it's used to check for puName uniqueness in initPersistenceUnits
       initialEmSetupImpls = new HashMap();
       // ailitchev - copied from findPersistenceUnitInfoInArchives: mkeith - get resource name from prop and include in subsequent call

       //String descriptorPath = (String) m.get(PersistenceUnitProperties.ECLIPSELINK_PERSISTENCE_XML);
       //WebJET zmena
       final Set<Archive> pars = findPersistenceArchives(initializationClassloader);

       try {
           for (Archive archive: pars) {
               AbstractSessionLog.getLog().log(SessionLog.FINER, SessionLog.JPA, "cmp_init_initialize", archive);
               initPersistenceUnits(archive, m);
           }
       } finally {
           for (Archive archive: pars) {
               archive.close();
           }
           initialEmSetupImpls = null;
       }
   }

   /**
	 * WebJET verzia PersistenceUnitPRocessor metody kvoli premenovanemu persistence.xml kvoli jBossu v ING (kde persistence.xml kolidovalo)
	 * @param loader
	 * @return
	 */
   private static Set<Archive> findPersistenceArchives(ClassLoader loader){
      Logger.debug(WebJETJavaSECMPInitializer.class, "findPersistenceArchives");

   	String descriptor = "META-INF/persistence-webjet.xml";

      Set<Archive> pars = new HashSet<>();
      try {
          Enumeration<URL> resources = loader.getResources(descriptor);
          while (resources.hasMoreElements()){
              URL pxmlURL = resources.nextElement();
              URL puRootURL = PersistenceUnitProcessor.computePURootURL(pxmlURL, descriptor);
              Archive archive = PersistenceUnitProcessor.getArchiveFactory(loader).createArchive(puRootURL, descriptor, null);
              pars.add(archive);
          }
      } catch (java.io.IOException|URISyntaxException exc){
          //clean up first
          for (Archive archive : pars) {
              archive.close();
          }
          throw PersistenceUnitLoadingException.exceptionSearchingForPersistenceResources(loader, exc);
      }

      return pars;
  }


	/**
    * Initialize persistence units.
    * Initialization is a two phase process.  First the predeploy process builds the metadata
    * and creates any required transformers. This is WebJET specific version, any metadata from persistence.xml is abandoned.
    * Instead, a new SEPersistenceUnitInfo is created from metadata contained in poolman.xml. Only classes in specific packages
    * with specific class name patterns are included to be scanned as managedClasses.
    * Second the deploy process creates an EclipseLink session based on that metadata.
    */
   @Override
   protected void initPersistenceUnits(Archive archive, Map m){

   	/*
   	 *  povodny PersistenceUnitsList obsahujuci udaje z persistence.xml nahradime vlastnym, postavenym na zaklade DBPool a poolman.xml
   	 */
   	try
		{
   		Logger.debug(WebJETJavaSECMPInitializer.class, "Archive: persistence:" + archive.getEntry("META-INF/persistence.xml")+" root="+archive.getRootURL());
		}
		catch (Exception e)
		{
			//  handle exception
		}


   	//List<SEPersistenceUnitInfo> persistenceUnitsList =  PersistenceUnitProcessor.getPersistenceUnits(archive, initializationClassloader);
		List<SEPersistenceUnitInfo> persistenceUnitsList = new ArrayList<>();

      persistenceUnitsList.clear();
      DBPool dbPool = DBPool.getInstance();

      // nacitaj vsetky DataSources z DBPool a vytvor k nim SEPersistenceUnitInfo, nasledne vloz do Listu
      for(String dbName:DBPool.getDataSourceNames())
      {
      	if (JpaTools.isJPADatasource(dbName))
      	{
	      	DataSource ds = dbPool.getWebJETAbandonedDataSource(dbName);
	      	SEPersistenceUnitInfo puInfo = new SEPersistenceUnitInfo();
	      	puInfo.setPersistenceUnitName(dbName);
	      	puInfo.setNonJtaDataSource(ds);
	      	puInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
	      	puInfo.setPersistenceProviderClassName("sk.iway.iwcm.system.jpa.WebJETPersistenceProvider");
	      	puInfo.setPersistenceUnitRootUrl(archive.getRootURL());

            String[] packages = {
                "sk.iway.iwcm.calendar",
                "sk.iway.iwcm.components.banner.model",
                "sk.iway.iwcm.components.basket",
                "sk.iway.iwcm.components.dictionary.model",
                "sk.iway.iwcm.components.domainRedirects",
                "sk.iway.iwcm.components.enumerations.model",
                "sk.iway.iwcm.components.export",
                "sk.iway.iwcm.components.file_archiv",
                "sk.iway.iwcm.components.gdpr",
                "sk.iway.iwcm.components.insertScript",
                "sk.iway.iwcm.components.inquirySimple",
                "sk.iway.iwcm.components.quiz.jpa",
                "sk.iway.iwcm.components.reservation",
                "sk.iway.iwcm.components.todo",
                "sk.iway.iwcm.dmail",
                "sk.iway.iwcm.doc",
                "sk.iway.iwcm.editor",
                "sk.iway.iwcm.io",
                "sk.iway.iwcm.system.cache",
                "sk.iway.iwcm.system.captcha",
                "sk.iway.iwcm.system.jpa",
                "sk.iway.iwcm.system.msg",
                "sk.iway.iwcm.users",
                "sk.iway.spirit.model"
            };

            List<String> managedClasses = new ArrayList<>();
            for (String packageToScan : packages) {
                managedClasses.addAll(JpaTools.getJpaClassNames("/WEB-INF/classes/" + packageToScan.replace(".", "/")));
            }

            managedClasses.add("sk.iway.iwcm.system.UrlRedirectBean");

	      	if (Tools.isNotEmpty(Constants.getString("jpaAddPackages")))
	      	{
	      		for (String packageToScan : Constants.getString("jpaAddPackages").split(","))
	      		{
	      			if (isEmpty(packageToScan)) continue;
	      			String packagePath = "/WEB-INF/classes/"+packageToScan.replace(".", "/");
	      			packagePath = Tools.replace(packagePath, "/*", "");
	      			managedClasses.addAll(JpaTools.getJpaClassNames(packagePath));
	      		}
	      	}

	      	Logger.debug(WebJETJavaSECMPInitializer.class, "initPersistenceUnits["+dbName+"], beans="+managedClasses.size());

	      	puInfo.setManagedClassNames(managedClasses);
	      	puInfo.setExcludeUnlistedClasses(true);

            List<URL> libUrlsList = new ArrayList<>();

            //pridaj lokacie z JarPackagingu
            try
            {
               List<String> jarLocations = JarPackaging.getJarLocations();
               for (String location : jarLocations)
               {
                  if (location == null) continue;
                  location = Tools.URLDecode(location);
                  if (location.endsWith("-admin.jar") || location.endsWith("-components.jar")) continue;

                  Logger.debug(WebJETJavaSECMPInitializer.class, "JPA: Adding "+location);
                  libUrlsList.add(new File(location).toURI().toURL());
               }
            }
            catch (Exception e)
            {
               sk.iway.iwcm.Logger.error(e);
            }

            if (libUrlsList.size()>0) puInfo.setJarFileUrls(libUrlsList);

	      	Properties properties = new Properties();
	      	String driverClassName = "";
            try
            {
                Connection connection = ds.getConnection();
                driverClassName = DriverManager.getDriver(connection.getMetaData().getURL()).getClass().getName();
                connection.close();
            }
            catch (SQLException e){sk.iway.iwcm.Logger.error(e);}

	      	String jpaTargetDatabase = TargetDatabase.Auto;
	      	if(driverClassName.contains("com.mysql.jdbc.Driver") || driverClassName.contains("mariadb")) {
	      		jpaTargetDatabase = TargetDatabase.MySQL;
            } else if(driverClassName.contains("oracle.jdbc.OracleDriver")) {
	      		jpaTargetDatabase = TargetDatabase.Oracle;
            } else if(driverClassName.contains("net.sourceforge.jtds.jdbc.Driver")) {
	      		jpaTargetDatabase = TargetDatabase.SQLServer;
            } else if(driverClassName.contains("org.postgresql.Driver")) {
                jpaTargetDatabase = TargetDatabase.PostgreSQL;
            }

	      	Logger.debug(WebJETJavaSECMPInitializer.class, "JPA: Setting TargetDatabase to " + jpaTargetDatabase + " driverClassName="+driverClassName);

	      	properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, jpaTargetDatabase);

            if (Constants.getBoolean("serverMonitoringEnableJPA")) properties.setProperty(PersistenceUnitProperties.PROFILER, "sk.iway.iwcm.system.jpa.WebJETPerformanceProfiler");

            setDefaultProperties(properties);

	      	puInfo.setProperties(properties);

	      	puInfo.setClassLoader(this.initializationClassloader);

	      	persistenceUnitsList.add(puInfo);
      	}
      }

       for (SEPersistenceUnitInfo persistenceUnitInfo : persistenceUnitsList)
       {
             // puName uniquely defines the pu on a class loader
             String puName = persistenceUnitInfo.getPersistenceUnitName();

             // don't add puInfo that could not be used standalone (only as composite member).
             if (EntityManagerSetupImpl.mustBeCompositeMember(persistenceUnitInfo)) {
                 continue;
             }

             // If puName is already in the map then there are two jars containing persistence units with the same name.
             // Because both are loaded from the same classloader there is no way to distinguish between them - throw exception.
             EntityManagerSetupImpl anotherEmSetupImpl = null;
             if (initialEmSetupImpls != null){
                 anotherEmSetupImpl = this.initialEmSetupImpls.get(puName);
             }
             if(anotherEmSetupImpl != null) {
                 EntityManagerSetupImpl.throwPersistenceUnitNameAlreadyInUseException(puName, persistenceUnitInfo, anotherEmSetupImpl.getPersistenceUnitInfo());
             }

             // Note that session name is extracted only from puInfo, the passed properties ignored.
             String sessionName = EntityManagerSetupImpl.getOrBuildSessionName(Collections.emptyMap(), persistenceUnitInfo, puName);
             EntityManagerSetupImpl emSetupImpl = callPredeploy(persistenceUnitInfo, m, puName, sessionName);
             if (initialEmSetupImpls != null){
                 this.initialEmSetupImpls.put(puName, emSetupImpl);
             }
             if (initialPuInfos != null){
                 this.initialPuInfos.put(puName, persistenceUnitInfo);
             }
       }
   }

   public static void setDefaultProperties(Properties properties)
   {
      properties.setProperty(PersistenceUnitProperties.CACHE_TYPE_DEFAULT, "None");
      properties.setProperty(PersistenceUnitProperties.CACHE_SIZE_DEFAULT, "0");
      properties.setProperty(PersistenceUnitProperties.CACHE_SHARED_DEFAULT, "false");

      if (Constants.getBoolean("serverMonitoringEnableJPA"))
      {
         properties.setProperty("eclipselink.logging.level", "FINE");
         properties.setProperty("eclipselink.logging.level.sql", "FINE");

         properties.setProperty(PersistenceUnitProperties.LOGGING_PARAMETERS, "true");
         properties.setProperty("eclipselink.jdbc.bind-parameters", "true");
      }

      properties.setProperty("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer");
   }
}
