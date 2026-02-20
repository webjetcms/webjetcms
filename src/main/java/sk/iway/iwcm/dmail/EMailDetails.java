package sk.iway.iwcm.dmail;


/**
 *  drzi info o emaile
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2003/06/14 16:04:06 $
 *@modified     $Date: 2003/06/14 16:04:06 $
 */
public class EMailDetails
{
   private String full_name;
   private String email;
   private String userGroups;
   private int userId=-1;

   /**
    *  Gets the name attribute of the EMailDetails object
    *
    *@return    The name value
    */
   public String getFull_name()
   {
      return full_name;
   }

   /**
    *  Sets the name attribute of the EMailDetails object
    *
    *@param  full_name  The new full_name value
    */
   public void setFull_name(String full_name)
   {
      this.full_name = full_name;
   }

   /**
    *  Sets the email attribute of the EMailDetails object
    *
    *@param  email  The new email value
    */
   public void setEmail(String email)
   {
      this.email = email;
   }

   /**
    *  Gets the email attribute of the EMailDetails object
    *
    *@return    The email value
    */
   public String getEmail()
   {
      return email;
   }

   /**
    *  Sets the userGroups attribute of the EMailDetails object
    *
    *@param  userGroups  The new userGroups value
    */
   public void setUserGroups(String userGroups)
   {
      this.userGroups = userGroups;
   }

   /**
    *  Gets the userGroups attribute of the EMailDetails object
    *
    *@return    The userGroups value
    */
   public String getUserGroups()
   {
      return userGroups;
   }
   public void setUserId(int userId)
   {
      this.userId = userId;
   }
   public int getUserId()
   {
      return userId;
   }
}
