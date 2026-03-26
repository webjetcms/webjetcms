package sk.iway.iwcm.filebrowser;

import static sk.iway.iwcm.Tools.isNotEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.helpers.MailHelper;

/**
 *  UnusedFilesCleaner.java
 *
 *  Cron task disabling file-pages of files that
 *  are no longer referenced by any page/component/banner
 *
 *  Takes 2 parameters:
 *  1. directory to scan in - defaults to "/files"
 *  2. notification mail recipient, defaults to null
 *
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.12.2010 14:56:04
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UnusedFilesCleaner
{
	private final String rootDirectory;
	private final String recipient;

	public UnusedFilesCleaner(String rootDirectory, String recipient)
	{
		this.rootDirectory = rootDirectory;
		this.recipient = recipient;
	}

	public static void main(String[] args){
		try{
			Logger.debug(UnusedFilesCleaner.class, "About to run UnusedFilesCleaner");
			String rootDirectory = "/files";
			if (args != null && args.length > 0)
				rootDirectory = args[0];
			String recipient = null;
			if (args != null && args.length > 1)
				recipient = args[1];

			boolean disableUnusedPages = true;
			if (args != null && args.length > 2)
				disableUnusedPages = "true".equals(args[2]);

			UnusedFilesCleaner cleaner = new UnusedFilesCleaner(rootDirectory, recipient);
			List<DocDetails> unusedPages = cleaner.getUnusedFiles();
			Logger.debug(UnusedFilesCleaner.class, "UnusedFilesCleaner size="+unusedPages.size());
			if (unusedPages.size() == 0)
			{
				return;
			}
			if (disableUnusedPages) cleaner.disable(unusedPages);
			cleaner.notifyOfChange(unusedPages);
			DocDB.getInstance(true);
			Logger.debug(UnusedFilesCleaner.class, "UnusedFilesCleaner finished");
		}
		catch (Exception e){
			sk.iway.iwcm.Logger.error(e);
		}
	}

	private void notifyOfChange(List<DocDetails> unusedPages)
	{
		if (recipient == null)
			return;

		Logger.printf(UnusedFilesCleaner.class, "Sending email to %s", recipient);
		StringBuilder messageContent = new StringBuilder("Following file-pages were disabled due to no link pointing at them.<br />");

		for (DocDetails disabled : unusedPages)
			messageContent.append(disabled.getExternalLink()).append("<br />\n");

		new MailHelper().
			addRecipient(recipient).
			setFromEmail(recipient).
			setFromName("Pages automatically disabled").
			setSubject("Pages automatically disabled").
			setMessage(messageContent.toString()).
			send();
	}

	private void disable(List<DocDetails> unusedPages)
	{
		Logger.debug(UnusedFilesCleaner.class, "Disabling unused pages");
		StringBuilder disableSqlIn = new StringBuilder();
		for(int i = 0; i < unusedPages.size(); i++)
		{
			disableSqlIn.append(',').append(unusedPages.get(i).getDocId());
			// SQL IN() containing >10 000 ids would fire an exception similar to "query too long"
			if (i % 100 == 0)
			{
				disableBatch(disableSqlIn);
				disableSqlIn = new StringBuilder();
			}
		}
		disableBatch(disableSqlIn);
	}

	private void disableBatch(StringBuilder disableSqlIn)
	{
		if(disableSqlIn.length() == 0)
			return;
		//preceding ","
		disableSqlIn.deleteCharAt(0);
		String sql = "UPDATE documents SET available = "+DB.getBooleanSql(false)+" WHERE doc_id IN("+disableSqlIn+")";
		Adminlog.add(Adminlog.TYPE_PAGE_UPDATE, "UnusedFileCleaner ran: "+sql, -1, -1);
		new SimpleQuery().execute(sql);
	}

	private List<DocDetails> getUnusedFiles()
	{
		List<UnusedFile> unused = FileTools.getDirFileUsage(rootDirectory, null);
		List<DocDetails> pagesOfUnusedFiles = new ArrayList<>();
		//map lookup is WAY faster than iterating through basicDocDetailsAll
		Map<String, Integer> externalLinkToDocId = new HashMap<>();
		for (DocDetails doc : DocDB.getInstance().getBasicDocDetailsAll())
			if (doc.isAvailable() && isNotEmpty(doc.getExternalLink()))
				externalLinkToDocId.put(doc.getExternalLink(), doc.getDocId());

		for (UnusedFile file : unused)
		{
			String filePath = String.format("%s/%s", file.getVirtualParent(), file.getName());
			Integer docId = externalLinkToDocId.get(filePath);

			Logger.debug(UnusedFilesCleaner.class, "Testing: "+filePath+" docId="+docId+" size="+pagesOfUnusedFiles.size());

			if (docId == null)
				continue;

			pagesOfUnusedFiles.add(DocDB.getInstance().getBasicDocDetails(docId, false));
		}

		return pagesOfUnusedFiles;
	}
}