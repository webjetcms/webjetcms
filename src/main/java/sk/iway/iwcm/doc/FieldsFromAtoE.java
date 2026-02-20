package sk.iway.iwcm.doc;

/**
 *  FieldsFromAtoE.java
 *  
 *  Internal part of {@link DocDetails} class. Fields were moved here in order to reduce memory
 *  consumption of {@link DocDetails}. Custom fields were split in 2 parts, A-E and F-T. This class
 *  stores fields A-E, fields that are the most often used if any custom field is used at all.
 *  
 *  Documents in most WebJET installations, however, do not utilize any of those fields, thus
 *  the pointer from {@link DocDetails}.firstFields remains null and saves some space, too.
 *  
 *  An instance of this class exists if and only if at least one of the fields is not empty.
 *  
 *  @see FieldsFromFtoT the remaining fields
 *  @see DocumentAdvancedFields another outsourced memory saving class
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 1.4.2011 16:42:37
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class FieldsFromAtoE
{
	String fieldA = "";
	String fieldB = "";
	String fieldC = "";
	String fieldD = "";
	String fieldE = "";
}