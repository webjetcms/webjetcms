package sk.iway.iwcm.doc;

/**
 *  Drzi info pre DocTreeDB
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2003/05/09 12:54:35 $
 */

public class DocTreeDetails
{
   private int parent;
   private String name;
   private int id;
   private String link;
   private String jsTree = "";

   /**
    *  vrati rodicovsky objekt
    *
    *@return    rodicovsky objekt
    */
   public int getParent()
   {
      return parent;
   }

   /**
    *  nastavi rodicovsky objekt
    *
    *@param  newParent  The new parent value
    */
   public void setParent(int newParent)
   {
      parent = newParent;
   }

   /**
    *  Sets the name attribute of the DocTreeDetails object
    *
    *@param  newName  The new name value
    */
   public void setName(String newName)
   {
      name = newName;
   }

   /**
    *  Gets the name attribute of the DocTreeDetails object
    *
    *@return    The name value
    */
   public String getName()
   {
      return(sk.iway.iwcm.Tools.replace(name, "\"", "&quot;"));
   }

   public String getNameNoSlash()
   {
      return name.replace('\'', ' ');
   }

   /**
    *  Sets the id attribute of the DocTreeDetails object
    *
    *@param  newId  The new id value
    */
   public void setId(int newId)
   {
      id = newId;
   }

   /**
    *  Gets the id attribute of the DocTreeDetails object
    *
    *@return    The id value
    */
   public int getId()
   {
      return id;
   }

   /**
    *  Sets the link attribute of the DocTreeDetails object
    *
    *@param  newLink  The new link value
    */
   public void setLink(String newLink)
   {
      link = newLink;
   }

   /**
    *  Gets the link attribute of the DocTreeDetails object
    *
    *@return    The link value
    */
   public String getLink()
   {
      return link;
   }

   /**
    *  pouzite v javascripte aby sa dal vyrenderovat strom
    *
    *@param  jsTree  The new jsTree value
    */
   public void setJsTree(String jsTree)
   {
      this.jsTree = jsTree;
   }

   /**
    *  Gets the jsTree attribute of the DocTreeDetails object
    *
    *@return    The jsTree value
    */
   public String getJsTree()
   {
      return jsTree;
   }
}
