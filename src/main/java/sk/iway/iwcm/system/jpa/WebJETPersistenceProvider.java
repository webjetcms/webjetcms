package sk.iway.iwcm.system.jpa;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.eclipse.persistence.internal.jpa.deployment.SEPersistenceUnitInfo;
import org.eclipse.persistence.jpa.PersistenceProvider;

/**
 *  WebJETPersistenceProvider.java
 *  EclipseLink PersistenceProvider subclass, that uses WebJETInitializationHelper as its PersistenceInitialionHelper.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 22.1.2010 17:03:02
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class WebJETPersistenceProvider extends PersistenceProvider
{
    @SuppressWarnings("rawtypes")
	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map properties){
      Map nonNullProperties = (properties == null) ? new HashMap() : properties;

      if (checkForProviderProperty(nonNullProperties)){
          String name = (emName == null) ? "" : emName;
          ClassLoader loader = getClassLoader(name, nonNullProperties);
          WebJETJavaSECMPInitializer initializer = WebJETJavaSECMPInitializer.getJavaSECMPInitializer(loader);
          SEPersistenceUnitInfo  unitInfo = initializer.findPersistenceUnitInfo(name, nonNullProperties);
          return createEntityManagerFactoryImpl(unitInfo, nonNullProperties, true);
      }

      // Not EclipseLink so return null;
      return null;
  }
}
