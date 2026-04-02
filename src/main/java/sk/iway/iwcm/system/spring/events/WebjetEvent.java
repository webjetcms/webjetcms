package sk.iway.iwcm.system.spring.events;

import org.springframework.context.ApplicationEvent;

import sk.iway.iwcm.Identity;

/**
 * Genericky event pre udalosti vo WebJETe, zalozene na
 * https://www.baeldung.com/spring-events
 */
public class WebjetEvent<T> extends ApplicationEvent {

   private static final long serialVersionUID = 1L;
   private T source;
   private WebjetEventType eventType;
   private String clazz;
   private Identity user;

   public WebjetEvent(T source, WebjetEventType eventType) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      //nefunguje @EventListener ako handleAfterSaveEditor(final GenericWebjetEvent<EditorForm> event)
      //preto je v condition potrebne testovat clazz
      this.clazz = source.getClass().getName();
   }

   public WebjetEvent(T source, WebjetEventType eventType, Identity user) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      this.user = user;
      //nefunguje @EventListener ako handleAfterSaveEditor(final GenericWebjetEvent<EditorForm> event)
      //preto je v condition potrebne testovat clazz
      this.clazz = source.getClass().getName();
   }

   public void publishEvent() {
		WebjetEventPublisher.publishEvent(this);
	}

   public T getSource() {
      return source;
   }

   public void setSource(T source) {
      this.source = source;
   }

   public WebjetEventType getEventType() {
      return eventType;
   }

   public void setEventType(WebjetEventType eventType) {
      this.eventType = eventType;
   }

   public String getClazz() {
      return clazz;
   }

   public void setClazz(String clazz) {
      this.clazz = clazz;
   }

   public Identity getUser() {
      return user;
   }

   public void setUser(Identity user) {
      this.user = user;
   }
}
