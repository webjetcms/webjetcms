package sk.iway.iwcm.doc;

/**
 *  RarelyUsedFields.java
 *  
 *  Internal part of {@link DocDetails} class. Fields were moved here in order to reduce memory
 *  consumption of {@link DocDetails} in case none of the fields here is set. That happens most often in cases of
 *  documents loaded via {@link DocDB}.basicDocDetails property which loads only a limited number of fields
 *  for a {@link DocDetails} instance and none of the fields outsourced here.
 *  
 *  Can save ~100 bytes per instance.
 *  
 *  @see FieldsFromAtoE	a class with the same purpose
 *  @see FieldsFromFtoT a class with the same purpose
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 1.4.2011 18:22:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class DocumentAdvancedFields
{
	String docLink;
	String data = "";
	String eventDateString = "";
	String eventTimeString = "";
	String historyApproveDate;
	String historyApprovedByName;
	String historyDisapprovedByName;
	String historySaveDate;
	String htmlData = "";
	String htmlHead = "";
	String tempName;
	String perexImage = "";
	String perexPlace = "";
	String publishEndString = "";
	String publishEndTimeString = "";
	String publishStartString = "";
	String publishStartStringExtra = "";
	String publishStartTimeString = "";
	String syncDefaultForGroupId;
	String syncRemotePath;
	int footerDocId = -1;
	int headerDocId = -1;
	int menuDocId = -1;
	int rightMenuDocId = -1;
	int logonPageDocId = 0;
	int syncId;
	int syncStatus;
	int historyId;
	int historyApprovedBy;
	int historyDisapprovedBy;
	long publishStart;
	long publishEnd;
	long eventDate;
	int forumCount;
	int authorId;
	String authorName;
	String fileName;
	String authorEmail;
	String authorPhoto;
	int viewsTotal = 0;
}