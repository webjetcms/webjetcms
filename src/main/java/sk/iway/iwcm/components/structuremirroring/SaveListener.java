package sk.iway.iwcm.components.structuremirroring;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

/**
 * Pocuva eventy z WebJETu potrebne na mirroring struktury
 */
@Component
public class SaveListener {

   /**
    * Ulozenie/zmazanie adresara
    * @param event
    */
   @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
   public void handleGroupSave(final WebjetEvent<GroupDetails> event) {
      try {
         //Logger.debug(SaveListener.class, "================================================= handleGroupSave type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
         GroupMirroringServiceV9 service = new GroupMirroringServiceV9();
         service.handleGroupSave(event.getSource(), event.getEventType());
      } catch (Exception ex) {
          Logger.error(SaveListener.class, ex);
      }
   }
   
    /**
    * Ulozenie web stranky vo forme EditorForm (stara verzia)
    * @param event
    */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.editor.EditorForm'")
    public void handleEditorSave(final WebjetEvent<EditorForm> event) {
       try {
          Logger.debug(SaveListener.class, "================================================= handleEditorSave type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
          DocMirroringServiceV9 service = new DocMirroringServiceV9();
          DocDetails doc = event.getSource().toDocDetails();
          service.handleDocSave(doc, event.getEventType());
          event.getSource().setSyncId(doc.getSyncId());
       } catch (Exception ex) {
        Logger.error(SaveListener.class, ex);
       }
    }

   /**
    * Ulozenie/Zmazanie web stranky vo forme DocDetails
    * @param event
    */
   @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
   public void handleDocSave(final WebjetEvent<DocDetails> event) {
      try {
         Logger.debug(SaveListener.class, "================================================= handleEditorSave type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
         DocMirroringServiceV9 service = new DocMirroringServiceV9();
         service.handleDocSave(event.getSource(), event.getEventType());
      } catch (Exception ex) {
        Logger.error(SaveListener.class, ex);
      }
   }

   /**
    * Ulozenie konfiguracie, potrebujeme to na nastavenie syncu korenovych adesarov mirroringu
    * @param event
    */
   @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'")
   public void handleConfSave(final WebjetEvent<ConfDetails> event) {
      try {
         //Logger.debug(SaveListener.class, "================================================= handleConfSave type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
         ConfDetails conf = event.getSource();
         if (conf == null) return;

         Logger.debug(SaveListener.class, "conf name="+conf.getName()+" value="+conf.getValue());

         if ("structureMirroringConfig".equals(conf.getName())) {
            MirroringService.checkRootGroupsConfig();
         }
      } catch (Exception ex) {
        Logger.error(SaveListener.class, ex);
      }
   }

}
