package sk.iway.iwcm.doc;

/**
 *  FieldsFromFtoT.java
 *  
 *  Internal part of {@link DocDetails} class. Fields were moved here in order to reduce memory
 *  consumption of {@link DocDetails}. Custom fields were split in 2 parts, A-E and F-T. This class
 *  stores fields F-T, fields that are rarely used at all.
 *  
 *  The reasoning behind this class's creation is that even though {@link DocDetails} had
 *  all of those fields empty, they nevertheless consumed memory, 4 or 8 bytes per field.
 *  15 x 4 bytes = 60 bytes of wasted space per DocDetails instance. Those fields were outsourced here,
 *  so that only a 4 byte pointer is a part of {@link DocDetails}. In great majority of cases,
 *  this link is pointing to <code>null</code>, consuming 4 bytes instead of 60. On rare occassions,
 *  when any of the fields is not empty, {@link DocDetails} memory consumption suffers a 4 byte overhead.
 *  
 *  An instance of this class exists if and only if at least one of the fields is not empty.
 *  
 *  @see FieldsFromAtoE the remaining fields
 *  @see DocumentAdvancedFields another outsourced memory saving class
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 1.4.2011 16:22:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class FieldsFromFtoT
{
	String fieldF = "";
	String fieldG = "";
	String fieldH = "";
	String fieldI = "";
	String fieldJ = "";
	String fieldK = "";
	String fieldL = "";
	String fieldM = "";
	String fieldN = "";
	String fieldO = "";
	String fieldP = "";
	String fieldQ = "";
	String fieldR = "";
	String fieldS = "";
	String fieldT = "";
}