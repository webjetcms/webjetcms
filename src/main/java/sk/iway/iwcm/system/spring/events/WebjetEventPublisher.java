package sk.iway.iwcm.system.spring.events;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/**
 * Universal event publisher for all ApplicationEvent types in WebJET
 */
public class WebjetEventPublisher {

   /**
    * Publish any ApplicationEvent to Spring ApplicationContext
    * @param event - any event extending ApplicationEvent (WebjetEvent, DatatableEvent, etc.)
    */
   public static void publishEvent(ApplicationEvent event) {
      RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();

      //if ("sk.iway.iwcm.system.ConfDetails".equals(event.getClazz())==false) Logger.debug(WebjetEventPublisher.class, "publishEvent, event.clazz="+event.getClazz()+" event.type="+event.getEventType().name());

      ApplicationContext context;
      if (requestBean == null) context = (ApplicationContext) Constants.getServletContext().getAttribute("springContext");
		else context = requestBean.getSpringContext();

		if (context != null) {
			try {
				context.publishEvent(event);
			} catch (Exception ex) {
				sk.iway.iwcm.Logger.error(ex);
			}
		}
   }
}
