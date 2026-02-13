package sk.iway.iwcm.system.datatable.events;

import org.springframework.context.ApplicationEvent;

import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/**
 * Generic event for datatable operations in WebJET, based on
 * https://www.baeldung.com/spring-events
 *
 * Supports listening to specific entity types using:
 * @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
 */
public class DatatableEvent<T> extends ApplicationEvent {

   private static final long serialVersionUID = 1L;
   private T source;
   private DatatableEventType eventType;
   private String clazz;

   // Optional context fields for specific event types
   private T originalEntity;  // For AFTER_SAVE: the original entity before save
   private Long entityId;      // For AFTER_DELETE: the ID of deleted entity
   private Long originalId;    // For AFTER_DUPLICATE: the ID of original record that was duplicated

   /**
    * Constructor for BEFORE_SAVE, BEFORE_DELETE, BEFORE_DUPLICATE events
    * @param source - the entity being operated on
    * @param eventType - type of event
    */
   public DatatableEvent(T source, DatatableEventType eventType) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      this.clazz = source.getClass().getName();
   }

   /**
    * Constructor for AFTER_SAVE event
    * @param source - the saved entity (with updated ID)
    * @param eventType - AFTER_SAVE
    * @param originalEntity - the original entity before save
    */
   public DatatableEvent(T source, DatatableEventType eventType, T originalEntity) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      this.clazz = source.getClass().getName();
      this.originalEntity = originalEntity;
   }

   /**
    * Constructor for AFTER_DELETE event
    * @param source - the deleted entity
    * @param eventType - AFTER_DELETE
    * @param entityId - the ID of deleted entity
    */
   public DatatableEvent(T source, DatatableEventType eventType, Long entityId) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      this.clazz = source.getClass().getName();
      this.entityId = entityId;
   }

   /**
    * Constructor for AFTER_DUPLICATE event
    * @param source - the newly duplicated entity
    * @param eventType - AFTER_DUPLICATE
    * @param originalEntity - not used for duplicate
    * @param originalId - the ID of original record that was duplicated
    */
   public DatatableEvent(T source, DatatableEventType eventType, T originalEntity, Long originalId) {
      super(source);
      this.source = source;
      this.eventType = eventType;
      this.clazz = source.getClass().getName();
      this.originalId = originalId;
   }

   /**
    * Publish this event to Spring ApplicationContext
    */
   public void publishEvent() {
      WebjetEventPublisher.publishEvent(this);
   }

   public T getSource() {
      return source;
   }

   public void setSource(T source) {
      this.source = source;
   }

   public DatatableEventType getEventType() {
      return eventType;
   }

   public void setEventType(DatatableEventType eventType) {
      this.eventType = eventType;
   }

   public String getClazz() {
      return clazz;
   }

   public void setClazz(String clazz) {
      this.clazz = clazz;
   }

   public T getOriginalEntity() {
      return originalEntity;
   }

   public void setOriginalEntity(T originalEntity) {
      this.originalEntity = originalEntity;
   }

   public Long getEntityId() {
      return entityId;
   }

   public void setEntityId(Long entityId) {
      this.entityId = entityId;
   }

   public Long getOriginalId() {
      return originalId;
   }

   public void setOriginalId(Long originalId) {
      this.originalId = originalId;
   }
}
