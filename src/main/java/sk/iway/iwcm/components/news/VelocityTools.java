package sk.iway.iwcm.components.news;

import sk.iway.iwcm.Tools;

public class VelocityTools {

   private VelocityTools() {
      //private constructor
   }

   /**
    * Aktualizuje zapis Velocity Template z V1 na V2 pre jednoduchsiu aktualizaciu WebJETov
    * @param template
    * @return
    */
   public static String upgradeTemplate(String template) {
      String upgraded = template;
      upgraded = Tools.replace(upgraded, "$velocityCount", "$foreach.count");
      upgraded = Tools.replace(upgraded, "$velocityHasNext", "$foreach.hasNext");
      return upgraded;
   }
}
