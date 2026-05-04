package sk.iway.iwcm.doc;

import sk.iway.iwcm.Logger;


/**
 *  Casovac na vypis casov pri behu triedy
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Utorok, 2003, november 25
 *@modified     $Date: 2003/11/25 18:01:04 $
 */
public class DebugTimer
{

   private String name;
   private long startTimestamp;
   private long lastTimestamp;
   private boolean enabled = true;

   /**
    *  Constructor for the DebugTimer object
    *
    *@param  name  Description of the Parameter
    */
   public DebugTimer(String name)
   {
      startTimestamp = System.currentTimeMillis();
      lastTimestamp = startTimestamp;
      this.name = name;
   }

   /**
    *  Log diff using Logger.debug
    *@param  message  Description of the Parameter
    */
   public long diff(String message) {
      return diff(message, true);
   }

   /**
    * Log diff using Logger.info
    * @param message
    * @return
    */
   public long diffInfo(String message) {
      return diff(message, false);
   }

   private long diff(String message, boolean debug)
   {
   	long now = System.currentTimeMillis();
      long diff = now - startTimestamp;
      long lastDiff = now - lastTimestamp;
      if (enabled) {
         if (debug) Logger.debug(this,name + " " + diff + " ms (+"+lastDiff+"): " + message);
         else Logger.info(this,name + " " + diff + " ms (+"+lastDiff+"): " + message);
      }
      lastTimestamp = now;

      return lastDiff;
   }


   /**
    * Upravena verzia diff() metody, ktora vypise cas iba vtedy ked cas od posledneho merania je vyssi nez 0ms
    * Uzitocne najma v cykloch kde vacsina opakovani zbehne hned.
    * @param message
    * @return
    */
   public boolean diffNotZero(String message)
   {
   	boolean ret=false;
   	long now = System.currentTimeMillis();
      long diff = now - startTimestamp;
      long lastDiff = now - lastTimestamp;
      if(lastDiff>0)
      {
      	if (enabled) Logger.debug(this,name + " " + diff + " ms (+"+lastDiff+"): " + message);
      	ret=true;
      }
      lastTimestamp = now;
      return ret;
   }

   public long getDiff()
   {
   	return(System.currentTimeMillis() - startTimestamp);
   }

   public long getLastDiff()
   {
   	long now = System.currentTimeMillis();
      long lastDiff = now - lastTimestamp;
      lastTimestamp = now;
      return(lastDiff);
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
